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


/**
 * This class implements a status data object that contains the status information of a specific nagios server
 *
 * @author Alexander Kiefer
 */
public class StatusData implements NagiosData
{
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

	/**
	 * Method to find a specific metric for status
	 *
	 * @param ressourceName name of the status, not necessary here because there is only one status, can be null
	 * @param metricName name of the metric
	 * @return metric object
	 * @throws InvalidMetricRequestException if metric was not found
	 */
	public Metric getSingleMetricForRessource(String metricName, String ressourceName, String hostname) throws InvalidMetricRequestException
	{
		Metric metric = null;

		//check if metric is existent
		if(status.getMetricTable().containsKey(metricName))
		{
			metric = status.getMetricTable().get(metricName);
		}
		else
		{
			//if emtric is not exisitent
			throw new InvalidMetricRequestException(metricName);
		}

		return metric;
	}

	/**
	 *
	 * @return Hashtable with all the metric of the status
	 */
	public Hashtable<String, Metric> getAllMetrics()
	{
		Hashtable<String, Metric> metricTable = status.getMetricTable();

		return metricTable;
	}

	/**
	 * Method that creates status object and fills it with all the metric data
	 */
	public void fillWithData(String[] ressourceNames, String[] metricNames, String[] metricValues)
	{
		Hashtable<String , Metric> metricTable = new Hashtable<String, Metric>();

		//Create metric object for each metric an write information to it
		for(int metricCounter = 0; metricCounter < metricNames.length; metricCounter++)
		{
			Metric metric = new Metric(metricNames[metricCounter], metricValues[metricCounter]);
			metricTable.put(metric.getId(), metric);
		}
		//put table with all the metrics to status object
		status.setMetricTable(metricTable);
	}
}
