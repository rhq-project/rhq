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
package org.rhq.plugins.jbossas5;

import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

import org.jboss.deployers.spi.management.ManagementView;

/**
 * @author Ian Springer
 */
public class WarMeasurementFacetDelegate implements MeasurementFacet
{
    private static final String CONTEXT_ROOT_TRAIT = "contextRoot";
    private static final String VIRTUAL_HOSTS_TRAIT = "virtualHosts";

    private final Log log = LogFactory.getLog(this.getClass());

    private ResourceContext<ProfileServiceComponent> resourceContext;

    public WarMeasurementFacetDelegate(ResourceContext<ProfileServiceComponent> resourceContext)
    {
        this.resourceContext = resourceContext;
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests)
            throws Exception
    {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String contextPath = pluginConfig.getSimple(AbstractWarDiscoveryComponent.CONTEXT_PATH_PROPERTY).getStringValue();
        for (MeasurementScheduleRequest request : requests) {
            String metricName = request.getName();
            try
            {
                if (metricName.equals(CONTEXT_ROOT_TRAIT)) {
                    String contextRoot = (contextPath.equals("/")) ? "/" : contextPath.substring(1);
                    MeasurementDataTrait trait = new MeasurementDataTrait(request, contextRoot);
                    report.addData(trait);
                } else if (metricName.equals(VIRTUAL_HOSTS_TRAIT)) {
                    Set<String> virtualHosts = WebApplicationContextDiscoveryComponent.getVirtualHosts(contextPath,
                            getManagementView());
                    String value = "";
                    for (Iterator<String> iterator = virtualHosts.iterator(); iterator.hasNext();)
                    {
                        String virtualHost = iterator.next();
                        value += virtualHost;
                        if (iterator.hasNext())
                            value += ", ";
                    }
                    MeasurementDataTrait trait = new MeasurementDataTrait(request, value);
                    report.addData(trait);
                }
            }
            catch (Exception e)
            {
                // Don't let one bad apple spoil the barrel.
                log.error("Failed to collect metric '" + metricName + "' for " + this.resourceContext.getResourceType()
                        + " Resource with key " + this.resourceContext.getResourceKey() + ".", e);
            }
        }
    }

    private ManagementView getManagementView()
    {
        ProfileServiceComponent jbasComponent = this.resourceContext.getParentResourceComponent();
        return jbasComponent.getConnection().getManagementView();
    }
}
