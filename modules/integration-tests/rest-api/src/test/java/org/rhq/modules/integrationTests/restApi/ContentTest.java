/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.integrationTests.restApi;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;

import org.junit.Test;

import org.rhq.modules.integrationTests.restApi.d.CreateCBRRequest;
import org.rhq.modules.integrationTests.restApi.d.Resource;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.isOneOf;

/**
 * Test content upload and creation of content based resources
 * @author Heiko W. Rupp
 */
public class ContentTest extends AbstractBase {

    private static final String DEPLOYED_WAR_NAME = "test-simple.war";

    @Test
    public void testUpload() throws Exception {

        InputStream in =
            getClass().getClassLoader().getResourceAsStream("test-simple.war");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int data;
        while ((data = in.read())!=-1) {
            baos.write(data);
        }

        byte[] bytes = baos.toByteArray();

        given()
            .auth().preemptive().basic("rhqadmin", "rhqadmin")
            .body(bytes)
            .contentType(ContentType.BINARY)
            .header(acceptJson)
            .log().everything()
        .expect()
            .statusCode(isOneOf(200, 201))
            .body("value", startsWith("rhq-rest-"))
            .body("value",endsWith(".bin"))
            .log().ifError()
        .when()
            .post("/content/fresh");


    }
    @Test
    public void testUploadAndDelete() throws Exception {

        InputStream in =
            getClass().getClassLoader().getResourceAsStream("test-simple.war");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int data;
        while ((data = in.read())!=-1) {
            baos.write(data);
        }

        byte[] bytes = baos.toByteArray();
        int size = bytes.length;

        String handle =
        given()
            .auth().preemptive().basic("rhqadmin", "rhqadmin")
            .body(bytes)
            .contentType(ContentType.BINARY)
            .header(acceptJson)
        .expect()
            .body("value", startsWith("rhq-rest-"))
            .body("value", endsWith(".bin"))
            .statusCode(isOneOf(200, 201))
        .when()
            .post("/content/fresh")
        .jsonPath()
            .getString("value");

        Integer uploadedSize =
        given()
            .pathParam("handle", handle)
            .header(acceptJson)
        .expect()
            .statusCode(200)
        .when()
            .get("/content/{handle}/info")
        .jsonPath().getInt("value");

        assert uploadedSize!=null;
        assert uploadedSize==size;

        removeContent(handle, false);

    }

    @Test
    public void testDeleteUnknownContent() throws Exception {

        removeContent("Frobnitz", false);

    }

    @Test
    public void testDeleteUnknownContentWithVaildate() throws Exception {

        removeContent("Frobnitz", true);

    }

