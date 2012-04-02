package org.rhq.modules.plugins.jbossas7.itest;

import org.testng.annotations.Test;

import org.rhq.test.arquillian.RunDiscovery;

/**
 * Generic AS7 plugin tests.
 *
 * @author Ian Springer
 */
@Test(groups = {"integration", "pc"}, singleThreaded = true)
public class GenericJBossAS7PluginTest extends AbstractJBossAS7PluginTest {

    // ******************************* METRICS ******************************* //
    @Test(priority = 1)
    @RunDiscovery
    public void testAllMetricsHaveNonNullValues() throws Exception {
        assertAllNumericMetricsAndTraitsHaveNonNullValues();
    }

}
