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

package org.rhq.enterprise.server.cloud;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.Server.OperationMode;
import org.rhq.core.domain.criteria.ServerCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestConstants;
import org.rhq.enterprise.server.test.TransactionCallback;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.ServerFactory;

/**
 * @author Jirka Kremser
 */
@Test
public class TopologyManagerBeanTest extends AbstractEJB3Test {
    
    private TopologyManagerLocal topologyManager;
    private Subject overlord;

    @Override
    protected void beforeMethod() throws Exception {
        topologyManager = LookupUtil.getTopologyManager();
        overlord = LookupUtil.getSubjectManager().getOverlord();
    }

    @Test(groups = "integration.ejb3")
    public void testParsingCriteriaQueryResults1() throws Exception {

        final int serverCount = 208;
        executeInTransaction(new TransactionCallback() {

            public void execute() throws Exception {
                // verify that all server objects are actually parsed. 
                final Set<String> serverNames = new HashSet<String>(serverCount);
                serverNames.add(TestConstants.RHQ_TEST_SERVER_NAME);

                final String prefix = "server";

                for (int i = 0; i < serverCount - 1; i++) {
                    String name = prefix + String.format(" %03d", i + 1);
                    Server server = ServerFactory.newInstance();
                    server.setName(name);
                    server.setOperationMode(OperationMode.NORMAL);
                    server.setAddress("address" + i);
                    server.setPort(7080 + i);
                    server.setSecurePort(7443 + i);

                    em.persist(server);
                    serverNames.add(name);
                    em.flush();
                }
                em.flush();

                assertTrue("The number of created servers should be " + serverCount + ". Was: " + serverNames.size(),
                    serverCount == serverNames.size());

                // query the results and delete the servers
                final int pageSize = 42;
                ServerCriteria criteria = new ServerCriteria();
                criteria.addFilterName(prefix);
                criteria.addSortName(PageOrdering.DESC); // use DESC just to make sure sorting on name is different than insert order
                criteria.setPaging(0, pageSize);

                // iterate over the results with CriteriaQuery
                CriteriaQueryExecutor<Server, ServerCriteria> queryExecutor = new CriteriaQueryExecutor<Server, ServerCriteria>() {
                    @Override
                    public PageList<Server> execute(ServerCriteria criteria) {
                        return topologyManager.findServersByCriteria(overlord, criteria);
                    }
                };

                // initiate first/(total depending on page size) request.
                CriteriaQuery<Server, ServerCriteria> servers = new CriteriaQuery<Server, ServerCriteria>(criteria,
                    queryExecutor);

                String prevName = null;
                // iterate over the entire result set efficiently
                int actualCount = 0;
                for (Server s : servers) {
                    assert null == prevName || s.getName().compareTo(prevName) < 0 || s.getName().equals(TestConstants.RHQ_TEST_SERVER_NAME) : "Results should be sorted by name DESC, something is out of order";
                    prevName = s.getName();
                    actualCount++;
                    serverNames.remove(s.getName());
                }

                // test that entire list parsed spanning multiple pages
                assertTrue("Expected resourceNames to be empty. Still " + serverNames.size() + " name(s).",
                    serverNames.size() == 0);

                assertTrue("Expected " + serverCount + " to be parsed, but there were parsed " + actualCount
                    + " servers", actualCount == serverCount);
            }
        });
    }

