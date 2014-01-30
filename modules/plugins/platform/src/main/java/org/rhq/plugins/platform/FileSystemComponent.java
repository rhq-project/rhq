/*
  * RHQ Management Platform
  * Copyright (C) 2005-2011 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.plugins.platform;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.AvailabilityType;
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
import org.rhq.core.system.SystemInfo;

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
        FileSystemInfo fileSystemInfo = getFileSystemInfo();
        if (fileSystemInfo != null && fileSystemInfo.getFileSystem() != null
            && this.resourceContext.getResourceKey().equals(fileSystemInfo.getFileSystem().getDirName())) {
            return AvailabilityType.UP;
        } else {
            return AvailabilityType.DOWN;
        }
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {
        FileSystemInfo fileSystemInfo = getFileSystemInfo();
        for (MeasurementScheduleRequest request : requests) {
            try {
                switch (request.getDataType()) {
                    case TRAIT:
                        Object object = ObjectUtil.lookupDeepAttributeProperty(fileSystemInfo, request.getName());
                        report.addData(new MeasurementDataTrait(request, String.valueOf(object)));
                        break;
                    case MEASUREMENT:
                        Double value = ObjectUtil.lookupDeepNumericAttributeProperty(fileSystemInfo, request.getName());
                        report.addData(new MeasurementDataNumeric(request, value));
                        break;
                    default:
                        throw new IllegalStateException("Unsupported metric type: " + request.getDataType());
                }
            } catch (Exception e) {
                LOG.info("Unable to collect metric [" + request.getName() + "] on filesystem resource ["
                    + this.resourceContext.getResourceKey() + "].", e);
            }
        }
    }

    private FileSystemInfo getFileSystemInfo() {
        SystemInfo systemInfo = resourceContext.getSystemInformation();
        return systemInfo.getFileSystem(resourceContext.getResourceKey());
    }
}
