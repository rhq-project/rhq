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
package org.rhq.plugins.perftest;

import java.util.Set;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.domain.content.PackageCategory;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.perftest.content.ContentFactory;
import org.rhq.plugins.perftest.resource.ResourceFactory;

/**
 * @author Jason Dobies
 */
public class ScenarioManagerTest {
    @BeforeSuite
    public void setScenario() {
        System.setProperty(ScenarioManager.SCENARIO_PROPERTY, "unit-test-scenario");
    }

    @Test
    public void discoverResources() {
        // Setup
        ResourceType resourceType = new ResourceType("server-a", "plugin", ResourceCategory.SERVER, null);
        ResourceDiscoveryContext context = new ResourceDiscoveryContext(resourceType, null, null, null, null, null, "test");

        // Test
        ScenarioManager manager = ScenarioManager.getInstance();
        ResourceFactory resourceFactory = manager.getResourceFactory("server-a");
        Set<DiscoveredResourceDetails> resources = resourceFactory.discoverResources(context);

        // Verify
        assert resources != null : "Null set of resources returned from resource factory";
        assert resources.size() == 10 : "Incorrect number of resources returned from resource factory. Expected: 10, Found: "
            + resources.size();
    }

    @Test
    public void discoverNoResources() {
        // Setup
        ResourceType resourceType = new ResourceType("server-c", "plugin", ResourceCategory.SERVER, null);
        ResourceDiscoveryContext context = new ResourceDiscoveryContext(resourceType, null, null, null, null, null, "test");

        // Test
        ScenarioManager manager = ScenarioManager.getInstance();
        ResourceFactory resourceFactory = manager.getResourceFactory("server-c");
        Set<DiscoveredResourceDetails> resources = resourceFactory.discoverResources(context);

        // Verify
        assert resources != null : "Null set of resources returned from resource factory";
        assert resources.size() == 0 : "Incorrect number of resources returned from resource factory. Expected: 0, Found: "
            + resources.size();
    }

    @Test
    public void configurableResources() {
        // Setup
        System.setProperty("on.perftest.server-b-test", "20");

        ResourceType resourceType = new ResourceType("server-b", "plugin", ResourceCategory.SERVER, null);
        ResourceDiscoveryContext context = new ResourceDiscoveryContext(resourceType, null, null, null, null, null, "test");

        // Test
        ScenarioManager manager = ScenarioManager.getInstance();
        ResourceFactory resourceFactory = manager.getResourceFactory("server-b");
        Set<DiscoveredResourceDetails> resources = resourceFactory.discoverResources(context);

        // Verify
        assert resources != null : "Null set of resources returned from resource factory";
        assert resources.size() == 20 : "Incorrect number of resources returned from resource factory. Expected: 20, Found: "
            + resources.size();
    }

    @Test
    public void content() {
        // Setup
        PackageType artifactType1 = new PackageType();
        artifactType1.setName("artifact1");
        artifactType1.setDescription("");
        artifactType1.setCategory(PackageCategory.CONFIGURATION);

        PackageType artifactType2 = new PackageType();
        artifactType2.setName("artifact2");
        artifactType2.setDescription("");
        artifactType2.setCategory(PackageCategory.CONFIGURATION);

        // Test
        ScenarioManager manager = ScenarioManager.getInstance();

        ContentFactory artifactFactory;

        // Type: artifact1
        artifactFactory = manager.getContentFactory("server-a", "artifact1");

        assert artifactFactory != null : "Null artifact factory for artifact1";

        Set<ResourcePackageDetails> artifacts = artifactFactory.discoverContent(artifactType1);

        assert artifacts != null : "Null set of artifacts returned from factory for artifact1";
        assert artifacts.size() == 5 : "Incorrect number of artifacts returned from factory for artifact1. Expected: 5, Found: "
            + artifacts.size();

        ResourcePackageDetails packageDetails = artifacts.iterator().next();

        // Type: artifact2
        artifactFactory = manager.getContentFactory("server-a", "artifact2");

        assert artifactFactory != null : "Null artifact factory for artifact2";

        artifacts = artifactFactory.discoverContent(artifactType2);

        assert artifacts != null : "Null set of artifacts returned from factory for artifact2";
        assert artifacts.size() == 1 : "Incorrect number of artifacts returned from factory for artifact2. Expected: 1, Found: "
            + artifacts.size();

        packageDetails = artifacts.iterator().next();

        // Type: artifact3
        artifactFactory = manager.getContentFactory("server-a", "artifact3");
        assert artifactFactory == null : "Incorrect artifact factory returned for artifact3";
    }
}