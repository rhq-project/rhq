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

package org.rhq.modules.plugins.jbossas7;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * @author Thomas Segismont
 */
public class MemoryPoolComponent extends BaseComponent<BaseComponent<?>> {

    private static final String USAGE_THRESHOLD_PREFIX = "usage-threshold-";
    private static final String USAGE_THRESHOLD_SUPPORTED_ATTRIBUTE = USAGE_THRESHOLD_PREFIX + "supported";
    private static final String COLLECTION_USAGE_THRESHOLD_PREFIX = "collection-" + USAGE_THRESHOLD_PREFIX;
    private static final String COLLECTION_USAGE_THRESHOLD_SUPPORTED_ATTRIBUTE = COLLECTION_USAGE_THRESHOLD_PREFIX
        + "supported";

    private Boolean usageThresholdSupported;
    private Boolean collectionUsageThresholdSupported;

    @Override
    public void start(ResourceContext<BaseComponent<?>> context) throws Exception {
        super.start(context);
        usageThresholdSupported = readAttribute(getAddress(), USAGE_THRESHOLD_SUPPORTED_ATTRIBUTE, Boolean.class);
        collectionUsageThresholdSupported = readAttribute(getAddress(), COLLECTION_USAGE_THRESHOLD_SUPPORTED_ATTRIBUTE,
            Boolean.class);
    }

    @Override
    public void stop() {
        super.stop();
        usageThresholdSupported = null;
        collectionUsageThresholdSupported = null;
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        Set<MeasurementScheduleRequest> filteredMetrics = new HashSet<MeasurementScheduleRequest>();
        for (MeasurementScheduleRequest request : metrics) {
            String requestName = request.getName();
            if (USAGE_THRESHOLD_SUPPORTED_ATTRIBUTE.equals(requestName)) {
                report.addData(new MeasurementDataTrait(request, usageThresholdSupported.toString()));
                continue;
            }
            if (requestName.startsWith(USAGE_THRESHOLD_PREFIX)) {
                if (usageThresholdSupported) {
                    filteredMetrics.add(request);
                }
                continue;
            }
            if (COLLECTION_USAGE_THRESHOLD_SUPPORTED_ATTRIBUTE.equals(requestName)) {
                report.addData(new MeasurementDataTrait(request, collectionUsageThresholdSupported.toString()));
                continue;
            }
            if (requestName.startsWith(COLLECTION_USAGE_THRESHOLD_PREFIX)) {
                if (collectionUsageThresholdSupported) {
                    filteredMetrics.add(request);
                }
                continue;
            }
            filteredMetrics.add(request);
        }
        super.getValues(report, filteredMetrics);
    }
}
