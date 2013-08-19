package org.rhq.enterprise.server.plugins.yum;/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.util.stream.StreamUtil;
import org.rhq.test.PortScout;

import Acme.Serve.Serve;
import Acme.Serve.UrlReaderTestServer;

/**
 * @author Lukas Krejci
 * @since 4.9
 */
@Test
public class UrlReaderTest {

    private static final String TEST_USER = "testUser";
    private static final String TEST_PASSWORD = "password";

    private static class AuthServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String authType = req.getAuthType();
            String remoteUser = req.getRemoteUser();

            assertEquals(authType, "BASIC", "Unexpected authentication type");
            assertEquals(remoteUser, TEST_USER, "Unexpected authenticated user.");

            String path = req.getPathTranslated();
            if (path != null) {
                FileInputStream in = new FileInputStream(path);
                try {
                    StreamUtil.copy(in, resp.getOutputStream(), false);
                } finally {
                    in.close();
                }
            }
        }
    }

    private UrlReaderTestServer httpServer;
    private String rootUrl;

    @BeforeClass
    public void startWebServer() throws IOException, URISyntaxException {
        PortScout portScout = new PortScout();
        int httpPort = portScout.getNextFreePort();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put(Serve.ARG_PORT, httpPort);
        params.put(Serve.ARG_NOHUP, "nohup");

        httpServer = new UrlReaderTestServer(params, System.err);

        Serve.PathTreeDictionary aliases = new Serve.PathTreeDictionary();
        File root = getRoot();
        aliases.put("/", root);
        aliases.put("/*", root);

        httpServer.setMappingTable(aliases);
        httpServer.addDefaultServlets(null);

        httpServer.addServlet("/auth", new AuthServlet());

        UrlReaderTestServer.AuthRealm authRealm = new UrlReaderTestServer.AuthRealm("auth");
        authRealm.put(TEST_USER, TEST_PASSWORD);

        Serve.PathTreeDictionary realms = new Serve.PathTreeDictionary();
        realms.put("/auth", authRealm);

        httpServer.setRealms(realms);
        portScout.close();

        httpServer.runInBackground();

        rootUrl = InetAddress.getLocalHost().getHostAddress() + ":" + httpPort;
    }

    @AfterClass(alwaysRun = true)
    public void stopWebServer() throws IOException {
        httpServer.stopBackground();
        httpServer.destroyAllServlets();
    }

    public void picksCorrectImpl() throws Exception {
        URI httpUrl = new URI("http://jboss.org/rhq");
        URI httpsUrl = new URI("https://jboss.org/rhq");
        URI noSchemeUrl = new URI("stairway/to/heaven");
        URI fileUrl = new URI("file:/over/the/rainbow");

        UrlReader httpRdr = UrlReader.fromUri(httpUrl, null, null);
        UrlReader httpsRdr = UrlReader.fromUri(httpsUrl, null, null);
        UrlReader noSchemeRdr = UrlReader.fromUri(noSchemeUrl, null, null);
        UrlReader fileRdr = UrlReader.fromUri(fileUrl, null, null);

        assertReader(httpRdr, httpUrl.toURL(), HttpReader.class);
        assertReader(httpsRdr, httpsUrl.toURL(), HttpReader.class);
        assertReader(noSchemeRdr, new URL("file:stairway/to/heaven"), DiskReader.class);
        assertReader(fileRdr, fileUrl.toURL(), DiskReader.class);
    }

    public void readsFiles() throws Exception {
        UrlReader fileReader = UrlReader.fromUri(getRoot().toURI(), null, null);

        testReaderWithTestFile(fileReader);
    }

    public void readsHttp() throws Exception {
        URI uri = new URI("http://" + rootUrl);

        UrlReader httpReader = UrlReader.fromUri(uri, null, null);

        testReaderWithTestFile(httpReader);
    }

    public void authenticatesInHttp() throws Exception {
        URI uri = new URI("http://" + rootUrl + "/auth");

        UrlReader httpReader = UrlReader.fromUri(uri, TEST_USER, TEST_PASSWORD);

        testReaderWithTestFile(httpReader);
    }

    private static void assertReader(UrlReader instance, URL expectedUrl, Class<? extends UrlReader> expectedType) {
        assertEquals(instance.getClass(), expectedType, "Unexpected reader type");
        assertEquals(instance.getBaseURL(), expectedUrl, "Unexpected baseUrl");
    }

    private void testReaderWithTestFile(UrlReader reader) throws IOException, URISyntaxException {
        try {
            reader.validate();
        } catch (IOException e) {
            Assert.fail("Validation of " + reader.getClass().getSimpleName() + " reader failed", e);
        }

        Reader rdr = new InputStreamReader(reader.openStream("test.file"));
        try {
            String contents = StreamUtil.slurp(rdr);

            assertEquals(contents, "kachny\n", "Unexpected contents of the test file");
        } finally {
            rdr.close();
        }
    }

    private File getRoot() throws URISyntaxException {
        URI testUri = getClass().getResource("/test.file").toURI();

        File testFile = new File(testUri.getSchemeSpecificPart());
        return testFile.getParentFile();
    }
}
