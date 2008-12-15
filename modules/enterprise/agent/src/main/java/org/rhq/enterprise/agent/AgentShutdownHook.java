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

import java.util.ArrayList;
import java.util.List;

import mazz.i18n.Logger;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Provides a way to gracefully shutdown the agent.  This can be used
 * as a shutdown hook, and it can also be used to perform some shutdown
 * functions, like attempting to interrupt hanging threads.
 * 
 * @author John Mazzitelli
 */
public class AgentShutdownHook extends Thread {
    private final Logger log = AgentI18NFactory.getLogger(AgentShutdownHook.class);

    /**
     * The agent that will be shutdown when the shutdown hook is triggered.
     */
    private final AgentMain agent;

    /**
     * Constructor for {@link AgentShutdownHook} that retains the reference to the agent that will be shutdown when
     * the shutdown hook is triggered.
     *
     * @param agent the agent to be shutdown when shutdown hook is triggered.
     */
    public AgentShutdownHook(AgentMain agent) {
        this.agent = agent;
    }

    /**
     * This is executed when the VM is shutting down.
     */
    @Override
    public void run() {
        showMessage(AgentI18NResourceKeys.EXIT_SHUTTING_DOWN);

        try {
            agent.shutdown();

            // try to interrupt all non-daemon threads so they die faster; but only try a fixed number of times
            int threadsStillAlive = waitForNonDaemonThreads();
            if (threadsStillAlive > 0) {
                showMessage(AgentI18NResourceKeys.SHUTDOWNHOOK_THREADS_STILL_ALIVE, threadsStillAlive);
            }

            // set our timebomb to ensure the agent dies
            spawnKillThread(1000L * 60 * 5);

        } catch (Throwable t) {
            String errors = ThrowableUtil.getAllMessages(t);
            log.error(t, AgentI18NResourceKeys.EXIT_SHUTDOWN_ERROR, errors);
            agent.getOut().println(agent.getI18NMsg().getMsg(AgentI18NResourceKeys.EXIT_SHUTDOWN_ERROR, errors));
        }

        showMessage(AgentI18NResourceKeys.EXIT_SHUTDOWN_COMPLETE);
        return;
    }

    /**
     * If you want the current agent to be dead this method will attept to wait for all
     * other non-daemon threads to die before returning.
     * 
     * We log messages if we can't wait for them all for whatever reason.
     * 
     * Note that obviously this method will not wait for the calling thread
     * that is currently running this method.
     * 
     * @return the number of still active non-daemon threads that haven't died even after waiting
     */
    public int waitForNonDaemonThreads() {
        try {
            int countdown = 10; // we don't want to do this forever, when this gets to 0, stop
            int threadsStillActive = Integer.MAX_VALUE; // prime the pump
            while ((threadsStillActive > 0) && (countdown-- > 0)) {
                threadsStillActive = 0;
                List<Thread> threads = interruptAllNonDaemonThreads();
                showMessage(AgentI18NResourceKeys.SHUTDOWNHOOK_THREAD_WAIT, threads.size());
                for (Thread thread : threads) {
                    try {
                        thread.join(10000L);
                    } catch (InterruptedException ie) {
                    } finally {
                        if (thread.isAlive()) {
                            threadsStillActive++;
                        }
                    }
                }
            }
            if (threadsStillActive > 0) {
                showMessage(AgentI18NResourceKeys.SHUTDOWNHOOK_THREAD_NO_MORE_WAIT, threadsStillActive);
            }
            return threadsStillActive;
        } catch (Throwable t) {
            showMessage(AgentI18NResourceKeys.SHUTDOWNHOOK_THREAD_CANNOT_WAIT, ThrowableUtil.getAllMessages(t));
            return Thread.activeCount();
        }
    }

    /**
     * We need to make sure our VM can die quickly, so this method will send interrupts
     * to all non-daemon threads to try to get them to hurry up and die.
     * 
     * @return the threads that were interrupted
     */
    public List<Thread> interruptAllNonDaemonThreads() {
        List<Thread> nonDaemonThreads = new ArrayList<Thread>();
        int threadCount = Thread.activeCount();
        Thread[] threads = new Thread[threadCount + 50]; // give a little more in case more threads were added quickly
        Thread.enumerate(threads);
        for (Thread thread : threads) {
            // do not interrupt or count:
            // - daemon threads
            // - threads with 0 stack elements (these are system threads that won't hold up the VM exit)
            // - our current thread
            // interrupt but do not count:
            // - the agent input thread
            if (thread != null && !thread.isDaemon() && thread.getStackTrace().length > 0
                && !thread.getName().equals(Thread.currentThread().getName())) {
                if (!thread.getName().equals(AgentMain.PROMPT_INPUT_THREAD_NAME)) {
                    nonDaemonThreads.add(thread);
                }
                thread.interrupt();
            }
        }
        return nonDaemonThreads;
    }

    /**
     * If something goes wrong and the VM does not die on its own when we think it should,
     * the thread created by this method will explicitly kill the VM.
     * Calling this method sets a timebomb that will kill the VM after a timer runs out.
     * There is no way to stop this timebomb short of performing 007-type heroics.
     * 
     * @param doomsday the number of milliseconds that the VM has left to live; once this
     *        time expires, a kill thread will execute System.exit(0).
     */
    public void spawnKillThread(final long doomsday) {
        Runnable killRunnable = new Runnable() {
            public void run() {
                try {
                    Thread.sleep(doomsday);
                } catch (Throwable t) {
                } finally {
                    System.exit(0); // boom.
                }
            }
        };

        Thread killThread = new Thread(killRunnable, "RHQ Agent Kill Thread");
        killThread.setDaemon(true); // don't let the VM hang around just for us, hopefully, this kill thread isn't needed
        killThread.start();
    }

    /**
     * Because this object is performing very important and serious things, we will
     * both log the message and output it to the console, to give the user ample
     * notification of what is going on.
     * 
     * @param msg
     * @param args
     */
    private void showMessage(String msg, Object... args) {
        try {
            log.info(msg, args);
            agent.getOut().println(this.agent.getI18NMsg().getMsg(msg, args));
        } catch (Throwable t) {
            // do not allow exceptions that occur in here to stop our shutdown
        }
    }
}
