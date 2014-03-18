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



import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.path.xml.XmlPath;
import com.jayway.restassured.path.xml.element.Node;
import com.jayway.restassured.response.Headers;
import com.jayway.restassured.response.Response;

import org.apache.http.HttpStatus;
import org.junit.Test;

import org.rhq.modules.integrationTests.restApi.d.Availability;
import org.rhq.modules.integrationTests.restApi.d.CreateCBRRequest;
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
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.iterableWithSize;
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
    public void testPagingWrappingCorrectness() throws Exception {

        // First get the lastPage from the paging side

        JsonPath path =
        given()
            .header("Accept", "application/vnd.rhq.wrapped+json")
        .with()
            .queryParam("page", 0)
            .queryParam("ps", 5)  // Unusually small to provoke having more than 1 page
            .queryParam("status","COMMITTED")
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/resource")
        .jsonPath();

        int pagingLastPage = path.getInt("lastPage");
        int pagingTotalSize = path.getInt("totalSize");

        // Now get resource counts from status

        JsonPath statusPath =
        given()
            .header(acceptJson)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/status")
        .jsonPath();

        int platforms = statusPath.getInt("values.PlatformCount");
        int servers = statusPath.getInt("values.ServerCount");
        int services = statusPath.getInt("values.ServiceCount");

        int resources = platforms + servers + services;

        assert resources == pagingTotalSize;

        int statusLastPage = (resources/5)-1; // Page numbers start at 0

        assert statusLastPage == pagingLastPage : statusLastPage + " != " + pagingLastPage;
    }

    @Test
    public void testPagingHeaderCorrectness() throws Exception {

        // First get the lastPage from the paging headers

        Response response =
        given()
            .header(acceptJson)
        .with()
            .queryParam("page", 0)
            .queryParam("ps", 5)  // Unusually small to provoke having more than 1 page
            .queryParam("status", "COMMITTED")
        .expect()
            .statusCode(200)
            .log().everything()
        .when()
            .get("/resource");

        String tmp = response.getHeader("X-collection-size");
        int pagingTotalSize = Integer.parseInt(tmp);

        Headers responseHeaders = response.getHeaders();
        List<String> headers = responseHeaders.getValues("Link");
        tmp = null;
        for (String header : headers) {
            if (header.contains("rel=\"last\"")) {
                tmp = header;
                break;
            }
        }
        assert tmp != null : "Found no Link header for rel=last";

        Matcher m = Pattern.compile(".*page=([0-9]+).*").matcher(tmp);
        assert m.matches();

        tmp = m.group(1);

        System.out.println(tmp);
        System.out.flush();
        int pagingLastPage = Integer.parseInt(tmp);

        // Now get resource counts from status

        JsonPath statusPath =
        given()
            .header(acceptJson)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/status")
        .jsonPath();

        int platforms = statusPath.getInt("values.PlatformCount");
        int servers = statusPath.getInt("values.ServerCount");
        int services = statusPath.getInt("values.ServiceCount");

        int resources = platforms + servers + services;

        assert resources == pagingTotalSize;

        int statusLastPage = (resources/5)-1; // Page numbers start at 0

        assert statusLastPage == pagingLastPage : statusLastPage + " != " + pagingLastPage;
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
    public void testGetResourcesWithPagingAndWrappingAndJsonP() throws Exception {

        Response response =
        given()
            .header("Accept", "application/vnd.rhq.wrapped+json")
            .queryParam("jsonp","jsonp") // Use jsonp-wrapping e.g. for JavaScript access
        .with()
            .queryParam("page", 1)
            .queryParam("ps", 2)  // Unusually small to provoke having more than 1 page
            .queryParam("category", "service")
        .expect()
            .statusCode(200)
            .log().everything()
        .when()
            .get("/resource");

        String mediaType = response.getContentType();
        assert mediaType.startsWith("application/javascript");

        // check for jsonp wrapping
        String bodyString = response.asString();
        assert bodyString.startsWith("jsonp(");
        assert bodyString.endsWith(");");

        // extract the internal json data
        String body = bodyString.substring(6,bodyString.length()-2);

        // validate
        JsonPath jsonPath = new JsonPath(body);
        assert jsonPath.getInt("pageSize") == 2;
        assert jsonPath.getInt("currentPage") == 1;
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
            .body("resourceId", instanceOf(Number.class))
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
    public void testCreateUpdateRemovePlatform() throws Exception {

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

        int platformId=0;
        try {
            XmlPath xmlPath = response.xmlPath();
            Node resource1 = xmlPath.get("resource");
            Node platformIdNode =  resource1.get("resourceId");
            platformId = Integer.parseInt(platformIdNode.value());

            // Now update the description
            resource.setDescription("li la lu");
            resource.setLocation("Datacenter 1");
            resource.setResourceName("DummY");

            given()
                .pathParam("id",platformId)
                .body(resource)
                .contentType(ContentType.JSON)
                .header(acceptJson)
            .expect()
                .statusCode(200)
                .log().ifError()
                .body("location",is("Datacenter 1"))
                .body("description",is("li la lu"))
                .body("resourceName",is("DummY"))
            .when()
                .put("/resource/{id}");

        } finally {
            given()
                .pathParam("id", platformId)
            .expect()
                .statusCode(HttpStatus.SC_NO_CONTENT)
            .when()
                .delete("/resource/{id}");
        }
    }

    @Test
    public void testCreateUpdateWithLinksRemovePlatform() throws Exception {

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

        int platformId=0;
        try {
            XmlPath xmlPath = response.xmlPath();
            Node resource1 = xmlPath.get("resource");
            Node platformIdNode =  resource1.get("resourceId");
            platformId = Integer.parseInt(platformIdNode.value());

            // Now update the description
            resource.setDescription("li la lu");
            resource.setLocation("Datacenter 1");
            resource.setResourceName("DummY");

            /* Now add links -- JSON looks like this:
            "links": [
                  {
                      "operationDefinitions": {
                          "href": "http://localhost:7080/rest/operation/definitions?resourceId=10584"
                      }
                  },
            */

            List<Map> links = new ArrayList<Map>(1);
            Map<String,Map<String,String>> map = new HashMap<String, Map<String,String>>(1);
            Map<String,String> link = new HashMap<String, String>(1);
            link.put("href","http:/abc");
            map.put("self",link);
            links.add(map);
            resource.setLinks(links);


            given()
                .pathParam("id",platformId)
                .body(resource)
                .contentType(ContentType.JSON)
                .header(acceptJson)
                .log().everything()
            .expect()
                .statusCode(200)
                .log().everything()
                .body("location",is("Datacenter 1"))
                .body("description",is("li la lu"))
                .body("resourceName",is("DummY"))
            .when()
                .put("/resource/{id}");

        } finally {
            given()
                .pathParam("id", platformId)
            .expect()
                .statusCode(HttpStatus.SC_NO_CONTENT)
            .when()
                .delete("/resource/{id}");
        }
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
    public void testCreateResourceRegularChildAndRemove() throws Exception {
        int as7Id = findIdOfARealEAP6();

        int createdResourceId=-1;

        try {
            CreateCBRRequest resource = new CreateCBRRequest();
            resource.setParentId(as7Id);
            resource.setResourceName("netinterface-rest");

            // type of the new resource
            resource.setTypeName("Network Interface");
            resource.setPluginName("JBossAS7");

            // pass resourceConfig
            resource.getResourceConfig().put("any-address", "false");
            resource.getResourceConfig().put("any-ipv4-address", "true");
            Response response =
            given()
                .body(resource) // Type of new resource
                .contentType(ContentType.JSON)
                .header(acceptJson)
                .log().everything()
            .expect()
                .statusCode(isOneOf(200, 201, 302))
                .log().everything()
            .when()
                .post("/resource");

            System.out.println("after post");
            System.out.flush();

            int status = response.getStatusCode();
            String location = response.getHeader("Location");

            if (status!=200) {
                System.out.println("\nLocation " + location + "\n\n");
                assert location!=null;
            }

            // We need to check what we got. A 302 means the deploy is still
            // in progress, so we need to wait a little longer
            while (status==302) {

                response =
                given()
                    .header(acceptJson)
                    .log().everything()
                    .redirects().follow(false)
                    .redirects().allowCircular(true)
                .expect()
                    .statusCode(isOneOf(200, 201, 302))
                    .log().everything()
                .when()
                    .get(location);

                status = response.getStatusCode();
            }

            createdResourceId = response.jsonPath().getInt("resourceId");

            System.out.flush();
            System.out.println("\n  Resource is created, resource Id = " + createdResourceId + " \n");
            System.out.flush();
            // TODO validate resource configuration once our rest api can tell us
            assert  createdResourceId != -1;

        } finally {
            // We need to wait here a little, as the machinery is not used to
            // quick create-delete-cycles
            Thread.sleep(20*1000L);

            given()
                .header(acceptJson)
                .queryParam("physical", "true") // Also remove target on the EAP instance
                .pathParam("id",createdResourceId)
                .log().everything()
            .expect()
                .log().everything()
            .when()
                .delete("/resource/{id}");
        }
    }

    /**
     * this test creates Network Interface resource child on real AS7 without passing resourceConfiguration. 
     * Resource must be created, because our REST API is smart enough to use default configuration.  
     * @throws Exception
     */
    @Test
    public void testCreateResourceRegularChildEmptyConfigAndRemove() throws Exception {
        int as7Id = findIdOfARealEAP6();

        int createdResourceId=-1;

        try {
            CreateCBRRequest resource = new CreateCBRRequest();
            resource.setParentId(as7Id);
            resource.setResourceName("netinterface-rest");

            // type of the new resource
            resource.setTypeName("Network Interface");
            resource.setPluginName("JBossAS7");

            // leave pluginConfig and resourceConfigs empty, so defaults are used
            Response response =
            given()
                .body(resource) // Type of new resource
                .contentType(ContentType.JSON)
                .header(acceptJson)
                .log().everything()
            .expect()
                .statusCode(isOneOf(200, 201, 302))
                .log().everything()
            .when()
                .post("/resource");

            System.out.println("after post");
            System.out.flush();

            int status = response.getStatusCode();
            String location = response.getHeader("Location");

            if (status!=200) {
                System.out.println("\nLocation " + location + "\n\n");
                assert location!=null;
            }

            // We need to check what we got. A 302 means the deploy is still
            // in progress, so we need to wait a little longer
            while (status==302) {

                response =
                given()
                    .header(acceptJson)
                    .log().everything()
                    .redirects().follow(false)
                    .redirects().allowCircular(true)
                .expect()
                    .statusCode(isOneOf(200, 201, 302))
                    .log().everything()
                .when()
                    .get(location);

                status = response.getStatusCode();
            }

            createdResourceId = response.jsonPath().getInt("resourceId");

            System.out.flush();
            System.out.println("\n  Resource is created, resource Id = " + createdResourceId + " \n");
            System.out.flush();

            assert  createdResourceId != -1;

        } finally {
            // We need to wait here a little, as the machinery is not used to
            // quick create-delete-cycles
            Thread.sleep(20*1000L);

            given()
                .header(acceptJson)
                .queryParam("physical", "true") // Also remove target on the EAP instance
                .pathParam("id",createdResourceId)
                .log().everything()
            .expect()
                .log().everything()
            .when()
                .delete("/resource/{id}");
        }
    }

    /**
     * a little bit more complex test. First we create a manually imported resource (ScriptServer), 
     * then we attempt to create it again (this must fail on duplicate resource error), finally
     * we uninventory
     * @throws Exception
     */
    @Test
    public void testCreateResourceManualImportAndRemove() throws Exception {
        int platformId = findIdOfARealPlatform();
        int createdResourceId=-1;

        try {
            CreateCBRRequest resource = new CreateCBRRequest();
            resource.setParentId(platformId);
            resource.setResourceName("script-server");

            // type of the new resource
            resource.setTypeName("Script Server");
            resource.setPluginName("Script");

            resource.getPluginConfig().put("executable", "/bin/ls");

            Response response =
            given()
                .body(resource) // Type of new resource
                .contentType(ContentType.JSON)
                .header(acceptJson)
                .log().everything()
            .expect()
                .statusCode(isOneOf(200, 201))
                .log().everything()
            .when()
                .post("/resource");

            System.out.println("after post");
            System.out.flush();
            // in this case, we don't get 302, because manual import is synchronous

            createdResourceId = response.jsonPath().getInt("resourceId");

            System.out.flush();
            System.out.println("\n  Resource is created, resource Id = " + createdResourceId + " \n");
            System.out.flush();

            assert  createdResourceId != -1;

        } finally {
            // We need to wait here a little, as the machinery is not used to
            // quick create-delete-cycles
            Thread.sleep(20*1000L);

            given()
                .header(acceptJson)
                .queryParam("physical", "false") // Also uninventory
                .pathParam("id",createdResourceId)
                .log().everything()
            .expect()
                .log().everything()
            .when()
                .delete("/resource/{id}");
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
    public void testTypeByNameAndPlugin() throws Exception {

        given()
            .header(acceptJson)
            .queryParam("q","CPU")
            .queryParam("plugin","Platforms")
        .expect()
            .statusCode(200)
            .log().ifError()
            .body("",iterableWithSize(1))
            .body("[0].name",is("CPU"))
            .header("X-collection-size",is("1"))
        .when()
            .get("/resource/type");
    }

    @Test
    public void testTypeByNameAndPlugin2() throws Exception {

        given()
            .header(acceptJson)
            .queryParam("q","C P U")
            .queryParam("plugin","Frobnitz")
        .expect()
            .statusCode(200)
            .log().ifError()
            .body("",iterableWithSize(0))
            .header("X-collection-size",is("0"))
        .when()
            .get("/resource/type");
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

    @Test
    public void testDeleteUnknownResource() throws Exception {

        given()
            .pathParam("id",22)
        .expect()
            .statusCode(204)
        .when()
            .delete("/resource/{id}");

    }

    @Test
    public void testDeleteUnknownResourceWithValidate() throws Exception {

        given()
            .pathParam("id",22)
            .queryParam("validate",true)
        .expect()
            .statusCode(404)
        .when()
            .delete("/resource/{id}");

    }
}
