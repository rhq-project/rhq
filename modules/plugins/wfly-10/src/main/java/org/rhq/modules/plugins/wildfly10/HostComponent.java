/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.modules.plugins.wildfly10;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.modules.plugins.wildfly10.json.Address;
import org.rhq.modules.plugins.wildfly10.json.Operation;
import org.rhq.modules.plugins.wildfly10.json.ReadAttribute;
import org.rhq.modules.plugins.wildfly10.json.Result;

/**
 * Component class for a host in a domain
 * @author Heiko W. Rupp
 */
public class HostComponent extends BaseComponent<HostControllerComponent<?>> implements MeasurementFacet
{

   public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {

      Set<MeasurementScheduleRequest> leftovers = new HashSet<MeasurementScheduleRequest>(requests.size());

      for (MeasurementScheduleRequest request: requests) {
         if (request.getName().equals("startTime")) {
            String path = getPath();
            path = path.replace("server-config","server");
            Address address = new Address(path);
            address.add("core-service","platform-mbean");
            address.add("type","runtime");
            Operation op = new ReadAttribute(address,"start-time");
            Result res = getASConnection().execute(op);

            if (res.isSuccess()) {
               Long startTime= (Long) res.getResult();
               MeasurementDataTrait data = new MeasurementDataTrait(request,new Date(startTime).toString());
               report.addData(data);
            }
         }
         else {
            leftovers.add(request);
         }
      }

      if (!leftovers.isEmpty())
         super.getValues(report, leftovers);
   }
}
