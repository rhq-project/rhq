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

import org.rhq.core.domain.cloud.composite.FailoverListComposite;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.client.FailureCallback;
import org.rhq.enterprise.communications.command.client.RemoteCommunicator;
import org.rhq.enterprise.communications.util.CommUtils;

/**
 * This is a {@link FailureCallback} that will attempt to failover to another server if appropriate.
 * This callback will examine the failure exception that was detected and if it is recoverable
 * by failing over to another server, the agent will be switched to talk to another server found
 * in the agent failover list and the command will be retried.
 * 
 * @author John Mazzitelli
 */
public class FailoverFailureCallback implements FailureCallback {

    private static final String FAILOVER_ATTEMPTS = "rhq.failover-attempts";
    private static final Logger LOG = AgentI18NFactory.getLogger(FailoverFailureCallback.class);

    private AgentMain agent;

    public FailoverFailureCallback(AgentMain agent) {
        this.agent = agent;
    }

    public boolean failureDetected(RemoteCommunicator remoteCommunicator, Command command, CommandResponse response,
        Throwable throwable) {

        Throwable theProblem;

        // get the exception that caused the failure
        if (throwable != null) {
            theProblem = throwable;
        } else if (response != null && response.getException() != null) {
            theProblem = response.getException();
        } else {
            return false;
        }

        // determine if its an exception that can be corrected if we send it to another server
        boolean failoverNow = CommUtils.isExceptionFailoverable(theProblem);
        if (failoverNow == false) {
            return false;
        }

        // find out how many times we've already tried to failover
        int failoverAttempts;

        try {
            failoverAttempts = Integer.parseInt(command.getConfiguration().getProperty(FAILOVER_ATTEMPTS, "0"));
        } catch (Exception e) {
            // why in the world did this fail? should never happen, if it does, abort failover
            // since something is very weird and we don't want to be caught in a infinite loop
            // constantly attempting failovers
            command.getConfiguration().remove(FAILOVER_ATTEMPTS); // something's wrong with the value, so remove it
            return false;
        }

        FailoverListComposite failoverList = this.agent.getServerFailoverList();
        if (failoverList.hasNext() == false) {
            return false; // nothing to do, there isn't a next server in the list
        }

        int numFailoverServers = failoverList.size();

        // if we tried all the servers in our failover list, then stop trying
        if (failoverAttempts >= numFailoverServers) {
            LOG.warn(AgentI18NResourceKeys.TOO_MANY_FAILOVER_ATTEMPTS, numFailoverServers, theProblem);
            command.getConfiguration().remove(FAILOVER_ATTEMPTS);
            return false;
        } else {
            command.getConfiguration().setProperty(FAILOVER_ATTEMPTS, Integer.toString(failoverAttempts + 1));
        }

        // attempt to go to another server.  we always retry after this - we either successfully
        // got a new server in which case the request should succeed now or the next server on the failure list
        // is also down and our next request will trigger us again to try the next server in the list
        this.agent.failoverToNewServer(remoteCommunicator);
        return true;
    }
}
