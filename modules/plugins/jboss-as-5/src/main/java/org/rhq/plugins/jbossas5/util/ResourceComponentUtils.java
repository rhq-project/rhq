/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * @author Ian Springer
 */
public class ResourceComponentUtils {    
    private static final String CUSTOM_PROPERTIES_PROPERTY = "customProperties";

    private static final Log LOG = LogFactory.getLog(ResourceComponentUtils.class);

    public static Map<String, PropertySimple> getCustomProperties(Configuration pluginConfig) {
        Map<String, PropertySimple> customProperties = new LinkedHashMap<String, PropertySimple>();
        if (pluginConfig == null)
            return customProperties;
        PropertyMap customPropsMap = pluginConfig.getMap(CUSTOM_PROPERTIES_PROPERTY);
        if (customPropsMap != null) {
            Collection<Property> customProps = customPropsMap.getMap().values();
            for (Property customProp : customProps) {
                if (!(customProp instanceof PropertySimple)) {
                    LOG.error("Custom property definitions in plugin configuration must be simple properties - property "
                            + customProp + " is not - ignoring...");
                    continue;
                }
                customProperties.put(customProp.getName(), (PropertySimple)customProp);
            }
        }
        return customProperties;
    }

    /**
     * TODO
     *
     * @param template
     * @param configuration
     * @return
     */
    public static String replacePropertyExpressionsInTemplate(String template, Configuration configuration)
    {
        if (template == null) {
            return null;
        }
        Pattern propExpressionPattern = Pattern.compile("%[^%]+%");
        Matcher matcher = propExpressionPattern.matcher(template);
        StringBuffer stringBuffer = new StringBuffer();
        while (matcher.find()) {
            // e.g. "%foo%"
            String match = matcher.group();
            // Strip off the percent signs, e.g. "foo".
            String propName = match.substring(1, match.length() - 1);
            PropertySimple prop = configuration.getSimple(propName);
            if (prop == null) {
                LOG.debug("WARNING: Template '" + template + "' references property '" + propName
                        + "' that does not exist in " + configuration.toString(true));
                continue;
            }
            if (prop.getStringValue() != null) {
                matcher.appendReplacement(stringBuffer, prop.getStringValue());
            }
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
    }
}
