/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.rhqtransform.impl;

import org.rhq.augeas.AugeasProxy;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.rhqtransform.AugeasToConfiguration;
import org.rhq.rhqtransform.ConfigurationToAugeas;
import org.rhq.rhqtransform.RhqAugeasMapping;


/**
 * 
 * @author Filip Drabek
 *
 */
public class RhqAugeasMappingSimple implements RhqAugeasMapping {

    private AugeasToConfiguration augeasToConfiguration;
    private ConfigurationToAugeas configurationToAugeas;
    private String moduleName;

    public RhqAugeasMappingSimple(String moduleName) {
        this.augeasToConfiguration = new AugeasToConfigurationSimple();
        this.configurationToAugeas = new ConfigurationToAugeasSimple();
        this.moduleName = moduleName;
    }

    public RhqAugeasMappingSimple(AugeasToConfiguration toConfig, ConfigurationToAugeas toAugeas, String moduleName) {
        this.augeasToConfiguration = toConfig;
        this.configurationToAugeas = toAugeas;
        this.moduleName = moduleName;
    }

    public void updateAugeas(AugeasProxy component, Configuration config, ConfigurationDefinition configDef)
        throws Exception {
        AugeasTree tree = component.getAugeasTree(moduleName, true);
        AugeasNode startNode = getStartNode(tree);
        configurationToAugeas.updateResourceConfiguration(startNode, configDef, config);
    }

    public Configuration updateConfiguration(AugeasProxy augeasComponent, ConfigurationDefinition configDef)
        throws Exception {
        AugeasTree tree = augeasComponent.getAugeasTree(moduleName, true);
        AugeasNode startNode = getStartNode(tree);
        return augeasToConfiguration.loadResourceConfiguration(startNode, configDef);
    }

    protected AugeasNode getStartNode(AugeasTree tree) throws Exception {
        return tree.getRootNode();
    }

}
