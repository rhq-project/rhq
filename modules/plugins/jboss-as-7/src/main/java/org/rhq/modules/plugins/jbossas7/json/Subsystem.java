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

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Heiko W. Rupp
 */
public class Subsystem {

    String subsystemName;

    @JsonProperty("head-comment-allowed") boolean headCommentAllowed;
    @JsonProperty("tail-comment-allowed") boolean tailCommentAllowed;
//    public DataType type;
    public String description;
    @JsonProperty("attributes") Map <String,Attribute> attributes;
    @JsonProperty("model-description")
    Subsystem modelDescription;
    @JsonProperty("operations") Map<String,Operation> operations = new HashMap<String, Operation>();
    public Map<String,Subsystem> children;


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Subsystem");
        sb.append("{headCommentAllowed=").append(headCommentAllowed);
        sb.append(", tailCommentAllowed=").append(tailCommentAllowed);
//        sb.append(", type=").append(type).append('\n');
        sb.append(", description='").append(description).append('\'').append('\n');
        sb.append(", attributes=\n");
        if (attributes!=null) {
            for (Map.Entry<String,Attribute> attr : attributes.entrySet())
                sb.append("    ").append(attr).append('\n');
        }
        sb.append(", model-description=").append(modelDescription).append('\n');
        sb.append("\n, operations=");
        if (operations!=null) {
            for (Map.Entry<String,Operation> ops : operations.entrySet())
                sb.append("    ").append(ops).append('\n');
            }
        sb.append(", children=").append(children).append('\n');
        sb.append("\n}");
        return sb.toString();
    }
}
