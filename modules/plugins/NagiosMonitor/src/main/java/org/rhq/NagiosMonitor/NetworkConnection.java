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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class implements the communication interface to mk_livetstatus
 *  
 * @author Alexander Kiefer
 */
public class NetworkConnection 
{	
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
	
	private LqlRequest lqlRequest;
	private ArrayList<String> queries;
	
	private LqlReply lqlReply;
	
	/**
	 * 
	 * @param destinationAdress - Adress of Host where MK_livestatus runs
	 * @param destinationPort - Port to communicate with Mk_livetstatus  
	 * @param lqlRequest -  LivestatusQueryLanguage request which is sent to MK_livetstatus
	 */
	public NetworkConnection(String destinationAdress, int destinationPort, LqlRequest lqlRequest)
	{
		this.destinationAdress = destinationAdress;
		this.destinationPort = destinationPort;
		
		this.lqlRequest = lqlRequest;
		
		queries = lqlRequest.getRequestQuery();
		lqlReply = new LqlReply(lqlRequest.getRequestType());
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
	 * @throws UnknownHostException
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
     * Method sets the query for the requests and gives the request type information to the LqlReply object
     * This information is necessary for the Controller class to create the right object type after receiving 
     * the answer from Nagios
     */
    public void setQueries()
    {	
		queries = lqlRequest.getRequestQuery();
		lqlReply = new LqlReply(lqlRequest.getRequestType());
    }
    
    /**
     * Method sends all commands which are contained in the query,
	 * receives the answers an writes them to the reply object    
     */
	public void sendAndReceive() 
    {
		ArrayList<String> resultList = new ArrayList<String>();    	
		String lineRead = "";
    	
    	//Iterate all query commands contained in the array list
		for(Iterator<String> it = queries.iterator(); it.hasNext(); ) 
    	{
    		try 
    		{
    			//TODO change the socket handling in livestatus
				openConnection();
				
				String s = it.next();
				
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
    		catch (UnknownHostException e) 
    		{
				//log.error(e);
			}
    		catch (IOException e) 
    		{
  			   //log.error(e);
			}
		}			
		//Write whole result list to LqlReply object
    	lqlReply.setLqlReply(resultList);
    }
	
	/**
	 * @return LqlReply object which contains the answer received via socket   
	 */
	public LqlReply getLqlReply()
	{
		return lqlReply;
	}
}
