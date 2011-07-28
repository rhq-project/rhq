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

import org.rhq.core.domain.configuration.Configuration;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class ImporterConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

    private String importerClassName;
    private Configuration configuration;
    
    public ImporterConfiguration() {
        
    }
    
    public ImporterConfiguration(String importerClassName, Configuration configuration) {
        this.importerClassName = importerClassName;
        this.configuration = configuration;
    }

    /**
     * @return the importerClassName
     */
    public String getImporterClassName() {
        return importerClassName;
    }

    /**
     * @param importerClassName the importerClassName to set
     */
    public void setImporterClassName(String importerClassName) {
        this.importerClassName = importerClassName;
    }

    /**
     * @return the configuration
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * @param configuration the configuration to set
     */
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
    
    @Override
    public int hashCode() {
        return importerClassName.hashCode();
    }
    
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        
        if (!(other instanceof ImporterConfiguration)) {
            return false;
        }
            
        ImporterConfiguration o = (ImporterConfiguration) other;
        
        return importerClassName.equals(o.getImporterClassName());        
    }
}
