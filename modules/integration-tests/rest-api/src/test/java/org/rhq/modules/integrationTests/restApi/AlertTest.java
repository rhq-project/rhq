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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;

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
    public void testCRUDNotification() throws Exception {

        int definitionId = createEmptyAlertDefinition();

        // Now add a condition
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
    public void testCRUDCondition() throws Exception {

        int definitionId = createEmptyAlertDefinition();

        // Now add a condition
        try {

            AlertCondition condition = new AlertCondition("LESS_THAN","THRESHOLD");
            condition.setOption("12345");
            condition.setComparator(">");
            condition.setMeasurementDefinition(10173);

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
                .pathParam("cid",cid)
            .expect()
                .statusCode(200)
                .log().ifError()
                .body("option",is("23456"))
                .body("comparator",is(">"))
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

            AlertCondition condition = new AlertCondition("AVAIL_GOES_DOWN","AVAILABILITY");
            alertDefinition.getConditions().add(condition);

            AlertDefinition result =
            given()
                .contentType(ContentType.JSON)
                .header(acceptJson)
                .body(alertDefinition)
                .queryParam("resourceId", 10001)
            .expect()
                .statusCode(201)
                .body("priority", is("HIGH"))
                .body("conditions", iterableWithSize(1))
                .body("notifications", iterableWithSize(1))
                .body("name",is("-x-test-full-definition2"))
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
