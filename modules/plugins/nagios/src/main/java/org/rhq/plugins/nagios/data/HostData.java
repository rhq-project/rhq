package org.rhq.plugins.nagios.data;
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

import java.util.Hashtable;
import org.rhq.plugins.nagios.error.InvalidHostRequestException;
import org.rhq.plugins.nagios.error.InvalidMetricRequestException;
import org.rhq.plugins.nagios.error.InvalidServiceRequestException;

/**
 * Class implements HostData object that contains all the host objects of a Nagios system
 *
 * @author Alexander Kiefer
 */
public class HostData implements NagiosData
{
	private Hashtable<String, Host> hostTable;

	public HostData()
	{
		hostTable = new Hashtable<String, Host>();
	}


	/**
	 * Method calculates the current number of Hosts, creates Host objects for each
	 * of them and puts the objects into the Hashtable for the hosts
	 */
	public void fillWithData(String[] ressourceNames, String[] metricNames, String[] metricValues)
	{
		//divide length of metric values with 2 to get number of hosts because
		//its always a pair of hostname/host ip
		int hostCounter = metricValues.length / 2;
		int index = 0;

		//for number of hosts
		for(int stepCounter = 0; stepCounter < hostCounter; stepCounter++)
		{
			Host host = null;
			//necessary because index is double of host number
			index = stepCounter * 2;

			//create host instance and fill it with name/ip address
			host = new Host(metricValues[index], metricValues[index + 1] );

			//put new instance in hash table
			hostTable.put(host.getHostName(), host);
		}
	}

	/**
	 *
	 */
	public Metric getSingleMetricForRessource(String metricName, String ressourceName, String hostname)
			throws InvalidHostRequestException, InvalidServiceRequestException, InvalidMetricRequestException
	{
		Metric metric = null;

		//check if requested hostname is element of hashtable
		if(getHostTable().containsKey(hostname))
		{
			//fill metric object with data
			metric = getHostTable().get(hostname).getServiceData().getSingleMetricForRessource(metricName, ressourceName, hostname);
		}
		else
		{
			//if not throw exception
			throw new InvalidHostRequestException(hostname);
		}

		return metric;
	}

	public Hashtable<String, Host> getHostTable()
	{
		return hostTable;
	}

	public void setHostTable(Hashtable<String, Host> hostTable)
	{
		this.hostTable = hostTable;
	}
}
