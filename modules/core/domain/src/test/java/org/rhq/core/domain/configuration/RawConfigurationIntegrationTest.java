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

import javax.persistence.EntityManager;

import org.testng.annotations.Test;

import org.rhq.core.domain.test.AbstractEJB3Test;
import org.rhq.core.util.MessageDigestGenerator;

public class RawConfigurationIntegrationTest extends AbstractEJB3Test {

    @Test(groups = "integration.ejb3")
    public void veryPersistAndFindById() throws Exception {
        getTransactionManager().begin();
        EntityManager entityMgr = getEntityManager();

        try {
            Configuration config = createAndSaveConfiguration();

            RawConfiguration rawConfig = new RawConfiguration();
            rawConfig.setConfiguration(config);
            String contents = "contents";
            String sha256 = new MessageDigestGenerator(MessageDigestGenerator.SHA_256).calcDigestString(contents);
            rawConfig.setContents(contents, sha256);
            rawConfig.setPath("/tmp/foo.txt");

            entityMgr.persist(rawConfig);

            RawConfiguration savedRawConfig = entityMgr.find(RawConfiguration.class, rawConfig.getId());

            assertNotNull("Failed to find " + RawConfiguration.class.getSimpleName() + " by id.", savedRawConfig);
        } finally {
            getTransactionManager().rollback();
        }
    }

    Configuration createAndSaveConfiguration() {
        Configuration config = new Configuration();
        EntityManager entityMgr = getEntityManager();

        entityMgr.persist(config);

        return config;
    }

}
