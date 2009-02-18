/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.plugins.jmx;

import java.util.Arrays;
import java.util.logging.LoggingMXBean;

import org.mc4j.ems.connection.bean.attribute.EmsAttribute;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;

/**
 * @author Greg Hinkle
 */
public class JavaUtilLoggingResourceComponent extends MBeanResourceComponent {
    @Override
    public Configuration loadResourceConfiguration() {
        EmsAttribute namesAttribute = getEmsBean().getAttribute("LoggerNames");

        String[] names = (String[]) namesAttribute.refresh();

        //There should only be 50 elements (checked against jmx-console, but it is returning 51 the first element is blank
        //so I put code in the for loop to not add if the name is blank
        Arrays.sort(names);

        LoggingMXBean logging = getEmsBean().getProxy(LoggingMXBean.class);

        Configuration configuration = new Configuration();
        PropertyList list = new PropertyList("AppenderList");
        for (String name : names) {
            if ((name != null) && !name.equals("")) {
                PropertyMap map = new PropertyMap("Appender");
                String level = getLoggerLevel(logging, name);
                map.put(new PropertySimple("name", name));
                map.put(new PropertySimple("level", level));
                list.add(map);
            }
        }

        configuration.put(list);
        return configuration;
    }

    public String getLoggerLevel(LoggingMXBean logging, String name) {
        String level = logging.getLoggerLevel(name);
        if (level != null) {
            if (level.equals("") && (logging.getParentLoggerName(name) != null)) {
                level = getLoggerLevel(logging, logging.getParentLoggerName(name));
            }
        } else {
            level = "Pseudo"; // only needed for the compare in updateResourceConfiguration() below
        }

        return level;
    }

    /**
     * Note, this only supports editing existing appenders; adding or removing appenders is not supported, since the
     * underlying JVM mBean (LoggingMXBean) does not support it.
     *
     * @param report the report which will contain the results of the requested update
     */
    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        PropertyList list = report.getConfiguration().getList("AppenderList");
        LoggingMXBean logging = getEmsBean().getProxy(LoggingMXBean.class);
        for (Property property : list.getList()) {
            PropertyMap map = (PropertyMap) property;
            String name = map.getSimple("name").getStringValue();
            String level = map.getSimple("level").getStringValue();

            if ((level != null) && !level.equals(getLoggerLevel(logging, name))) {
                try {
                    logging.setLoggerLevel(name, level);
                } catch (IllegalArgumentException iae) {
                    report.setErrorMessage("Error setting logger level: " + iae.getMessage());
                }
            }
        }

        report.setStatus(ConfigurationUpdateStatus.SUCCESS);
    }
}