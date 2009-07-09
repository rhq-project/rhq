/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.platform;

import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.AggregateProcessInfo;
import org.rhq.core.system.SystemInfo;

import java.util.Set;
import java.util.List;
import java.beans.PropertyDescriptor;
import java.beans.Introspector;
import java.beans.BeanInfo;
import java.io.*;

/**
 * @author Greg Hinkle
 */
public class ProcessComponent implements ResourceComponent, MeasurementFacet {

    private enum Type {
        pidFile, piql
    }

    private ProcessInfo process;

    private ResourceContext resourceContext;

    private Type type;
    private String pidFile;
    private String piql;
    private boolean fullProcessTree;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;

        Configuration config = this.resourceContext.getPluginConfiguration();

        this.type = Type.valueOf(config.getSimpleValue("type", "pidFile"));

        this.pidFile = config.getSimpleValue("pidFile", null);
        this.piql = config.getSimpleValue("piql", null);
        this.fullProcessTree = config.getSimple("fullProcessTree").getBooleanValue();


    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        return getProcess().isRunning() ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    private ProcessInfo getProcess() {
        if (this.process == null || !this.process.isRunning()) {
            this.process = getProcessForConfiguration(resourceContext.getPluginConfiguration(), resourceContext.getSystemInformation());
        }
        return this.process;
    }


    protected static ProcessInfo getProcessForConfiguration(Configuration config, SystemInfo systemInfo) {
        long pid = 0;
        Type type = Type.valueOf(config.getSimpleValue("type", "pidFile"));

        String pidFile = config.getSimpleValue("pidFile", null);
        String piql = config.getSimpleValue("piql", null);
        boolean fullProcessTree = config.getSimple("fullProcessTree").getBooleanValue();

        if (type == Type.pidFile) {
            File file = new File(pidFile);
            if (!file.exists()) {
                try {
                    FileInputStream fis = new FileInputStream(file);
                    BufferedReader r = new BufferedReader(new InputStreamReader(fis));

                    pid = Long.parseLong(r.readLine());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }


        } else {
            List<ProcessInfo> processes = systemInfo.getProcesses(piql);

            if (processes.size() == 1) {
                pid = processes.get(0).getPid();
            }
        }

        if (fullProcessTree) {
            return new AggregateProcessInfo(pid);
        } else {
            return  new ProcessInfo(pid);
        }


    }


    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) {
        if (process != null) {
            process.refresh();
            for (MeasurementScheduleRequest request : metrics) {
                if (request.getName().startsWith("Process.")) {

                    Object val = lookupAttributeProperty(process, request.getName().substring("Process.".length()));
                    if (val != null && val instanceof Number) {
                        report.addData(new MeasurementDataNumeric(request, ((Number) val).doubleValue()));
                    }
                }
            }
        }
    }


    protected Object lookupAttributeProperty(Object value, String property) {
        String[] ps = property.split("\\.", 2);

        String searchProperty = ps[0];

        // Try to use reflection
        try {
            PropertyDescriptor[] pds = Introspector.getBeanInfo(value.getClass()).getPropertyDescriptors();
            for (PropertyDescriptor pd : pds) {
                if (pd.getName().equals(searchProperty)) {
                    value = pd.getReadMethod().invoke(value);
                }
            }
        } catch (Exception e) {
//            log.debug("Unable to read property from measurement attribute [" + searchProperty + "] not found on ["
//                    + this.resourceContext.getResourceKey() + "]");
        }

        if (ps.length > 1) {
            value = lookupAttributeProperty(value, ps[1]);
        }

        return value;
    }

    public double getObjectProperty(Object object, String name) {
        try {
            BeanInfo info = Introspector.getBeanInfo(object.getClass());
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
                if (pd.getName().equals(name)) {
                    return ((Number) pd.getReadMethod().invoke(object)).doubleValue();
                }
            }
        } catch (Exception e) {
//            log.error("Error occurred while retrieving property '" + name + "' from object [" + object + "]", e);
        }

        return Double.NaN;
    }


}
