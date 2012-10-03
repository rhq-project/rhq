/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.plugins.server;

import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.plugins.jbossas7jmx.AbstractDiscoveryComponent;

/**
 * Leverage the provided discovery component class to bridge between this plugin and the JMX
 * plugin, which does all of the heavy lifting. 
 * 
 * @author Jay Shaughnessy
 */
public class SubsystemDiscoveryComponent<T extends ResourceComponent<?>> extends AbstractDiscoveryComponent<T> {

}
