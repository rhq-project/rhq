/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.modules.plugins.jbossas7.itest.nonpc;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.modules.plugins.jbossas7.ASConnection;
import org.rhq.modules.plugins.jbossas7.ASConnectionParams;
import org.rhq.modules.plugins.jbossas7.ASConnectionParamsBuilder;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Test {@link ASConnection} behavior with respect to management http connection persistence.
 * 
 * All test methods submit a list of tasks in parallel to make sure ASConnection will use its http connection pool.
 * 
 * @author Thomas Segismont
 */
public class ManagementConnectionPersistenceTest extends AbstractIntegrationTest {

    private ExecutorService executorService;

    @BeforeMethod
    private void setup() {
        executorService = Executors.newFixedThreadPool(5);
    }

    @AfterMethod
    private void tearDown() {
        executorService.shutdownNow();
    }

    @Test(timeOut = 60 * 1000)
    public void testWithDisabledConnectionPersistence() throws Exception {
        ASConnectionParams asConnectionParams = new ASConnectionParamsBuilder() //
            .setHost(DC_HOST) //
            .setPort(DC_HTTP_PORT) //
            .setUsername(DC_USER) //
            .setPassword(DC_PASS) //
            .setKeepAliveTimeout(Long.valueOf(-1)) //
            .createASConnectionParams();
        ASConnection asConnection = new ASConnection(asConnectionParams);
        List<Future<Result>> results = doTest(asConnection);
        checkResults(results, true);
        // Wait a bit
        Thread.sleep(1000 * 30);
        // We should still be able to reach the server
        assertTrue(executeRead(asConnection).isSuccess());
    }

    @Test(timeOut = 60 * 1000)
    public void testWithDefaultConnectionPersistence() throws Exception {
        ASConnectionParams asConnectionParams = new ASConnectionParamsBuilder() //
            .setHost(DC_HOST) //
            .setPort(DC_HTTP_PORT) //
            .setUsername(DC_USER) //
            .setPassword(DC_PASS) //
            .createASConnectionParams();
        ASConnection asConnection = new ASConnection(asConnectionParams);
        List<Future<Result>> results = doTest(asConnection);
        checkResults(results, true);
        // Wait for EAP to close persistent connections server side
        Thread.sleep(1000 * 30);
        // We should still be able to reach the server
        assertTrue(executeRead(asConnection).isSuccess());
    }

    @Test(timeOut = 60 * 1000)
    public void shouldFailForTooLongKeepAliveDuration() throws Exception {
        ASConnectionParams asConnectionParams = new ASConnectionParamsBuilder() //
            .setHost(DC_HOST) //
            .setPort(DC_HTTP_PORT) //
            .setUsername(DC_USER) //
            .setPassword(DC_PASS) //
            .setKeepAliveTimeout(Long.valueOf(1000 * 60 * 60)) //
            .createASConnectionParams();
        ASConnection asConnection = new ASConnection(asConnectionParams);
        List<Future<Result>> results = doTest(asConnection);
        checkResults(results, true);
        // Wait for EAP to close persistent connections server side
        Thread.sleep(1000 * 30);
        // Next operation should fail as server has closed connection and client did not
        assertFalse(executeRead(asConnection).isSuccess());
    }

    private List<Future<Result>> doTest(final ASConnection asConnection) throws Exception {
        List<Future<Result>> results = new LinkedList<Future<Result>>();
        for (int i = 0; i < 20; i++) {
            results.add(executorService.submit(new Callable<Result>() {
                @Override
                public Result call() throws Exception {
                    return executeRead(asConnection);
                }
            }));
        }
        return results;
    }

    private Result executeRead(ASConnection asConnection) {
        return asConnection.execute(new ReadAttribute(new Address("/"), "product-version"));
    }

    private void checkResults(List<Future<Result>> results, boolean success) throws Exception {
        for (Future<Result> result : results) {
            if (success) {
                assertTrue(result.get().isSuccess());
            } else {
                assertFalse(result.get().isSuccess());
            }
        }
    }

}
