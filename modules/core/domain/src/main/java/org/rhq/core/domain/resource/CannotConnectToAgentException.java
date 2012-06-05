/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.rhq.core.domain.resource;

import javax.ejb.ApplicationException;

/**
 * This exception should be throw when invoking an agent service fails because the agent is
 * down or otherwise unreachable. This class is defined in the in the domain module instead
 * of the server/jar module because it needs to be accessible by the UI.
 *
 * @author John Sanda
 */
@ApplicationException(rollback = true)
public class CannotConnectToAgentException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CannotConnectToAgentException() {
        super();
    }

    public CannotConnectToAgentException(String message) {
        super(message);
    }

    public CannotConnectToAgentException(String message, Throwable cause) {
        super(message, cause);
    }

    public CannotConnectToAgentException(Throwable cause) {
        super(cause);
    }
}
