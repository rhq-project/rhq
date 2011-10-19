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

package org.rhq.enterprise.server.sync.validators;

import java.util.Collections;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.xml.stream.XMLStreamException;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.sync.entity.SystemSettings;
import org.rhq.enterprise.server.sync.ExportReader;
import org.rhq.enterprise.server.sync.ExportWriter;
import org.rhq.enterprise.server.sync.ValidationException;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class SystemSettingsValidator implements ConsistencyValidator {

    private Subject subject;
    private SystemManagerLocal systemManager;
    
    public SystemSettingsValidator() {
        this(LookupUtil.getSystemManager());
    }
    
    public SystemSettingsValidator(SystemManagerLocal systemManager) {
        this.systemManager = systemManager;
    }
    
    @Override
    public void initialize(Subject subject, EntityManager entityManager) {
        this.subject = subject;
    }
    
    @Override
    public void exportState(ExportWriter writer) throws XMLStreamException {
    }

    @Override
    public void initializeExportedStateValidation(ExportReader reader) throws XMLStreamException {
    }

    @Override
    public void validateExportedState() throws InconsistentStateException {
    }

    @Override
    public Set<Class<?>> getValidatedEntityTypes() {
        return Collections.<Class<?>>singleton(SystemSettings.class);
    }

    @Override
    public void validateExportedEntity(Object entity) throws ValidationException {
        if (entity instanceof SystemSettings) {
            try {
                systemManager.validateSystemConfiguration(subject, ((SystemSettings)entity).toProperties());
            } catch (Exception e) {
                throw new ValidationException("The system settings failed to validate: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public int hashCode() {
        return 0; //all system settings validators are equal
    }

    @Override
    public boolean equals(Object obj) {
        //all system settings validators are equal
        return obj instanceof SystemSettingsValidator;
    }
}
