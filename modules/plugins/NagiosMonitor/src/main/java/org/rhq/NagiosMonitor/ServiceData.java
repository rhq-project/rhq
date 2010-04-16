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

import java.util.Hashtable;
/**
 * 
 * 
 * @author Alexander Kiefer
 */
public class ServiceData implements NagiosData  
{
	private String requestType;
	private Hashtable<String, Service> serviceTable;
	
	public ServiceData()
	{
		serviceTable = new Hashtable<String, Service>();
	}
	
	public Hashtable<String, Service> getServiceTable() 
	{
		return serviceTable;
	}
	
	public void setServiceTable(Hashtable<String, Service> serviceTable) 
	{
		this.serviceTable = serviceTable;
	}
	
	public Metric getSingleMetricForRessource(String metricName, String ressourceName) throws InvalidServiceRequestException, InvalidMetricRequestException
	{
		Metric metric = null;
		
		if(serviceTable.containsKey(ressourceName))
		{
			if(serviceTable.get(ressourceName).getMetricTable().containsKey(metricName))
			{
				metric = serviceTable.get(ressourceName).getMetricTable().get(metricName);
			}
			else
			{
				throw new InvalidMetricRequestException(metricName);
			}
		}
		else
		{
			throw new InvalidServiceRequestException(ressourceName);
		}
		
		return metric;
	}
	
	public Hashtable<String, Metric> getAllMetricsForRessource(String ressourceName) throws InvalidServiceRequestException
	{
		Hashtable<String, Metric> metricTable = null;
		
		if(serviceTable.containsKey(ressourceName))
		{
			metricTable = serviceTable.get(ressourceName).getMetricTable();
		}
		else
		{
			throw new InvalidServiceRequestException(ressourceName);
		}
			
		return metricTable;
	}

	public void fillWithData(String[] ressourceNames, String[] metricNames, String[] metricValues)
	{
		Hashtable<String , Metric> metricTable = new Hashtable<String, Metric>();	
		Service service = new Service(ressourceNames[0]);

		for(int metricCounter = 0; metricCounter < metricNames.length; metricCounter++)
		{
			Metric metric = new Metric();
			metric.setId(metricNames[metricCounter]);
			metric.setValue(metricValues[metricCounter]);
			
			metricTable.put(metric.getId(), metric);
		}
		
		service.setMetricTable(metricTable);
		serviceTable.put(service.getId(), service);
	}

	public String getRequestType() 
	{
		return requestType;
	}

	public void setRequestType() 
	{
		requestType = NagiosRequestTypes.SERVICE_REQUEST;
	}
}			

