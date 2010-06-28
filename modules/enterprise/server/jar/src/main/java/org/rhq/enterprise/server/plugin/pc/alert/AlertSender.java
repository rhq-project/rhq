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
package org.rhq.enterprise.server.plugin.pc.alert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;

/**
 * Abstract base class for all Alert senders. In order to implement your
 * AlertSender, you need to at least overwrite #send(). When the AlertSenderManager
 * constructs this object, it will inject the preferences for your specific alert sender
 * type.
 * For each Alert a new instance of your implementation will be called and destroyed
 * afterwards.
 *
 * @author Heiko W. Rupp
 *
 */
public abstract class AlertSender<T extends ServerPluginComponent> {

    /** Configuration from the global per plugin type preferences */
    protected Configuration preferences;

    /** Configuration from the per alert definition parameters */
    protected Configuration alertParameters;

    /** Configuration from the per alert definition parameters */
    protected Configuration extraParameters;

    /** Global component holding persistent resources */
    protected T pluginComponent;

    /** Environement of the plugin to e.g. get the classloader */
    protected ServerPluginEnvironment serverPluginEnvironment;

    /**
     * This method is called to actually send an alert notification.
     * This is where you implement all functionality.
     *
     * The return value is a SenderResult object, which encodes a log message,
     * success or failure and can contain email addresses that got computed by
     * your AlertSender and which will be sent by the system after *all* senders
     * have been run.
     * @param alert the Alert to operate on
     * @return result of sending - a ResultState and a message for auditing
     */
    public abstract SenderResult send(Alert alert);

    /**
     * Allow users to see a preview of the stored configuration data without having to edit it.
     * A default implementation is already provided which will print the properties in alphabetical
     * order, one per line, each followed by a string representation of that property's data.
     */
    public String previewConfiguration() {
        StringBuilder builder = new StringBuilder();

        List<Property> properties = new ArrayList<Property>(alertParameters.getProperties());
        Collections.sort(properties); // alpha sort by property name

        boolean first = true;
        for (Property next : properties) {
            if (first) {
                first = false;
            } else {
                builder.append(" | ");
            }
            builder.append(next.getName());
            builder.append(": ");
            builder.append(printProperty(next));
        }

        return builder.toString();
    }

    private String printProperty(Property property) {
        if (property instanceof PropertySimple) {
            return ((PropertySimple) property).getStringValue();
        } else if (property instanceof PropertyMap) {
            Map<String, Property> map = ((PropertyMap) property).getMap();
            StringBuilder builder = new StringBuilder();
            for (Property next : map.values()) {
                builder.append(printProperty(next));
            }
            return builder.toString();
        }
        return "no preview available";
    }

    /**
     * Presumes the data is in the format "|a|b|c|d|e|"
     * where '|' delimits all elements as well as wraps
     * the entire expression.
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> unfence(String fencedData, Class<T> type) {
        String[] elements = fencedData.split("\\|");
        List<T> results = new ArrayList<T>(elements.length);

        if (Integer.class.equals(type)) {
            for (String next : elements) {
                if (next.length() != 0) {
                    results.add((T) Integer.valueOf(next));
                }
            }
        } else if (String.class.equals(type)) {
            for (String next : elements) {
                if (next.length() != 0) {
                    results.add((T) next);
                }
            }
        } else {
            throw new IllegalArgumentException("No support for unfencing data of type " + type);
        }
        return results;
    }

    /**
     * Takes the list of elements e1, e2, e3 and fences
     * them with '|' delimiters such that the result looks
     * like "|e1|e2|e3|"
     */
    public static String fence(List<?> elements) {
        if (elements.size() == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append('|');
        for (Object next : elements) {
            builder.append(String.valueOf(next)).append('|');
        }
        return builder.toString();
    }
}
