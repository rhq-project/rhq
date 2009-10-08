/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.resource.metadata.test;

import java.util.Set;

import org.testng.annotations.Test;

import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.domain.resource.ResourceType;

public class UpdateNativesSubsystemTest extends UpdateSubsytemTestBase {

    @Override
    protected String getSubsystemDirectory() {
        return "natives";
    }

    /**
     * Tests the behaviour of the MetadataManager wrt <process-scan> entries.
     *
     * @throws Exception
     */
    @Test
    public void testProcessScans() throws Exception {
        getTransactionManager().begin();
        try {
            ResourceType server1 = getServer1ForConfig5(null);

            /*
             * TODO check process scans as well
             */
            Set<ProcessScan> scans1 = server1.getProcessScans();
            assert scans1.size() == 3 : "Expected to find 3 process scans in v1, but got " + scans1.size();
            int found = 0;
            for (ProcessScan ps : scans1) {
                if (containedIn(ps.getName(), new String[] { "JBoss4", "JBoss5", "JBoss6" })) {
                    found++;
                }
            }

            assert found == 3 : "Expected to find 3 process scans in v1";
            // TODO also check query

            getEntityManager().flush();

            /*
             * check process scans in v2 as well
             */
            ResourceType server2 = getServer2ForConfig5(null);
            Set<ProcessScan> scans2 = server2.getProcessScans();
            assert scans2.size() == 3 : "Expected to find 3 process scans in v2, but got " + scans2.size();
            found = 0;
            for (ProcessScan ps : scans2) {
                if (containedIn(ps.getName(), new String[] { "JBoss5", "JBoss6", "Hibernate" })) {
                    found++;
                }
            }

            assert found == 3 : "Expected to find 3 specific process scans in v2, but got " + found;

            getEntityManager().flush();

            /*
             * Now return to first version of plugin
             */
            server1 = getServer1ForConfig5("3.0");
            scans1 = server1.getProcessScans();
            assert scans1.size() == 3 : "Expected to find 3 process scans in v1, but got " + scans1.size();
            found = 0;
            for (ProcessScan ps : scans1) {
                if (containedIn(ps.getName(), new String[] { "JBoss4", "JBoss5", "JBoss6" })) {
                    found++;
                }
            }

            assert found == 3 : "Expected to find 3 specific process scans in v1 again, but got " + found;
        } finally {
            getTransactionManager().rollback();
        }
    }

    protected ResourceType getServer1ForConfig5(String version) throws Exception {
        registerPlugin("update5-v1_0.xml", version);
        ResourceType platform1 = getResourceType("myPlatform5");
        Set<ResourceType> servers = platform1.getChildResourceTypes();
        assert servers.size() == 1 : "Expected to find 1 server in v1, but got " + servers.size();
        ResourceType server1 = servers.iterator().next();
        return server1;
    }

    protected ResourceType getServer2ForConfig5(String version) throws Exception {
        registerPlugin("update5-v2_0.xml", version);
        ResourceType platform2 = getResourceType("myPlatform5");
        Set<ResourceType> servers2 = platform2.getChildResourceTypes();
        assert servers2.size() == 1 : "Expected to find 1 server in v2, but got " + servers2.size();
        ResourceType server2 = servers2.iterator().next();
        return server2;
    }
}
