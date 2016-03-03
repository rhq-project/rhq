/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.modules.plugins.wildfly10;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.pluginapi.util.CommandLineOption;
import org.rhq.core.pluginapi.util.JavaCommandLine;
import org.rhq.core.system.ProcessInfo;

/**
 * Parses a JBoss AS7 command line and provides easy access to its parts.
 *
 * @author Ian Springer
 */
public class AS7CommandLine extends JavaCommandLine {

    public static final String HOME_DIR_SYSPROP = "jboss.home.dir";

    private static final String APP_SERVER_MODULE_NAME_PREFIX = "org.jboss.as";
    private static final CommandLineOption PROPERTIES_OPTION = new CommandLineOption("P", "properties", true);
    private static final String[] PROPERTIES_OPTION_PREFIXES = new String[]{
            "-" + PROPERTIES_OPTION.getShortName(),
            "--" + PROPERTIES_OPTION.getLongName()
    };

    private static final Log LOG = LogFactory.getLog(AS7CommandLine.class);

    private String appServerModuleName;
    private List<String> appServerArgs;
    private ProcessInfo process;

    public AS7CommandLine(String[] args) {
        // Note, we don't use EnumSet.allOf() just in case some other option delimiter is added to the enum in the future.
        super(args, true, EnumSet.of(OptionValueDelimiter.WHITESPACE, OptionValueDelimiter.EQUALS_SIGN),
                EnumSet.of(OptionValueDelimiter.WHITESPACE, OptionValueDelimiter.EQUALS_SIGN));
    }

    public AS7CommandLine(ProcessInfo process) {
        this(process.getCommandLine());
        this.process = process;
    }

    @NotNull
    public String getAppServerModuleName() {
        if (!isArgumentsParsed()) {
            parseCommandLine();
        }

        return this.appServerModuleName;
    }

    @NotNull
    public List<String> getAppServerArguments() {
        if (!isArgumentsParsed()) {
            parseCommandLine();
        }

        return this.appServerArgs;
    }

    @Override
    protected void parseCommandLine() {
        super.parseCommandLine();

        // In the case of AS7, the class arguments are actually the arguments to the jboss-modules.jar main class. We
        // want to split out the arguments to the app server module (i.e. "org.jboss.as.standalone" or
        // "org.jboss.as.host-controller"). e.g. For the class arguments
        // "-mp /home/ips/Applications/jboss-as-7.1.1.Final/modules -jaxpmodule javax.xml.jaxp-provider
        // org.jboss.as.standalone -Djboss.home.dir=/opt/jboss-as-7.1.1.Final --server-config=standalone-full.xml",
        // this.appServerModuleName would get set to "org.jboss.as.standalone" and this.appServerArgs would get set to
        // "-Djboss.home.dir=/opt/jboss-as-7.1.1.Final --server-config=standalone-full.xml"
        List<String> classArgs = super.getClassArguments();
        for (int i = 0, classArgsSize = classArgs.size(); i < classArgsSize; i++) {
            String classArg = classArgs.get(i);
            if (classArg.startsWith(APP_SERVER_MODULE_NAME_PREFIX)) {
                this.appServerModuleName = classArg;
                if ((i + 1) < classArgsSize) {
                    this.appServerArgs = Collections.unmodifiableList(classArgs.subList(i + 1, classArgsSize));
                } else {
                    this.appServerArgs = Collections.emptyList();
                }
                break;
            }
        }
        if (this.appServerModuleName == null) {
            throw new IllegalArgumentException("Class arguments do not contain an argument starting with \""
                    + APP_SERVER_MODULE_NAME_PREFIX + "\".");
        }
    }

    @Override
    protected void processClassArgument(String classArg, String nextArg) {
        super.processClassArgument(classArg, nextArg);

        String propertiesOptionValue = null;
        for (String propertiesOption : PROPERTIES_OPTION_PREFIXES) {
            if (classArg.startsWith(propertiesOption)) {
                if ((propertiesOption.length() < classArg.length()) &&
                        (classArg.charAt(propertiesOption.length()) == '=')) {
                    // single-arg option, e.g. "--properties=jboss-as.properties"
                    propertiesOptionValue = classArg.substring(propertiesOption.length() + 1);
                } else {
                    // double-arg option, e.g. "--properties jboss-as.properties"
                    propertiesOptionValue = nextArg;
                }
            }
        }
        if (propertiesOptionValue != null) {
            URL propertiesURL = toURL(propertiesOptionValue);
            if (propertiesURL != null) {
                Properties props = loadProperties(propertiesURL);
                if (props != null) {
                    Map<String, String> sysProps = getSystemProperties();
                    for (Map.Entry<?, ?> entry : props.entrySet()) {
                        sysProps.put((String) entry.getKey(), (String) entry.getValue());
                    }
                }
            }
        }
    }

    private URL toURL(String value) {
        URL propertiesURL;
        try {
            propertiesURL = new URL(value);
            if (propertiesURL.getProtocol().equals("file")) {
                String path = propertiesURL.getPath();
                File file = new File(path);
                if (!file.isAbsolute()) {
                    // it's a file URL with a relative path, e.g. "file:jboss-as.properties"
                    File absoluteFile = getAbsoluteFile(file);
                    propertiesURL = absoluteFile.toURI().toURL();
                }
            }
        } catch (MalformedURLException murle) {
            // it's probably just a path, e.g. "/opt/jboss-as-7.1.1.Final/bin/jboss-as.properties" or "jboss-as.properties"
            File file = new File(value);
            File absoluteFile = getAbsoluteFile(file);
            try {
                propertiesURL = absoluteFile.toURI().toURL();
            } catch (MalformedURLException murle2) {
                propertiesURL = null;
                LOG.error("Value of class option " + PROPERTIES_OPTION + " (" + value + ") is not a valid URL.");
            }
        }

        return propertiesURL;
    }

    private File getAbsoluteFile(File file) {
        File absoluteFile;
        if (!file.isAbsolute()) {
            if ((this.process != null) && (this.process.getExecutable() != null)) {
                String cwd = this.process.getExecutable().getCwd();
                absoluteFile = new File(cwd, file.getPath());
            } else {
                String homeDir = getSystemProperties().get(HOME_DIR_SYSPROP);
                if (homeDir != null) {
                    File binDir = new File(homeDir, "bin");
                    absoluteFile = new File(binDir, file.getPath());
                } else {
                    LOG.error("Failed to resolve relative properties file path [" + file + "].");
                    return null;
                }
            }
        } else {
            absoluteFile = file;
        }

        return absoluteFile;
    }

    private Properties loadProperties(URL propertiesURL) {
        URLConnection urlConnection;
        try {
            urlConnection = propertiesURL.openConnection();
        } catch (IOException e) {
            LOG.error("Failed to connect to URL [" + propertiesURL + "].", e);
            return null;
        }
        InputStream inputStream;
        try {
            inputStream = urlConnection.getInputStream();
            if (inputStream == null) {
                LOG.error("Failed to read from URL [" + propertiesURL + "].");
                return null;
            }
        } catch (IOException e) {
            LOG.error("Failed to read from URL [" + propertiesURL + "].", e);
            return null;
        }

        Properties props = new Properties();
        try {
            props.load(inputStream);
        } catch (IOException e) {
            LOG.error("Failed to parse properties from URL [" + propertiesURL + "].", e);
            return null;
        }
        return props;
    }

}
