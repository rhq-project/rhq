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
package org.rhq.enterprise.server.measurement;

import java.util.Arrays;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.enterprise.server.RHQConstants;

/**
 * A manager for {@link MeasurementDefinition}s.
 */
@Stateless
public class MeasurementDefinitionManagerBean implements MeasurementDefinitionManagerLocal {
    private Log log = LogFactory.getLog(MeasurementDefinitionManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    EntityManager entityManager;

    @EJB
    MeasurementDataManagerLocal measurementDataManager;

    @EJB
    MeasurementScheduleManagerLocal scheduleManager;

    public MeasurementDefinition getMeasurementDefinitionById(Subject subject, int id) {
        // TODO: AUTHZ CHECK
        MeasurementDefinition definition = entityManager.find(MeasurementDefinition.class, id);
        return definition;
    }

    // needed because schedule->definition is a lazy relationship
    public MeasurementDefinition getMeasurementDefinitionByScheduleId(Subject subject, int id) {
        // TODO: AUTHZ CHECK
        MeasurementSchedule schedule = entityManager.find(MeasurementSchedule.class, id);

        if (schedule == null) {
            return null;
        }

        schedule.getDefinition().getId(); // because definition is lazily loaded

        return schedule.getDefinition();
    }

    /**
     * Remove the given definition with its attached schedules and MeasurementData
     *
     * @param def the MeasuremendDefinition to delete
     */
    public void removeMeasurementDefinition(MeasurementDefinition def) {
        // get the schedules and unschedule them on the agents
        List<MeasurementSchedule> schedules = def.getSchedules();
        scheduleManager.sendAgentUnschedules(schedules);

        // remove the measurement data
        measurementDataManager.removeGatheredMetricsForSchedules(schedules);

        // remove the schedules 
        for (MeasurementSchedule sched : schedules) {
            entityManager.remove(sched);
        }

        // finally remove the definition itself
        try {
            if ((def.getId() != 0) && entityManager.contains(def)) {
                entityManager.remove(def);
            }
        } catch (EntityNotFoundException enfe) {
            if (log.isDebugEnabled()) {
                log.debug("Definition # " + def.getId() + " not found: " + enfe.getMessage());
            }
        } catch (PersistenceException pe) {
            if (log.isDebugEnabled()) {
                log.debug("Exception when deleting Definition # " + def.getId() + ": " + pe.getMessage());
            }
        } catch (Exception e) {
            log.warn(e.fillInStackTrace());
        }
    }

    @SuppressWarnings("unchecked")
    public List<MeasurementDefinition> getMeasurementDefinitionsByResourceType(Subject user, int resourceTypeId,
        DataType dataType, DisplayType displayType) {
        Query query = entityManager
            .createNamedQuery(MeasurementDefinition.FIND_BY_RESOURCE_TYPE_DATA_TYPE_DISPLAY_TYPE);
        query.setParameter("resourceTypeId", resourceTypeId);
        query.setParameter("dataType", dataType);
        query.setParameter("displayType", displayType);

        List<MeasurementDefinition> results = query.getResultList();
        return results;
    }

    public List<MeasurementDefinition> getMeasurementDefinitionsByIds(Subject subject,
        Integer[] measurementDefinitionIds) {
        Query query = entityManager.createNamedQuery(MeasurementDefinition.FIND_BY_IDS);
        List<Integer> ids = Arrays.asList(measurementDefinitionIds);
        query.setParameter("ids", ids);

        List<MeasurementDefinition> results = query.getResultList();
        return results;
    }
}