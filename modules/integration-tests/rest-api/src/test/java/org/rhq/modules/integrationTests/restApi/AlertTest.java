/*
 * RHQ Management Platform
 *  Copyright (C) 2005-2012 Red Hat, Inc.
 *  All rights reserved.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation version 2 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.integrationTests.restApi;

import static com.jayway.restassured.RestAssured.delete;
import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;

import java.util.List;
import java.util.Map;

import com.jayway.restassured.config.RedirectConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.path.xml.XmlPath;
import com.jayway.restassured.response.Response;

import org.hamcrest.Matchers;
import org.junit.Test;

import org.rhq.modules.integrationTests.restApi.d.AlertCondition;
import org.rhq.modules.integrationTests.restApi.d.AlertDefinition;
import org.rhq.modules.integrationTests.restApi.d.AlertNotification;
import org.rhq.modules.integrationTests.restApi.d.Availability;
import org.rhq.modules.integrationTests.restApi.d.Group;

/**
 * Testing of the Alerting part of the rest-api
 * @author Heiko W. Rupp
 */
public class AlertTest extends AbstractBase {

    @Test
    public void testListAllAlertsJson() throws Exception {

        given()
            .header(acceptJson)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/alert");

    }

    @Test
    public void testListAllAlertsXml() throws Exception {

        given()
            .header(acceptXml)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/alert");
    }

    @Test
    public void testListAllAlertsHtml() throws Exception {

        given()
            .header(acceptHtml)
        .expect()
            .statusCode(200)
        .when()
            .get("/alert");

    }

    @Test
    public void testListAllAlertsTextPlain() throws Exception {

        given()
            .header("Accept","text/plain")
        .expect()
            .statusCode(503)
        .when()
            .get("/alert");

    }

    @Test
    public void testListAlertsWithPaging() throws Exception {

        given()
            .header(acceptJson)
            .queryParam("ps", 2)
            .queryParam("page", 0)
        .expect()
            .statusCode(200)
            .header("Link", anyOf(containsString("current"), Matchers.containsString("last")))
            .header("X-collection-size", notNullValue())
            .log().ifError()
        .when()
            .get("/alert");
    }

    @Test
    public void testListAlertsWithPagingAndWrapped() throws Exception {

        given()
            .header(acceptWrappedJson)
            .queryParam("ps", 2)
            .queryParam("page", 0)
        .expect()
            .statusCode(200)
            .header("Link", nullValue())
            .body("totalSize", notNullValue())
            .log().ifError()
        .when()
            .get("/alert");
    }


    @Test
    public void testGetAlertCountJson() throws Exception {

        given()
            .header(acceptJson)
        .expect()
            .statusCode(200)
            .log().ifError()
            .body("value", instanceOf(Number.class))
        .when()
            .get("/alert/count");
    }

    @Test
    public void testGetAlertCountXml() throws Exception {

        XmlPath xmlPath =
        given()
            .header(acceptXml)
        .expect()
            .statusCode(200)
            .log().everything()
        .when()
            .get("/alert/count")
        .xmlPath();

        xmlPath.getInt("value.@value");
    }

    @Test
    public void testGetAlertByBadId() throws Exception {
        given()
            .header(acceptJson)
            .pathParam("id",123)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .get("/alert/{id}");
    }

    @Test
    public void testGetAlertConditionLogsByBadId() throws Exception {
        given()
            .header(acceptJson)
            .pathParam("id",123)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .get("/alert/{id}/conditions");
    }

    @Test
    public void testGetAlertNotificationLogsByBadId() throws Exception {
        given()
            .header(acceptJson)
            .pathParam("id",123)
        .expect()
            .statusCode(404)
            .log().everything()
        .when()
            .get("/alert/{id}/notifications");
    }

    @Test
    public void testListAllAlertDefinitions() throws Exception {

        expect()
            .statusCode(200)
        .when()
            .get("/alert/definitions");
    }

    @Test
    public void testListAllAlertDefinitionsRedirects() throws Exception {

        given()
            .config(RestAssuredConfig.config().redirect(RedirectConfig.redirectConfig().followRedirects(false)))
        .expect()
            .statusCode(303)
            .log().ifError()
            .header("Location",endsWith("rest/alert/definitions"))
        .when()
            .get("/alert/definition");

        given()
            .config(RestAssuredConfig.config().redirect(RedirectConfig.redirectConfig().followRedirects(false)))
        .expect()
            .statusCode(303)
            .log().ifError()
            .header("Location",endsWith("rest/alert/definitions.json"))
        .when()
            .get("/alert/definition.json");

        // This time follow redirect
        expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/alert/definition.json");

        expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/alert/definition");

        expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/alert/definition.xml");
    }

    @Test
    public void testListAllAlertDefinitionsWithWrapping() throws Exception {

        given()
            .header(acceptWrappedJson)
            .log().everything()
        .expect()
            .statusCode(200)
            .log().ifError()
            .body("currentPage", Matchers.notNullValue())
            .body("totalSize", Matchers.notNullValue())
        .when()
            .get("/alert/definitions");
    }

    @Test
    public void testRedirectForDefinition() throws Exception {
        given()
            .header(acceptJson)
        .expect()
            .statusCode(200)
        .when()
            .get("/alert/definition");

        // TODO check that some definitions exist after we know how to create them
    }

    @Test
    public void testGetAlertSenders() throws Exception {
        given()
            .header(acceptJson)
        .expect()
            .statusCode(200)
            .log().everything()
        .when()
            .get("/alert/senders");
    }

    @Test
    public void testGetAlertSendersXML() throws Exception {
        given()
            .header(acceptXml)
        .expect()
            .statusCode(200)
            .log().everything()
        .when()
            .get("/alert/senders");
    }

    @Test
    public void testGetSenderByName() throws Exception {
        given()
            .header(acceptJson)
            .pathParam("name", "Direct Emails")
        .expect()
            .statusCode(200)
            .log().everything()
        .when()
            .get("/alert/sender/{name}");
    }

    @Test
    public void testGetSenderByNameXML() throws Exception {
        given()
            .header(acceptXml)
            .pathParam("name", "Direct Emails")
        .expect()
            .statusCode(200)
            .log().everything()
        .when()
            .get("/alert/sender/{name}");
    }

    @Test
    public void testGetUnknownSenderByName() throws Exception {
        given()
            .header(acceptJson)
            .pathParam("name", "Frobnitz")
        .expect()
            .statusCode(404)
            .log().everything()
        .when()
            .get("/alert/sender/{name}");
    }

