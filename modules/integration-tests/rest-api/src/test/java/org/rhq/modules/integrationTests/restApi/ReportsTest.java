/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.integrationTests.restApi;

import com.jayway.restassured.RestAssured;

import org.junit.Before;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;

/**
 * Tests for the reports that also run via the Rest-api (but different base url)
 * @author Heiko W. Rupp
 */
public class ReportsTest extends AbstractBase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        RestAssured.basePath = "/coregui/reports";
    }

//    @Test
    public void testGetAlertDefinitions() throws Exception {

        given()
            .header(acceptCsv)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/alertDefinitions");


    }
}
