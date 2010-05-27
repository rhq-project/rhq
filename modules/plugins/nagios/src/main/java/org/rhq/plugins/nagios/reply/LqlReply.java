package org.rhq.plugins.nagios.reply;
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

import org.rhq.plugins.nagios.request.NagiosRequestType;


/**
 * This class implements a reply that is sent by nagios as an answer to a specific request
 *
 * @author Alexander Kiefer
 */

public class LqlReply
{
	/**
	 * Each LQLReply has a context and a Array list with the reply Strings
	 */
	private NagiosRequestType context;
	private ArrayList<String> lqlReply;

	/**
	 * private because it should not be used
	 */
	private LqlReply()
	{

	}

	public LqlReply(NagiosRequestType context)
	{
		this.context = context;
	}

	public LqlReply(NagiosRequestType context, ArrayList<String> lql_reply)
	{
		this.context = context;
		this.lqlReply = lql_reply;
	}

	public NagiosRequestType getContext()
	{
		return context;
	}

	public void setContext(NagiosRequestType context)
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
