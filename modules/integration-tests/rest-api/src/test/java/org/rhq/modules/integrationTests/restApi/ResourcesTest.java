/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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



import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.path.xml.XmlPath;
import com.jayway.restassured.path.xml.element.Node;
import com.jayway.restassured.response.Response;

import org.apache.http.HttpStatus;
import org.junit.Test;

import org.rhq.modules.integrationTests.restApi.d.Availability;
import org.rhq.modules.integrationTests.restApi.d.Resource;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.with;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.not;

/**
 * Test the resources part
 * @author Heiko W. Rupp
 */
public class ResourcesTest extends AbstractBase {

    @Test
    public void testPlatformsPresent() {
        expect()
            .statusCode(200)
            .body("links.self", notNullValue())
        .when()
            .get("/resource/platforms.json");
    }

    @Test
    public void testGetPlatformJson() {

        given()
            .header("Accept","application/json")
            .pathParam("id",_platformId)
        .expect()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .log().everything()
            .body("links.self", notNullValue())
            .body("resourceId", is(_platformId))
            .body("typeId", is(_platformTypeId))
            .body("parentId",is(0))
        .when()
            .get("/resource/{id}");

    }

    @Test
    public void testGetPlatformJsonWrapping() {

        // Actually this object should not be wrapped
        // as it is no list
        given()
            .header(acceptWrappedJson)
            .pathParam("id",_platformId)
        .expect()
            .statusCode(200)
            .contentType(WRAPPED_JSON)
            .log().everything()
            .body("links.self", notNullValue())
            .body("resourceId", is(_platformId))
            .body("typeId", is(_platformTypeId))
            .body("parentId",is(0))
        .when()
            .get("/resource/{id}");

    }

    @Test
    public void testGetPlatformAndTypeJson() {

        Integer typeId =
        given()
            .header("Accept","application/json")
            .pathParam("id",_platformId)
        .expect()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .log().ifError()
            .body("links.self", notNullValue())
        .when()
            .get("/resource/{id}")
        .jsonPath().getInt("typeId");

        assert typeId!=null;
        assert typeId>0;

        given()
            .header(acceptJson)
            .pathParam("typeId",typeId)
            .log().everything()
        .expect()
            .statusCode(200)
            .body("id", is(typeId))
            .body("name", is("Linux"))
            .body("pluginName", is("Platforms"))
            .log().everything()
        .when()
            .get("/resource/type/{typeId}");

    }

    @Test
    public void testGetPlatformUILink() {

        given()
            .header(acceptJson)
            .pathParam("id", _platformId)
        .expect()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .log().ifError()
            .body("links.coregui.href[0]", containsString("coregui/#Resource/" + _platformId))
        .when()
            .get("/resource/{id}");

    }

    @Test
    public void testResourceQuery() throws Exception {
        String json = get("/resource/platforms.json").asString();
        String platformName = JsonPath.with(json).get("[0].resourceName");

        given()
            .header("Accept", "application/json")
        .with()
            .queryParam("q", platformName)
            .queryParam("category", "platform")
        .expect()
            .statusCode(200)
            .body("links.self", notNullValue())
        .when()
            .get("/resource");
    }

    @Test
    public void testResourceQueryAllStatus() throws Exception {
        String json = get("/resource/platforms.json").asString();
        String platformName = JsonPath.with(json).get("[0].resourceName");

        given()
            .header("Accept", "application/json")
        .with()
            .queryParam("q", platformName)
            .queryParam("status","all")
            .queryParam("category", "platform")
        .expect()
            .statusCode(200)
            .body("links.self", notNullValue())
        .when()
            .get("/resource");
    }

    @Test
    public void testResourceQueryCommittedStatus() throws Exception {
        String json = get("/resource/platforms.json").asString();
        String platformName = JsonPath.with(json).get("[0].resourceName");

        given()
            .header("Accept", "application/json")
        .with()
            .queryParam("q", platformName)
            .queryParam("status","committed")
            .queryParam("category", "platform")
        .expect()
            .statusCode(200)
            .body("links.self", notNullValue())
        .when()
            .get("/resource");
    }

