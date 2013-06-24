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

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import org.rhq.enterprise.server.rest.domain.Link;

/**
 * Deserialize incoming json Links into Link objects
 * Our input is like this:
 *
<pre>
   {
       "operationDefinitions": {
           "href": "http://localhost:7080/rest/operation/definitions?resourceId=10584"
       }
   }
 </pre>
 * @author Heiko W. Rupp
 * @see LinkSerializer
 */
public class LinkDeserializer extends JsonDeserializer<Link>{

    @Override
    public Link deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {

        String tmp = jp.getText(); // {
        jp.nextToken(); // skip over { to the rel
        String rel = jp.getText();
        jp.nextToken(); // skip over  {
        tmp = jp.getText();
        jp.nextToken(); // skip over "href"
        tmp = jp.getText();
//        jp.nextToken(); // skip over :
        jp.nextToken(); // skip to "http:// ... "
        String href = jp.getText();
        jp.nextToken(); // skip }
        tmp = jp.getText();
        jp.nextToken(); // skip }

        Link link = new Link(rel,href);

        return link;
    }
}
