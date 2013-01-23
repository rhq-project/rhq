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


import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;

import org.apache.http.HttpStatus;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.with;
import static org.hamcrest.CoreMatchers.containsString;

/**
 * Test the resources part
 * @author Heiko W. Rupp
 */
public class ResourcesTest extends AbstractBase {

    @Test
    public void testPlatformsPresent() {
        expect()
            .statusCode(200)
            .body("links[0].rel", CoreMatchers.hasItem("self"))
        .when()
            .get("/resource/platforms.json");
    }

    @Test
    public void testGetPlatformJson() {

        given()
            .header("Accept","application/json")
        .expect()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("links.rel", CoreMatchers.hasItem("self"))
        .when()
            .get("/resource/10001");

    }

    @Test
    public void testResourceQuery() throws Exception {
        String json = get("/resource/platforms.json").asString();
        String platformName = JsonPath.with(json).get("[0].resourceName");

        given()
            .header("Accept","application/json")
        .with()
            .queryParam("q",platformName)
            .queryParam("category","platform")
        .expect()
            .statusCode(200)
            .body("links[0].rel", CoreMatchers.hasItem("self"))
        .when()
            .get("/resource");
    }

    @Test
    public void testResourceQueryCategory() throws Exception {

        with()
            .queryParam("category","PlAtForM")
        .expect()
            .statusCode(200)
        .when()
            .get("/resource");


        with()
            .queryParam("category","SeRvEr")
        .expect()
            .statusCode(200)
        .when()
            .get("/resource");


        with()
            .queryParam("category", "seRVice")
        .expect()
            .statusCode(200)
        .when()
            .get("/resource");
    }

    @Test
    public void testPaging() throws Exception {

        given()
            .header("Accept", "application/json")
        .with()
            .queryParam("page", 1)
            .queryParam("ps", 2)  // Unusually small to provoke having more than 1 page
            .queryParam("category", "service")
        .expect()
            .statusCode(200)
            .header("Link", containsString("page=2"))
            .body("links[0].rel", CoreMatchers.hasItem("self"))
        .when().get("/resource");
    }

    @Test
    public void testGetPlatformXml() {
        given()
            .header("Accept","application/xml")
        .expect()
            .statusCode(200)
            .contentType(ContentType.XML)
        .when()
            .get("/resource/10001");
    }

    @Test
    public void testGetPlatformSchedules() {
        given()
            .header("Accept","application/json")
        .expect()
            .statusCode(200)
        .when()
            .get("/resource/10001/schedules");
    }

    @Test
    public void testGetPlatformChildren() {
        given()
            .header("Accept","application/json")
        .expect()
            .statusCode(200)
        .when()
            .get("/resource/10001/children");
    }

    @Test
    public void testCreatePlatform() throws Exception {

        given().body("{\"value\":\"Linux\"}")
                .header("Content-Type","application/json")
                .header("Accept","application/json")
            .expect().statusCode(201)
            .when().post("/resource/platform/api-test-dummy");
    }

    @Test
    public void testCreatePlatformAndRemove() throws Exception {

        Response response =
            with().body("{\"value\":\"Linux\"}")
                .header("Content-Type","application/json")
                .header("Accept","application/json")
            .expect().statusCode(201)
            .when().post("/resource/platform/api-test-dummy").andReturn();

        String platformId = response.jsonPath().getString("resourceId");

        given().pathParam("id",platformId)
            .expect().statusCode(HttpStatus.SC_NO_CONTENT)
            .when().delete("/resource/{id}");
    }

    @Test
    public void testCreatePlatformWithChildAndRemove() throws Exception {

        Response response =
            with().body("{\"value\":\"Linux\"}")
                .header("Content-Type","application/json")
                .header("Accept","application/json")
            .expect()
                .statusCode(201)
            .when()
                .post("/resource/platform/api-test-dummy");

        String platformId = response.jsonPath().getString("resourceId");

        try {
            Response child =
                with().body("{\"value\":\"CPU\"}") // Type of new resource
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .pathParam("name", "test")
                    .queryParam("plugin", "Platforms")
                    .queryParam("parentId", platformId)
                .expect()
                        .statusCode(201)
                        .log().ifError()
                .when().post("/resource/{name}").andReturn();
        }
        finally {
            given().pathParam("id",platformId)
                .expect().statusCode(HttpStatus.SC_NO_CONTENT)
                .when().delete("/resource/{id}");
        }
    }

