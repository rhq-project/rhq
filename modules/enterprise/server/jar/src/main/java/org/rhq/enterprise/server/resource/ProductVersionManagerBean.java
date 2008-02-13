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

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.resource.ProductVersion;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.RHQConstants;

/**
 * SLSB implementation of the {@link ProductVersionManagerLocal}.
 *
 * @author Jason Dobies
 */
@Stateless
public class ProductVersionManagerBean implements ProductVersionManagerLocal {
    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(this.getClass());

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    // ProductVersionManagerLocal Implementation  --------------------------------------------

    public ProductVersion addProductVersion(ResourceType resourceType, String version) {
        Query query = entityManager.createNamedQuery(ProductVersion.QUERY_FIND_BY_RESOURCE_TYPE_AND_VERSION);

        query.setParameter("resourceType", resourceType);
        query.setParameter("version", version);

        List resultList = query.getResultList();

        ProductVersion productVersion;
        if (resultList.size() == 0) {
            productVersion = new ProductVersion();
            productVersion.setResourceType(resourceType);
            productVersion.setVersion(version);

            entityManager.persist(productVersion);
        } else {
            productVersion = (ProductVersion) resultList.get(0);
        }

        return productVersion;
    }
}