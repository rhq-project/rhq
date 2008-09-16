/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.tool.pluginvalidator;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import org.rhq.core.pc.plugin.PluginValidator;
import org.rhq.core.pc.plugin.SimplePluginFinder;

/**
 * Maven plugin for running the plugin validator against an RHQ plugin.
 *
 * @author Jason Dobies
 * @goal rhq-plugin-validate
 */
public class PluginValidatorMojo extends AbstractMojo {

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    @SuppressWarnings( { "UnusedDeclaration" })
    private MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        log.info("Validating RHQ Plugin");

        // Determine where the plugin is
        String pluginJarName = project.getArtifactId() + "-" + project.getVersion() + ".jar";
        String pluginDirectory = project.getBasedir().getAbsolutePath();

        log.debug("Plugin JAR: " + pluginJarName);
        log.debug("Plugin Directory: " + pluginDirectory);

        File pluginFile = new File(pluginDirectory, pluginJarName);
        if (!pluginFile.exists()) {
            throw new MojoFailureException("Cannot find plugin");
        }

        // Load into validator
        URL pluginUrl;
        try {
            pluginUrl = pluginFile.toURL();
        } catch (MalformedURLException e) {
            throw new MojoFailureException("Could not load URL for plugin file: " + pluginFile);
        }

        log.info("Plugin descriptor directory URL:" + pluginUrl);

        SimplePluginFinder finder = new SimplePluginFinder(pluginUrl);

        // Validate
        boolean success = PluginValidator.validatePlugins(finder);

        if (success) {
            log.info("--------------------------");
            log.info("Plugin Validation: SUCCESS");
            log.info("--------------------------");
        } else {
            log.info("--------------------------");
            log.info("Plugin Validation: FAILURE");
            log.info("--------------------------");
            throw new MojoFailureException("Plugin validation failed. Check the rest of the log for more information.");
        }
    }
}
