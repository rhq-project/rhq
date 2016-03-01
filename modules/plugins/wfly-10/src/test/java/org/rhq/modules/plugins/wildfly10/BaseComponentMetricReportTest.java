/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.modules.plugins.wildfly10;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;
import static org.rhq.core.domain.measurement.DataType.MEASUREMENT;
import static org.rhq.core.domain.measurement.DataType.TRAIT;
import static org.rhq.modules.plugins.wildfly10.BaseComponent.EXPRESSION;
import static org.rhq.modules.plugins.wildfly10.BaseComponent.EXPRESSION_VALUE_KEY;
import static org.rhq.modules.plugins.wildfly10.BaseComponent.INTERNAL;
import static org.rhq.modules.plugins.wildfly10.json.Result.SUCCESS;
import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.modules.plugins.wildfly10.json.Address;
import org.rhq.modules.plugins.wildfly10.json.ReadAttribute;
import org.rhq.modules.plugins.wildfly10.json.ResolveExpression;
import org.rhq.modules.plugins.wildfly10.json.Result;

/**
 * @author Thomas Segismont
 */
public class BaseComponentMetricReportTest {

    @Mock
    private ASConnection asConnection;

    private Address address;

    private MeasurementReport report;

    private Set<MeasurementScheduleRequest> requests;

    private SampleComponent sampleComponent;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        report = new MeasurementReport();
        requests = new HashSet<MeasurementScheduleRequest>();
        address = new Address("path");
        sampleComponent = new SampleComponent();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {

    }

    @Test
    public void testInternalMetric() throws Exception {
        int mgmtRequestsCount = 13;
        for (int i = 0; i < mgmtRequestsCount; i++) {
            PluginStats.getInstance().incrementRequestCount();
        }
        String requestName = INTERNAL + "mgmtRequests";
        requests.add(newMeasurementRequest(requestName));

        sampleComponent.getValues(report, requests);

        Set<MeasurementDataNumeric> numericMetrics = report.getNumericData();
        assertEquals(numericMetrics.size(), 1);
        MeasurementDataNumeric numericMetric = numericMetrics.iterator().next();
        assertEquals(numericMetric.getName(), requestName);
        assertEquals(numericMetric.getValue(), (double) mgmtRequestsCount);
    }

    @Test
    public void testSimpleMetric() throws Exception {
        String requestName = "pipo";
        requests.add(newMeasurementRequest(requestName));
        double metricValue = 13d;

        when(asConnection.execute(readAttribute(address, requestName))).thenReturn(result(metricValue));
        sampleComponent.getValues(report, requests);

        Set<MeasurementDataNumeric> numericMetrics = report.getNumericData();
        assertEquals(numericMetrics.size(), 1);
        MeasurementDataNumeric numericMetric = numericMetrics.iterator().next();
        assertEquals(numericMetric.getName(), requestName);
        assertEquals(numericMetric.getValue(), metricValue);
    }

    @Test
    public void testSimpleMetricWithExpression() throws Exception {
        String attributeName = "pipo";
        String requestName = EXPRESSION + attributeName;
        requests.add(newMeasurementRequest(requestName));
        double metricValue = 13d;
        String expressionValue = "${prop:14}";

        when(asConnection.execute(readAttribute(address, attributeName))).thenReturn(
            result(expression(expressionValue)));
        when(asConnection.execute(resolveExpression(expressionValue))).thenReturn(result(metricValue));
        sampleComponent.getValues(report, requests);

        Set<MeasurementDataNumeric> numericMetrics = report.getNumericData();
        assertEquals(numericMetrics.size(), 1);
        MeasurementDataNumeric numericMetric = numericMetrics.iterator().next();
        assertEquals(numericMetric.getName(), requestName);
        assertEquals(numericMetric.getValue(), metricValue);
    }

    @Test
    public void testSimpleTrait() throws Exception {
        String requestName = "pipo";
        requests.add(newTraitRequest(requestName));
        String traitValue = "marseille";

        when(asConnection.execute(readAttribute(address, requestName))).thenReturn(result(traitValue));
        sampleComponent.getValues(report, requests);

        Set<MeasurementDataTrait> traits = report.getTraitData();
        assertEquals(traits.size(), 1);
        MeasurementDataTrait trait = traits.iterator().next();
        assertEquals(trait.getName(), requestName);
        assertEquals(trait.getValue(), traitValue);
    }

    @Test
    public void testSimpleTraitWithExpression() throws Exception {
        String attributeName = "pipo";
        String requestName = EXPRESSION + attributeName;
        requests.add(newTraitRequest(requestName));
        String traitValue = "marseille";
        String expressionValue = "${prop:paris}";

        when(asConnection.execute(readAttribute(address, attributeName))).thenReturn(
            result(expression(expressionValue)));
        when(asConnection.execute(resolveExpression(expressionValue))).thenReturn(result(traitValue));
        sampleComponent.getValues(report, requests);

        Set<MeasurementDataTrait> traits = report.getTraitData();
        assertEquals(traits.size(), 1);
        MeasurementDataTrait trait = traits.iterator().next();
        assertEquals(trait.getName(), requestName);
        assertEquals(trait.getValue(), traitValue);
    }

    private class SampleComponent extends BaseComponent {

        private SampleComponent() {
        }

        @Override
        public ASConnection getASConnection() {
            return asConnection;
        }
    }

    private static MeasurementScheduleRequest newMeasurementRequest(String requestName) {
        return new MeasurementScheduleRequest(-1, requestName, -1, true, MEASUREMENT);
    }

    private static MeasurementScheduleRequest newTraitRequest(String requestName) {
        return new MeasurementScheduleRequest(-1, requestName, -1, true, TRAIT);
    }

    private static Result result(Object value) {
        Result result = new Result();
        result.setOutcome(SUCCESS);
        result.setResult(value);
        return result;
    }

    private static ReadAttribute readAttribute(Address address, String requestName) {
        return argThat(new IsReadAttribute(address, requestName));
    }

    private static Object expression(String expression) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(EXPRESSION_VALUE_KEY, expression);
        return map;
    }

    private static ResolveExpression resolveExpression(String expressionValue) {
        return argThat(new IsResolveExpression(expressionValue));
    }

    private static class IsReadAttribute extends ArgumentMatcher<ReadAttribute> {

        private Address address;
        private String attributeName;

        private IsReadAttribute(Address address, String attributeName) {
            this.address = address;
            this.attributeName = attributeName;
        }

        @Override
        public boolean matches(Object argument) {
            if (!(argument instanceof ReadAttribute)) {
                return false;
            }
            ReadAttribute readAttribute = (ReadAttribute) argument;
            return address.getPath().equals(readAttribute.getAddress().getPath())
                && attributeName.equals(readAttribute.getName());
        }
    }

    private static class IsResolveExpression extends ArgumentMatcher<ResolveExpression> {

        private String expressionValue;

        private IsResolveExpression(String expressionValue) {
            this.expressionValue = expressionValue;
        }

        @Override
        public boolean matches(Object argument) {
            if (!(argument instanceof ResolveExpression)) {
                return false;
            }
            ResolveExpression resolveExpression = (ResolveExpression) argument;
            return expressionValue.equals(resolveExpression.getAdditionalProperties().get("expression"));
        }
    }
}
