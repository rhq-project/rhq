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
package org.rhq.core.clientapi.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.rhq.core.util.exception.WrappedRemotingException;

/**
 * This generic exception thrown by the plugin container is part of its client API; thus this exception is available to
 * both the plugin container as well as its remote clients.
 *
 * <p>Make sure that when constructing these exceptions, that any {@link #getCause() cause} is also available to remote
 * clients as well! If you are unsure, wrap the cause in a {@link org.rhq.core.util.exception.WrappedRemotingException}
 * before passing it to this class' constructor.</p>
 *
 * @author John Mazzitelli
 */
public class PluginContainerException extends Exception {
    private static final long serialVersionUID = 1L;

    private static Throwable wrapIfNecessary(Throwable e){
     // we assume everyone has java.* exception definitions available
        // see if the exception and all its causes are all java.* exceptions
        Throwable check_for_java = e;
        boolean all_java_exceptions = true; // if false, e or one of its causes is not a java.* exception

        while (check_for_java != null) {
            if (!check_for_java.getClass().getName().startsWith("java.")) {
                all_java_exceptions = false;
                break; // don't bother continuing, we found a non-java.* exception
            }

            if (check_for_java.getCause() == check_for_java) {
                check_for_java = null; // reached the end of the causes chain
            } else {
                check_for_java = check_for_java.getCause();
            }
        }
        
        // if the exception and all its causes are java.*, then just return e as-is, unless its not serializable
        if (!all_java_exceptions) {
            if (e.getClass().equals(WrappedRemotingException.class)) return e;
            return new WrappedRemotingException(e);
        }
        return e;
    }
    
    
    /**
     * Because this exception is part of the plugin container's client API and thus is to be available on remote clients
     * as well, make sure the <code>cause</code> throwable you pass to this constructor is also available to remote
     * clients. If it is not or you are unsure, wrap the cause in a
     * {@link org.rhq.core.util.exception.WrappedRemotingException} before passing it in to this constructor.
     *
     * @see Throwable#Throwable(Throwable)
     */
    public PluginContainerException(Throwable cause) {
        super(wrapIfNecessary(cause));
    }

    /**
     * Because this exception is part of the plugin container's client API and thus is to be available on remote clients
     * as well, make sure the <code>cause</code> throwable you pass to this constructor is also available to remote
     * clients. If it is not or you are unsure, wrap the cause in a
     * {@link org.rhq.core.util.exception.WrappedRemotingException} before passing it in to this constructor.
     *
     * @see Throwable#Throwable(String, Throwable)
     */
    public PluginContainerException(String message, Throwable cause) {
        super(message, wrapIfNecessary(cause));
     }

    /**
     * @see Throwable#Throwable()
     */
    public PluginContainerException() {
    }

    /**
     * @see Throwable#Throwable(String)
     */
    public PluginContainerException(String message) {
        super(message);
    }

}