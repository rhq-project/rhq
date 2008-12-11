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

import mazz.i18n.Logger;

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
    private static final Logger LOG = AgentI18NFactory.getLogger(AgentUpdateThread.class);

    private final AgentMain agent;
    private AgentPrintWriter console;

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
     */
    public static void updateAgentNow(AgentMain agent, boolean wait) {
        Thread updateThread = new AgentUpdateThread(agent);
        updateThread.start();
        if (wait) {
            try {
                updateThread.join();
            } catch (InterruptedException e) {
            }
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
        super("RHQ Agent Update Thread");
        setDaemon(false); // can't be daemon because this thread is going to be the last one alive in this VM shortly
        this.agent = agent;
        this.console = agent.getOut();
    }

    @Override
    public void run() {
        boolean agentWasStarted = false;

        try {
            showMessage(AgentI18NResourceKeys.UPDATE_THREAD_STARTED);

            // if the agent is started, we need to shut it down now
            if (this.agent.isStarted()) {
                this.agent.shutdown();
                agentWasStarted = true;
            }

            // get the agent update binary
            try {
                AgentUpdateDownload aud = new AgentUpdateDownload(this.agent);
                aud.download();
                aud.validate();
                showMessage(AgentI18NResourceKeys.UPDATE_DOWNLOADED, aud.getAgentUpdateBinaryFile());
            } catch (Exception e) {
                showErrorMessage(AgentI18NResourceKeys.UPDATE_DOWNLOAD_FAILED, e.getMessage());
                throw e;
            }

            // TODO: spawn a new Java VM to run the update jar: java -jar aud.getAgentUpdateBinaryFile()

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
                showFinalFailureMessage();
            }

            // do not continue - we were not successful so we should not kill the agent VM
            return;
        }

        // We should only ever get here if everything was successful.
        // We now need to exit the thread; and if everything goes according to plan, the VM will now exit
        spawnKillThread(); // pull the pin - FIRE IN THE HOLE!
        return;
    }

    /**
     * If something goes wrong and the VM does not die on its own when we think it should,
     * the thread created by this method will explicitly kill the VM.
     * Calling this method sets a timebomb that will kill the VM after a timer runs out.
     * There is no way to stop this timebomb short of performing 007-type heroics.
     */
    private void spawnKillThread() {
        Runnable killRunnable = new Runnable() {
            public void run() {
                try {
                    Thread.sleep(100L * 60 * 5); // 5 minutes is enough time to wait
                } catch (Throwable t) {
                } finally {
                    System.exit(1); // boom.
                }
            }
        };

        Thread killThread = new Thread(killRunnable, "RHQ Agent Kill Thread");
        killThread.setDaemon(true); // don't let the VM hang around just for us, hopefully, this kill thread isn't needed
        killThread.start();
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
        LOG.info(msg, args);
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
        LOG.fatal(msg, args);
        this.console.println(this.agent.getI18NMsg().getMsg(msg, args));
    }

    /**
     * This will also log a generic failure message to tell the user that the agent
     * is in a really bad state now and manual intervention by an administrator is
     * probably needed.
     */
    private void showFinalFailureMessage() {
        LOG.fatal(AgentI18NResourceKeys.UPDATE_THREAD_FAILURE);
        this.console.println(this.agent.getI18NMsg().getMsg(AgentI18NResourceKeys.UPDATE_THREAD_FAILURE));
    }
}