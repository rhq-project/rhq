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
package org.rhq.enterprise.communications.util;

/** 
 * Thrown when command processing is unavailable.  This is typically due to temporary global suspension of command processing.
 * It is different from {@link NotPermittedException} in that the command request itself did not violate any
 * constraint (e.g. security, concurrency limit).
 * 
 * Can optionally be provided a String supplying a reason for not processing the command.
 */
public class NotProcessedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    String reason = null;

    public NotProcessedException(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

}
