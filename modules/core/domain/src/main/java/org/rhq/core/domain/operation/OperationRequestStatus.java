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
package org.rhq.core.domain.operation;

/**
 * Simple statuses for operation invocation requests. When a request is first initiated, it will be considered in the
 * {@link #INPROGRESS} state. When the request has completed, it will have been either a {@link #SUCCESS} or
 * {@link #FAILURE}. An operation can be {@link #CANCELED} while in the inprogress state.
 *
 * @author John Mazzitelli
 */
public enum OperationRequestStatus {
    INPROGRESS("In Progress"), SUCCESS("Success"), FAILURE("Failure"), CANCELED("Cancelled");

    private String displayName;

    OperationRequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String toString() {
        return displayName;
    }
}