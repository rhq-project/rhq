/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.core.domain.bundle;

/**
 * Simple statuses for Bundle (Group) Deploy requests. When a request is first initiated, it will be considered in the
 * {@link #INPROGRESS} state. When the request has completed, it will have been either a {@link #SUCCESS} or
 * {@link #FAILURE}. For group deploy requests the status values are the same. Any individual failure will be
 * reflected as a group failure. 
 *
 * @author Jay Shaughnessy
 */
public enum BundleDeploymentStatus {
    PENDING("Pending"), // for future, when we support true scheduling of bundle deployments
    IN_PROGRESS("In Progress"), //
    MIXED("Mixed"), //
    SUCCESS("Success"), //
    FAILURE("Failure") //
    ; // need this for GWT

    private String displayName;

    BundleDeploymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String toString() {
        return displayName;
    }
}