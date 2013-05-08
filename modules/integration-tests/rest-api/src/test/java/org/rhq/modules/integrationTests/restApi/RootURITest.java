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

import java.util.List;
import java.util.Map;

import com.jayway.restassured.path.json.JsonPath;

import org.junit.Test;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;

/**
 * Test the / resource (http://localhost:7080/rest/ )
 * @author Heiko W. Rupp
 */
public class RootURITest extends AbstractBase {

    @Test
    public void testRootPresent() throws Exception {

        given().header("Accept","text/html")
                .expect().statusCode(200)
                .when().get("/");

        given().header("Accept","text/html")
                .expect().statusCode(200)
                .when().get("/index");

        given().header("Accept","application/json")
                .expect().statusCode(200)
                .when().get("/");

        given().header("Accept","application/xml")
                .expect().statusCode(200)
                .when().get("/index");

        expect().statusCode(200)
                .when().get("/.json");

        expect().statusCode(200)
                .when().get("/.xml");

        expect().statusCode(200)
                .when().get("/.html");

        expect().statusCode(200)
                .when().get("/index.json");

        expect().statusCode(200)
                .when().get("/index.xml");

        expect().statusCode(200)
                .when().get("/index.html");

    }

    @Test
    public void testLinksInRoot() throws Exception {

        String json = get("/index.json").asString();
        List<Map<String,Map<String,String>>> links = JsonPath.with(json).getList(""); // with() already returns the list

        assert links != null;
        assert links.size()>0;

        String[] mediaTypes = {"text/html","application/xml","application/json"};

        for (Map<String,Map<String,String>> link : links) {
            String key = link.keySet().iterator().next();
            Map<String,String> map = link.get(key);
            String href= map.get("href");

            for (String mediaType : mediaTypes) {
                given().header("Accept",mediaType)
                    .expect().statusCode(200)
                    .log().ifError()
                    .when().get(href);
            }
        }
    }

    @Test
    public void testJsonPWrapping() throws Exception {

        String json = get("/index.json").asString();
        assert !json.startsWith("foo(");

        String jsonp = get("/index.json?jsonp=foo").asString();
        assert jsonp.startsWith("foo(");
        String jp2 = "foo(" + json+ ");";
        assert jsonp.equals(jp2);

    }
}
