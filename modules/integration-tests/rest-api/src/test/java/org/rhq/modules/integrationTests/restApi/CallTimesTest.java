/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

import static com.jayway.restassured.RestAssured.given;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;

import org.junit.Before;
import org.junit.Test;

import org.rhq.modules.integrationTests.restApi.d.CallTimeValueRest;

public class CallTimesTest extends AbstractBase {

    private int callTimeScheduleId;
    private int webRuntimeResourceId;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Determine a calltime schedule id

        // first lookup coregui.war resource
        Response r = 
            given()
                .header(acceptJson)
                .queryParam("q", "coregui.war")
            .expect()
                .statusCode(200)
                .log().ifError()
            .when()
                .get("/resource");

        JsonPath jp = r.jsonPath();

        int coreGuiId = jp.getInt("[0].resourceId");
        // now list it's children
        r = 
            given()
                .header(acceptJson)
                .pathParam("rid", coreGuiId)
            .expect()
                .statusCode(200)
                .log().ifError()
            .when()
                .get("/resource/{rid}/children");

        jp = r.jsonPath();

        webRuntimeResourceId = jp.getInt("[0].resourceId");

        // finally lookup calltime
        r = 
            given()
                .header(acceptJson)
                .queryParam("type", "calltime")
                .queryParam("enabledOnly", false)
                .pathParam("rid", webRuntimeResourceId)
            .expect()
                .statusCode(200)
                .log().ifError()
            .when()
                .get("/resource/{rid}/schedules");

