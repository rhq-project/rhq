/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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

import java.io.*;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.*;
import org.apache.tools.ant.helper.AntXMLContext;
import org.apache.tools.ant.helper.ProjectHelper2;

import org.rhq.bundle.ant.task.BundleTask;
import org.rhq.bundle.ant.task.DeployTask;
import org.rhq.bundle.ant.task.InputPropertyTask;

/**
 * This object enables you to invoke an Ant script within the running VM. You can fully run the script
 * or you can ask that the script just be parsed and validated but no tasks executed.
 * 
 * @author John Mazzitelli
 * @author Ian Springer
 */
public class AntLauncher {        
    // "out of box" we will provide the ant contrib optional tasks (from ant-contrib.jar)
    private static final String ANTCONTRIB_ANT_TASKS = "net/sf/antcontrib/antcontrib.properties";

    // "out of box" we will provide the liquibase tasks (from liquibase-core.jar)
    private static final String LIQUIBASE_ANT_TASKS = "liquibasetasks.properties";

    // private constant ProjectHelper2.REFID_CONTEXT value
    private static final String REFID_CONTEXT = "ant.parsing.context";

    private final Log log = LogFactory.getLog(AntLauncher.class);

    /**
     * Executes the specified bundle deploy Ant build file (i.e. rhq-deploy.xml).
     *
     * @param buildFile       the path to the build file (i.e. rhq-deploy.xml)
     * @param targetName      the target to run, <code>null</code> will run the default target
     * @param buildProperties the properties to pass into Ant
     * @param buildListeners  a list of build listeners (provide callback methods for targetExecuted, taskExecuted, etc.)
     *
     * @return the bundle Ant project containing information about the specified build file
     *
     * @throws InvalidBuildFileException if the build file is invalid
     */
    public BundleAntProject executeBundleDeployFile(File buildFile, String targetName, Properties buildProperties,
                                                    List<BuildListener> buildListeners)
            throws InvalidBuildFileException {
        parseBundleDeployFile(buildFile);
                
        BundleAntProject project = new BundleAntProject();
        ClassLoader classLoader = getClass().getClassLoader();
        project.setCoreLoader(classLoader);
        project.init();
        project.setBaseDir(buildFile.getParentFile());

        try {
            if (buildProperties != null) {
                for (Map.Entry<Object, Object> property : buildProperties.entrySet()) {
                    // On the assumption that these properties will be slurped in via Properties.load we
                    // need to escape backslashes to have them treated as literals
                    project.setUserProperty(property.getKey().toString(), property.getValue().toString().replace("\\",
                        "\\\\"));
                }
            }
            project.setUserProperty(MagicNames.ANT_FILE,
                                buildFile.getAbsolutePath());
            project.setUserProperty(MagicNames.ANT_FILE_TYPE,
                                    MagicNames.ANT_FILE_TYPE_FILE);
            ProjectHelper.configureProject(project, buildFile);

            if (buildListeners != null) {
                for (BuildListener buildListener : buildListeners) {
                    project.addBuildListener(buildListener);
                }
            }

            // Add tasks defs for the Ant tasks that we bundle, so user won't have to explicitly declare them in their
            // build file.
            addTaskDefsForBundledTasks(project);

            // First execute the implicit target, which contains all tasks defined in the build file outside of any
            // targets.
            project.executeTarget("");

            // Now execute the target the user actually specified.
            project.executeTarget((targetName == null) ? project.getDefaultTarget() : targetName);

            return project;
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute bundle deploy file [" + buildFile.getAbsolutePath() + "]. Cause: " + e, e);
        }
    }

    public BundleAntProject parseBundleDeployFile(File buildFile) throws InvalidBuildFileException {
        ClassLoader classLoader = getClass().getClassLoader();

        BundleAntProject project = new BundleAntProject();
        project.setCoreLoader(classLoader);
        project.init();
        project.setBaseDir(buildFile.getParentFile());

        AllOrNothingTarget allOrNothingTarget = new AllOrNothingTarget(true);
        allOrNothingTarget.setName("");
        allOrNothingTarget.setProject(project);

        AntXMLContext context = new AntXMLContext(project);
        context.setImplicitTarget(allOrNothingTarget);
        context.getTargets().clear();
        context.getTargets().addElement(context.getImplicitTarget());

        project.addReference(REFID_CONTEXT, context);
        project.addReference(ProjectHelper2.REFID_TARGETS, context.getTargets());

        ProjectHelper2 helper = new ProjectHelper2();
        try {
            helper.parse(project, buildFile);
        } catch (BuildException e) {
            throw new InvalidBuildFileException("Failed to parse bundle Ant build file.", e);
        }

        validateAndPreprocess(project);

        log.debug("==================== PARSED BUNDLE ANT BUILD FILE ====================");
        log.debug(" Bundle Name: " + project.getBundleName());
        log.debug(" Bundle Version: " + project.getBundleVersion());
        log.debug(" Bundle Description: " + project.getBundleDescription());
        log.debug(" Deployment Config Def: " + project.getConfigurationDefinition().getPropertyDefinitions().values());
        log.debug("======================================================================");

        return project;
    }

