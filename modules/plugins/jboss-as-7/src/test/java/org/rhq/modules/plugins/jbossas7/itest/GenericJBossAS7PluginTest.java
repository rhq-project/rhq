package org.rhq.modules.plugins.jbossas7.itest;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.modules.plugins.jbossas7.itest.domain.ManagedServerTest;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * Generic AS7 plugin tests.
 *
 * @author Ian Springer
 */
@Test(groups = {"integration", "pc"}, singleThreaded = true)
public class GenericJBossAS7PluginTest extends AbstractJBossAS7PluginTest {

    // ****************************** LIFECYCLE ****************************** //
    @Test(priority = 1)
    @RunDiscovery
    public void testAllResourceComponentsStarted() throws Exception {
        Thread.sleep(30*1000L);
        assertAllResourceComponentsStarted();
    }

    // ******************************* METRICS ******************************* //
    @Test(priority = 2)
    public void testAllMetricsHaveNonNullValues() throws Exception {
        Map<ResourceType, String[]> excludedMetricNamesByType = new HashMap<ResourceType, String[]>();
        // It's normal for the "startTime" trait to be null for a Managed Server that is down/disabled.
        excludedMetricNamesByType.put(ManagedServerTest.RESOURCE_TYPE, new String[] {"startTime"});
        assertAllNumericMetricsAndTraitsHaveNonNullValues(excludedMetricNamesByType);
    }

    // **************************** RESOURCE CONFIG ************************** //
    @Test(priority = 3)
    public void testAllResourceConfigsLoad() throws Exception {
        assertAllResourceConfigsLoad();
    }

}
