/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7.json;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Heiko W. Rupp
 */
public class Attribute {

    public String name;
    public String description;
    public boolean required;
    public boolean nillable;
    @JsonProperty("access-type")
    public AccessType accessType;
    public Storage storage;
    public Type type;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Attribute");
        sb.append("{name='").append(name).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", required=").append(required);
        sb.append(", nillable=").append(nillable);
        sb.append(", accessType='").append(accessType).append('\'');
        sb.append(", storage='").append(storage).append('\'');
        sb.append(", type=").append(type);
        sb.append('}');
        return sb.toString();
    }


    public enum AccessType {
        METRIC("metric"),
        READ_ONLY("read-only")
        ;

        String jsonName;

        AccessType(String jsonName) {
            this.jsonName = jsonName;
        }

        @Override
        public String toString() {
            return jsonName;
        }
    }

    public enum Storage {
        CONFIGURATION("configuration"),
        RUNTIME("runtime")
        ;

        String jsonName;

        Storage(String jsonName) {
            this.jsonName =jsonName;
        }

        @Override
        public String toString() {
            return jsonName;
        }
    }
}
