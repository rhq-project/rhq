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

import java.util.Collections;
import java.util.Set;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.sync.entity.SystemSettings;
import org.rhq.enterprise.server.sync.exporters.Exporter;
import org.rhq.enterprise.server.sync.exporters.ExportingIterator;
import org.rhq.enterprise.server.sync.exporters.MetricTemplatesExporter;
import org.rhq.enterprise.server.sync.exporters.SystemSettingsExporter;
import org.rhq.enterprise.server.sync.validators.ConsistencyValidator;

/**
 * 
 *
 * @author Lukas Krejci
 */
public enum SynchronizedEntity {
    
    /*
    SUBJECT {
        @Override
        public Exporter<?, ?> getExporter(Subject subject) {            
            return DummyExporter.create(Subject.class);
        }
    },
    ROLE {
        @Override
        public Exporter<?, ?> getExporter(Subject subject) {
            return DummyExporter.create(Role.class);
        }
    },
    GROUP {
        @Override
        public Exporter<?, ?> getExporter(Subject subject) {
            return DummyExporter.create(Group.class);
        }
    },
    ALERT_TEMPLATE {
        @Override
        public Exporter<?, ?> getExporter(Subject subject) {
            return DummyExporter.create(AlertDefinition.class);
        }
    },
    */
    METRIC_TEMPLATE {
        @Override
        public Exporter<?, ?> getExporter(Subject subject) {
            return new MetricTemplatesExporter(subject);
        }
    },
    SYSTEM_SETTINGS {
        @Override
        public Exporter<?, ?> getExporter(Subject subject) {
            return new SystemSettingsExporter(subject);
        }  
    };
    
    public static class DummyExporter<T> implements Exporter<T, T> {

        private Class<T> clazz;
        
        public static <U> DummyExporter<U> create(Class<U> clazz) {
            return new DummyExporter<U>(clazz);
        }
        
        public DummyExporter(Class<T> clazz) {
            this.clazz = clazz;
        }

        @Override
        public Set<ConsistencyValidator> getRequiredValidators() {
            return Collections.emptySet();
        }
        
        @Override
        public Class<T> getExportedEntityType() {            
            return clazz;
        }
        
        @Override
        public void init() throws ExportException {
            throw new ExportException("Export not implemented for type " + clazz.getName());
        }

        @Override
        public String getNotes() {
            return null;
        }

        @Override
        public ExportingIterator<T> getExportingIterator() {
            return null;
        }
        
    }
    
    /**
     * Gets an exporter for given subsystem.
     * 
     * @param subject the currently logged on user.
     * @return
     */
    public abstract Exporter<?, ?> getExporter(Subject subject);
}
