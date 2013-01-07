/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.enterprise.server.rest;

import javax.ejb.ApplicationException;

/**
 * Exception if stuff is not found
 * @author Heiko W. Rupp
 */
@ApplicationException(rollback = false, inherited = true)
public class StuffNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Construct a new instance of this Exception.
     * @param message Denotes what can't be found
     */
    public StuffNotFoundException(String message) {
        super(message + " not found");
    }
}