    @Test
    public void testCreateDeleteBasicAlertDefinition() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);

        cleanupDefinition(definitionId);
    }

    @Test
    public void testCreateDeleteBasicAlertDefinitionNoneDampening() throws Exception {

        int definitionId=0;
        try {
            AlertDefinition alertDefinition = new AlertDefinition();
            alertDefinition.setName("-x-test-definition");
            alertDefinition.setEnabled(false);
            alertDefinition.setPriority("LOW");
            alertDefinition.setDampeningCategory("NONE");

            AlertDefinition result =
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(alertDefinition)
                .queryParam("resourceId",_platformId)
            .expect()
                .statusCode(201)
                .body("dampeningCategory",is("NONE"))
                .body("dampeningCount",is(0))
                .body("dampeningPeriod",is(0))
            .when()
                .post("/alert/definitions")
            .as(AlertDefinition.class);

            definitionId = result.getId();

        } finally {
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateDeleteBasicAlertDefinitionWithBadSender() throws Exception {

        int definitionId=0;
        try {
            AlertDefinition alertDefinition = new AlertDefinition();
            alertDefinition.setName("-x-test-definition");
            alertDefinition.setEnabled(false);
            alertDefinition.setPriority("LOW");
            alertDefinition.setDampeningCategory("NONE");

            AlertNotification notification = new AlertNotification("Invalid sender name");
            alertDefinition.getNotifications().add(notification);

            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(alertDefinition)
                .queryParam("resourceId",_platformId)
            .expect()
                .statusCode(404)
                .log().everything()
            .when()
                .post("/alert/definitions");

        } finally {
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateDeleteBasicAlertDefinitionWithBadRecoveryId() throws Exception {

        int definitionId=0;
        try {
            AlertDefinition alertDefinition = new AlertDefinition();
            alertDefinition.setName("-x-test-definition");
            alertDefinition.setEnabled(false);
            alertDefinition.setPriority("LOW");
            alertDefinition.setDampeningCategory("NONE");
            alertDefinition.setRecoveryId(13);

            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(alertDefinition)
                .queryParam("resourceId",_platformId)
            .expect()
                .statusCode(404)
                .log().everything()
            .when()
                .post("/alert/definitions");

        } finally {
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateDeleteBasicAlertDefinitionBadDampeningCategory() throws Exception {

        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setName("-x-test-definition");
        alertDefinition.setEnabled(false);
        alertDefinition.setPriority("LOW");
        alertDefinition.setDampeningCategory("Hulla");

        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .body(alertDefinition)
            .queryParam("resourceId",_platformId)
        .expect()
            .statusCode(406)
            .log().everything()
        .when()
            .post("/alert/definitions");

    }

    @Test
    public void testCreateDeleteBasicAlertDefinition3of5Dampening() throws Exception {

        int definitionId=0;
        try {
            AlertDefinition alertDefinition = new AlertDefinition();
            alertDefinition.setName("-x-test-definition");
            alertDefinition.setEnabled(false);
            alertDefinition.setPriority("LOW");
            alertDefinition.setDampeningCategory("PARTIAL_COUNT");
            alertDefinition.setDampeningCount(3);
            alertDefinition.setDampeningPeriod(5);

            AlertDefinition result =
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(alertDefinition)
                .queryParam("resourceId",_platformId)
            .expect()
                .statusCode(201)
                .body("dampeningCategory",is("PARTIAL_COUNT"))
                .body("dampeningCount",is(3))
                .body("dampeningPeriod",is(5))
            .when()
                .post("/alert/definitions")
            .as(AlertDefinition.class);

            definitionId = result.getId();

        } finally {
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateDeleteBasicAlertDefinitionOncein3MinDampening() throws Exception {

        int definitionId=0;
        try {
            AlertDefinition alertDefinition = new AlertDefinition();
            alertDefinition.setName("-x-test-definition");
            alertDefinition.setEnabled(false);
            alertDefinition.setPriority("LOW");
            alertDefinition.setDampeningCategory("DURATION_COUNT");
            alertDefinition.setDampeningCount(1);
            alertDefinition.setDampeningPeriod(3);
            alertDefinition.setDampeningUnit("minutes");

            AlertDefinition result =
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(alertDefinition)
                .queryParam("resourceId", _platformId)
            .expect()
                .statusCode(201)
                .body("dampeningCategory", is("DURATION_COUNT"))
                .body("dampeningCount", is(1))
                .body("dampeningPeriod", is(3))
                .body("dampeningUnit", is("MINUTES"))
            .when()
                .post("/alert/definitions")
            .as(AlertDefinition.class);

            definitionId = result.getId();

        } finally {
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateDeleteAlertDefinitionWith1ConditionAvail() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);

        // Now add a condition
        try {

            AlertCondition alertCondition = new AlertCondition("AVAILABILITY", "AVAIL_GOES_UP");
            addConditionToDefinition(definitionId, alertCondition);

            // Retrieve the definition with the added condition
            AlertDefinition updatedDefinition =
            given()
                .pathParam("id",definitionId)
                .queryParam("full",true)
            .expect()
                .statusCode(200)
                .log().everything()
            .when()
                .get("/alert/definition/{id}")
                .as(AlertDefinition.class);

            int size = updatedDefinition.getConditions().size();
            assert size ==1 : "Did not find 1 condition, but " + size;
        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateDeleteAlertDefinitionWith1ConditionAvailDuration() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);

        // Now add a condition
        try {

            AlertCondition alertCondition = new AlertCondition("AVAIL_DURATION", "AVAIL_DURATION_NOT_UP");
            alertCondition.setOption("300"); // duration in seconds
            addConditionToDefinition(definitionId, alertCondition);

            // Retrieve the definition with the added condition
            AlertDefinition updatedDefinition =
            given()
                .pathParam("id",definitionId)
                .queryParam("full",true)
            .expect()
                .statusCode(200)
                .log().everything()
            .when()
                .get("/alert/definition/{id}")
                .as(AlertDefinition.class);

            int size = updatedDefinition.getConditions().size();
            assert size ==1 : "Did not find 1 condition, but " + size;
        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateDeleteAlertDefinitionWith1ConditionAvailDurationBadOption() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);

        // Now add a condition
        try {

            AlertCondition alertCondition = new AlertCondition("AVAIL_DURATION", "AVAIL_DURATION_DOWN");
            alertCondition.setOption("300 sec");
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(alertCondition)
                .pathParam("defId",definitionId)
            .expect()
                .statusCode(406)
                .log().ifError()
            .when()
                .post("/alert/definition/{defId}/conditions");
        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateDeleteAlertDefinitionWith1ConditionEvent() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);

        // Now add a condition
        try {

            AlertCondition alertCondition = new AlertCondition("EVENT", "DEBUG");
            alertCondition.setOption(".*lala.*"); // RegEx to match
            addConditionToDefinition(definitionId, alertCondition);

            // Retrieve the definition with the added condition
            AlertDefinition updatedDefinition =
            given()
                .pathParam("id",definitionId)
                .queryParam("full",true)
            .expect()
                .statusCode(200)
                .log().everything()
            .when()
                .get("/alert/definition/{id}")
                .as(AlertDefinition.class);

            int size = updatedDefinition.getConditions().size();
            assert size ==1 : "Did not find 1 condition, but " + size;
        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateDeleteAlertDefinitionWith1ConditionBaseline() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);

        int metricDefinitionId = findAMetricDefinitionForResourceId(_platformId, "metric");

        // Now add a condition
        try {

            AlertCondition alertCondition = new AlertCondition("BASELINE");
            alertCondition.setOption("mean");
            alertCondition.setComparator("<");
            alertCondition.setThreshold(0.10); // %
            alertCondition.setMeasurementDefinition(metricDefinitionId);

            addConditionToDefinition(definitionId, alertCondition);

            // Retrieve the definition with the added condition
            AlertDefinition updatedDefinition =
            given()
                .pathParam("id",definitionId)
                .queryParam("full",true)
            .expect()
                .statusCode(200)
                .log().everything()
            .when()
                .get("/alert/definition/{id}")
                .as(AlertDefinition.class);

            int size = updatedDefinition.getConditions().size();
            assert size ==1 : "Did not find 1 condition, but " + size;
        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateDeleteAlertDefinitionWith1ConditionBaselineBadComparator() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);

        int metricDefinitionId = findAMetricDefinitionForResourceId(_platformId, "metric");

        // Now add a condition
        try {

            AlertCondition alertCondition = new AlertCondition("BASELINE");
            alertCondition.setOption("mean");
            alertCondition.setComparator("==");
            alertCondition.setThreshold(0.10); // %
            alertCondition.setMeasurementDefinition(metricDefinitionId);

            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(alertCondition)
                .pathParam("defId",definitionId)
            .expect()
                .statusCode(406)
                .log().ifError()
            .when()
                .post("/alert/definition/{defId}/conditions");
        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateDeleteAlertDefinitionWith1ConditionBaselineBadMeticDef() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);

        int metricDefinitionId = -42;

        // Now add a condition
        try {

            AlertCondition alertCondition = new AlertCondition("BASELINE");
            alertCondition.setOption("mean");
            alertCondition.setComparator("==");
            alertCondition.setThreshold(0.10); // %
            alertCondition.setMeasurementDefinition(metricDefinitionId);

            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(alertCondition)
                .pathParam("defId",definitionId)
            .expect()
                .statusCode(404) // definition not found
                .log().ifError()
            .when()
                .post("/alert/definition/{defId}/conditions");
        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }

    private int findAMetricDefinitionForResourceId(int resourceId, String type) {
        Response response =
        given()
            .pathParam("id",resourceId)
            .queryParam("type",type)
            .header(acceptJson)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/resource/{id}/schedules");

        JsonPath jsonPath = response.jsonPath();
        int definitionId = jsonPath.getInt("[0].definitionId");

        return definitionId;
    }

    @Test
    public void testCreateDeleteAlertDefinitionWith1ConditionDrift() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);

        // Now add a condition
        try {

            AlertCondition alertCondition = new AlertCondition("DRIFT", "CHANGES");
            addConditionToDefinition(definitionId, alertCondition);

            // Retrieve the definition with the added condition
            AlertDefinition updatedDefinition =
            given()
                .pathParam("id",definitionId)
                .queryParam("full",true)
            .expect()
                .statusCode(200)
                .log().everything()
            .when()
                .get("/alert/definition/{id}")
                .as(AlertDefinition.class);

            int size = updatedDefinition.getConditions().size();
            assert size ==1 : "Did not find 1 condition, but " + size;
        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateDeleteAlertDefinitionWith1ConditionOperation() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);

        // Now add a condition
        try {

            AlertCondition alertCondition = new AlertCondition("CONTROL", "CHANGES");
            alertCondition.setOption("SUCCESS");
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(alertCondition)
                .pathParam("defId", definitionId)
                .log().everything()
            .expect()
                .statusCode(201)
                .log().ifError()
            .when()
                .post("/alert/definition/{defId}/conditions");

            // Retrieve the definition with the added condition
            AlertDefinition updatedDefinition =
            given()
                .pathParam("id",definitionId)
                .queryParam("full",true)
            .expect()
                .statusCode(200)
                .log().everything()
            .when()
                .get("/alert/definition/{id}")
                .as(AlertDefinition.class);

            int size = updatedDefinition.getConditions().size();
            assert size ==1 : "Did not find 1 condition, but " + size;
        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateDeleteAlertDefinitionWith1ConditionOperationBadOption() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);

        // Now add a condition
        try {

            AlertCondition alertCondition = new AlertCondition("CONTROL", "CHANGES");
            alertCondition.setOption("Frobnitz");
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(alertCondition)
                .pathParam("defId", definitionId)
                .log().everything()
            .expect()
                .statusCode(406)
                .log().ifError()
            .when()
                .post("/alert/definition/{defId}/conditions");

        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateDeleteAlertDefinitionWithBadConditionOperation() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);

        // Now add a condition
        try {

            AlertCondition alertCondition = new AlertCondition("CONTROL", "LA_LA");
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(alertCondition)
                .pathParam("defId", definitionId)
                .log().everything()
            .expect()
                .statusCode(406)
                .log().ifError()
            .when()
                .post("/alert/definition/{defId}/conditions");

        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateDeleteAlertDefinitionWithBadCategory() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);

        // Now add a condition
        try {

            AlertCondition alertCondition = new AlertCondition("OOPS", "CHANGES");
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(alertCondition)
                .pathParam("defId",definitionId)
                .log().everything()
            .expect()
                .statusCode(406)
                .log().ifError()
            .when()
                .post("/alert/definition/{defId}/conditions");

        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }


    @Test
    public void testCreateDeleteAlertDefinitionWith2Conditions() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);


        try {
            // Now add a 1st condition
            AlertCondition alertCondition = new AlertCondition("AVAILABILITY", "AVAIL_GOES_UP");
            addConditionToDefinition(definitionId, alertCondition);

            // Retrieve the definition with the added condition
            AlertDefinition updatedDefinition =
            given()
                .pathParam("id",definitionId)
                .queryParam("full",true)
            .expect()
                .statusCode(200)
                .log().everything()
            .when()
                .get("/alert/definition/{id}")
                .as(AlertDefinition.class);

            int size = updatedDefinition.getConditions().size();
            assert size ==1 : "Did not find 1 condition, but " + size;

            // Now add a 2nd condition
            AlertCondition secondCondition = new AlertCondition("AVAILABILITY", "AVAIL_GOES_DOWN");
            addConditionToDefinition(definitionId, secondCondition);

            // Retrieve the definition with the added condition
            updatedDefinition =
            given()
                .pathParam("id",definitionId)
                .queryParam("full",true)
            .expect()
                .statusCode(200)
                .log().everything()
            .when()
                .get("/alert/definition/{id}")
                .as(AlertDefinition.class);

            size = updatedDefinition.getConditions().size();
            assert size ==2 : "Did not find 2 condition, but " + size;
        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateDeleteAlertDefinitionWith1Notification() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);

        // Now add a condition
        try {

            AlertNotification notification = new AlertNotification("Direct Emails"); // short-name from server plugin descriptor
            notification.getConfig().put("emailAddress", "root@eruditorium.org");

            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(notification)
                .pathParam("defId", definitionId)
                .log().everything()
            .expect()
                .statusCode(201)
                .log().everything()
            .when()
                .post("/alert/definition/{defId}/notifications");

            // Retrieve the definition with the added condition
            AlertDefinition updatedDefinition =
            given()
                .pathParam("id",definitionId)
                .queryParam("full",true)
            .expect()
                .statusCode(200)
                .log().everything()
            .when()
                .get("/alert/definition/{id}")
                .as(AlertDefinition.class);

            int size = updatedDefinition.getNotifications().size();
            assert size ==1 : "Did not find 1 notification, but " + size;
        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateDeleteAlertDefinitionWith1NotificationExtraConfig() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);

        // find operation definition ID for discovery operation
        int discoveryDefinitionId = -1;
        Response r =
            given()
                .header(acceptJson)
                .queryParam("resourceId",_platformId)
            .expect()
                .statusCode(200)
                .log().ifError()
            .when()
                .get("/operation/definitions");

            discoveryDefinitionId = -1;
            List<Map<String,Object>> list = r.as(List.class);
            for (Map<String,Object> map : list) {
                String name = (String) map.get("name");
                Integer id = (Integer) map.get("id");
                if (name.equals("discovery")) {
                    discoveryDefinitionId = id;
                }
            }
         assert discoveryDefinitionId !=-1 : "No discovery operation found";

        // Now add a condition
        try {

            AlertNotification notification = new AlertNotification("Resource Operations"); // short-name from server plugin descriptor
            notification.getConfig().put("selection-mode", "SELF");
            notification.getConfig().put("operation-definition-id", discoveryDefinitionId);
            notification.getExtraConfig().put("detailedDiscovery", false);

            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(notification)
                .pathParam("defId", definitionId)
                .log().everything()
            .expect()
                .statusCode(201)
                .log().everything()
            .when()
                .post("/alert/definition/{defId}/notifications");

            // Retrieve the definition with the added condition
            AlertDefinition updatedDefinition =
            given()
                .pathParam("id",definitionId)
                .queryParam("full",true)
            .expect()
                .statusCode(200)
                .log().everything()
            .when()
                .get("/alert/definition/{id}")
                .as(AlertDefinition.class);

            int size = updatedDefinition.getNotifications().size();
            assert size ==1 : "Did not find 1 notification, but " + size;
            AlertNotification newNotification = updatedDefinition.getNotifications().get(0);
            assert newNotification.getExtraConfig().size() == 1;
            assert (Boolean) newNotification.getExtraConfig().get("detailedDiscovery") == false;
        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCRUDNotification() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);

        // Now add a notification
        try {

            AlertNotification notification = new AlertNotification("Direct Emails"); // short-name from server plugin descriptor
            notification.getConfig().put("emailAddress", "root@eruditorium.org");

            Integer nid =
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(notification)
                .pathParam("defId",definitionId)
            .expect()
                .statusCode(201)
                .log().ifError()
            .when()
                .post("/alert/definition/{defId}/notifications")
            .getBody()
                .jsonPath().get("id");

            // Update the notification
            notification.getConfig().put("emailAddress", "root@eruditorium.org,enoch@root.com");
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(notification)
                .pathParam("nid",nid)
            .expect()
                .statusCode(200)
                .log().ifError()
            .when()
                .put("/alert/notification/{nid}");


            // Retrieve the definition with the added condition
            AlertDefinition updatedDefinition =
            given()
                .pathParam("id",definitionId)
                .queryParam("full",true)
            .expect()
                .statusCode(200)
                .log().ifError()
            .when()
                .get("/alert/definition/{id}")
                .as(AlertDefinition.class);

            int size = updatedDefinition.getNotifications().size();
            assert size ==1 : "Did not find 1 notification, but " + size;

            // Need to use the updated id
            nid = updatedDefinition.getNotifications().get(0).getId();
            given()
                .pathParam("nid",nid)
            .expect()
                .statusCode(204)
            .when()
                .delete("/alert/notification/{nid}");

            // delete the notification
        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateUpdateConditionWithBadMetricDefinition() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);

        try {
            AlertCondition condition = new AlertCondition("THRESHOLD", "LESS_THAN");
            condition.setOption("12345");
            condition.setComparator(">");
            condition.setMeasurementDefinition(10173);

            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(condition)
                .pathParam("defId", definitionId)
            .expect()
                .statusCode(406)
                .log().ifError()
            .when()
                .post("/alert/definition/{defId}/conditions");
        } finally {
            cleanupDefinition(definitionId);
        }
    }

        @Test
    public void testCRUDCondition() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);
        int metricDefinitionId = findAMetricDefinitionForResourceId(_platformId, "metric");


        // Now add a condition
        try {

            AlertCondition condition = new AlertCondition("THRESHOLD", "LESS_THAN");
            condition.setOption("12345");
            condition.setComparator(">");
            condition.setMeasurementDefinition(metricDefinitionId);

            Integer cid =
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(condition)
                .pathParam("defId", definitionId)
            .expect()
                .statusCode(201)
                .log().ifError()
                .body("option",is("12345"))
                .body("comparator",is(">"))
            .when()
                .post("/alert/definition/{defId}/conditions")
            .getBody()
                .jsonPath().get("id");

            // Update the condition
            condition.setOption("23456");
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(condition)
                .pathParam("cid", cid)
            .expect()
                .statusCode(200)
                .log().ifError()
                .body("option", is("23456"))
                .body("comparator", is(">"))
            .when()
                .put("/alert/condition/{cid}");


            // Retrieve the definition with the added condition
            AlertDefinition updatedDefinition =
            given()
                .pathParam("id",definitionId)
                .queryParam("full",true)
            .expect()
                .statusCode(200)
                .log().everything()
            .when()
                .get("/alert/definition/{id}")
                .as(AlertDefinition.class);

            int size = updatedDefinition.getConditions().size();
            assert size ==1 : "Did not find 1 condition, but " + size;

            // Need to use the updated id
            cid = updatedDefinition.getConditions().get(0).getId();
            given()
                .pathParam("cid",cid)
            .expect()
                .statusCode(204)
            .when()
                .delete("/alert/condition/{cid}");

            // delete the notification
        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testGetNonExistingDefinition() throws Exception {

        given()
            .header(acceptJson)
            .pathParam("did",14)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .get("/alert/definition/{did}");

    }

    @Test
    public void testGetCachedDefinition() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);

        Response response =
        given()
            .header(acceptJson)
            .pathParam("did",definitionId)
        .expect()
            .statusCode(200)
            .log().everything()
        .when()
            .get("/alert/definition/{did}");

        String etag = response.getHeader("ETag");
        System.out.println(etag);

        // now pass the etag in to get a "no change" back
        given()
            .header(acceptJson)
            .header("If-none-match",etag)
            .pathParam("did",definitionId)
        .expect()
            .statusCode(304) // Not modified
            .log().everything()
        .when()
            .get("/alert/definition/{did}");

    }

    @Test
    public void testGetNonExistingCondition() throws Exception {

        given()
            .header(acceptJson)
            .pathParam("cid",14)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .get("/alert/condition/{cid}");

    }

    @Test
    public void testGetNonExistingNotification() throws Exception {

        given()
            .header(acceptXml)
            .pathParam("cid",14)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .get("/alert/notification/{cid}");

    }

    @Test
    public void testUpdateNonExistingCondition() throws Exception {

        given()
            .header(acceptJson)
            .pathParam("cid",14)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .put("/alert/condition/{cid}");

    }

    @Test
    public void testUpdateNonExistingNotification() throws Exception {

        given()
            .header(acceptXml)
            .pathParam("cid",14)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .put("/alert/notification/{cid}");

    }

    @Test
    public void testDeleteNonExistingDefinition() throws Exception {

        given()
            .header(acceptXml)
            .pathParam("cid",14)
        .expect()
            .statusCode(204)
            .log().ifError()
        .when()
            .delete("/alert/definition/{cid}");

    }

    @Test
    public void testDeleteNonExistingDefinitionWithValidate() throws Exception {

        given()
            .header(acceptXml)
            .pathParam("cid",14)
            .queryParam("validate",true)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .delete("/alert/definition/{cid}");

    }

    @Test
    public void testDeleteNonExistingNotification() throws Exception {

        given()
            .header(acceptJson)
            .pathParam("cid",14)
        .expect()
            .statusCode(204)
            .log().ifError()
        .when()
            .delete("/alert/notification/{cid}");
    }

    @Test
    public void testDeleteNonExistingNotificationWithValidate() throws Exception {

        given()
            .header(acceptJson)
            .pathParam("cid",14)
            .queryParam("validate",true)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .delete("/alert/notification/{cid}");
    }

    @Test
    public void testDeleteNonExistingCondition() throws Exception {

        given()
            .header(acceptJson)
            .pathParam("cid",14)
        .expect()
            .statusCode(204)
            .log().ifError()
        .when()
            .delete("/alert/condition/{cid}");
    }

    @Test
    public void testDeleteNonExistingConditionWithValidate() throws Exception {

        given()
            .header(acceptJson)
            .pathParam("cid",14)
            .queryParam("validate",true)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .delete("/alert/condition/{cid}");
    }

    @Test
    public void testCreateDeleteAlertDefinitionWithUnknwonSender() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);

        // Now add a condition
        try {

            AlertNotification notification = new AlertNotification("Frobnitz"); // short-name from server plugin descriptor

            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(notification)
                .pathParam("defId",definitionId)
            .expect()
                .statusCode(404)
                .log().everything()
            .when()
                .post("/alert/definition/{defId}/notifications");
        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateDeleteAlertDefinitionWithNoPriority() throws Exception {

        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setName("-x-test-definition");
        alertDefinition.setEnabled(false);
        alertDefinition.setPriority("LOW");
        alertDefinition.setDampeningCategory("NONE");

        AlertDefinition result =
        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .body(alertDefinition)
            .queryParam("resourceId",_platformId)
        .expect()
            .statusCode(201)
            .log().ifError()
        .when()
            .post("/alert/definitions")
        .as(AlertDefinition.class);

        int definitionId = result.getId();

        // Now update with no priority
        try {
            alertDefinition.setId(definitionId);
            alertDefinition.setPriority(null);

            alertDefinition =
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(alertDefinition)
                .pathParam("defId",definitionId)
            .expect()
                .statusCode(200)
                .log().ifError()
            .when()
                .put("/alert/definition/{defId}")
            .as(AlertDefinition.class);

            assert alertDefinition.getPriority().equals("LOW");
        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateDeleteAlertDefinitionWith2Notifications() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);

        // Now add a condition
        try {

            AlertNotification notification = new AlertNotification("Direct Emails"); // short-name from server plugin descriptor
            notification.getConfig().put("emailAddress", "root@eruditorium.org");

            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(notification)
                .pathParam("defId",definitionId)
            .expect()
                .statusCode(201)
                .log().everything()
            .when()
                .post("/alert/definition/{defId}/notifications");

            // Retrieve the definition with the added condition
            AlertDefinition updatedDefinition =
            given()
                .pathParam("id",definitionId)
                .queryParam("full",true)
            .expect()
                .statusCode(200)
                .log().everything()
            .when()
                .get("/alert/definition/{id}")
                .as(AlertDefinition.class);

            int size = updatedDefinition.getNotifications().size();
            assert size ==1 : "Did not find 1 notifications, but " + size;

            AlertNotification secondNotification = new AlertNotification("System Roles");
            secondNotification.getConfig().put("roleId","|1]");
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(secondNotification)
                .pathParam("defId",definitionId)
            .expect()
                .statusCode(201)
                .log().everything()
            .when()
                .post("/alert/definition/{defId}/notifications");

            // Retrieve the definition with the added condition
            updatedDefinition =
            given()
                .pathParam("id",definitionId)
                .queryParam("full",true)
            .expect()
                .statusCode(200)
                .log().everything()
            .when()
                .get("/alert/definition/{id}")
                .as(AlertDefinition.class);

            size = updatedDefinition.getNotifications().size();
            assert size ==2 : "Did not find 2 notifications, but " + size;

        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testNewFullDefinition() throws Exception {

        int definitionId = 0;
        try {
            AlertDefinition alertDefinition = new AlertDefinition();
            alertDefinition.setName("-x-test-full-definition");
            alertDefinition.setEnabled(false);
            alertDefinition.setPriority("HIGH");

            AlertNotification notification = new AlertNotification("Direct Emails");
            notification.getConfig().put("emailAddress","enoch@root.org");
            alertDefinition.getNotifications().add(notification);

            AlertCondition condition = new AlertCondition("AVAILABILITY", "AVAIL_GOES_DOWN");
            alertDefinition.getConditions().add(condition);

            AlertDefinition result =
            given()
                .contentType(ContentType.JSON)
                .header(acceptJson)
                .body(alertDefinition)
                .log().everything()
                .queryParam("resourceId", _platformId)
            .expect()
                .statusCode(201)
                .log().ifError()
            .when()
                .post("/alert/definitions")
            .as(AlertDefinition.class);

            assert result != null;
            definitionId = result.getId();

            assert result.getConditions().size()==1;
            assert result.getNotifications().size()==1;

            // Now retrieve the condition and notification individually

            given()
                .header(acceptJson)
                .pathParam("id",result.getNotifications().get(0).getId())
            .expect()
                .statusCode(200)
                .body("id",is(result.getNotifications().get(0).getId()))
                .body("senderName",is(result.getNotifications().get(0).getSenderName()))
                .log().ifError()
            .when()
                .get("/alert/notification/{id}");

            given()
                .header(acceptJson)
                .pathParam("id",result.getConditions().get(0).getId())
            .expect()
                .statusCode(200)
                .body("id",is(result.getConditions().get(0).getId()))
                .body("name",is(result.getConditions().get(0).getName()))
                .log().ifError()
            .when()
                .get("/alert/condition/{id}");


        } finally {
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testNewFullDefinition2() throws Exception {

        int definitionId = 0;
        try {
            AlertDefinition alertDefinition = new AlertDefinition();
            alertDefinition.setName("-x-test-full-definition");
            alertDefinition.setEnabled(false);
            alertDefinition.setPriority("HIGH");

            AlertNotification notification = new AlertNotification("Direct Emails");
            notification.getConfig().put("emailAddress","enoch@root.org");
            alertDefinition.getNotifications().add(notification);

            List<AlertCondition> conditions = alertDefinition.getConditions();

            AlertCondition condition = new AlertCondition("AVAILABILITY", "AVAIL_GOES_DOWN");
            conditions.add(condition);

            condition = new AlertCondition("AVAIL_DURATION","AVAIL_DURATION_DOWN");
            condition.setOption("300"); // seconds
            conditions.add(condition);

            AlertDefinition result =
            given()
                .contentType(ContentType.JSON)
                .header(acceptJson)
                .body(alertDefinition)
                .log().everything()
                .queryParam("resourceId", _platformId)
            .expect()
                .statusCode(201)
                .log().ifError()
            .when()
                .post("/alert/definitions")
            .as(AlertDefinition.class);

            assert result != null;
            definitionId = result.getId();

            int numberConditions = result.getConditions().size();
            assert numberConditions ==2 : "Expected 2 conditions but got " + numberConditions;
            assert result.getNotifications().size()==1;

            // Now retrieve the condition and notification individually

            given()
                .header(acceptJson)
                .pathParam("id",result.getNotifications().get(0).getId())
            .expect()
                .statusCode(200)
                .body("id",is(result.getNotifications().get(0).getId()))
                .body("senderName",is(result.getNotifications().get(0).getSenderName()))
                .log().ifError()
            .when()
                .get("/alert/notification/{id}");

            given()
                .header(acceptJson)
                .pathParam("id",result.getConditions().get(0).getId())
            .expect()
                .statusCode(200)
                .body("id",is(result.getConditions().get(0).getId()))
                .body("name",is(result.getConditions().get(0).getName()))
                .log().ifError()
            .when()
                .get("/alert/condition/{id}");


        } finally {
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testNewFullDefinitionPlusRemovals() throws Exception {

        int definitionId = 0;
        try {
            AlertDefinition alertDefinition = new AlertDefinition();
            alertDefinition.setName("-x-test-full-definition2");
            alertDefinition.setEnabled(false);
            alertDefinition.setPriority("HIGH");

            AlertNotification notification = new AlertNotification("Direct Emails");
            notification.getConfig().put("emailAddress","enoch@root.org");
            alertDefinition.getNotifications().add(notification);

            AlertCondition condition = new AlertCondition("AVAILABILITY", "AVAIL_GOES_DOWN");
            alertDefinition.getConditions().add(condition);

            AlertDefinition result =
            given()
                .contentType(ContentType.JSON)
                .header(acceptJson)
                .body(alertDefinition)
                .queryParam("resourceId", _platformId)
            .expect()
                .statusCode(201)
                .body("priority", is("HIGH"))
                .body("conditions", iterableWithSize(1))
                .body("notifications", iterableWithSize(1))
                .body("name", is("-x-test-full-definition2"))
                .log().everything()
            .when()
                .post("/alert/definitions")
            .as(AlertDefinition.class);

            assert result != null;
            definitionId = result.getId();
            System.out.println("Definition id: " + definitionId);


            // Now retrieve the condition and notification individually

            given()
                .header(acceptJson)
                .pathParam("id",result.getNotifications().get(0).getId())
            .expect()
                .statusCode(204)
                .log().ifError()
            .when()
                .delete("/alert/notification/{id}");


            //retrieve definition again to see if notification is really gone
            AlertDefinition result2 =
            given()
                .contentType(ContentType.JSON)
                .header(acceptJson)
                .pathParam("did", definitionId)
                .queryParam("full", true)
            .expect()
                .statusCode(200)
                .body("conditions", iterableWithSize(1))
                .body("notifications", iterableWithSize(0))
                .body("name",is("-x-test-full-definition2"))
                .body("priority",is("HIGH"))
                .log().everything()
            .when()
                .get("/alert/definition/{did}")
            .as(AlertDefinition.class);

            assert result2.getId() == result.getId();

            // Now also remove the condition
            int conditionId = result2.getConditions().get(0).getId(); //


            System.out.println("Condition id " + conditionId +  " result-> " + result.getConditions().get(0).getId());
            given()
                .header(acceptJson)
                .pathParam("id", conditionId)
            .expect()
                .statusCode(204)
                .log().ifError()
            .when()
                .delete("/alert/condition/{id}");

            //retrieve definition again to see if notification is really gone
            given()
                .contentType(ContentType.JSON)
                .header(acceptJson)
                .pathParam("did", definitionId)
                .queryParam("full", true)
            .expect()
                .statusCode(200)
                .body("conditions", iterableWithSize(0))
                .body("notifications", iterableWithSize(0))
                .body("name",is("-x-test-full-definition2"))
                .body("priority",is("HIGH"))
                .log().everything()
            .when()
                .get("/alert/definition/{did}");


        } finally {
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testUpdateDefinition() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);
        try {
            AlertDefinition definition =
            given()
                .header(acceptJson)
                .pathParam("did",definitionId)
            .expect()
                .statusCode(200)
            .when()
                .get("/alert/definition/{did}")
            .as(AlertDefinition.class);

            definition.setEnabled(true);
            definition.setDampeningCategory("ONCE");

            given()
                .contentType(ContentType.JSON)
                .header(acceptJson)
                .body(definition)
                .pathParam("did", definitionId)
            .expect()
                .statusCode(200)
                .log().ifError()
                .body("enabled", is(true))
                .body("dampeningCategory",is("ONCE"))
            .when()
                .put("/alert/definition/{did}");

        }
        finally {
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testUpdateNonExistingDefinition() throws Exception {

        given()
            .contentType(ContentType.JSON)
            .header(acceptJson)
            .pathParam("did",123)
        .expect()
            .statusCode(404)
            .log().ifError()
        .when()
            .put("/alert/definition/{did}");

    }

    @Test
    public void testUpdateDefinitionWithBadRecoveryId() throws Exception {

        int definitionId = createEmptyAlertDefinition(false);
        try {
            AlertDefinition definition =
            given()
                .header(acceptJson)
                .pathParam("did",definitionId)
            .expect()
                .statusCode(200)
            .when()
                .get("/alert/definition/{did}")
            .as(AlertDefinition.class);

            definition.setRecoveryId(43);

            given()
                .contentType(ContentType.XML)
                .header(acceptJson)
                .body(definition)
                .pathParam("did",definitionId)
            .expect()
                .statusCode(404)
                .log().ifError()
            .when()
                .put("/alert/definition/{did}");

        }
        finally {
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateDefinitionForResourceAndGroup() throws Exception {

        // This is supposed to fail, as we specify both a resource and a group
        // to work on

        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setName("-x-test-definition");

        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .body(alertDefinition)
            .queryParam("resourceId",10001)
            .queryParam("groupId",10001)
        .expect()
            .statusCode(406)
            .log().ifError()
        .when()
            .post("/alert/definitions");
    }

    @Test
    public void testCreateDefinitionForGroup() throws Exception {

        assert _platformTypeId!=0 : "Set up did not run or failed";

        // Create a group
        Group group = new Group("test-group-" + System.currentTimeMillis()/1000);
        group.setCategory("COMPATIBLE");
        group.setResourceTypeId(_platformTypeId);

        String groupUri =
        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .body(group)
        .expect()
            .statusCode(201)
            .log().ifError()
        .when()
            .post("/group/")
        .header("Location");

        int groupId = Integer.parseInt(groupUri.substring(groupUri.lastIndexOf("/")+1));

        int definitionId = 0;
        try {
            AlertDefinition alertDefinition = new AlertDefinition();
            alertDefinition.setName("-x-test-definition");

            alertDefinition =
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(alertDefinition)
                .queryParam("groupId", groupId)
            .expect()
                .statusCode(201)
                .log().ifError()
            .when()
                .post("/alert/definitions")
            .as(AlertDefinition.class);

            definitionId = alertDefinition.getId();
        } finally {
            cleanupDefinition(definitionId);
            delete(groupUri);
        }
    }

    @Test
    public void testCreateDefinitionForResourceType() throws Exception {

        // This is supposed to fail, as we specify both a resource and a group
        // to work on

        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setName("-x-test-definition");

        AlertDefinition result =
        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .body(alertDefinition)
            .queryParam("resourceTypeId",_platformTypeId)
        .expect()
            .statusCode(201)
            .log().ifError()
        .when()
            .post("/alert/definitions")
        .as(AlertDefinition.class);

        cleanupDefinition(result.getId());
    }

    @Test
    public void testCreateDeleteAlertDefinitionWith1ConditionAndFire() throws Exception {

        int definitionId = createEmptyAlertDefinition(true);

        int alertId;

        // Now add a condition
        try {

            AlertCondition alertCondition = new AlertCondition("AVAILABILITY", "AVAIL_GOES_UP");
            addConditionToDefinition(definitionId, alertCondition);

            System.out.println("Definition created, waiting 60s for it to become active");

            // Wait a while - see https://bugzilla.redhat.com/show_bug.cgi?id=830299
            Thread.sleep(60*1000);

            // Send a avail down/up sequence -> alert definition should fire
            long now = System.currentTimeMillis();
            Availability a = new Availability(_platformId,now-2000,"DOWN");
            given()
                .contentType(ContentType.JSON)
                .pathParam("id", _platformId)
                .body(a)
            .expect()
                .statusCode(204)
                .log().ifError()
            .when()
                .put("/resource/{id}/availability");

            a = new Availability(_platformId,now-1000,"UP");
            given()
                .contentType(ContentType.JSON)
                .pathParam("id", _platformId)
                .body(a)
            .expect()
                .statusCode(204)
                .log().ifError()
            .when()
                .put("/resource/{id}/availability");

            // wait a little
            Thread.sleep(5000);

            alertId =
            given()
                .header(acceptJson)
                .queryParam("definitionId",definitionId)
                .queryParam("since", now - 3000)
            .expect()
                .statusCode(200)
                .log().ifError()
                .body("alertDefinition.name",contains("-x-test-definition"))
                .body("",iterableWithSize(1))
            .when()
                .get("/alert")
            .body().jsonPath().getInt("id[0]");

            System.out.println(alertId);

            // Find this alert by id and then its condition logs and notification logs
            given()
                .header(acceptJson)
                .pathParam("id",alertId)
                .log().everything()
            .expect()
                .statusCode(200)
                .log().ifError()
                .body("id",is(alertId))
            .when()
                .get("/alert/{id}");


            given()
                .header(acceptJson)
                .pathParam("id", alertId)
                .log().everything()
            .expect()
                .statusCode(200)
                .log().ifError()
                .body("",iterableWithSize(1))
            .when()
                .get("/alert/{id}/conditions");

            given()
                .header(acceptJson)
                .pathParam("id", alertId)
            .expect()
                .statusCode(200)
                .log().ifError()
                .body("",iterableWithSize(0))
            .when()
                .get("/alert/{id}/notifications");


            // See if the resource has an alert recorded
            given()
                .header(acceptJson)
                .pathParam("resourceId",_platformId)
            .expect()
                .statusCode(200)
                .log().ifError()
                .body("",not(emptyIterable()))
            .when()
                .get("/resource/{resourceId}/alerts");

            if (alertId>0) {
                // Acknowledge the alert
                given()
                    .header(acceptWrappedJson)
                    .pathParam("id", alertId)
                .expect()
                    .statusCode(200)
                    .log().ifError()
                .when()
                    .put("/alert/{id}");

                Thread.sleep(500);

                // purge the alert
                given()
                    .header(acceptJson)
                    .pathParam("id", alertId)
                .expect()
                    .statusCode(204)
                    .log().ifError()
                .when()
                    .delete("/alert/{id}");
            }
        }
        finally {

            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }


    @Test
    public void testCreateDeleteAlertDefinitionWith1ConditionAndNotificationAndFire() throws Exception {

        int definitionId = createEmptyAlertDefinition(true);

        // Now add a condition
        try {

            AlertCondition alertCondition = new AlertCondition("AVAILABILITY", "AVAIL_GOES_UP");
            addConditionToDefinition(definitionId, alertCondition);

            AlertNotification notification = new AlertNotification("Direct Emails"); // short-name from server plugin descriptor
            notification.getConfig().put("emailAddress", "root@eruditorium.org");

            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(notification)
                .pathParam("defId", definitionId)
                .log().everything()
            .expect()
                .statusCode(201)
                .log().ifError()
            .when()
                .post("/alert/definition/{defId}/notifications");


            System.out.println("Definition created, waiting 60s for it to become active");

            // Wait a while - see https://bugzilla.redhat.com/show_bug.cgi?id=830299
            Thread.sleep(60*1000);

            // Send a avail down/up sequence -> alert definition should fire
            long now = System.currentTimeMillis();
            Availability a = new Availability(_platformId,now-2000,"DOWN");
            given()
                .contentType(ContentType.JSON)
                .pathParam("id", _platformId)
                .body(a)
            .expect()
                .statusCode(204)
                .log().ifError()
            .when()
                .put("/resource/{id}/availability");

            a = new Availability(_platformId,now-1000,"UP");
            given()
                .contentType(ContentType.JSON)
                .pathParam("id", _platformId)
                .body(a)
            .expect()
                .statusCode(204)
                .log().ifError()
            .when()
                .put("/resource/{id}/availability");

            // wait a little
            Thread.sleep(5000);

            int alertId =
            given()
                .header(acceptJson)
                .queryParam("definitionId",definitionId)
                .queryParam("since", now - 3000)
            .expect()
                .statusCode(200)
                .log().ifError()
                .body("alertDefinition.name",contains("-x-test-definition"))
                .body("",iterableWithSize(1))
            .when()
                .get("/alert")
            .body().jsonPath().getInt("id[0]");

            System.out.println(alertId);

            // Find this alert by id and then its condition logs and notification logs
            given()
                .header(acceptJson)
                .pathParam("id",alertId)
            .expect()
                .statusCode(200)
                .log().ifError()
                .body("id",is(alertId))
            .when()
                .get("/alert/{id}");


            given()
                .header(acceptJson)
                .pathParam("id", alertId)
                .log().everything()
            .expect()
                .statusCode(200)
                .log().ifError()
                .body("",iterableWithSize(1))
            .when()
                .get("/alert/{id}/conditions");

            given()
                .header(acceptJson)
                .pathParam("id", alertId)
            .expect()
                .statusCode(200)
                .log().ifError()
                .body("",iterableWithSize(1))
            .when()
                .get("/alert/{id}/notifications");


        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }

    @Test
    public void testCreateDeleteAlertDefinitionWithManyConditionsAndFire() throws Exception {

        int definitionId = createEmptyAlertDefinition(true);

        // Now add a condition
        try {

            AlertCondition alertCondition = new AlertCondition("AVAILABILITY", "AVAIL_GOES_UP");
            addConditionToDefinition(definitionId, alertCondition);

            alertCondition = new AlertCondition("EVENT","ERROR");
            alertCondition.setOption(".*JBAS123.*");
            addConditionToDefinition(definitionId, alertCondition);

            int metricDef = findAMetricDefinitionForResourceId(_platformId, "metric");
            assert metricDef != 0;
            alertCondition = new AlertCondition("THRESHOLD");
            alertCondition.setComparator("<");
            alertCondition.setThreshold(12345.0);
            alertCondition.setMeasurementDefinition(metricDef);
            addConditionToDefinition(definitionId, alertCondition);

            alertCondition = new AlertCondition("BASELINE");
            alertCondition.setThreshold(0.5);
            alertCondition.setOption("mean");
            alertCondition.setComparator("<");
            alertCondition.setMeasurementDefinition(metricDef);
            addConditionToDefinition(definitionId, alertCondition);

            alertCondition = new AlertCondition("AVAIL_DURATION","AVAIL_DURATION_DOWN");
            alertCondition.setOption("240"); // 4 min
            addConditionToDefinition(definitionId, alertCondition);

            alertCondition = new AlertCondition("CHANGE");
            alertCondition.setMeasurementDefinition(metricDef);
            addConditionToDefinition(definitionId, alertCondition);

            int traitDef = findAMetricDefinitionForResourceId(_platformId, "trait");
            assert traitDef!=0;
            alertCondition = new AlertCondition("TRAIT");
            alertCondition.setOption("10.*");
            alertCondition.setMeasurementDefinition(traitDef);
            addConditionToDefinition(definitionId, alertCondition);

            alertCondition = new AlertCondition("RANGE");
            alertCondition.setMeasurementDefinition(metricDef);
            alertCondition.setThreshold(4.0); // lower bound
            alertCondition.setOption("7.0"); // upper bound
            alertCondition.setComparator(">=");
            addConditionToDefinition(definitionId, alertCondition);

            alertCondition = new AlertCondition("CONTROL","discovery");
            alertCondition.setOption("FAILURE");
            addConditionToDefinition(definitionId, alertCondition);

            System.out.println("Definition created, waiting 60s for it to become active");

            // Wait a while - see https://bugzilla.redhat.com/show_bug.cgi?id=830299
            Thread.sleep(60*1000);

            // Send a avail down/up sequence -> alert definition should fire
            long now = System.currentTimeMillis();
            Availability a = new Availability(_platformId,now-2000,"DOWN");
            given()
                .contentType(ContentType.JSON)
                .pathParam("id", _platformId)
                .body(a)
            .expect()
                .statusCode(204)
                .log().ifError()
            .when()
                .put("/resource/{id}/availability");

            a = new Availability(_platformId,now-1000,"UP");
            given()
                .contentType(ContentType.JSON)
                .pathParam("id", _platformId)
                .body(a)
            .expect()
                .statusCode(204)
                .log().ifError()
            .when()
                .put("/resource/{id}/availability");

            // wait a little
            Thread.sleep(5000);

            int alertId =
            given()
                .header(acceptJson)
                .queryParam("definitionId",definitionId)
                .queryParam("since", now - 3000)
            .expect()
                .statusCode(200)
                .log().ifError()
                .body("alertDefinition.name",contains("-x-test-definition"))
                .body("",iterableWithSize(1))
            .when()
                .get("/alert")
            .body().jsonPath().getInt("id[0]");

            System.out.println(alertId);

            // Find this alert by id and then its condition logs and notification logs
            given()
                .header(acceptJson)
                .pathParam("id",alertId)
            .expect()
                .statusCode(200)
                .log().ifError()
                .body("id",is(alertId))
            .when()
                .get("/alert/{id}");


            // Check that one condition matched in the generated alert
            given()
                .header(acceptJson)
                .pathParam("id", alertId)
            .expect()
                .statusCode(200)
                .log().ifError()
                .body("", iterableWithSize(1))
            .when()
                .get("/alert/{id}/conditions");

            // Check that the definition indeed has 5 conditions
            given()
                .header(acceptJson)
                .pathParam("id", definitionId)
            .expect()
                .statusCode(200)
                .body("conditions",iterableWithSize(9))
            .when()
                .get("/alert/definition/{id}");

        }

        finally {
            // delete the definition again
            cleanupDefinition(definitionId);
        }
    }

    private void addConditionToDefinition(int definitionId, AlertCondition alertCondition) {
        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .body(alertCondition)
            .pathParam("defId", definitionId)
        .expect()
            .statusCode(201)
            .log().ifError()
        .when()
            .post("/alert/definition/{defId}/conditions");
    }

    private void cleanupDefinition(int definitionId) {

        if (definitionId==0)
            return;

        given()
            .pathParam("id", definitionId)
        .expect()
            .statusCode(204)
        .when()
            .delete("/alert/definition/{id}");
    }

    private int createEmptyAlertDefinition(boolean enabled) {
        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setName("-x-test-definition");
        alertDefinition.setEnabled(enabled);
        alertDefinition.setPriority("LOW");
        alertDefinition.setDampeningCategory("NONE");

        Response response =
        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .body(alertDefinition)
            .queryParam("resourceId", _platformId)
        .expect()
            .statusCode(201)
            .log().ifError()
        .when()
            .post("/alert/definitions");

        AlertDefinition result = response.as(AlertDefinition.class);

        assert result.getConditions()==null || result.getConditions().size()==0;

        return result.getId();
    }
}
