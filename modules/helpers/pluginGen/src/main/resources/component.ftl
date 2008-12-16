<#--
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
-->

package ${props.packagePrefix};

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.domain.measurement.AvailabilityType;
<#if props.monitoring>
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
</#if>

import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
<#if props.monitoring>
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
</#if>
<#if props.events>
import org.rhq.core.pluginapi.event.EventContext;
</#if>
<#if props.operations>
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
</#if>


public class ${props.componentClass} implements ResourceComponent<#if props.parentType??><${props.parentType}></#if>
<#if props.monitoring>
, MeasurementFacet
</#if>
<#if props.operations>
, OperationFacet
</#if>
<#if props.resourceConfiguration>
, ConfigurationFacet
</#if>
{
    private final Log log = LogFactory.getLog(this.getClass());

    private static final int CHANGEME = 1; // TODO remove or change this


    <#if props.events>
    public static final String DUMMY_EVENT = "dummyEvent"; // Same as in Plugin-Descriptor

    EventContext eventContext;
    </#if>


    /**
     * Return availability of this resource
     *  @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }


    /**
     * Start the resource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext<#if props.parentType??><${props.parentType}></#if> context) throws InvalidPluginConfigurationException, Exception {

        Configuration conf = context.getPluginConfiguration();
        // TODO add code to start the resource / connection to it

        <#if props.events>
        eventContext = context.getEventContext();
        DummyEventPoller eventPoller = new DummyEventPoller();
        eventContext.registerEventPoller(eventPoller, 60);
        </#if>

    }


    /**
     * Tear down the rescource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() {


        <#if props.events>
        eventContext.unregisterEventPoller(DUMMY_EVENT);
        </#if>
    }


<#if props.monitoring>

    /**
     * Gather measurement data
     *  @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport, java.util.Set)
     */
    public  void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

         for (MeasurementScheduleRequest req : metrics) {
            if (req.getName().equals("dummyMetric")) {
                 MeasurementDataNumeric res = new MeasurementDataNumeric(req, Double.valueOf(CHANGEME));
                 report.addData(res);
            }
            // TODO add more metrics here
         }
    }
</#if>

<#if props.operations>

    public void startOperationFacet(OperationContext context) {

    }


    public OperationResult invokeOperation(String name, Configuration params) throws Exception {

        OperationResult res = new OperationResult();
        if ("dummyOperation".equals(name)) {
            // TODO implement me

        }
        return res;
    }
</#if>


<#if props.resourceConfiguration>
    public Configuration loadResourceConfiguration()
    {
        // TODO supply code to load the configuration from the resource into the plugin
        return null;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report)
    {
        // TODO supply code to update the passed report into the resource
    }
</#if>
}