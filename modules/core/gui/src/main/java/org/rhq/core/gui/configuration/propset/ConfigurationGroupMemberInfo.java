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
package org.rhq.core.gui.configuration.propset;

import org.rhq.core.domain.configuration.Configuration;

/**
 * @author Ian Springer
 */
public class ConfigurationGroupMemberInfo implements Comparable<ConfigurationGroupMemberInfo>
{
    private String label;
    private Configuration configuration; 

    public ConfigurationGroupMemberInfo(String label, Configuration configuration)
    {
        this.label = label;
        this.configuration = configuration;
    }

    public String getLabel()
    {
        return this.label;
    }

    public Configuration getConfiguration()
    {
        return this.configuration;
    }

    public int compareTo(ConfigurationGroupMemberInfo that)
    {
        return 0; // TODO
    }
}
