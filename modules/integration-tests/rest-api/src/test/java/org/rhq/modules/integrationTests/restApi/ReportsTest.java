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

import com.jayway.restassured.path.xml.XmlPath;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.*;

import static com.jayway.restassured.RestAssured.given;

/**
 * Tests for the reports that also run via the Rest-api (but different base url)
 * @author Heiko W. Rupp
 */
public class ReportsTest extends AbstractBase {

    private static final int NUMBER_REPORTS = 9;

    @Test
    public void testReportListJson() throws Exception {
        given()
            .header(acceptJson)
        .expect()
            .statusCode(200)
            .log().ifError()
            .body("", iterableWithSize(NUMBER_REPORTS))
        .when()
            .get("/reports");

    }

    @Test
    public void testReportListCsv() throws Exception {
        String line =
        given()
            .header(acceptCsv)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/reports")
        .asString();

        String[] lines = line.split("\n");
        assert lines != null;
        assert lines[0].equals("Report,URL"): lines[0];
        assert lines.length==1 + NUMBER_REPORTS; // header + 9 lines of links
    }

    @Test
    public void testReportListXml() throws Exception {
        given()
            .header(acceptXml)
        .expect()
            .statusCode(200)
            .log().ifError()
            .body("/collection",iterableWithSize(NUMBER_REPORTS))
        .when()
            .get("/reports");

    }

    @Test
    public void testReportListHtml() throws Exception {
        Response response =
        given()
            .header(new Header("Accept","text/html"))
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/reports");

        XmlPath xmlPath = response.xmlPath();
        assert xmlPath!=null;
        assert xmlPath.getNodeChildren("html.body.ul.li").size()== NUMBER_REPORTS;

    }

    @Test
    public void testGetAlertDefinitions() throws Exception {

        Response response =
        given()
            .header(acceptCsv)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/reports/alertDefinitions");

        assert response != null;
        assert response.asString().startsWith("Name,Description,Enabled,Priority,Parent,Ancestry,Details URL");

    }

    @Test
    public void testGetConfigurationHistory() throws Exception {

        Response response =
        given()
            .header(acceptCsv)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/reports/configurationHistory");

        assert response != null;
        assert response.asString().startsWith("Version,Date Submitted,Date Completed,Status,Name,Ancestry,Details URL");
    }

    @Test
    public void testGetDriftCompliance() throws Exception {

        Response response =
        given()
            .header(acceptCsv)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/reports/driftCompliance");

        assert response!=null;
        assert response.asString().startsWith("Resource Type,Plugin,Category,Version,Count");
    }

    @Test
    public void testGetInventorySummary() throws Exception {

        Response response =
        given()
            .header(acceptCsv)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/reports/inventorySummary");

        assert response!=null;
        assert response.asString().startsWith("Resource Type,Plugin,Category,Version,Count");
    }

    @Test
    public void testGetPlatformUtilization() throws Exception {

        Response response =
        given()
            .header(acceptCsv)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/reports/platformUtilization");

        assert response!=null;
        assert response.asString().startsWith("Name,Version,CPU,Memory,Swap");
    }

    @Test
    public void testGetRecentAlerts() throws Exception {

        Response response =
        given()
            .header(acceptCsv)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/reports/recentAlerts");

        assert response!=null;
        assert response.asString().startsWith("Creation Time,Name,Condition Text,Priority");
    }

    @Test
    public void testGetRecentDrift() throws Exception {

        Response response =
        given()
            .header(acceptCsv)
            .queryParam("categories", "file_added")
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/reports/recentDrift");

        assert response!=null;
        assert response.asString().startsWith("Creation Time,Definition,Snapshot,Category,Path");
    }

    @Test
    public void testGetRecentDriftNoCategory() throws Exception {

        given()
            .header(acceptCsv)
        .expect()
            .statusCode(406) // Not acceptable
            .log().ifError()
        .when()
            .get("/reports/recentDrift");
    }

    @Test
    public void testGetRecentDriftBadCategory() throws Exception {

        given()
            .header(acceptCsv)
            .queryParam("categories","frobnitz")
        .expect()
            .statusCode(406) // Not acceptable
            .log().ifError()
        .when()
            .get("/reports/recentDrift");
    }

    @Test
    public void testGetRecentOperations() throws Exception {

        Response response =
        given()
            .header(acceptCsv)
        .expect()
            .statusCode(200)
            .log().everything()
        .when()
            .get("/reports/recentOperations");

        assert response!=null;
        assert response.asString().startsWith("Date Submitted,Operation,Requester");

    }

    @Test
    public void testGetSuspectMetrics() throws Exception {

        Response response =
        given()
            .header(acceptCsv)
        .expect()
            .statusCode(200)
            .log().ifError()
        .when()
            .get("/reports/suspectMetrics");

        assert response!=null;
        assert response.asString().startsWith("Resource,Ancestry");
    }
}
