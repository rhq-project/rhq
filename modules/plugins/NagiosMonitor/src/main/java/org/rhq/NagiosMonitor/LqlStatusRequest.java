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
 * This class is a concrete implementation of the LQLRequest interface for a status request
 * 
 * @author Alexander Kiefer
 */
public class LqlStatusRequest implements LqlRequest 
{
	private final String GET_STATUS_METRICS = "GET status\n";
		
	private String requestType;
	private ArrayList<String> requestQuery;
	
	public LqlStatusRequest()
	{
		requestType = NagiosRequestTypes.STATUS_REQUEST;
		setRequestQuery();
	}
	
	public void setRequestQuery()
	{
		requestQuery = new ArrayList<String>();
		requestQuery.add(GET_STATUS_METRICS);
	}
	
	public ArrayList<String> getRequestQuery() 
	{
		return requestQuery;
	}

	public String getRequestType() 
	{	
		return requestType;
	}

	public void setRequestType(String requestType) 
	{
		this.requestType = requestType;
	}
}
