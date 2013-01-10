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

import com.jayway.restassured.http.ContentType;

import org.junit.Test;

import org.rhq.modules.integrationTests.restApi.d.AlertCondition;
import org.rhq.modules.integrationTests.restApi.d.AlertDefinition;
import org.rhq.modules.integrationTests.restApi.d.AlertNotification;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;

/**
 * Testing of the Alerting part of the rest-api
 * @author Heiko W. Rupp
 */
public class AlertTest extends AbstractBase {

    @Test
    public void testListAllAlerts() throws Exception {

        expect()
            .statusCode(200)
        .when()
            .get("/alert");

    }

    @Test
    public void testGetAlertCount() throws Exception {

        expect()
            .statusCode(200)
        .when()
            .get("/alert/count");

    }

    @Test
    public void testListAllAlertDefinitions() throws Exception {

        expect()
            .statusCode(200)
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
    public void testGetUnknownSenderByName() throws Exception {
        given()
            .header(acceptJson)
            .pathParam("name","Frobnitz")
        .expect()
            .statusCode(404)
            .log().everything()
        .when()
            .get("/alert/sender/{name}");
    }

    @Test
    public void testCreateDeleteBasicAlertDefinition() throws Exception {

        int definitionId = createEmptyAlertDefinition();

        cleanupDefinition(definitionId);
    }

    @Test
    public void testCreateDeleteAlertDefinitionWith1Condition() throws Exception {

        int definitionId = createEmptyAlertDefinition();

        // Now add a condition
        try {

            AlertCondition alertCondition = new AlertCondition("AVAIL_GOES_UP","AVAILABILITY");
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(alertCondition)
                .pathParam("defId",definitionId)
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
    public void testCreateDeleteAlertDefinitionWith2Conditions() throws Exception {

        int definitionId = createEmptyAlertDefinition();


        try {
            // Now add a 1st condition
            AlertCondition alertCondition = new AlertCondition("AVAIL_GOES_UP","AVAILABILITY");
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(alertCondition)
                .pathParam("defId",definitionId)
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

            // Now add a 2nd condition
            AlertCondition secondCondition = new AlertCondition("AVAIL_GOES_DOWN","AVAILABILITY");
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(secondCondition)
                .pathParam("defId",definitionId)
            .expect()
                .statusCode(201)
                .log().ifError()
            .when()
                .post("/alert/definition/{defId}/conditions");

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

        int definitionId = createEmptyAlertDefinition();

        // Now add a condition
        try {

            AlertNotification notification = new AlertNotification("Direct Emails"); // short-name from server plugin descriptor
            notification.getConfig().put("emailAddress", "root@eruditorium.org");

            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(notification)
                .pathParam("defId",definitionId)
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
    public void testCreateDeleteAlertDefinitionWithUnknwonSender() throws Exception {

        int definitionId = createEmptyAlertDefinition();

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
    public void testCreateDeleteAlertDefinitionWith2Notifications() throws Exception {

        int definitionId = createEmptyAlertDefinition();

        // Now add a condition
        try {

            AlertNotification notification = new AlertNotification("Direct Emails"); // short-name from server plugin descriptor
            notification.getConfig().put("emailAddress","root@eruditorium.org");

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

            AlertCondition condition = new AlertCondition("AVAIL_GOES_DOWN","AVAILABILITY");
            alertDefinition.getConditions().add(condition);

            AlertDefinition result =
            given()
                .contentType(ContentType.JSON)
                .header(acceptJson)
                .body(alertDefinition)
                .log().everything()
                .queryParam("resourceId", 10001)
            .expect()
                .statusCode(201)
                .log().everything()
            .when()
                .post("/alert/definitions")
            .as(AlertDefinition.class);

            assert result != null;
            definitionId = result.getId();

            assert result.getConditions().size()==1;
            assert result.getNotifications().size()==1;
        } finally {
//            cleanupDefinition(definitionId);
        }

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

    private int createEmptyAlertDefinition() {
        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setName("-x-test-definition");
        alertDefinition.setEnabled(false);
        alertDefinition.setPriority("LOW");

        AlertDefinition result =
        given()
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .body(alertDefinition)
            .queryParam("resourceId",10001)
        .expect()
            .statusCode(201)
            .log().ifError()
        .when()
            .post("/alert/definitions")
        .as(AlertDefinition.class);

        assert result.getConditions()==null || result.getConditions().size()==0;

        return result.getId();
    }
}
