package org.rhq.plugins.nagios.data;

import java.util.Hashtable;

import org.rhq.plugins.nagios.error.InvalidHostRequestException;
import org.rhq.plugins.nagios.error.InvalidMetricRequestException;
import org.rhq.plugins.nagios.error.InvalidServiceRequestException;

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

/**
 * This class implements the upper data layer of a Nagios system
 * It contains the servers status information and all the monitored hosts
 *
 * @author Alexander Kiefer
 */
public class NagiosSystemData
{
	private HostData hostData;
	private StatusData statusData;

	/**
	 *
	 * @param hostData - the object which contains all the monitored hosts of the nagios system
	 * @param statusData - the object which contains all the status information of the nagios system
	 */
	public NagiosSystemData(HostData hostData, StatusData statusData)
	{
		this.hostData = hostData;
		this.statusData = statusData;
	}

	public HostData getHostData()
	{
		return hostData;
	}

	public void setHostData(HostData hostData)
	{
		this.hostData = hostData;
	}

	public StatusData getStatusData()
	{
		return statusData;
	}

	public void setStatusData(StatusData statusData)
	{
		this.statusData = statusData;
	}

	/**
	 *
	 * @param metricName - Name of the searched Metric
	 * @return requested Metric if it does exist
	 * @throws InvalidMetricRequestException if requested metric does not exist
	 */
	public Metric getSingleStatusMetric(String metricName) throws InvalidMetricRequestException
	{
		return statusData.getSingleMetricForRessource(metricName, null, null);
	}

	/**
	 *
	 * @param metricName - Name of requested metric
	 * @param serviceName - Name of service the metric belongs to
	 * @param hostName - name of host the service belongs to
	 * @return requested metric object if available
	 * @throws InvalidMetricRequestException if Metric does not exist
	 * @throws InvalidServiceRequestException if Service does not exist
	 * @throws InvalidHostRequestException if Host does not exist
	 */
	public Metric getSingleHostServiceMetric(String metricName, String serviceName, String hostName) throws InvalidHostRequestException, InvalidServiceRequestException, InvalidMetricRequestException
	{
		return hostData.getSingleMetricForRessource(metricName, serviceName, hostName);
	}

	/**
	 *
	 * @return Hashtable with all metrics of status data
	 */
	public Hashtable<String, Metric> getAllStatusMetrics()
	{
		return getStatusData().getAllMetrics();
	}

	/**
	 *
	 * @param serviceName - name of specific service
	 * @param hostName - hostname the service belongs to
	 * @return Hashtable with all the metrics that belong to the service
	 * @throws InvalidServiceRequestException if service does not exist
	 */
	public Hashtable<String, Metric> getAllMetricsForHostService(String serviceName, String hostName) throws InvalidServiceRequestException
	{
		return hostData.getHostTable().get(hostName).getServiceData().getAllMetricsForRessource(serviceName);
	}
}
