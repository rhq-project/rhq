/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.enterprise.server.plugins.filetemplate;

import org.rhq.bundle.filetemplate.recipe.RecipeParser;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.plugin.pc.ControlFacet;
import org.rhq.enterprise.server.plugin.pc.ControlResults;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.plugin.pc.bundle.BundleServerPluginFacet;

/**
 * A bundle server-side plugin component that the server uses to process file template bundles.
 * 
 * @author John Mazzitelli
 */
public class BundleServerPluginComponent implements ServerPluginComponent, BundleServerPluginFacet, ControlFacet {

    private ServerPluginContext context;

    public void initialize(ServerPluginContext context) throws Exception {
        this.context = context;
        System.out.println("The filetemplate bundle plugin has been initialized!!! : " + this);
    }

    public void start() {
        System.out.println("The filetemplate bundle plugin has started!!! : " + this);
    }

    public void stop() {
        System.out.println("The filetemplate bundle plugin has stopped!!! : " + this);
    }

    public void shutdown() {
        System.out.println("The filetemplate bundle plugin has been shut down!!! : " + this);
    }

    public void parseRecipe(String recipe) throws Exception {
        RecipeParser parser = new RecipeParser();
        parser.parseRecipe(recipe);
    }

    public ControlResults invoke(String name, Configuration parameters) {
        ControlResults controlResults = new ControlResults();
        if (name.equals("testControl")) {
            System.out.println("Invoked 'testControl': " + this);
        } else {
            controlResults.setError("Unknown operation name: " + name);
        }
        return controlResults;
    }

    @Override
    public String toString() {
        if (this.context == null) {
            return "<no context>";
        }

        StringBuilder str = new StringBuilder();
        str.append("plugin-key=").append(this.context.getPluginEnvironment().getPluginKey()).append(",");
        str.append("plugin-url=").append(this.context.getPluginEnvironment().getPluginUrl()).append(",");
        str.append("plugin-config=[").append(getPluginConfigurationString()).append(']'); // do not append ,
        return str.toString();
    }

    private String getPluginConfigurationString() {
        String results = "";
        Configuration config = this.context.getPluginConfiguration();
        for (PropertySimple prop : config.getSimpleProperties().values()) {
            if (results.length() > 0) {
                results += ", ";
            }
            results = results + prop.getName() + "=" + prop.getStringValue();
        }
        return results;
    }
}
