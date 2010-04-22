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
 *	This class creates the object instance to save the nagios data that was requested from mk_livestatus
 *	The created model depends on the kind of request that was send to livestatus
 *
 * @author Alexander Kiefer
 */
public class Controller 
{
	private LqlReply lqlReply;
	private LqlReplyParser lqlReplyParser;
	
	public Controller(LqlReply lqlReply)
	{
		this.lqlReply = lqlReply;
		lqlReplyParser = new LqlReplyParser(this.lqlReply);
	}
	
	/**
	 * Method creates object depending on the context of the reply that was created an puts
	 * it to the LqlReplyParser where it is filled with the data received from nagios 
	 *	
	 * @return concrete instance of NagiosData where data has been parsed into
	 */
	public NagiosData createDataModel()
	{
		NagiosData data = null;
		
		//each LqlReply gets a context when its created to know later what kind of data it will contain
		switch(lqlReply.getContext().getNagiosRequestType())
		{
			case SERVICE_REQUEST:
				data = new ServiceData();
				lqlReplyParser.parseLqlServiceReply(data);
				break;
			case STATUS_REQUEST:
				data = new StatusData();
				lqlReplyParser.parseLqlStatusReply(data);
				break;
		}
		
		return data;
	}
}
