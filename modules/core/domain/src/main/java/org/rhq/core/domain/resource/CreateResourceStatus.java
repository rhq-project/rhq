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
package org.rhq.core.domain.resource;

/**
 * Status indicators for a create resource request made to a plugin.
 *
 * @author Jason Dobies
 */
public enum CreateResourceStatus {
    // Enumeration  --------------------------------------------

    /**
     * Indicates the request has been submitted to the agent but we have not yet received a response. Requests in this
     * state are subject to cleanup if they have been in this state for an extended period of time without a reply
     * from the agent.
     */
    IN_PROGRESS("In Progress"),

    /**
     * Indicates the result has been returned from the agent and the plugin was able to create the requested resource.
     */
    SUCCESS("Success"),

    /**
     * Indicates the agent has responded to this request and indicated a failure occurred in either the agent,
     * plugin container, or plugin itself. An error message will be provided in the request to further describe
     * the failure. The resource has <em>not</em> been created.
     */
    FAILURE("Failure"),

    /**
     * The resource was created, however there were errors configuring it. The likely cause is that one or more values
     * in the configuration failed plugin-side validation. The resource will appear as created and will be discovered,
     * however it may not function correctly until the configuration errors are corrected.
     */
    INVALID_CONFIGURATION("Invalid Configuration"),

    /**
     * The resource was created, however there were errors while creating the artifact associated with the resource.
     * The resource will appear as created and will be discovered, however it may not function correctly until the
     * artifact is correctly deployed to the resource.
     */
    INVALID_ARTIFACT("Invalid Artifact"),

    /**
     * Indicates the request was successfully sent to the agent, however a response was not received in a timely manner.
     * This state prevents the UI from showing a perpetually in progress request. It is possible that a request may
     * leave this state at a later time if the agent eventually returns with a response on this request.
     */
    TIMED_OUT("Timed Out");

    // Attributes  --------------------------------------------

    private String displayName;

    // Constructors  --------------------------------------------

    CreateResourceStatus(String displayName) {
        this.displayName = displayName;
    }

    // Object Overridden Methods  --------------------------------------------

    public String toString() {
        return displayName;
    }
}