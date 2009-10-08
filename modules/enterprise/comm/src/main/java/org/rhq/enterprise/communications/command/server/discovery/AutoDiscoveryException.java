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
package org.rhq.enterprise.communications.command.server.discovery;

/**
 * Exception that occurs with auto-discovery.
 *
 * @author John Mazzitelli
 */
public class AutoDiscoveryException extends Exception {
    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@link AutoDiscoveryException}.
     */
    public AutoDiscoveryException() {
        super();
    }

    /**
     * Constructor for {@link AutoDiscoveryException}.
     *
     * @param message
     */
    public AutoDiscoveryException(String message) {
        super(message);
    }

    /**
     * Constructor for {@link AutoDiscoveryException}.
     *
     * @param cause
     */
    public AutoDiscoveryException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor for {@link AutoDiscoveryException}.
     *
     * @param message
     * @param cause
     */
    public AutoDiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }
}