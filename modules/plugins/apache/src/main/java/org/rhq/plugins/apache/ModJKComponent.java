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
package org.rhq.plugins.apache;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.apache.util.HttpdConfParser;

/**
 * Management of a mod_jk plugin in the parent apache server
 *
 * @author Heiko W. Rupp
 */
public class ModJKComponent implements ResourceComponent<ApacheServerComponent> {

    private static final Log log = LogFactory.getLog(ModJKComponent.class);

    private static final String OUTPUT_RESULT_PROP = "output";

    public void start(ResourceContext<ApacheServerComponent> parentResourceContext)
        throws InvalidPluginConfigurationException, Exception {

    }

    public void stop() {

    }

    public AvailabilityType getAvailability() {
        return AvailabilityType.UP; // For now we are always up when our parent is.
    }

    /**
     * Delegate method to install a simple mod_jk in an apache httpd
     * @param serverComponent The parents server component with the configuration
     * @param params Params we got passed from the GUI
     * @return The outcome of the operation
     */
    public static OperationResult installModJk(ApacheServerComponent serverComponent, Configuration params)
        throws Exception {

        StringBuilder builder = new StringBuilder();
        boolean errorSeen = false;
        boolean needWorkersProps = false;
        boolean needUriWorkers = false;

        // First see (what) if stuff is present
        File httpdConf = serverComponent.getServerConfiguration().getHttpdConfFile();
        String confPath = httpdConf.getAbsolutePath();

        // If we can't update the file, then there is nothing left to do.
        if (!httpdConf.canWrite()) {
            throw new Exception("Httpd.conf is not writable at " + confPath);
        }

        HttpdConfParser cparser = new HttpdConfParser();
        cparser.parse(confPath);
        // TODO back up original file
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(httpdConf, true));

            if (cparser.isModJkInstalled()) {
                builder.append("Mod_jk is already installed\n");
                if (cparser.getWorkerPropertiesFile() != null) {
                    builder.append("Found a worker.properties file at ").append(cparser.getWorkerPropertiesFile());
                    builder.append("\n");
                } else
                    needWorkersProps = true;

                if (cparser.getUriWorkerLocation() != null) {
                    builder.append("Found a urimap file at ").append(cparser.getUriWorkerLocation());
                } else
                    needUriWorkers = true;
            } else {
                builder.append("No mod_jk installed yet at ").append(confPath).append("\n");

                writer.append("LoadModule jk_module modules/mod_jk.so"); // TODO obtain modules location
                writer.newLine();

                builder.append(".. written a LoadModule line \n");
                needWorkersProps = true;
                needUriWorkers = true;
            }

            if (needWorkersProps) {
                writer.append("JkWorkersFile ").append("conf/workers.properties");
                writer.newLine();
                builder.append(".. installed worker.properties");
            }
            if (needUriWorkers) {
                writer.append("JkMountFile ").append("conf/uriworkermap");
                writer.newLine();
                builder.append(".. installed uriworkermap");
            }
            writer.flush();
            writer.close();

        } catch (IOException e) {
            builder.append("Error when installing mod_jk: \n");
            builder.append(e.fillInStackTrace());
            throw new Exception(builder.toString());

        }

        OperationResult result = new OperationResult();

        Configuration complexResults = result.getComplexResults();
        complexResults.put(new PropertySimple(OUTPUT_RESULT_PROP, builder.toString()));

        return result;
    }
}
