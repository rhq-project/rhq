/*
 * RHQ Management Platform
 *  Copyright (C) 2005-2013 Red Hat, Inc.
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

import java.util.List;
import java.util.Map;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;

import org.junit.Before;
import org.junit.Test;

import org.rhq.modules.integrationTests.restApi.d.Link;
import org.rhq.modules.integrationTests.restApi.d.Operation;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

/**
 * Test the operations part of the rest api
 * @author Heiko W. Rupp
 */
public class OperationsTest extends AbstractBase {

    private int discoveryDefinitionId;
    private int viewPLDefinitionId;

    @Before
    public void setUp() throws Exception {
        super.setUp();

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
            if (name.equals("viewProcessList")) {
                viewPLDefinitionId = id;
            }
        }

        assert discoveryDefinitionId !=-1 : "No discovery operation found";
    }

    @Test
    public void testGetDefinitionById() throws Exception {

        // Now retrieve that definition by id

        given()
            .header(acceptJson)
            .pathParam("did", discoveryDefinitionId)
        .expect()
            .statusCode(200)
            .body("name",is("discovery"))
        .when()
            .get("/operation/definition/{did}");

     }

    @Test
    public void testGetDefinitionByUnknownId() throws Exception {

        // Now retrieve that definition by id

        given()
            .header(acceptJson)
            .pathParam("did", -42)
        .expect()
            .statusCode(404)
        .when()
            .get("/operation/definition/{did}");

     }

    @Test
    public void testGetDefinitionsForUnknownResource() throws Exception {

        // Now retrieve that definition by id

        given()
            .header(acceptJson)
            .queryParam("resourceId", 42)
        .expect()
            .statusCode(404)
        .when()
            .get("/operation/definitions");
     }

    @Test
    public void testGetDefinitionsForMissingResourceId() throws Exception {

        // Now retrieve that definition by id

        given()
            .header(acceptJson)
        .expect()
            .statusCode(406)
        .when()
            .get("/operation/definitions");
     }

    @Test
    public void testCreateScheduleByUnknownDefinitionId() throws Exception {

        // Now retrieve that definition by id

        given()
            .header(acceptJson)
            .pathParam("did", -42)
        .expect()
            .statusCode(406)
        .when()
            .post("/operation/definition/{did}");

     }

    @Test
    public void testCreateScheduleForUnknownResource() throws Exception {

        // Now retrieve that definition by id

        given()
            .header(acceptJson)
            .queryParam("resourceId", 42)
            .pathParam("definitionId", discoveryDefinitionId)
        .expect()
            .statusCode(404)
        .when()
            .post("/operation/definition/{definitionId}");
     }

    @Test
    public void testCreateScheduleForMissingResourceId() throws Exception {

        // Now retrieve that definition by id

        given()
            .header(acceptJson)
            .pathParam("definitionId", discoveryDefinitionId)
        .expect()
            .statusCode(406)
        .when()
            .post("/operation/definition/{definitionId}");
     }

    @Test
    public void testCreateDraftOperation() throws Exception {

        Operation draft = getADraftOperation(_platformId, discoveryDefinitionId);

        int draftId = draft.getId();

        // check if we can retrieve one single draft

        Operation op = get("/operation/" + draftId).as(Operation.class);
        assert op !=null;
        assert op.equals(draft);

    }

    @Test
    public void testCreateAndUpdateDraftOperation() throws Exception {

        Operation draft = getADraftOperation(_platformId, discoveryDefinitionId);

        int draftId = draft.getId();
        draft.getParams().put("detailedDiscovery",true);

        try {
            given()
                .contentType(ContentType.JSON)
                .pathParam("id", draftId)
                .body(draft)
                .log().everything()
            .expect()
                .statusCode(200)
                .log().ifError()
            .when()
                .put("/operation/{id}");
        } finally {
            // delete the draft again
            expect()
                .statusCode(204)
            .when()
                .delete("/operation/" + draftId);
        }
    }

    @Test
    public void testCatchBadLinkSerialization() throws Exception {

        // Test that when we get Links back in bad format, we
        // correctly bail out.

        Operation draft = getADraftOperation(_platformId, discoveryDefinitionId);

        int draftId = draft.getId();
        draft.getParams().put("detailedDiscovery",true);

        String jsonWithBadLinkSer = //
        "{\n" +
            "    \"id\": " + draftId + ",\n" +
            "    \"name\": \"discovery\",\n" +
            "    \"readyToSubmit\": false,\n" +
            "    \"resourceId\": " + _platformId + ",\n" +
            "    \"definitionId\": " + discoveryDefinitionId + ",\n" +
            "    \"params\": {\n" +
            "        \"detailedDiscovery\": true\n" +
            "    },\n" +
            "    \"links\": [\n" +
            "        {\n" +
            "            \"rel\": \"edit\",\n" +
            "            \"href\": \"http://localhost:7080/rest/operation/" + draftId + "\"\n" +
            "        }\n" +
            "    ]\n" +
            "}";

        try {
            given()
                .contentType(ContentType.JSON)
                .pathParam("id", draftId)
                .body(jsonWithBadLinkSer)
                .log().everything()
            .expect()
                .statusCode(503)
                .log().ifError()
            .when()
                .put("/operation/{id}");
        } finally {
            // delete the draft again
            expect()
                .statusCode(204)
            .when()
                .delete("/operation/" + draftId);
        }


    }



    @Test
    public void testCreateDraftOperationAndScheduleExecution() throws Exception {

        int platformId = findIdOfARealPlatform();

        Operation draft = getADraftOperation(platformId, discoveryDefinitionId);

        int draftId = draft.getId();

        draft.setReadyToSubmit(true);
        draft.getParams().put("detailedDiscovery", false);

        // update to schedule
        Operation scheduled =
        given()
            .contentType(ContentType.JSON)
            .pathParam("id",draftId)
            .body(draft)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .put("/operation/{id}")
        .as(Operation.class);

        System.out.println(scheduled.getId());
        String history = findHistoryItem(scheduled);

        String historyId = history.substring(history.lastIndexOf("/")+1);
        try {
            waitAndCheckStatus(platformId, historyId);

        } finally {

            // Wait until the operation has finished and then delete
            waitForTerminationAndDelete(historyId);

        }
    }

    @Test
    public void testCreateDraftOperationNoParamsAndScheduleExecution() throws Exception {

        int platformId = findIdOfARealPlatform();

        Operation draft = getADraftOperation(platformId, viewPLDefinitionId);

        int draftId = draft.getId();

        draft.setReadyToSubmit(true);

        // update to schedule
        Operation scheduled =
        given()
            .contentType(ContentType.JSON)
            .pathParam("id",draftId)
            .body(draft)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .put("/operation/{id}")
        .as(Operation.class);

        System.out.println(scheduled.getId());
        String history = findHistoryItem(scheduled);

        String historyId = history.substring(history.lastIndexOf("/")+1);
        try {
            waitAndCheckStatus(platformId, historyId);

        } finally {

            // Wait until the operation has finished and then delete
            waitForTerminationAndDelete(historyId);

        }
    }

    private Operation getADraftOperation(int platformId, int definitionId) {
        Operation draft =
        given()
            .header(acceptJson)
            .pathParam("definitionId", definitionId)
            .queryParam("resourceId",platformId)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .post("/operation/definition/{definitionId}")
        .as(Operation.class);

        assert draft != null;
        assert draft.getDefinitionId() == definitionId;

        System.out.println("--- Draft created --");
        System.out.flush();

        return draft;
    }

    private String findHistoryItem(Operation scheduled) {
        String history = null;
        List<Link> links = scheduled.getLinks();
        for (Link link : links) {
            if (link.getRel().equals("history")) {
                history = link.getHref();
            }
        }
        assert history != null;
        return history;
    }

    private void waitAndCheckStatus(int platformId, String historyId) throws InterruptedException {
        Thread.sleep(15000); // we need to wait a little as the execution may take time

        given()
            .pathParam("hid", historyId)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/operation/history/{hid}");

        // See if we also find it when we are looking for histories on the resource
        Response response =
        given()
            .queryParam("resourceId", platformId)
            .header(acceptJson)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/operation/history");

        //  compare
        List<Map<String,Object>> list = response.as(List.class);
        boolean found = false;
        for (Map<String,Object> map : list) {
            if (map.get("jobId").equals(historyId)) {
                found = true;
            }
        }
        assert found;
    }

    private void waitForTerminationAndDelete(String historyId) throws InterruptedException {
        boolean done = false;
        int count = 0;
        while (!done) {
            Response response =
            given()
                .header(acceptJson)
                .pathParam("hid", historyId)
            .when()
                .get("/operation/history/{hid}");

            JsonPath jsonPath = response.jsonPath();
            String status= jsonPath.getString("status");
            int code = response.statusCode();

            if (code==200 && (status.equals("Success") || status.equals("Failed"))) {
                done = true;
            } else {
                Thread.sleep(2000);
            }
            count ++;
            assert count < 10 :"Waited for 20sec -- something is wrong";
        }

        // Delete the history item
        given()
            .pathParam("hid", historyId)
        .expect()
            .statusCode(204)
            .log().ifError()
        .when()
            .delete("/operation/history/{hid}");
    }

    @Test
    public void testOpsScheduleMissingRequiredParam() throws Exception {

        int platformId = findIdOfARealPlatform();

        Operation draft = getADraftOperation(platformId, discoveryDefinitionId);

        int draftId = draft.getId();

        // explicitly remove the param from the draft for
        // the test
        Map<String, Object> params = draft.getParams();
        if (params.containsKey("detailedDiscovery")) {
            params.remove("detailedDiscovery");
        }

        // Update to put the new version in the server
        // We don't want to submit, so the server does not
        // validate and we should get a 200 back
        draft.setReadyToSubmit(false);
        given()
            .contentType(ContentType.JSON)
            .pathParam("id",draftId)
            .body(draft)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .put("/operation/{id}");


        // update to schedule, lacking the required param
        draft.setReadyToSubmit(true);

        given()
            .contentType(ContentType.JSON)
            .pathParam("id",draftId)
            .body(draft)
        .expect()
            .statusCode(406)
            .log().ifError()
        .when()
            .put("/operation/{id}");
    }

    @Test
    public void testOpsScheduleRequiredParamWrongDataType() throws Exception {

        int platformId = findIdOfARealPlatform();

        Operation draft = getADraftOperation(platformId, discoveryDefinitionId);

        int draftId = draft.getId();

        draft.getParams().put("detailedDiscovery", 42);
        draft.setReadyToSubmit(true);

        // update to schedule
        given()
            .contentType(ContentType.JSON)
            .pathParam("id",draftId)
            .body(draft)
        .expect()
            .statusCode(406)
            .log().ifError()
        .when()
            .put("/operation/{id}");
    }

    @Test
    public void testDeleteBadOperationHistory() throws Exception {

        given()
            .pathParam("id","bla-44-15")
        .expect()
            .statusCode(406)
        .when()
            .delete("/operation/history/{id}");
    }

    @Test
    public void testDeleteUnknownOperationHistory() throws Exception {

        given()
            .pathParam("id","bla_=_44_=_15")
        .expect()
            .statusCode(204)
        .when()
            .delete("/operation/history/{id}");
    }

    @Test
    public void testDeleteUnknownOperationHistoryWithValidate() throws Exception {

        given()
            .pathParam("id","bla_=_44_=_15")
            .queryParam("validate",true)
        .expect()
            .statusCode(404)
        .when()
            .delete("/operation/history/{id}");
    }
}
