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
 * 
 * @author Alexander Kiefer
 */
public class LqlResourceTypeRequest implements LqlRequest 
{
	private final String GET_RESOURCE_TYPES = "GET services\nColumns: display_name\nFilter: state = 0\n";
	
	private NagiosRequestType requestType;
	private ArrayList<String> requestQueryList;
	
	public LqlResourceTypeRequest()
	{
		requestType = new NagiosRequestType(NagiosRequestType.NagiosRequestTypes.RESOURCE_TYPE_REQUEST);
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

	public void setRequestQueryList() 
	{
		requestQueryList = new ArrayList<String>();
		requestQueryList.add(GET_RESOURCE_TYPES);
	}

	public void setRequestType(NagiosRequestType requestType) 
	{
		this.requestType = requestType;
	}

}