    @Test
    public void testCreatePackageBasedResource() throws Exception {

        wipeWarArchiveIfNecessary();

        InputStream in =
            getClass().getClassLoader().getResourceAsStream("test-simple.war");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int data;
        while ((data = in.read())!=-1) {
            baos.write(data);
        }

        byte[] bytes = baos.toByteArray();

        // Upload content
        String handle =
        given()
            .auth().preemptive().basic("rhqadmin", "rhqadmin")
            .body(bytes)
            .contentType(ContentType.BINARY)
            .header(acceptJson)
        .expect()
            .body("value",startsWith("rhq-rest-"))
            .body("value",endsWith(".bin"))
            .statusCode(isOneOf(200, 201))
        .when()
            .post("/content/fresh")
        .jsonPath()
            .getString("value");

        // Find an EAP 6 server
        int as7Id = findIdOfARealEAP6();

        int createdResourceId=-1;

        // create child of eap6 as deployment
        try {
            CreateCBRRequest resource = new CreateCBRRequest();
            resource.setParentId(as7Id);
            resource.setResourceName("test-simple.war");


            // type of the new resource
            resource.setTypeName("Deployment");
            resource.setPluginName("JBossAS7");

            // set plugin config (path) and deploy config (runtime-name)
            resource.getPluginConfig().put("path","deployment");
            resource.getResourceConfig().put("runtimeName", DEPLOYED_WAR_NAME);

            Response response =
            given()
                .body(resource) // Type of new resource
                .queryParam("handle", handle)
                .contentType(ContentType.JSON)
                .header(acceptJson)
                .log().everything()
            .expect()
                .statusCode(isOneOf(200, 201, 302))
                .log().everything()
            .when()
                .post("/resource");

            System.out.println("after post");
            System.out.flush();

            int status = response.getStatusCode();
            String location = response.getHeader("Location");

            if (status!=200) {
                System.out.println("\nLocation " + location + "\n\n");
                assert location!=null;
            }

            // We need to check what we got. A 302 means the deploy is still
            // in progress, so we need to wait a little longer
            while (status==302) {

                response =
                given()
                    .header(acceptJson)
                    .log().everything()
                    .redirects().follow(false)
                    .redirects().allowCircular(true)
                .expect()
                    .statusCode(isOneOf(200, 201, 302))
                    .log().everything()
                .when()
                    .get(location);

                status = response.getStatusCode();
            }

            createdResourceId = response.jsonPath().getInt("resourceId");

            System.out.flush();
            System.out.println("\n  Deploy is done, resource Id = " + createdResourceId + " \n");
            System.out.flush();

            assert  createdResourceId != -1;

        } finally {

            // Remove the uploaded content
            removeContent(handle, false);

            System.out.flush();
            System.out.println("\n  Content removed \n");
            System.out.flush();


            // We need to wait here a little, as the machinery is not used to
            // quick create-delete-cycles
            Thread.sleep(20*1000L);

            given()
                .header(acceptJson)
                .queryParam("physical", "true") // Also remove target on the EAP instance
                .pathParam("id",createdResourceId)
                .log().everything()
            .expect()
                .log().everything()
            .when()
                .delete("/resource/{id}");

        }

    }

    private void removeContent(String handle, boolean validate) {
        given()
            .pathParam("handle", handle)
            .header(acceptJson)
        .expect()
            .statusCode(204)
            .log().ifError()
        .when()
            .delete("/content/{handle}");
    }

    private void wipeWarArchiveIfNecessary() {

        @SuppressWarnings("unchecked")
        List<Resource> resources =
        given()
            .queryParam("q",DEPLOYED_WAR_NAME)
            .queryParam("category", "SERVICE")
            .header(acceptJson)
        .expect()
            .log().everything()
        .when()
            .get("/resource")
        .as(List.class);

        if (resources!=null && resources.size()>0) {
            int resourceId = (Integer) ((Map < String,Object>)resources.get(0)).get("resourceId");

            given()
                .pathParam("id", resourceId)
                .queryParam("physical", "true") // Also remove target on the EAP instance
            .expect()
                .statusCode(204)
            .when()
                .delete("/resource/{id}");
        }
    }

    @Test
    public void testCreateCBRBadHandle() throws Exception {

        CreateCBRRequest resource = new CreateCBRRequest();
        resource.setParentId(123);
        resource.setResourceName("test-simple.war");


        // type of the new resource
        resource.setTypeName("Deployment");
        resource.setPluginName("JBossAS7");

        // set plugin config (path) and deploy config (runtime-name)
        resource.getPluginConfig().put("path","deployment");
        resource.getResourceConfig().put("runtimeName","test-simple.war");

        Response response =
        given()
            .body(resource) // Type of new resource
            .queryParam("handle", "This is a joke")
            .contentType(ContentType.JSON)
            .header(acceptJson)
            .log().everything()
        .expect()
                .statusCode(404)
            .log().everything()
        .when()
            .post("/resource");

    }

