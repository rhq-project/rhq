/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.bundle.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.helper.ProjectHelper2;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;

public class AntLauncher {

    // currently, the bundle plugins don't need custom ant tasks, but there is an ant task stubbed for future use here
    private static final String BUNDLE_ANT_TASKS = "bundle-ant-tasks.properties";

    // "out of box" we will provide the antcontrib optional tasks
    private static final String ANTCONTRIB_ANT_TASKS = "net/sf/antcontrib/antcontrib.properties";

    /**
     * Launches ANT and parses the given build file and optionally executes it.
     *
     * @param buildFile      the build file that ANT will run
     * @param target         the target to run, <code>null</code> will run the default target
     * @param customTaskDefs the properties files found in classloader that contains all the taskdef definitions
     * @param properties     set of properties to set for the ANT task to access
     * @param logFile        where ANT messages will be logged
     * @param logStdOut      if <code>true</code>, log messages will be sent to stdout as well as the log file
     * @param execute        if <code>true</code> the ant script will be parsed and executed; otherwise, it will only be parsed
     *
     * @throws RuntimeException
     */
    public BundleAntProject startAnt(File buildFile, String target, Set<String> customTaskDefs, Properties properties,
        File logFile, boolean logStdOut, boolean execute) {

        PrintWriter logFileOutput = null;

        try {
            logFileOutput = new PrintWriter(new FileOutputStream(logFile, true));

            ClassLoader classLoader = getClass().getClassLoader();

            if (customTaskDefs == null) {
                customTaskDefs = new HashSet<String>(1);
            }
            customTaskDefs.add(ANTCONTRIB_ANT_TASKS); // we always want to provide these

            Properties taskDefs = new Properties();
            for (String customTaskDef : customTaskDefs) {
                InputStream taskDefsStream = classLoader.getResourceAsStream(customTaskDef);
                try {
                    taskDefs.load(taskDefsStream);
                } finally {
                    taskDefsStream.close();
                }
            }

            BundleAntProject project = new BundleAntProject();
            project.setCoreLoader(classLoader);
            project.init();
            project.setBaseDir(buildFile.getParentFile());

            if (properties != null) {
                for (Map.Entry<Object, Object> property : properties.entrySet()) {
                    // On the assumption that these properties will be slurped in via Properties.load we
                    // need to escape backslashes to have them treated as literals 
                    project.setProperty(property.getKey().toString(), property.getValue().toString().replace("\\",
                        "\\\\"));
                }
            }

            // notice we are adding the listener after we set the properties - we do not want the
            // the properties echoed out in the log (in case they contain sensitive passwords)
            project.addBuildListener(new LoggerAntBuildListener(target, logFileOutput, Project.MSG_DEBUG));
            if (logStdOut) {
                PrintWriter stdout = new PrintWriter(System.out);
                project.addBuildListener(new LoggerAntBuildListener(target, stdout, Project.MSG_INFO));
            }

            for (Map.Entry<Object, Object> taskDef : taskDefs.entrySet()) {
                project.addTaskDefinition(taskDef.getKey().toString(), Class.forName(taskDef.getValue().toString(),
                    true, classLoader));
            }

            ProjectHelper2 helper = new ProjectHelper2();
            helper.parse(project, buildFile);

            parseBundleFiles(project);
            parseBundleConfiguration(project);

            if (execute) {
                project.executeTarget((target == null) ? project.getDefaultTarget() : target);
            }

            return project;

        } catch (Exception e) {
            throw new RuntimeException("Cannot run ANT on script [" + buildFile + "]. Cause: " + e, e);
        } finally {
            if (logFileOutput != null) {
                logFileOutput.close();
            }
        }
    }

    private void parseBundleFiles(BundleAntProject project) {
        Map<String, ? extends Task> refs = project.getReferences();
        for (Map.Entry<String, ? extends Task> entry : refs.entrySet()) {
            String refId = entry.getKey();
            if (refId.startsWith("_bundlefile.")) {
                Hashtable attribs = entry.getValue().getRuntimeConfigurableWrapper().getAttributeMap();

                if (!attribs.containsKey("name")) {
                    throw new BuildException("Property id=[" + refId + "] must specify the 'name' attribute");
                }
                String name = attribs.get("name").toString();

                // allow the bundle author to specify the bundle file in one of the standard two ways
                Object location = attribs.get("location");
                Object value = attribs.get("value");

                String bundleFilename;
                if (location != null) {
                    bundleFilename = location.toString();
                } else if (value != null) {
                    bundleFilename = value.toString();
                } else {
                    throw new BuildException("Property id=[" + refId + "], name=[" + name
                        + "] must specify the bundle file name in an attribute named 'location' or 'value'");
                }

                project.addBundleFile(name, bundleFilename);
            }
        }
    }

    private void parseBundleConfiguration(BundleAntProject project) {
        Map<String, ? extends Task> refs = project.getReferences();
        for (Map.Entry<String, ? extends Task> entry : refs.entrySet()) {
            String refId = entry.getKey();
            if (refId.startsWith("_bundleconfig.")) {
                Hashtable attribs = entry.getValue().getRuntimeConfigurableWrapper().getAttributeMap();

                if (!attribs.containsKey("name")) {
                    throw new BuildException("Property id=[" + refId + "] must specify the 'name' attribute");
                }
                String name = attribs.get("name").toString();

                // allow the bundle author to specify the bundle config prop in one of the standard two ways
                Object location = attribs.get("location");
                Object value = attribs.get("value");

                String bundlePropValue;
                if (location != null) {
                    bundlePropValue = location.toString();
                } else if (value != null) {
                    bundlePropValue = value.toString();
                } else {
                    throw new BuildException("Property id=[" + refId + "], name=[" + name
                        + "] must specify the bundle configuration name in an attribute named 'location' or 'value'");
                }

                PropertyDefinitionSimple prop = new PropertyDefinitionSimple(name, "Needed by bundle recipe.", false,
                    PropertySimpleType.STRING);
                prop.setDisplayName(name);
                prop.setDefaultValue(bundlePropValue);// TODO: I would like to set this-setDefaultValue is deprecated, how to do this?
                ConfigurationDefinition configDef = project.getConfigurationDefinition();
                configDef.put(prop);
            }
        }
    }
}
