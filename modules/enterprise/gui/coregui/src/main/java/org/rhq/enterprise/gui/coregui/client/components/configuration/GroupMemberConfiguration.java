/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.rhq.enterprise.gui.coregui.client.components.configuration;

import org.rhq.core.domain.configuration.Configuration;

/**
 * @author Ian Springer
 */
public class GroupMemberConfiguration {
    private int id;
    private String label;
    private Configuration configuration;

    public GroupMemberConfiguration(int id, String label, Configuration configuration) {
        this.id = id;
        this.label = (label != null) ? label : String.valueOf(id);
        this.configuration = configuration;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GroupMemberConfiguration that = (GroupMemberConfiguration)o;

        if (id != that.id) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "GroupMemberConfiguration{" +
            "id=" + id +
            ", label='" + label + '\'' +
            ", configuration=" + configuration +
            '}';
    }
}
