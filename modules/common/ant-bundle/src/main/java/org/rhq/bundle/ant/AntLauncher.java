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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.helper.AntXMLContext;
import org.apache.tools.ant.helper.ProjectHelper2;

import org.rhq.bundle.ant.task.BundleTask;
import org.rhq.bundle.ant.type.DeploymentUnitType;

/**
 * This object enables you to invoke an Ant script within the running VM. You can fully run the script
 * or you can ask that the script just be parsed and validated but no tasks executed.
 * 
 * @author John Mazzitelli
 * @author Ian Springer
 */
public class AntLauncher {
    private final Log log = LogFactory.getLog(this.getClass());

    // "out of box" we will provide the ant contrib optional tasks (from ant-contrib.jar)
    private static final String ANTCONTRIB_ANT_TASKS = "net/sf/antcontrib/antcontrib.properties";

    // "out of box" we will provide the liquibase tasks (from liquibase-core.jar)
    private static final String LIQUIBASE_ANT_TASKS = "liquibasetasks.properties";

    // private constant ProjectHelper2.REFID_CONTEXT value
    private static final String REFID_CONTEXT = "ant.parsing.context";

    // TODO (ips, 04/28/10): Figure out a way to avoid assuming the prefix is "rhq".
    private static final String BUNDLE_TASK_NAME = "rhq:bundle";

    private boolean requireExplicitCompliance;

    /**
     * For backwards compatibility reasons, this calls {@link #AntLauncher(boolean) AntLauncher(false)}.
     * Note that this might change in the future, because we are <b>requiring</b> the explicit declaration of the
     * destination directory's compliance mode starting with RHQ 4.9.0.
     * <p/>
     * Nevertheless this constructor is behaving as it was before RHQ 4.9.0 so that users of it aren't surprised
     * by its behavior.
     *
     * @deprecated since 4.9.0. You can keep using this constructor but be aware that it might change behavior in some
     * future version of RHQ. It will NOT be removed though.
     */
    @Deprecated
    public AntLauncher() {
        this(false);
    }

    /**
     * @since 4.9.0
     * @param requireExplicitCompliance whether or not to enforce the presence of {@code compliance} attribute in the
     *                                  deployment unit definitions. Before RHQ 4.9.0 a similar deprecated attribute
     *                                  called {@code manageRootDir} was optional and defaulted to {@code true}. Since
     *                                  RHQ 4.9.0 we require the user to explicitly specify the compliance of the
     *                                  destination directory. But to keep backwards compatibility with the older
     *                                  bundle recipes already deployed on the agents, we make this behavior optional.
     */
    public AntLauncher(boolean requireExplicitCompliance) {
        this.requireExplicitCompliance = requireExplicitCompliance;
    }

    /**
     * Executes the specified bundle deploy Ant build file (i.e. rhq-deploy.xml).
     *
     * @param buildFile       the path to the build file (i.e. rhq-deploy.xml)
     * @param buildProperties the properties to pass into Ant
     * @param buildListeners  a list of build listeners (provide callback methods for targetExecuted, taskExecuted, etc.)
     *
     * @return the bundle Ant project containing information about the specified build file
     *
     * @throws InvalidBuildFileException if the build file is invalid
     */
    public BundleAntProject executeBundleDeployFile(File buildFile, Properties buildProperties,
        List<BuildListener> buildListeners) throws InvalidBuildFileException {
        // Parse and validate the build file before even attempting to execute it.
        BundleAntProject parsedProject = parseBundleDeployFile(buildFile, buildProperties);

        BundleAntProject project = createProject(buildFile, false, buildProperties);

        // The parse above got us all the bundle files names. The rest of this method
        // will be able to re-determine everything else for 'project' but these filenames.
        // Therefore, we need to copy those filenames from the parsedProject to project.
        // The rest of project will be filled in later.
        project.getBundleFileNames().addAll(parsedProject.getBundleFileNames());

        try {
            if (buildListeners != null) {
                for (BuildListener buildListener : buildListeners) {
                    project.addBuildListener(buildListener);
                }
            }

            // Add taskdefs for the Ant tasks that we bundle, so user won't have to explicitly declare them in their
            // build file.
            addTaskDefsForBundledTasks(project);

            // This will parse the build file and initialize all tasks, as well as execute the implicit target,
            // which contains the rhq:bundle task.
            ProjectHelper.configureProject(project, buildFile);

            // Should we execute the default target here?
            //project.executeTarget(project.getDefaultTarget());

            return project;
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute bundle deploy file [" + buildFile.getAbsolutePath()
                + "]. Cause: " + e, e);
        }
    }

