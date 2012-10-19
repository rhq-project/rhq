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
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Component class for the WebConnector (subsystem=web,connector=* )
 * @author Heiko W. Rupp
 */
public class WebConnectorComponent extends BaseComponent<WebConnectorComponent> implements MeasurementFacet {

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        Set<MeasurementScheduleRequest> leftovers = new HashSet<MeasurementScheduleRequest>(metrics.size());
        for (MeasurementScheduleRequest request : metrics) {
            if (request.getName().equals("_maxConnections")) {
                ReadAttribute op = new ReadAttribute(getAddress(), "max-connections");
                Result res = getASConnection().execute(op);
                int val;
                if (res.isSuccess() && res.getResult() != null) {
                    Object o = res.getResult(); // If the attribute was written as string, it stays string until :reload
                    if (o instanceof String)
                        val = Integer.valueOf((String) o);
                    else
                        val = (Integer) o;
                } else { // this is not set in the server, we need to compute it ourselves
                    val = computeMaxConnections();
                }

                MeasurementDataNumeric data = new MeasurementDataNumeric(request, (double) val);
                report.addData(data);
            } else {
                leftovers.add(request);
            }
        }
        super.getValues(report, leftovers);
    }

    /**
     * Post-process the result from confguration reading to return the computed
     * value of max-connections if none is provided by the server.
     * @return The configuration of the web connector
     * @throws Exception If anything goes wrong
     */
    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        Configuration configuration = super.loadResourceConfiguration();
        PropertySimple maxConnProp = configuration.getSimple("max-connections");
        if (maxConnProp.getStringValue() == null) {
            int val = computeMaxConnections();
            maxConnProp.setIntegerValue(val);
        }
        return configuration;
    }

    /**
     * Compute the number for max connections by looking at the scheme of the
     * connector and the available cores.
     * Multipliers for the number of cores are
     * <ul>
     *     <li>512 for http/https </li>
     *     <li>32 for ajp</li>
     * </ul>
     * @return Number for max connections or -1 if we can't determine them.
     */
    private int computeMaxConnections() {
        int val = -1;

        ReadAttribute op;
        Result res;
        String scheme = null;
        op = new ReadAttribute(getAddress(), "scheme");
        res = getASConnection().execute(op);
        if (res.isSuccess()) {
            scheme = (String) res.getResult();
        }

        // Determine the address for the platform "mbean" via the grand parent
        Address address1 = new Address(getAddress());
        address1 = address1.getParent();
        address1 = address1.getParent();

        address1.add("core-service", "platform-mbean");
        address1.add("type", "operating-system");

        op = new ReadAttribute(address1, "available-processors");
        res = getASConnection().execute(op);
        int cores = 1;
        if (res.isSuccess() && res.getResult() != null) {
            cores = (Integer) res.getResult();
        }
        if ("http".equals(scheme))
            val = cores * 512;
        else if ("https".equals(scheme)) {
            val = cores * 512;
        } else if ("ajp".equals(scheme))
            val = 32 * cores;
        return val;
    }
}
