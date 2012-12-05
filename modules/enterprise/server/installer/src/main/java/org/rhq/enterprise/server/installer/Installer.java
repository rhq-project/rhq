/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.server.installer;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.installer.InstallerService.AlreadyInstalledException;
import org.rhq.enterprise.server.installer.InstallerService.AutoInstallDisabledException;

/**
 * The entry point to the RHQ Installer.
 *
 * @author John Mazzitelli
 */
public class Installer {
    private static final Log LOG = LogFactory.getLog(Installer.class);

    private static final int EXIT_CODE_ALREADY_INSTALLED = 0;
    private static final int EXIT_CODE_INSTALLATION_DONE = 0;
    private static final int EXIT_CODE_AUTOINSTALL_DISABLED = 1;
    private static final int EXIT_CODE_INSTALLATION_ERROR = 2;

    private InstallerConfiguration installerConfig;

    private enum WhatToDo {
        DISPLAY_USAGE, TEST, LIST_SERVERS, INSTALL
    }

    public static void main(String[] args) {
        try {
            LOG.info("Starting installer...");
            final Installer installer = new Installer();
            installer.doInstall(args);
        } catch (Exception e) {
            LOG.error("An error occurred", e);
            System.exit(EXIT_CODE_INSTALLATION_ERROR);
        }

        System.exit(EXIT_CODE_INSTALLATION_DONE);
    }

    public Installer() {
        this.installerConfig = new InstallerConfiguration();
    }

    public InstallerConfiguration getInstallerConfiguration() {
        return this.installerConfig;
    }

    public void doInstall(String[] args) throws Exception {

        WhatToDo[] thingsToDo = processArguments(args);

        for (WhatToDo whatToDo : thingsToDo) {
            switch (whatToDo) {
            case DISPLAY_USAGE: {
                displayUsage();
                continue;
            }
            case LIST_SERVERS: {
                new InstallerServiceImpl(installerConfig).listServers();
                continue;
            }
            case TEST: {
                try {
                    new InstallerServiceImpl(installerConfig).test();
                } catch (AutoInstallDisabledException e) {
                    LOG.error(e.getMessage());
                    System.exit(EXIT_CODE_AUTOINSTALL_DISABLED);
                } catch (AlreadyInstalledException e) {
                    LOG.info(e.getMessage());
                    System.exit(EXIT_CODE_ALREADY_INSTALLED);
                }
                continue;
            }
            case INSTALL: {
                try {
                    final InstallerService installerService = new InstallerServiceImpl(installerConfig);
                    final HashMap<String, String> serverProperties = installerService.preInstall();
                    installerService.install(serverProperties, null, null);
                    LOG.info("Installation is complete. The server should be ready shortly.");
                } catch (AutoInstallDisabledException e) {
                    LOG.error(e.getMessage());
                    System.exit(EXIT_CODE_AUTOINSTALL_DISABLED);
                } catch (AlreadyInstalledException e) {
                    LOG.info(e.getMessage());
                    System.exit(EXIT_CODE_ALREADY_INSTALLED);
                }
                continue;
            }
            default: {
                throw new IllegalStateException("Please report this bug: " + whatToDo);
            }
            }
        }

        return;
    }

    private void displayUsage() {
        StringBuilder usage = new StringBuilder("RHQ Installer\n");
        usage.append("\t--help, -H: this help text").append("\n");
        usage.append("\t-Dname=value: set system properties for the Installer VM").append("\n");
        usage.append("\t--host=<hostname>, -h: hostname where the app server is running").append("\n");
        usage.append("\t--port=<port>, -p: talk to the app server over this management port").append("\n");
        usage.append("\t--test, -t: test the validity of the server properties (install not performed)").append("\n");
        usage.append("\t--listservers, -l: show list of known installed servers (install not performed)").append("\n");
        LOG.info(usage);
    }

    private WhatToDo[] processArguments(String[] args) throws Exception {
        String sopts = "-:HD:h:p:lt";
        LongOpt[] lopts = { new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'H'),
            new LongOpt("host", LongOpt.REQUIRED_ARGUMENT, null, 'h'),
            new LongOpt("port", LongOpt.REQUIRED_ARGUMENT, null, 'p'),
            new LongOpt("listservers", LongOpt.NO_ARGUMENT, null, 'l'),
            new LongOpt("test", LongOpt.NO_ARGUMENT, null, 't') };

        boolean test = false;
        boolean listservers = false;

        Getopt getopt = new Getopt("installer", args, sopts, lopts);
        int code;

        while ((code = getopt.getopt()) != -1) {
            switch (code) {
            case ':':
            case '?': {
                // for now both of these should exit
                displayUsage();
                throw new IllegalArgumentException();
            }

            case 1: {
                // this will catch non-option arguments (which we don't currently support)
                throw new IllegalArgumentException("Unknown option: " + getopt.getOptarg());
            }

            case 'H': {
                return new WhatToDo[] { WhatToDo.DISPLAY_USAGE };
            }

            case 'D': {
                // set a system property
                String sysprop = getopt.getOptarg();
                int i = sysprop.indexOf("=");
                String name;
                String value;

                if (i == -1) {
                    name = sysprop;
                    value = "true";
                } else {
                    name = sysprop.substring(0, i);
                    value = sysprop.substring(i + 1, sysprop.length());
                }

                System.setProperty(name, value);
                LOG.info("System property set: " + name + "=" + value);

                break;
            }

            case 'h': {
                String hostString = getopt.getOptarg();
                if (hostString == null) {
                    throw new IllegalArgumentException("Missing host value");
                }
                this.installerConfig.setManagementHost(hostString);
                break;
            }

            case 'p': {
                String portString = getopt.getOptarg();
                if (portString == null) {
                    throw new IllegalArgumentException("Missing port value");
                }
                this.installerConfig.setManagementPort(Integer.parseInt(portString));
                break;
            }

            case 'l': {
                listservers = true;
                break; // don't return, we need to allow more args to be processed, like -p or -h
            }

            case 't': {
                test = true;
                break; // don't return, we need to allow more args to be processed, like -p or -h
            }
            }
        }

        if (test || listservers) {
            ArrayList<WhatToDo> whatToDo = new ArrayList<WhatToDo>();
            if (test) {
                whatToDo.add(WhatToDo.TEST);
            }
            if (listservers) {
                whatToDo.add(WhatToDo.LIST_SERVERS);
            }
            return whatToDo.toArray(new WhatToDo[whatToDo.size()]);
        }

        return new WhatToDo[] { WhatToDo.INSTALL };
    }
}
