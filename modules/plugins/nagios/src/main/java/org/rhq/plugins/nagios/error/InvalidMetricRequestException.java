package org.rhq.plugins.nagios.error;
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
 * This class implements an Exception that is thrown if a metric is requested that does not exist
 *
 * @author Alexander Kiefer
 */
public class InvalidMetricRequestException extends NagiosException
{
	/**
	 * Default Constructor is private because it should not be used
	 */
	@SuppressWarnings("unused")
	private InvalidMetricRequestException()
	{

	}

	/**
	 *
	 * @param metricName - Name of metric that was called is given to
	 * super constructor to maintain more detailed information in case of error
	 */
	public InvalidMetricRequestException(String metricName )
	{
		super("REQUESTED METRIC <" +  metricName + "> DOES NOT EXIST");
	}
}
