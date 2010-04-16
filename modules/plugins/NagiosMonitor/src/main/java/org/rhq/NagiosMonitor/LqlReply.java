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

/**
 * This class implements a reply that is sent by nagios as an answer to a specific request
 * 
 * @author Alexander Kiefer
 */
import java.util.ArrayList;

public class LqlReply 
{
	/**
	 * Each LQLReply has a context and a Array list with the reply Strings 
	 */
	private String context;
	private ArrayList<String> lqlReply;
	
	public LqlReply(String context)
	{
		this.context = context;
		//TODO ?? 
		lqlReply = null;
	}
	
	public LqlReply(String context, ArrayList<String> lql_reply)
	{
		this.context = context;
		this.lqlReply = lql_reply;
	}
	
	public String getContext() 
	{
		return context;
	}

	public void setContext(String context) 
	{
		this.context = context;
	}

	public ArrayList<String> getLqlReply()
	{
		return lqlReply;
	}

	public void setLqlReply(ArrayList<String> lql_reply)
	{
		this.lqlReply = lql_reply;
	}
}
