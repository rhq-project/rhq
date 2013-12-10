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

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.availability.AvailabilityFacet;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.rhq.modules.plugins.jbossas7.modcluster.ProxyInfo;

/**
 * Component class for ModCluster Webapp Context 
 * @author Simeon Pinder
 */
public class ModClusterContextComponent extends ModClusterComponent implements AvailabilityFacet {

    @Override
    public AvailabilityType getAvailability() {

        String rawProxyInfo = getRawProxyInfo();

        ProxyInfo proxyInfo = new ProxyInfo(rawProxyInfo);

        ProxyInfo.Context context = null;
        try {
            //remove {modcluster adddress}: from key.
            int indexOfSeparator = this.context.getResourceKey().indexOf(":");
            context = ProxyInfo.Context.fromString(this.context.getResourceKey().substring(indexOfSeparator + 1));
        } catch (Exception e) {
            getLog().warn("Invalid resourcekey is being used for modcluster component: " + e.getMessage());
            return AvailabilityType.DOWN;
        }

        int indexOfCurrentContext = proxyInfo.getAvailableContexts().indexOf(context);

        if (indexOfCurrentContext != -1) {
            ProxyInfo.Context currentContext = proxyInfo.getAvailableContexts().get(indexOfCurrentContext);

            if (currentContext.isEnabled()) {
                return AvailabilityType.UP;
            }
        }

        return AvailabilityType.DOWN;

    }

    private String getRawProxyInfo() {
        String rawProxyInfo = "";
        ModClusterContextComponent component = this;
        while ((component != null) && !(component instanceof ModClusterComponent)) {
            component = (ModClusterContextComponent) context.getParentResourceComponent();
        }
        //get root modcluster component
        String resourceKey = component.key;
        String[] resourceKeyComponents = resourceKey.split(":");

        Operation op = new Operation("read-proxies-info", new Address(resourceKeyComponents[0]));
        Result result = getASConnection().execute(op);
        //get ProxyInfo and parse
        rawProxyInfo = ModClusterContextDiscoveryComponent.extractRawProxyInfo(result);

        return rawProxyInfo;
    }

    void addAdditionalToOp(Operation op, Configuration parameters, String parameterName, boolean optional) {
        String value = parameters.getSimpleValue(parameterName, null);
        if (value == null) {
            if (!optional) {
                throw new IllegalArgumentException("Required parameter [" + parameterName + "] for operation ["
                    + op.getName() + "] is not defined.");
            }
        } else {
            op.addAdditionalProperty(parameterName, value);
        }
    }

    @Override
    public Address getAddress() {
        int indexOfSeparator = this.context.getResourceKey().indexOf(":");
        return new Address(this.context.getResourceKey().substring(0, indexOfSeparator));
    }

}
