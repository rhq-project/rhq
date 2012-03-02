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
package org.rhq.modules.plugins.jbossas7;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;

/**
 * Component class for Infinispan caches
 * @author Heiko W. Rupp
 */
public class IspnCacheComponent extends BaseComponent implements ConfigurationFacet {


    @Override
    public void getValues(MeasurementReport report, Set metrics) throws Exception {

        Set<MeasurementScheduleRequest> requests = metrics;
        Set<MeasurementScheduleRequest> todo = new HashSet<MeasurementScheduleRequest>();

        for (MeasurementScheduleRequest req: requests) {
            if (req.getName().equals("__flavor")) {
                String flavor = getCacheFlavorFromPath();
                MeasurementDataTrait trait = new MeasurementDataTrait(req,flavor);
                report.addData(trait);
            }
            else
                todo.add(req);
        }


        super.getValues(report, todo);
    }

    private String getCacheFlavorFromPath() {
        String flavor = path.substring(path.lastIndexOf(",")+1);
        flavor = flavor.substring(0,flavor.indexOf("="));
        return flavor;
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        Configuration config = super.loadResourceConfiguration();
        String f = getCacheFlavorFromPath();
        PropertySimple flavor = new PropertySimple(IspnCContainerComponent.FLAVOR,f);
        config.put(flavor);
        return config;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {

        report.getConfiguration().remove(IspnCContainerComponent.FLAVOR);

        super.updateResourceConfiguration(report);
    }
}
