/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.plugins.jbossas5.test.ejb2;

import static org.testng.Assert.fail;
import static org.testng.Assert.assertEquals;

import java.util.ArrayList;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.Resource;
import org.rhq.plugins.jbossas5.test.util.AppServerUtils;
import org.rhq.plugins.jbossas5.test.util.MethodArgDef;
import org.testng.Assert;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

/**
 * 
 * @author Lukas Krejci
 */
@Test(groups = "as5-plugin")
public class Ejb2EntityBeanResourceTest extends AbstractEjb2ResourceTest {

    private static final int CREATE_COUNT = 5;
    private static final int UPDATE_COUNT = 3;
    private static final int DELETE_COUNT = 3;

    private static final String KEY_PREFIX = "as5-plugin-ejb2-entity-bean-test-entity";

    private static final String JNDI_NAME = "as5-plugin-test/AllTypes";

    protected String getResourceTypeName() {
        return "EJB2 Entity Bean";
    }

    /**
     * Perform some CRUD operations so that we have some values to collect as metrics.
     */
    @BeforeGroups(groups = "as5-plugin")
    public void setupBean() {
        try {
            Object entityHome = AppServerUtils.getRemoteObject(JNDI_NAME, Object.class);

            //create the entities
            ArrayList<Object> entities = new ArrayList<Object>();
            for (int i = 0; i < CREATE_COUNT; ++i) {
                entities.add(AppServerUtils.invokeMethod("create", entityHome, new MethodArgDef[] { new MethodArgDef(String.class,
                    KEY_PREFIX + i) }));
            }

            //try to find the entities
            for (int i = 0; i < CREATE_COUNT; ++i) {
                AppServerUtils.invokeMethod("findByPrimaryKey", entityHome, new MethodArgDef[] { new MethodArgDef(String.class,
                    KEY_PREFIX + i) });
            }

            //update some
            for (int i = 0; i < UPDATE_COUNT; ++i) {
                Object entity = entities.get(i);
                AppServerUtils.invokeMethod("setInt", entity, new MethodArgDef[] { new MethodArgDef(int.class, i) });
            }

            //delete some
            for (int i = 0; i < DELETE_COUNT; ++i) {
                AppServerUtils.invokeMethod("remove", entities.get(i), (MethodArgDef[]) null);
            }
        } catch (Exception e) {
            fail("Failed to setup the EJB2 entity bean for testing.", e);
        }
    }

    //    @AfterGroups(groups = "as5-plugin")
    //    void cleanUpDb() {
    //        try {
    //            Object entityHome = AppServerUtils.getRemoteObject("as5-plugin-test/AllTypes", Object.class);
    //
    //            for (int i = DELETE_COUNT; i < CREATE_COUNT; ++i) {
    //                invokeMethod("remove", entityHome, new MethodArgDef[] {
    //                   new MethodArgDef(Object.class, KEY_PREFIX + i) 
    //                });
    //            }
    //        } catch (Exception e) {
    //            fail("Failed to clean up the DB after EJB2 entity test.", e);
    //        }
    //    }

    protected Configuration getTestResourceConfiguration() {
        return new Configuration();
    }

    @Override
    protected void validateNumericMetricValue(String metricName, Double value, Resource resource) {
        if (resource.getResourceKey().contains(JNDI_NAME)) {
            if ("CreateCount".equals(metricName)) {
                assertEquals(value, Double.valueOf(CREATE_COUNT), "Unexpected CreateCount");
            } else if ("RemoveCount".equals(metricName)) {
                assertEquals(value, Double.valueOf(DELETE_COUNT), "Unexpected RemoveCount");
            } else if ("CurrentPoolSize".equals(metricName)) {
                Assert.assertNotSame(value, Double.valueOf(0), "CurrentPoolSize shouldn't be zero. We've read some beans.");
            }
        } else {
            super.validateNumericMetricValue(metricName, value, resource);
        }
    }
}
