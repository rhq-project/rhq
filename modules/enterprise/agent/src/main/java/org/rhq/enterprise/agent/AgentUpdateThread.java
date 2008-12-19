/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.agent;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import mazz.i18n.Logger;

import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * This is a thread that will attempt to update the agent to the latest
 * version. This will shutdown the currently running agent, so it is
 * "destructive" in the sense that if successful, the VM this thread is running
 * in will eventually die. 
 * 
 * @author John Mazzitelli
 */
public class AgentUpdateThread extends Thread {
    private static final String THREAD_NAME = "RHQ Agent Update Thread";

    private final Logger log = AgentI18NFactory.getLogger(AgentUpdateThread.class);

    private final AgentMain agent;
    private AgentPrintWriter console;

    private static AtomicBoolean updating = new AtomicBoolean(false);

    /**
     * This static method will immediately begin to update the agent.
     * Once you call this, there is no turning back - if all goes well,
     * the currently running agent (and the VM its running in) will soon exit.
     * 
     * @param agent the running agent that is to be updated
     * @param wait if <code>true</code>, this will wait for the update thread
     *        to die. Note that if the agent update is successful, and you pass
     *        wait=<code>true</code>, this method will never return. It will only
     *        return if the update failed and the VM is still alive. Pass
     *        <code>false</code> if you want to fire-and-forget the agent update
     *        thread and return immediately.
     * @throws IllegalStateException if the agent is already being updated
     * @throws UnsupportedOperationException if the agent is not allowed to update itself
     */
    public static void updateAgentNow(AgentMain agent, boolean wait) throws IllegalStateException {

        if (!agent.getConfiguration().isAgentUpdateEnabled()) {
            throw new UnsupportedOperationException(agent.getI18NMsg().getMsg(
                AgentI18NResourceKeys.UPDATE_DOWNLOAD_DISABLED_BY_AGENT));
        }

        lock(agent); // throws exception if we are already updating the agent

        Thread updateThread;

        try {
            updateThread = new AgentUpdateThread(agent);
            updateThread.start();
        } catch (Throwable t) {
            unlock(); // if for any reason we can't start it, unlock us so we can attempt later
            updateThread = null;
        }

        if (wait && updateThread != null) {
            while (updateThread.isAlive()) {
                try {
                    updateThread.join();
                } catch (InterruptedException e) {
                    // if we were interrupted, our while loop will keep us waiting
                }
            }
            Thread.currentThread().interrupt(); // interrupt the current thread so we help it exit faster
        }

        return;
    }

    /**
     * The constructor for the thread object. This is private, go through the
     * static factory method to instantiate (and start) the thread.
     * 
     * @param agent
     */
    private AgentUpdateThread(AgentMain agent) {
        super(THREAD_NAME);
        setDaemon(false); // can't be daemon because this thread is going to be the last one alive in this VM shortly
        this.agent = agent;
        this.console = agent.getOut();
    }

    @Override
    public void run() {
        boolean agentWasStarted = false;
        AgentShutdownHook shutdownHook = new AgentShutdownHook(this.agent);

        try {
            showMessage(AgentI18NResourceKeys.UPDATE_THREAD_STARTED);

            // if the agent is started, we need to shut it down now
            if (this.agent.isStarted()) {
                this.agent.shutdown();
                agentWasStarted = true;
            }

            // let's wait for the agent's threads to fully shutdown
            // (sometimes, the JBoss/Remoting threads take a long time to die)
            int numThreadsStillAlive = shutdownHook.waitForNonDaemonThreads();

            // get the agent update binary
            AgentUpdateDownload aud;
            try {
                aud = new AgentUpdateDownload(this.agent);
                aud.download();
                aud.validate();
                showMessage(AgentI18NResourceKeys.UPDATE_DOWNLOADED, aud.getAgentUpdateBinaryFile());
            } catch (Exception e) {
                showErrorMessage(AgentI18NResourceKeys.UPDATE_DOWNLOAD_FAILED, e.getMessage());
                throw e;
            }

            // spawn a new Java VM to run the update jar
            // if threads still aren't dead yet, make sure we pause the update longer than our kill thread wait time
            String javaExe = findJavaExe();
            List<String> args = new ArrayList<String>();
            args.add("-jar");
            args.add(aud.getAgentUpdateBinaryFile().getAbsolutePath());
            args.add("--pause=" + ((numThreadsStillAlive > 0) ? "80000" : "20000"));
            args.add("--update=" + this.agent.getAgentHomeDirectory());

            SystemInfo sysInfo = SystemInfoFactory.createSystemInfo();
            ProcessExecution processExecution = new ProcessExecution(javaExe);
            processExecution.setArguments(args);
            //processExecution.setEnvironmentVariables(envvars); 
            processExecution.setWorkingDirectory(new File(this.agent.getAgentHomeDirectory()).getParent());
            processExecution.setCaptureOutput(false);
            processExecution.setWaitForCompletion(0);
            ProcessExecutionResults results = sysInfo.executeProcess(processExecution);
            if (results.getError() != null) {
                throw results.getError();
            }

            // update has started! if this agent is running in non-daemon mode, kill
            // the input stream so the input thread knows to shutdown now
            try {
                BufferedReader in = this.agent.getIn();
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
            }
        } catch (Throwable t) {
            try {
                showErrorMessage(AgentI18NResourceKeys.UPDATE_THREAD_EXCEPTION, ThrowableUtil.getAllMessages(t));

                // We might not be able to do anything, but let's start the agent just in case it can still do something.
                // Note that if the agent wasn't started before, don't bother starting it now. The agent is probably
                // in non-daemon mode and the user has a console they can use to manipulate the agent state.
                if (agentWasStarted) {
                    try {
                        this.agent.start();
                    } catch (Throwable t2) {
                        showErrorMessage(AgentI18NResourceKeys.UPDATE_THREAD_CANNOT_RESTART, ThrowableUtil
                            .getAllMessages(t2));
                    }
                }
            } finally {
                unlock(); // we failed to update, unlock us so we can attempt later
                showFinalFailureMessage();
            }

            // do not continue - we were not successful so we should not kill the agent VM
            return;
        }

        // We should only ever get here if everything was successful.
        // We now need to exit the thread; and if everything goes according to plan, the VM will now exit
        shutdownHook.spawnKillThread(1000L * 60 * 1); // pull the pin - FIRE IN THE HOLE!
        return;
    }

