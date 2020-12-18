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
package org.rhq.enterprise.server.resource;

import javax.ejb.ApplicationException;

/**
 * Indicates a query for a single JON resource did not return any results. Note, we intentionally do not provide
 * constructors that take a cause, since the three screen long Hibernate stack trace doesn't add any value here.<br/>
 * Declare this an {@link ApplicationException} because we don't want these to be wrapped or to rollback an ongoing
 * transaction.
 */
@ApplicationException(rollback = false, inherited = true)
public class ResourceNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    // Default no-arg constructor required by JAXB
    public ResourceNotFoundException() {
    }

    /**
     * Create an exception indicating the resource with the specified id was not found.
     *
     * @param resourceId a resource id
     */
    public ResourceNotFoundException(int resourceId) {
        super("A Resource with id " + resourceId + " does not exist in inventory.");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException doomedResource(int resourceId) {
        return new ResourceNotFoundException("Resource with id " + resourceId + " was removed from inventory");
    }
}