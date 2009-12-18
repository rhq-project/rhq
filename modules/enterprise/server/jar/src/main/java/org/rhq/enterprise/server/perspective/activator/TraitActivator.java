/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.perspective.activator;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.perspective.activator.context.AbstractResourceOrGroupActivationContext;
import org.rhq.enterprise.server.perspective.activator.context.GroupActivationContext;
import org.rhq.enterprise.server.perspective.activator.context.ResourceActivationContext;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Ian Springer
 */
public class TraitActivator extends AbstractResourceOrGroupActivator {
    static final long serialVersionUID = 1L;

    private String name;
    private Pattern value;

    public TraitActivator(String name, Pattern value) {
        this.name = name;
        this.value = value;
    }

    public boolean matches(AbstractResourceOrGroupActivationContext context) {
        if (context instanceof ResourceActivationContext) {
            ResourceActivationContext resourceContext = (ResourceActivationContext)context;
            Resource resource = resourceContext.getResource();
            ResourceType resourceType = context.getResourceType();
            Set<MeasurementDefinition> measurementDefinitions = resourceType.getMetricDefinitions();
            MeasurementDefinition measurementDefinition = getMeasurementDefiniton(measurementDefinitions);
            if (measurementDefinition == null) {
                // No such trait.
                // TODO: Mistake in descriptor error - log an error.
                return false;
            }
            if (measurementDefinition.getDataType() != DataType.TRAIT) {
                // Measurement isn't a trait.
                // TODO: Mistake in descriptor error - log an error.
                return false;
            }

            MeasurementScheduleManagerLocal measurementScheduleManager = LookupUtil.getMeasurementScheduleManager();

            Subject subject = context.getSubject();
            MeasurementSchedule schedule = measurementScheduleManager.getSchedule(subject, resource.getId(),
                    measurementDefinition.getId(), false);
            MeasurementDataManagerLocal measurementDataManager = LookupUtil.getMeasurementDataManager();
            MeasurementDataTrait trait = measurementDataManager.getCurrentTraitForSchedule(schedule.getId());
            String value = trait.getValue();
            if (value == null) {
                return false;
            }
            Matcher matcher = this.value.matcher(value);
            return matcher.matches();
        } else if (context instanceof GroupActivationContext) {
            GroupActivationContext groupContext = (GroupActivationContext)context;
            ResourceGroup group = groupContext.getGroup();
            // TODO: Finish this.
            return true;
        } else {
            return false;
        }
    }

    private MeasurementDefinition getMeasurementDefiniton(Set<MeasurementDefinition> measurementDefinitions) {
        for (MeasurementDefinition measurementDefinition : measurementDefinitions) {
            if (measurementDefinition.getName().equals(this.name)) {
                return measurementDefinition;
            }
        }
        return null;
    }
}