        jp = r.jsonPath();
        callTimeScheduleId = jp.getInt("[0].scheduleId");

    }

    @Test
    public void putAndGetCallTimes() throws Exception {
        List<CallTimeValueRest> list = new ArrayList<CallTimeValueRest>();
        String destination1 = "/1/" + String.valueOf(System.currentTimeMillis());
        for (int i = 0; i < 100; i++) {
            CallTimeValueRest c = CallTimeValueRest.defaultCallTimeValue(destination1);
            list.add(c);
        }
        String destination2 = "/2/" + String.valueOf(System.currentTimeMillis());
        for (int i = 0; i < 99; i++) {
            CallTimeValueRest c = CallTimeValueRest.defaultCallTimeValue(destination2);
            list.add(c);
        }

        Response r = 
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .pathParam("id", callTimeScheduleId)
                .body(list)
            .expect()
                .statusCode(201)
                .log().ifError()
            .when()
                .put("/metric/data/{id}/callTime");

        String location = r.getHeader("Location");

        assert location != null : "Location header was not found in server's response";

        r = 
            given()
                .header(acceptJson)
            .expect()
                .statusCode(200)
                .log().body()
            .when()
                .get(location);

        List<Map<String, Object>> result = r.as(List.class);
        boolean found1 = false;
        boolean found2 = false;
        for (Map<String, Object> map : result) {
            String dest = (String) map.get("callDestination");
            if (destination1.equals(dest)) {
                found1 = true;
                int count = (Integer) map.get("count");
                double total = (Double) map.get("total");
                assert count == 100 : "we've pushed 100 calltime values, but server retuns " + count;
                assert total == 100 * 1000L : "expected total was 100000, but server returned " + total;
            }
            if (destination2.equals(dest)) {
                found2 = true;
                int count = (Integer) map.get("count");
                double total = (Double) map.get("total");
                assert count == 99 : "we've pushed 100 calltime values, but server retuns " + count;
                assert total == 99 * 1000L : "expected total was 990000, but server returned " + total;
            }

        }
        assert found1 && found2 : "we just created callTime data, but server did not return it";
    }

    @Test
    public void putAndGetRawCallTimes() throws Exception {
        List<CallTimeValueRest> list = new ArrayList<CallTimeValueRest>();
        long now = System.currentTimeMillis();
        String destination = "/aggr/" + String.valueOf(now);

        // send 2 buckets of callTime values to the same destination

        for (int i = 0; i < 100; i++) {
            CallTimeValueRest c = CallTimeValueRest.defaultCallTimeValue(destination);
            list.add(c);
        }

        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .pathParam("id", callTimeScheduleId)
            .body(list)
        .expect()
            .statusCode(201)
            .log().ifError()
        .when()
            .put("/metric/data/{id}/callTime");

        list.clear();

        for (int i = 0; i < 100; i++) {
            CallTimeValueRest c = CallTimeValueRest.defaultCallTimeValue(destination);
            list.add(c);
        }

        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .pathParam("id", callTimeScheduleId)
            .body(list)
        .expect()
            .statusCode(201)
            .log().ifError()
        .when()
            .put("/metric/data/{id}/callTime");

        // first get it aggregated
        Response r = 
            given()
                .header(acceptJson)
                .pathParam("id", callTimeScheduleId)
                .queryParam("endTime", now + 2000)
                .queryParam("aggregate", true)
            .expect()
                .statusCode(200)
                .log().body()
            .when()
                .get("/metric/data/{id}/callTime");

        List<Map<String, Object>> result = r.as(List.class);
        boolean found1 = false;
        for (Map<String, Object> map : result) {
            String dest = (String) map.get("callDestination");
            if (destination.equals(dest)) {
                found1 = true;
                int count = (Integer) map.get("count");
                double total = (Double) map.get("total");
                assert count == 200 : "we've pushed 2 * 100 calltime values, but server retuns " + count;
                assert total == 200 * 1000L : "expected total was 200000, but server returned " + total;
            }

        }
        assert found1 : "we just created callTime data, but server did not return it";

        // lets get raw metrics now

        r = 
            given()
                .header(acceptJson)
                .pathParam("id", callTimeScheduleId)
                .queryParam("endTime", now + 2000)
                .queryParam("aggregate", false)
            .expect()
                .statusCode(200).log().body()
            .when()
                .get("/metric/data/{id}/callTime");

        result = r.as(List.class);
        found1 = false;
        boolean found2 = false;
        for (Map<String, Object> map : result) {
            String dest = (String) map.get("callDestination");
            if (destination.equals(dest)) {
                if (found1) {
                    found2 = true;
                }
                found1 = true;
                int count = (Integer) map.get("count");
                double total = (Double) map.get("total");
                assert count == 100 : "we've pushed 100 calltime values per 1 raw report, but server retuns " + count;
                assert total == 100 * 1000L : "expected total was 100000, but server returned " + total;
            }

        }
        assert found1 && found2 : "server should have returned 2 raw records for our callDestination";
    }

    @Test
    public void putInvalidCallTimes() throws Exception {
        List<CallTimeValueRest> list = new ArrayList<CallTimeValueRest>();
        CallTimeValueRest c = CallTimeValueRest.defaultCallTimeValue("test");
        list.add(c);

        c.setCallDestination(null); // set invalid

        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .pathParam("id", callTimeScheduleId)
            .body(list)
        .expect()
            .statusCode(406)
            .log().ifError()
        .when()
            .put("/metric/data/{id}/callTime");

        c.setCallDestination("test"); // set back to valid
        c.setDuration(-1L);

        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .pathParam("id", callTimeScheduleId)
            .body(list)
        .expect()
            .statusCode(406)
            .log().ifError()
        .when()
            .put("/metric/data/{id}/callTime");

        c.setDuration(1); // set back to valid
        c.setBeginTime(1); // set invalid

        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .pathParam("id", callTimeScheduleId)
            .body(list)
        .expect()
            .statusCode(406)
            .log().ifError()
        .when()
            .put("/metric/data/{id}/callTime");

        // send empty list
        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .pathParam("id", callTimeScheduleId)
            .body(Collections.EMPTY_LIST)
        .expect().statusCode(200).log().ifError()
        .when()
            .put("/metric/data/{id}/callTime");
    }
}
