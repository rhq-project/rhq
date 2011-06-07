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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.unitils.thirdparty.org.apache.commons.io.FileUtils;

import org.rhq.plugins.apache.util.HttpdAddressUtility.Address;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class ApacheDeploymentUtil {

    private static final Log LOG = LogFactory.getLog(ApacheDeploymentUtil.class);
    
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
    }
    
    public static DeploymentConfig getDeploymentConfigurationFromSystemProperties(String variablesPrefix) {
        DeploymentConfig ret = new DeploymentConfig();
        
        ret.serverRoot = System.getProperty(variablesPrefix + ".server.root");
        ret.documentRoot = System.getProperty(variablesPrefix + ".document.root", "htdocs");
        ret.snmpHost = System.getProperty(variablesPrefix + ".snmp.host", "localhost");
        ret.snmpPort = Integer.parseInt(System.getProperty(variablesPrefix + ".snmp.port", "1610"));
        ret.mainServer.address1 = HttpdAddressUtility.parseListen(System.getProperty(variablesPrefix + ".listen1"));
        ret.mainServer.address2 = HttpdAddressUtility.parseListen(System.getProperty(variablesPrefix + ".listen2"));
        ret.mainServer.address3 = HttpdAddressUtility.parseListen(System.getProperty(variablesPrefix + ".listen3"));
        ret.mainServer.address4 = HttpdAddressUtility.parseListen(System.getProperty(variablesPrefix + ".listen4"));
        ret.mainServer.serverNameDirective = System.getProperty(variablesPrefix + ".servername.directive");
        
        String additionalDirectives = System.getProperty(variablesPrefix + ".additional.directives");
        fillAdditionalDirectives(additionalDirectives, ret.mainServer.additionalDirectives);
        
        readVHostConfigFromSystemProperties(ret.vhost1, variablesPrefix + ".vhost1");
        readVHostConfigFromSystemProperties(ret.vhost2, variablesPrefix + ".vhost2");
        readVHostConfigFromSystemProperties(ret.vhost3, variablesPrefix + ".vhost3");
        readVHostConfigFromSystemProperties(ret.vhost4, variablesPrefix + ".vhost4");

        return ret;
    }
    
    public static void deployConfiguration(File targetConfDirectory, List<String> configFilesOnClasspath, DeploymentConfig config) throws IOException {
        List<File> targetFiles = new ArrayList<File>();
        for(String fileOnClassPath : configFilesOnClasspath) {
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
        StringBuilder wholeFile = new StringBuilder();
        for(File file : configFiles) {
            BufferedReader rdr = null;
            boolean errorOccured = false;
            try {
                rdr = new BufferedReader(new FileReader(file));
                
                int cnt = -1;
              
                while ((cnt = rdr.read(buffer)) != -1) {
                    wholeFile.append(buffer, 0, cnt);
                }                
            } catch (FileNotFoundException e) {
                LOG.error("File '" + file + "' not found.", e); 
                errorOccured = true;
            } catch (IOException e) {
                LOG.error("Error while reading file '" + file + "'.", e);
                errorOccured = true;
            }

            if (rdr != null) {
                try {
                    rdr.close();
                } catch (IOException e) {
                    LOG.error("Failed to close the file '" + file + "'.");
                }
            }

            if (errorOccured) {
                continue;
            }
            
            String newFileContents = replaceTokens(wholeFile, config);
            
            Writer wrt = null;
            try {
                wrt = new FileWriter(file);
                
                wrt.write(newFileContents);
            } catch (IOException e) {
                LOG.error("Error while writing to the configuration file '" + file + "'.", e);
            } finally {
                try {
                    wrt.close();
                } catch (IOException e) {
                    LOG.error("Failed to close the file '" + file + "'.", e);
                }
            }
        }
    }

    private static String replaceTokens(CharSequence input, DeploymentConfig config) {
        String s = input.toString();
             
        s = s.replaceAll("\\$SERVER_ROOT", config.serverRoot);
        s = s.replaceAll("\\$DOCUMENT_ROOT", config.documentRoot);
        s = s.replaceAll("\\$SNMP_HOST", config.snmpHost);
        s = s.replaceAll("\\$SNMP_PORT", Integer.toString(config.snmpPort));
        s = s.replaceAll("\\$LISTEN_1", config.mainServer.address1.toString());
        s = s.replaceAll("\\$LISTEN_2", config.mainServer.address2.toString());
        s = s.replaceAll("\\$LISTEN_3", config.mainServer.address3.toString());
        s = s.replaceAll("\\$LISTEN_4", config.mainServer.address4.toString());
        s = s.replaceAll("\\$SNMP_HOST", config.snmpHost);
        
        s = replaceVHostTokens(s, 0, config.mainServer);
        s = replaceVHostTokens(s, 1, config.vhost1);
        s = replaceVHostTokens(s, 2, config.vhost2);
        s = replaceVHostTokens(s, 3, config.vhost3);
        s = replaceVHostTokens(s, 4, config.vhost4);
        
        return s;
    }
    
    private static String replaceVHostTokens(String input, int vhostNumber, DeploymentConfig.VHost vhost) {
        String serverNameToken = getVhostOrMainServerToken(vhostNumber, "SERVER_NAME_DIRECTIVE");
        String urlsToken = getVhostOrMainServerToken(vhostNumber, "URLS");
        String additionalDirectivesToken = getVhostOrMainServerToken(vhostNumber, "ADDITIONAL_DIRECTIVES");
        
        input = input.replaceAll(serverNameToken, vhost.serverNameDirective == null ? "" : vhost.serverNameDirective);
        
        if (vhostNumber > 0) {
            String urls = vhost.address1.toString(false, false);
            if (vhost.address2 != null) {
                urls += " " + vhost.address2.toString(false, false);
            }
            if (vhost.address3 != null) {
                urls += " " + vhost.address3.toString(false, false);
            }
            if (vhost.address4 != null) {
                urls += " " + vhost.address4.toString(false, false);
            }
            
            input = input.replaceAll(urlsToken, urls);
        }
        
        String additionalDirectives = "";
        if (!vhost.additionalDirectives.isEmpty()) {
            String newline = System.getProperty("line.separator");
            for(String dir : vhost.additionalDirectives) {
                additionalDirectives += dir + newline;
            }            
        }

        input = input.replaceAll(additionalDirectivesToken, additionalDirectives);

        return input;
    }
    
    private static String getVhostOrMainServerToken(int vhostNumber, String tokenBody) {
        return "\\$" + (vhostNumber == 0 ? "" : ("VHOST" + vhostNumber + "_")) + tokenBody;
    }
    
    private static void readVHostConfigFromSystemProperties(DeploymentConfig.VHost vhost, String prefix) {
        String addrsString = System.getProperty(prefix + ".urls");
        
        if (addrsString == null) {
            throw new IllegalStateException("The system property '" + prefix + ".urls' doesn't exist. It is needed to define the vhost.");
        }
        
        String[] addrs = addrsString.split("[ \t]+");
                
        //the fallthroughs below are intentional
        switch(addrs.length) {
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
            throw new IllegalStateException("The system property '" + prefix + ".urls' specified " + addrs.length + " addresses. Only 1-4 addresses are supported.");
        }
        
        vhost.serverNameDirective = System.getProperty(prefix + ".servername.directive");
        
        String additionalDirectives = System.getProperty(prefix + ".additional.directives");
        fillAdditionalDirectives(additionalDirectives, vhost.additionalDirectives);
    }
    
    private static void fillAdditionalDirectives(String additionalDirectivesString, List<String> additionalDirectives) {
        if (additionalDirectivesString != null) {
            for(String dir : additionalDirectivesString.split("\\n")) {
                additionalDirectives.add(dir);
            }
        }
    }
}
