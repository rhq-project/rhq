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

import javax.ejb.Local;
import org.rhq.core.domain.resource.ProductVersion;
import org.rhq.core.domain.resource.ResourceType;

/**
 * EJB interface to deal with product versions.
 *
 * @author Jason Dobies
 */
@Local
public interface ProductVersionManagerLocal {
    /**
     * Adds a new product version into the system, returning the entity representation. If the product version already
     * exists, the entity will simply be returned.
     *
     * @param  resourceType cannot be <code>null</code>
     * @param  version      cannot be <code>null</code>
     *
     * @return product version persisted entity
     */
    ProductVersion addProductVersion(ResourceType resourceType, String version);
}