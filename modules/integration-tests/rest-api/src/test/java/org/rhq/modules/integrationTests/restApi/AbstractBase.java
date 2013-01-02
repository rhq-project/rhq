/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.modules.integrationTests.restApi;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Header;

import org.junit.Before;

import static com.jayway.restassured.RestAssured.basic;

/**
 * Common setup for the tests
 * @author Heiko W. Rupp
 */
public abstract class AbstractBase {

    static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_XML = "application/xml";
    static Header acceptJson = new Header("Accept", APPLICATION_JSON);
    static Header acceptXml = new Header("Accept", APPLICATION_XML);

    @Before
    public void setUp() throws Exception {

        RestAssured.baseURI = "http://" + System.getProperty("rest.server","localhost")  ;
        RestAssured.port = 7080;
        RestAssured.basePath = "/rest/";
        RestAssured.authentication = basic("rhqadmin","rhqadmin");

    }
}
