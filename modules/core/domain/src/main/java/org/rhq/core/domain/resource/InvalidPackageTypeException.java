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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.core.domain.resource;

import javax.ejb.ApplicationException;

/**
 * Indicates an invalid PackageType for deploying to resource. Note that this is an ApplicationException
 * and when throwing this from an EJB it will not be wrapped as an EJBException.
 */
@ApplicationException(inherited = true)
public class InvalidPackageTypeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidPackageTypeException(String packageTypeName, ResourceType resourceType, String supportedTypes) {
        super("Package of PackageType[name=" + packageTypeName + "] cannot be deployed to resource of " + resourceType
            + " allowed packageTypes are " + supportedTypes);
    }

}
