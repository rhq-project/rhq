/*
  * RHQ Management Platform
  * Copyright (C) 2005-2014 Red Hat, Inc.
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
import org.hyperic.sigar.DirUsage;

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
 * @author Heiko W. Rupp
*/
public class DirectoryComponent implements ResourceComponent<PlatformComponent>, MeasurementFacet {

   private static final Log LOG = LogFactory.getLog(DirectoryComponent.class);

   private ResourceContext<PlatformComponent> resourceContext;

   public void start(ResourceContext<PlatformComponent> resourceContext) throws InvalidPluginConfigurationException,
       Exception {
       this.resourceContext = resourceContext;
   }

   public void stop() {
   }

   public AvailabilityType getAvailability() {
       DirUsage dirUsage = getDirectoryInfo();
       if (dirUsage != null ) {
           return AvailabilityType.UP;
       } else {
           return AvailabilityType.DOWN;
       }
   }

   public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {
       DirUsage dirUsage = getDirectoryInfo();
       if (dirUsage==null)
           return;

       for (MeasurementScheduleRequest request : requests) {
           Double value = null;
           if (request.getName().equals("usage")) {
               value = Double.valueOf(dirUsage.getDiskUsage());
           } else if (request.getName().equals("files")) {
               value = Double.valueOf(dirUsage.getFiles());
           } else if (request.getName().equals("total")) {
               value = Double.valueOf(dirUsage.getTotal());
           }

           if (value!=null) {
               MeasurementDataNumeric result = new MeasurementDataNumeric(request, value);
               report.addData(result);
           }
       }
   }

   private DirUsage getDirectoryInfo() {
       SystemInfo systemInfo = resourceContext.getSystemInformation();
       return systemInfo.getDirectoryUsage(resourceContext.getResourceKey());
   }
}
