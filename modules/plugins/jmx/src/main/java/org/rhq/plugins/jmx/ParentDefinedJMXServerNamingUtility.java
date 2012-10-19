/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.plugins.jmx;

import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Delegate for the original version of this
 * class that got moved to util/
 * @deprecated Use the version of the class in the util Package
 * @see org.rhq.plugins.jmx.util.ParentDefinedJMXServerNamingUtility
 * @author Heiko W. Rupp
 */
@Deprecated
public class ParentDefinedJMXServerNamingUtility {
    public static final String PROPERTY_CHILD_JMX_SERVER_NAME = "childJmxServerName";

    /** @deprecated use the version in util package instead */
    @Deprecated
    public static String getJVMName(ResourceDiscoveryContext<?> context) {
        return org.rhq.plugins.jmx.util.ParentDefinedJMXServerNamingUtility.getJVMName(context);
    }
}
