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
import org.rhq.enterprise.server.sync.exporters.SystemSettingsExporter;
import org.rhq.enterprise.server.sync.importers.Importer;
import org.rhq.enterprise.server.sync.importers.SystemSettingsImporter;
import org.rhq.enterprise.server.sync.validators.ConsistencyValidator;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class SystemSettingsSynchronizer implements Synchronizer<NoSingleEntity, SystemSettings> {

    private Subject subject;
    private SystemManagerLocal systemManager;

    public SystemSettingsSynchronizer() {
        this(LookupUtil.getSystemManager());
    }
    
    public SystemSettingsSynchronizer(SystemManagerLocal systemManager) {
        this.systemManager = systemManager;
    }
    
    @Override
    public void initialize(Subject subject, EntityManager entityManager) {
        this.subject = subject;
    }
    
    public Exporter<NoSingleEntity, SystemSettings> getExporter() {
        return new SystemSettingsExporter(subject, systemManager);
    }

    public Importer<NoSingleEntity, SystemSettings> getImporter() {
        return new SystemSettingsImporter(subject, systemManager);
    }

    public Set<ConsistencyValidator> getRequiredValidators() {
        return Collections.emptySet();
    }

}
