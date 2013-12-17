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

package org.rhq.plugins.oracle;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;

public class OracleServerComponentTest extends ComponentTest {

    private static final String ORACLE_SERVER = "Oracle Server";
    private ResourceComponent<?> server;
    private ResourceComponent<?> details;

    // Login username; test USERS resource
    private String username;

    @BeforeMethod
    public void reset() {
    }

    @BeforeTest
    @Override
    protected void before() throws Exception {
        log.info("before");
        super.before();
        server = byName(ORACLE_SERVER);
        assertNotNull(server);
        details = byName("Oracle Advanced Statistics");
        assertNotNull(details);
    }

    @AfterTest
    protected void after() throws Exception {
        super.after();
        assertDown(server);
    }

    @Override
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
        if (resourceType.getName().equals(ORACLE_SERVER)) {
            log.info(configuration.getProperties());
            Pattern p = Pattern.compile("jdbc:.*:@(.*):(\\d+):(\\w++)");
            String url = System.getProperty("oracle.url");
            if (url == null)
                throw new IllegalStateException("oracle.url system property not set");
            Matcher matcher = p.matcher(url);
            if (!matcher.matches())
                throw new IllegalStateException("Cannot match " + p.pattern() + " against " + url);
            configuration.getSimple("host").setStringValue(matcher.group(1));
            configuration.getSimple("port").setIntegerValue(Integer.parseInt(matcher.group(2)));
            configuration.getSimple("sid").setStringValue(matcher.group(3));
            username = System.getProperty("oracle.username");
            String password = System.getProperty("oracle.password");
            configuration.getSimple("principal").setStringValue(username);
            configuration.getSimple("credentials").setStringValue(password);
        }
    }

    @Test
    public void up() throws Exception {
        log.info("testUp");
        assertUp(server);
        MeasurementReport report = getMeasurementReport(server);
        Double rt;
        rt = getValue(report, "totalSize");
        assertTrue(rt > 100);
        rt = getValue(report, "user calls");
        assertTrue(rt > 0);

        assertUp(details);
        report = getMeasurementReport(details);
        rt = getValue(report, "Buffer Cache Hit Ratio");
        log.debug("hit " + rt);

        ResourceComponent<?> user = byName(username.toUpperCase());
        assertUp(user);
        report = getMeasurementReport(user);
        rt = getValue(report, "active");
        assertTrue("have at least one active connection", rt >= 1);
        rt = getValue(report, "connections");
        assertTrue("have at least one connection", rt >= 1);
    }


}
