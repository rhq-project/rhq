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


import static com.jayway.restassured.RestAssured.*;

import org.junit.Test;

/**
 * Test the status part of the API
 */
public class StatusTest extends AbstractBase{


    @Test
    public void testAuthRequired() {
        given()
            .auth().none()
        .expect()
            .statusCode(401)
        .when()
            .get("/status");
    }


    @Test
    public void testAuthRhqadmin() {
        expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/status");

        given()
            .header("Accept","text/html")
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/status");

        given().header("Accept","application/json")
        .expect().statusCode(200)
                .when().get("/status");

        given().header("Accept","application/xml")
        .expect().statusCode(200)
                .when().get("/status");

    }

    @Test
    public void testAuthRestricted() {
        given()
            .auth().basic("user","name23")
        .expect()
            .statusCode(401)
        .when()
            .get("/rest/status"); // Wrong url does not matter, as the access is checked before hitting the rest api
    }

    @Test
    public void testServerOpMode() throws Exception {

        expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/status/server");

    }
}
