package org.rhq.plugins.nagios.request;
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
 * This class implements a request type which is necessary for the later creation of the object instances
 * after receiving the reply. It makes the Controller class know  which kind of object it has to create
 * to store the information received from livetstatus
 *
 * @author Alexander Kiefer
 */
public class NagiosRequestType
{
	/**
	 * Enumeration type for all the possible request types
	 */
	public static enum NagiosRequestTypes
	{
		HOST_REQUEST,
		SERVICE_REQUEST ,
		HOSTGROUP_REQUEST,
		SERVICEGROUP_REQUEST,
		CONTACTGROUP_REQUEST,
		STATUS_REQUEST,
		RESOURCE_TYPE_REQUEST;
	}

	private NagiosRequestTypes nagiosRequestType;

	public NagiosRequestType(NagiosRequestTypes nagiosRequestType)
	{
		this.nagiosRequestType = nagiosRequestType;
	}

	public NagiosRequestTypes getNagiosRequestType()
	{
		return nagiosRequestType;
	}

	public void setNagiosRequestType(NagiosRequestTypes nagiosRequestType)
	{
		this.nagiosRequestType = nagiosRequestType;
	}
}


