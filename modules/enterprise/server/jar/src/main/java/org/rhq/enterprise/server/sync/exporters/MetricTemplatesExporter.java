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

package org.rhq.enterprise.server.sync.exporters;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.MeasurementDefinitionCriteria;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.sync.entity.MetricTemplate;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.sync.ExportException;
import org.rhq.enterprise.server.sync.ExportWriter;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class MetricTemplatesExporter implements Exporter<MetricTemplate> {

    private MeasurementDefinitionManagerLocal measurementDefinitionManager = LookupUtil.getMeasurementDefinitionManager();
    private ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
    private Subject subject;

    private class MetricTemplateIterator implements ExportingIterator<MetricTemplate> {

        private Iterator<ResourceType> allResourceTypesIterator;
        private Iterator<MeasurementDefinition> currentResourceTypeDefinitionsIterator;
        private MetricTemplate current;
        private Marshaller marshaller;

        public MetricTemplateIterator(Iterator<ResourceType> allResourceTypesIterator) {
            this.allResourceTypesIterator = allResourceTypesIterator;
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(MetricTemplate.class);
                marshaller = jaxbContext.createMarshaller();
            } catch (JAXBException e) {
                throw new IllegalStateException("Could not create a JAXB marshaller for serializing metric templates.",
                    e);
            }
        }

        @Override
        public boolean hasNext() {
            if (currentResourceTypeDefinitionsIterator != null && currentResourceTypeDefinitionsIterator.hasNext()) {
                return true;
            } else {
                readNextSchedules();
                return currentResourceTypeDefinitionsIterator != null && currentResourceTypeDefinitionsIterator.hasNext();
            }
        }

        @Override
        public MetricTemplate next() {
            if (currentResourceTypeDefinitionsIterator == null) {
                readNextSchedules();
            }

            if (currentResourceTypeDefinitionsIterator == null) {
                throw new NoSuchElementException();
            }

            current = new MetricTemplate(currentResourceTypeDefinitionsIterator.next());

            return current;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void export(ExportWriter output) throws XMLStreamException {
            try {
                marshaller.marshal(current, output);
            } catch (JAXBException e) {
                throw new XMLStreamException("Failed to export a metric template.", e);
            }
        }

        @Override
        public String getNotes() {
            return null;
        }

        private void readNextSchedules() {
            if (!allResourceTypesIterator.hasNext()) {
                currentResourceTypeDefinitionsIterator = null;
                return;
            }

            ResourceType resourceType = allResourceTypesIterator.next();

            MeasurementDefinitionCriteria criteria = new MeasurementDefinitionCriteria();
            criteria.addFilterResourceTypeId(resourceType.getId());
            List<MeasurementDefinition> defs = measurementDefinitionManager.findMeasurementDefinitionsByCriteria(subject, criteria);

            currentResourceTypeDefinitionsIterator = defs.iterator();
        }
    };

    public MetricTemplatesExporter(Subject subject) {
        this.subject = subject;
    }

    @Override
    public Class<MetricTemplate> exportedEntityType() {
        return MetricTemplate.class;
    }

    @Override
    public void init() throws ExportException {
    }

    @Override
    public ExportingIterator<MetricTemplate> getExportingIterator() {
        List<ResourceType> allResourceTypes = null;

        //TODO find all the resource types

        return new MetricTemplateIterator(allResourceTypes.iterator());
    }

    @Override
    public String getNotes() {
        return null;
    }

}