    @Test
    public void testDoubleChildCreate() throws Exception {
        // a resource can be created again and again


        Response response =
            with().body("{\"value\":\"Linux\"}")
                .header("Content-Type","application/json")
                .header("Accept","application/json")
            .expect()
                .statusCode(201)
            .when()
                .post("/resource/platform/api-test-dummy");

        String platformId = response.jsonPath().getString("resourceId");

        try {
            Response child =
                with().body("{\"value\":\"CPU\"}") // Type of new resource
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .pathParam("name", "test")
                    .queryParam("plugin", "Platforms")
                    .queryParam("parentId", platformId)
                .expect()
                        .statusCode(201)
                        .log().ifError()
                .when().post("/resource/{name}").andReturn();

            child =
                with().body("{\"value\":\"CPU\"}") // Type of new resource
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .pathParam("name", "test")
                    .queryParam("plugin", "Platforms")
                    .queryParam("parentId", platformId)
                .expect()
                        .statusCode(201)
                        .log().ifError()
                .when().post("/resource/{name}").andReturn();
        }
        finally {
            given().pathParam("id",platformId)
                .expect().statusCode(HttpStatus.SC_NO_CONTENT)
                .when().delete("/resource/{id}");
        }

    }

    @Test
    public void testAlertsForResource() throws Exception {
        given()
                .header("Accept","application/json")
            .expect().statusCode(200)
            .when().get("/resource/10001/alerts");
    }

    @Test
    public void testSchedulesForResource() throws Exception {
        given()
                .header("Accept","application/json")
            .expect().statusCode(200)
            .when().get("/resource/10001/schedules");
    }

    @Test
    public void testAvailabilityForResource() throws Exception {
        given()
                .header("Accept", "application/json")
            .expect().statusCode(200)
            .when().get("/resource/10001/availability");
    }

    @Test
    public void testAvailabilityHistoryForResource() throws Exception {
        given()
                .header("Accept", "application/json")
            .expect().statusCode(200)
            .when().get("/resource/10001/availability/history");
    }

    @Test
    public void testUpdateAvailability() throws Exception {

        Response response = given()
            .header("Accept", "application/json")
            .expect().statusCode(200)
            .when().get("/resource/10001/availability");

        String oldType = response.jsonPath().get("type");

        try {
            long now = System.currentTimeMillis()-100;
            given().body("{\"since\":" + now + ",\"type\":\"DOWN\",\"resourceId\":10001}")
                    .header("Content-Type","application/json")
                    .header("Accept","application/json")
                    .pathParam("id",10001)
            .expect()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .log().ifError()
            .when().put("/resource/{id}/availability");

            response = given()
                .header("Accept", "application/json")
                .expect().statusCode(200)
                .when().get("/resource/10001/availability");

            String currentType = response.jsonPath().get("type");
            assert currentType.equals("DOWN"); // TODO small window where an agent may have sent an update
        } finally {

            // Set back to original value
            long now = System.currentTimeMillis()-100;
            given().body("{\"since\":" + now + ",\"type\":\""+oldType+"\",\"resourceId\":10001}")
                    .header("Content-Type","application/json")
                    .header("Accept","application/json")
                    .pathParam("id",10001)
            .expect()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .log().ifError()
            .when().put("/resource/{id}/availability");

        }
    }

    @Test
    public void testNoDisabledForPlatforms() throws Exception {

        // Platforms should not be set to DISABLED according ot JSHAUGHN

        long now = System.currentTimeMillis()-100;
        given().body("{\"since\":" + now + ",\"type\":\"DISABLED\",\"resourceId\":10001}")
              .header("Content-Type","application/json")
              .header("Accept","application/json")
              .pathParam("id",10001)
        .expect()
              .statusCode(HttpStatus.SC_NOT_ACCEPTABLE)
              .log().ifError()
        .when().put("/resource/{id}/availability");

    }
}
