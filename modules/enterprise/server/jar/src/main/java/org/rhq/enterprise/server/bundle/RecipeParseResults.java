/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.server.bundle;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;

/**
 * This class is general to all Bundle handling server side plugins.  It is returned by the SSP when parsing
 * a bundle's recipe and contains all of the information neded to create a bundle version.  
 * 
 * @author jay shaughnessy
 */
public class RecipeParseResults {

    /** The configuration definition parsed out of, or expcitly provided by, the recipe */
    private ConfigurationDefinition configDef;

    /** The set of bundle files that make up the bundle (version) as specified in the recipe commands */
    private Set<String> bundleFileNames;

    private RecipeParseResults(ConfigurationDefinition configDef, Set<String> bundleFileNames) {
        this.configDef = configDef;
        this.bundleFileNames = bundleFileNames;
    }

    public ConfigurationDefinition getConfigDef() {
        // stub out a test ConfigDef here until we actual parse the recipe
        configDef = new ConfigurationDefinition("SampleBundle", "Stubbed ConfigDef for Sample Bundle");
        configDef.put(new PropertyDefinitionSimple("sample.bundle.deploy.directory",
            "Stubbed Property for deploy directory (must already exist on target platform)", true,
            PropertySimpleType.STRING));

        return configDef;
    }

    public void setConfigDef(ConfigurationDefinition configDef) {
        this.configDef = configDef;
    }

    public Set<String> getBundleFileNames() {
        // stub out a test set of BundleFiles here until we actual parse the recipe. For now lets just deploy
        // a war file into an existing app server dir.
        bundleFileNames = new HashSet<String>();
        bundleFileNames.add("sample-bundle.war");

        return bundleFileNames;
    }

    public void setBundleFiles(Set<String> bundleFileNames) {
        this.bundleFileNames = bundleFileNames;
    }

}
