/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.core.domain.alert.notification;

/**
 * State of a SenderResult
 * @author Heiko W. Rupp
 * @see SenderResult
 */
public enum ResultState {
    /** Success: sending of the notification was a known success */
    SUCCESS,
    /** Partial: sending of the notification has some failures */
    PARTIAL,
    /** Failure: sending of the notification was a known failure */
    FAILURE,
    /** Deferred: sender action was invoked, but response can not be immediately determined */
    DEFERRED,
    /** Unknown: alert sender returned a null result */
    UNKNOWN
}
