/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.core.domain.sync;

import java.io.Serializable;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class ImportConfigurationDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    private String synchronizerClassName;
    private ConfigurationDefinition configurationDefinition;
    
    public ImportConfigurationDefinition() {
        
    }
    
    public ImportConfigurationDefinition(String synchronizerClassName, ConfigurationDefinition configuration) {
        this.synchronizerClassName = synchronizerClassName;
        this.configurationDefinition = configuration;
    }

    /**
     * @return the synchronizerClassName
     */
    public String getSynchronizerClassName() {
        return synchronizerClassName;
    }

    /**
     * @param synchronizerClassName the synchronizerClassName to set
     */
    public void setSynchronizerClassName(String synchronizerClassName) {
        this.synchronizerClassName = synchronizerClassName;
    }

    /**
     * @return the configuration
     */
    public ConfigurationDefinition getConfigurationDefinition() {
        return configurationDefinition;
    }

    /**
     * @param configuration the configuration to set
     */
    public void setConfigurationDefinition(ConfigurationDefinition configuration) {
        this.configurationDefinition = configuration;
    }
    
    @Override
    public int hashCode() {
        return synchronizerClassName.hashCode();
    }
    
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        
        if (!(other instanceof ImportConfigurationDefinition)) {
            return false;
        }
            
        ImportConfigurationDefinition o = (ImportConfigurationDefinition) other;
        
        return synchronizerClassName.equals(o.getSynchronizerClassName());        
    }
}
