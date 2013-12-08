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

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import org.rhq.modules.integrationTests.restApi.d.Baseline;
import org.rhq.modules.integrationTests.restApi.d.Datapoint;
import org.rhq.modules.integrationTests.restApi.d.DoubleValue;
import org.rhq.modules.integrationTests.restApi.d.Group;
import org.rhq.modules.integrationTests.restApi.d.MDataPoint;
import org.rhq.modules.integrationTests.restApi.d.Schedule;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.iterableWithSize;

/**
 * Test stuff related to metrics
 * @author Heiko W. Rupp
 */
public class MetricsTest extends AbstractBase {

    private static final String X_TEST_GROUP = "-metric-x-test-group";
    private int numericScheduleId;
    private int numericScheduleDefinitionId;
    private long defaultInterval;
    private String scheduleName;

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
        numericScheduleDefinitionId = jp.getInt("[0].definitionId");
        defaultInterval = jp.getLong("[0].collectionInterval");
        scheduleName = jp.getString("[0].scheduleName");
    }

    @Test
    public void testGetScheduleById() throws Exception {

        given()
            .header(acceptJson)
            .pathParam("id", numericScheduleId)
        .expect()
            .statusCode(200)
            .body("scheduleId", is(numericScheduleId))
            .header("ETag",notNullValue())
//            .body("links", hasItems("edit", "metric", "resource")) TODO
            .log().everything()
        .when()
            .get("/metric/schedule/{id}");
    }

    @Test
    public void testGetScheduleByIdAndEtag() throws Exception {

        Response response =
        given()
            .header(acceptJson)
            .pathParam("id", numericScheduleId)
        .expect()
            .statusCode(200)
            .body("scheduleId", is( numericScheduleId))
        .when()
            .get("/metric/schedule/{id}");

        String etag = response.getHeader("ETag");
        given()
            .header(acceptJson)
            .header("If-none-match",etag)
            .pathParam("id", numericScheduleId)
        .expect()
            .statusCode(304)
        .when()
            .get("/metric/schedule/{id}");
    }

    @Test
    public void testGetScheduleByIdAndBadEtag() throws Exception {

        String etag = "-frobnitz-";
        given()
            .header(acceptJson)
            .header("If-none-match", etag)
            .pathParam("id", numericScheduleId)
        .expect()
            .statusCode(200)
            .body("scheduleId", is( numericScheduleId))
            .header("ETag", notNullValue())
//            .body("links.rel", hasItems("edit", "metric", "resource")) TODO
            .log().ifError()
        .when()
            .get("/metric/schedule/{id}");
    }

    @Test
    public void testGetScheduleByBadId() throws Exception {

        given()
            .header(acceptJson)
            .pathParam("id", 11)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .get("/metric/schedule/{id}");
    }

    @Test
    public void testGetScheduleByName() throws Exception {

        System.out.println("Searching for schedule with name " + scheduleName);
        given()
            .header(acceptJson)
            .pathParam("rid", _platformId)
            .queryParam("name", scheduleName)
            .queryParam("enabledOnly", false)
        .expect()
            .statusCode(200)
            .body("scheduleId", hasItem( numericScheduleId))
            .body("scheduleName", hasItem(scheduleName))
            .log().everything()
        .when()
            .get("/resource/{rid}/schedules");
    }

    @Test
    public void testPutGetRawData() throws Exception {

        long now = System.currentTimeMillis();

        DoubleValue dataPoint = new DoubleValue(1.5);

        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .pathParam("id", numericScheduleId)
            .pathParam("timestamp", now)
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
            .queryParam("startTime", now - 10)
            .queryParam("endTime", now + 10)
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
            if (mp.getTimeStamp()==now && mp.getScheduleId()==numericScheduleId && mp.getValue().compareTo(1.5d)==0)
                found = true;
        }
        assert found;

    }

    @Test
    public void testGetDataRawTooBigInterval() throws Exception {

        long now = System.currentTimeMillis();

        given()
            .header(acceptJson)
            .pathParam("id", numericScheduleId)
            .queryParam("startTime",now - 8* 86400 * 1000L ) // 8 days
            .queryParam("endTime",now )
        .expect()
            .statusCode(406)
            .log().ifError()
        .when()
            .get("/metric/data/{id}/raw");
    }

    @Test
    public void testGetDataRawTooLongDuration() throws Exception {

        long now = System.currentTimeMillis();

        given()
            .header(acceptJson)
            .pathParam("id", numericScheduleId)
            .queryParam("duration", 8 * 86400 * 1000L) // 8 days
        .expect()
            .statusCode(406)
            .log().ifError()
        .when()
            .get("/metric/data/{id}/raw");
    }

    /**
     * Submit data points for arbitrary metrics, identified by
     * their schedule ids
     *
     * @throws Exception on error
     */
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
            .body("", not(emptyIterable()))
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

    /**
     * Here we submit data for a single resource, and where the
     * data points have the schedule name encoded
     * @throws Exception On error
     */
    @Test
    public void testPostGetRawData2() throws Exception {

        long now = System.currentTimeMillis();

        Datapoint dataPoint = new Datapoint();
        dataPoint.setMetric(scheduleName);
        dataPoint.setTimestamp(now);
        dataPoint.setValue(1.5);
        List<Datapoint> points = new ArrayList<Datapoint>(1);
        points.add(dataPoint);

        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .pathParam("resourceId",_platformId)
            .body(points)
        .expect()
            .statusCode(204)
            .log().ifError()
        .when()
            .post("/metric/data/raw/{resourceId}");

        given()
            .header(acceptXml)
            .pathParam("id", numericScheduleId)
            // no start and end time -> last 8h of data
        .expect()
            .statusCode(200)
            .log().ifError()
            .body("",not(emptyIterable()))
        .when()
            .get("/metric/data/{id}/raw");

    }

    /**
     * Submit data points for arbitrary metrics, identified by
     * their schedule ids. We want to exactly retrieve that one
     * data point submitted
     *
     * @throws Exception on error
     */
    @Test
    public void testPostGetRawData3() throws Exception {

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
            .queryParam("startTime",now)
            .queryParam("endTime",now)
        .expect()
            .statusCode(200)
            .log().ifError()
            .body("", not(emptyIterable()))
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
    public void testUpdateUnknownSchedule() throws Exception {

        Schedule schedule = new Schedule();
        schedule.setCollectionInterval(1234567);
        schedule.setEnabled(true);

        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .pathParam("id", 43)
            .body(schedule)
        .expect()
            .statusCode(404)
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
            .pathParam("timeStamp",System.currentTimeMillis())
            .contentType(ContentType.JSON)
            .body(trait)
            .pathParam("id",tsId)
        .expect()
            .statusCode(200) // TODO 201 ?
            .log().ifError()
        .when()
            .put("/metric/data/{id}/trait/{timeStamp}");

        given()
            .header(acceptJson)
            .pathParam("id",tsId)
        .expect()
            .statusCode(200)
            .log().ifError()
            .body("value", is("Hello World!"))
        .when()
            .get("/metric/data/{id}/trait");
    }

    @Test
    public void testGetUnknownTrait() throws Exception {
        given()
            .header(acceptJson)
            .pathParam("id",41)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .get("/metric/data/{id}/trait");

    }

    @Test
    public void testAddToUnknownTrait() throws Exception {
        String trait = "{\"value\":\"Hello World!\" }";
        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .body(trait)
            .pathParam("id",123)
            .pathParam("timeStamp",System.currentTimeMillis())
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .put("/metric/data/{id}/trait/{timeStamp}");


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
    public void testSetUnknownBaseline() throws Exception {
        Baseline baseline = new Baseline(0.0,2.0,1.0,1000L);

        given()
            .contentType(ContentType.XML)
            .header(acceptJson)
            .body(baseline)
            .pathParam("sid", 123)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .put("/metric/data/{sid}/baseline");
    }

    @Test
    public void testGetAggregateForSchedule() throws Exception {
        int num = 44;
        addDataToSchedule(num);

        Response r =
        given()
            .header(acceptJson)
            .pathParam("scheduleId", numericScheduleId)
        .expect()
            .statusCode(200)
            .body("scheduleId", is(numericScheduleId))
            .body("numDataPoints", is(60))
            .body("dataPoints",iterableWithSize(60))
            .body("min",comparesEqualTo(1.5f))
            .body("max",comparesEqualTo(2.0f + num))
            .body("avg",notNullValue())
            .log().everything()
        .when()
            .get("/metric/data/{scheduleId}");

        Map map = r.as(Map.class);
        List<MDataPoint> points = (List<MDataPoint>) map.get("dataPoints");
        assert points.size()==60;

    }

    @Test
    public void testGetAggregateForResource() throws Exception {

        addDataToSchedule(1);


        JsonPath jp =
        given()
            .header(acceptJson)
            .pathParam("resourceId", _platformId)
            .queryParam("includeDataPoints",true)
        .expect()
            .statusCode(200)
            .body("scheduleId", hasItem(numericScheduleId))
            .body("", not(emptyIterable()))
            .log().everything()
        .when()
            .get("/metric/data/resource/{resourceId}")
        .jsonPath();

        List<Map<String,Object>> map = jp.getList("");
        for (Map<String,Object> entry : map) {
            if (((Integer)entry.get("scheduleId")) == numericScheduleId) {

//                assert entry.get("avg").equals(1.5f) : "Expected an avg of 1.5, but was " + entry.get("avg");
                assert entry.get("min").equals(1.5f) : "Expected an min of 1.5, but was " + entry.get("min");
//                assert entry.get("max").equals(1.5f) : "Expected an max of 1.5, but was " + entry.get("max");
                assert ((Integer)entry.get("numDataPoints"))==60;
                System.out.println(entry);
            }
        }
    }

    @Test
    public void testGetAggregateForResourceNoDataPoints() throws Exception {

        addDataToSchedule(1);

        given()
            .header(acceptJson)
            .pathParam("resourceId", _platformId)
            .queryParam("includeDataPoints",false)
        .expect()
            .statusCode(200)
            .body("scheduleId", hasItem(numericScheduleId))
            .body("[0].dataPoints", emptyIterable())
            .body("[0].numDataPoints",is(0))
            .log().everything()
        .when()
            .get("/metric/data/resource/{resourceId}");
    }

    @Test
    public void testGetAggregateForResource77DataPoints() throws Exception {

        addDataToSchedule(1);


        JsonPath jp =
        given()
            .header(acceptJson)
            .pathParam("resourceId", _platformId)
            .queryParam("includeDataPoints",true)
            .queryParam("dataPoints",77)
        .expect()
            .statusCode(200)
            .body("scheduleId", hasItem(numericScheduleId))
            .body("[0].dataPoints", iterableWithSize(77))
            .body("[0].numDataPoints",is(77))
            .log().everything()
        .when()
            .get("/metric/data/resource/{resourceId}")
        .jsonPath();

        List<Map<String,Object>> map = jp.getList("");
        for (Map<String,Object> entry : map) {
            if (((Integer)entry.get("scheduleId")) == numericScheduleId) {

//                assert entry.get("avg").equals(1.5f) : "Expected an avg of 1.5, but was " + entry.get("avg");
                assert entry.get("min").equals(1.5f) : "Expected an min of 1.5, but was " + entry.get("min");
//                assert entry.get("max").equals(1.5f) : "Expected an max of 1.5, but was " + entry.get("max");
                assert ((Integer)entry.get("numDataPoints"))==77;
                System.out.println(entry);
            }
        }
    }


    @Test
    public void testGetAggregateForUnknownResource() throws Exception {

        Response r =
        given()
            .header(acceptJson)
            .pathParam("resourceId", 12)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .get("/metric/data/resource/{resourceId}");
    }

    @Test
    public void testGetAggregateForGroup() throws Exception {

        Group group = new Group(X_TEST_GROUP);

        Response resp =
        given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(group)
            .expect()
                .statusCode(isOneOf(200,201))
                .log().ifError()
            .when()
                .post("/group");


        Group createdGroup = resp.as(Group.class);


        // Determine location from response
        int groupId = createdGroup.getId();

        try {
            // add the platform
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(group)
                .pathParam("id", groupId)
                .pathParam("resourceId",_platformId)
            .expect()
                .statusCode(HttpStatus.SC_OK)
                .log().ifError()
            .when()
                .put("/group/{id}/resource/{resourceId}");

            addDataToSchedule(1);


            // Now get the aggregate
            given()
                .header(acceptJson)
                .pathParam("groupId", groupId)
            .expect()
                .statusCode(200)
                .body("", not(emptyIterable()))
                .body("definitionId",hasItem(numericScheduleDefinitionId))
                .log().everything()
            .when()
                .get("/metric/data/group/{groupId}");

        }
        finally {
                    // delete the group again
            given()
                .pathParam("id",groupId)
            .expect()
                .statusCode(204)
                .log().ifError()
            .when()
                .delete("/group/{id}");
        }

    }

    @Test
    public void testGetDataForGroup() throws Exception {

        Group group = new Group(X_TEST_GROUP);

        Response resp =
        given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(group)
            .expect()
                .statusCode(isOneOf(200,201))
                .log().ifError()
            .when()
                .post("/group");


        Group createdGroup = resp.as(Group.class);


        // Determine location from response
        int groupId = createdGroup.getId();

        try {
            // add the platform
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(group)
                .pathParam("id", groupId)
                .pathParam("resourceId",_platformId)
            .expect()
                .statusCode(HttpStatus.SC_OK)
                .log().ifError()
            .when()
                .put("/group/{id}/resource/{resourceId}");

            addDataToSchedule(1);


            // Now get the data
            given()
                .header(acceptJson)
                .pathParam("groupId", groupId)
                .pathParam("defId",numericScheduleDefinitionId)
            .expect()
                .statusCode(200)
                .body("dataPoints", iterableWithSize(60))
                .body("numDataPoints",is(60))
                .log().everything()
            .when()
                .get("/metric/data/group/{groupId}/{defId}");

        }
        finally {
                    // delete the group again
            given()
                .pathParam("id",groupId)
            .expect()
                .statusCode(204)
                .log().ifError()
            .when()
                .delete("/group/{id}");
        }

    }

    @Test
    public void testGetDataForGroup45Points() throws Exception {

        Group group = new Group(X_TEST_GROUP);

        Response resp =
        given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(group)
            .expect()
                .statusCode(isOneOf(200,201))
                .log().ifError()
            .when()
                .post("/group");


        Group createdGroup = resp.as(Group.class);


        // Determine location from response
        int groupId = createdGroup.getId();

        try {
            // add the platform
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(group)
                .pathParam("id", groupId)
                .pathParam("resourceId",_platformId)
            .expect()
                .statusCode(HttpStatus.SC_OK)
                .log().ifError()
            .when()
                .put("/group/{id}/resource/{resourceId}");

            addDataToSchedule(1);


            // Now get the data
            given()
                .header(acceptJson)
                .pathParam("groupId", groupId)
                .pathParam("defId",numericScheduleDefinitionId)
                .queryParam("dataPoints",45)
            .expect()
                .statusCode(200)
                .body("dataPoints", iterableWithSize(45))
                .body("numDataPoints",is(45))
                .log().everything()
            .when()
                .get("/metric/data/group/{groupId}/{defId}");

        }
        finally {
                    // delete the group again
            given()
                .pathParam("id",groupId)
            .expect()
                .statusCode(204)
                .log().ifError()
            .when()
                .delete("/group/{id}");
        }

    }


    @Test
    public void testGetAggregateForUnknownGroup() throws Exception {

        given()
            .header(acceptXml)
            .pathParam("groupId", 41)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .get("/metric/data/group/{groupId}");
    }

    @Test
    public void testGetAggregateUnknownSchedule() throws Exception {

        given()
            .header(acceptJson)
            .pathParam("scheduleId", 123)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .get("/metric/data/{scheduleId}");

    }

    @Test
    public void testGetAggregate120PointsSchedule() throws Exception {

        addDataToSchedule(34);

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
            .queryParam("dataPoints", -1)
        .expect()
            .statusCode(406)
        .when()
            .get("/metric/data/{scheduleId}");
    }

    @Test
    public void testGetMetricDataNoSchedule() throws Exception {
        given()
            .header(acceptJson)
        .expect()
            .statusCode(406)
        .when()
            .get("/metric/data");


    }

    @Test
    public void testGetMetricDataOneSchedule() throws Exception {

        int num = 13;
        addDataToSchedule(num);
JsonPath jp =
        given()
            .header(acceptJson)
            .queryParam("sid", numericScheduleId)
        .expect()
            .statusCode(200)
            .log().ifError()
//            .body("[0].min",closeTo(1.5,0.1))
//            .body("[0].max",closeTo(46.0,0.1)) // We may have data already
//            .body("[0].avg",notNullValue())
        .when()
            .get("/metric/data").jsonPath();
    }

    @Test
    public void testGetMetricDataOneSchedule99Points() throws Exception {

        int num = 13;
        addDataToSchedule(num);
JsonPath jp =
        given()
            .header(acceptJson)
            .queryParam("sid", numericScheduleId)
            .queryParam("dataPoints", 99)
        .expect()
            .statusCode(200)
            .log().ifError()
            .body("[0].dataPoints", iterableWithSize(99))
//            .body("[0].min",closeTo(1.5,0.1))
//            .body("[0].max",closeTo(46.0,0.1)) // We may have data already
//            .body("[0].avg",notNullValue())
        .when()
            .get("/metric/data").jsonPath();
    }

    @Test
    public void testGetMetricDataOneScheduleSkipEmpty() throws Exception {

        int num = 13;
        addDataToSchedule(num);

        given()
            .header(acceptJson)
            .queryParam("sid", numericScheduleId)
            .queryParam("hideEmpty",true)
        .expect()
            .statusCode(200)
            .log().ifError()
//            .body("[0].min", closeTo(1.5,0.1))
//            .body("[0].max", notNullValue()) // We may have data already
//            .body("[0].avg", notNullValue())
        .when()
            .get("/metric/data");
    }

    @Test
    public void testGetMetricDataTwoSchedule() throws Exception {

        addDataToSchedule(5);

        given()
            .header(acceptJson)
            .queryParam("sid",numericScheduleId+","+numericScheduleId)
        .expect()
            .statusCode(200)
            .log().ifError()
            .body("", iterableWithSize(2))
//            .body("[0].min", closeTo(1.5,0.1))
//            .body("[0].max", notNullValue()) // We may have data already
//            .body("[0].avg", notNullValue())

        .when()
            .get("/metric/data");
    }

    @Test
    public void testGetUpdateDefinition() throws Exception {

        Schedule schedule = new Schedule();
        schedule.setCollectionInterval(1234567);
        schedule.setEnabled(true);

        // First read the current data

        Schedule def =
        given()
            .header(acceptJson)
            .pathParam("id", numericScheduleDefinitionId)
        .expect()
            .statusCode(200)
        .when()
            .get("/metric/definition/{id}")
        .as(Schedule.class);

        long oldInterval = def.getCollectionInterval();


        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .pathParam("id", numericScheduleDefinitionId)
            .body(schedule)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .put("/metric/definition/{id}");

        // reset to original interval

        schedule.setCollectionInterval(oldInterval);
        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .pathParam("id", numericScheduleDefinitionId)
            .body(schedule)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .put("/metric/definition/{id}");



    }

    @Test
    public void testUpdateBadDefinition() throws Exception {

        Schedule schedule = new Schedule();
        schedule.setCollectionInterval(1234567);
        schedule.setEnabled(true);


        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .pathParam("id", 42)
            .body(schedule)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .put("/metric/definition/{id}");

    }

    @Test
    public void testGetBadDefinition() throws Exception {

        given()
            .header(acceptJson)
            .pathParam("id",42)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .get("/metric/definition/{id}");

    }

    private void addDataToSchedule(int howMany) {
        long now = System.currentTimeMillis();
        long tenMinutes = 10 * 60 * 1000L; // 10 mins

        // Post some data points
        List<MDataPoint> points = new ArrayList<MDataPoint>(howMany);
        MDataPoint dataPoint = new MDataPoint();
        dataPoint.setScheduleId(numericScheduleId);
        dataPoint.setTimeStamp(now);
        dataPoint.setValue(1.5);
        points.add(dataPoint);

        for (int i=1; i < howMany; i++) {
            dataPoint = new MDataPoint();
            dataPoint.setScheduleId(numericScheduleId);
            dataPoint.setTimeStamp(now-(i*tenMinutes));
            dataPoint.setValue(i+3.0);
            points.add(dataPoint);
        }

        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .body(points)
        .expect()
            .statusCode(204)
            .log().ifError()
        .when()
            .post("/metric/data/raw");
    }


}
