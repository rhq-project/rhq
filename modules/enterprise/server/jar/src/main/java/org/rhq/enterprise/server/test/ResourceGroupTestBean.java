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
package org.rhq.enterprise.server.test;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.RHQConstants;

@Stateless
public class ResourceGroupTestBean implements ResourceGroupTestBeanLocal {
    private static int compatCounter = 0;

    private int getNextCompat() {
        return compatCounter++;
    }

    private static int mixedCounter = 0;

    private int getNextMixed() {
        return mixedCounter++;
    }

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public void setupCompatibleGroups() {
        List<ResourceType> types = entityManager.createQuery("SELECT rt FROM ResourceType rt").getResultList();
        for (ResourceType type : types) {
            Query query = entityManager.createQuery("SELECT res FROM Resource res WHERE res.resourceType = :type");
            query.setParameter("type", type);
            List<Resource> resources = query.getResultList();
            ResourceGroup compatGroup = new ResourceGroup("Compat Group - " + getNextCompat(), type);
            entityManager.persist(compatGroup);

            compatGroup.getExplicitResources().addAll(resources);
            compatGroup.getImplicitResources().addAll(resources);
            for (Resource resource : resources) {
                resource.getExplicitGroups().add(compatGroup);
                resource.getImplicitGroups().add(compatGroup);
                entityManager.merge(resource);
            }

            entityManager.merge(compatGroup);
        }
    }

    @SuppressWarnings("unchecked")
    public void setupUberMixedGroup() {
        Query query = entityManager.createQuery("SELECT res FROM Resource res");
        List<Resource> resources = query.getResultList();
        ResourceGroup mixedGroup = new ResourceGroup("Mixed Group - " + getNextMixed());
        entityManager.persist(mixedGroup);

        mixedGroup.getExplicitResources().addAll(resources);
        mixedGroup.getImplicitResources().addAll(resources);
        for (Resource resource : resources) {
            resource.getExplicitGroups().add(mixedGroup);
            resource.getImplicitGroups().add(mixedGroup);
            entityManager.merge(resource);
        }

        entityManager.merge(mixedGroup);
    }
}