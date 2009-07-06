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
package org.rhq.core.tool.plugindoc;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Generates both Confluence and Docbook format documentation for an RHQ plugin based on the plugin's descriptor (i.e.
 * rhq-plugin.xml). Invoke from an RHQ plugin module directory as follows:
 * <code>mvn org.rhq:rhq-core-plugindoc:plugindoc</code>
 *
 * @author                       Ian Springer
 * @author                       Joseph Marques
 * @goal                         plugindoc
 * @requiresProject
 * @requiresDependencyResolution
 */
public class PluginDocMojo extends AbstractMojo {

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The main Confluence URL (e.g. "http://support.rhq-project.org/").
     *
     * @parameter expression="${confluenceUrl}"
     */
    private String confluenceUrl;

    /**
     * The Confluence space (e.g. "RHQ").
     *
     * @parameter expression="${confluenceSpace}"
     */
    private String confluenceSpace;

    /**
     * The Confluence parent page name (e.g. "Managed Resources").
     *
     * @parameter expression="${confluenceParentPageTitle}"
     */
    private String confluenceParentPageTitle;

    /**
     * @parameter expression="${confluenceUserName}"
     */
    private String confluenceUserName;

    /**
     * @parameter expression="${confluencePassword}"
     */
    private String confluencePassword;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        PluginDocGenerator generator = new PluginDocGenerator();
        generator.setConfluenceProperties(confluenceUrl, confluenceSpace, confluenceParentPageTitle,
            confluenceUserName, confluencePassword);
        try {
            generator.execute(this.project.getBasedir().getAbsolutePath());
        } catch (PluginDocGeneratorException pdge) {
            throw new MojoExecutionException(pdge.getMessage(), pdge.getCause());
        }
    }

}