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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.sync.entity.MetricTemplate;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.resource.metadata.PluginManagerLocal;
import org.rhq.enterprise.server.sync.exporters.Exporter;
import org.rhq.enterprise.server.sync.exporters.MetricTemplateExporter;
import org.rhq.enterprise.server.sync.importers.Importer;
import org.rhq.enterprise.server.sync.importers.MetricTemplateImporter;
import org.rhq.enterprise.server.sync.validators.ConsistencyValidator;
import org.rhq.enterprise.server.sync.validators.DeployedAgentPluginsValidator;
import org.rhq.enterprise.server.sync.validators.MetricTemplateValidator;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class MetricTemplateSynchronizer implements Synchronizer<MeasurementDefinition, MetricTemplate> {

    private Subject subject;
    private EntityManager entityManager;
    private MeasurementDefinitionManagerLocal measurementDefinitionManager;
    private MeasurementScheduleManagerLocal measurementScheduleManager;
    private PluginManagerLocal pluginManager;

    public MetricTemplateSynchronizer() {
        this(LookupUtil.getMeasurementDefinitionManager(), LookupUtil.getMeasurementScheduleManager(), LookupUtil.getPluginManager());
    }

    public MetricTemplateSynchronizer(MeasurementDefinitionManagerLocal measurementDefinitionManager,
        MeasurementScheduleManagerLocal measurementScheduleManager, PluginManagerLocal pluginManager) {
        this.measurementDefinitionManager = measurementDefinitionManager;
        this.measurementScheduleManager = measurementScheduleManager;
        this.pluginManager = pluginManager;
    }

    @Override
    public void initialize(Subject subject, EntityManager entityManager) {
        this.subject = subject;
        this.entityManager = entityManager;
    }

    public Exporter<MeasurementDefinition, MetricTemplate> getExporter() {
        return new MetricTemplateExporter(subject, measurementDefinitionManager);
    }

    public Importer<MeasurementDefinition, MetricTemplate> getImporter() {
        return new MetricTemplateImporter(subject, entityManager, measurementScheduleManager);
    }

    public Set<ConsistencyValidator> getRequiredValidators() {
        HashSet<ConsistencyValidator> ret = new HashSet<ConsistencyValidator>();
        ret.add(new DeployedAgentPluginsValidator(pluginManager));
        ret.add(new MetricTemplateValidator());
        return ret;
    }

}
