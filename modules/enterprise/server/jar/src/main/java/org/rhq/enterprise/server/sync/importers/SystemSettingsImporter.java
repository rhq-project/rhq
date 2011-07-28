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

package org.rhq.enterprise.server.sync.importers;

import javax.persistence.EntityManager;
import javax.xml.stream.XMLStreamException;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.sync.entity.SystemSettings;
import org.rhq.enterprise.server.sync.ExportReader;
import org.rhq.enterprise.server.sync.NoSingleEntity;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class SystemSettingsImporter implements Importer<NoSingleEntity, SystemSettings> {

    /* (non-Javadoc)
     * @see org.rhq.enterprise.server.sync.importers.Importer#getImportConfigurationDefinition()
     */
    @Override
    public ConfigurationDefinition getImportConfigurationDefinition() {
        return new ConfigurationDefinition("SystemSettingsConfiguration", "TBD");
    }

    /* (non-Javadoc)
     * @see org.rhq.enterprise.server.sync.importers.Importer#init(org.rhq.core.domain.auth.Subject, javax.persistence.EntityManager, org.rhq.core.domain.configuration.Configuration)
     */
    @Override
    public void init(Subject subject, EntityManager entityManager, Configuration importConfiguration) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.rhq.enterprise.server.sync.importers.Importer#getExportedEntityMatcher()
     */
    @Override
    public ExportedEntityMatcher<NoSingleEntity, SystemSettings> getExportedEntityMatcher() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.rhq.enterprise.server.sync.importers.Importer#update(java.lang.Object, java.lang.Object)
     */
    @Override
    public void update(NoSingleEntity entity, SystemSettings exportedEntity) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.rhq.enterprise.server.sync.importers.Importer#unmarshallExportedEntity(org.rhq.enterprise.server.sync.ExportReader)
     */
    @Override
    public SystemSettings unmarshallExportedEntity(ExportReader reader) throws XMLStreamException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.rhq.enterprise.server.sync.importers.Importer#finishImport()
     */
    @Override
    public void finishImport() {
        // TODO Auto-generated method stub
        
    }


    
}
