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
 * This class implements a parser that does the parsing of an LQLReply 
 * 
 * @author Alexander Kiefer
 */
import java.util.regex.Pattern;

public class LqlReplyParser
{	
	private LqlReply lqlReply;
	
	public LqlReplyParser(LqlReply lqlReply)
	{
		this.lqlReply = lqlReply;
	}
	
	/**
	 * Method to parse a LQLStatusReply
	 * @param nagiosData is concrete instance of a class that has to implement the interface NagiosData
	 */
	public void parseLqlStatusReply(NagiosData data)
	{
		String[] metricNames = null;
		String[] metricValues = null;;
		
			
		metricNames = lqlReply.getLqlReply().get(0).split(Pattern.quote(";"));
		metricValues = lqlReply.getLqlReply().get(1).split(Pattern.quote(";"));  
		
		data.fillWithData(null, metricNames, metricValues);
	}
	
	/**
	 * Method to parse a LQLStatusReply
	 * @param nagiosData is concrete instance of a class that has to implement the interface NagiosData
	 */
	public void parseLqlServiceReply(NagiosData data) 
	{		
		String[] ressourceNames = null;
		String[] metricNames = null;
		String[] metricValues = null;
		
		int numberOfRessources = Integer.parseInt(lqlReply.getLqlReply().get(0));

		lqlReply.getLqlReply().remove(0);
				
		for(int ressourceCounter = 0; ressourceCounter < numberOfRessources; ressourceCounter++)
		{			
			ressourceNames = lqlReply.getLqlReply().get(ressourceCounter).split(Pattern.quote(";"));
    		metricNames = lqlReply.getLqlReply().get(numberOfRessources).split(Pattern.quote(";"));
    		metricValues = lqlReply.getLqlReply().get(ressourceCounter + (numberOfRessources + 1)).split(Pattern.quote(";"), metricNames.length);  
    		
			data.fillWithData(ressourceNames, metricNames, metricValues);
		}
	}
}
