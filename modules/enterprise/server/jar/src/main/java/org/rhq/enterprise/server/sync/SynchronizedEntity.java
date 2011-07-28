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

package org.rhq.enterprise.server.sync;

import org.rhq.enterprise.server.sync.exporters.Exporter;
import org.rhq.enterprise.server.sync.exporters.MetricTemplateExporter;
import org.rhq.enterprise.server.sync.exporters.SystemSettingsExporter;
import org.rhq.enterprise.server.sync.importers.Importer;
import org.rhq.enterprise.server.sync.importers.MetricTemplateImporter;
import org.rhq.enterprise.server.sync.importers.SystemSettingsImporter;

/**
 * The enum of all possible synchronized entities. 
 *
 * @author Lukas Krejci
 */
public enum SynchronizedEntity {
    
    /*
    SUBJECT {
        @Override
        public Exporter<?, ?> getExporter(Subject subject) {            
            return ???;
        }
    },
    ROLE {
        @Override
        public Exporter<?, ?> getExporter(Subject subject) {
            return ???;
        }
    },
    GROUP {
        @Override
        public Exporter<?, ?> getExporter(Subject subject) {
            return ???;
        }
    },
    ALERT_TEMPLATE {
        @Override
        public Exporter<?, ?> getExporter(Subject subject) {
            return ???;
        }
    },
    */
    METRIC_TEMPLATE {
        @Override
        public Exporter<?, ?> getExporter() {
            return new MetricTemplateExporter();
        }
        
        @Override
        public Importer<?, ?> getImporter() {
            return new MetricTemplateImporter();
        }
    },
    SYSTEM_SETTINGS {
        @Override
        public Exporter<?, ?> getExporter() {
            return new SystemSettingsExporter();
        }  
        
        @Override
        public Importer<?, ?> getImporter() {
            return new SystemSettingsImporter();
        }
    };
    
    /**
     * Returns an exporter for given subsystem.
     */
    public abstract Exporter<?, ?> getExporter();
    
    /**
     * Returns the importer for given subsystem.
     * @return
     */
    public abstract Importer<?, ?> getImporter();
}
