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
public class StatusData implements NagiosData
{
	private String requestType;
	private Status status;
	
	public StatusData()
	{
		this.status = new Status();
	}
	
	public Status getStatus() 
	{
		return status;
	}
	
	public void setStatus(Status status) 
	{
		this.status= status;
	}
	
	public Metric getSingleMetricForRessource(String metricName, String ressourceName) throws InvalidMetricRequestException
	{
		Metric metric = null; 
			
		if(status.getMetricTable().contains(metricName))
		{
			metric = status.getMetricTable().get(metricName);
		}
		else
		{
			throw new InvalidMetricRequestException(metricName);
		}
			
		return metric;
	}
	
	public Hashtable<String, Metric> getAllMetrics()
	{
		Hashtable<String, Metric> metricTable = status.getMetricTable();
		
		return metricTable;
	}

	public void fillWithData(String[] ressourceNames, String[] metricNames, String[] metricValues)
	{
		Hashtable<String , Metric> metricTable = new Hashtable<String, Metric>();	
		
		for(int metricCounter = 0; metricCounter < metricNames.length; metricCounter++)
		{
			Metric metric = new Metric();
			metric.setId(metricNames[metricCounter]);
			metric.setValue(metricValues[metricCounter]);
				
			metricTable.put(metric.getId(), metric);
		}
		status.setMetricTable(metricTable);
	}

	public String getRequestType() 
	{
		return requestType;
	}

	public void setRequestType() 
	{
		requestType = NagiosRequestTypes.STATUS_REQUEST;
		
	}
}
