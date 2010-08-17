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
package org.rhq.enterprise.server.exception;

/**
* This exception should be used whenever there was an error while trying to schedule an operation.  
* @author Noam Malki
*/
public class ScheduleException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    // Default no-arg constructor required by JAXB
    public ScheduleException() {
    }

    public ScheduleException(String message) {
        super(message);
    }

    public ScheduleException(Throwable t) {
        super(t);
    }

    public ScheduleException(String msg, Throwable t) {
        super(msg, t);
    }
}
