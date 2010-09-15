/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.domain.content;

/**
 * Indicates the current state of a {@link ContentServiceRequest request}.
 *
 * @author Jason Dobies
 */
public enum ContentRequestStatus {
    /**
     * Indicates the request has been submitted to the agent but we have not yet received a response. Requests in this
     * state are subject to cleanup if they have been in this state for an extended period of time without a reply
     * from the agent.
     */
    IN_PROGRESS("In Progress"), //

    /**
     * Indicates the result has been returned from the agent and the plugin was able to execute the actions
     * described by the request.
     */
    SUCCESS("Success"), //

    /**
     * Indicates the agent has responded to this request and indicated a failure occurred in either the agent,
     * plugin container, or plugin itself. An error message will be provided in the request to further describe
     * the failure.
     */
    FAILURE("Failure"), //

    /**
     * Indicates the request was successfully sent to the agent, however a response was not received in a timely manner.
     * This state prevents the UI from showing a perpetually in progress request. It is possible that a request may
     * leave this state at a later time if the agent eventually returns with a response on this request.
     */
    TIMED_OUT("Timed Out");

    private String displayText;

    ContentRequestStatus(String displayText) {
        this.displayText = displayText; // not very I18N'ish :)
    }

    @Override
    public String toString() {
        return displayText;
    }
}