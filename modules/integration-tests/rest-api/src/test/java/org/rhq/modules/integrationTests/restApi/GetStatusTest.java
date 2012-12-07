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
import com.eclipsesource.restfuse.annotation.HttpTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the status part of the API
 */
@RunWith(HttpJUnitRunner.class)
public class GetStatusTest {



    @Rule
    public Destination destination = new Destination("http://" + System.getProperty("rest.server","localhost") + ":7080/rest");

    @Context
    private Response response;


    @Test
    @HttpTest( method = Method.GET, path = "/status" )
    public void testAuthRequired() {
        com.eclipsesource.restfuse.Assert.assertUnauthorized(response);
    }


    @Test
    @HttpTest( method = Method.GET, path = "/status" ,authentications =
       @Authentication(type = AuthenticationType.BASIC, user = "rhqadmin", password = "rhqadmin")
    )
    public void testAuthRhqadmin() {
        com.eclipsesource.restfuse.Assert.assertOk(response);
    }

    @Test
    @HttpTest( method = Method.GET, path = "/status" ,authentications =
        @Authentication(type = AuthenticationType.BASIC, user = "user", password = "name23")
    )
    public void testAuthRestricted() {
        com.eclipsesource.restfuse.Assert.assertUnauthorized(response);
    }


}
