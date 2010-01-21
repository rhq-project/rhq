/*
* RHQ Management Platform
* Copyright (C) 2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.ws.test;

import java.lang.Exception;
import java.net.URL;
import java.util.List;

import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.enterprise.server.ws.*;
import org.rhq.enterprise.server.ws.test.util.WsResourceUtility;
import org.rhq.enterprise.server.ws.test.util.WsSubjectUtility;
import org.rhq.enterprise.server.ws.utility.WsUtility;

/**
 * Web service tests for the registration manager services.
 *
 * @author Jason Dobies
 */
public class WsRegistrationManagerTest extends AssertJUnit implements TestPropertiesInterface {

    private static final boolean TESTS_ENABLED = true;

    private WebservicesRemote service;
    private ObjectFactory objectFactory;

    private Subject subject;

    @BeforeClass
    public void setup() throws Exception {

        // Variables needed
        URL gUrl = WsUtility.generateRemoteWebserviceURL(WebservicesManagerBeanService.class, host, port, useSSL);
        QName gQName = WsUtility.generateRemoteWebserviceQName(WebservicesManagerBeanService.class);

        // Establish outbound webservices connections
        WebservicesManagerBeanService jws = new WebservicesManagerBeanService(gUrl, gQName);
        service = jws.getWebservicesManagerBeanPort();

        objectFactory = new ObjectFactory();

        // Establish a new user in the system
        WsSubjectUtility subjectUtil = new WsSubjectUtility(service);
        subject = subjectUtil.createOrLoginUser("reg-manager-test", "reg-manager-test");
    }

    @AfterClass
    public void teardown() throws Exception {
        WsSubjectUtility subjectUtil = new WsSubjectUtility(service);
        subjectUtil.deleteUser(subject.getId());
    }

    @Test(enabled = TESTS_ENABLED)
    public void importPlatform() throws Exception {

        // Setup
        WsResourceUtility resourceUtil = new WsResourceUtility(service);
        WsSubjectUtility subjectUtil = new WsSubjectUtility(service);
        Subject admin = subjectUtil.admin();

        // Test
        Resource created = resourceUtil.randomPlatform();
        service.importPlatform(admin, created);

        // Verify and Cleanup
        try {
            ResourceCriteria criteria = objectFactory.createResourceCriteria();
            criteria.setFilterId(created.getId());
            List<Resource> resources = service.findResourcesByCriteria(admin, criteria);

            assert resources.size() == 1 : "Incorrect number of resources returned. Found: " + resources.size();

            Resource retrieved = resources.get(0);

            assert retrieved.getName().equals(created.getName());
            assert retrieved.getInventoryStatus().equals(InventoryStatus.COMMITTED);
        } finally {
            resourceUtil.deleteResource(created.getId());
        }

    }
}
