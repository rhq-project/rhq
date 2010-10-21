/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.core.domain.configuration;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.test.AbstractEJB3Test;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;

public class ResourceConfigurationUpdateIntegrationTest extends AbstractEJB3Test {

    @Test(groups = "integration.ejb3")
    public void testUpdate() throws Exception {
        try {
            getTransactionManager().begin();
            EntityManager entityMgr = getEntityManager();

            ResourceType resourceType = new ResourceType("Test Resource Type", "Test Plugin", ResourceCategory.PLATFORM,
                null);
            entityMgr.persist(resourceType);
            entityMgr.flush();

            Resource resource = new Resource("test-resource", "Test Resource", resourceType);
            resource.setUuid("123456789");
            entityMgr.persist(resource);
            entityMgr.flush();

            Configuration configuration = new Configuration();

            PropertyList listProperty = new PropertyList("listProperty");
            listProperty.add(new PropertySimple("x", 1));
            listProperty.add(new PropertySimple("x", 2));

            configuration.put(listProperty);

            ResourceConfigurationUpdate configUpdate = new ResourceConfigurationUpdate(resource, configuration,
                "tester");
            entityMgr.persist(configUpdate);
            entityMgr.flush();

            entityMgr.clear();

            configUpdate = entityMgr.find(ResourceConfigurationUpdate.class, configUpdate.getId());
            entityMgr.flush();
        } finally {
            getTransactionManager().rollback();
        }
    }

}
