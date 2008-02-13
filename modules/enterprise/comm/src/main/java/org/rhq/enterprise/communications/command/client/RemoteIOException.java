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
package org.rhq.enterprise.communications.command.client;

import java.io.IOException;

/**
 * This represents an IOException that occurred on a remote input stream.
 *
 * @author John Mazzitelli
 */
public class RemoteIOException extends IOException {
    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@link RemoteIOException}.
     */
    public RemoteIOException() {
        super();
    }

    /**
     * Constructor for {@link RemoteIOException}.
     *
     * @param message the exception message
     */
    public RemoteIOException(String message) {
        super(message);
    }

    /**
     * Constructor for {@link RemoteIOException}.
     *
     * @param t the true exception that occurred
     */
    public RemoteIOException(Throwable t) {
        // why, oh, why doesn't IOException have a constructor that takes a cause?
        super();
        initCause(t);
    }

    /**
     * Constructor for {@link RemoteIOException}.
     *
     * @param message the exception message
     * @param t       the true exception that occurred
     */
    public RemoteIOException(String message, Throwable t) {
        super(message);
        initCause(t);
    }
}