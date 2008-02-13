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
package org.rhq.enterprise.gui.legacy.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.config.MessageResourcesConfig;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.MessageResourcesFactory;
import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.Test;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplayConstants;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplaySummary;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplayValue;

/**
 * Test to make sure that refactoring to the MeasurementUnits, and ConvertUnits class does not break the current
 * MonitorUntils.formatMetrics() functionality
 *
 * @author Jessica Sant
 */
@Test
public class MonitorUtilsTest {
    private static Log log = LogFactory.getLog(MonitorUtils.class.getName());

    /**
     * test various byte-based metrics
     */
    @Test
    public void testFormatByteMetrics() {
        List<MetricDisplaySummary> metricDisplaySummaries = new ArrayList<MetricDisplaySummary>();
        Locale locale = Locale.US;
        MessageResources messageResources = createMessageResources();

        // test bytes
        MetricDisplaySummary t = createFakeMetric(1234567890.0987654321, MeasurementUnits.BYTES);
        MetricDisplaySummary u = createFakeMetric(1023, MeasurementUnits.BYTES);
        MetricDisplaySummary v = createFakeMetric(65536, MeasurementUnits.BYTES);
        MetricDisplaySummary w = createFakeMetric(0.1845, MeasurementUnits.MEGABYTES);

        metricDisplaySummaries.add(t);
        metricDisplaySummaries.add(u);
        metricDisplaySummaries.add(v);
        metricDisplaySummaries.add(w);

        Integer resourceCount = MonitorUtils.formatMetrics(metricDisplaySummaries, locale, messageResources);

        assertEquals("1.1GB", t.getAvgMetric().getValueFmt().toString());
        assertEquals("1,023.0B", u.getAvgMetric().getValueFmt().toString());
        assertEquals("64.0KB", v.getAvgMetric().getValueFmt().toString());
        assertEquals("188.9KB", w.getAvgMetric().getValueFmt().toString());
    }

    /**
     * test various time-based values
     */
    @Test
    public void testFormatTimeMetrics() {
        List<MetricDisplaySummary> metricDisplaySummaries = new ArrayList<MetricDisplaySummary>();
        Locale locale = Locale.US;
        MessageResources messageResources = createMessageResources();

        // test time
        MetricDisplaySummary i = createFakeMetric(18515.52, MeasurementUnits.SECONDS);
        MetricDisplaySummary j = createFakeMetric(1234567890, MeasurementUnits.MILLISECONDS);
        MetricDisplaySummary k = createFakeMetric(80280000, MeasurementUnits.MILLISECONDS);
        MetricDisplaySummary l = createFakeMetric(23333331.99, MeasurementUnits.SECONDS);

        metricDisplaySummaries.add(i);
        metricDisplaySummaries.add(j);
        metricDisplaySummaries.add(k);
        metricDisplaySummaries.add(l);

        Integer resourceCount = MonitorUtils.formatMetrics(metricDisplaySummaries, locale, messageResources);

        assertEquals("5.1h", i.getAvgMetric().getValueFmt().toString());
        assertEquals("14.3d", j.getAvgMetric().getValueFmt().toString());
        assertEquals("22.3h", k.getAvgMetric().getValueFmt().toString());
        assertEquals("270.1d", l.getAvgMetric().getValueFmt().toString());
    }

    /**
     * test various bit-based values
     */
    @Test
    public void testFormatBitMetrics() {
        List<MetricDisplaySummary> metricDisplaySummaries = new ArrayList<MetricDisplaySummary>();
        Locale locale = Locale.US;
        MessageResources messageResources = createMessageResources();

        // test bits
        MetricDisplaySummary a = createFakeMetric(1234567890.0987654321, MeasurementUnits.BITS);
        MetricDisplaySummary b = createFakeMetric(1023, MeasurementUnits.BITS);
        MetricDisplaySummary c = createFakeMetric(65536, MeasurementUnits.BITS);
        MetricDisplaySummary d = createFakeMetric(0.1845, MeasurementUnits.MEGABITS);

        metricDisplaySummaries.add(a);
        metricDisplaySummaries.add(b);
        metricDisplaySummaries.add(c);
        metricDisplaySummaries.add(d);

        Integer resourceCount = MonitorUtils.formatMetrics(metricDisplaySummaries, locale, messageResources);

        assertEquals("1.1Gb", a.getAvgMetric().getValueFmt().toString());
        assertEquals("1,023.0b", b.getAvgMetric().getValueFmt().toString());
        assertEquals("64.0Kb", c.getAvgMetric().getValueFmt().toString());
        assertEquals("188.9Kb", d.getAvgMetric().getValueFmt().toString());
    }

    /**
     * creates the MessageResources object needed for the metric formatting
     *
     * @return
     */
    private MessageResources createMessageResources() {
        MessageResources messageResources;
        MessageResourcesConfig messageResConfig = new MessageResourcesConfig();
        String factory = messageResConfig.getFactory();
        MessageResourcesFactory.setFactoryClass(factory);
        MessageResourcesFactory factoryObject = MessageResourcesFactory.createFactory();
        messageResources = factoryObject.createResources("ApplicationResources.properties");
        return messageResources;
    }

    /**
     * defines a fake metric to be formatted
     *
     * @param  value the numeric value for the metric
     * @param  units the units of this metric (MB, B, s, etc)
     *
     * @return a display object represeinting this metric
     */
    private MetricDisplaySummary createFakeMetric(double value, MeasurementUnits units) {
        MetricDisplaySummary result = new MetricDisplaySummary();
        result.setMetric(MetricDisplayConstants.AVERAGE_KEY, new MetricDisplayValue(value));
        result.setAvailUp(2);
        result.setUnits(units.name());
        return result;
    }
}