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
import java.util.Iterator;
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
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.QueryGenerator;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.exception.FetchException;

/**
 * A manager for {@link MeasurementDefinition}s.
 */
@Stateless
public class MeasurementDefinitionManagerBean implements MeasurementDefinitionManagerLocal {
    private Log log = LogFactory.getLog(MeasurementDefinitionManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private MeasurementOOBManagerLocal oobManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    public MeasurementDefinition getMeasurementDefinition(Subject subject, int definitionId) {
        // no authz check, this is basically interrogating a plugin, logged in auth should be enough
        MeasurementDefinition definition = entityManager.find(MeasurementDefinition.class, definitionId);
        return definition;
    }

    /**
     * Remove the given definition with its attached schedules.
     *
     * @param def the MeasuremendDefinition to delete
     */
    public void removeMeasurementDefinition(MeasurementDefinition def) {
        long now = System.currentTimeMillis();

        // get the schedules and unschedule them on the agents
        List<MeasurementSchedule> schedules = def.getSchedules();

        // remove the schedules
        Iterator<MeasurementSchedule> schedIter = schedules.iterator();
        while (schedIter.hasNext()) {
            MeasurementSchedule sched = schedIter.next();
            if (sched.getBaseline() != null) {
                entityManager.remove(sched.getBaseline());
                sched.setBaseline(null);
            }
            oobManager.removeOOBsForSchedule(subjectManager.getOverlord(), sched);
            sched.getResource().setMtime(now); // changing MTime tells the agent this resource needs to be synced
            entityManager.remove(sched);
            schedIter.remove();
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

    @SuppressWarnings("unchecked")
    public List<MeasurementDefinition> getMeasurementDefinitionsByIds(Subject subject,
        Integer[] measurementDefinitionIds) {
        Query query = entityManager.createNamedQuery(MeasurementDefinition.FIND_BY_IDS);
        List<Integer> ids = Arrays.asList(measurementDefinitionIds);
        query.setParameter("ids", ids);

        List<MeasurementDefinition> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    @Override
    public PageList<MeasurementDefinition> findMeasurementDefinitions(Subject subject, MeasurementDefinition criteria,
        PageControl pc) throws FetchException {

        try {
            QueryGenerator generator = new QueryGenerator(criteria, pc);

            Query query = generator.getQuery(entityManager);
            Query countQuery = generator.getCountQuery(entityManager);

            long count = (Long) countQuery.getSingleResult();
            List<MeasurementDefinition> alertDefinitions = query.getResultList();

            return new PageList<MeasurementDefinition>(alertDefinitions, (int) count, pc);
        } catch (Exception e) {
            throw new FetchException(e.getMessage());
        }

    }
}