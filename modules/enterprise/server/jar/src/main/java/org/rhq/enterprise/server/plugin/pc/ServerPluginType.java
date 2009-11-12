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
package org.rhq.enterprise.server.plugin.pc;

import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * Defines the types of server-side plugins that are currently supported.
 * Server side plugin types are identified uniquely by their plugin descriptor types.
 * 
 * @author John Mazzitelli
 */
public class ServerPluginType {
    private final Class<? extends ServerPluginDescriptorType> descriptorType;

    public ServerPluginType(Class<? extends ServerPluginDescriptorType> descriptorType) {
        if (descriptorType == null) {
            throw new NullPointerException("descriptorType must not be null");
        }
        this.descriptorType = descriptorType;
    }

    /**
     * Given a string that was returned by a call to {@link #stringify()}, this
     * will create its server plugin type.
     *  
     * @param stringifiedType string representation of a server plugin type.
     *
     * @throws Exception if the string was invalid
     */
    @SuppressWarnings("unchecked")
    public ServerPluginType(String stringifiedType) throws Exception {
        this((Class<? extends ServerPluginDescriptorType>) Class.forName(stringifiedType));
    }

    public Class<? extends ServerPluginDescriptorType> getDescriptorType() {
        return this.descriptorType;
    }

    /**
     * Returns a "serialized" form of this instance by returning a string
     * representation of the type. This is not the same as {@link #toString()}.
     * The returned string can be used to reconstitute the type via
     * the constructor {@link ServerPluginType#ServerPluginType(String)}.
     * 
     * @return string representation of this type
     */
    public String stringify() {
        return this.descriptorType.getName();
    }

    @Override
    public String toString() {
        return this.descriptorType.getSimpleName().replace("DescriptorType", "");
    }

    @Override
    public int hashCode() {
        return this.descriptorType.getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ServerPluginType)) {
            return false;
        }
        return this.descriptorType.getName().equals(((ServerPluginType) obj).descriptorType.getName());
    }
}
