 /*
  * RHQ Management Platform
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
package org.rhq.plugins.platform.content.yum;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;

/**
 * Represents the yum command line tool.
 *
 * @author jortel
 */
public class YumProxy {
    /**
     * The fully qualified path to the yum executable.
     */
    private String yum = "/usr/bin/yum";

    private final Log log = LogFactory.getLog(YumProxy.class);

    @SuppressWarnings("unchecked")
    private ResourceContext resourceContext;

    /**
     * Initialize the proxy with the current system information. During which the fully qualified path to the yum
     * executable is determined.
     *
     * @param resourceContext contains things like plugin config and system information
     */
    @SuppressWarnings("unchecked")
    public void init(ResourceContext resourceContext) {
        this.resourceContext = resourceContext;
        ProcessExecution processExecution = new ProcessExecution("/usr/bin/which");
        processExecution.setArguments(new String[] { "yum" });
        processExecution.setCaptureOutput(true);
        ProcessExecutionResults executionResults = resourceContext.getSystemInformation().executeProcess(
            processExecution);
        String capturedOutput = executionResults.getCapturedOutput();
        yum = (((capturedOutput == null) || "".equals(capturedOutput)) ? null : capturedOutput.trim());
        log.info("Using (yum) found at: " + yum);
    }

    /**
     * Get the version of the yum executable. Equal to "yum --version".
     *
     * @return The version string of the yum execuatble.
     *
     * @throws Exception
     */
    public String version() throws Exception {
        List<String> args = new ArrayList<String>(1);
        args.add("--version");
        return execute(args);
    }

    /**
     * Install the specified packages. Equal to "yum -y install package, ...."
     *
     * @param  packages A list of packages to install.
     *
     * @throws Exception On any error.
     */
    public void install(Collection<String> packages) throws Exception {
        log.debug("install packages\n" + packages);
        List<String> args = new ArrayList<String>();
        args.add("install");
        args.add("-y");
        args.addAll(packages);
        execute(args);
    }

    /**
     * Remove the specified packages. Equal to "yum -y remove package, ...."
     *
     * @param  packages A list of packages to remove.
     *
     * @throws Exception On any error.
     */
    public void remove(Collection<String> packages) throws Exception {
        log.debug("remove packages\n" + packages);
        List<String> args = new ArrayList<String>();
        args.add("remove");
        args.add("-y");
        args.addAll(packages);
        execute(args);
    }

    /**
     * Clean the yum metadata Equal to "yum clean metadata"
     *
     * @throws Exception On any error.
     */
    public void cleanMetadata() throws Exception {
        log.debug("clean metadata");
        List<String> args = new ArrayList<String>();
        args.add("clean");
        args.add("metadata");
        execute(args);
    }

    /**
     * Update packages Equal to "yum update"
     *
     * @throws Exception On any error.
     */
    public void update() throws Exception {
        log.debug("update packages");
        List<String> args = new ArrayList<String>();
        args.add("update");
        args.add("-y");
        execute(args);
    }

    /**
     * Execute the specified yum command.
     *
     * @param  args Command arguments.
     *
     * @return The string output.
     *
     * @throws Exception On all errors.
     */
    private String execute(List<String> args) throws Exception {
        if (yum == null) {
            String msg = "yum executable: not-found";
            log.error(msg);
            throw new Exception(msg);
        }

        ProcessExecution installPackages = new ProcessExecution(yum);
        installPackages.setArguments(args);
        installPackages.setCaptureOutput(true);
        installPackages.setWaitForCompletion(1000L * 60 * 30); // wait for it to finish in 30mins
        ProcessExecutionResults result = this.resourceContext.getSystemInformation().executeProcess(installPackages);
        String output = result.getCapturedOutput();
        Integer exitCode = result.getExitCode();
        log.info("yum result: " + exitCode + "\n" + output);
        if ((output == null) && ((exitCode == null) || (result.getExitCode() != 0))) {
            throw new Exception(output);
        }

        return output;
    }
}