package org.rhq.plugins.nagios.parser;
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
import java.util.regex.Pattern;

import org.rhq.plugins.nagios.data.NagiosData;
import org.rhq.plugins.nagios.error.InvalidReplyTypeException;
import org.rhq.plugins.nagios.reply.LqlReply;
import org.rhq.plugins.nagios.request.NagiosRequestType.NagiosRequestTypes;

/**
 * This class implements a parser that does the parsing of an LQLReply
 * It gets a LQLReply object an fills an instance of NagiosData with the parsed data
 *
 * @author Alexander Kiefer
 */

public class LqlReplyParser
{
	/**
	 *
	 * @param data
	 */
	public void parseLqlHostReply(LqlReply hostReply ,NagiosData data) throws InvalidReplyTypeException
	{
		String[] metricValues = null;

		if(hostReply.getContext().getNagiosRequestType() == NagiosRequestTypes.HOST_REQUEST)
		{
			int hostCounter = Integer.parseInt(	hostReply.getLqlReply().get(0));
			//Remove first element of host reply which is the number of hosts
			hostReply.getLqlReply().remove(0);

			//for each host split hostname and ip and put it to host object
			for(int i = 0; i < hostCounter; i++)
			{
				metricValues = hostReply.getLqlReply().get(i).split(Pattern.quote(";"));
				data.fillWithData(null, null, metricValues);
			}
		}
		else
		{
			throw new InvalidReplyTypeException(NagiosRequestTypes.HOST_REQUEST.toString(), hostReply.getContext().getNagiosRequestType().toString());
		}
	}

	/**
	 * Method to parse a LQLStatusReply
	 * @param data is concrete instance of a class that has to implement the interface NagiosData
	 */
	public void parseLqlStatusReply(LqlReply statusReply, NagiosData data) throws InvalidReplyTypeException
	{
		String[] metricNames = null;
		String[] metricValues = null;;

        ArrayList<String> lqlReply = statusReply.getLqlReply();
        if(statusReply.getContext().getNagiosRequestType() == NagiosRequestTypes.STATUS_REQUEST)
		{
			metricValues = lqlReply.get(0).split(Pattern.quote(";"));
		}
		else
		{
			throw new InvalidReplyTypeException(NagiosRequestTypes.STATUS_REQUEST.toString(), statusReply.getContext().getNagiosRequestType().toString());
		}

		//Status reply contains always two strings, first string is the name of the metrics
		//and the second string are the values that belong to the metrics
		metricNames = lqlReply.get(0).split(Pattern.quote(";"));
		metricValues = lqlReply.get(1).split(Pattern.quote(";"));

		//First param is not used in status objects and can be null
		data.fillWithData(null, metricNames, metricValues);
	}

	/**
	 * Method to parse a LQLStatusReply
	 * @param data is concrete instance of a class that has to implement the interface NagiosData
	 */
	public void parseLqlServiceReply(LqlReply serviceReply, NagiosData data) throws InvalidReplyTypeException
	{
		String[] resourceNames = new String[1];
		String[] metricNames = null;
		String[] metricValues = null;

        ArrayList<String> lqlReply = serviceReply.getLqlReply();
        if(serviceReply.getContext().getNagiosRequestType() != NagiosRequestTypes.SERVICE_REQUEST)
		{
			throw new InvalidReplyTypeException(NagiosRequestTypes.SERVICE_REQUEST.toString(), serviceReply.getContext().getNagiosRequestType().toString());
		}

		//First element is always the number of services running
//		int numberOfResources = Integer.parseInt(lqlReply.get(0));
		//Delete first element after information is stored
//		lqlReply.remove(0);

        int numberOfResources = lqlReply.size();

		for(int resourceCounter = 0; resourceCounter < numberOfResources; resourceCounter++)
		{
			//For each resource get the name --> the first elements contain the names
//			resourceNames = lqlReply.get(resourceCounter).split(Pattern.quote(";"));
    		//After the names of the resources there are always the names of the metrics
          //  String row = lqlReply.get(numberOfResources);
          //  metricNames = row.split(Pattern.quote(";"));
            metricNames = new String[]{ "display_name","plugin_output"};
    		//After the names of the metrics there are the values to each resource
// We filter for columns, so display_name is now always the first
//            int i = 0;
//            for (String name : metricNames) {
//                if (name.equals("display_name"))
//                    break;
//                i++;
//            }


//			metricValues = lqlReply.get(resourceCounter + (numberOfResources + 1)).split(Pattern.quote(";"), metricNames.length);
			metricValues = lqlReply.get(resourceCounter).split(Pattern.quote(";"));
            String resourceName = metricValues[0];
            resourceNames[0] = resourceName;

			//for a service all information are necessary because there is more than one service per host
			data.fillWithData(resourceNames, metricNames, metricValues);
		}
	}
}
