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

import java.net.URL;

import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.enterprise.server.ws.Advisory;
import org.rhq.enterprise.server.ws.ObjectFactory;
import org.rhq.enterprise.server.ws.Subject;
import org.rhq.enterprise.server.ws.TestPropertiesInterface;
import org.rhq.enterprise.server.ws.WebservicesManagerBeanService;
import org.rhq.enterprise.server.ws.WebservicesRemote;
import org.rhq.enterprise.server.ws.test.util.WsSubjectUtility;
import org.rhq.enterprise.server.ws.utility.WsUtility;

/**
 * Web service tests for the advisory manager services.
 *
 * @author Jason Dobies
 */
@Test(groups = "ws")
public class WsAdvisoryManagerTest extends AssertJUnit implements TestPropertiesInterface {

    private static final boolean TESTS_ENABLED = true;

    private WebservicesRemote service;
    private ObjectFactory objectFactory;

    private WsSubjectUtility subjectUtil;

    @BeforeClass
    public void setup() throws Exception {

        // Variables needed
        URL gUrl = WsUtility.generateRemoteWebserviceURL(WebservicesManagerBeanService.class, host, port, useSSL);
        QName gQName = WsUtility.generateRemoteWebserviceQName(WebservicesManagerBeanService.class);

        // Establish outbound webservices connections
        WebservicesManagerBeanService jws = new WebservicesManagerBeanService(gUrl, gQName);
        service = jws.getWebservicesManagerBeanPort();

        objectFactory = new ObjectFactory();

        subjectUtil = new WsSubjectUtility(service);
    }

    @Test(enabled = TESTS_ENABLED)
    public void createGetDeleteAdvisory() throws Exception {

        // Setup
        String label = "WsAdvisoryManagerTest.createGetDeleteAdvisory.label";
        String synopsis = "WsAdvisoryManagerTest.createGetDeleteAdvisory.name";
        String type = "bugfix";

        Subject admin = subjectUtil.admin();

        // Test
        Advisory created = service.createAdvisory(admin, label, type, synopsis);

        assert created != null;

        Advisory retrieved = service.getAdvisoryByName(label);

        service.deleteAdvisoryByAdvId(admin, created.getId());

        // Verify
        assert label.equals(created.getAdvisory()) : "Found: " + created.getAdvisory();
        assert synopsis.equals(created.getSynopsis()) : "Found: " + created.getAdvisoryName();
        assert type.equals(created.getAdvisoryType()) : "Found: " + created.getAdvisoryType();

        assert retrieved != null;
        assert label.equals(retrieved.getAdvisory()) : "Found: " + retrieved.getAdvisory();
        assert synopsis.equals(retrieved.getSynopsis()) : "Found: " + retrieved.getAdvisoryName();
        assert type.equals(retrieved.getAdvisoryType()) : "Found: " + retrieved.getAdvisoryType();

        retrieved = service.getAdvisoryByName(label);

        assert retrieved == null;
    }

}
