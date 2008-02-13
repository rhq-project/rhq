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
package org.rhq.core.clientapi.agent;

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

    /**
     * Because this exception is part of the plugin container's client API and thus is to be available on remote clients
     * as well, make sure the <code>cause</code> throwable you pass to this constructor is also available to remote
     * clients. If it is not or you are unsure, wrap the cause in a
     * {@link org.rhq.core.util.exception.WrappedRemotingException} before passing it in to this constructor.
     *
     * @see Throwable#Throwable(Throwable)
     */
    public PluginContainerException(Throwable cause) {
        super(cause);
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
        super(message, cause);
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