    @Test
    public void testResourceQueryNewStatus() throws Exception {

        // Unfortunately we can not assume that there are
        // any resources in other states than COMMITTED
        given()
            .header("Accept", "application/json")
        .with()
            .queryParam("status", "NeW")
        .expect()
            .statusCode(200)
        .when()
            .get("/resource");
    }

    @Test
    public void testResourceQueryBadStatus() throws Exception {
        String json = get("/resource/platforms.json").asString();
        String platformName = JsonPath.with(json).get("[0].resourceName");

        given()
            .header("Accept", "application/json")
        .with()
            .queryParam("q", platformName)
            .queryParam("status", "Frobnitz")
            .queryParam("category", "platform")
        .expect()
            .statusCode(406)
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
    public void testGetResourcesWithPaging() throws Exception {

        given()
            .header("Accept", "application/json")
        .with()
            .queryParam("page", 1)
            .queryParam("ps", 2)  // Unusually small to provoke having more than 1 page
            .queryParam("category", "service")
        .expect()
            .statusCode(200)
            .log().everything()
           // .header("Link", allOf(containsString("page=2"), containsString("current")))
            .header("Link", not(containsString("prev")))
            .body("links.self", notNullValue())
        .when()
            .get("/resource");
    }

    @Test
    public void testGetResourcesWithPagingAndUniquenessCheck() throws Exception {

        int currentPage = 0;
        Set<Integer> seen = new HashSet<Integer>();

        for(;;) {
            JsonPath path =
            given()
                .header("Accept", "application/vnd.rhq.wrapped+json")
            .with()
                .queryParam("page", currentPage)
                .queryParam("ps", 5)  // Unusually small to provoke having more than 1 page
                .queryParam("status","COMMITTED")
            .expect()
                .statusCode(200)
                .log().ifError()
            .when()
                .get("/resource")
            .jsonPath();

            List<Integer> ids = path.getList("data.resourceId");

            for (Integer id : ids ) {
                assert !seen.contains(id);
                seen.add(id);
            }

            currentPage++;
            if (currentPage > path.getInt("lastPage")) {
                break;
            }
            System.out.print("+");
        }
        System.out.println();
    }

    @Test
    public void testGetResourcesWithPagingAndWrapping() throws Exception {

        given()
            .header("Accept", "application/vnd.rhq.wrapped+json")
        .with()
            .queryParam("page", 1)
            .queryParam("ps", 2)  // Unusually small to provoke having more than 1 page
            .queryParam("category", "service")
        .expect()
            .statusCode(200)
            .log().everything()
            .body("pageSize",is(2))
            .body("currentPage",is(1))
        .when()
            .get("/resource");
    }

    @Test
    public void testGetResourcesWithPagingAndWrappingByExtension() throws Exception {

        given()
            .queryParam("page", 1)
            .queryParam("ps", 2)  // Unusually small to provoke having more than 1 page
            .queryParam("category", "service")
        .expect()
            .statusCode(200)
            .log().everything()
            .body("pageSize",is(2))
            .body("currentPage",is(1))
        .when()
            .get("/resource.jsonw");
    }

    @Test
    public void testGetPlatformsWithPaging() throws Exception {

        given()
            .header("Accept", "application/json")
        .with()
            .queryParam("page", 0)
            .queryParam("ps", 5)
        .expect()
            .statusCode(200)
            .log().ifError()
            .body("links.self", notNullValue())
            .header("Link", not(containsString("prev=")))
            .header("Link", anyOf(containsString("current"), containsString("last")))
        .when().get("/resource/platforms");
    }

    @Test
    public void testGetPlatformXml() {

        assert _platformId!=0 : "Setup did not run or was no success";

        given()
            .header("Accept", "application/xml")
            .pathParam("id", _platformId)
        .expect()
            .statusCode(200)
            .contentType(ContentType.XML)
        .when()
            .get("/resource/{id}");
    }

    @Test
    public void testGetPlatformSchedules() {
        given()
            .header("Accept", "application/json")
            .pathParam("id", _platformId)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/resource/{id}/schedules");
    }

    @Test
    public void testGetPlatformChildren() {
        given()
            .header("Accept", "application/json")
            .pathParam("id", _platformId)
        .expect()
            .statusCode(200)
        .when()
            .get("/resource/{id}/children");
    }

    @Test
    public void testCreatePlatformOld() throws Exception {

        given().body("{\"value\":\"Linux\"}")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
            .expect().statusCode(201)
            .when().post("/resource/platform/api-test-dummy");
    }

    @Test
    public void testCreatePlatform() throws Exception {

        Resource resource = new Resource();
        resource.setResourceName("dummy-test");
        resource.setTypeName("Linux");

        given()
            .header(acceptXml)
            .contentType(ContentType.JSON)
            .body(resource)
        .expect()
            .statusCode(201)
            .log().ifError()
        .when()
            .post("/resource/platforms");

    }

    @Test
    public void testCreatePlatformJson() throws Exception {

        Resource resource = new Resource();
        resource.setResourceName("dummy-test");
        resource.setTypeName("Linux");

        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .body(resource)
        .expect()
            .statusCode(201)
            .log().everything()
            .body("resourceId",instanceOf(Number.class))
        .when()
            .post("/resource/platforms");

    }

    @Test
    public void testCreatePlatformWithBadType() throws Exception {

        Resource resource = new Resource();
        resource.setResourceName("dummy-test");
        resource.setTypeName("myGreatestOS");

        given()
            .header(acceptXml)
            .contentType(ContentType.JSON)
            .body(resource)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .post("/resource/platforms");

    }

    @Test
    public void testCreatePlatformAndRemove() throws Exception {

        Resource resource = new Resource();
        resource.setResourceName("dummy-test");
        resource.setTypeName("Linux");

        Response response =
        given()
            .header(acceptXml)
            .contentType(ContentType.JSON)
            .body(resource)
        .expect()
            .statusCode(201)
            .log().ifError()
        .when()
            .post("/resource/platforms");

        XmlPath xmlPath = response.xmlPath();
        Node resource1 = xmlPath.get("resource");
        Node platformIdNode =  resource1.get("resourceId");
        String platformId = platformIdNode.value();

        given().pathParam("id", platformId)
            .expect().statusCode(HttpStatus.SC_NO_CONTENT)
            .when().delete("/resource/{id}");

    }

    @Test
    public void testCreatePlatformUpdateAvailabilityAndRemove() throws Exception {

        Resource resource = new Resource();
        resource.setResourceName("dummy-test");
        resource.setTypeName("Linux");

        Response response =
        given()
            .header(acceptXml)
            .contentType(ContentType.JSON)
            .body(resource)
        .expect()
            .statusCode(201)
            .log().ifError()
        .when()
            .post("/resource/platforms");

        XmlPath xmlPath = response.xmlPath();
        Node resource1 = xmlPath.get("resource");
        Node platformIdNode =  resource1.get("resourceId");
        String platformId = platformIdNode.value();

        try {
            long now = System.currentTimeMillis()-100;
            given().body("{\"since\":" + now + ",\"type\":\"DOWN\",\"resourceId\":" + platformId + "}")
                    .header("Content-Type","application/json")
                    .header("Accept","application/json")
                    .pathParam("id",platformId)
            .expect()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .log().ifError()
            .when().put("/resource/{id}/availability");

            now += 50;
            given().body("{\"since\":" + now + ",\"type\":\"UP\",\"resourceId\":" + platformId + "}")
                    .header("Content-Type","application/json")
                    .header("Accept","application/json")
                    .pathParam("id",platformId)
            .expect()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .log().ifError()
            .when().put("/resource/{id}/availability");


        }

        finally {
            given().pathParam("id", platformId)
                .expect().statusCode(HttpStatus.SC_NO_CONTENT)
                .when().delete("/resource/{id}");
        }

    }

    @Test
    public void testCreatePlatformOLDAndRemove() throws Exception {

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
    public void testCreatePlatformOLDWithChildOLDAndRemove() throws Exception {

        Response response =
            with().body("{\"value\":\"Linux\"}")
                .header("Content-Type","application/json")
                .header("Accept", "application/json")
            .expect()
                .statusCode(201)
            .when()
                .post("/resource/platform/api-test-dummy");

        String platformId = response.jsonPath().getString("resourceId");

        try {
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
    public void testCreatePlatformWithChildAndRemove() throws Exception {

        Resource platform = new Resource();
        platform.setResourceName("dummy-test");
        platform.setTypeName("Linux");

        Response response =
            with().body(platform)
                .header("Content-Type","application/json")
                .header("Accept","application/json")
            .expect()
                .statusCode(201)
                .log().ifError()
            .when()
                .post("/resource/platforms");

        String platformId = response.jsonPath().getString("resourceId");

        Resource child = new Resource();
        child.setResourceName("test");
        child.setTypeName("CPU");
        child.setPluginName("Platforms");
        child.setParentId(Integer.valueOf(platformId));

        try {

            with()
                .body(child)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
            .expect()
                .statusCode(201)
                .log().ifError()
            .when()
                .post("/resource");
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
            with().body("{\"value\":\"CPU\"}") // Type of new resource
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .pathParam("name", "test")
                .queryParam("plugin", "Platforms")
                .queryParam("parentId", platformId)
            .expect()
                    .statusCode(201)
                    .log().ifError()
            .when().post("/resource/{name}");

            with().body("{\"value\":\"CPU\"}") // Type of new resource
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .pathParam("name", "test")
                .queryParam("plugin", "Platforms")
                .queryParam("parentId", platformId)
            .expect()
                    .statusCode(201)
                    .log().ifError()
            .when().post("/resource/{name}");
        }
        finally {
            given().pathParam("id",platformId)
                .expect().statusCode(HttpStatus.SC_NO_CONTENT)
                .when().delete("/resource/{id}");
        }

    }

    @Test
    public void testCreateChildForUnknownParent() throws Exception {

        given()
            .body("{\"value\":\"CPU\"}") // Type of new resource
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .pathParam("name", "test")
            .queryParam("plugin", "Platforms")
            .queryParam("parentId", 321)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when().post("/resource/{name}");
    }

    @Test
    public void testAlertsForResource() throws Exception {
        given()
            .header("Accept", "application/json")
            .pathParam("id", _platformId)
        .expect()
            .statusCode(200)
        .when()
            .get("/resource/{id}/alerts");
    }

    @Test
    public void testAlertsForUnknownResource() throws Exception {
        given()
            .header("Accept", "application/json")
            .pathParam("id", 12345)
        .expect()
            .statusCode(404)
        .when()
            .get("/resource/{id}/alerts");
    }

    @Test
    public void testSchedulesForResource() throws Exception {
        given()
            .header("Accept", "application/json")
            .pathParam("id", _platformId)
        .expect()
            .statusCode(200)
        .when()
            .get("/resource/{id}/schedules");
    }

    @Test
    public void testSchedulesForUnknownResource() throws Exception {
        given()
            .header("Accept", "application/json")
            .pathParam("id", 123)
        .expect()
            .statusCode(404)
        .when()
            .get("/resource/{id}/schedules");
    }

    @Test
    public void testAvailabilityForResourceJson() throws Exception {
        given()
            .header(acceptJson)
            .pathParam("id", _platformId)
        .expect()
            .statusCode(200)
        .when()
            .get("/resource/{id}/availability");
    }

    @Test
    public void testAvailabilityForUnknownResource() throws Exception {
        given()
            .header(acceptJson)
            .pathParam("id", 532)
        .expect()
            .statusCode(404)
        .when()
            .get("/resource/{id}/availability");
    }

    @Test
    public void testAvailabilityForResourceXml() throws Exception {
        given()
            .header(acceptXml)
            .pathParam("id", _platformId)
        .expect()
            .statusCode(200)
        .when()
            .get("/resource/{id}/availability");
    }

    @Test
    public void testAvailabilityHistoryForResourceJson() throws Exception {
        given()
            .header(acceptJson)
            .pathParam("id", _platformId)
        .expect()
            .statusCode(200)
        .when()
            .get("/resource/{id}/availability/history");
    }

    @Test
    public void testAvailabilityHistoryForUnknownResource() throws Exception {
        given()
            .header(acceptJson)
            .pathParam("id", -42)
        .expect()
            .statusCode(404)
        .when()
            .get("/resource/{id}/availability/history");
    }

    @Test
    public void testAvailabilityHistoryForResourceXml() throws Exception {
        given()
            .header(acceptXml)
            .pathParam("id", _platformId)
        .expect()
            .statusCode(200)
        .when()
            .get("/resource/{id}/availability/history");
    }

    @Test
    public void testAvailabilitySummaryForResourceJson() throws Exception {
        given()
            .header(acceptJson)
            .pathParam("id", _platformId)
        .expect()
            .statusCode(200)
            .log().everything()
            .body("currentTime", instanceOf(Long.class))
            .body("failures", instanceOf(Integer.class))
            .body("current", containsString("UNKNOWN"))
            .body("upPercentage", instanceOf(Float.class))
        .when()
            .get("/resource/{id}/availability/summary");
    }

    @Test
    public void testAvailabilitySummaryForResourceXml() throws Exception {

        given()
            .header(acceptXml)
            .pathParam("id", _platformId)
        .expect()
            .statusCode(200)
            .log().ifError()
            .body("availabilitySummary.current", containsString("UNKNOWN"))
        .when()
            .get("/resource/{id}/availability/summary");

    }

    @Test
    public void testUpdateAvailability() throws Exception {

        Response response =
        given()
            .header("Accept", "application/json")
            .pathParam("id", _platformId)
        .expect()
            .statusCode(200)
        .when()
            .get("/resource/{id}/availability");

        String oldType = response.jsonPath().get("type");

        try {
            long now = System.currentTimeMillis()-100;
            given()
                .body("{\"since\":" + now + ",\"type\":\"DOWN\",\"resourceId\":" + _platformId + "}")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .pathParam("id", _platformId)
            .expect()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .log().ifError()
            .when()
                .put("/resource/{id}/availability");

            response = given()
                .header("Accept", "application/json")
                .pathParam("id", _platformId)
            .expect()
                .statusCode(200)
            .when()
                .get("/resource/{id}/availability");

            String currentType = response.jsonPath().get("type");
            assert currentType.equals("DOWN");
        } finally {

            // Set back to original value
            long now = System.currentTimeMillis()-100;
            given()
                .body("{\"since\":" + now + ",\"type\":\"" + oldType + "\",\"resourceId\":" + _platformId + "}")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .pathParam("id", _platformId)
            .expect()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .log().ifError()
            .when().put("/resource/{id}/availability");

        }
    }

    @Test
    public void testNoDisabledForPlatforms() throws Exception {

        // Platforms should not be set to DISABLED according to JSHAUGHN

        long now = System.currentTimeMillis()-100;
        Availability avail = new Availability(_platformId,now,"DISABLED");

        given()
            .body(avail)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .pathParam("id", _platformId)
        .expect()
            .statusCode(HttpStatus.SC_NOT_ACCEPTABLE)
            .log().ifError()
        .when()
            .put("/resource/{id}/availability");

    }

    @Test
    public void testGetUnknownType() throws Exception {


        given()
            .header(acceptJson)
            .pathParam("typeId",123)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .get("/resource/type/{typeId}");

    }

    @Test
    public void testUnknownCreateResourceStatusId() throws Exception {

        given()
            .pathParam("id",123)
        .expect()
            .statusCode(404)
        .when()
            .get("/resource/creationStatus/{id}");


    }
}