    public BundleAntProject parseBundleDeployFile(File buildFile, Properties buildProperties)
        throws InvalidBuildFileException {
        BundleAntProject project = createProject(buildFile, true, buildProperties);

        ProjectHelper2 projectHelper = new ProjectHelper2();
        try {
            // Use the 3-param version of parse(), rather than the 2-param version, or ProjectHelper.configureProject(),
            // to avoid actually executing the implicit target (which would cause the rhq:bundle task to be executed).
            AntXMLContext context = (AntXMLContext) project.getReference(REFID_CONTEXT);
            projectHelper.parse(project, buildFile, new ProjectHelper2.RootHandler(context,
                new ProjectHelper2.MainHandler()));
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

    private BundleAntProject createProject(File buildFile, boolean parseOnly, Properties buildProperties) {

        ClassLoader classLoader = getClass().getClassLoader();

        BundleAntProject project = new BundleAntProject(parseOnly);

        if (buildProperties != null) {
            for (Map.Entry<Object, Object> property : buildProperties.entrySet()) {
                // On the assumption that these properties will be slurped in via Properties.load we
                // need to escape backslashes to have them treated as literals
                project.setProperty(property.getKey().toString(), property.getValue().toString().replace("\\",
                    "\\\\"));
            }
        }
        project.setProperty(MagicNames.ANT_FILE, buildFile.getAbsolutePath());
        project.setProperty(MagicNames.ANT_FILE_TYPE, MagicNames.ANT_FILE_TYPE_FILE);

        project.setCoreLoader(classLoader);
        project.init();
        project.setBaseDir(buildFile.getParentFile());

        AntXMLContext context = new AntXMLContext(project);
        context.setCurrentTargets(new HashMap());

        project.addReference(REFID_CONTEXT, context);
        project.addReference(ProjectHelper2.REFID_TARGETS, context.getTargets());
        return project;
    }

    private void addTaskDefsForBundledTasks(BundleAntProject project) throws IOException, ClassNotFoundException {
        Properties taskDefs = buildTaskDefProperties(project.getCoreLoader());
        for (Map.Entry<Object, Object> taskDef : taskDefs.entrySet()) {
            project.addTaskDefinition(taskDef.getKey().toString(), Class.forName(taskDef.getValue().toString(), true,
                project.getCoreLoader()));
        }
    }

    private Properties buildTaskDefProperties(ClassLoader classLoader) throws IOException {
        Set<String> customTaskDefs = new HashSet<String>(2);

        customTaskDefs.add(ANTCONTRIB_ANT_TASKS);
        customTaskDefs.add(LIQUIBASE_ANT_TASKS);

        Properties taskDefProps = new Properties();
        for (String customTaskDef : customTaskDefs) {
            InputStream taskDefsStream = classLoader.getResourceAsStream(customTaskDef);
            if (taskDefsStream != null) {
                try {
                    taskDefProps.load(taskDefsStream);
                } catch (Exception e) {
                    log.warn("Ant task definitions [" + customTaskDef
                        + "] failed to load - ant bundles cannot use their tasks", e);
                } finally {
                    taskDefsStream.close();
                }
            } else {
                log.warn("Missing ant task definitions [" + customTaskDef + "] - ant bundles cannot use their tasks");
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
                if (task.getTaskName().equals(BUNDLE_TASK_NAME)) {
                    abortIfTaskWithinTarget(target, task);
                    bundleTaskCount++;
                    unconfiguredBundleTask = task;
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
        Collection<DeploymentUnitType> deployments = bundleTask.getDeploymentUnits().values();
        if (deployments.isEmpty()) {
            throw new InvalidBuildFileException(
                "The bundle task must contain exactly one rhq:deploymentUnit child element.");
        }
        DeploymentUnitType deployment = deployments.iterator().next();

        if (requireExplicitCompliance && deployment.getCompliance() == null) {
            throw new InvalidBuildFileException(
                "The deployment unit must specifically declare compliance mode of the destination directory.");
        }

        project.setDestinationCompliance(deployment.getCompliance());

        Map<File, String> files = deployment.getLocalFileNames();
        for (String file : files.values()) {
            project.getBundleFileNames().add(file);
        }
        Map<File, String> archives = deployment.getLocalArchiveNames();
        for (String archive : archives.values()) {
            project.getBundleFileNames().add(archive);
        }

        // note that we do NOT add url-files and url-archives to the BundleFileNames because those are
        // not true "bundle files" that are stored with the bundle version in the database. Those will
        // be downloaded by the agents at the time the recipe is invoked. There is nothing server side
        // that need to be known about the files/archives from URLs.

        return;
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
}
