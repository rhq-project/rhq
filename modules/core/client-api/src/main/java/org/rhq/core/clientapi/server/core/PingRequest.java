/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.core.clientapi.server.core;

/**
 * A simple POJO for requesting actions or data from the ping.
 * 
 * @author Jay Shaughnessy
 */
public class PingRequest {
    private int agentId;
    private boolean requestUpdateAvailability;
    private boolean requestServerTimestamp;

    private boolean replyUpdateAvailability;
    private Long replyServerTimestamp;

    public PingRequest(int agentId) {
        this(agentId, true, true);
    }

    public PingRequest(int agentId, boolean requestUpdateAvailability, boolean requestServerTimestamp) {
        this.requestUpdateAvailability = requestUpdateAvailability;
        this.requestServerTimestamp = requestServerTimestamp;
    }

    public boolean isRequestUpdateAvailability() {
        return requestUpdateAvailability;
    }

    public boolean isRequestServerTimestamp() {
        return requestServerTimestamp;
    }

    public boolean isReplyUpdateAvailability() {
        return replyUpdateAvailability;
    }

    public int getAgentId() {
        return agentId;
    }

    public void setReplyUpdateAvailability(boolean replyUpdateAvailability) {
        this.replyUpdateAvailability = replyUpdateAvailability;
    }

    public Long getReplyServerTimestamp() {
        return replyServerTimestamp;
    }

    public void setReplyServerTimestamp(Long replyServerTimestamp) {
        this.replyServerTimestamp = replyServerTimestamp;
    }
}
