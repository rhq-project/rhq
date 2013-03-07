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

    private int definitionId;

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

        definitionId = -1;
        List<Map<String,Object>> list = r.as(List.class);
        for (Map<String,Object> map : list) {
            if (map.get("name").equals("discovery"))
                definitionId = (Integer) map.get("id");
        }

        assert definitionId !=-1 : "No discovery operation found";
    }

    @Test
    public void testGetDefinitionById() throws Exception {

        // Now retrieve that definition by id

        given()
            .header(acceptJson)
            .pathParam("did",definitionId)
        .expect()
            .statusCode(200)
            .body("name",is("discovery"))
        .when()
            .get("/operation/definition/{did}");

     }

    @Test
    public void testCreateDraftOperation() throws Exception {

        Operation draft =
        given()
            .header(acceptJson)
            .pathParam("definitionId",definitionId)
            .queryParam("resourceId",_platformId)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .post("/operation/definition/{definitionId}")
        .as(Operation.class);

        assert draft != null;
        assert draft.getDefinitionId() == definitionId;

        int draftId = draft.getId();

        // check if we can retrieve one single draft

        Operation op = get("/operation/" + draftId).as(Operation.class);
        assert op !=null;
        assert op.equals(draft);

    }

    @Test
    public void testCreateAndUpdateDraftOperation() throws Exception {

        Operation draft =
        given()
            .header(acceptJson)
            .pathParam("definitionId",definitionId)
            .queryParam("resourceId",_platformId)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .post("/operation/definition/{definitionId}")
        .as(Operation.class);

        assert draft != null;
        assert draft.getDefinitionId() == definitionId;

        int draftId = draft.getId();
        draft.getParams().put("detailed",true);

        try {
            given()
                .contentType(ContentType.JSON)
                .pathParam("id", draftId)
                .body(draft)
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
    public void testCreateDraftOperationAndScheduleExecution() throws Exception {

        int platformId = findIdOfARealPlatform();

        Operation draft =
        given()
            .header(acceptJson)
            .pathParam("definitionId",definitionId)
            .queryParam("resourceId",platformId)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .post("/operation/definition/{definitionId}")
        .as(Operation.class);

        assert draft != null;
        assert draft.getDefinitionId() == definitionId;

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
            .log().everything()
        .when()
            .put("/operation/{id}")
        .as(Operation.class);

        System.out.println(scheduled.getId());
        String history = null;
        List<Map<String,Object>> links = scheduled.getLinks();
        for (Map<String,Object> link : links) {
            if (link.get("rel").equals("history"))
                history = (String) link.get("href");
        }
        assert history != null;

        String historyId = history.substring(history.lastIndexOf("/")+1);
        try {
            Thread.sleep(15000); // we need to wait a little as the execution may take time

            given()
                .pathParam("hid",historyId)
            .expect()
                .statusCode(200)
                .log().everything()
            .when()
                .get("/operation/history/{hid}");

            // See if we also find it when we are looking for histories on the resource
            Response response =
            given()
                .queryParam("resourceId",platformId)
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

        } finally {

            // Wait until the operation has finished and then delete
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
                .pathParam("hid",historyId)
            .expect()
                .statusCode(204)
                .log().ifError()
            .when()
                .delete("/operation/history/{hid}");

        }
    }

}