    private void addTaskDefsForBundledTasks(BundleAntProject project) throws IOException, ClassNotFoundException {
        Properties taskDefs = buildTaskDefProperties(project.getCoreLoader());
        for (Map.Entry<Object, Object> taskDef : taskDefs.entrySet()) {
            project.addTaskDefinition(taskDef.getKey().toString(), Class.forName(taskDef.getValue().toString(),
                true, project.getCoreLoader()));
        }
    }

    private Properties buildTaskDefProperties(ClassLoader classLoader) throws IOException {
        Set<String> customTaskDefs = new HashSet<String>(1);

        customTaskDefs.add(ANTCONTRIB_ANT_TASKS);
        customTaskDefs.add(LIQUIBASE_ANT_TASKS);

        Properties taskDefProps = new Properties();
        for (String customTaskDef : customTaskDefs) {
            InputStream taskDefsStream = classLoader.getResourceAsStream(customTaskDef);
            try {
                taskDefProps.load(taskDefsStream);
            } finally {
                taskDefsStream.close();
            }
        }
        return taskDefProps;
    }

    private void validateAndPreprocess(BundleAntProject project) throws InvalidBuildFileException {
        AntXMLContext antParsingContext = (AntXMLContext) project.getReference("ant.parsing.context");
        Vector targets = antParsingContext.getTargets();
        int bundleTaskCount = 0;
        Task unconfiguredBundleTask = null;
        for (Object targetObj : targets) {
            Target target = (Target) targetObj;
            Task[] tasks = target.getTasks();
            for (Task task : tasks) {
                // NOTE: For rhq:inputProperty tasks, the below call will add propDefs to the project configDef.
                if (task.getTaskName().equals("rhq:bundle")) {
                    abortIfTaskWithinTarget(target, task);
                    bundleTaskCount++;
                    unconfiguredBundleTask = task;
                } else if (task.getTaskName().equals("rhq:inputProperty")) {
                    abortIfTaskWithinTarget(target, task);
                    InputPropertyTask inputPropertyTask = (InputPropertyTask) preconfigureTask(task);
                } else if (task.getTaskName().equals("rhq:deploy")) {
                    DeployTask deployTask = (DeployTask) preconfigureTask(task);
                    Map<File, File> files = deployTask.getFiles();
                    for (File file : files.values()) {
                        project.getBundleFileNames().add(file.getName());
                    }
                    Set<File> archives = deployTask.getArchives();
                    for (File archive : archives) {
                        project.getBundleFileNames().add(archive.getName());
                    }
                }
            }
        }
        if (bundleTaskCount == 0) {
            throw new InvalidBuildFileException(
                "rhq:bundle task not found - an RHQ bundle Ant build file must contain exactly one rhq:bundle task.");
        }
        if (bundleTaskCount > 1) {
            throw new InvalidBuildFileException(
                "More than one rhq:bundle task found - an RHQ bundle Ant build file must contain exactly one rhq:bundle task.");
        }

        BundleTask bundleTask = (BundleTask) preconfigureTask(unconfiguredBundleTask);
    }

    private void abortIfTaskWithinTarget(Target target, Task task) throws InvalidBuildFileException {
        if (!target.getName().equals("")) {
            throw new InvalidBuildFileException(task.getTaskName() + " task found within [" + target.getName()
                + "] target - it must be outside of any targets (at the top of the build file).");
        }
    }

    private static Task preconfigureTask(Task task) {
        if (task instanceof UnknownElement) {
            task.maybeConfigure();
            Task resolvedTask = ((UnknownElement) task).getTask();
            return (resolvedTask != null) ? resolvedTask : task;
        } else {
            return task;
        }
    }

    class AllOrNothingTarget extends Target {
        public boolean doNothing = true;

        AllOrNothingTarget(boolean doNothing) {
            this.doNothing = doNothing;
        }

        @Override
        public void execute() throws BuildException {
            if (!doNothing) {
                super.execute();
            }
        }
    }
}
