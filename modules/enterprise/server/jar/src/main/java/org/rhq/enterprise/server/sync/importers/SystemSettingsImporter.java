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

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.sync.entity.SystemSettings;
import org.rhq.enterprise.server.sync.NoSingleEntity;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class SystemSettingsImporter implements Importer<NoSingleEntity, SystemSettings> {

    /* (non-Javadoc)
     * @see org.rhq.enterprise.server.sync.importers.Importer#init(org.rhq.core.domain.auth.Subject)
     */
    @Override
    public void init(Subject subject) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.rhq.enterprise.server.sync.importers.Importer#getExportedEntityMatcher(javax.persistence.EntityManager)
     */
    @Override
    public ExportedEntityMatcher<NoSingleEntity, SystemSettings> getExportedEntityMatcher(EntityManager entityManager) {
        return new NoSingleEntityMatcher<SystemSettings>();
    }

    /* (non-Javadoc)
     * @see org.rhq.enterprise.server.sync.importers.Importer#update(java.lang.Object, java.lang.Object, javax.persistence.EntityManager)
     */
    @Override
    public void update(NoSingleEntity entity, SystemSettings exportedEntity, EntityManager entityManager) {
        // TODO Auto-generated method stub
        
    }

    
}
