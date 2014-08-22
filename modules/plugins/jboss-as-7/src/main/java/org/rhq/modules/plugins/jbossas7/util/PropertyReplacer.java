/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.modules.plugins.jbossas7.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * @author Thomas Segismont
 */
public class PropertyReplacer {

    private PropertyReplacer() {
        // Utility class
    }

    /**
     * Replaces any "%xxx%" substring with the values of the <code>pluginConfig</code> properties "xxx".
     *
     * @param value the template
     * @param pluginConfig the configuration object for template properties
     * @return the result of template processing 
     */
    public static String replacePropertyPatterns(String value, Configuration pluginConfig) {
        Pattern pattern = Pattern.compile("(%([^%]*)%)");
        Matcher matcher = pattern.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String propName = matcher.group(2);
            PropertySimple prop = pluginConfig.getSimple(propName);
            String propValue = ((prop != null) && (prop.getStringValue() != null)) ? prop.getStringValue() : "";
            String propPattern = matcher.group(1);
            String replacement = (prop != null) ? propValue : propPattern;
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
