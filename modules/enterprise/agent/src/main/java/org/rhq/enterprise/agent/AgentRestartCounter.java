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

/**
 * Provides a count of the number of times the agent has been restarted
 * and a reason for the last time the agent was restarted.  More
 * technically, this is the count of the number of times the
 * agent was started during the full lifetime of the agent's JVM.
 * 
 * @author John Mazzitelli
 */
public class AgentRestartCounter {
    public enum AgentRestartReason {
        /**
         * The agent was started at agent JVM process start time. 
         */
        PROCESS_START,

        /**
         * The agent was restarted by an agent prompt command.
         */
        PROMPT_COMMAND,

        /**
         * The agent was restarted by the agent plugin's restart operation.
         */
        OPERATION,

        /**
         * The agent was restarted by the VM Health Check in an attempt
         * to correct a critical error occurring in the agent.
         */
        VM_HEALTH_CHECK
    }

    private AgentRestartReason lastRestartReason = AgentRestartReason.PROCESS_START;
    private int restartCount = 0;

    /**
     * The reason why the agent was last restarted. This is normally
     * useful when you want to find out if the last time the restart occurred
     * was because the VM health check thread reset the agent, but it could be
     * potentially useful if you want to know that a user restarted it
     * via an operation or the start prompt command.
     * 
     * @return reason code
     */
    public AgentRestartReason getLastAgentRestartReason() {
        return this.lastRestartReason;
    }

    /**
     * Returns the number of times the agent has been restarted since the
     * beginning of the agent's JVM lifetime. Typically, this value will
     * be 1 and the {@link #getLastAgentRestartReason() reason} will be
     * {@link AgentRestartReason#PROCESS_START} to indicate the agent
     * was started at JVM startup time.
     * This will be 0 if the agent JVM process was created but the agent
     * itself was never started at process start time.
     * If this value is larger than 1, the agent was restarted for some
     * reason, e.g. the VM health check thread deemed the agent at
     * critically low memory and thus restarted the agent in an attempt
     * to bring the agent back to normal memory usage.
     * 
     * @return restart count
     */
    public int getNumberOfRestarts() {
        return this.restartCount;
    }

    /**
     * This should be called whenever the agent is restated. This will increment
     * the internal counter and set the {@link #getLastAgentRestartReason() last reason}
     * to the given reason code.
     * 
     * @param reason the reason why the agent was restarted
     */
    public void restartedAgent(AgentRestartReason reason) {
        this.restartCount++;
        this.lastRestartReason = reason;
    }
}
