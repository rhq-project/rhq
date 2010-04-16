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
 * This class implements all the possible types of requests that can be sent to Nagios
 * 
 * @author Alexander Kiefer
 */
public class NagiosRequestTypes 
{
	static final String HOST_REQUEST = "HOST_REQUEST";
	static final String SERVICE_REQUEST = "SERVICE_REQUEST";
	static final String HOSTGROUP_REQUEST = "HOSTGROUP_REQUEST";
	static final String SERVICEGROUP_REQUEST = "SERVICEGROUP_REQUEST";
	static final String CONTACTGROUP_REQUEST = "CONTACTGROUP_REQUEST";
	static final String STATUS_REQUEST = "STATUS_REQUEST";
}
