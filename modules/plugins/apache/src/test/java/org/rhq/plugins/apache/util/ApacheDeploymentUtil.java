/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.plugins.apache.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.unitils.thirdparty.org.apache.commons.io.FileUtils;

import org.rhq.plugins.apache.util.HttpdAddressUtility.Address;
import org.rhq.test.TokenReplacingProperties;
import org.rhq.test.TokenReplacingReader;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class ApacheDeploymentUtil {

    private static final Log LOG = LogFactory.getLog(ApacheDeploymentUtil.class);

    private static final String VHOST = "vhost";
    private static final String SERVER_ROOT = "server.root";
    private static final String ADDITIONAL_DIRECTIVES = "additional.directives";
    private static final String SERVERNAME_DIRECTIVE = "servername.directive";
    private static final String SERVERNAME = "servername";
    private static final String LISTEN4 = "listen4";
    private static final String LISTEN3 = "listen3";
    private static final String LISTEN2 = "listen2";
    private static final String LISTEN1 = "listen1";
    private static final String SNMP_PORT = "snmp.port";
    private static final String SNMP_HOST = "snmp.host";
    private static final String DOCUMENT_ROOT = "document.root";
    private static final String URLS = "urls";

    private ApacheDeploymentUtil() {

    }

    public static class DeploymentConfig {
        public static class VHost {
            public Address address1 = new Address(null, null, Address.NO_PORT_SPECIFIED_VALUE);
            public Address address2 = null;
            public Address address3 = null;
            public Address address4 = null;
            public String serverNameDirective = null;
            public final List<String> additionalDirectives = new ArrayList<String>();

            public String getServerName() {
                String serverName = null;
                if (serverNameDirective != null && serverNameDirective.startsWith("ServerName")) {
                    int startIdx = serverNameDirective.indexOf(' ');
                    if (startIdx >= 0) {
                        while (serverNameDirective.charAt(startIdx) == ' ') {
                            ++startIdx;
                        }

                        serverName = serverNameDirective.substring(startIdx);
                        serverName = serverName.trim();
                    }
                }

                return serverName;
            }

            public VHostSpec getVHostSpec() {
                VHostSpec ret = new VHostSpec();
                ret.serverName = getServerName();
                ret.hosts = getAddresses();
                return ret;
            }
            
            public List<String> getAddresses() {
                ArrayList<String> ret = new ArrayList<String>();
                if (address1 != null) {
                    ret.add(address1.toString());
                }
                if (address2 != null) {
                    ret.add(address2.toString());
                }
                if (address3 != null) {
                    ret.add(address3.toString());
                }
                if (address4 != null) {
                    ret.add(address4.toString());
                }

                return ret;
            }

            private void addToTokenReplacements(int ordinal, Map<String, String> tokenReplacements) {
                String prefix = null;

                if (ordinal == 0) {
                    prefix = "";
                } else {
                    prefix = VHOST + ordinal + ".";
                }

                tokenReplacements.put(prefix + SERVERNAME_DIRECTIVE, serverNameDirective == null ? ""
                    : serverNameDirective);

                String serverName = getServerName();
                if (serverName != null) {
                    tokenReplacements.put(prefix + SERVERNAME, serverName);
                }

                String dirs = "";
                if (!additionalDirectives.isEmpty()) {
                    String newline = System.getProperty("line.separator");
                    for (String dir : additionalDirectives) {
                        dirs += dir + newline;
                    }
                }

                tokenReplacements.put(prefix + ADDITIONAL_DIRECTIVES, dirs);

                if (ordinal != 0) {
                    String urls = address1.toString(false, false);
                    if (address2 != null) {
                        urls += " " + address2.toString(false, false);
                    }
                    if (address3 != null) {
                        urls += " " + address3.toString(false, false);
                    }
                    if (address4 != null) {
                        urls += " " + address4.toString(false, false);
                    }

                    tokenReplacements.put(prefix + URLS, urls);
                } else {
                    tokenReplacements.put(LISTEN1, address1 == null ? "" : address1.toString(false, false));
                    tokenReplacements.put(LISTEN2, address2 == null ? "" : address2.toString(false, false));
                    tokenReplacements.put(LISTEN3, address3 == null ? "" : address3.toString(false, false));
                    tokenReplacements.put(LISTEN4, address4 == null ? "" : address4.toString(false, false));
                }
            }
        }

        public String serverRoot = null;
        public String documentRoot = "hdocs";
        public String snmpHost = "localhost";
        public int snmpPort = 1610;
        public final VHost mainServer = new VHost();
        public final VHost vhost1 = new VHost();
        public final VHost vhost2 = new VHost();
        public final VHost vhost3 = new VHost();
        public final VHost vhost4 = new VHost();

        {
            mainServer.address2 = new Address(null, null, Address.NO_PORT_SPECIFIED_VALUE);
            mainServer.address3 = new Address(null, null, Address.NO_PORT_SPECIFIED_VALUE);
            mainServer.address4 = new Address(null, null, Address.NO_PORT_SPECIFIED_VALUE);
        }

        public Map<String, String> getTokenReplacements() {
            HashMap<String, String> ret = new HashMap<String, String>();

            ret.put(SERVER_ROOT, serverRoot == null ? "" : serverRoot);
            ret.put(DOCUMENT_ROOT, documentRoot == null ? "" : documentRoot);
            ret.put(SNMP_HOST, snmpHost == null ? "" : snmpHost);
            ret.put(SNMP_PORT, Integer.toString(snmpPort));
            mainServer.addToTokenReplacements(0, ret);
            vhost1.addToTokenReplacements(1, ret);
            vhost2.addToTokenReplacements(2, ret);
            vhost3.addToTokenReplacements(3, ret);
            vhost4.addToTokenReplacements(4, ret);

            return ret;
        }
    }

    public static void addDefaultVariables(Map<String, String> variables, String prefix) {
        InetAddress localhost = determineLocalhost();
        checkOrAddDefault(variables, "localhost", localhost.getHostName());
        checkOrAddDefault(variables, "localhost.ip", localhost.getHostAddress());
        checkOrAddDefault(variables, "unresolvable.host", "unreachable.host.com");
        checkOrAddDefault(variables, "port1", "11675");
        checkOrAddDefault(variables, "port2", "11676");
        checkOrAddDefault(variables, "port3", "11677");
        checkOrAddDefault(variables, "port4", "11678");

        if (prefix != null && !prefix.trim().isEmpty()) {
            prefix += ".";
        } else {
            prefix = "";
        }
        
        checkOrAddDefault(variables, prefix + "listen1", "${port1}");
        checkOrAddDefault(variables, prefix + "listen2", "${port2}");
        checkOrAddDefault(variables, prefix + "listen3", "${port3}");
        checkOrAddDefault(variables, prefix + "listen4", "${port4}");

        checkOrAddDefault(variables, prefix + "vhost1.servername", "${localhost}:${port1}");
        checkOrAddDefault(variables, prefix + "vhost1.urls", "${" + prefix + "vhost1.servername}");
        checkOrAddDefault(variables, prefix + "vhost1.servername.directive", "ServerName ${" + prefix
            + "vhost1.servername}");

        checkOrAddDefault(variables, prefix + "vhost2.servername", "${localhost}:${port2}");
        checkOrAddDefault(variables, prefix + "vhost2.urls", "${" + prefix + "vhost2.servername}");
        checkOrAddDefault(variables, prefix + "vhost2.servername.directive", "ServerName ${" + prefix
            + "vhost2.servername}");

        checkOrAddDefault(variables, prefix + "vhost3.servername", "${localhost}:${port3}");
        checkOrAddDefault(variables, prefix + "vhost3.urls", "${" + prefix + "vhost3.servername}");
        checkOrAddDefault(variables, prefix + "vhost3.servername.directive", "ServerName ${" + prefix
            + "vhost3.servername}");

        checkOrAddDefault(variables, prefix + "vhost4.servername", "${localhost}:${port4}");
        checkOrAddDefault(variables, prefix + "vhost4.urls", "${" + prefix + "vhost4.servername}");
        checkOrAddDefault(variables, prefix + "vhost4.servername.directive", "ServerName ${" + prefix
            + "vhost4.servername}");
    }

    private static void checkOrAddDefault(Map<String, String> map, String key, String value) {
        if (!map.containsKey(key)) {
            map.put(key, value);
        }
    }
    
    public static DeploymentConfig getDeploymentConfigurationFromSystemProperties(String variablesPrefix, Map<String, String> defaultOverrides) {
        DeploymentConfig ret = new DeploymentConfig();

        TokenReplacingProperties properties = new TokenReplacingProperties(defaultOverrides);
        addDefaultVariables(properties, variablesPrefix);
        properties.putAll(System.getProperties());

        variablesPrefix += ".";

        ret.serverRoot = properties.get(variablesPrefix + SERVER_ROOT);
        ret.documentRoot = properties.get(variablesPrefix + DOCUMENT_ROOT);
        ret.documentRoot = ret.documentRoot == null ? "htdocs" : ret.documentRoot;
        ret.snmpHost = properties.get(variablesPrefix + SNMP_HOST);
        ret.snmpHost = ret.snmpHost == null ? "localhost" : ret.snmpHost;
        String snmpPort = properties.get(variablesPrefix + SNMP_PORT);
        snmpPort = snmpPort == null ? "1610" : snmpPort;
        ret.snmpPort = Integer.parseInt(snmpPort);
        ret.mainServer.address1 = HttpdAddressUtility.parseListen(properties.get(variablesPrefix + LISTEN1));
        ret.mainServer.address2 = HttpdAddressUtility.parseListen(properties.get(variablesPrefix + LISTEN2));
        ret.mainServer.address3 = HttpdAddressUtility.parseListen(properties.get(variablesPrefix + LISTEN3));
        ret.mainServer.address4 = HttpdAddressUtility.parseListen(properties.get(variablesPrefix + LISTEN4));
        ret.mainServer.serverNameDirective = properties.get(variablesPrefix + SERVERNAME_DIRECTIVE);

        String additionalDirectives = properties.get(variablesPrefix + ADDITIONAL_DIRECTIVES);
        fillAdditionalDirectives(additionalDirectives, ret.mainServer.additionalDirectives);

        readVHostConfigFromProperties(ret.vhost1, variablesPrefix + VHOST + 1, properties);
        readVHostConfigFromProperties(ret.vhost2, variablesPrefix + VHOST + 2, properties);
        readVHostConfigFromProperties(ret.vhost3, variablesPrefix + VHOST + 3, properties);
        readVHostConfigFromProperties(ret.vhost4, variablesPrefix + VHOST + 4, properties);

        return ret;
    }

    public static void deployConfiguration(File targetConfDirectory, Collection<String> configFilesOnClasspath,
        DeploymentConfig config) throws IOException {
        List<File> targetFiles = new ArrayList<File>();
        for (String fileOnClassPath : configFilesOnClasspath) {
            String fileName = new File(fileOnClassPath).getName();

            File targetFile = new File(targetConfDirectory, fileName);

            URL fileUrl = ApacheDeploymentUtil.class.getResource(fileOnClassPath);

            FileUtils.copyURLToFile(fileUrl, targetFile);

            targetFiles.add(targetFile);
        }

        replaceTokensInConfigFiles(targetFiles, config);
    }

    private static void replaceTokensInConfigFiles(List<File> configFiles, DeploymentConfig config) {
        char[] buffer = new char[8192];

        Map<String, String> replacements = config.getTokenReplacements();

        for (File file : configFiles) {
            TokenReplacingReader rdr = null;
            FileWriter wrt = null;

            try {
                rdr = new TokenReplacingReader(new FileReader(file), replacements);

                File tmp = File.createTempFile("apache-deployment-util", null);

                wrt = new FileWriter(tmp);

                int cnt = -1;

                while ((cnt = rdr.read(buffer)) != -1) {
                    wrt.write(buffer, 0, cnt);
                }

                wrt.close();

                tmp.renameTo(file);
            } catch (IOException e) {
                LOG.error("Error while replacing the tokens in file '" + file + "'.", e);
            }
        }
    }

    private static void readVHostConfigFromProperties(DeploymentConfig.VHost vhost, String prefix,
        Map<String, String> properties) {
        prefix += ".";

        String addrsString = properties.get(prefix + URLS);

        if (addrsString == null) {
            throw new IllegalStateException("The system property '" + prefix
                + "urls' doesn't exist. It is needed to define the vhost.");
        }

        String[] addrs = addrsString.split("[ \t]+");

        //the fallthroughs below are intentional
        switch (addrs.length) {
        case 4:
            vhost.address4 = Address.parse(addrs[3], null);
        case 3:
            vhost.address3 = Address.parse(addrs[2], null);
        case 2:
            vhost.address2 = Address.parse(addrs[1], null);
        case 1:
            vhost.address1 = Address.parse(addrs[0], null);
            break;
        default:
            throw new IllegalStateException("The system property '" + prefix + "urls' specified " + addrs.length
                + " addresses. Only 1-4 addresses are supported.");
        }

        vhost.serverNameDirective = properties.get(prefix + SERVERNAME_DIRECTIVE);

        String additionalDirectives = properties.get(prefix + ADDITIONAL_DIRECTIVES);
        fillAdditionalDirectives(additionalDirectives, vhost.additionalDirectives);
    }

    private static void fillAdditionalDirectives(String additionalDirectivesString, List<String> additionalDirectives) {
        if (additionalDirectivesString != null) {
            for (String dir : additionalDirectivesString.split("\\n")) {
                additionalDirectives.add(dir);
            }
        }
    }

    private static InetAddress determineLocalhost() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            try {
                return InetAddress.getByName("127.0.0.1");
            } catch (UnknownHostException u) {
                //doesn't happen
                return null;
            }
        }
    };
}
