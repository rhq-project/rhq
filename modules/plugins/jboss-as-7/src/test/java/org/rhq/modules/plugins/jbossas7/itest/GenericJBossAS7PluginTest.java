/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7.itest;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.plugin.testutil.AbstractAgentPluginTest;
import org.rhq.modules.plugins.jbossas7.itest.domain.ManagedServerTest;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * AS7 plugin tests that are not specific to particular Resource types. The tests delegate to methods in
 * {@link AbstractAgentPluginTest}, which provides generic impls of such tests.
 *
 * @author Ian Springer
 */
@Test(groups = {"integration", "pc"}, singleThreaded = true)
public class GenericJBossAS7PluginTest extends AbstractJBossAS7PluginTest {

    // ****************************** LIFECYCLE ****************************** //
    // TODO: Re-enable this once the issue with Resources not getting auto-imported has been fixed.
    @Test(priority = 1, enabled = false)
    @RunDiscovery
    public void testAllResourceComponentsStarted() throws Exception {
        assertAllResourceComponentsStarted();
    }

    // ******************************* METRICS ******************************* //
    @Test(priority = 2)
    @RunDiscovery
    public void testAllMetricsHaveNonNullValues() throws Exception {
        Map<ResourceType, String[]> excludedMetricNamesByType = new HashMap<ResourceType, String[]>();
        // It's normal for the "startTime" trait to be null for a Managed Server that is down/disabled.
        // It's normal for the "multicastAddress" trait to be null for a Managed Server that is not configured for JGroups HA.
        excludedMetricNamesByType.put(ManagedServerTest.RESOURCE_TYPE, new String[] {"startTime", "multicastAddress"});
        // Some memory pools do not expose those statistics (by default), so in case they
        // are not exposed, it is normal that the server returns 'undefined' for the value
        // Note that those
        excludedMetricNamesByType.put(new ResourceType("Memory Pool Resource", PLUGIN_NAME, ResourceCategory.SERVICE,
            null), new String[] { "collection-usage-threshold-count", "collection-usage-threshold", "collection-usage",
            "collection-usage-threshold-exceeded", "collection-usage:committed", "collection-usage:init",
            "collection-usage:max", "collection-usage:used", "usage-threshold-count", "usage-threshold-exceeded" });

        //the max-connections will be 'undefined' if no specific value is set. This is AS's way of saying the value
        //is connector specific
        excludedMetricNamesByType.put(new ResourceType("Connector (Managed Server)", PLUGIN_NAME, ResourceCategory.SERVICE, null),
            new String[] {"_expr:max-connections"});
        excludedMetricNamesByType.put(new ResourceType("Connector", PLUGIN_NAME, ResourceCategory.SERVICE, null),
            new String[] {"_expr:max-connections"});

        assertAllNumericMetricsAndTraitsHaveNonNullValues(excludedMetricNamesByType);
    }

    // **************************** RESOURCE CONFIG ************************** //
    @Test(priority = 3)
    public void testAllResourceConfigsLoad() throws Exception {
        assertAllResourceConfigsLoad();
    }

}
