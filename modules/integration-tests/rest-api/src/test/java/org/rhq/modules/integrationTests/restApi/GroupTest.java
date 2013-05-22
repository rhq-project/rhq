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

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import org.rhq.modules.integrationTests.restApi.d.Group;
import org.rhq.modules.integrationTests.restApi.d.GroupDef;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;

/**
 * Test group related stuff
 * @author Heiko W. Rupp
 */
public class GroupTest extends AbstractBase {

    private static final String X_TEST_GROUP = "-x-test-group";

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Remove group that was left over (just in case)
        Response response =
            given()
                .header(acceptJson)
                .queryParam("q", X_TEST_GROUP)
            .when()
                .get("/group");

        JsonPath jsonPath = response.jsonPath();
        if (jsonPath.get("id[0]")!=null) {
            int groupId = jsonPath.getInt("id[0]");
            given()
                .pathParam("id", groupId)
            .delete("/group/{id}");
        }
    }

    @Test
    public void testGetGroups() throws Exception {
        expect()
            .statusCode(200)
        .when()
            .get("/group");
    }

    @Test
    public void testGetGroupsWithPaging() throws Exception {
        given()
            .header(acceptJson)
            .queryParam("page",0)
            .queryParam("ps",2)
        .expect()
            .statusCode(200)
            .header("Link", notNullValue())
        .when()
            .get("/group");
    }

    @Test
    public void testGetGroupsWithPagingWrapped() throws Exception {
        given()
            .header(acceptWrappedJson)
            .queryParam("page",0)
            .queryParam("ps",2)
        .expect()
            .statusCode(200)
            .log().ifError()
            .header("Link",nullValue())
            .body("pageSize",is(2))
            .body("currentPage",is(0))
        .when()
            .get("/group");
    }

    @Test
    public void testGetGroupsQuery() throws Exception {
        given()
            .queryParam("q", "lala")
        .expect()
            .statusCode(200)
        .when()
            .get("/group");
    }

    @Test
    public void testCreateGroupAndRemove() throws Exception {
        Group group = new Group(X_TEST_GROUP);

        // create the group
        Response created =
        given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(group)
            .expect()
                .statusCode(HttpStatus.SC_CREATED)
                .log().ifError()
            .when()
                .post("/group");

        // Determine location from response
        // and compare id with the found group below
        String location = created.header("Location");
        int createdId = Integer.parseInt(location.substring(location.lastIndexOf("/")+1));

        // Search for it
        Response response =
        given()
            .header(acceptJson)
            .queryParam("q", X_TEST_GROUP)
        .expect()
            .statusCode(200)
        .when()
            .get("/group");

        JsonPath jsonPath = response.jsonPath();
        assert jsonPath.get("[0].name").equals(X_TEST_GROUP); // [0] as the query returns a list
        int groupId = jsonPath.get("[0].id");
        assert groupId == createdId;

        // Fetch id by id
        given()
            .pathParam("id",groupId)
            .header(acceptJson)
        .expect()
            .statusCode(200)
            .body("name",is(X_TEST_GROUP))
            .body("id",is(groupId))
            .body("explicitCount",is(0))
            .log().ifError()
        .when()
            .get("/group/{id}");


        // delete the group again
        given()
                .pathParam("id",groupId)
        .expect()
                .statusCode(204)
                .log().ifError()
        .when()
                .delete("/group/{id}");

    }

    @Test
    public void testGetGroupWithInvalidId() throws Exception {
        given()
            .pathParam("gid", 42)
        .expect()
            .statusCode(404)
        .when()
            .get("/group/{gid}");
    }

    @Test
    public void testUpdateGroupWithInvalidId() throws Exception {
        Group group = new Group(X_TEST_GROUP);
        given()
            .pathParam("gid",42)
            .header(acceptJson)
            .contentType(ContentType.JSON)
            .body(group)
        .expect()
            .statusCode(404)
        .when()
            .put("/group/{gid}");
    }

    @Test
    public void testUpdateGroup() throws Exception {
        Group group = new Group(X_TEST_GROUP);

        // Generate the group
        Response response =
        given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(group)
            .expect()
                .statusCode(HttpStatus.SC_CREATED)
                .log().ifError()
            .when()
                .post("/group");

        String location = response.header("Location");
        int id = Integer.parseInt(location.substring(location.lastIndexOf("/")+1));

        try {
            group.setName("-x-test-2");
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(group)
                .pathParam("id",id)
            .expect()
                .statusCode(HttpStatus.SC_OK)
                .log().ifError()
            .when()
                .put("/group/{id}");
        }
        finally {
            // delete the group
            given()
                    .pathParam("id", id)
            .expect()
                    .statusCode(204)
                    .log().ifError()
            .when()
                    .delete("/group/{id}");
        }
    }

    @Test
    public void testAddResourceToGroup() throws Exception {

        assert _platformId !=0 : "Set up did not run or was not successful";

        Group group = new Group(X_TEST_GROUP);

        // Generate the group
        Response response =
        given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(group)
            .expect()
                .statusCode(HttpStatus.SC_CREATED)
                .log().ifError()
            .when()
                .post("/group");

        String location = response.header("Location");
        int id = Integer.parseInt(location.substring(location.lastIndexOf("/")+1));

        try {
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(group)
                .pathParam("id", id)
                .pathParam("resourceId",_platformId)
            .expect()
                .statusCode(HttpStatus.SC_OK)
                .log().ifError()
            .when()
                .put("/group/{id}/resource/{resourceId}");
        }
        finally {
            // delete the group
            given()
                    .pathParam("id",id)
            .expect()
                    .statusCode(204)
                    .log().ifError()
            .when()
                    .delete("/group/{id}");
        }
    }

    @Test
    public void testAddResourceToGroupAndGetResources() throws Exception {
        Group group = new Group(X_TEST_GROUP);

        // Generate the group
        Response response =
        given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(group)
            .expect()
                .statusCode(HttpStatus.SC_CREATED)
                .log().ifError()
            .when()
                .post("/group");

        String location = response.header("Location");
        int groupId = Integer.parseInt(location.substring(location.lastIndexOf("/")+1));

        try {
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(group)
                .pathParam("gid", groupId)
                .pathParam("resourceId",_platformId)
            .expect()
                .statusCode(HttpStatus.SC_OK)
                .log().ifError()
            .when()
                .put("/group/{gid}/resource/{resourceId}");

            given()
                .pathParam("groupId",groupId)
            .expect()
                .statusCode(200)
                .log().ifError()
                .body("", iterableWithSize(1)) // Expect one
            .when()
                .get("/group/{groupId}/resources");

        }
        finally {
            // delete the group
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
    public void testGetResourcesForGroupWithInvalidId() throws Exception {
        given()
            .pathParam("groupId", 14)
        .expect()
            .statusCode(404)
        .when()
            .get("/group/{groupId}/resources");
    }

    @Test
    public void testAddNonExistingResourceToGroup() throws Exception {
        Group group = new Group(X_TEST_GROUP);

        // Generate the group
        Response response =
        given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(group)
            .expect()
                .statusCode(HttpStatus.SC_CREATED)
                .log().ifError()
            .when()
                .post("/group");

        String location = response.header("Location");
        int id = Integer.parseInt(location.substring(location.lastIndexOf("/")+1));

        try {
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(group)
                .pathParam("id",id)
                .pathParam("resourceId",1)
            .expect()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .log().ifError()
            .when()
                .put("/group/{id}/resource/{resourceId}");
        }
        finally {
            // delete the group
            given()
                    .pathParam("id",id)
            .expect()
                    .statusCode(204)
                    .log().ifError()
            .when()
                    .delete("/group/{id}");
        }
    }

    @Test
    public void testRemoveResourceFromGroup() throws Exception {
        Group group = new Group(X_TEST_GROUP);

        // Generate the group
        Response response =
        given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(group)
            .expect()
                .statusCode(HttpStatus.SC_CREATED)
                .log().ifError()
            .when()
                .post("/group");

        String location = response.header("Location");
        int id = Integer.parseInt(location.substring(location.lastIndexOf("/")+1));

        try {
            // Add a resource to the group
            given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(group)
                .pathParam("id",id)
                .pathParam("resourceId",_platformId)
            .expect()
                .statusCode(HttpStatus.SC_OK)
                .log().ifError()
            .when()
                .put("/group/{id}/resource/{resourceId}");

            // and remove it again
            given()
                .header(acceptJson)
                .pathParam("id",id)
                .pathParam("resourceId",_platformId)
            .expect()
                .statusCode(HttpStatus.SC_OK)
                .log().ifError()
            .when()
                .delete("/group/{id}/resource/{resourceId}");

        }
        finally {
            // delete the group
            given()
                    .pathParam("id",id)
            .expect()
                    .statusCode(204)
                    .log().ifError()
            .when()
                    .delete("/group/{id}");
        }
    }

    @Test
    public void testGetMetricDefinitionsForGroup() throws Exception {
        Group group = new Group(X_TEST_GROUP);
        group.setCategory("COMPATIBLE");
        group.setResourceTypeId(_platformTypeId);

        // Generate the group
        Response response =
        given()
                .header(acceptJson)
                .contentType(ContentType.JSON)
                .body(group)
                .log().everything()
            .expect()
                .statusCode(HttpStatus.SC_CREATED)
                .log().ifError()
            .when()
                .post("/group");

        String location = response.header("Location");
        int id = Integer.parseInt(location.substring(location.lastIndexOf("/")+1));

        try {
            given()
                .header(acceptJson)
                .pathParam("id",id)
            .expect()
                .statusCode(HttpStatus.SC_OK)
                .log().ifError()
            .when()
                .get("/group/{id}/metricDefinitions");

        }
        finally {
            // delete the group
            given()
                    .pathParam("id",id)
            .expect()
                    .statusCode(204)
                    .log().ifError()
            .when()
                    .delete("/group/{id}");
        }
    }

    @Test
    public void testGetGroupDefinitions() throws Exception {

        expect()
                .statusCode(200)
                .log().ifError()
        .when()
                .get("/group/definitions");
    }

    @Test
    public void testCreateDefinitionWithoutName() throws Exception {

        GroupDef gd = new GroupDef();
        gd.setDescription("Just testing");
        List<String> list = new ArrayList<String>();
        list.add("groupby resource");
        list.add("resource.name");
        gd.setExpression(list);

        given()
            .contentType(ContentType.JSON)
            .header("Accept","application/json")
            .body(gd)
        .expect()
            .statusCode(406)
            .log().ifError()
        .when()
            .post("/group/definitions");
    }

    @Test
    public void testCreateRetrieveDeleteDefinition() throws Exception {

        GroupDef gd = new GroupDef("-x-test-def");
        gd.setDescription("Just testing");
        List<String> list = new ArrayList<String>();
        list.add("groupby resource");
        list.add("resource.name");
        gd.setExpression(list);

        Response response =
        given()
            .contentType(ContentType.JSON)
            .header("Accept","application/json")
            .body(gd)
        .expect()
            .statusCode(201)
            .log().ifError()
        .when()
            .post("/group/definitions");


        String location = response.getHeader("Location");
        int definitionId = Integer.parseInt(location.substring(location.lastIndexOf("/")+1));

        try {
            // retrieve by id
            given()
                .pathParam("id", definitionId)
            .expect()
                .statusCode(200)
            .when()
                .get("/group/definition/{id}");

            // retrieve by query
            given()
                .queryParam("q", "-x-test-def")
            .expect()
                .statusCode(200)
                .body("name", hasItem("-x-test-def"))
                .log().ifError()
            .when()
                .get("/group/definitions");
        } finally {

            // remove the definition again
            expect()
                .statusCode(204)
            .when()
                .delete("/group/definition/" + definitionId);
        }

    }

    @Test
    public void testGetUnknownGroupDefinition() throws Exception {

        given()
            .pathParam("id", 44)
        .expect()
            .statusCode(404)
        .when()
            .get("/group/definition/{id}");
    }

    @Test
    public void testUpdateUnknownGroupDefinition() throws Exception {

        GroupDef gd = new GroupDef("-x-test-def");
        gd.setDescription("Just testing");

        given()
            .contentType(ContentType.JSON)
            .header("Accept","application/json")
            .body(gd)
            .pathParam("id", 44)
        .expect()
            .statusCode(404)
        .when()
            .put("/group/definition/{id}");
    }

    @Test
    public void testDeleteUnknownGroupDefinition() throws Exception {
            given()
                .pathParam("id", 44)
            .expect()
                .statusCode(204)
            .when()
                .delete("/group/definition/{id}");
    }

    @Test
    public void testCreateUpdateRecalcDeleteDefinition() throws Exception {

        GroupDef gd = new GroupDef("-x-test-def2");
        gd.setDescription("Just testing");
        List<String> list = new ArrayList<String>();
        list.add("groupby resource.trait[partitionName]");
        list.add("resource.type.plugin = JBossAS");
        list.add("resource.type.name = JBossAS Server");
        gd.setExpression(list);

        Response response =
        given()
            .contentType(ContentType.JSON)
            .header("Accept","application/json")
            .body(gd)
        .expect()
            .statusCode(201)
            .log().ifError()
        .when()
            .post("/group/definitions");


        String location = response.getHeader("Location");
        int defintionId = Integer.parseInt(location.substring(location.lastIndexOf("/")+1));

        try {
            gd.setDescription("Hulla");
            list = new ArrayList<String>(1);
            list.add("groupby resource.pluginConfiguration[productType]");
            gd.setExpression(list);

            given()
                .contentType(ContentType.JSON)
                .header("Accept", "application/json")
                .body(gd)
                .pathParam("id", defintionId)
            .expect()
                .statusCode(200)
                .log().ifError()
            .when()
                .put("/group/definition/{id}");

            given()
                .contentType(ContentType.JSON)
                .header("Accept","application/json")
                .body(gd) // needs to be supplied as dummy
                .pathParam("id", defintionId)
                .queryParam("recalculate",true)
            .expect()
                .statusCode(204)
                .log().ifError()
            .when()
                .put("/group/definition/{id}");
        }
        finally {

            expect()
                .statusCode(204)
                .when()
                .delete("/group/definition/" + defintionId);
        }
    }

    @Test
    public void testCreatingBadGroup1() throws Exception {

        given()
            .contentType(ContentType.XML)
            .header(acceptJson)
        .expect()
            .statusCode(406)
        .when()
            .post("/group/");

    }

    @Test
    public void testCreatingBadGroup2() throws Exception {

        Group group = new Group();
        group.setCategory("COMPATIBLE");
        group.setResourceTypeId(10001);

        given()
            .contentType(ContentType.JSON)
            .header(acceptJson)
            .body(group)
        .expect()
            .statusCode(406)
            .log().everything()
        .when()
            .post("/group/");

    }
}