    @Test
    public void testUploadPlugin() throws Exception {

        // A Skeleton jar-less plugin.
        String plugin = "<plugin name=\"rest-api-No-op\"\n" +
            "        displayName=\"Abstract NO-OP plugin\"\n" +
            "        version=\"1.0\"\n" +
            "        description=\"Abstract plugin supporting concrete plugins that don't want java-agent support\"\n" +
            "        package=\"org.rhq.plugins.noop\"\n" +
            "        xmlns=\"urn:xmlns:rhq-plugin\">\n" +
            "</plugin>";

        byte[] bytes = plugin.getBytes();

        // Upload content
        String handle =
        given()
            .auth().preemptive().basic("rhqadmin", "rhqadmin")
            .body(bytes)
            .contentType(ContentType.BINARY)
            .header(acceptJson)
        .expect()
            .body("value",startsWith("rhq-rest-"))
            .body("value",endsWith(".bin"))
            .statusCode(isOneOf(200, 201))
        .when()
            .post("/content/fresh")
        .jsonPath()
            .getString("value");

        try {
            given()
                .pathParam("handle", handle)
                .queryParam("name", "rest-test-rhq-plugin.xml")
                .queryParam("scan", "true")
                .queryParam("pushOutDelay", "5000")
            .expect()
                .statusCode(200)
                .log().everything()
            .when()
                .put("/content/{handle}/plugins");
        } finally {
            removeContent(handle, false);
        }

    }
    @Test
    public void testUploadPluginBadHandle() throws Exception {

        // A Skeleton jar-less plugin.
        String plugin = "<plugin name=\"rest-api-No-op\"\n" +
            "        displayName=\"Abstract NO-OP plugin\"\n" +
            "        version=\"1.0\"\n" +
            "        description=\"Abstract plugin supporting concrete plugins that don't want java-agent support\"\n" +
            "        package=\"org.rhq.plugins.noop\"\n" +
            "        xmlns=\"urn:xmlns:rhq-plugin\">\n" +
            "</plugin>";

        byte[] bytes = plugin.getBytes();

        // Upload content
        String handle =
        given()
            .auth().preemptive().basic("rhqadmin", "rhqadmin")
            .body(bytes)
            .contentType(ContentType.BINARY)
            .header(acceptJson)
        .expect()
            .body("value",startsWith("rhq-rest-"))
            .body("value",endsWith(".bin"))
            .statusCode(isOneOf(200, 201))
        .when()
            .post("/content/fresh")
        .jsonPath()
            .getString("value");

        try {
            given()
                .pathParam("handle", "Frobnitz")
                .queryParam("name", "rest-test-rhq-plugin.xml")
                .queryParam("scan", "true")
            .expect()
                .statusCode(404)
                .log().everything()
            .when()
                .put("/content/{handle}/plugins");
        } finally {
            removeContent(handle, false);
        }

    }

    @Test
    public void testUploadPluginNoName() throws Exception {

        // A Skeleton jar-less plugin.
        String plugin = "<plugin name=\"rest-api-No-op\"\n" +
            "        displayName=\"Abstract NO-OP plugin\"\n" +
            "        version=\"1.0\"\n" +
            "        description=\"Abstract plugin supporting concrete plugins that don't want java-agent support\"\n" +
            "        package=\"org.rhq.plugins.noop\"\n" +
            "        xmlns=\"urn:xmlns:rhq-plugin\">\n" +
            "</plugin>";

        byte[] bytes = plugin.getBytes();

        // Upload content
        String handle =
        given()
            .auth().preemptive().basic("rhqadmin", "rhqadmin")
            .body(bytes)
            .contentType(ContentType.BINARY)
            .header(acceptJson)
        .expect()
            .body("value",startsWith("rhq-rest-"))
            .body("value",endsWith(".bin"))
            .statusCode(isOneOf(200, 201))
        .when()
            .post("/content/fresh")
        .jsonPath()
            .getString("value");

        try {
            given()
                .pathParam("handle", handle)
                .queryParam("scan", "true")
            .expect()
                .statusCode(406)
                .log().everything()
            .when()
                .put("/content/{handle}/plugins");
        } finally {
            removeContent(handle, false);
        }

    }
}
