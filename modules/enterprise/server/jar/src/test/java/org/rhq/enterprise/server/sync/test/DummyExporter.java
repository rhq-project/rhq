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

package org.rhq.enterprise.server.sync.test;

import java.util.Collections;
import java.util.Set;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.sync.ExportException;
import org.rhq.enterprise.server.sync.NoSingleEntity;
import org.rhq.enterprise.server.sync.exporters.Exporter;
import org.rhq.enterprise.server.sync.exporters.ExportingIterator;
import org.rhq.enterprise.server.sync.importers.Importer;
import org.rhq.enterprise.server.sync.validators.ConsistencyValidator;

public class DummyExporter<T> implements Exporter<NoSingleEntity, T> {

    private Class<? extends Importer<NoSingleEntity, T>> importerClass;
    
    public static <U> DummyExporter<U> create(Class<? extends Importer<NoSingleEntity, U>> clazz) {
        return new DummyExporter<U>(clazz);
    }
    
    public DummyExporter(Class<? extends Importer<NoSingleEntity, T>> clazz) {
        this.importerClass = clazz;
    }

    @Override
    public Set<ConsistencyValidator> getRequiredValidators() {
        return Collections.emptySet();
    }
    
    @Override
    public Class<? extends Importer<NoSingleEntity, T>> getImporterType() {            
        return importerClass;
    }
    
    @Override
    public void init(Subject subject) throws ExportException {
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