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

import org.rhq.enterprise.server.ws.ObjectFactory;
import org.rhq.enterprise.server.ws.TestPropertiesInterface;
import org.rhq.enterprise.server.ws.WebservicesManagerBeanService;
import org.rhq.enterprise.server.ws.WebservicesRemote;
import org.rhq.enterprise.server.ws.test.util.WsResourceUtility;
import org.rhq.enterprise.server.ws.test.util.WsSubjectUtility;
import org.rhq.enterprise.server.ws.utility.WsUtility;

/**
 * Base class for all webservice unit tests. This class runs the setup and teardown
 * necessary to establish and release the connection to the remote server. Subclasses
 * can use the variables initialized here for testing purposes. 
 *
 * @author Jason Dobies
 */
public class WsUnitTestBase extends AssertJUnit implements TestPropertiesInterface {

    protected WebservicesRemote service;
    protected ObjectFactory objectFactory;

    protected WsSubjectUtility subjectUtil;
    protected WsResourceUtility resourceUtil;

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
        resourceUtil = new WsResourceUtility(service);
    }

}
