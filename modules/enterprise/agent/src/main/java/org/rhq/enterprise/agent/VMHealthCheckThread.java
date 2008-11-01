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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import mazz.i18n.Logger;

import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * This is a thread that will periodically check the health of the VM
 * (e.g. check the memory usage within the VM to detect if
 * memory is critically low), and if the health is poor it will put the
 * agent into hibernate mode, which will essentially shutdown the agent,
 * let it pause for some amount of time, then restart the agent. This
 * will hopefully clear up the poor VM condition.
 * 
 * @author John Mazzitelli
 */
public class VMHealthCheckThread extends Thread {
    private static final Logger LOG = AgentI18NFactory.getLogger(VMHealthCheckThread.class);

    /**
     * Will be <code>true</code> when this thread is told to stop checking. Note that this does not necessarily mean the
     * thread is stopped, it just means this thread was told to stop. See {@link #stopped}.
     */
    private boolean stop;

    /**
     * Will be <code>true</code> when this thread is stopped or will be stopped shortly.
     */
    private boolean stopped;

    /**
     * The agent that will be hibernated if the VM is critically sick.
     */
    private final AgentMain agent;

    /**
     * The amount of time in milliseconds that this thread will sleep in between checks
     */
    private final long interval;

    /**
     * If the amount of used heap memory is larger than this percentage of max heap memory
     * then the VM will be considered critically low on heap.
     */

    private final float heapThreshold;

    /**
     * If the amount of used non-heap memory is larger than this percentage of max non-heap memory,
     * then the VM will be considered critically low on heap.
     */
    private final float nonheapThreshold;

    public VMHealthCheckThread(AgentMain agent) {
        super("RHQ VM Health Check Thread");
        setDaemon(true);
        this.stop = false;
        this.stopped = true;
        this.agent = agent;
        this.interval = 5000L;
        this.heapThreshold = 0.90f;
        this.nonheapThreshold = 0.90f;
    }

    /**
     * Tells this thread to stop checking. This will block and wait for the thread to die.
     */
    public void stopChecking() {
        this.stop = true;

        // tell the thread that we flipped the stop flag in case it is waiting in a sleep interval
        synchronized (this) {
            while (!this.stopped) {
                try {
                    notify();
                    wait(5000L);
                } catch (InterruptedException e) {
                }
            }
        }

        return;
    }

    @Override
    public void run() {
        this.stopped = false;

        LOG.debug(AgentI18NResourceKeys.VM_HEALTH_CHECK_THREAD_STARTED, this.interval);

        MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();

        try {
            while (!this.stop) {
                try {
                    if (checkMemory(memoryMxBean)) {
                        LOG.fatal(AgentI18NResourceKeys.VM_HEALTH_CHECK_SEES_MEM_PROBLEM);
                        restartAgent(60000L);
                        continue;
                    }

                    // TODO: if our memory is good, we might have to check and make sure we are
                    //       the only thread running.  Under an odd and rare circumstance (if
                    //       restartAgent fails to completely start the agent but did manage to
                    //       start another VM check thread and failed to "re-shutdown" the agent)
                    //       there will end up being more than one of these threads running.
                    //       We'll need to make sure we kill all threads but one.

                    // go to sleep before we check again
                    synchronized (this) {
                        wait(this.interval);
                    }
                } catch (VirtualMachineError vme) {
                    // We're too late - OOM probably happening now.
                    // Try to do as little as possible here (no logging, no creating objects)
                    // and immediately try to shutdown our agent and restart it.
                    restartAgent(0L);
                } catch (InterruptedException e) {
                    this.stop = true;
                }
            }
        } catch (Throwable t) {
            LOG.error(AgentI18NResourceKeys.VM_HEALTH_CHECK_THREAD_EXCEPTION, t);
        }

        LOG.debug(AgentI18NResourceKeys.VM_HEALTH_CHECK_THREAD_STOPPED);
        this.stopped = true;

        return;
    }

