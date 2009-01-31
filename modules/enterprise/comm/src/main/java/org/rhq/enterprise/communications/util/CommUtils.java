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

import java.net.ConnectException;

import org.jboss.remoting.CannotConnectException;
import org.jboss.remoting.transport.http.WebServerError;

/**
 * Just a place where basic comm utility methods are placed for use by anything
 * that needs to use the comm layer.
 * 
 * @author John Mazzitelli
 */
public abstract class CommUtils {
    /**
     * Examines the given exception and determines if failing over to another server might help.
     * Typically, this method will look for "cannot connect" type exceptions.
     * 
     * @param t the exception to examine
     * 
     * @return <code>true</code> if failing over to another server might help; <code>false</code> if
     *         there is nothing about the exception that would lead us to believe another server would produce
     *         a different result.
     */
    public static boolean isExceptionFailoverable(Throwable t) {
        return (t instanceof CannotConnectException || t instanceof ConnectException
            || t instanceof NotProcessedException || t instanceof WebServerError);
    }
}
