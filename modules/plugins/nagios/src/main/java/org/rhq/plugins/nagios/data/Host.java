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

/**
 * Class implements a single Host which is monitored by Nagios
 *
 * @author Alexander Kiefer
 */
public class Host
{
	private String hostName;
	private String hostIp;
	private ServiceData serviceData;

	/**
	 *
	 * @param hostName - Name of the host computer
	 * @param hostIp - IP adress of host computer
	 * @param serviceData - Data of all the services running on the host
	 */
	public Host(String hostName, String hostIp, ServiceData serviceData)
	{
		this.hostName = hostName;
		this.hostIp = hostIp;
		this.serviceData = serviceData;
	}

	public Host(String hostName, String hostIp)
	{
		this.hostName = hostName;
		this.hostIp = hostIp;
	}

	public String getHostName()
	{
		return hostName;
	}

	public void setHostName(String hostName)
	{
		this.hostName = hostName;
	}

	public String getHostIp()
	{
		return hostIp;
	}

	public void setHostIp(String hostIp)
	{
		this.hostIp = hostIp;
	}

	public ServiceData getServiceData()
	{
		return serviceData;
	}

	public void setServiceData(ServiceData serviceData)
	{
		this.serviceData = serviceData;
	}
}
