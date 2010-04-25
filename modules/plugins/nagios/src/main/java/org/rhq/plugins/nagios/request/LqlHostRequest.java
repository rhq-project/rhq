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

import java.util.ArrayList;

/**
 * his class is a concrete implelentation of the LQLRequest interface for a host request
 *
 * @author Alexander Kiefer
 */
public class LqlHostRequest implements LqlRequest
{
	/**
	 * Strings with the Livestatus Query Language commands necessary to get the required host information
	 */
	private final String GET_NUMBER_OF_HOSTS = "GET hosts\nStats: state = 0\n";
	private final String GET_HOSTNAMES = "GET hosts\nColumns: name address\nFilter: state = 0\n";

	private NagiosRequestType requestType;
	private ArrayList<String> requestQueryList;

	/**
	 * Constructor sets the correct request type and the array list with the commands
	 */
	public LqlHostRequest()
	{
		requestType = new NagiosRequestType(NagiosRequestType.NagiosRequestTypes.HOST_REQUEST);
		setRequestQueryList();
	}

	public ArrayList<String> getRequestQueryList()
	{
		return requestQueryList;
	}

	public NagiosRequestType getRequestType()
	{
		return requestType;
	}

	/**
	 * setter that builds the array list with the lql commands
	 */
	public void setRequestQueryList()
	{
		requestQueryList = new ArrayList<String>();
		requestQueryList.add(GET_NUMBER_OF_HOSTS);
		requestQueryList.add(GET_HOSTNAMES);
	}

	public void setRequestType(NagiosRequestType requestType)
	{
		this.requestType = requestType;
	}
}
