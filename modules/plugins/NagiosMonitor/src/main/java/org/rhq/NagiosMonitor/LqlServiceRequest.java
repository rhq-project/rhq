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

import java.util.ArrayList;
/**
 * This class is a concrete implelentation of the LQLRequest interface for a service request
 * 
 * @author Alexander Kiefer
 */
public class LqlServiceRequest implements LqlRequest
{
	/**
	 * Strings with the Livestatus Query Language commands necessary to get the required service information 
	 */
	private final String GET_NUMBER_OF_SERVICES = "GET services \nStats: state = 0\n";
	private final String GET_SERVICE_NAMES = "GET services\nColumns: display_name\n";
	private final String GET_SERVICE_METRICS = "GET services\n";
	
	private NagiosRequestType requestType;
	private ArrayList<String> requestQuery;
	
	/**
	 * Constructor sets the correct request type and the array list with the commands 
	 */
	public LqlServiceRequest()
	{
		requestType = new NagiosRequestType(NagiosRequestType.NagiosRequestTypes.SERVICE_REQUEST);
		setRequestQuery();
	}
	
	/**
	 * setter that builds the array list with the lql commands  
	 */
	public void setRequestQuery()
	{
		requestQuery = new ArrayList<String>();
		requestQuery.add(GET_NUMBER_OF_SERVICES);
		requestQuery.add(GET_SERVICE_NAMES);
		requestQuery.add(GET_SERVICE_METRICS);
	}

	public ArrayList<String> getRequestQuery() 
	{
		return requestQuery;
	}

	public NagiosRequestType getRequestType() 
	{
		return requestType;
	}

	public void setRequestType(NagiosRequestType requestType) 
	{
		this.requestType = requestType;
	}
}
