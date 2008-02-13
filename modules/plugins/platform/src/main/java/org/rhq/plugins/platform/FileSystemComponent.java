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
package org.rhq.plugins.platform;

import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.util.ObjectUtil;
import org.rhq.core.system.FileSystemInfo;

/**
 * @author Greg Hinkle
 */
public class FileSystemComponent implements ResourceComponent<PlatformComponent>, MeasurementFacet {
    private static final Log LOG = LogFactory.getLog(FileSystemComponent.class);
    private ResourceContext<PlatformComponent> resourceContext;

    public void start(ResourceContext<PlatformComponent> resourceContext) throws InvalidPluginConfigurationException,
        Exception {
        this.resourceContext = resourceContext;
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        if (getFileSystemInfo() != null) {
            return AvailabilityType.UP;
        } else {
            return AvailabilityType.DOWN;
        }
    }

    private FileSystemInfo getFileSystemInfo() {
        return resourceContext.getSystemInformation().getFileSystem(resourceContext.getResourceKey());
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        FileSystemInfo fileSystemInfo = getFileSystemInfo();
        fileSystemInfo.refresh();
        for (MeasurementScheduleRequest request : metrics) {
            try {
                if (request.getDataType() == DataType.TRAIT) {
                    Object value = ObjectUtil.lookupDeepAttributeProperty(fileSystemInfo, request.getName());
                    report.addData(new MeasurementDataTrait(request, String.valueOf(value)));
                } else if (request.getDataType() == DataType.MEASUREMENT) {
                    report.addData(new MeasurementDataNumeric(request, ObjectUtil.lookupDeepNumericAttributeProperty(
                        fileSystemInfo, request.getName())));
                }
            } catch (Exception e) {
                LOG.info("Unable to collection file system metric [" + request.getName() + "] on resource "
                    + this.resourceContext.getResourceKey(), e);
            }
        }
    }
}