    @Test(groups = "integration.ejb3")
    public void testParsingCriteriaQueryResults2() throws Exception {

        final int serverCount = 307;
        executeInTransaction(new TransactionCallback() {

            public void execute() throws Exception {
                // verify that all server objects are actually parsed. 
                final Set<String> serverNames = new HashSet<String>(serverCount);
                serverNames.add(TestConstants.RHQ_TEST_SERVER_NAME);

                final String prefix = "server";

                int shouldBeFoundCount = 1;
                
                for (int i = 0; i < serverCount - 1; i++) {
                    String name = prefix + String.format(" %03d", i + 1);
                    Server server = ServerFactory.newInstance();
                    server.setName(name);
                    switch (i % 5) {
                    case 0:
                    case 1:
                        server.setOperationMode(OperationMode.NORMAL);
                        break;
                    case 2:
                        server.setOperationMode(OperationMode.MAINTENANCE);
                        shouldBeFoundCount++;
                        serverNames.add(name);
                        break;
                    case 3:
                        server.setOperationMode(OperationMode.DOWN);
                        shouldBeFoundCount++;
                        serverNames.add(name);
                        break;
                    case 4:
                        server.setOperationMode(OperationMode.INSTALLED);
                        shouldBeFoundCount++;
                        serverNames.add(name);
                        break;
                    }
                    server.setAddress("address" + i);
                    server.setPort(7080 + i);
                    server.setSecurePort(7443 + i);

                    em.persist(server);
                    em.flush();
                }
                em.flush();

                // query the results and delete the servers
                final int pageSize = 24;
                ServerCriteria criteria = new ServerCriteria();
                criteria.addFilterName(prefix);
                criteria.addFilterOperationMode(OperationMode.MAINTENANCE, OperationMode.DOWN, OperationMode.INSTALLED);
                criteria.addSortName(PageOrdering.DESC); // use DESC just to make sure sorting on name is different than insert order
                criteria.setPaging(0, pageSize);

                // iterate over the results with CriteriaQuery
                CriteriaQueryExecutor<Server, ServerCriteria> queryExecutor = new CriteriaQueryExecutor<Server, ServerCriteria>() {
                    @Override
                    public PageList<Server> execute(ServerCriteria criteria) {
                        return topologyManager.findServersByCriteria(overlord, criteria);
                    }
                };

                // initiate first/(total depending on page size) request.
                CriteriaQuery<Server, ServerCriteria> servers = new CriteriaQuery<Server, ServerCriteria>(criteria,
                    queryExecutor);

                String prevName = null;
                // iterate over the entire result set efficiently
                int actualCount = 0;
                for (Server s : servers) {
                    assert null == prevName || s.getName().compareTo(prevName) < 0 || s.getName().equals(TestConstants.RHQ_TEST_SERVER_NAME) : "Results should be sorted by name DESC, something is out of order";
                    prevName = s.getName();
                    actualCount++;
                    if (!serverNames.contains(prevName)) {
                        fail("Following server entity shouldn't be here: " + s);
                    }
                    serverNames.remove(s.getName());
                }

                // test that entire list parsed spanning multiple pages
                assertTrue("Expected resourceNames to be empty. Still " + serverNames.size() + " name(s).",
                    serverNames.size() == 0);

                assertTrue("Expected " + shouldBeFoundCount + " to be parsed, but there were parsed " + actualCount
                    + " servers", actualCount == shouldBeFoundCount);
            }
        });
    }
    
    
    @Test(groups = "integration.ejb3")
    public void testParsingCriteriaQueryResultsStrict() throws Exception {

        final int serverCount = 10;
        executeInTransaction(new TransactionCallback() {

            public void execute() throws Exception {
                // verify that all server objects are actually parsed. 
                final Set<String> serverNames = new HashSet<String>(serverCount);

                final String namePrefix = "server";
                final String addressPrefix = "address";

                int shouldBeFoundCount = 1;
                serverNames.add(namePrefix + " 007");
                
                for (int i = 0; i < serverCount; i++) {
                    String name = namePrefix + String.format(" %03d", i + 1);
                    Server server = ServerFactory.newInstance();
                    server.setName(name);
                    server.setOperationMode(OperationMode.NORMAL);
                    server.setAddress(addressPrefix + i);
                    server.setPort(7080);
                    server.setSecurePort(7443);

                    em.persist(server);
                    em.flush();
                }
                em.flush();

                // query the results and delete the servers
                final int pageSize = 2;
                final int startPage = 0;
                ServerCriteria criteria = new ServerCriteria();
                criteria.addFilterName(namePrefix + " 007");
                criteria.setStrict(true);
                criteria.setPaging(startPage, pageSize);

                // iterate over the results with CriteriaQuery
                CriteriaQueryExecutor<Server, ServerCriteria> queryExecutor = new CriteriaQueryExecutor<Server, ServerCriteria>() {
                    @Override
                    public PageList<Server> execute(ServerCriteria criteria) {
                        return topologyManager.findServersByCriteria(overlord, criteria);
                    }
                };

                // initiate first/(total depending on page size) request.
                CriteriaQuery<Server, ServerCriteria> servers = new CriteriaQuery<Server, ServerCriteria>(criteria,
                    queryExecutor);

                String prevName = null;
                // iterate over the entire result set efficiently
                int actualCount = 0;
                for (Server s : servers) {
                    assert null == prevName || s.getName().compareTo(prevName) < 0 : "Results should be sorted by name DESC, something is out of order";
                    prevName = s.getName();
                    actualCount++;
                    serverNames.remove(s.getName());
                }

                // test that entire list parsed spanning multiple pages
                assertTrue("Expected resourceNames to be empty. Still " + serverNames.size() + " name(s).",
                    serverNames.size() == 0);

                assertTrue("Expected " + shouldBeFoundCount + " to be parsed, but there were parsed " + actualCount
                    + " servers", actualCount == shouldBeFoundCount);
            }
        });
    }

