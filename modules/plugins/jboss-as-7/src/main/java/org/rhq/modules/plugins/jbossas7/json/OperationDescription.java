/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * // TODO: Document this
 * @author Heiko W. Rupp
 */
public class OperationDescription {
    @JsonProperty("operation-name")
    public String operationName;
    public String description;
    @JsonProperty("request-properties")
    public Map<String,As7RequestProperty> requestProperties;
//    @JsonProperty("reply-properties")
//    public ArrayList<As7ReplyProperty> replyProperties;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("OperationDescription");
        sb.append("{operationName='").append(operationName).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", requestProperties=").append(requestProperties);
//        sb.append(", replyProperties=").append(replyProperties);
        sb.append('}');
        return sb.toString();
    }

    public static class As7RequestProperty {
        public String description;
        public boolean required;

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("As7RequestProperty");
            sb.append("{description='").append(description).append('\'');
            sb.append(", required=").append(required);
            sb.append('}');
            return sb.toString();
        }
    }

    public  static class As7ReplyProperty {
        @JsonProperty("value-type")
        public String valueType;
        public String description;

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("As7ReplyProperty");
            sb.append("{valueType='").append(valueType).append('\'');
            sb.append(", description='").append(description).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
