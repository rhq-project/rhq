/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.storage.installer;

/**
 * @author John Sanda
 */
public class StorageInstallerException extends Exception {

    private int errorCode;

    public StorageInstallerException() {
        super();
    }

    public StorageInstallerException(String message) {
        super(message);
    }

    public StorageInstallerException(String message, int errorCode) {
        this(message);
        this.errorCode = errorCode;
    }

    public StorageInstallerException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageInstallerException(String message, Throwable cause, int errorCode) {
        this(message, cause);
        this.errorCode = errorCode;
    }

    public StorageInstallerException(Throwable cause) {
        super(cause);
    }

    public int getErrorCode() {
        return errorCode;
    }
}