    /**
     * This will {@link AgentMain#shutdown()} the agent, pause for the given number of milliseconds, then
     * {@link AgentMain#start()} the agent again.
     * 
     * @param pause number of milliseconds before restarting the agent after shutting down
     */
    private void restartAgent(long pause) throws Exception {
        // this method is going to kill our thread by calling stopChecking; to avoid deadlock, set our flags now
        this.stop = true;
        this.stopped = true;

        // immediately attempt to shutdown the agent which should free up alot of VM resources (memory/threads)
        try {
            this.agent.shutdown();
        } catch (Throwable t) {
            // this is bad, we can't even shutdown the agent.
            // but this thread is our only hope to recover, so do not stop the thread now
            // let it continue and see if we can recover the next time
            this.stop = false;
            this.stopped = false;
            Thread.interrupted(); // clear the interrupted status to ensure our thread doesn't abort
            Thread.sleep(30000L); // give our thread time to breath - do avoid fast infinite looping that might occur
            return;
        }

        if (pause > 0) {
            Thread.sleep(pause);
        }

        // now that the agent is shutdown and we've paused a bit, let's try to restart it
        try {
            this.agent.start();
        } catch (Throwable t) {
            // uh-oh, we can't start the agent for some reason; our thread is our last and only hope to recover

            // first try to shutdown again, in case start() got half way there but couldn't finish
            // do NOT set stop flags to false yet as this would cause a deadlock
            try {
                this.agent.shutdown();
                // TODO: purging spool: agentConfig.getDataDirectory() + agentConfig.getClientSenderCommandSpoolFileName()
            } catch (Throwable ignore) {
                // at this point, we may (or may not) have two VM check threads running, what should we do?
            }

            // do not stop the thread - let it continue and see if we can recover the next time
            this.stop = false;
            this.stopped = false;
            Thread.interrupted(); // clear the interrupted status to ensure our thread doesn't abort
            return;
        }

        // At this point, we have "rebooted" the agent - our memory usage should be back to normal.
        // TODO: what can we do to notify the server / user that we rebooted the agent?

        return;
    }

    /**
     * Checks the VM's memory subsystem and if it detects the VM is critically
     * low on memory, <code>true</code> will be returned.
     * 
     * @param bean the platform MBean that contains the memory statistics
     * 
     * @return <code>true</code> if the VM is critically low on memory
     */
    private boolean checkMemory(MemoryMXBean bean) {
        boolean heapCritical = false;
        boolean nonheapCritical = false;

        try {
            heapCritical = isCriticallyLow(bean.getHeapMemoryUsage(), this.heapThreshold, "heap");
            nonheapCritical = isCriticallyLow(bean.getNonHeapMemoryUsage(), this.nonheapThreshold, "nonheap");

            if (heapCritical || nonheapCritical) {
                // uh-oh, we are low on memory, before we say we are truly critical, try to GC
                try {
                    LOG.warn(AgentI18NResourceKeys.VM_HEALTH_CHECK_THREAD_GC);
                    bean.gc();

                    // let see what our memory usage is now
                    heapCritical = isCriticallyLow(bean.getHeapMemoryUsage(), this.heapThreshold, "heap");
                    nonheapCritical = isCriticallyLow(bean.getNonHeapMemoryUsage(), this.nonheapThreshold, "nonheap");
                } catch (Throwable t) {
                    // something bad is happening, let's return true and see if we can recover
                    return true;
                }
            }
        } catch (Throwable t) {
            // this should never happen unless something odd occurred.
            // let's return true only if we have previously detected critically low memory
        }

        return heapCritical || nonheapCritical;
    }

    /**
     * Returns <code>true</code> if the given memory usage indicates that
     * memory is critically low.
     * 
     * @param memoryUsage
     * @param d the percentage of used memory to max available memory that is
     *          the threshold to be considered critical. e.g. If this is 0.9, that means
     *          if the used memory is 90% or higher of the max, then there is
     *          a critical shortest of free memory and true will be returned
     * @param type the type of memory
     *          
     * @return <code>true</code> if the amount of used memory is over the threshold
     */
    private boolean isCriticallyLow(MemoryUsage memoryUsage, float thresholdPercentage, String type) {
        long used = memoryUsage.getUsed();
        long max = memoryUsage.getMax();

        if ((max > -1) && (used > (max * thresholdPercentage))) {
            LOG.warn(AgentI18NResourceKeys.VM_HEALTH_CHECK_THREAD_MEM_LOW, type, thresholdPercentage, memoryUsage);
            return true;
        }

        return false;
    }
}
