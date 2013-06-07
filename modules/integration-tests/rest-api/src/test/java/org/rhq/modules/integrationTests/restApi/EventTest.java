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
import com.jayway.restassured.response.Headers;
import com.jayway.restassured.response.Response;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import org.rhq.modules.integrationTests.restApi.d.Event;
import org.rhq.modules.integrationTests.restApi.d.EventSource;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static com.jayway.restassured.RestAssured.given;

/**
 * Test the event endpoints of the REST api
 * @author Heiko W. Rupp
 */
public class EventTest extends AbstractBase {

    @Test
    public void testGetSourcesForResource() throws Exception {

        given()
            .header(acceptJson)
            .pathParam("id",_platformId)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/event/{id}/sources");

    }

    @Test
    public void testGetDefinitionsForResource() throws Exception {

        given()
            .header(acceptJson)
            .pathParam("id",_platformId)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/event/{id}/definitions");

    }

    @Test
    public void testAddEventSourceForUnknownResource() throws Exception {

        EventSource es = new EventSource();
        es.setResourceId(15);
        es.setName("Event Log"); // Name of the event definition
        es.setLocation("-x-test-location");

        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .pathParam("id",15)
            .body(es)
        .expect()
            .statusCode(404)
        .when()
            .post("/event/{id}/sources");
    }

    @Test
    public void testAddEventSourceForUnknownDefinition() throws Exception {

        EventSource es = new EventSource();
        es.setResourceId(_platformId);
        es.setName("Frobnitz"); // Name of the event definition
        es.setLocation("-x-test-location");

        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .pathParam("id", _platformId)
            .body(es)
        .expect()
            .statusCode(404)
        .when()
            .post("/event/{id}/sources");
    }

    @Test
    public void testAddGetDeleteEventSource() throws Exception {

        EventSource es = new EventSource();
        es.setResourceId(_platformId);
        es.setName("Event Log"); // Name of the event definition
        es.setLocation("-x-test-location");

        Response response =
        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .pathParam("id",_platformId)
            .body(es)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .post("/event/{id}/sources");

        EventSource result = response.as(EventSource.class);

        try {

            // Directly find our generated source

            response =
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .pathParam("id",result.getId())
            .expect()
                .statusCode(200)
                .log().ifError()
            .when()
                .get("/event/source/{id}");

            EventSource ev2 = response.as(EventSource.class);
            assert result.equals(ev2);

            // Search in the list for the resource
            response =
            given()
                .header(acceptJson)
                .pathParam("id",_platformId)
            .expect()
                .statusCode(200)
            .when()
                .get("/event/{id}/sources");

            List<Map<String,Object>> listOfMaps = response.as(List.class);
            boolean found = false;
            for (Map<String,Object> map: listOfMaps) {
                if (map.get("id").equals(result.getId()) && map.get("name").equals(result.getName()))
                    found=true;
            }
            assert found;
        }
        finally {

            // Delete the source again
            given()
                .pathParam("id", result.getId())
            .expect()
                .statusCode(204)
            .when()
                .delete("/event/source/{id}");
        }
    }

    @Test
    public void testAddGetEventOnSource() throws Exception {

        EventSource es = new EventSource();
        es.setResourceId(_platformId);
        es.setName("Event Log"); // Name of the event definition
        es.setLocation("-x-test-location");

        Response response =
        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .pathParam("id",_platformId)
            .body(es)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .post("/event/{id}/sources");

        EventSource eventSource = response.as(EventSource.class);

        long now = System.currentTimeMillis();
        try {

            // Add an event
            Event event = new Event(eventSource.getId(),now,"Li la lu :->");
            List<Event> events = new ArrayList<Event>(1);
            events.add(event);

            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .pathParam("id",eventSource.getId())
                .body(events)
            .expect()
                .statusCode(204) // no content returned
                .log().ifError()
            .when()
                .post("/event/source/{id}/events");


            // and retrieve it again from the event source
            response =
            given()
                .header(acceptJson)
                .pathParam("id", eventSource.getId())
                .queryParam("startTime",now - 10)
                .queryParam("endTime",now + 10)
            .expect()
                .statusCode(200)
                .log().ifError()
            .when()
                .get("/event/source/{id}/events");
            List list = response.as(List.class);
            assert list.size()>0;

            // Get the list of events from the resource
            response =
            given()
                .header(acceptJson)
                .pathParam("id", _platformId)
                .queryParam("startTime",now - 10)
                .queryParam("endTime",now + 10)
            .expect()
                .statusCode(200)
                .log().ifError()
            .when()
                .get("/event/{id}/events");
            list = response.as(List.class);
            assert list.size()>0;


        }
        finally {

            // Delete the source again
            given()
                .pathParam("id", eventSource.getId())
            .expect()
                .statusCode(204)
            .when()
                .delete("/event/source/{id}");
        }
    }

