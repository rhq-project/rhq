package org.rhq.plugins.nagios.data;
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

import org.rhq.plugins.nagios.error.InvalidMetricRequestException;
import org.rhq.plugins.nagios.error.InvalidServiceRequestException;

/**
 * This class a service data object that contains the information about all the services of a specific host
 *
 * @author Alexander Kiefer
 */
public class ServiceData implements NagiosData
{
	/**
	 * Table of all the services
	 */
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

	/**
	 * Method to find a specific metric for a specific service
	 *
	 * @param ressourceName name of the service
	 * @param metricName name of the metric
	 * @return metric object
	 * @throws InvalidServiceRequestException if service was not found
	 * @throws InvalidMetricRequestException if metric was not found
	 */
	public Metric getSingleMetricForRessource(String metricName, String ressourceName, String hostname) throws InvalidServiceRequestException, InvalidMetricRequestException
	{
		Metric metric = null;

		//Check if service is existing
		if(serviceTable.containsKey(ressourceName))
		{
			//check if metric for the service is existing
			if(serviceTable.get(ressourceName).getMetricTable().containsKey(metricName))
			{
				//fill metric instance with data
				metric = serviceTable.get(ressourceName).getMetricTable().get(metricName);
			}
			else
			{
				//if metric is not existing
				throw new InvalidMetricRequestException(metricName);
			}
		}
		else
		{
			//if service is not existing
			throw new InvalidServiceRequestException(ressourceName);
		}

		return metric;
	}

	/**
	 * Method to get all the metrics for a specific service
	 *
	 * @param ressourceName Name of the service
	 * @return Hashtable with all metric of specific service
	 * @throws InvalidServiceRequestException if service was not found
	 */
	public Hashtable<String, Metric> getAllMetricsForRessource(String ressourceName) throws InvalidServiceRequestException
	{
		Hashtable<String, Metric> metricTable = null;

		//check if service is existing
		if(serviceTable.containsKey(ressourceName))
		{
			//fill object
			metricTable = serviceTable.get(ressourceName).getMetricTable();
		}
		else
		{
			//if service is not existing
			throw new InvalidServiceRequestException(ressourceName);
		}

		return metricTable;
	}

	/**
	 * Method creates instances for all the services running and fills them with data
	 */
	public void fillWithData(String[] ressourceNames, String[] metricNames, String[] metricValues)
	{
		Hashtable<String , Metric> metricTable = new Hashtable<String, Metric>();
		//Always only one name contained in ressource names
		Service service = new Service(ressourceNames[0]);

		//create metric objects for all the metrics of a service
		for(int metricCounter = 0; metricCounter < metricNames.length; metricCounter++)
		{
			//Name and value of a metric are at the same position within the arrays and array length is equal
			Metric metric = new Metric(metricNames[metricCounter], metricValues[metricCounter]);
			//put metric in table of the service it belongs to
			metricTable.put(metric.getId(), metric);
		}
		//put table to service object
		service.setMetricTable(metricTable);
		//put service object to service table
		serviceTable.put(service.getId(), service);
	}
}

