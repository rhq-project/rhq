/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

import javax.persistence.EntityManager;
import javax.transaction.Status;

import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.ResourceTypeBundleConfiguration;
import org.rhq.core.domain.bundle.ResourceTypeBundleConfiguration.BundleDestinationBaseDirectory;
import org.rhq.core.domain.bundle.ResourceTypeBundleConfiguration.BundleDestinationBaseDirectory.Context;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Note, plugins are registered in new transactions. For tests, this means
 * you can't do everything in a trans and roll back at the end. You must clean up manually.
 */
public class UpdateResourceTypeSubsystemTest extends UpdateSubsytemTestBase {

    @Override
    protected String getSubsystemDirectory() {
        return "resource-type";
    }

    /**
     * Tests updating bundle-target config
     */
    @Test
    public void testResourceTypeBundleTarget() throws Exception {
        try {
            // register the plugin - it has a platform with child server that is a bundle target
            registerPlugin("updateResourceTypeBundleTarget-v1.xml");
            ResourceType platform1 = getResourceType("myPlatform1");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            platform1 = em.find(ResourceType.class, platform1.getId());

            assert platform1.getResourceTypeBundleConfiguration() == null : "platform should not be a bundle target";
            Set<ResourceType> servers1 = platform1.getChildResourceTypes();

            assert servers1.size() == 1 : "must only have one child server under the test platform";
            ResourceType server1 = servers1.iterator().next();
            ResourceTypeBundleConfiguration bundleConfig1 = server1.getResourceTypeBundleConfiguration();
            assert bundleConfig1 != null : "server should have been a bundle target";

            Set<BundleDestinationBaseDirectory> baseDirs1 = bundleConfig1.getBundleDestinationBaseDirectory();
            assert baseDirs1.size() == 2 : "should have been 2 bundle dest base dirs: " + baseDirs1;

            for (BundleDestinationBaseDirectory baseDir : baseDirs1) {
                if (baseDir.getName().equals("firstDestBaseDir")) {
                    assert baseDir.getValueContext() == Context.pluginConfiguration : "bad context: " + baseDir;
                    assert baseDir.getValueName().equals("prop1") : "bad value" + baseDir;
                } else if (baseDir.getName().equals("secondDestBaseDir")) {
                    assert baseDir.getValueContext() == Context.fileSystem : "bad context: " + baseDir;
                    assert baseDir.getValueName().equals("/") : "bad value" + baseDir;
                } else {
                    assert false : "wrong dest base dir was retrieved: " + baseDir;
                }
            }

            getTransactionManager().rollback();

            // now upgrade the plugin - the bundle config will have changed in the server
            registerPlugin("updateResourceTypeBundleTarget-v2.xml");
            ResourceType platform2 = getResourceType("myPlatform1");
            getTransactionManager().begin();
            em = getEntityManager();
            platform2 = em.find(ResourceType.class, platform2.getId());

            assert platform1.getResourceTypeBundleConfiguration() == null : "platform should not be a bundle target";
            Set<ResourceType> servers2 = platform2.getChildResourceTypes();

            assert servers2.size() == 1 : "Expected to find 1 server";
            ResourceType server2 = servers2.iterator().next();
            ResourceTypeBundleConfiguration bundleConfig2 = server2.getResourceTypeBundleConfiguration();
            assert bundleConfig2 != null : "server should have been a bundle target";

            Set<BundleDestinationBaseDirectory> baseDirs2 = bundleConfig2.getBundleDestinationBaseDirectory();
            assert baseDirs2.size() == 1 : "should have been 1 bundle dest base dir: " + baseDirs2;

            BundleDestinationBaseDirectory baseDir = baseDirs2.iterator().next();
            assert baseDir.getName().equals("thirdDestBaseDir");
            assert baseDir.getValueContext() == Context.resourceConfiguration : "bad context: " + baseDir;
            assert baseDir.getValueName().equals("resourceProp1") : "bad value" + baseDir;

            // make sure the old bundle config was deleted when we upgraded and overwrite it with the new config
            assert null == em.find(Configuration.class, bundleConfig1.getBundleConfiguration().getId()) : "The configuration "
                + bundleConfig1 + " should have been deleted";

            getTransactionManager().rollback();

        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testResourceTypeBundleTarget");
            }
        }
    }

    /**
     * See if deletion of a resource type just works
     *
     * @throws Exception on error
     */
    @Test
    public void testResourceTypeDeletion() throws Exception {
        try {
            registerPlugin("update4-v1_0.xml");
            ResourceType platform1 = getResourceType("myPlatform4");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            platform1 = em.find(ResourceType.class, platform1.getId());

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
            getTransactionManager().rollback();

            registerPlugin("update4-v2_0.xml");
            ResourceType platform2 = getResourceType("myPlatform4");
            getTransactionManager().begin();
            em = getEntityManager();
            platform2 = em.find(ResourceType.class, platform2.getId());

            Set<ResourceType> servers2 = platform2.getChildResourceTypes();
            assert servers2.size() == 1 : "Expected to find 1 servers in v2";
            ResourceType server2 = servers2.iterator().next();
            assert server2.getName().equals("testServer1");
            Set<MeasurementDefinition> mdef = server2.getMetricDefinitions();
            assert mdef.size() == 1 : "Expected one MeasurementDefinition in v2";
            getTransactionManager().rollback();

            registerPlugin("update4-v1_0.xml", "3.0");
            ResourceType platform3 = getResourceType("myPlatform4");
            getTransactionManager().begin();
            em = getEntityManager();
            platform3 = em.find(ResourceType.class, platform3.getId());

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
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testResourceTypeDeletion");
            }
        }
    }

    @Test
    /**
     * Tests moving a resource type to a new parent resource type.
     */
    public void testMoveResourceType() throws Exception {
        System.out.println("testMoveResourceType --- start");
        try {
            registerPlugin("update2-v1_0.xml");

            ResourceType platform1 = getResourceType("myPlatform");
            Resource platformResource = createResource("foo-myPlatform", "foo-myPlatform", platform1);
            ResourceType service1 = getResourceType("service1");
            Resource service1Resource = createResource("foo-service1", "foo-service1", service1);
            platformResource.addChildResource(service1Resource);
            ResourceType nestedOne = getResourceType("nestedOne");
            Resource nestedOneResource = createResource("foo-nestedOne", "foo-nestedOne", nestedOne);
            service1Resource.addChildResource(nestedOneResource);
            Subject overlord = LookupUtil.getSubjectManager().getOverlord();
            resourceManager.createResource(overlord, platformResource, -1);

            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            platform1 = em.find(ResourceType.class, platform1.getId());

            assert platform1 != null : "I did not find myPlatform";
            Set<MeasurementDefinition> defs = platform1.getMetricDefinitions();
            assert defs.size() == 1 : "I was expecting 1 metric definition at platform level in v1";
            assert DisplayType.DETAIL == defs.iterator().next().getDisplayType() : "Display type should be DETAIL in v1";

            // one child service in v1
            Set<ResourceType> platformChildren = platform1.getChildResourceTypes();
            assert platformChildren.size() == 1 : "Expected 1 direct child service of platform in v1";
            service1 = platformChildren.iterator().next();
            assert service1.getName().equals("service1") : "Expected 'service1' as name of direct platform child in v1";
            assert service1.getMetricDefinitions().size() == 1 : "Expected 1 metric for 'service1' in v1";
            Set<ResourceType> nestedServices = service1.getChildResourceTypes();
            assert nestedServices.size() == 1 : "Expected 1 nested service of 'service1' in v1";
            Set<MeasurementDefinition> nestedDefs = nestedServices.iterator().next().getMetricDefinitions();
            assert nestedDefs.size() == 1 : "Expected 1 definition within 'nestedService' in v1";
            MeasurementDefinition defThree = nestedDefs.iterator().next();
            int definitionId = defThree.getId(); // get the id of the definition "Three" and save it for later use
            getTransactionManager().rollback();

            System.out.println("testMoveResourceType -- done with the first plugin version");

            /*
             * The nested service got pulled out and put under platform
             */
            registerPlugin("update2-v2_0.xml");
            ResourceType platform2 = getResourceType("myPlatform");
            getTransactionManager().begin();
            em = getEntityManager();
            platform2 = em.find(ResourceType.class, platform2.getId());

            assert platform2 != null : "I did not find myPlatform";
            Set<MeasurementDefinition> defs2 = platform2.getMetricDefinitions();
            assert defs2.size() == 1 : "I was expecting 1 definition at platform level in v2";
            assert DisplayType.SUMMARY == defs2.iterator().next().getDisplayType() : "Display type should be SUMMARY in v2";

            ResourceCriteria resourceCriteria = new ResourceCriteria();
            resourceCriteria.setStrict(true);
            resourceCriteria.addFilterResourceKey("foo-myPlatform");
            resourceCriteria.fetchChildResources(true);
            Resource platform2Resource = getResource(resourceCriteria);
            assert platform2Resource != null : "Expected to find platform Resource in db.";

            // two children in v2
            Set<ResourceType> platformChildren2 = platform2.getChildResourceTypes();
            assert platformChildren2.size() == 2 : "Expected 2 direct child service types of platform in v2";

            Set<Resource> platform2ChildResources = platform2Resource.getChildResources();
            assert platform2ChildResources.size() == 2 : "Expected 2 direct child services of platform in v2";
            boolean foundMovedResource = false;
            for (Resource childResource : platform2ChildResources) {
                assert childResource.getChildResources().isEmpty() : "Expected child Resource " + childResource
                    + " to have no children";
                if (childResource.getResourceKey().equals("foo-nestedOne")) {
                    foundMovedResource = true;
                }
            }
            assert foundMovedResource : "Expected 'foo-nestedOne' Resource to have been moved directly under platform Resource";

            for (ResourceType type : platformChildren2) {
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
                     * Else we would loose all measurement schedules (and disassociate them from measurement data. But the
                     * latter is a different story. We probably should cascade that anyway.
                     */
                    assert three.getId() == definitionId : "Expected the id of 'Three' to be " + definitionId
                        + ", but it was " + three.getId() + " in v2";
                } else if (typeName.equals("service1")) {
                    // check that the nested service is gone
                    Set<ResourceType> childrenOfService = type.getChildResourceTypes();
                    assert childrenOfService.size() == 0 : "No children of 'service1' expected in v2, but found: "
                        + childrenOfService.size();
                } else {
                    assert true == false : "We found an unknown type with name " + typeName;
                }
            }

            // TODO now that we're done here, apply v1 again to also test the other direction
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out
                    .println("CANNOT CLEAN UP TEST: " + this.getClass().getSimpleName() + ".testMoveResourceType");
            }
        }

        System.out.println("testMoveResourceType --- end");
    }

    @Test
    public void testDuplicateResourceType() throws Exception {
        System.out.println("= testDuplicateResourceType");
        try {
            System.out.println("NOTE: A stack trace coming out of this is expected.");
            registerPlugin("duplicateResourceType.xml");
            getResourceType("ops");
            assert false : "We should not have hit this line";
        } catch (Exception e) {
            // ignore - We expect an exception to come out of the ResourceMetadataManager
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testDuplicateResourceType");
            }
        }
    }
}
