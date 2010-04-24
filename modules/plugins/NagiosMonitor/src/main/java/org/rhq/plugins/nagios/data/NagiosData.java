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

import org.rhq.plugins.nagios.error.InvalidHostRequestException;
import org.rhq.plugins.nagios.error.InvalidMetricRequestException;
import org.rhq.plugins.nagios.error.InvalidServiceRequestException;

/**
 * Interface with method definition for all kinds of data objects that have to be created according to
 * which kind of reply was received from livetstatus. The concrete types have to implement this interface
 * and implement the methods for their purpose
 *
 * @author Alexander Kiefer
 */

public interface NagiosData
{
	public void fillWithData(String[] ressourceNames, String[] metricNames, String[] metricValues);
	public Metric getSingleMetricForRessource(String metricName, String ressourceName, String hostname)
		throws InvalidMetricRequestException, InvalidServiceRequestException, InvalidHostRequestException;
}
