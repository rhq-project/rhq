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

import java.util.List;
import java.util.Map;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Header;

import org.junit.AfterClass;
import org.junit.Before;

import org.rhq.modules.integrationTests.restApi.d.Resource;

import static com.jayway.restassured.RestAssured.basic;
import static com.jayway.restassured.RestAssured.given;

/**
 * Common setup for the tests
 * @author Heiko W. Rupp
 */
public abstract class AbstractBase {

    static final String APPLICATION_JSON = "application/json";
    static final String WRAPPED_JSON = "application/vnd.rhq.wrapped+json";
    private static final String APPLICATION_XML = "application/xml";
    private static final String TEXT_CSV = "text/csv";
    private static final String TEXT_HTML = "text/html";
    static final String REST_TEST_DUMMY = "-rest-test-dummy-";
    static Header acceptJson = new Header("Accept", APPLICATION_JSON);
    static Header acceptXml = new Header("Accept", APPLICATION_XML);
    static Header acceptHtml = new Header("Accept", TEXT_HTML);
    static Header acceptCsv = new Header("Accept", TEXT_CSV);
    static Header acceptWrappedJson = new Header("Accept",WRAPPED_JSON);

    int _platformId ;
    int _platformTypeId;

    @Before
    public void setUp() throws Exception {

        RestAssured.baseURI = "http://" + System.getProperty("rest.server","localhost")  ;
        RestAssured.port = 7080;
        RestAssured.basePath = "/rest/";
        RestAssured.authentication = basic("rhqadmin","rhqadmin");

        Resource resource = new Resource();
        resource.setResourceName(REST_TEST_DUMMY);
        resource.setTypeName("Linux");

        Resource platform =
        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .body(resource)
        .expect()
            .statusCode(201)
            .log().ifError()
        .when()
            .post("/resource/platforms")
        .as(Resource.class);

        _platformId = platform.getResourceId();
        _platformTypeId = platform.getTypeId();

    }

    @AfterClass
    public static void tearDownClass() throws Exception {


        List res =
        given()
            .header(acceptJson)
            .queryParam("q",REST_TEST_DUMMY)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/resource")
        .as(List.class);

        if (res!=null && res.get(0)!=null) {

            Integer pid  = ((Map <String,Integer>)res.get(0)).get("resourceId");

            given()
                .pathParam("id", pid)
            .expect()
                .statusCode(204)
                .log().ifError()
            .when()
                .delete("/resource/{id}");

        }
    }
    protected int findIdOfARealEAP6() {
        // Find an EAP 6 server
        List<Map<String,Object>> resources =
        given()
            .header(acceptJson)
            .queryParam("q","EAP (")
            .queryParam("category","SERVER")
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/resource")
        .jsonPath().getList("$");

        assert resources.size() > 0 : "No real EAP6 server found in inventory";

        int as7Id = (Integer)resources.get(0).get("resourceId");
        // try to find stock EAP6 
        if (resources.size() > 1) {
          for (Map<String,Object> res : resources) {
            if (!res.get("resourceName").toString().contains("RHQ Server")) {
              as7Id = (Integer)res.get("resourceId");
              break;
            }
          }
        }
        return as7Id;
    }

    protected int findIdOfARealPlatform() {
        List res =
        given()
            .header(acceptJson)
        .expect()
            .statusCode(200)
        .when()
            .get("/resource/platforms")
        .as(List.class);

        assert res != null;
        for (Object entry : res) {
            if (entry instanceof Map) {
                Map<String,Object> map = (Map<String, Object>) entry;
                if (!map.get("resourceName").equals(REST_TEST_DUMMY)) {
                    return (Integer)map.get("resourceId");
                }
            }
        }

        assert true : "No real platform found in inventory. Please add one";

        return 0;
    }
}
