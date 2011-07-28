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

import javax.persistence.EntityManager;
import javax.xml.stream.XMLStreamException;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.enterprise.server.sync.ExportReader;
import org.rhq.enterprise.server.sync.NoSingleEntity;
import org.rhq.enterprise.server.sync.importers.ExportedEntityMatcher;
import org.rhq.enterprise.server.sync.importers.Importer;
import org.rhq.enterprise.server.sync.importers.NoSingleEntityMatcher;

public class DummyImporter<T> implements Importer<NoSingleEntity, T> {

    @Override
    public ConfigurationDefinition getImportConfigurationDefinition() {
        return new ConfigurationDefinition("dummy", "dummy");
    }
    
    @Override
    public void init(Subject subject, EntityManager entityManager, Configuration configuration) {
    }

    @Override
    public ExportedEntityMatcher<NoSingleEntity, T> getExportedEntityMatcher() {
        return new NoSingleEntityMatcher<T>();
    }

    @Override
    public void update(NoSingleEntity entity, T exportedEntity) {
    }        
    
    @Override
    public void finishImport() {
    }
    
    @Override
    public T unmarshallExportedEntity(ExportReader reader) throws XMLStreamException {
        return null;
    }
}