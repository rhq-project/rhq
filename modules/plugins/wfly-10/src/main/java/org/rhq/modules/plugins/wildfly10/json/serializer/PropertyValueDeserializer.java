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
package org.rhq.modules.plugins.wildfly10.json.serializer;

import java.io.IOException;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import org.rhq.modules.plugins.wildfly10.json.PROPERTY_VALUE;

/**
 * Deserialize {"foo":"bar"} into a PROPERTY_VALUE
 * @author Heiko W. Rupp
 */
public class PropertyValueDeserializer extends JsonDeserializer<PROPERTY_VALUE> {


    @Override
    public PROPERTY_VALUE deserialize(JsonParser jsonParser,
                                      DeserializationContext deserializationContext) throws IOException, JsonProcessingException {

        String tmp = jsonParser.getText(); // {
        jsonParser.nextToken();
        String key = jsonParser.getText();
        jsonParser.nextToken();
        String value = jsonParser.getText();
        jsonParser.nextToken();
        tmp = jsonParser.getText(); // }

        PROPERTY_VALUE pv = new PROPERTY_VALUE(key,value);
        return pv;
    }
}
