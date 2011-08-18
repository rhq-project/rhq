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
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.xml.stream.XMLStreamException;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.sync.entity.MetricTemplate;
import org.rhq.enterprise.server.sync.ExportReader;
import org.rhq.enterprise.server.sync.ExportWriter;
import org.rhq.enterprise.server.sync.ValidationException;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class MetricTemplateValidator implements ConsistencyValidator {

    private EntityManager entityManager;
    private Set<MetricTemplate> allMeasurementDefinitions;
    private Set<MetricTemplate> alreadyCheckedTemplates;
    private boolean dataInitialized = false;
    
    @Override
    public void initialize(Subject subject, EntityManager entityManager) {
        this.entityManager = entityManager;
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
        return Collections.<Class<?>> singleton(MetricTemplate.class);
    }

    @Override
    public void validateExportedEntity(Object entity) throws ValidationException {
        initializeData();
        
        MetricTemplate template = (MetricTemplate) entity;

        if (!allMeasurementDefinitions.contains(template)) {
            throw new ValidationException(
                "The metric template "
                    + template
                    + " does not have any corresponding metric template in the database. This either means that the source of the export had different plugins installed, the export file has been manually edited to contain this inconsistency or that the database is corrupted.");
        }
        
        if (!alreadyCheckedTemplates.add(template)) {
            throw new ValidationException("Was the export file manually updated? The metric template " + template + " has been seen multiple times.");
        }
    }

    private void initializeData() {
        if (dataInitialized) {
            return;
        }
        
        allMeasurementDefinitions = new HashSet<MetricTemplate>();
        alreadyCheckedTemplates = new HashSet<MetricTemplate>();
        
        Query q = entityManager.createQuery("SELECT md FROM MeasurementDefinition md");

        for (Object r : q.getResultList()) {
            MeasurementDefinition md = (MeasurementDefinition) r;

            allMeasurementDefinitions.add(new MetricTemplate(md));
        }
        
        dataInitialized = true;
    }
    
    @Override
    public int hashCode() {
        return 0;
    }
    
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        
        return other instanceof MetricTemplateValidator;
    }
}
