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

import javax.persistence.EntityManager;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.sync.entity.SystemSettings;
import org.rhq.enterprise.server.sync.exporters.Exporter;
import org.rhq.enterprise.server.sync.exporters.ExportingIterator;
import org.rhq.enterprise.server.sync.exporters.MetricTemplateExporter;
import org.rhq.enterprise.server.sync.exporters.SystemSettingsExporter;
import org.rhq.enterprise.server.sync.importers.ExportedEntityMatcher;
import org.rhq.enterprise.server.sync.importers.Importer;
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
        public Exporter<?, ?> getExporter() {
            return new MetricTemplateExporter();
        }
    },
    SYSTEM_SETTINGS {
        @Override
        public Exporter<?, ?> getExporter() {
            return new SystemSettingsExporter();
        }  
    };
    
    public static class DummyExporter<T> implements Exporter<T, T> {

        private Class<? extends Importer<T, T>> importerClass;
        
        public static <U> DummyExporter<U> create(Class<? extends Importer<U, U>> clazz) {
            return new DummyExporter<U>(clazz);
        }
        
        public DummyExporter(Class<? extends Importer<T, T>> clazz) {
            this.importerClass = clazz;
        }

        @Override
        public Set<ConsistencyValidator> getRequiredValidators() {
            return Collections.emptySet();
        }
        
        @Override
        public Class<? extends Importer<T, T>> getImporterType() {            
            return importerClass;
        }
        
        @Override
        public void init(Subject subject) throws ExportException {
            throw new ExportException("Export not implemented for type " + importerClass.getName());
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
    
    public static class DummyImporter<T> implements Importer<T, T> {

        @Override
        public void init(Subject subject) {
        }

        @Override
        public ExportedEntityMatcher<T, T> getExportedEntityMatcher(EntityManager entityManager) {
            return null;
        }

        @Override
        public void update(T entity, T exportedEntity, EntityManager entityManager) {
        }        
    }
    
    /**
     * Returns an exporter for given subsystem.
     */
    public abstract Exporter<?, ?> getExporter();
}
