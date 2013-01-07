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

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;

import org.apache.http.HttpStatus;
import org.junit.Test;

import org.rhq.modules.integrationTests.restApi.d.Group;

import static com.jayway.restassured.RestAssured.delete;
import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;

/**
 * Test user related functionality of the rest api
 * @author Heiko W. Rupp
 */
public class UserTest extends AbstractBase {

    @Test
    public void testGetFavoritesResources() throws Exception {

        expect().statusCode(200)
                .log().ifError()
            .when().get("/user/favorites/resource");
    }

    @Test
    public void testGetFavoriteGroups() throws Exception {

        expect().statusCode(200)
                .log().ifError()
            .when().get("/user/favorites/group");
    }

    @Test
    public void testAddRemoveFavoriteResources() throws Exception {

        expect().statusCode(204)
            .when().put("/user/favorites/resource/10001");

        try {
            Response r =
            expect()
                .statusCode(200)
            .when()
                .get("/user/favorites/resource");
            JsonPath jp = r.jsonPath();
            assert jp.getList("resourceId").contains("10001");
        }
        finally {
            expect()
                .statusCode(204)
            .when().delete("/user/favorites/resource/10001");
        }
    }

    @Test
    public void testAddRemoveFavoriteGroup() throws Exception {
        Group group = new Group("-x-test-group-user");

        //  create a group
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

        String location = created.header("Location");
        int groupId = Integer.parseInt(location.substring(location.lastIndexOf("/")+1));

        try {
            given()
                .header("Accept", "application/json")
                .contentType(ContentType.XML)
                .pathParam("id", groupId)
            .expect()
                .statusCode(204)
            .when()
                .put("/user/favorites/group/{id}");

            Response r =
                given()
                    .header("Accept", "application/json")
            .expect()
                .statusCode(200)
            .when()
                .get("/user/favorites/group");
//            JsonPath jp = r.jsonPath();  // TODO enable as soon as JsonPath syntax is known
//            assert jp.getList("$[].id").contains(""+groupId);
        }
        finally {
            given()
                .pathParam("id",groupId)
            .expect()
                .statusCode(204)
            .when()
                .delete("/user/favorites/group/{id}");

            delete("/group/" + groupId);
        }
    }

    @Test
    public void testAddNonExistingResource() throws Exception {

        expect()
            .statusCode(404)
        .when()
            .put("/user/favorites/resource/1"); // RHQ resource ids are > 10k
    }

    @Test
    public void testAddNonExistingGroup() throws Exception {

        expect()
            .statusCode(404)
            .log().everything()
        .when()
            .put("/user/favorites/group/1"); // RHQ group ids are > 10k
    }

    @Test
    public void testGetUserInfo() throws Exception {
        given()
                .pathParam("id","rhqadmin")
        .expect()
                .statusCode(200)
        .when()
                .get("/user/{id}");

    }
}
