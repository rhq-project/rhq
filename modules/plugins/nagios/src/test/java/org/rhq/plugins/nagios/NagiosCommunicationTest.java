package org.rhq.plugins.nagios;
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

import org.rhq.plugins.nagios.controller.NagiosManagementInterface;
import org.rhq.plugins.nagios.data.NagiosSystemData;
import org.rhq.plugins.nagios.error.InvalidHostRequestException;
import org.rhq.plugins.nagios.error.InvalidMetricRequestException;
import org.rhq.plugins.nagios.error.InvalidReplyTypeException;
import org.rhq.plugins.nagios.error.InvalidServiceRequestException;
import org.rhq.plugins.nagios.network.NetworkConnection;
import org.rhq.plugins.nagios.reply.LqlReply;
import org.rhq.plugins.nagios.request.LqlResourceTypeRequest;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Simple Test class that works without rhq, uses implemented classes
 *
 * @author Alexander Kiefer
 */

public class NagiosCommunicationTest
{
	public void nagiosCommunicationTest()
	{
		Logger logger = Logger.getLogger(NagiosCommunicationTest.class);
		logger.setLevel(Level.INFO);
		BasicConfigurator.configure();

		String ip = "172.31.127.218";
		int port = 6557;
		LqlResourceTypeRequest resourceTypeRequest = new LqlResourceTypeRequest();
		
		NetworkConnection livestatusConnectionInterface = new NetworkConnection(ip, port);
		LqlReply reply = livestatusConnectionInterface.sendAndReceive(resourceTypeRequest);
		
		for(int i = 0; i < reply.getLqlReply().size(); i++)
		{
			System.out.println(reply.getLqlReply().get(i));
		}
	
//		NagiosManagementInterface nagiosManagementInterface= new NagiosManagementInterface(ip,port);
//		NagiosSystemData nagiosSystemData= null;
//
//		try
//		{
//			nagiosSystemData = nagiosManagementInterface.createNagiosSystemData();
//
//			logger.info("nagiosComTest: " + nagiosSystemData.getSingleHostServiceMetric("execution_time", "Current Load", "localhost").getId());
//			logger.info("nagiosComTest: " + nagiosSystemData.getSingleHostServiceMetric("execution_time", "Current Load", "localhost").getValue());
//
//			logger.info("nagiosComTest: " + nagiosSystemData.getSingleStatusMetric("requests").getId());
//			logger.info("nagiosComTest: " + nagiosSystemData.getSingleStatusMetric("requests").getValue());
//		}
//		catch (InvalidReplyTypeException e)
//		{
//			logger.info(e);
//		}
//		catch (InvalidMetricRequestException e)
//		{
//			logger.info(e);
//		}
//		catch (InvalidServiceRequestException e)
//		{
//			logger.info(e);
//		}
//		catch (InvalidHostRequestException e)
//		{
//			logger.info(e);
//		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		NagiosCommunicationTest nagiosCommunicationTest = new NagiosCommunicationTest();
		nagiosCommunicationTest.nagiosCommunicationTest();
	}
}
