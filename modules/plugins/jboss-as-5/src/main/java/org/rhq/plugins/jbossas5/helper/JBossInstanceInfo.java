 /*
  * Jopr Management Platform
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
package org.rhq.plugins.jbossas5.helper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Properties;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.util.StringPropertyReplacer;

import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.jbossas5.util.JBossConfigurationUtility;

 /**
 * Information about a running JBossAS process. System properties, other JVM arguments, JBoss properties, and other
  * JBossAS arguments are parsed as <code>org.jboss.Main</code> would parse them, and exposed via getters.
  *
  * @author Ian Springer
  */
 public class JBossInstanceInfo
 {
     private static final String JBOSS_MAIN_CLASS_NAME = "org.jboss.Main";

     private static final String ANY_ADDRESS = "0.0.0.0";

     private final Log log = LogFactory.getLog(this.getClass());

     private ProcessInfo processInfo;
     private Properties sysProps = new Properties();
     private String[] classPath;
     private JBossInstallationInfo installInfo;

     /**
      * Parses the command line of a JBossAS process.
      *
      * @param processInfo the process info for the JBossAS process
      * @throws Exception if the process does not appear to be a JBossAS process or if the JBoss home dir cannot be
      *                   determined
      */
     public JBossInstanceInfo(ProcessInfo processInfo) throws Exception {
         log.debug("Parsing JBossAS command line " + Arrays.asList(processInfo.getCommandLine()) + "...");
         this.processInfo = processInfo;
         int i;
         String[] args = this.processInfo.getCommandLine();
         for (i = 0; i < args.length; i++) {
             if (args[i].equals(JBOSS_MAIN_CLASS_NAME)) {
                 break;
             }
         }
         String[] jvmArgs = new String[i + 1];
         System.arraycopy(args, 0, jvmArgs, 0, i + 1);
         processJvmArgs(jvmArgs);
         String[] jbossArgs = new String[args.length - jvmArgs.length];
         System.arraycopy(args, i + 1, jbossArgs, 0, jbossArgs.length);
         processJBossArgs(jbossArgs);
         finalizeSysProps();
         printSysProps();
     }

     public Properties getSystemProperties() {
         return this.sysProps;
     }

     public JBossInstallationInfo getInstallInfo() {
         return this.installInfo;
     }

     private void processJvmArgs(String args[])
             throws Exception {
         // NOTE: We can't use GNU GetOpt to process the JVM options, because they are not GNU-style options.
         for (int i = 0; i < args.length; i++) {
             String arg = args[i];
             if (arg.equals("-cp") || arg.equals("-classpath")) {
                 if (i == args.length - 1) {
                     log.error("'" + arg + "' option has no value.");
                     continue;
                 }
                 this.classPath = args[i + 1].split(File.pathSeparator);
             }
             else if (arg.startsWith("-D")) {
                 addPropArgToProps(arg.substring("-D".length()), this.sysProps);
             }
         }
     }

     private void processJBossArgs(String args[]) {
         String programName = this.sysProps.getProperty("program.name", "jboss");
         String shortOpts = "-:b:c:D:P:";
         LongOpt longOpts[] = {
                 new LongOpt("configuration", 1, null, 'c'),
                 new LongOpt("properties", 1, null, 'P'),
                 new LongOpt("host", 1, null, 'b'),
         };
         Getopt options = new Getopt(programName, args, shortOpts, longOpts);
         // Tell Getopt not to complain to stderr about unrecognized options...
         options.setOpterr(false);
         int c;
         while ((c = options.getopt()) != -1) {
             switch (c) {
                 case 'b': // 'b' (--host)
                 {
                     String arg = options.getOptarg();
                     this.sysProps.setProperty(JBossProperties.BIND_ADDRESS, arg);
                     break;
                 }

                 case 'c': // 'c' (--configuration)
                 {
                     String arg = options.getOptarg();
                     this.sysProps.setProperty(JBossProperties.SERVER_NAME, arg);
                     break;
                 }

                 case 'D': // 'D'
                 {
                     String arg = options.getOptarg();
                     String name = addPropArgToProps(arg, this.sysProps);
                     String value = this.sysProps.getProperty(name);
                     if (value.equals("")) {
                         // unlike the JVM, org.jboss.Main interprets -Dfoo as foo="true", rather than as foo=""
                         this.sysProps.setProperty(name, Boolean.TRUE.toString());
                     }
                     break;
                 }

                 case 'P': // 'P' (--properties)
                 {
                     String arg = options.getOptarg();
                     URL url;
                     try {
                         File currentWorkingDir; // e.g. /jon/dev-container/jbossas/bin
                         currentWorkingDir = new File(getHomeDir(), "bin");
                         url = JBossConfigurationUtility.makeURL(arg, currentWorkingDir);
                     }
                     catch (Exception e) {
                         log.error("Failed to parse argument to --properties option: " + options.getOptarg());
                         break;
                     }
                     Properties props = new Properties();
                     InputStream inputStream = null;
                     try {
                         inputStream = new BufferedInputStream(url.openConnection().getInputStream());
                         props.load(inputStream);
                     }
                     catch (IOException e) {
                         log.error("Could not read properties from file: " + arg, e);
                         break;
                     }
                     finally {
                         if (inputStream != null) {
                             try {
                                 inputStream.close();
                             }
                             catch (IOException e) {
                                 log.error("Failed to close properties file: " + arg, e);
                                 // not fatal - continue processing...
                             }
                         }
                     }
                     for (Object nameObj : props.keySet()) {
                         String name = (String)nameObj;
                         String value = props.getProperty(name);
                         String newValue = StringPropertyReplacer.replaceProperties(value, this.sysProps);
                         this.sysProps.setProperty(name, newValue);
                     }
                     break;
                 }
             }
         }
     }

     private static String addPropArgToProps(String arg, Properties props) {
         int i = arg.indexOf("=");
         String name;
         String value;
         if (i == -1) {
             name = arg;
             value = "";
         }
         else {
             name = arg.substring(0, i);
             value = arg.substring(i + 1, arg.length());
         }
         props.setProperty(name, value);
         return name;
     }

     /**
      * Returns the home directory of the JBossAS server - this is the base directory in which you
      * can find bin/ server/ lib/ client/ docs/ directories.
      * @return a File object pointing to the home directory
      * @throws Exception If there is no way to obtain that directory.
      */
     private File getHomeDir() throws Exception {

        File runJar=null;
        File binDir=null;
        File homeDir=null;
         // method 1: should work 99% of the time.
        if (this.processInfo != null && this.processInfo.getExecutable()!=null &&
                 this.processInfo.getExecutable().getCwd() != null) {
           homeDir = new File(this.processInfo.getExecutable().getCwd()).getParentFile();
           binDir = new File(homeDir, "bin");
           runJar = new File(binDir, "run.jar");
           if (runJar.exists()) {
             return homeDir;
           }
        }
         // method 2: more expensive, but also more reliable.
         for (String pathElement : this.classPath) {
             if (pathElement.endsWith("run.jar")) {
                 runJar = new File(pathElement);
                 if (!runJar.isAbsolute()) {
                     runJar = new File(this.processInfo.getExecutable().getCwd(), runJar.getPath());
                 }
                 if (!runJar.exists()) {
                     break;
                 }
                 binDir = runJar.getParentFile();
                 if (binDir == null || !binDir.exists()) {
                     break;
                 }
                 homeDir = binDir.getParentFile();
                 if (homeDir == null || !homeDir.exists()) {
                     break;
                 }
                 return homeDir;
             }
         }
         throw new Exception("Could not determine JBossAS home dir - classpath is: " + Arrays.toString(this.classPath));
     }

     private void finalizeSysProps() throws Exception {
         File homeDir;
         if (this.sysProps.containsKey(JBossProperties.HOME_DIR)) {
             homeDir = new File(this.sysProps.getProperty(JBossProperties.HOME_DIR));
         }
         else {
             homeDir = getHomeDir();
             this.sysProps.setProperty(JBossProperties.HOME_DIR, homeDir.toString());
         }
         if (!this.sysProps.containsKey(JBossProperties.HOME_URL)) {
             this.sysProps.setProperty(JBossProperties.HOME_URL, homeDir.toURL().toString());
         }
         File serverBaseDir;
         if (!sysProps.containsKey(JBossProperties.SERVER_BASE_DIR)) {
             serverBaseDir = new File(homeDir, "server");
             this.sysProps.setProperty(JBossProperties.SERVER_BASE_DIR, serverBaseDir.toString());
         } else {
             serverBaseDir = new File(this.sysProps.getProperty(JBossProperties.SERVER_BASE_DIR));
         }
         if (!this.sysProps.containsKey(JBossProperties.SERVER_BASE_URL)) {
             this.sysProps.setProperty(JBossProperties.SERVER_BASE_URL, serverBaseDir.toURL().toString());
         }
         this.installInfo = new JBossInstallationInfo(homeDir);
         if (!this.sysProps.containsKey(JBossProperties.SERVER_NAME)) {
             this.sysProps.setProperty(JBossProperties.SERVER_NAME, this.installInfo.getProductType().DEFAULT_CONFIG_NAME);
         }
         String serverName = this.sysProps.getProperty(JBossProperties.SERVER_NAME);
         if (!this.sysProps.containsKey(JBossProperties.SERVER_HOME_DIR)) {
             this.sysProps.setProperty(JBossProperties.SERVER_HOME_DIR, serverBaseDir + File.separator + serverName);
         }
         if (!this.sysProps.containsKey(JBossProperties.SERVER_HOME_URL)) {
             String serverHomeDir = this.sysProps.getProperty(JBossProperties.SERVER_HOME_DIR);
             this.sysProps.setProperty(JBossProperties.SERVER_HOME_URL, new File(serverHomeDir).toURL().toString());
         }

         if (!this.sysProps.containsKey(JBossProperties.BIND_ADDRESS)) {
             this.sysProps.setProperty(JBossProperties.BIND_ADDRESS, this.installInfo.getDefaultBindAddress());
         }
         String bindAddress = this.sysProps.getProperty(JBossProperties.BIND_ADDRESS); // this will not be null
         String remoteAddress = getRemoteAddress(bindAddress);
         String jgroupsBindAddress = this.sysProps.getProperty(JBossProperties.JGROUPS_BIND_ADDRESS);
         String jgroupsBindAddr = this.sysProps.getProperty(JBossProperties.JGROUPS_BIND_ADDR);
         if (jgroupsBindAddress == null) {
             jgroupsBindAddress = (jgroupsBindAddr != null) ? jgroupsBindAddr : remoteAddress;
             this.sysProps.setProperty(JBossProperties.JGROUPS_BIND_ADDRESS,
                     jgroupsBindAddress);
         }
         if (jgroupsBindAddr == null) {
             this.sysProps.setProperty(JBossProperties.JGROUPS_BIND_ADDR, jgroupsBindAddress);
         }
         if (!this.sysProps.contains(JavaSystemProperties.JAVA_RMI_SERVER_HOSTNAME)) {
             this.sysProps.setProperty(JavaSystemProperties.JAVA_RMI_SERVER_HOSTNAME, remoteAddress);
         }
     }

     private void printSysProps() {
         if (log.isDebugEnabled()) {
             printSysProp(JBossProperties.HOME_DIR);
             printSysProp(JBossProperties.SERVER_HOME_DIR);
             printSysProp(JBossProperties.SERVER_NAME);
             printSysProp(JBossProperties.BIND_ADDRESS);
         }
     }

     private void printSysProp(String name) {
         String value = this.sysProps.getProperty(name);
         log.debug(name + "=" + ((value != null) ? "\"" + value + "\"" : null));
     }

     /**
      * Get the remote address corresponding to the given server address.
      * <p/>
      * If we pass the address to the client we don't want to tell it to connect to 0.0.0.0, use our host name instead.
      *
      * @param address the passed address
      * @return the remote address
      */
     private static String getRemoteAddress(String address) {
         try {
             if (address == null || address.equals(ANY_ADDRESS))
                 return InetAddress.getLocalHost().getHostName();
         }
         catch (UnknownHostException ignored) {
         }
         return address;
     }

     @Override
     public String toString() {
         StringBuilder buf = new StringBuilder(this.getClass().getSimpleName());
         buf.append("[");
         buf.append("sysProps").append("=").append(this.sysProps).append(", ");
         buf.append("installInfo").append("=").append(this.installInfo);
         buf.append("]");
         return buf.toString();
     }
 }