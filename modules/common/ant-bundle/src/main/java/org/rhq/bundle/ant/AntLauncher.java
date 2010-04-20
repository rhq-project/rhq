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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.helper.AntXMLContext;
import org.apache.tools.ant.helper.ProjectHelper2;

import org.rhq.bundle.ant.task.BundleTask;
import org.rhq.bundle.ant.task.DeployTask;
import org.rhq.bundle.ant.task.InputPropertyTask;

/**
 * @author John Mazzitelli
 * @author Ian Springer
 */
public class AntLauncher {
    public static final String DEPLOY_DIR_PROP = "rhq.deploy.dir";

    // "out of box" we will provide the antcontrib optional tasks
    private static final String ANTCONTRIB_ANT_TASKS = "net/sf/antcontrib/antcontrib.properties";

    /**
     * Launches Ant and parses the given build file and optionally executes it.
     *
     * @param buildFile      the build file that Ant will run
     * @param targetName     the target to run, <code>null</code> will run the default target
     * @param customTaskDefs the properties files found in classloader that contains all the taskdef definitions
     * @param properties     set of properties to set for the Ant task to access
     * @param logFile        where Ant messages will be logged
     * @param logStdOut      if <code>true</code>, log messages will be sent to stdout as well as the log file
     * @param execute        if <code>true</code> the Ant script will be parsed and executed; otherwise, it will only be
     *                       parsed and validated
     *
     * @throws RuntimeException
     */
    public BundleAntProject startAnt(File buildFile, String targetName, Set<String> customTaskDefs,
        Properties properties, File logFile, boolean logStdOut, boolean execute) {

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
            project.addBuildListener(new LoggerAntBuildListener(targetName, logFileOutput, Project.MSG_DEBUG));
            if (logStdOut) {
                PrintWriter stdout = new PrintWriter(System.out);
                project.addBuildListener(new LoggerAntBuildListener(targetName, stdout, Project.MSG_INFO));
            }

            for (Map.Entry<Object, Object> taskDef : taskDefs.entrySet()) {
                project.addTaskDefinition(taskDef.getKey().toString(), Class.forName(taskDef.getValue().toString(),
                    true, classLoader));
            }

            class AllOrNothingTarget extends Target {
                public boolean doNothing = true;

                @Override
                public void execute() throws BuildException {
                    if (!doNothing) {
                        super.execute();
                    }
                }
            }
            AllOrNothingTarget allOrNothingTarget = new AllOrNothingTarget();
            allOrNothingTarget.setName("");
            allOrNothingTarget.setProject(project);

            AntXMLContext context = new AntXMLContext(project);
            context.setImplicitTarget(allOrNothingTarget);
            context.getTargets().clear();
            context.getTargets().addElement(context.getImplicitTarget());

            String REFID_CONTEXT = "ant.parsing.context"; // private constant ProjectHelper2.REFID_CONTEXT value
            project.addReference(REFID_CONTEXT, context);
            project.addReference(ProjectHelper2.REFID_TARGETS, context.getTargets());

            ProjectHelper2 helper = new ProjectHelper2();
            try {
                helper.parse(project, buildFile);
            } catch (BuildException e) {
                throw new InvalidBuildFileException("Failed to parse bundle Ant build file.", e);
            }

            validateAndPreprocess(project);

            System.out.println("==================== PARSED BUNDLE ANT BUILD FILE ====================");
            System.out.println(" Bundle Name: " + project.getBundleName());
            System.out.println(" Bundle Version: " + project.getBundleVersion());
            System.out.println(" Bundle Description: " + project.getBundleDescription());
            System.out.println(" Deployment Configuration: " + project.getConfiguration().toString(true));
            System.out.println("======================================================================");

            if (execute) {
                // parse it again, this time, allowing the implicit target to be executed
                allOrNothingTarget.doNothing = false;
                helper.parse(project, buildFile);

                String deployDir = properties.getProperty(DEPLOY_DIR_PROP);
                if (deployDir == null) {
                    throw new BuildException("Required property '" + DEPLOY_DIR_PROP + "' was not specified.");
                }
                File deployDirFile = new File(deployDir);
                if (!deployDirFile.isAbsolute()) {
                    throw new BuildException("Value of property '" + DEPLOY_DIR_PROP + "' (" + deployDirFile
                        + ") is not an absolute path.");
                }
                project.setDeployDir(deployDirFile);
                project.executeTarget((targetName == null) ? project.getDefaultTarget() : targetName);
            }

            return project;

        } catch (Exception e) {
            throw new RuntimeException("Cannot run Ant on build file [" + buildFile + "]. Cause: " + e, e);
        } finally {
            if (logFileOutput != null) {
                logFileOutput.close();
            }
        }
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
}
