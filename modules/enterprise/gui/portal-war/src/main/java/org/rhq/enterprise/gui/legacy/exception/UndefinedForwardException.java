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
package org.rhq.enterprise.gui.legacy.exception;

import javax.servlet.ServletException;

/**
 * An exception thrown by the Service Locator to indicate a JNDI failure of some sort.
 */
public class UndefinedForwardException extends ServletException {
    public UndefinedForwardException() {
    }

    public UndefinedForwardException(String s) {
        super(s);
    }

    public UndefinedForwardException(Throwable t) {
        super(t);
    }

    public UndefinedForwardException(String s, Throwable t) {
        super(s, t);
    }
}