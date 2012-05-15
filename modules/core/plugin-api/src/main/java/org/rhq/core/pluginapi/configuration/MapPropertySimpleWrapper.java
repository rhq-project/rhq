/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.core.pluginapi.configuration;

import java.util.LinkedHashMap;
import java.util.Map;

import org.rhq.core.domain.configuration.PropertySimple;

/**
 * @author Ian Springer
 */
public class MapPropertySimpleWrapper {

    private PropertySimple prop;

    public MapPropertySimpleWrapper(PropertySimple prop) {
        if (prop == null) {
            throw new IllegalArgumentException("'prop' parameter must not be null.");
        }
        this.prop = prop;
    }

    /**
     * @param map
     * @throws IllegalArgumentException if the map values can not be translated to a storable string. Typically this
     * means max property length is exceeded.
     */
    public void setValue(Map<String, String> map) {
        String stringValue;
        if (map != null) {
            StringBuilder buffer = new StringBuilder();
            for (String key : map.keySet()) {
                String value = map.get(key);
                buffer.append(key).append('=').append(value).append('\n');
            }
            stringValue = buffer.toString();
        } else {
            stringValue = null;
        }

        // If the value is too long then don't store it, because it will likely invalidate the Map on the way back out.
        if (null != stringValue && stringValue.length() > PropertySimple.MAX_VALUE_LENGTH) {
            throw new IllegalArgumentException(stringValue);
        }

        this.prop.setStringValue(stringValue);
    }

    public Map<String, String> getValue() {
        Map<String, String> map = new LinkedHashMap<String, String>();

        String stringValue = this.prop.getStringValue();
        if (stringValue != null) {
            String[] lines = stringValue.split("\n+");
            for (String line : lines) {
                String entry = line.trim();
                int equalsIndex = entry.indexOf('=');
                if (equalsIndex == -1) {
                    throw new IllegalStateException("Malformed entry (no equals sign): " + entry);
                }
                String key = entry.substring(0, equalsIndex);
                String value = entry.substring(equalsIndex + 1);
                map.put(key, value);
            }
        }

        return map;
    }

}
