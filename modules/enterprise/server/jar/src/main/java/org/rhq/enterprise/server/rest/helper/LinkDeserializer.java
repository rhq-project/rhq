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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jackson.JsonLocation;
import org.codehaus.jackson.JsonParseException;
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

    Pattern textPattern = Pattern.compile("\\S+"); // Non whitespace; could possibly be narrowed

    @Override
    public Link deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {

        String tmp = jp.getText(); // {
        validate(jp, tmp,"{");
        jp.nextToken(); // skip over { to the rel
        String rel = jp.getText();
        validateText(jp, rel);
        jp.nextToken(); // skip over  {
        tmp = jp.getText();
        validate(jp, tmp,"{");
        jp.nextToken(); // skip over "href"
        tmp = jp.getText();
        validate(jp, tmp,"href");
        jp.nextToken(); // skip to "http:// ... "
        String href = jp.getText();
        validateText(jp, href);
        jp.nextToken(); // skip }
        tmp = jp.getText();
        validate(jp, tmp, "}");
        jp.nextToken(); // skip }
        tmp = jp.getText();
        validate(jp, tmp, "}");

        Link link = new Link(rel,href);

        return link;
    }

    private void validateText(JsonParser jsonParser, String input) throws JsonProcessingException {
        Matcher m = textPattern.matcher(input);
        if (!m.matches()) {
            throw new JsonParseException("Unexpected token: " + input, jsonParser.getTokenLocation());
        }
    }

    private void validate(JsonParser jsonParser, String input, String expected) throws JsonProcessingException {
        if (!input.equals(expected)) {
            throw new JsonParseException("Unexpected token: " + input, jsonParser.getTokenLocation());
        }
    }
}
