/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.modules.integrationTests.restApi;

import com.eclipsesource.restfuse.AuthenticationType;
import com.eclipsesource.restfuse.Destination;
import com.eclipsesource.restfuse.HttpJUnitRunner;
import com.eclipsesource.restfuse.Method;
import com.eclipsesource.restfuse.Response;
import com.eclipsesource.restfuse.annotation.Authentication;
import com.eclipsesource.restfuse.annotation.Context;
import com.eclipsesource.restfuse.annotation.Header;
import com.eclipsesource.restfuse.annotation.HttpTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertTrue;

/**
 * Test the resources part
 * @author Heiko W. Rupp
 */
@RunWith(HttpJUnitRunner.class)
public class ResourcesTest {

    @Rule
    public Destination destination = new Destination("http://" + System.getProperty("rest.server","localhost") + ":7080/rest");

    @Context
    private Response response;


    @HttpTest( method = Method.GET, path = "/resource/platforms" )
    public void testAuthRequired() {
        com.eclipsesource.restfuse.Assert.assertUnauthorized(response);
    }

    @HttpTest( method = Method.GET, path = "/resource/platforms",authentications =
        @Authentication(type = AuthenticationType.BASIC, user = "rhqadmin", password = "rhqadmin")
    )
    public void testPlatformsPresent() {
        com.eclipsesource.restfuse.Assert.assertOk(response);
        assertTrue(response.hasBody());
    }

    @HttpTest( method = Method.GET, path = "/resource/10001",authentications =
        @Authentication(type = AuthenticationType.BASIC, user = "rhqadmin", password = "rhqadmin"),
            headers = {@Header(name = "Accept",value ="application/json")}
    )
    public void testGetPlatformJson() {
        com.eclipsesource.restfuse.Assert.assertOk(response);
        assertTrue(response.hasBody());
    }

    @HttpTest( method = Method.GET, path = "/resource/10001",authentications =
        @Authentication(type = AuthenticationType.BASIC, user = "rhqadmin", password = "rhqadmin"),
            headers = {@Header(name = "Accept",value ="application/xml")}
    )
    public void testGetPlatformXml() {
        com.eclipsesource.restfuse.Assert.assertOk(response);
        assertTrue(response.hasBody());
    }

    @HttpTest( method = Method.GET, path = "/resource/10001/schedules",authentications =
        @Authentication(type = AuthenticationType.BASIC, user = "rhqadmin", password = "rhqadmin"),
            headers = {@Header(name = "Accept",value ="application/json")}
    )
    public void testGetPlatformSchedules() {
        com.eclipsesource.restfuse.Assert.assertOk(response);
        assertTrue(response.hasBody());
    }

    @HttpTest( method = Method.GET, path = "/resource/10001/children",authentications =
        @Authentication(type = AuthenticationType.BASIC, user = "rhqadmin", password = "rhqadmin"),
            headers = {@Header(name = "Accept",value ="application/json")}
    )
    public void testGetPlatformChildren() {
        com.eclipsesource.restfuse.Assert.assertOk(response);
        assertTrue(response.hasBody());
    }

}