    @Test(groups = "integration.ejb3")
    public void testParsingAllCriteriaQueryResults1() throws Exception {
        int startPage = 0;
        int pageSize = 3;
        int serverCount = 605;
        testParsingHelperStartingPageEqualTo(startPage, pageSize, serverCount);
    }

    @Test(groups = "integration.ejb3", enabled = true)
    public void testParsingAllCriteriaQueryResults2() throws Exception {
        int startPage = 1;
        int pageSize = 3;
        int serverCount = 605;
        testParsingHelperStartingPageEqualTo(startPage, pageSize, serverCount);
    }

    @Test(groups = "integration.ejb3", enabled = true)
    public void testParsingAllCriteriaQueryResults3() throws Exception {
        int startPage = 2;
        int pageSize = 3;
        int serverCount = 609;
        testParsingHelperStartingPageEqualTo(startPage, pageSize, serverCount);
    }

    @Test(groups = "integration.ejb3", enabled = true)
    public void testParsingAllCriteriaQueryResults4() throws Exception {
        int startPage = 3;
        int pageSize = 7;
        int serverCount = 800;
        testParsingHelperStartingPageEqualTo(startPage, pageSize, serverCount);
    }

    @Test(groups = "integration.ejb3", enabled = true)
    public void testParsingAllCriteriaQueryResults5() throws Exception {
        int startPage = 4;
        int pageSize = 2;
        int serverCount = 444;
        testParsingHelperStartingPageEqualTo(startPage, pageSize, serverCount);
    }

    @Test(groups = "integration.ejb3", enabled = true)
    public void testParsingAllCriteriaQueryResults6() throws Exception {
        int startPage = 5;
        int pageSize = 3;
        int serverCount = 605;
        testParsingHelperStartingPageEqualTo(startPage, pageSize, serverCount);
    }

    @Test(groups = "integration.ejb3", enabled = true)
    public void testParsingAllCriteriaQueryResults7() throws Exception {
        int startPage = 1;
        int pageSize = 5;
        int serverCount = 934;
        testParsingHelperStartingPageEqualTo(startPage, pageSize, serverCount);
    }

    @Test(groups = "integration.ejb3", enabled = true)
    public void testParsingAllCriteriaQueryResults8() throws Exception {
        int startPage = 4;
        int pageSize = 7;
        int serverCount = 1234;
        testParsingHelperStartingPageEqualTo(startPage, pageSize, serverCount);
    }

    @Test(groups = "integration.ejb3", enabled = true)
    public void testParsingAllCriteriaQueryResults9() throws Exception {
        int startPage = 2;
        int pageSize = 1;
        int serverCount = 109;
        testParsingHelperStartingPageEqualTo(startPage, pageSize, serverCount);
    }

    @Test(groups = "integration.ejb3", enabled = true)
    public void testParsingAllCriteriaQueryResults10() throws Exception {
        int startPage = 0;
        int pageSize = 11;
        int serverCount = 999;
        testParsingHelperStartingPageEqualTo(startPage, pageSize, serverCount);
    }

