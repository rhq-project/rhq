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