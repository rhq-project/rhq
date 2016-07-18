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

import java.io.Console;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.crypto.CryptoUtil;

import org.rhq.core.util.exception.ThrowableUtil;
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
        DISPLAY_USAGE, DO_NOTHING, TEST, SETUPDB, LIST_SERVERS, INSTALL, UPDATESTORAGESCHEMA, CLEARCOLUMNFAMILIES, LIST_VERSIONS, UPGRADE
    }

    public static void main(String[] args) {
        try {
            final Installer installer = new Installer();
            installer.doInstall(args);
        } catch (Exception e) {
            LOG.error("The installer will now exit due to previous errors", e);
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
            case LIST_VERSIONS: {
                new InstallerServiceImpl(installerConfig).listVersions();
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
            case SETUPDB: {
                try {
                    final InstallerService installerService = new InstallerServiceImpl(installerConfig);
                    final HashMap<String, String> serverProperties = installerService.getServerProperties();
                    installerService.prepareDatabase(serverProperties, null, null, false);
                    LOG.info("Database setup is complete.");
                } catch (Exception e) {
                    LOG.error(ThrowableUtil.getAllMessages(e));
                    System.exit(EXIT_CODE_INSTALLATION_ERROR);
                }
                continue;
            }
            case CLEARCOLUMNFAMILIES: {
                try {
                    final InstallerService installerService = new InstallerServiceImpl(installerConfig);
                    final HashMap<String, String> serverProperties = installerService.getServerProperties();
                    installerService.clearColumnFamilies(serverProperties);
                    LOG.info("Clearing unused column families from storage inventory is complete.");
                } catch (Exception e) {
                    LOG.error(ThrowableUtil.getAllMessages(e));
                    System.exit(EXIT_CODE_INSTALLATION_ERROR);
                }
                continue;
            }
            case UPDATESTORAGESCHEMA: {
                try {
                    final InstallerService installerService = new InstallerServiceImpl(installerConfig);
                    final HashMap<String, String> serverProperties = installerService.getServerProperties();
                    installerService.updateStorageSchema(serverProperties);
                    LOG.info("Storage schema update is complete.");
                } catch (Exception e) {
                    LOG.error(ThrowableUtil.getAllMessages(e));
                    System.exit(EXIT_CODE_INSTALLATION_ERROR);
                }
                continue;
            }
            case INSTALL:
            case UPGRADE: {
                try {
                    final InstallerService installerService = new InstallerServiceImpl(installerConfig);
                    final HashMap<String, String> serverProperties = installerService.preInstall();
                    installerService.install(serverProperties, null, null, (WhatToDo.UPGRADE == whatToDo));
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
            case DO_NOTHING: {
                continue; // this will occur if processArguments() already did the work
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
        usage.append("\t--force, -f: force the installer to try to install everything").append("\n");
        usage.append("\t--listservers, -l: show list of known installed servers (install not performed)").append("\n");
        usage.append("\t--listversions, -v: show list of server and storage node versions (install not performed)")
            .append("\n");
        usage.append("\t--setupdb, -b: only perform database schema creation or update").append("\n");
        usage.append("\t--updatestorageschema, -u: only perform storage cluster schema update").append("\n");
        usage.append("\t--upgrade, -g: this is an upgrade installation (as opposed to a new install)").append("\n");
        usage
            .append("\t--encodevalue, -e: prompts for password or value to encode for editing configuration files for agent or server");
        usage.append("\n");
        LOG.info(usage);
    }

    private WhatToDo[] processArguments(String[] args) throws Exception {
        String sopts = "-:HD:h:p:e:buflt";
        LongOpt[] lopts = { new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'H'),
            new LongOpt("host", LongOpt.REQUIRED_ARGUMENT, null, 'h'),
            new LongOpt("port", LongOpt.REQUIRED_ARGUMENT, null, 'p'),
            new LongOpt("encodevalue", LongOpt.NO_ARGUMENT, null, 'e'),
            new LongOpt("setupdb", LongOpt.NO_ARGUMENT, null, 'b'),
            new LongOpt("updatestorageschema", LongOpt.NO_ARGUMENT, null, 'u'),
            new LongOpt("clearcolumnfamilies", LongOpt.NO_ARGUMENT, null, 'c'),
            new LongOpt("upgrade", LongOpt.NO_ARGUMENT, null, 'g'),
            new LongOpt("listservers", LongOpt.NO_ARGUMENT, null, 'l'),
            new LongOpt("listversions", LongOpt.NO_ARGUMENT, null, 'v'),
            new LongOpt("force", LongOpt.NO_ARGUMENT, null, 'f'), new LongOpt("test", LongOpt.NO_ARGUMENT, null, 't') };

        boolean test = false;
        boolean listservers = false;
        boolean listversions = false;
        boolean setupdb = false;
        boolean upgrade = false;
        boolean clearcolumnfamilies = false;
        boolean updatestorage = false;
        String valueToEncode = null;
        String associatedProperty = null;

        Getopt getopt = new Getopt("installer", args, sopts, lopts);
        int code;

        while ((code = getopt.getopt()) != -1) {
            switch (code) {
            case ':':
            case '?': {
                // for now both of these should exit
                LOG.error("Invalid option");
                return new WhatToDo[] { WhatToDo.DISPLAY_USAGE };
            }

            case 1: {
                // this will catch non-option arguments (which we don't currently support)
                LOG.error("Unknown option: " + getopt.getOptarg());
                return new WhatToDo[] { WhatToDo.DISPLAY_USAGE };
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

            case 'e': {
                // Prompt for the property and value to be encoded.
                // Don't use a command line option because the plain text password
                // could get captured in command history.
                Console console = System.console();
                if (null != console) {
                    associatedProperty = "rhq.autoinstall.server.admin.password";
                    if (!confirm(console, "Property " + associatedProperty)) {
                        associatedProperty = "rhq.server.database.password";
                        if (!confirm(console, "Property " + associatedProperty)) {
                            associatedProperty = ask(console, "Property: ");
                        }
                    }

                    String prompt = "Value: ";
                    if (associatedProperty != null && associatedProperty.toLowerCase().contains("password")) {
                        prompt = "Password: ";
                    }

                    valueToEncode = String.valueOf(console.readLine("%s", prompt));
                } else {
                    LOG.error("NO CONSOLE!");
                }

                break;
            }

            case 'b': {
                setupdb = true;
                break; // don't return, in case we need to allow more args
            }

            case 'g': {
                upgrade = true;
                break; // don't return, in case we need to allow more args
            }

            case 'u': {
                updatestorage = true;
                break; // don't return, in case we need to allow more args
            }

            case 'c': {
                clearcolumnfamilies = true;
                break;
            }

            case 'f': {
                this.installerConfig.setForceInstall(true);
                break; // don't return, in case we need to allow more args
            }

            case 'l': {
                listservers = true;
                break; // don't return, we need to allow more args to be processed, like -p or -h
            }

            case 't': {
                test = true;
                break; // don't return, we need to allow more args to be processed, like -p or -h
            }

            case 'v': {
                listversions = true;
                break; // don't return, we need to allow more args to be processed, like -p or -h
            }
            }
        }

        // if value encoding was asked, that's all we do on the execution
        if (valueToEncode != null) {
            String encodedValue;
            if ("rhq.autoinstall.server.admin.password".equals(associatedProperty)) {
                encodedValue = CryptoUtil.createPasswordHash("MD5", CryptoUtil.BASE64_ENCODING, null, null,
                    valueToEncode);
            } else {
                encodedValue = new InstallerServiceImpl(installerConfig).obfuscatePassword(String
                    .valueOf(valueToEncode));
            }

            System.out.println("     ");
            System.out.println("     ");

            if ("rhq.server.database.password".equals(associatedProperty)
                || "rhq.autoinstall.server.admin.password".equals(associatedProperty)
                || "rhq.storage.password".equals(associatedProperty)) {
                System.out.println("Encoded password for rhq-server.properties:");
                System.out.println("     " + associatedProperty + "=" + encodedValue);
                System.out.println("     ");
            } else {
                String prompt = "value";
                if (associatedProperty != null && associatedProperty.toLowerCase().contains("password")) {
                    prompt = "password";
                }

                System.out.println("!!! WARNING !!!");
                System.out
                    .println("Both standalone-full.xml and rhq-server.properties need to be updated if a property from rhq-server.properties is used in standalone-full.xml");
                System.out.println("!!! WARNING !!!");
                System.out.println("     ");
                System.out.println("Encoded " + prompt + " for rhq-server.properties:");
                System.out.println("     " + associatedProperty + "=RESTRICTED::" + encodedValue);
                System.out.println("     ");
                System.out.println("Encoded " + prompt + " for standalone-full.xml with selected " + prompt
                    + " as default:");
                System.out.println("     ${VAULT::restricted::" + associatedProperty + "::" + encodedValue + "}");
                System.out.println("     ");
                System.out.println("Encoded " + prompt + " for standalone-full.xml without default:");
                System.out.println("     ${VAULT::restricted::" + associatedProperty + ":: }");
                System.out.println("     ");
                System.out.println("Encoded " + prompt + " for agent-configuration.xml:");
                System.out.println("     <entry key=\"" + associatedProperty + "\" value=\"RESTRICTED::" + encodedValue
                    + "\" />");
                System.out.println("     ");
            }

            System.out.println("Please consult the documentation for additional help.");
            System.out.println("     ");

            return new WhatToDo[] { WhatToDo.DO_NOTHING };
        }

        if (test || setupdb || updatestorage || listservers || listversions || clearcolumnfamilies) {
            ArrayList<WhatToDo> whatToDo = new ArrayList<WhatToDo>();
            if (test) {
                whatToDo.add(WhatToDo.TEST);
            }
            if (setupdb) {
                whatToDo.add(WhatToDo.SETUPDB);
            }
            if (updatestorage) {
                whatToDo.add(WhatToDo.UPDATESTORAGESCHEMA);
            }
            if(clearcolumnfamilies) {
                whatToDo.add(WhatToDo.CLEARCOLUMNFAMILIES);
            }
            if (listservers) {
                whatToDo.add(WhatToDo.LIST_SERVERS);
            }
            if (listversions) {
                whatToDo.add(WhatToDo.LIST_VERSIONS);
            }
            return whatToDo.toArray(new WhatToDo[whatToDo.size()]);
        }

        return new WhatToDo[] { (upgrade ? WhatToDo.UPGRADE : WhatToDo.INSTALL) };
    }

    private String ask(Console console, String prompt) {
        String response = "";
        do {
            response = String.valueOf(console.readLine("%s", prompt).trim());
        } while (response.isEmpty());

        return response;
    }

    private boolean confirm(Console console, String option) {
        String response = "";
        do {
            response = ask(console, option + " [y/n]: ").trim().toLowerCase();
        } while (!(response.startsWith("y") || response.startsWith("n")));

        return response.startsWith("y");
    }
}