    @Test(groups = "integration.ejb3")
    public void testFindNonExistentServer() throws Exception {

        final int serverCount = 5;

        executeInTransaction(new TransactionCallback() {
            public void execute() throws Exception {

                final String namePrefix = "server";
                final String addressPrefix = "address";
                
                for (int i = 0; i < serverCount; i++) {
                    String name = namePrefix + String.format(" %03d", i + 1);
                    Server server = ServerFactory.newInstance();
                    server.setName(name);
                    server.setOperationMode(OperationMode.NORMAL);
                    server.setAddress(addressPrefix + i);
                    server.setPort(7080);
                    server.setSecurePort(7443);

                    em.persist(server);
                    em.flush();
                }
                em.flush();

                ServerCriteria criteria = new ServerCriteria();
                criteria.addFilterName("very unlikely name of a server");
                criteria.setStrict(true);
                PageList<Server> servers = topologyManager.findServersByCriteria(overlord, criteria);
                assertNotNull("The result of topologyManager.findServersByCriteria() is null", servers);
                assertTrue("Some servers have been found, even if they shouldn't", servers.isEmpty());
                
                criteria = new ServerCriteria();
                criteria.addFilterSecurePort(1000);
                servers = topologyManager.findServersByCriteria(overlord, criteria);
                assertNotNull("The result of topologyManager.findServersByCriteria() is null", servers);
                assertTrue("Some servers have been found, even if they shouldn't", servers.isEmpty());
                
                criteria = new ServerCriteria();
                criteria.addFilterAffinityGroupId(Integer.MAX_VALUE / 2);
                servers = topologyManager.findServersByCriteria(overlord, criteria);
                assertNotNull("The result of topologyManager.findServersByCriteria() is null", servers);
                assertTrue("Some servers have been found, even if they shouldn't", servers.isEmpty());
            }
        });
    }
    
    
    private void testParsingHelperStartingPageEqualTo(final int startPage, final int pageSize, final int serverCount) throws Exception {

        executeInTransaction(new TransactionCallback() {
            public void execute() throws Exception {
                // verify that all server objects are actually parsed. 
                final Set<String> serverNames = new TreeSet<String>();
                final String namePrefix = "server";
                final String addressPrefix = "address";
                int shouldBeFoundCount = 0;
                int shouldBeSkipped = pageSize * startPage;

                for (int i = 0; i < serverCount; i++) {
                    String name = namePrefix + String.format(" %03d", i + 1);
                    Server server = ServerFactory.newInstance();
                    server.setName(name);
                    switch (i % 2) {
                    case 0:
                        server.setOperationMode(OperationMode.NORMAL);
                        break;
                    case 1:
                        server.setOperationMode(OperationMode.MAINTENANCE);
                        if (i % 20 == 9) {
                            shouldBeFoundCount++;
                            serverNames.add(name);
                        }
                        break;
                    }
                    server.setAddress(addressPrefix + i);
                    server.setPort(7080 + (i % 20));
                    server.setSecurePort(7443 + (i % 20));

                    em.persist(server);
                    em.flush();
                }
                em.flush();
                
                if (shouldBeSkipped > 0) {
                    // delete the members to be skipped because of the start page
                    String[] serverNamesArray = serverNames.toArray(new String[serverNames.size()]);
                    for (int i = 0; i < shouldBeSkipped; i++) {
                        serverNames.remove(serverNamesArray[serverNamesArray.length - i - 1]);
                    }
                    shouldBeFoundCount -= shouldBeSkipped;
                }

                // query the results and delete the servers
                ServerCriteria criteria = new ServerCriteria();
                criteria.addFilterOperationMode(OperationMode.MAINTENANCE);
                criteria.addFilterPort(7089);
                criteria.addFilterSecurePort(7452);
                criteria.addFilterName(namePrefix);
                criteria.addFilterAddress(addressPrefix);
                criteria.addSortName(PageOrdering.DESC); // use DESC just to make sure sorting on name is different than insert order
                criteria.setPaging(startPage, pageSize);

                // iterate over the results with CriteriaQuery
                CriteriaQueryExecutor<Server, ServerCriteria> queryExecutor = new CriteriaQueryExecutor<Server, ServerCriteria>() {
                    @Override
                    public PageList<Server> execute(ServerCriteria criteria) {
                        return topologyManager.findServersByCriteria(overlord, criteria);
                    }
                };

                // initiate first/(total depending on page size) request.
                CriteriaQuery<Server, ServerCriteria> servers = new CriteriaQuery<Server, ServerCriteria>(criteria,
                    queryExecutor);

                String prevName = null;
                // iterate over the entire result set efficiently
                int actualCount = 0;
                for (Server s : servers) {
                    assert null == prevName || s.getName().compareTo(prevName) < 0 : "Results should be sorted by name DESC, something is out of order";
                    prevName = s.getName();
                    actualCount++;
                    serverNames.remove(s.getName());
                }

                // test that entire list parsed spanning multiple pages
                assertTrue("Expected resourceNames to be empty. Still " + serverNames.size() + " name(s).",
                    serverNames.size() == 0);

                assertTrue("Expected " + shouldBeFoundCount + " to be parsed, but there were parsed " + actualCount
                    + " servers", actualCount == shouldBeFoundCount);
            }
        });
    }
}
