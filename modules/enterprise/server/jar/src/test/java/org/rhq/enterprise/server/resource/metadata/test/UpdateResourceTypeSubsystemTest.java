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

import java.util.Iterator;
import java.util.Set;

import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.ResourceType;

public class UpdateResourceTypeSubsystemTest extends UpdateSubsytemTestBase {

    @Override
    protected String getSubsystemDirectory() {
        return "resource-type";
    }

    /**
     * See if deletion of a resource type just works
     *
     * @throws Exception
     */
    @Test
    public void testResourceTypeDeletion() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("update4-v1_0.xml");
            ResourceType platform1 = getResourceType("myPlatform4");
            Set<ResourceType> servers1 = platform1.getChildResourceTypes();
            assert servers1.size() == 2 : "Expected to find 2 servers in v1";
            int found = 0;
            for (ResourceType server : servers1) {
                if (server.getName().equals("testServer1")) {
                    found++;
                }

                if (server.getName().equals("testServer2")) {
                    found++;
                }
            }

            assert found == 2 : "I did not find the expected servers in v1";

            registerPlugin("update4-v2_0.xml");
            ResourceType platform2 = getResourceType("myPlatform4");
            Set<ResourceType> servers2 = platform2.getChildResourceTypes();
            assert servers2.size() == 1 : "Expected to find 1 servers in v2";
            ResourceType server2 = servers2.iterator().next();
            assert server2.getName().equals("testServer1");
            Set<MeasurementDefinition> mdef = server2.getMetricDefinitions();
            assert mdef.size() == 1 : "Expected one MeasurementDefinition in v2";

            registerPlugin("update4-v1_0.xml");
            ResourceType platform3 = getResourceType("myPlatform4");
            Set<ResourceType> servers3 = platform3.getChildResourceTypes();
            assert servers3.size() == 2 : "Expected to find 2 servers in v1/2";
            found = 0;
            for (ResourceType server : servers3) {
                if (server.getName().equals("testServer1")) {
                    found++;
                }

                if (server.getName().equals("testServer2")) {
                    found++;
                }
            }

            assert found == 2 : "I did not find the expected servers in v1/2";
        } finally {
            getTransactionManager().rollback();
        }
    }

    //   @Test  TODO further work on this
    public void testMoveResoureType() throws Exception {
        System.out.println("testUpdatePlugin2 --- start");
        getTransactionManager().begin();
        try {
            registerPlugin("update2-v1_0.xml");
            ResourceType platform1 = getResourceType("myPlatform");
            assert platform1 != null : "I did not find myPlatform";
            Set<MeasurementDefinition> defs = platform1.getMetricDefinitions();
            assert defs.size() == 1 : "I was expecting 1 definition at platform level in v1";
            assert DisplayType.DETAIL == defs.iterator().next().getDisplayType() : "Display type should be DETAIL in v1";

            // one child service in v1
            Set<ResourceType> platformChildren = platform1.getChildResourceTypes();
            assert platformChildren.size() == 1 : "Expected 1 direct child service of platform in v1";
            ResourceType service1 = platformChildren.iterator().next();
            assert service1.getName().equals("service1") : "Expected 'service1' as name of direct platform child in v1";
            assert service1.getMetricDefinitions().size() == 1 : "Expected 1 metric for 'service1' in v1";
            Set<ResourceType> nestedService = service1.getChildResourceTypes();
            assert nestedService.size() == 1 : "Expected 1 nested service of 'service1' in v1";
            Set<MeasurementDefinition> nestedDefs = nestedService.iterator().next().getMetricDefinitions();
            assert nestedDefs.size() == 1 : "Expected 1 definition within 'nestedService' in v1";
            MeasurementDefinition defThree = nestedDefs.iterator().next();
            int definitionId = defThree.getId(); // get the id of the definition "Three" and save it for later use

            getEntityManager().flush();

            System.out.println("testUpdatePlugin2 -- done with the first plugin version");

            /*
             * The nested service got pulled out and put under platform
             */
            registerPlugin("update2-v2_0.xml");
            ResourceType platform2 = getResourceType("myPlatform");
            assert platform2 != null : "I did not find myPlatform";
            Set<MeasurementDefinition> defs2 = platform2.getMetricDefinitions();
            assert defs2.size() == 1 : "I was expecting 1 definition at platform level in v2";
            assert DisplayType.SUMMARY == defs2.iterator().next().getDisplayType() : "Display type should be SUMMARY in v2";

            // two children in v2
            Set<ResourceType> platformChildren2 = platform2.getChildResourceTypes();
            assert platformChildren2.size() == 2 : "Expected 2 direct child services of platform in v2";
            Iterator<ResourceType> iter = platformChildren2.iterator();
            while (iter.hasNext()) {
                ResourceType type = iter.next();
                String typeName = type.getName();
                assert type.getMetricDefinitions().size() == 1 : "Expected one definition for " + typeName + " in v2";
                if (typeName.equals("nestedOne")) // The moved one
                {
                    Set<MeasurementDefinition> defs3 = type.getMetricDefinitions();
                    MeasurementDefinition three = defs3.iterator().next();
                    assert three.getDisplayName().equals("Three") : "Expected the nestedOne to have a metric withDisplayName Three in v2, but it was "
                        + three.getDisplayName();
                    assert three.getDisplayType() == DisplayType.SUMMARY : "Expected three to be SUMMARY in v2";

                    /*
                     * TODO check here if the Definition is the one from above which got moved. this should be the case.
                     * Else we would loose all measurment schedules (and disociate them from measurement data. But the
                     * later is a different story. We probably should cascade that anyway.
                     */
                    System.out.println("Expected the id of 'Three' to be " + definitionId + " but it was "
                        + three.getId() + " in v2");
                    assert three.getId() == definitionId : "Expected the id of 'Three' to be " + definitionId
                        + " but it was " + three.getId() + " in v2";
                } else if (typeName.equals("service1")) {
                    // check that the nested service is gone
                    Set<ResourceType> childrenOfService = type.getChildResourceTypes();
                    assert childrenOfService.size() == 0 : "No children of 'service1' expected in v2, but found: "
                        + childrenOfService.size();
                } else {
                    assert true == false : "We found an unknown type with name " + typeName;
                }
            }
        }

        // TODO now that we're done here, apply v1 again to also test the other direction
        finally {
            getTransactionManager().rollback();
        }

        System.out.println("testUpdatePlugin2 --- end");
    }

    @Test
    public void testDuplicateResourceType() throws Exception {
        System.out.println("= testDuplicateResourceType");
        getTransactionManager().begin();
        try {
            System.out.println(" A stack trace coming out of this is expected");
            System.out.flush();
            registerPlugin("duplicateResourceType.xml");
            getResourceType("ops");
            assert false : "We should not have hit this line";
        } catch (Exception e) {
            ; // We expect an exception to come out of the ResourceMetadataManager
        } finally {
            getTransactionManager().rollback();
        }
    }
}
