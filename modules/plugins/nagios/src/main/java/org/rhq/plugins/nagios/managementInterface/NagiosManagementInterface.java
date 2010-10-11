package org.rhq.plugins.nagios.managementInterface;
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

import java.util.Collection;

import org.rhq.plugins.nagios.controller.Controller;
import org.rhq.plugins.nagios.data.Host;
import org.rhq.plugins.nagios.data.HostData;
import org.rhq.plugins.nagios.data.NagiosSystemData;
import org.rhq.plugins.nagios.data.ServiceData;
import org.rhq.plugins.nagios.data.StatusData;
import org.rhq.plugins.nagios.error.InvalidMetricRequestException;
import org.rhq.plugins.nagios.error.InvalidReplyTypeException;
import org.rhq.plugins.nagios.error.InvalidServiceRequestException;
import org.rhq.plugins.nagios.network.NetworkConnection;
import org.rhq.plugins.nagios.reply.LqlReply;
import org.rhq.plugins.nagios.request.LqlHostRequest;
import org.rhq.plugins.nagios.request.LqlServiceRequest;
import org.rhq.plugins.nagios.request.LqlStatusRequest;

/**
 * Class implements the central control class that does all the work to call the right classes
 *
 * @author Alexander Kiefer
 */
public class NagiosManagementInterface
{
	private String livestatusAddress;
	private int livestatusPort;

	private NetworkConnection livestatusConnectionInterface;
	private Controller controller;


	/**
	 *
	 *
	 * @param livestatusAddress - IP address of mk_livestatus socket
	 * @param livetstatusPort - Port number of mk_livestatus socket
	 */
	public NagiosManagementInterface(String livestatusAddress, int livetstatusPort)
	{
		this.livestatusAddress = livestatusAddress;
		this.livestatusPort = livetstatusPort;
		livestatusConnectionInterface = new NetworkConnection(this.livestatusAddress, this.livestatusPort);
		controller = new Controller();
	}

    /**
     * Ping the nagios server by sending a host request.
     * @return true if >=1 hosts were listed in the answer or false otherwise
     */
    public boolean pingNagios() {
        LqlHostRequest hostRequest = new LqlHostRequest();
        LqlReply hostReply = livestatusConnectionInterface.sendAndReceive(hostRequest);
        if (hostReply.getLqlReply().size()>0)
            return true;

        return false;
    }

	/**
	 * Method does all the necessary steps to get the data about the current nagios hosts
	 *
	 * @throws InvalidReplyTypeException
	 * @throws InvalidMetricRequestException
	 * @throws InvalidServiceRequestException
	 */
	private HostData getHostInformation() throws InvalidReplyTypeException, InvalidServiceRequestException, InvalidMetricRequestException
	{
		LqlHostRequest hostRequest = new LqlHostRequest();
		LqlReply hostReply= new LqlReply(hostRequest.getRequestType());

		//Send host data request and fill reply object
		hostReply = livestatusConnectionInterface.sendAndReceive(hostRequest);

		HostData hostData = (HostData) controller.createDataModel(hostReply);
		Collection<Host> collection = hostData.getHostTable().values();

		//request service information for all current nagios hosts
		for(Host host : collection)
		{
			//Give hostname to LQLRequest to complete the lql query in LqlServiceRequest instance
			LqlServiceRequest lqlServiceRequest = new LqlServiceRequest(host.getHostName());

			//Set request Type for later usage when concrete instances are created in createDataModel
			LqlReply lqlServiceReply = new LqlReply(lqlServiceRequest.getRequestType());
			//Write result of service request to reply object
			lqlServiceReply = livestatusConnectionInterface.sendAndReceive(lqlServiceRequest);

			//Fill service data object
			ServiceData serviceData = (ServiceData) controller.createDataModel(lqlServiceReply);

			//put service Data object to the specific host it belongs to
	    	hostData.getHostTable().get(host.getHostName()).setServiceData(serviceData);
		}

		return hostData;
	}

	/**
	 * Method does all the necessary steps to get the status data of Nagios system
	 *
	 * @throws InvalidReplyTypeException
	 */
	private StatusData getStatusInformation() throws InvalidReplyTypeException
	{
		LqlStatusRequest lqlStatusRequest = new LqlStatusRequest();

		//Set request Type for later usage when concrete instances are created in createDataModel
		LqlReply lqlStatusReply = new LqlReply(lqlStatusRequest.getRequestType());
		//Write result of status request to reply object
		lqlStatusReply = livestatusConnectionInterface.sendAndReceive(lqlStatusRequest);

		//Fill service data object
		StatusData statusData = (StatusData) controller.createDataModel(lqlStatusReply);

//		String test = serviceData.getSingleMetricForRessource("host_execution_time", "Current Load", null).getId();
//    	String test2 = serviceData.getSingleMetricForRessource("host_execution_time", "Current Load", null).getValue();

//    	System.out.println(test);
//    	System.out.println(test2);

		return statusData;
	}

	public NagiosSystemData createNagiosSystemData() throws InvalidReplyTypeException, InvalidServiceRequestException, InvalidMetricRequestException
	{
		HostData hostData = getHostInformation();
		StatusData statusData = getStatusInformation();

		NagiosSystemData nagiosSystemData = new NagiosSystemData(hostData, statusData);

		return nagiosSystemData;
	}
}
