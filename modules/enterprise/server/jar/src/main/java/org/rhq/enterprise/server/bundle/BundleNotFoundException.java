/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.enterprise.server.bundle;

import javax.ejb.ApplicationException;

/**
 * Indicates a required RHQ bundle did not exist. Note, we intentionally do not provide
 * constructors that take a cause, since the three screen long Hibernate stack trace doesn't add any value here.<br/>
 * Declare this an {@link ApplicationException} because we don't want these to be wrapped or to rollback an ongoing
 * transaction.
 */
@ApplicationException(rollback = false, inherited = true)
public class BundleNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    // Default no-arg constructor required by JAXB
    public BundleNotFoundException() {
    }

    /**
     * Create an exception indicating the resource with the specified id was not found.
     *
     * @param bundleId a bundle id
     */
    public BundleNotFoundException(int bundleId) {
        super("A Bundle with id " + bundleId + " does not exist.");
    }

    public BundleNotFoundException(String message) {
        super(message);
    }
}