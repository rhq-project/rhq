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
package org.rhq.plugins.hardware;

import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;

import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;
import java.io.StringReader;
import java.io.BufferedReader;

/**
 * @author Greg Hinkle
 */
public class SmartDiskDiscoveryComponent implements ResourceDiscoveryComponent {

    private static String[] DRIVE_NAMES = {
            "/dev/hda", "/dev/hdb", "/dev/hdc", "/dev/hdd",
            "/dev/sda", "/dev/sdb", "/dev/sdc", "/dev/sdd",
            "/dev/disk0", "/dev/disk1", "/dev/disk2", "/dev/disk3"
    };

    public Set discoverResources(ResourceDiscoveryContext context) throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> found = new HashSet<DiscoveredResourceDetails>();
        File smartctl = getSmartctl();
        if (smartctl != null) {

            for (String driveName : DRIVE_NAMES) {

                ProcessExecution proc = ProcessExecutionUtility.createProcessExecution("/usr/bin/sudo", smartctl.getAbsoluteFile());
                //new ProcessExecution("/usr/bin/sudo"); //smartctl.getAbsolutePath());
                proc.setArguments(new String[]{smartctl.getAbsolutePath(), "-i", driveName});
                proc.setCaptureOutput(true);
                proc.setWaitForCompletion(4000);
                ProcessExecutionResults results = context.getSystemInformation().executeProcess(proc);

                StringReader r = new StringReader(results.getCapturedOutput());
                BufferedReader br = new BufferedReader(r);
                String line = null;
                try {
                    String model = null;
                    Pattern p = Pattern.compile("^SMART support is\\:\\s*Enabled$");
                    Pattern p2 = Pattern.compile("^Device Model\\:\\s*(.*)$");
                    while ((line = br.readLine()) != null) {
                        Matcher m = p2.matcher(line);
                        if (m.matches()) {
                            model = m.group(1);
                        }


                        m = p.matcher(line);
                        if (m.matches()) {
                            Configuration pluginConfig = context.getDefaultPluginConfiguration();
                            pluginConfig.put(new PropertySimple("prefix", "/usr/bin/sudo"));
                            pluginConfig.put(new PropertySimple("command", smartctl.getAbsolutePath()));

                            DiscoveredResourceDetails detail =
                                    new DiscoveredResourceDetails(
                                            context.getResourceType(),
                                            driveName,
                                            driveName,
                                            null,
                                            model,
                                            pluginConfig,
                                            null);
                            found.add(detail);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


        }

        return found;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public static File getSmartctl() {
        File f = new File("/usr/sbin/smartctl");
        if (!f.exists()) {
            f = new File("/usr/local/sbin/smartctl");
        }

        if (f.exists()) {
            return f;
        }
        return null;

    }

    public static void main(String[] args) throws Exception {
        new SmartDiskDiscoveryComponent().discoverResources(null);
    }
}