    /**
     * Tries to find the Java executable that launched this agent so we can use it to launch
     * the agent update binary.
     * 
     * @return the path to the Java executable
     * 
     * @throws Exception if the Java executable could not be found
     */
    private String findJavaExe() throws Exception {
        final String envName = "RHQ_AGENT_JAVA_EXE_FILE_PATH";
        String envString = System.getenv(envName);
        if (envString != null) {
            showMessage(AgentI18NResourceKeys.UPDATE_THREAD_USING_JAVA_EXE, envString);
            return envString;
        }

        // that RHQ environment variable should always be there. But in the odd case where it isn't
        // let's try to guess where we can find the Java executable using another method
        showMessage(AgentI18NResourceKeys.UPDATE_THREAD_LOOKING_FOR_JAVA_EXE, envName);
        String javaExe = "java"; // fallback to this if we can't find it - let's hope its in our path
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            File[] guesses = new File[] { new File(javaHome, "bin/java"), new File(javaHome, "bin/java.exe"),
                new File(javaHome, "java"), new File(javaHome, "java.exe") };
            for (File guess : guesses) {
                if (guess.isFile()) {
                    javaExe = guess.getAbsolutePath();
                    break;
                }
            }
        }

        showMessage(AgentI18NResourceKeys.UPDATE_THREAD_USING_JAVA_EXE, javaExe);
        return javaExe;
    }

    /**
     * This sets the flag that ensures only one update thread is ever created at any one time.
     * If there is already an agent update thread created, this throws an exception.
     *
     * @param agent the agent that is to be updated
     * @throws IllegalStateException if the agent is already updating itself
     */
    private static void lock(AgentMain agent) throws IllegalStateException {
        if (!updating.compareAndSet(false, true)) {
            throw new IllegalStateException(agent.getI18NMsg().getMsg(AgentI18NResourceKeys.UPDATE_THREAD_DUP));
        }
    }

    /**
     * This sets the flag to allow another update thread to be created.
     */
    private static void unlock() {
        updating.set(false);
    }

    /**
     * Because this thread is performing very important and serious things, we will
     * both log the message and output it to the console, to give the user ample
     * notification of what is going on.
     * 
     * @param msg
     * @param args
     */
    private void showMessage(String msg, Object... args) {
        log.info(msg, args);
        this.console.println(this.agent.getI18NMsg().getMsg(msg, args));
    }

    /**
     * Because this thread is performing very important and serious things, we will
     * both log the error message and output it to the console, to give the user ample
     * notification of what is going on.
     * 
     * @param msg
     * @param args
     */
    private void showErrorMessage(String msg, Object... args) {
        // log at fatal level because if we can't update, it probably means the agent is dead in the water
        // and will never be able to talk to the server again - manual admin intervention is probably required now
        log.fatal(msg, args);
        this.console.println(this.agent.getI18NMsg().getMsg(msg, args));
    }

    /**
     * This will also log a generic failure message to tell the user that the agent
     * is in a really bad state now and manual intervention by an administrator is
     * probably needed.
     */
    private void showFinalFailureMessage() {
        log.fatal(AgentI18NResourceKeys.UPDATE_THREAD_FAILURE);
        this.console.println(this.agent.getI18NMsg().getMsg(AgentI18NResourceKeys.UPDATE_THREAD_FAILURE));
    }
}