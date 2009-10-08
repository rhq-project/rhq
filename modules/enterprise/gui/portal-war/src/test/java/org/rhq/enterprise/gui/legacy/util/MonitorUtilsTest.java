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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.struts.config.MessageResourcesConfig;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.MessageResourcesFactory;
import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplayConstants;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplaySummary;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplayValue;

/**
 * Test to make sure that refactoring to the MeasurementUnits, and ConvertUnits class does not break the current
 * MonitorUntils.formatMetrics() functionality.
 *
 * Due to different international spelling of numeric units and a bug in MonitorUntils.formatMetrics
 * (http://jira.rhq-project.org/browse/RHQ-1129) we need to compare the output with a localized
 * version of the reference data.
 *
 * @author Jessica Sant
 * @author Heiko W. Rupp
 */
@Test
public class MonitorUtilsTest {

    NumberFormat nf = NumberFormat.getInstance(); // Use Default locale

    @BeforeTest
    public void beforeTest() {
        System.out.println("===> Locale is " + Locale.getDefault() + " <========");
    }

    /**
     * test various byte-based metrics
     */
    @Test
    public void testFormatByteMetrics() {
        List<MetricDisplaySummary> metricDisplaySummaries = new ArrayList<MetricDisplaySummary>();
        Locale locale = Locale.US; // This does not work as intended. See RHQ-1129
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


        assertEquals(nf.format(1.1)+"GB", t.getAvgMetric().getValueFmt());
        nf.setMinimumFractionDigits(1); // Otherwise NumberFormat swallows the fraction digit
        assertEquals(nf.format(1023.0)+"B", u.getAvgMetric().getValueFmt());
        assertEquals(nf.format(64.0)+"KB", v.getAvgMetric().getValueFmt());
        assertEquals(nf.format(188.9)+"KB", w.getAvgMetric().getValueFmt());
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

        assertEquals(nf.format(5.1)+"h", i.getAvgMetric().getValueFmt());
        assertEquals(nf.format(14.3)+"d", j.getAvgMetric().getValueFmt());
        assertEquals(nf.format(22.3)+"h", k.getAvgMetric().getValueFmt());
        assertEquals(nf.format(270.1)+"d", l.getAvgMetric().getValueFmt());
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

        assertEquals(nf.format(1.1)+"Gb", a.getAvgMetric().getValueFmt());
        nf.setMinimumFractionDigits(1); // Otherwise NumberFormat swallows the fraction digit
        assertEquals(nf.format(1023)+"b", b.getAvgMetric().getValueFmt());
        assertEquals(nf.format(64)+"Kb", c.getAvgMetric().getValueFmt());
        assertEquals(nf.format(188.9)+"Kb", d.getAvgMetric().getValueFmt());
    }

    /**
     * creates the MessageResources object needed for the metric formatting
     *
     * @return a new MessageResources object for testing
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