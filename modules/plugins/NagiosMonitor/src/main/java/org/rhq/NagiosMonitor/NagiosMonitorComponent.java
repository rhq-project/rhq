package org.rhq.NagiosMonitor;
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

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.NagiosMonitor.controller.Controller;
import org.rhq.NagiosMonitor.controller.NagiosManagementInterface;
import org.rhq.NagiosMonitor.data.NagiosData;
import org.rhq.NagiosMonitor.data.NagiosSystemData;
import org.rhq.NagiosMonitor.error.InvalidHostRequestException;
import org.rhq.NagiosMonitor.error.InvalidMetricRequestException;
import org.rhq.NagiosMonitor.error.InvalidReplyTypeException;
import org.rhq.NagiosMonitor.error.InvalidServiceRequestException;
import org.rhq.NagiosMonitor.network.NetworkConnection;
import org.rhq.NagiosMonitor.reply.LqlReply;
import org.rhq.NagiosMonitor.request.LqlServiceRequest;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 * Plugin Component Class
 * To make it work you have to change the ip adress and port for your purpose
 * The metric-, service- and hostnames depend on your nagios system, please make 
 * it sure that they exist and change the plugin descriptor too
 * 
 * @author Alexander Kiefer
 */
public class NagiosMonitorComponent implements ResourceComponent, MeasurementFacet
{
	private final Log log = LogFactory.getLog(this.getClass());
    
    private final String NAGIOSIP = "127.0.0.1";
    private final int NAGIOSPORT = 6557;
        
    private NagiosManagementInterface nagiosManagementInterface;
    /**
     * Return availability of this resource
     *  @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() 
    {
        // TODO supply real implementation
        return AvailabilityType.UP;
    }

    /**
     * Start the resource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception 
    {
        @SuppressWarnings("unused")
		Configuration conf = context.getPluginConfiguration();	
        
        //Interface class to the nagios system 
        nagiosManagementInterface = new NagiosManagementInterface(NAGIOSIP, NAGIOSPORT);
        	
		log.info("NagiosMonitor Plugin started");
    }


    /**
     * Tear down the rescource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() 
    {
    			
    }

    /**
     * Gather measurement data
     *  @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport, java.util.Set)
     */
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics)
    {
    	NagiosSystemData nagiosSystemData = null;
	
    	try 
		{	
			nagiosSystemData = nagiosManagementInterface.createNagiosSystemData();
			
			
			log.info(nagiosSystemData.getSingleHostServiceMetric("execution_time", "Current Load", "localhost").getValue());
	    	log.info(nagiosSystemData.getSingleHostServiceMetric("host_execution_time", "Current Load", "localhost").getValue());
	    	log.info(nagiosSystemData.getSingleHostServiceMetric("host_check_period", "Current Load", "localhost").getValue());
	    	
			
			for (MeasurementScheduleRequest req : metrics) 
	         {    
				if("execution_time".equals(req.getName()) ) 
				{      
		    		String value = nagiosSystemData.getSingleHostServiceMetric("execution_time", "Current Load", "localhost").getValue();
					MeasurementDataNumeric res = new MeasurementDataNumeric(req, Double.valueOf(value));
	    			report.addData(res);
				}
				else if("host_check_period".equals(req.getName()) ) 
				{   
					String value = nagiosSystemData.getSingleHostServiceMetric("host_check_period", "Current Load", "localhost").getValue();
					MeasurementDataTrait res = new MeasurementDataTrait(req, value);
					report.addData(res);
				}
				else if("host_execution_time".equals(req.getName()) ) 
				{   
					String value = nagiosSystemData.getSingleHostServiceMetric("host_execution_time", "Current Load", "localhost").getValue();
					MeasurementDataNumeric res = new MeasurementDataNumeric(req, Double.valueOf(value));
					report.addData(res);					
				}
	     	}
			
		} 
		catch (InvalidReplyTypeException e) 
		{
			log.error(e);
		}
		catch (InvalidMetricRequestException e) 
		{
			log.error(e);
		} 
		catch (InvalidServiceRequestException e) 
		{
			log.error(e);
		} 
		catch (InvalidHostRequestException e) 
		{
			log.error(e);
		}
	}
}