/*
 * RHQ Management Platform
 * Copyright (C) 2014 Red Hat, Inc.
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

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;

/**
 * @author Lukas Krejci
 * @since 4.11
 */
public class PluginsTest extends AbstractBase {

    private static int DUMMY_PLUGIN_ID = -1;

    @BeforeClass
    public static void installDummyPlugin() throws Exception {
        setupRestAssured();

        InputStream in =
            PluginsTest.class.getClassLoader().getResourceAsStream("dummy-rhq-plugin.xml");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int data;
        while ((data = in.read())!=-1) {
            baos.write(data);
        }

        byte[] bytes = baos.toByteArray();

        in.close();
        baos.close();

        String handle = given()
            .auth().preemptive().basic("rhqadmin", "rhqadmin")
            .body(bytes)
            .contentType(ContentType.BINARY)
            .header(acceptJson)
        .expect()
            .statusCode(201)
            .log().ifError()
        .when()
            .post("/content/fresh").body().path("value");

        @SuppressWarnings("unchecked")
        Map<String, Object> plugin = given()
            .header(acceptJson)
            .param("handle", handle)
            .param("name", "dummy-rhq-plugin.xml")
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .put("/plugins").jsonPath().get("find { p -> p.name == 'Dummy' }");

        Assert.assertEquals(plugin.get("name"), "Dummy");

        DUMMY_PLUGIN_ID = (Integer) plugin.get("id");
    }

    @AfterClass
    public static void uninstallDummyPlugin() throws Exception {
        if (DUMMY_PLUGIN_ID != -1) {
            given()
                .header(acceptJson)
                .parameter("purge", true)
            .expect()
                .statusCode(200)
                .body("id", equalTo(DUMMY_PLUGIN_ID))
                .body("status", equalTo("DELETED"))
                .log().ifError()
            .when()
                .delete("/plugins/{id}", DUMMY_PLUGIN_ID);
        }
    }

    @Test
    public void testListPlugins() throws Exception {
        JsonPath json = given()
            .header(acceptJson)
            .parameter("name", "dummy")
            .expect()
            .statusCode(200)
            .log().ifError()
            .when()
            .get("/plugins").jsonPath();

        List<Map<String, Object>> results = json.get();

        assert results != null;
        assert results.size() == 1;

        Map<String, Object> platformPlugin = results.get(0);

        assert platformPlugin != null;
        assert "Dummy".equals(platformPlugin.get("name"));
        assert DUMMY_PLUGIN_ID == (Integer) platformPlugin.get("id");
    }

    @Test
    public void testPluginInfo() throws Exception {
        given()
            .header(acceptJson)
        .expect()
            .statusCode(200)
            .body("id", equalTo(DUMMY_PLUGIN_ID))
            .body("name", equalTo("Dummy"))
            .log().ifError()
        .when()
            .get("/plugins/{id}", DUMMY_PLUGIN_ID);
    }

    @Test
    public void testDisablePlugin() throws Exception {
        given()
            .header(acceptJson)
            .parameter("enabled", false)
        .expect()
            .statusCode(200)
            .body("enabled", equalTo(false))
            .log().ifError()
        .when()
            .post("/plugins/{id}", DUMMY_PLUGIN_ID);

    }

    @Test
    public void testEnablePlugin() throws Exception {
        given()
            .header(acceptJson)
            .queryParameter("enabled", true)
        .expect()
            .statusCode(200)
            .body("enabled", equalTo(true))
            .log().ifError()
        .when()
            .post("/plugins/{id}", DUMMY_PLUGIN_ID);
    }
}
