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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;

import org.junit.Before;
import org.junit.Test;

import org.rhq.modules.integrationTests.restApi.d.Baseline;
import org.rhq.modules.integrationTests.restApi.d.MDataPoint;
import org.rhq.modules.integrationTests.restApi.d.Schedule;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

/**
 * Test stuff related to metrics
 * @author Heiko W. Rupp
 */
public class MetricsTest extends AbstractBase {

    private int numericScheduleId;
    private long defaultInterval;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Determine a schedule id for the common cases
        Response r =
        given()
            .header(acceptJson)
            .queryParam("type", "metric")
            .pathParam("rid",_platformId)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/resource/{rid}/schedules");

        JsonPath jp = r.jsonPath();
        numericScheduleId = jp.getInt("[0].scheduleId");
        defaultInterval = jp.getLong("[0].collectionInterval");
    }

    @Test
    public void testGetScheduleById() throws Exception {

        given()
            .header(acceptJson)
            .pathParam("id", numericScheduleId)
        .expect()
            .statusCode(200)
            .body("scheduleId", containsString("" + numericScheduleId))
            .log().ifError()
        .when()
            .get("/metric/schedule/{id}");

    }

    @Test
    public void testPutGetRawData() throws Exception {

        long now = System.currentTimeMillis();

        MDataPoint dataPoint = new MDataPoint();
        dataPoint.setScheduleId(numericScheduleId);
        dataPoint.setTimeStamp(now);
        dataPoint.setValue(1.5);

        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .pathParam("id", numericScheduleId)
            .pathParam("timestamp",now)
            .body(dataPoint)
        .expect()
            .statusCode(201)
            .log().ifError()
        .when()
            .put("/metric/data/{id}/raw/{timestamp}");

        Response response =
        given()
            .header(acceptJson)
            .pathParam("id", numericScheduleId)
            .queryParam("startTime",now - 10)
            .queryParam("endTime",now + 10)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/metric/data/{id}/raw");

        List<Map<String,Object>> list = response.as(List.class);
        assert  list.size()>0;

        boolean found = false;
        for (Map<String, Object> map : list) {
            MDataPoint mp = new MDataPoint(map);
            if (mp.equals(dataPoint))
                found = true;
        }
        assert found;

    }

    @Test
    public void testPostGetRawData() throws Exception {

        long now = System.currentTimeMillis();

        MDataPoint dataPoint = new MDataPoint();
        dataPoint.setScheduleId(numericScheduleId);
        dataPoint.setTimeStamp(now);
        dataPoint.setValue(1.5);
        List<MDataPoint> points = new ArrayList<MDataPoint>(1);
        points.add(dataPoint);

        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .body(points)
        .expect()
            .statusCode(204)
            .log().ifError()
        .when()
            .post("/metric/data/raw");

        Response response =
        given()
            .header(acceptJson)
            .pathParam("id", numericScheduleId)
            // no start and end time -> last 8h of data
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/metric/data/{id}/raw");

        List<Map<String,Object>> list = response.as(List.class);
        assert  list.size()>0 : "No data retrieved";

        boolean found = false;
        for (Map<String, Object> map : list) {
            MDataPoint mp = new MDataPoint(map);
            if (mp.equals(dataPoint))
                found = true;
        }
        assert found;

    }

    @Test
    public void testUpdateSchedule() throws Exception {

        Schedule schedule = new Schedule();
        schedule.setCollectionInterval(1234567);
        schedule.setEnabled(true);

        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .pathParam("id", numericScheduleId)
            .body(schedule)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .put("/metric/schedule/{id}");

        // reset to original interval

        schedule.setCollectionInterval(defaultInterval);
        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .pathParam("id", numericScheduleId)
            .body(schedule)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .put("/metric/schedule/{id}");

    }

    @Test
    public void testAddGetTrait() throws Exception {
        // Determine a trait schedule
        Response r =
        given()
            .header(acceptJson)
            .queryParam("type", "trait")
            .pathParam("rid",_platformId)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/resource/{rid}/schedules");

        JsonPath jp = r.jsonPath();
        int tsId = jp.getInt("[0].scheduleId");

        String trait = "{\"value\":\"Hello World!\" }";
        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .body(trait)
            .pathParam("id",tsId)
        .expect()
            .statusCode(200) // TODO 201 ?
            .log().ifError()
        .when()
            .put("/metric/data/{id}/trait");

        given()
            .header(acceptJson)
            .pathParam("id",tsId)
        .expect()
            .statusCode(200)
            .log().ifError()
            .body("value",is("Hello World!"))
        .when()
            .get("/metric/data/{id}/trait");

    }

    @Test
    public void testSetGetBaseline() throws Exception {

        long now = System.currentTimeMillis();

        Baseline baseline = new Baseline(0.0,2.0,1.0,now);

        given()
            .contentType(ContentType.XML)
            .header(acceptJson)
            .body(baseline)
            .pathParam("sid", numericScheduleId)
        .expect()
            .statusCode(201)
            .log().ifError()
        .when()
            .put("/metric/data/{sid}/baseline");

        Baseline result =
        given()
            .header(acceptJson)
            .pathParam("sid",numericScheduleId)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/metric/data/{sid}/baseline")
        .as(Baseline.class);

        assert result.equals(baseline);
    }

    @Test
    public void testSetBadBaseline() throws Exception {

        long now = System.currentTimeMillis();

        Baseline baseline = new Baseline(10.0,1.0,2.0,now);

        given()
            .contentType(ContentType.JSON)
            .header(acceptJson)
            .body(baseline)
            .pathParam("sid", numericScheduleId)
        .expect()
            .statusCode(406)
            .log().ifError()
        .when()
            .put("/metric/data/{sid}/baseline");
    }

    @Test
    public void testGetAggregate() throws Exception {

        long now = System.currentTimeMillis();

        Response r =
        given()
            .header(acceptJson)
            .pathParam("scheduleId", numericScheduleId)
        .expect()
            .statusCode(200)
            .body("scheduleId", is(numericScheduleId))
            .body("numDataPoints", is(60))
            .log().ifError()
        .when()
            .get("/metric/data/{scheduleId}");

        Map map = r.as(Map.class);
        List<MDataPoint> points = (List<MDataPoint>) map.get("dataPoints");
        assert points.size()==60;

    }

//    @Test Not yet - see https://bugzilla.redhat.com/show_bug.cgi?id=835647 TODO
    public void testGetAggregate120Points() throws Exception {

        long now = System.currentTimeMillis();

        Response r =
        given()
            .header(acceptJson)
            .pathParam("scheduleId", numericScheduleId)
            .queryParam("dataPoints",120)
        .expect()
            .statusCode(200)
            .body("scheduleId", is(numericScheduleId))
            .body("numDataPoints", is(120))
            .log().ifError()
        .when()
            .get("/metric/data/{scheduleId}");

        Map map = r.as(Map.class);
        List<MDataPoint> points = (List<MDataPoint>) map.get("dataPoints");
        assert points.size()==120;

    }
    @Test
    public void testGetAggregateMinusOnePoints() throws Exception {

        given()
            .header(acceptJson)
            .pathParam("scheduleId", numericScheduleId)
            .queryParam("dataPoints",-1)
        .expect()
            .statusCode(406)
        .when()
            .get("/metric/data/{scheduleId}");
    }
}