    @Test
    public void testAddGetEventOnSourceWithPaging() throws Exception {

        EventSource es = new EventSource();
        es.setResourceId(_platformId);
        es.setName("Event Log"); // Name of the event definition
        es.setLocation("-x-test-location");

        Response response =
        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .pathParam("id",_platformId)
            .body(es)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .post("/event/{id}/sources");

        EventSource eventSource = response.as(EventSource.class);

        long now = System.currentTimeMillis();
        try {

            // Add an event
            Event event = new Event(eventSource.getId(),now,"Li la lu 1:->");
            Event event1 = new Event(eventSource.getId(),now,"Li la lu 2:->");
            Event event2 = new Event(eventSource.getId(),now,"Li la lu 3:->");
            Event event3 = new Event(eventSource.getId(),now,"Li la lu 4:->");
            List<Event> events = new ArrayList<Event>(4);
            events.add(event);
            events.add(event1);
            events.add(event2);
            events.add(event3);

            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .pathParam("id",eventSource.getId())
                .body(events)
            .expect()
                .statusCode(204) // no content returned
                .log().ifError()
            .when()
                .post("/event/source/{id}/events");


            // and retrieve it again from the event source
            response =
            given()
                .header(acceptJson)
                .pathParam("id", eventSource.getId())
                .queryParam("startTime",now - 10)
                .queryParam("endTime",now + 10)
            .expect()
                .statusCode(200)
                .log().ifError()
                .header("X-collection-size", CoreMatchers.is("4"))
            .when()
                .get("/event/source/{id}/events");
            List list = response.as(List.class);
            assert list.size()>0;

            // Get the list of events from the resource
            response =
            given()
                .header(acceptJson)
                .pathParam("id", _platformId)
                .queryParam("startTime",now - 10)
                .queryParam("endTime",now + 10)
                .queryParam("page",0)
                .queryParam("ps",2)
            .expect()
                .statusCode(200)
                .log().ifError()
                .header("X-collection-size", CoreMatchers.is("4"))
                .header("Link",not(containsString("prev")))
            .when()
                .get("/event/{id}/events");
            list = response.as(List.class);
            assert list.size()==2;

            response =
            given()
                .header(acceptJson)
                .pathParam("id", _platformId)
                .queryParam("startTime",now - 10)
                .queryParam("endTime",now + 10)
                .queryParam("page",1)
                .queryParam("ps",2)
            .expect()
                .statusCode(200)
                .log().ifError()
                .header("X-collection-size", CoreMatchers.is("4"))
                .header("Link",not(containsString("next")))
            .when()
                .get("/event/{id}/events");

            Headers headers = response.getHeaders();
            int found = 0;
            for (String link: headers.getValues("Link")) {
                if (link.contains("rel=\"last\""))
                    found++;
                if (link.contains("rel=\"prev\""))
                    found++;
                if (link.contains("rel=\"current\""))
                    found++;
                assert !link.contains("rel=\"next\"");
            }
            assert found == 3;

        }
        finally {

            // Delete the source again
            given()
                .pathParam("id", eventSource.getId())
            .expect()
                .statusCode(204)
            .when()
                .delete("/event/source/{id}");
        }
    }

    @Test
    public void testDeleteUnknownSource() throws Exception {
        given()
            .pathParam("id", 123)
        .expect()
            .statusCode(204)
        .when()
            .delete("/event/source/{id}");

    }

    @Test
    public void testDeleteUnknownSourceWithValidate() throws Exception {
        given()
            .pathParam("id", 123)
            .queryParam("validate",true)
        .expect()
            .statusCode(404)
        .when()
            .delete("/event/source/{id}");

    }
}
