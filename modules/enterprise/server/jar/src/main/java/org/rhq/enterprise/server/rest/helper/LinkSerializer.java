/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.rest.helper;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import org.rhq.enterprise.server.rest.domain.Link;

/**
 * Special serializer for Link objects that does not map the classical {rel:abc, href:xyz} scheme,
 * but which puts the rel name "at the outside" like  { abc : { href : xyz }} to make it easier for
 * clients to access the link.
 * See also https://bugzilla.redhat.com/show_bug.cgi?id=845244
 * @author Heiko W. Rupp
 */
public class LinkSerializer extends JsonSerializer<Link> {

    @Override
    public void serialize(Link link, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException, JsonProcessingException {

        jsonGenerator.writeStartObject();
        jsonGenerator.writeFieldName(link.getRel());

        jsonGenerator.writeStartObject();
        jsonGenerator.writeFieldName("href");
        jsonGenerator.writeString(link.getHref());
        jsonGenerator.writeEndObject();

        jsonGenerator.writeEndObject();
    }

    @Override
    public Class<Link> handledType() {
        return Link.class;
    }
}
