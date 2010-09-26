package org.rhq.plugins.nagios.network;
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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.plugins.nagios.reply.LqlReply;
import org.rhq.plugins.nagios.request.LqlRequest;


/**
 * This class implements the communication interface to mk_livetstatus
 *
 * @author Alexander Kiefer
 */
public class NetworkConnection
{
    private static final Log log = LogFactory.getLog(NetworkConnection.class);

    /**
     * Destination ip adress and port
     */
	private String destinationAdress;
    private int destinationPort;

    /**
     * Socket, Reader and Writer
     */
	private Socket socket;
	private PrintWriter printWriter;
	private InputStreamReader inputStreamReader;
	private BufferedReader bufferedReader;

	/**
	 *
	 * @param destinationAdress - Adress of Host where MK_livestatus runs
	 * @param destinationPort - Port to communicate with Mk_livetstatus
	 */
	public NetworkConnection(String destinationAdress, int destinationPort)
	{
		this.destinationAdress = destinationAdress;
		this.destinationPort = destinationPort;
	}

	/**
	 * Default constructor is private because it should not be used
	 */
	@SuppressWarnings("unused")
	private NetworkConnection()
	{

	}

	/**
	 * Method opens connection to Nagios
	 * @throws IOException
	 */
	public void openConnection() throws UnknownHostException, IOException
    {
		socket = new Socket(destinationAdress, destinationPort);
		printWriter = new PrintWriter(socket.getOutputStream(), true);
		inputStreamReader = new InputStreamReader(socket.getInputStream());
		bufferedReader = new BufferedReader(inputStreamReader);
    }

	/**
	 * Method closes connection to Nagios
	 * @throws IOException
	 */
    public void closeConnection() throws IOException
    {
    	bufferedReader.close();
    	inputStreamReader.close();
		printWriter.close();
		socket.close();
    }

    /**
     * Method sends all commands which are contained in the query,
	 * receives the answers an writes them to the reply object
 	 * @param lqlRequest -  LivestatusQueryLanguage request which is sent to MK_livetstatus
     */
	public LqlReply sendAndReceive(LqlRequest lqlRequest)
    {
		LqlReply livestatusReply = new LqlReply(lqlRequest.getRequestType());
		ArrayList<String> resultList = new ArrayList<String>();
		String lineRead = "";

    	//Iterate all query commands contained in the array list
		for(Iterator<String> it = lqlRequest.getRequestQueryList().iterator(); it.hasNext(); )
    	{
    		try
    		{
                String s = it.next();
                //TODO change the socket handling in livestatus
                openConnection(); // TODO move outside the loop to inprove performance -- needs keepalives enabled?


				//Write query out to Nagios
				printWriter.println(s);
	    		printWriter.flush();

	    		//Receive answer and write it to result list
				while((lineRead = bufferedReader.readLine()) != null)
				{
					resultList.add(lineRead);
				}

				closeConnection();
			}
    		catch (IOException e)
    		{
  			   log.error("Error in sendAndReceive: " + e.getMessage());
               // return an empty reply and not partial data
               return new LqlReply(lqlRequest.getRequestType());
			}
		}
		//Write whole result list to LqlReply object
    	livestatusReply.setLqlReply(resultList);

    	return livestatusReply;
    }
}
