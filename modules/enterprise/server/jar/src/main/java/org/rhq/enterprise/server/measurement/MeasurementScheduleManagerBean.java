/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.H2DatabaseType;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.db.SQLServerDatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.MeasurementDefinitionCriteria;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.measurement.composite.MeasurementScheduleComposite;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.authz.RequiredPermissions;
import org.rhq.enterprise.server.common.PerformanceMonitorInterceptor;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A manager for {@link MeasurementSchedule}s.
 *
 * @author Heiko W. Rupp
 * @author Ian Springer
 * @author Joseph Marques
 */
@Stateless
@javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
@Interceptors(PerformanceMonitorInterceptor.class)
public class MeasurementScheduleManagerBean implements MeasurementScheduleManagerLocal,
    MeasurementScheduleManagerRemote {
    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
    private DataSource dataSource;

    @EJB
    //@IgnoreDependency
    private AgentManagerLocal agentManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @EJB
    private ResourceManagerLocal resourceManager;

    @EJB
    //@IgnoreDependency
    private ResourceGroupManagerLocal resourceGroupManager;

    @EJB
    private MeasurementScheduleManagerLocal measurementScheduleManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    private final Log log = LogFactory.getLog(MeasurementScheduleManagerBean.class);

    public Set<ResourceMeasurementScheduleRequest> findSchedulesForResourceAndItsDescendants(int[] resourceIds,
        boolean getDescendents) {
        Set<ResourceMeasurementScheduleRequest> allSchedules = new HashSet<ResourceMeasurementScheduleRequest>();
        getSchedulesForResourceAndItsDescendants(resourceIds, allSchedules, getDescendents);
        return allSchedules;
    }

    /**
     * Get the AgentClient (the connection to the agent) for a certain Schedule
     *
     * @param  sched A MeasurementSchedule for which we need a connection to the Agent
     *
     * @return an AgentClient to communicate with the Agent
     */
    public AgentClient getAgentClientForSchedule(MeasurementSchedule sched) {
        Resource pRes = sched.getResource();

        // Get agent and open a connection to it
        Agent agent = pRes.getAgent();
        AgentClient ac = agentManager.getAgentClient(agent);
        return ac;
    }

    /**
     * Returns a MeasurementSchedule by its primary key or null.
     *
     * @param  scheduleId the id of the desired schedule
     *
     * @return The MeasurementSchedule or null if not found
     */
    public MeasurementSchedule getScheduleById(int scheduleId) {
        MeasurementSchedule ms;
        try {
            ms = entityManager.find(MeasurementSchedule.class, scheduleId);
        } catch (NoResultException n) {
            ms = null;
        }

        return ms;
    }

    /**
     * Return a list of MeasurementSchedules for the given ids
     *
     * @param  scheduleIds PrimaryKeys of the schedules searched
     *
     * @return a list of Schedules
     */
    @SuppressWarnings("unchecked")
    public List<MeasurementSchedule> findSchedulesByIds(int[] scheduleIds) {
        if (scheduleIds == null || scheduleIds.length == 0) {
            return new ArrayList<MeasurementSchedule>();
        }

        Query q = entityManager.createNamedQuery(MeasurementSchedule.FIND_BY_IDS);
        q.setParameter("ids", ArrayUtils.wrapInList(scheduleIds));
        List<MeasurementSchedule> ret = q.getResultList();
        return ret;
    }

    /**
     * Obtain a MeasurementSchedule by its Id after a check for a valid session
     *
     * @param  subject a session id that must be valid
     * @param  scheduleId The primary key of the Schedule
     *
     * @return a MeasurementSchedule or null, if there is 
     */
    public MeasurementSchedule getScheduleById(Subject subject, int scheduleId) {
        MeasurementSchedule schedule = entityManager.find(MeasurementSchedule.class, scheduleId);
        if (schedule == null) {
            return null;
        }

        // the auth check eagerly loads the resource
        if (authorizationManager.canViewResource(subject, schedule.getResource().getId()) == false) {
            throw new PermissionException("User[" + subject.getName()
                + "] does not have permission to view measurementSchedule[id=" + scheduleId + "]");
        }

        // and this eagerly loads the definition
        schedule.getDefinition().getId();
        return schedule;
    }

    /**
     * <p>Ensures the collection interval is valid by increasing it to the minimum if necessary.</p>
     * <p>Be careful not to call this for template enable/disable, because that uses special values for the
     * interval.</p> 
    
     * @param schedule
     */
    private void verifyMinimumCollectionInterval(MeasurementSchedule schedule) {
        schedule.setInterval(verifyMinimumCollectionInterval(schedule.getInterval()));
    }

    /**
     * <p>Ensures the collection interval is valid by increasing it to the minimum if necessary.</p>
     * <p>Be careful not to call this for template enable/disable, because that uses special values for the
     * interval.</p> 
     *  
     * @param collectionInterval
     * @return valid interval
     */
    private long verifyMinimumCollectionInterval(long collectionInterval) {
        long validCollectionInterval = collectionInterval;

        if (collectionInterval < MeasurementConstants.MINIMUM_COLLECTION_INTERVAL_MILLIS) {
            validCollectionInterval = MeasurementConstants.MINIMUM_COLLECTION_INTERVAL_MILLIS;
        }

        return validCollectionInterval;
    }

    /**
     * Find MeasurementSchedules that are attached to a certain definition and some resources
     *
     * @param  subject      A subject that must be valid
     * @param  definitionId The primary key of a MeasurementDefinition
     * @param  resourceIds  primary of Resources wanted
     *
     * @return a List of MeasurementSchedules
     */
    public List<MeasurementSchedule> findSchedulesByResourceIdsAndDefinitionId(Subject subject, int[] resourceIds,
        int definitionId) {
        return findSchedulesByResourcesAndDefinitions(subject, resourceIds, new int[] { definitionId });
    }

    @SuppressWarnings("unchecked")
    public List<MeasurementSchedule> findSchedulesByResourceIdsAndDefinitionIds(int[] resourceIds, int[] definitionIds) {
        Query query = entityManager.createNamedQuery(MeasurementSchedule.FIND_BY_RESOURCE_IDS_AND_DEFINITION_IDS);
        query.setParameter("definitionIds", ArrayUtils.wrapInList(definitionIds));
        query.setParameter("resourceIds", ArrayUtils.wrapInList(resourceIds));
        List<MeasurementSchedule> results = query.getResultList();
        return results;
    }

    private List<MeasurementSchedule> findSchedulesByResourcesAndDefinitions(Subject subject, int[] resourceIds,
        int[] definitionIds) {
        for (int resourceId : resourceIds) {
            if (!authorizationManager.canViewResource(subject, resourceId)) {
                throw new PermissionException("User[" + subject.getName()
                    + "] does not have permission to view metric schedules for resource[id=" + resourceId + "]");
            }
        }

        return findSchedulesByResourceIdsAndDefinitionIds(resourceIds, definitionIds);
    }

    /**
     * Find MeasurementSchedules that are attached to a certain definition and a resource
     *
     * @param  subject
     * @param  definitionId   The primary key of a MeasurementDefinition
     * @param  resourceId     the id of the resource
     * @param  attachBaseline baseline won't be attached to the schedule by default do to LAZY annotation on the managed
     *                        relationship. attachBaseline, if true, will eagerly load it for the caller
     *
     * @return the MeasurementSchedule of the given definition for the given resource
     */
    public MeasurementSchedule getSchedule(Subject subject, int resourceId, int definitionId, boolean attachBaseline)
        throws MeasurementNotFoundException {
        try {
            List<MeasurementSchedule> results = findSchedulesByResourcesAndDefinitions(subject,
                new int[] { resourceId }, new int[] { definitionId });
            if (results.size() != 1) {
                throw new MeasurementException("Could not find measurementSchedule[resourceId=" + resourceId
                    + ", definitionId=" + definitionId + "]");
            }

            MeasurementSchedule schedule = results.get(0);
            if (attachBaseline && (schedule.getBaseline() != null)) {
                schedule.getBaseline().getId(); // eagerly load the baseline
            }

            return schedule;
        } catch (NoResultException nre) {
            throw new MeasurementNotFoundException(nre);
        }
    }

    @Deprecated
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void disableDefaultCollectionForMeasurementDefinitions(Subject subject, int[] measurementDefinitionIds,
        boolean updateSchedules) {

        modifyDefaultCollectionIntervalForMeasurementDefinitions(subject, measurementDefinitionIds, -1, updateSchedules);

        return;
    }

    @Deprecated
    @RequiredPermissions({ @RequiredPermission(Permission.MANAGE_INVENTORY),
        @RequiredPermission(Permission.MANAGE_SETTINGS) })
    public void disableAllDefaultCollections(Subject subject) {
        entityManager.createNamedQuery(MeasurementDefinition.DISABLE_ALL).executeUpdate();
    }

    @Deprecated
    @RequiredPermissions({ @RequiredPermission(Permission.MANAGE_INVENTORY),
        @RequiredPermission(Permission.MANAGE_SETTINGS) })
    public void disableAllSchedules(Subject subject) {
        entityManager.createNamedQuery(MeasurementSchedule.DISABLE_ALL).executeUpdate();
        // TODO: how do we ensure the agents sync their schedules now so they turn off everything?
    }

    public void createSchedulesForExistingResources(ResourceType type, MeasurementDefinition newDefinition) {
        List<Resource> resources = type.getResources();
        if (resources != null) {
            for (Resource res : resources) {
                res.setAgentSynchronizationNeeded();
                MeasurementSchedule sched = new MeasurementSchedule(newDefinition, res);
                sched.setInterval(newDefinition.getDefaultInterval());
                entityManager.persist(sched);
            }
        }
        return;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @Deprecated
    public void updateDefaultCollectionIntervalForMeasurementDefinitions(Subject subject,
        int[] measurementDefinitionIds, long collectionInterval, boolean updateExistingSchedules) {

        collectionInterval = verifyMinimumCollectionInterval(collectionInterval);

        modifyDefaultCollectionIntervalForMeasurementDefinitions(subject, measurementDefinitionIds, collectionInterval,
            updateExistingSchedules);
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void updateDefaultCollectionIntervalAndEnablementForMeasurementDefinitions(Subject subject,
        int[] measurementDefinitionIds, long collectionInterval, boolean enable, boolean updateExistingSchedules) {

        modifyDefaultCollectionIntervalForMeasurementDefinitions(subject, measurementDefinitionIds, enable,
            collectionInterval, updateExistingSchedules);
    }

    /**
     * Updates the default enablement and/or collection intervals (i.e. metric templates) for the given measurement
     * definitions. If updateExistingSchedules is true, the schedules for the corresponding metrics or all inventoried
     * Resources are also updated. Otherwise, the updated templates will only affect Resources that added to
     * inventory in the future.
     * @param subject 
     *
     * @param measurementDefinitionIds the IDs of the metric defs whose default schedules should be updated
     * @param collectionInterval if > 0, enable the metric with this value as the the new collection
     *                           interval, in milliseconds; if == 0, enable the metric with its current
     *                           collection interval; if < 0, disable the metric; if >0, it is assumed that
     *                           the caller has verified the value is >=30000, since 30s is the minimum
     *                           interval allowed
     * @param updateExistingSchedules if true, existing Resource schedules for metrics of this type should also be updated
     */
    private void modifyDefaultCollectionIntervalForMeasurementDefinitions(Subject subject,
        int[] measurementDefinitionIds, long collectionInterval, boolean updateExistingSchedules) {

        if (measurementDefinitionIds == null || measurementDefinitionIds.length == 0) {
            log.debug("update metric template: no definitions supplied (interval = " + collectionInterval);
            return;
        }

        boolean enable = (collectionInterval >= 0);

        // batch the modifications to prevent the ORA error about IN clauses containing more than 1000 items
        for (int batchIndex = 0; (batchIndex < measurementDefinitionIds.length); batchIndex += 1000) {
            int[] batchIdArray = ArrayUtils.copyOfRange(measurementDefinitionIds, batchIndex, batchIndex + 1000);

            modifyDefaultCollectionIntervalForMeasurementDefinitions(subject, batchIdArray, enable, collectionInterval,
                updateExistingSchedules);
        }
    }

    /**
     * Updates the default enablement and/or collection intervals (i.e. metric templates) for the given measurement
     * definitions. If updateExistingSchedules is true, the schedules for the corresponding metrics or all inventoried
     * Resources are also updated. Otherwise, the updated templates will only affect Resources that are added to
     * inventory in the future.
     *
     * <strong>Only the 3-param modifyDefaultCollectionIntervalForMeasurementDefinitions method should call this method,
     * since it will batch the metric defs specified by the user to ensure no more than 1000 metric defs are passed to
     * this method.</strong>
     * @param subject 
     *
     * @param measurementDefinitionIds the IDs of the metric defs whose default schedules should be updated; the size of
     *                                 this array must be <= 1000
     * @param enable if true, enable the default schedule, otherwise, disable it
     * @param collectionInterval if > 0, enable the metric with this value as the the new collection
     *                           interval, in milliseconds; if == 0, enable the metric with its current
     *                           collection interval; if < 0, disable the metric; if >0, it is assumed that
     *                           the caller has verified the value is >=30000, since 30s is the minimum
     *                           interval allowed
     * @param updateExistingSchedules if true, existing Resource schedules for metrics of this type should also be updated
     */
    @SuppressWarnings("unchecked")
    private void modifyDefaultCollectionIntervalForMeasurementDefinitions(Subject subject,
        int[] measurementDefinitionIds, boolean enable, long collectionInterval, boolean updateExistingSchedules) {

        // this method has been rewritten to ensure that the Hibernate cache is not utilized in an
        // extensive way, regardless of the number of measurementDefinitionIds being processed. Future
        // enhancements must keep this in mind and avoid using attached objects.

        // update all of the measurement definitions via native query to avoid Hibernate caching
        Connection conn = null;
        PreparedStatement defUpdateStmt = null;
        PreparedStatement schedUpdateStmt = null;
        String queryString;
        int i;
        try {
            conn = dataSource.getConnection();

            // update the defaults on the measurement definitions
            if (collectionInterval > 0L) {
                // This query enables the default schedule and updates its collection interval.
                queryString = MeasurementDefinition.QUERY_NATIVE_UPDATE_DEFAULTS_BY_IDS;
            } else {
                // <=0 : This query enables (=0) or disables (<0) the default schedule but does not update the interval.
                queryString = MeasurementDefinition.QUERY_NATIVE_UPDATE_DEFAULT_ON_BY_IDS;
            }

            String transformedQuery = JDBCUtil.transformQueryForMultipleInParameters(queryString, "@@DEFINITION_IDS@@",
                measurementDefinitionIds.length);
            defUpdateStmt = conn.prepareStatement(transformedQuery);
            i = 1;
            defUpdateStmt.setBoolean(i++, enable);
            if (collectionInterval > 0L) {
                defUpdateStmt.setLong(i++, collectionInterval);
            }
            JDBCUtil.bindNTimes(defUpdateStmt, measurementDefinitionIds, i);
            defUpdateStmt.executeUpdate();

            if (updateExistingSchedules) {
                Map<Integer, ResourceMeasurementScheduleRequest> reqMap = new HashMap<Integer, ResourceMeasurementScheduleRequest>();
                List<Integer> idsAsList = ArrayUtils.wrapInList(measurementDefinitionIds);

                // update the schedules associated with the measurement definitions (i.e. the current inventory)
                if (collectionInterval > 0L) {
                    // This query enables the schedules and updates their collection intervals.
                    queryString = MeasurementDefinition.QUERY_NATIVE_UPDATE_SCHEDULES_BY_IDS;
                } else {
                    // <=0 : This query enables (=0) or disables (<0) the schedules but does not update their intervals.
                    queryString = MeasurementDefinition.QUERY_NATIVE_UPDATE_SCHEDULES_ENABLE_BY_IDS;
                }

                transformedQuery = JDBCUtil.transformQueryForMultipleInParameters(queryString, "@@DEFINITION_IDS@@",
                    measurementDefinitionIds.length);
                schedUpdateStmt = conn.prepareStatement(transformedQuery);
                i = 1;
                schedUpdateStmt.setBoolean(i++, enable);
                if (collectionInterval > 0L) {
                    schedUpdateStmt.setLong(i++, collectionInterval);
                }
                JDBCUtil.bindNTimes(schedUpdateStmt, measurementDefinitionIds, i++);
                schedUpdateStmt.executeUpdate();

                // Notify the agents of the updated schedules for affected resources

                // we need specific information to construct the agent updates. This query is specific to
                // this use case and therefore is defined here and not in a domain module. Note that this
                // query must not return domain entities as they would be placed in the Hibernate cache.
                // Return only the data necessary to construct minimal objects ourselves. Using JPQL
                // is ok, it just lets Hibernate do the heavy lifting for query generation.
                queryString = "" //
                    + "SELECT ms.id, ms.interval, ms.resource.id, ms.definition.name, ms.definition.dataType, ms.definition.rawNumericType" //
                    + " FROM  MeasurementSchedule ms" //
                    + " WHERE ms.definition.id IN ( :definitionIds )";
                Query query = entityManager.createQuery(queryString);
                query.setParameter("definitionIds", idsAsList);
                List<Object[]> rs = query.getResultList();

                for (Object[] row : rs) {
                    i = 0;
                    int schedId = (Integer) row[i++];
                    long existingInterval = (Long) row[i++];
                    int resourceId = (Integer) row[i++];
                    String name = (String) row[i++];
                    DataType dataType = (DataType) row[i++];
                    NumericType numericType = (NumericType) row[i++];

                    ResourceMeasurementScheduleRequest req = reqMap.get(resourceId);
                    if (null == req) {
                        req = new ResourceMeasurementScheduleRequest(resourceId);
                        reqMap.put(resourceId, req);
                    }
                    MeasurementScheduleRequest msr = new MeasurementScheduleRequest(schedId, name,
                        ((collectionInterval > 0) ? collectionInterval : existingInterval), enable, dataType,
                        numericType);
                    req.addMeasurementScheduleRequest(msr);
                }

                Map<Agent, Set<ResourceMeasurementScheduleRequest>> agentUpdates = null;
                agentUpdates = new HashMap<Agent, Set<ResourceMeasurementScheduleRequest>>();

                // The number of Agents is manageable, so we can work with entities here
                for (Integer resourceId : reqMap.keySet()) {
                    Agent agent = agentManager.getAgentByResourceId(subjectManager.getOverlord(), resourceId);

                    // Ignore resources that are not actually associated with an agent. For example,
                    // those with an UNINVENTORIED status. 
                    if (null == agent) {
                        if (log.isDebugEnabled()) {
                            log.debug("Ignoring measurement schedule change for non-agent-related resource ["
                                + resourceId + "]. It is probably waiting to be uninventoried.");
                        }

                        continue;
                    }

                    Set<ResourceMeasurementScheduleRequest> agentUpdate = agentUpdates.get(agent);
                    if (agentUpdate == null) {
                        agentUpdate = new HashSet<ResourceMeasurementScheduleRequest>();
                        agentUpdates.put(agent, agentUpdate);
                    }

                    agentUpdate.add(reqMap.get(resourceId));
                }

                // convert the int[] to Integer[], in case we need to set
                // send schedule updates to agents
                for (Map.Entry<Agent, Set<ResourceMeasurementScheduleRequest>> agentEntry : agentUpdates.entrySet()) {
                    boolean synced = sendUpdatedSchedulesToAgent(agentEntry.getKey(), agentEntry.getValue());
                    if (!synced) {
                        /* 
                         * only sync resources that are affected by this set of definitions that were updated, and only 
                         * for the agent that couldn't be contacted (under the assumption that 9 times out of 10 the agent
                         * will be up; so, we don't want to unnecessarily mark more resources as needing syncing that don't
                         */
                        int agentId = agentEntry.getKey().getId();
                        setAgentSynchronizationNeededByDefinitionsForAgent(agentId, idsAsList);
                    }
                }
            }
        } catch (Exception e) {
            String errorMessage = "Error updating measurement definitions";
            SQLException sqle = null;
            if (e instanceof SQLException) {
                sqle = (SQLException) e;
            } else if (e.getCause() instanceof SQLException) {
                sqle = (SQLException) e.getCause();
            }
            if (sqle != null) {
                String s = JDBCUtil.convertSQLExceptionToString((SQLException) e);
                errorMessage += ": " + s;
            }
            log.error(errorMessage, e);
            throw new MeasurementException("Error updating measurement definitions: " + e);
        } finally {
            JDBCUtil.safeClose(defUpdateStmt);
            JDBCUtil.safeClose(schedUpdateStmt);
            JDBCUtil.safeClose(conn);
        }
    }

    public int updateSchedulesForContext(Subject subject, EntityContext context, int[] measurementDefinitionIds,
        long collectionInterval) {

        collectionInterval = verifyMinimumCollectionInterval(collectionInterval);

        String measurementScheduleSubQuery = getMeasurementScheduleSubQueryForContext(subject, context,
            measurementDefinitionIds);

        String updateQuery = "" //
            + "UPDATE MeasurementSchedule " //
            + "   SET interval = :interval, " //
            + "       enabled = true " //
            + " WHERE id IN ( " + measurementScheduleSubQuery + " ) ";

        Query query = entityManager.createQuery(updateQuery);
        query.setParameter("interval", collectionInterval);
        int affectedRows = query.executeUpdate();

        scheduleJobToPushScheduleUpdatesToAgents(context, measurementScheduleSubQuery);

        return affectedRows;
    }

    public int enableSchedulesForContext(Subject subject, EntityContext context, int[] measurementDefinitionIds) {
        String measurementScheduleSubQuery = getMeasurementScheduleSubQueryForContext(subject, context,
            measurementDefinitionIds);

        String updateQuery = "" //
            + "UPDATE MeasurementSchedule " //
            + "   SET enabled = true " //
            + " WHERE id IN ( " + measurementScheduleSubQuery + " ) ";

        Query query = entityManager.createQuery(updateQuery);
        int affectedRows = query.executeUpdate();

        scheduleJobToPushScheduleUpdatesToAgents(context, measurementScheduleSubQuery);

        return affectedRows;
    }

    public int disableSchedulesForContext(Subject subject, EntityContext context, int[] measurementDefinitionIds) {
        String measurementScheduleSubQuery = getMeasurementScheduleSubQueryForContext(subject, context,
            measurementDefinitionIds);

        String updateQuery = "" //
            + "UPDATE MeasurementSchedule " //
            + "   SET enabled = false " //
            + " WHERE id IN ( " + measurementScheduleSubQuery + " ) ";

        Query query = entityManager.createQuery(updateQuery);
        int affectedRows = query.executeUpdate();

        scheduleJobToPushScheduleUpdatesToAgents(context, measurementScheduleSubQuery);

        return affectedRows;
    }

    public static final String TRIGGER_NAME = "TriggerName";
    public static final String TRIGGER_GROUP_NAME = "TriggerGroupName";
    public static final String SCHEDULE_SUBQUERY = "ScheduleSubQuery";
    public static final String ENTITYCONTEXT_RESOURCEID = "EntityContext.resourceId";
    public static final String ENTITYCONTEXT_GROUPID = "EntityContext.groupId";
    public static final String ENTITYCONTEXT_PARENT_RESOURCEID = "EntityContext.parentResourceId";
    public static final String ENTITYCONTEXT_RESOURCETYPEID = "EntityContext.resourceTypeId";

    private void scheduleJobToPushScheduleUpdatesToAgents(EntityContext entityContext, String scheduleSubQuery) {
        Scheduler scheduler;
        try {
            scheduler = LookupUtil.getSchedulerBean();

            final String DEFAULT_AGENT_JOB = "AGENT NOTIFICATION JOB";
            final String DEFAULT_AGENT_GROUP = "AGENT NOTIFICATION GROUP";
            final String DEFAULT_AGENT_TRIGGER = "AGENT NOTIFICATION TRIGGER";

            final String randomSuffix = UUID.randomUUID().toString();

            final String jobName = DEFAULT_AGENT_JOB + " - " + randomSuffix;
            JobDetail jobDetail = new JobDetail(jobName, DEFAULT_AGENT_GROUP, NotifyAgentsOfScheduleUpdatesJob.class);

            final String triggerName = DEFAULT_AGENT_TRIGGER + " - " + randomSuffix;
            SimpleTrigger simpleTrigger = new SimpleTrigger(triggerName, DEFAULT_AGENT_GROUP, new Date());

            JobDataMap jobDataMap = simpleTrigger.getJobDataMap();
            jobDataMap.put(TRIGGER_NAME, triggerName);
            jobDataMap.put(TRIGGER_GROUP_NAME, DEFAULT_AGENT_GROUP);
            jobDataMap.put(SCHEDULE_SUBQUERY, scheduleSubQuery);
            jobDataMap.put(ENTITYCONTEXT_RESOURCEID, Integer.toString(entityContext.getResourceId()));
            jobDataMap.put(ENTITYCONTEXT_GROUPID, Integer.toString(entityContext.getGroupId()));
            jobDataMap.put(ENTITYCONTEXT_PARENT_RESOURCEID, Integer.toString(entityContext.getParentResourceId()));
            jobDataMap.put(ENTITYCONTEXT_RESOURCETYPEID, Integer.toString(entityContext.getResourceTypeId()));

            if (isJobScheduled(scheduler, DEFAULT_AGENT_JOB, DEFAULT_AGENT_GROUP)) {
                simpleTrigger.setJobName(DEFAULT_AGENT_JOB);
                simpleTrigger.setJobGroup(DEFAULT_AGENT_GROUP);
                scheduler.scheduleJob(simpleTrigger);
            } else {
                scheduler.scheduleJob(jobDetail, simpleTrigger);
            }
        } catch (RuntimeException e) {
            // lookup wrapper throws runtime exceptions, no distinction between
            // types, so fallback and do the best we can.
            log.error("Failed to schedule agents update notification.", e);
            notifyAgentsOfScheduleUpdates(entityContext, scheduleSubQuery);
        } catch (SchedulerException e) {
            // should never happen, but fallback gracefully...
            log.error("Failed to schedule agents update notification.", e);
            notifyAgentsOfScheduleUpdates(entityContext, scheduleSubQuery);
        }
    }

    /*
    private boolean isTriggerScheduled(Scheduler scheduler, String name, String group) {
        boolean isScheduled = false;
        try {
            Trigger trigger = scheduler.getTrigger(name, group);
            if (trigger != null) {
                isScheduled = true;
            }
        } catch (SchedulerException se) {
            log.error("Error getting trigger", se);
        }
        return isScheduled;
    }
    */

    private boolean isJobScheduled(Scheduler scheduler, String name, String group) {
        boolean isScheduled = false;
        try {
            JobDetail jobDetail = scheduler.getJobDetail(name, group);
            if (jobDetail != null) {
                isScheduled = true;
            }
        } catch (SchedulerException se) {
            log.error("Error getting job detail", se);
        }
        return isScheduled;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void notifyAgentsOfScheduleUpdates(EntityContext entityContext, String scheduleSubQuery) {
        List<Integer> agentIds = new ArrayList<Integer>();
        try {
            String agentsQueryString = "" //
                + "SELECT DISTINCT ms.resource.agent.id " //
                + "  FROM MeasurementSchedule ms " //
                + " WHERE ms.id IN ( " + scheduleSubQuery + " ) ";
            if (log.isDebugEnabled()) {
                log.debug("agentsQueryString: " + agentsQueryString);
            }
            Query agentsQuery = entityManager.createQuery(agentsQueryString);
            agentIds = agentsQuery.getResultList();
        } catch (Throwable t) {
            log.error("Could not notify agents of updates", t);
        }

        // use composite query -- won't load managed entities, requires minimal wire transfer
        String scheduleRequestQueryString = "" //
            + "SELECT ms.resource.id, " //
            + "       ms.id, " //
            + "       ms.definition.name, " //
            + "       ms.interval, " //
            + "       ms.enabled, " //
            + "       ms.definition.dataType, " //
            + "       ms.definition.rawNumericType " //
            + "  FROM MeasurementSchedule ms " //
            + " WHERE ms.id IN ( " + scheduleSubQuery + " ) " //
            + "   AND ms.resource.agent.id = :agentId";
        if (log.isDebugEnabled()) {
            log.debug("scheduleRequestQueryString: " + scheduleRequestQueryString);
        }
        Query scheduleRequestQuery = entityManager.createQuery(scheduleRequestQueryString);

        Map<Integer, ResourceMeasurementScheduleRequest> agentRequests = new HashMap<Integer, ResourceMeasurementScheduleRequest>();
        for (int nextAgentId : agentIds) {
            scheduleRequestQuery.setParameter("agentId", nextAgentId);
            List<Object[]> scheduleRequests = scheduleRequestQuery.getResultList();
            for (Object[] nextScheduleDataSet : scheduleRequests) {
                int resourceId = (Integer) nextScheduleDataSet[0];
                ResourceMeasurementScheduleRequest resourceRequest = agentRequests.get(resourceId);
                if (resourceRequest == null) {
                    resourceRequest = new ResourceMeasurementScheduleRequest(resourceId);
                    agentRequests.put(resourceId, resourceRequest);
                }

                MeasurementScheduleRequest requestData = new MeasurementScheduleRequest( //
                    (Integer) nextScheduleDataSet[1], // scheduleId
                    (String) nextScheduleDataSet[2], // definitionName,
                    (Long) nextScheduleDataSet[3], // interval,
                    (Boolean) nextScheduleDataSet[4], // enabled,
                    (DataType) nextScheduleDataSet[5], // dataType,
                    (NumericType) nextScheduleDataSet[6]); // rawNumericType
                resourceRequest.addMeasurementScheduleRequest(requestData);
            }

            boolean markResources = false;
            try {
                Agent nextAgent = agentManager.getAgentByID(nextAgentId);
                AgentClient agentClient = agentManager.getAgentClient(nextAgent);

                boolean couldPing = agentClient.ping(2000); // see if agent is up for sending
                if (couldPing) {
                    Set<ResourceMeasurementScheduleRequest> requestsToSend = new HashSet<ResourceMeasurementScheduleRequest>(
                        agentRequests.values());
                    agentClient.getMeasurementAgentService().updateCollection(requestsToSend);
                } else {
                    log.error("Could not send measurement schedule updates to agent[id=" + nextAgentId
                        + "], marking resources for update; agent ping failed");
                    markResources = true;
                }
            } catch (Throwable t) {
                log.error("Could not send measurement schedule updates to agent[id=" + nextAgentId
                    + "], marking resources for update", t);
                markResources = true;
            }

            if (markResources) {
                markResources(entityContext, nextAgentId);
            }
        }
    }

    /**
     * The mtime on the Resources will tell the Agent it needs to pull down the
     * latest schedules next time it performs an Agent-Server sync.
     *
     * @param context the entity context
     * @param agentId the agent id
     */
    private void markResources(EntityContext context, int agentId) {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.clearPaging(); //important to avoid setting the ordering in the generated query
        if (context.type == EntityContext.Type.Resource) {
            criteria.addFilterId(context.resourceId);
        } else if (context.type == EntityContext.Type.ResourceGroup) {
            criteria.addFilterImplicitGroupIds(context.groupId);
        } else if (context.type == EntityContext.Type.AutoGroup) {
            criteria.addFilterParentResourceId(context.parentResourceId);
            criteria.addFilterResourceTypeId(context.resourceTypeId);
        }
        criteria.addFilterAgentId(agentId);

        try {
            CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);

            generator.alterProjection("resource.id");
            String resourceSubQuery = generator.getParameterReplacedQuery(false);

            String markResourceQueryString = "" //
                + "UPDATE Resource res " //
                + "   SET res.mtime = :now " //
                + " WHERE res.id IN ( " + resourceSubQuery + " ) ";
            if (log.isDebugEnabled()) {
                log.debug("markResourceQueryString: " + markResourceQueryString);
            }

            Query markResourceQuery = entityManager.createQuery(markResourceQueryString);
            markResourceQuery.setParameter("now", System.currentTimeMillis());
            int affectedRows = markResourceQuery.executeUpdate();
            if (log.isDebugEnabled()) {
                log.debug("Marked " + affectedRows + " for future measurement schedule update");
            }
        } catch (Throwable t) {
            log.error("Could not notify agents of updates", t);
        }
    }

    public String getMeasurementScheduleSubQueryForContext(Subject subject, EntityContext context,
        int[] measurementDefinitionIds) {
        if (context.type == EntityContext.Type.Resource) {
            if (authorizationManager.hasResourcePermission(subject, Permission.MANAGE_MEASUREMENTS, context.resourceId) == false) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to manage schedules for resource[id=" + context.resourceId + "]");
            }
        } else if (context.type == EntityContext.Type.ResourceGroup) {
            if (authorizationManager.hasGroupPermission(subject, Permission.MANAGE_MEASUREMENTS, context.groupId) == false) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to manage schedules for resourceGroup[id=" + context.groupId + "]");
            }
        } else if (context.type == EntityContext.Type.AutoGroup) {
            if (authorizationManager.hasAutoGroupPermission(subject, Permission.MANAGE_MEASUREMENTS,
                context.parentResourceId, context.resourceTypeId) == false) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to manage schedules for autoGroup[parentResourceId="
                    + context.parentResourceId + ", resourceTypeId=" + context.resourceTypeId + "]");
            }
        }

        MeasurementScheduleCriteria criteria = new MeasurementScheduleCriteria();
        criteria.clearPaging(); //important to avoid setting the ordering in the generated query
        if (context.type == EntityContext.Type.Resource) {
            criteria.addFilterResourceId(context.resourceId);
        } else if (context.type == EntityContext.Type.ResourceGroup) {
            criteria.addFilterResourceGroupId(context.groupId);
        } else if (context.type == EntityContext.Type.AutoGroup) {
            criteria.addFilterAutoGroupParentResourceId(context.parentResourceId);
            criteria.addFilterAutoGroupResourceTypeId(context.resourceTypeId);
        }
        criteria.addFilterDefinitionIds(ArrayUtils.wrapInArray(measurementDefinitionIds));

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        ;
        generator.alterProjection("measurementschedule.id");
        String measurementScheduleSubQuery = generator.getParameterReplacedQuery(false);

        return measurementScheduleSubQuery;
    }

    private void setAgentSynchronizationNeededByDefinitionsForAgent(int agentId, List<Integer> measurementDefinitionIds) {
        String updateSQL = "" //
            + "UPDATE Resource res " //
            + "   SET res.mtime = :now " //
            + " WHERE res.agent.id = :agentId AND " //
            + "       res.resourceType.id IN ( SELECT md.resourceType.id " //
            + "                                  FROM MeasurementDefinition md " //
            + "                                 WHERE md.id IN ( :definitionIds ) )";
        Query updateQuery = entityManager.createQuery(updateSQL);
        updateQuery.setParameter("now", System.currentTimeMillis());
        updateQuery.setParameter("agentId", agentId);
        updateQuery.setParameter("definitionIds", measurementDefinitionIds);
        int updateCount = updateQuery.executeUpdate();

        if (log.isDebugEnabled()) {
            log.debug("" + updateCount
                + " resources mtime fields were updated as a result of this metric template update");
        }
    }

    /**
     * @deprecated used for portal war
     */
    public void updateSchedulesForAutoGroup(Subject subject, int parentResourceId, int childResourceType,
        int[] measurementDefinitionIds, long collectionInterval) {

        updateSchedulesForContext(subject, EntityContext.forAutoGroup(parentResourceId, childResourceType),
            measurementDefinitionIds, collectionInterval);
    }

    @Deprecated
    @Override
    public void disableSchedulesForAutoGroup(Subject subject, int parentResourceId, int childResourceType,
        int[] measurementDefinitionIds) {
        disableSchedulesForContext(subject, EntityContext.forAutoGroup(parentResourceId, childResourceType),
            measurementDefinitionIds);
    }

    @Deprecated
    @Override
    public void enableSchedulesForAutoGroup(Subject subject, int parentResourceId, int childResourceType,
        int[] measurementDefinitionIds) {
        enableSchedulesForContext(subject, EntityContext.forAutoGroup(parentResourceId, childResourceType),
            measurementDefinitionIds);
    }

    /**
     * Determine the Schedules for a Resource and DataType. The data type is used to filter out (numerical) measurement
     * and / or traits. If it is null, then we don't filter by DataType
     *
     * @param  subject     Subject of the caller
     * @param  resourceId  PK of the resource we're interested in
     * @param  dataType    DataType of the desired results use null for no filtering
     * @param  displayType the display type of the property or null for no filtering
     *
     * @return List of MeasuremenSchedules for the given resource
     */
    @SuppressWarnings("unchecked")
    public List<MeasurementSchedule> findSchedulesForResourceAndType(Subject subject, int resourceId,
        DataType dataType, DisplayType displayType, boolean enabledOnly) {
        OrderingField sortOrder = new OrderingField("ms.definition.displayName", PageOrdering.ASC);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            MeasurementSchedule.FIND_ALL_FOR_RESOURCE_ID, sortOrder);
        query.setParameter("resourceId", resourceId);
        query.setParameter("dataType", dataType);
        query.setParameter("displayType", displayType);
        query.setParameter("enabled", enabledOnly ? true : null);
        List<MeasurementSchedule> results = query.getResultList();
        return results;
    }

    private boolean sendUpdatedSchedulesToAgent(Agent agent,
        Set<ResourceMeasurementScheduleRequest> resourceMeasurementScheduleRequest) {
        try {
            AgentClient agentClient = LookupUtil.getAgentManager().getAgentClient(agent);
            if (agentClient.ping(2000) == false) {
                if (log.isDebugEnabled()) {
                    log.debug("Won't send MeasurementSchedules to offline Agent[id=" + agent.getId() + "]");
                }
                return false;
            }
            agentClient.getMeasurementAgentService().updateCollection(resourceMeasurementScheduleRequest);
            return true; // successfully sync'ed schedules down to the agent
        } catch (Throwable t) {
            log.error("Error updating MeasurementSchedules for Agent[id=" + agent.getId() + "]: ", t);
        }
        return false; // catch all and presume the live sync failed
    }

    /**
     * Return a list of MeasurementSchedules for the given definition ids and resource id.
     *
     * @param  definitionIds
     * @param  resourceId
     *
     * @return a list of Schedules
     */
    public List<MeasurementSchedule> findSchedulesByResourceIdAndDefinitionIds(Subject subject, int resourceId,
        int[] definitionIds) {
        return findSchedulesByResourcesAndDefinitions(subject, new int[] { resourceId }, definitionIds);
    }

    /**
     * This gets measurement schedules for a resource and optionally its dependents. It creates them as necessary.
     *
     * @param resourceIds    The ids of the resources to retrieve schedules for
     * @param allSchedules   The set to which the schedules should be added
     * @param getDescendents If true, schedules for all descendent resources will also be loaded
     */
    private void getSchedulesForResourceAndItsDescendants(int[] resourceIds,
        Set<ResourceMeasurementScheduleRequest> allSchedules, boolean getDescendents) {

        if (resourceIds == null || resourceIds.length == 0) {
            // no work to do
            return;
        }

        try {
            for (int batchIndex = 0; batchIndex < resourceIds.length; batchIndex += 1000) {
                int[] batchIds = ArrayUtils.copyOfRange(resourceIds, batchIndex, batchIndex + 1000);

                /* 
                 * need to use a native query solution for both the insertion and returning the results because if we
                 * go through Hibernate to return the results it will not see the effects of the insert statement
                 */
                measurementScheduleManager.insertSchedulesFor(batchIds);
                measurementScheduleManager.returnSchedulesFor(batchIds, allSchedules);

                if (getDescendents) {
                    // recursively get all the default schedules for all children of the resource
                    int[] batchChildrenIds = getChildrenIdByParentIds(batchIds);
                    getSchedulesForResourceAndItsDescendants(batchChildrenIds, allSchedules, getDescendents);
                }
            }
        } catch (Throwable t) {
            log.warn("problem creating schedules for resourceIds [" + Arrays.toString(resourceIds) + "]", t);
        }

        return;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int insertSchedulesFor(int[] batchIds) throws Exception {
        /* 
         * JM: (April 15th, 2009)
         * 
         *     the "res.id" token on the final line does not get the "res" alias from the outer query appropriately;
         *     instead, it tries to reference the table name itself as "RHQ_RESOURCE.ID", which bombs with[2] on 
         *     postgres; i thought of using "WHERE ms.resource.uuid = res.uuid" which would work because UUID column 
         *     name is not reused for any other entity in the model, let alone on any table used in this query; however,
         *     this felt like a hack, and I wasn't sure whether UUID would be unique across very large inventories; if
         *     it's not, there is a slight chance that the insert query could do the wrong thing (albeit rare), so I 
         *     erred on the side of correctness and went with native sql which allowed me to use the proper id alias in 
         *     the correlated subquery; correctness aside, keeping the logic using resource id should allow the query 
         *     optimizer to use indexes instead of having to look up the rows on the resource table to get the uuid
         *
         * [1] - http://opensource.atlassian.com/projects/hibernate/browse/HHH-1397
         * [2] - ERROR: invalid reference to FROM-clause entry for table "rhq_resource"
         *
         * Query insertHQL = entityManager.createQuery("" //
         *     + "INSERT INTO MeasurementSchedule( enabled, interval, definition, resource ) \n"
         *     + "     SELECT md.defaultOn, md.defaultInterval, md, res \n"
         *     + "       FROM Resource res, ResourceType rt, MeasurementDefinition md \n"
         *     + "      WHERE res.id IN ( :resourceIds ) \n"
         *     + "        AND res.resourceType.id = rt.id \n"
         *     + "        AND rt.id = md.resourceType.id \n"
         *     + "        AND md.id NOT IN ( SELECT ms.definition.id \n" //
         *     + "                             FROM MeasurementSchedule ms \n" //
         *     + "                            WHERE ms.resource.id = res.id )");
         */

        Connection conn = null;
        PreparedStatement insertStatement = null;

        int created = -1;
        try {
            conn = dataSource.getConnection();
            DatabaseType dbType = DatabaseTypeFactory.getDatabaseType(conn);

            String insertQueryString = null;
            if (dbType instanceof PostgresqlDatabaseType) {
                insertQueryString = MeasurementSchedule.NATIVE_QUERY_INSERT_SCHEDULES_POSTGRES;
            } else if (dbType instanceof OracleDatabaseType || dbType instanceof H2DatabaseType) {
                insertQueryString = MeasurementSchedule.NATIVE_QUERY_INSERT_SCHEDULES_ORACLE;
            } else if (dbType instanceof SQLServerDatabaseType) {
                insertQueryString = MeasurementSchedule.NATIVE_QUERY_INSERT_SCHEDULES_SQL_SERVER;
            } else {
                throw new IllegalArgumentException("Unknown database type, can't continue: " + dbType);
            }

            insertQueryString = JDBCUtil.transformQueryForMultipleInParameters(insertQueryString, "@@RESOURCES@@",
                batchIds.length);
            insertStatement = conn.prepareStatement(insertQueryString);

            JDBCUtil.bindNTimes(insertStatement, batchIds, 1);

            // first create whatever schedules may be needed
            created = insertStatement.executeUpdate();
            if (log.isDebugEnabled()) {
                log.debug("Batch created [" + created + "] default measurement schedules for resource batch ["
                    + batchIds + "]");
            }
        } finally {
            if (insertStatement != null) {
                try {
                    insertStatement.close();
                } catch (Exception e) {
                }
            }

            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                }
            }
        }
        return created;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int returnSchedulesFor(int[] batchIds, Set<ResourceMeasurementScheduleRequest> allSchedules)
        throws Exception {
        Connection conn = null;
        PreparedStatement resultsStatement = null;

        int created = -1;
        try {
            conn = dataSource.getConnection();

            String resultsQueryString = MeasurementSchedule.NATIVE_QUERY_REPORTING_RESOURCE_MEASUREMENT_SCHEDULE_REQUEST;
            resultsQueryString = JDBCUtil.transformQueryForMultipleInParameters(resultsQueryString, "@@RESOURCES@@",
                batchIds.length);
            resultsStatement = conn.prepareStatement(resultsQueryString);

            JDBCUtil.bindNTimes(resultsStatement, batchIds, 1);

            Map<Integer, ResourceMeasurementScheduleRequest> scheduleRequestMap = new HashMap<Integer, ResourceMeasurementScheduleRequest>();
            ResultSet results = resultsStatement.executeQuery();
            try {
                while (results.next()) {
                    Integer resourceId = (Integer) results.getInt(1);
                    Integer scheduleId = (Integer) results.getInt(2);
                    String definitionName = (String) results.getString(3);
                    Long interval = (Long) results.getLong(4);
                    Boolean enabled = (Boolean) results.getBoolean(5);
                    DataType dataType = DataType.values()[results.getInt(6)];
                    NumericType rawNumericType = NumericType.values()[results.getInt(7)];
                    if (results.wasNull()) {
                        rawNumericType = null;
                    }

                    ResourceMeasurementScheduleRequest scheduleRequest = scheduleRequestMap.get(resourceId);
                    if (scheduleRequest == null) {
                        scheduleRequest = new ResourceMeasurementScheduleRequest(resourceId);
                        scheduleRequestMap.put(resourceId, scheduleRequest);
                        allSchedules.add(scheduleRequest);
                    }

                    MeasurementScheduleRequest requestData = new MeasurementScheduleRequest(scheduleId, definitionName,
                        interval, enabled, dataType, rawNumericType);
                    scheduleRequest.addMeasurementScheduleRequest(requestData);
                }
            } finally {
                results.close();
            }
        } finally {
            if (resultsStatement != null) {
                try {
                    resultsStatement.close();
                } catch (Exception e) {
                }
            }

            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                }
            }
        }
        return created;
    }

    @SuppressWarnings("unchecked")
    private int[] getChildrenIdByParentIds(int[] batchIds) {
        Query query = entityManager.createNamedQuery(Resource.QUERY_FIND_CHILDREN_IDS_BY_PARENT_IDS);
        query.setParameter("parentIds", ArrayUtils.wrapInList(batchIds));
        List<Integer> results = query.getResultList();
        int[] batchChildrenIds = ArrayUtils.unwrapCollection(results);
        return batchChildrenIds;
    }

    public int getScheduledMeasurementsPerMinute() {
        Number rate = 0;
        try {
            Query query = entityManager.createNamedQuery(MeasurementSchedule.GET_SCHEDULED_MEASUREMENTS_PER_MINUTED);
            query.setParameter("status", InventoryStatus.COMMITTED);
            rate = (Number) query.getSingleResult();
        } catch (Throwable t) {
            measurementScheduleManager.errorCorrectSchedules();
        }
        return (rate == null) ? 0 : rate.intValue();
    }

    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void errorCorrectSchedules() {
        /* 
         * update mtime of resources whose schedules are < 30s, this will indicate to the 
         * agent that it needs to sync / merge schedules for the resources updated here
         */
        try {
            long now = System.currentTimeMillis();
            String updateResourcesQueryString = "" //
                + " UPDATE Resource " //
                + "    SET mtime = :currentTime " //
                + "  WHERE id IN ( SELECT ms.resource.id " //
                + "                  FROM MeasurementSchedule ms " // 
                + "                 WHERE ms.interval < 30000 ) ";
            Query updateResourcesQuery = entityManager.createQuery(updateResourcesQueryString);
            updateResourcesQuery.setParameter("currentTime", now);
            int resourcesUpdatedCount = updateResourcesQuery.executeUpdate();

            // update schedules to 30s whose schedules are < 30s
            String updateSchedulesQueryString = "" //
                + " UPDATE MeasurementSchedule " //
                + "    SET interval = 30000 " //
                + "  WHERE interval < 30000 ";
            Query updateSchedulesQuery = entityManager.createQuery(updateSchedulesQueryString);
            int schedulesUpdatedCount = updateSchedulesQuery.executeUpdate();

            if (resourcesUpdatedCount > 0) {
                // now try to tell the agents that certain resources have changed
                String findResourcesQueryString = "" //
                    + " SELECT res.id " //
                    + "   FROM Resource res " //
                    + "  WHERE res.mtime = :currentTime ";
                Query findResourcesQuery = entityManager.createQuery(findResourcesQueryString);
                findResourcesQuery.setParameter("currentTime", now);
                List<Integer> updatedResourceIds = findResourcesQuery.getResultList();
                updateMeasurementSchedulesForResources(ArrayUtils.unwrapCollection(updatedResourceIds));

                log.error("MeasurementSchedule data was corrupt: automatically updated " + resourcesUpdatedCount
                    + " resources and " + schedulesUpdatedCount + " to correct the issue; agents were notified");
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("MeasurementSchedule data was checked for corruption, but all data was consistent");
                }
            }
        } catch (Throwable t) {
            log.error("There was a problem correcting errors for MeasurementSchedules", t);
        }
    }

    private void updateMeasurementSchedulesForResources(int[] resourceIds) {
        if (resourceIds.length == 0) {
            return;
        }

        // first get all the resources, which is needed to get the agent mappings
        Subject overlord = subjectManager.getOverlord();
        PageList<Resource> resources = resourceManager.findResourceByIds(overlord, resourceIds, false,
            PageControl.getUnlimitedInstance());

        // then get all the requests
        Set<ResourceMeasurementScheduleRequest> requests = findSchedulesForResourceAndItsDescendants(resourceIds, false);

        Map<Agent, Set<ResourceMeasurementScheduleRequest>> agentScheduleMap = new HashMap<Agent, Set<ResourceMeasurementScheduleRequest>>();
        for (Resource resource : resources) {
            Agent agent = resource.getAgent();
            Set<ResourceMeasurementScheduleRequest> agentSchedules = agentScheduleMap.get(agent);
            if (agentSchedules == null) {
                agentSchedules = new HashSet<ResourceMeasurementScheduleRequest>();
                agentScheduleMap.put(agent, agentSchedules);
            }
            for (ResourceMeasurementScheduleRequest request : requests) {
                int resId = resource.getId();
                if (request.getResourceId() == resId) {
                    agentSchedules.add(request);
                }
            }
        }

        for (Map.Entry<Agent, Set<ResourceMeasurementScheduleRequest>> agentScheduleEntry : agentScheduleMap.entrySet()) {
            Agent agent = agentScheduleEntry.getKey();
            Set<ResourceMeasurementScheduleRequest> schedules = agentScheduleEntry.getValue();
            try {
                sendUpdatedSchedulesToAgent(agent, schedules);
            } catch (Throwable t) {
                if (log.isDebugEnabled()) {
                    log.debug("Tried to immediately sync agent[name=" + agent.getName()
                        + "] with error-corrected schedules failed");
                }
            }
        }
    }

    public void disableSchedulesForResource(Subject subject, int resourceId, int[] measurementDefinitionIds) {
        disableSchedulesForContext(subject, EntityContext.forResource(resourceId), measurementDefinitionIds);
    }

    public void disableSchedulesForCompatibleGroup(Subject subject, int groupId, int[] measurementDefinitionIds) {
        disableSchedulesForContext(subject, EntityContext.forGroup(groupId), measurementDefinitionIds);
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @Override
    public void disableSchedulesForResourceType(Subject subject, int[] measurementDefinitionIds,
        boolean updateExistingSchedules) {

        modifyDefaultCollectionIntervalForMeasurementDefinitions(subject, measurementDefinitionIds, -1,
            updateExistingSchedules);
    }

    @Deprecated
    @Override
    public void disableMeasurementTemplates(Subject subject, int[] measurementDefinitionIds) {
        disableDefaultCollectionForMeasurementDefinitions(subject, measurementDefinitionIds, true);
    }

    public void enableSchedulesForResource(Subject subject, int resourceId, int[] measurementDefinitionIds) {
        enableSchedulesForContext(subject, EntityContext.forResource(resourceId), measurementDefinitionIds);
    }

    public void enableSchedulesForCompatibleGroup(Subject subject, int groupId, int[] measurementDefinitionIds) {
        enableSchedulesForContext(subject, EntityContext.forGroup(groupId), measurementDefinitionIds);
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @Override
    public void enableSchedulesForResourceType(Subject subject, int[] measurementDefinitionIds,
        boolean updateExistingSchedules) {

        modifyDefaultCollectionIntervalForMeasurementDefinitions(subject, measurementDefinitionIds, 0,
            updateExistingSchedules);
    }

    /**
     * @deprecated
     */
    @Override
    public void enableMeasurementTemplates(Subject subject, int[] measurementDefinitionIds) {
        modifyDefaultCollectionIntervalForMeasurementDefinitions(subject, measurementDefinitionIds, 0, true);
    }

    /**
     * @param  subject  A subject that must be valid
     * @param  schedule A MeasurementSchedule to persist.
     */
    public void updateSchedule(Subject subject, MeasurementSchedule schedule) {
        // attach it so we can navigate to its resource object for authz check
        MeasurementSchedule attached = entityManager.find(MeasurementSchedule.class, schedule.getId());
        if (authorizationManager.hasResourcePermission(subject, Permission.MANAGE_MEASUREMENTS, attached.getResource()
            .getId()) == false) {
            throw new PermissionException("User[" + subject.getName()
                + "] does not have permission to view measurementSchedule[id=" + schedule.getId() + "]");
        }

        verifyMinimumCollectionInterval(schedule);
        entityManager.merge(schedule);
    }

    public void updateSchedulesForResource(Subject subject, int resourceId, int[] measurementDefinitionIds,
        long collectionInterval) {

        updateSchedulesForContext(subject, EntityContext.forResource(resourceId), measurementDefinitionIds,
            collectionInterval);
    }

    public void updateSchedulesForCompatibleGroup(Subject subject, int groupId, int[] measurementDefinitionIds,
        long collectionInterval) {
        // don't verify minimum collection interval here, it will be caught by updateMeasurementSchedules callee
        updateSchedulesForContext(subject, EntityContext.forGroup(groupId), measurementDefinitionIds,
            collectionInterval);
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @Override
    public void updateSchedulesForResourceType(Subject subject, int[] measurementDefinitionIds,
        long collectionInterval, boolean updateExistingSchedules) {

        collectionInterval = verifyMinimumCollectionInterval(collectionInterval);

        modifyDefaultCollectionIntervalForMeasurementDefinitions(subject, measurementDefinitionIds, collectionInterval,
            updateExistingSchedules);
    }

    @Deprecated
    @Override
    public void updateMeasurementTemplates(Subject subject, int[] measurementDefinitionIds, long collectionInterval) {

        collectionInterval = verifyMinimumCollectionInterval(collectionInterval);

        updateDefaultCollectionIntervalForMeasurementDefinitions(subject, measurementDefinitionIds, collectionInterval,
            true);
    }

    @SuppressWarnings("unchecked")
    public PageList<MeasurementScheduleComposite> getMeasurementScheduleCompositesByContext(Subject subject,
        EntityContext context, PageControl pc) {

        // check authorization up front, so that criteria-based queries can run without authz checks
        switch (context.type) {
        case Resource:
            if (authorizationManager.canViewResource(subject, context.resourceId) == false) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view measurement schedules for resource[id=" + context.resourceId
                    + "]");
            }
            break;
        case ResourceGroup:
            if (authorizationManager.canViewGroup(subject, context.groupId) == false) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view measurement schedules for resourceGroup[id="
                    + context.groupId + "]");
            }
            break;
        case AutoGroup:
            if (authorizationManager.canViewAutoGroup(subject, context.parentResourceId, context.resourceTypeId) == false) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view measurement schedules for autoGroup[parentResourceId="
                    + context.parentResourceId + ", resourceTypeId=" + context.resourceTypeId + "]");
            }
            break;
        }

        PageList<MeasurementDefinition> definitions;
        Map<Integer, Long> definitionIntervalMap = new HashMap<Integer, Long>();
        Map<Integer, Boolean> definitionEnabledMap = new HashMap<Integer, Boolean>();
        if (context.type == EntityContext.Type.ResourceTemplate) {
            MeasurementDefinitionCriteria criteria = new MeasurementDefinitionCriteria();
            criteria.addFilterResourceTypeId(context.resourceTypeId);

            CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
            CriteriaQueryRunner<MeasurementDefinition> queryRunner = new CriteriaQueryRunner(criteria, generator,
                entityManager);
            definitions = queryRunner.execute();
            for (MeasurementDefinition definition : definitions) {
                definitionIntervalMap.put(definition.getId(), definition.getDefaultInterval());
                definitionEnabledMap.put(definition.getId(), definition.isDefaultOn());
            }
        } else {
            // Do general criteria setup.
            MeasurementScheduleCriteria criteria = new MeasurementScheduleCriteria();

            switch (context.type) {
            case Resource:
                criteria.addFilterResourceId(context.resourceId);
                break;
            case ResourceGroup:
                criteria.addFilterResourceGroupId(context.groupId);
                break;
            case AutoGroup:
                criteria.addFilterAutoGroupParentResourceId(context.parentResourceId);
                criteria.addFilterAutoGroupResourceTypeId(context.resourceTypeId);
                break;
            }

            criteria.setPageControl(pc); // for primary return list, use passed PageControl
            pc.addDefaultOrderingField("definition.displayName");

            // Get the core definitions.
            CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);

            // We previously used the following altered projection for the criteria query: 
            //
            //   generator.alterProjection(" distinct orderingField0");
            //
            // Hibernate4 no longer allowed for the generated criteria JPQL for this projection:
            //
            //    SELECT distinct measurementschedule.definition 
            //      FROM MeasurementSchedule measurementschedule
            // LEFT JOIN measurementschedule.definition orderingField0
            //     WHERE ( measurementschedule.resource.id IN ( :resourceId ) )
            //  ORDER BY orderingField0.displayName ASC
            //
            // It causes:
            //   SQLGrammarException: ERROR: for SELECT DISTINCT, ORDER BY expressions must appear in select list
            // 
            // In essence, using DISTINCT now requires that we use the LEFT JOIN alias in the select
            // list.  To support this we could probably have made some tricky coding changes to the
            // generator. But seeing that this would be to support non-default criteria queries (i.e
            // the altered projection using DISTINCT), of which this is the only one in the code base,
            // and we are in control of the order by clause, and therefore are predictably working with
            // the JPQL above, I've chosen to just make a change to the custom altered projection, using
            // the JPQL to guide me.
            generator.alterProjection(" distinct orderingField0");
            generator.alterCountProjection(" count(distinct orderingField0)");
            CriteriaQueryRunner<MeasurementDefinition> queryRunner = new CriteriaQueryRunner(criteria, generator,
                entityManager);
            definitions = queryRunner.execute();

            // Reset paging -- remove ordering, add group by.
            criteria.setPageControl(PageControl.getUnlimitedInstance());
            generator.setGroupByClause(" measurementschedule.definition.id ");

            // Get the interval results.
            generator.alterProjection("" //
                + " measurementschedule.definition.id, " //
                + " min(measurementschedule.interval), " //
                + " max(measurementschedule.interval) ");
            Query query = generator.getQuery(entityManager);
            List<Object[]> definitionIntervalResults = query.getResultList();

            // Get the enabled results.
            criteria.addFilterEnabled(true);
            generator.alterProjection(" measurementschedule.definition.id, count(measurementschedule.id) ");
            query = generator.getQuery(entityManager);
            List<Object[]> definitionEnabledResults = query.getResultList();

            // Generate intermediate maps for intervals and enabled values.
            for (Object[] nextInterval : definitionIntervalResults) {
                int definitionId = (Integer) nextInterval[0];
                long minInterval = (Long) nextInterval[1];
                long maxInterval = (Long) nextInterval[2];

                long interval = (minInterval != maxInterval) ? 0 : minInterval;
                definitionIntervalMap.put(definitionId, interval);
            }
            int size = getResourceCount(context);
            for (Object[] nextEnabled : definitionEnabledResults) {
                int definitionId = (Integer) nextEnabled[0];
                long enabledCount = (Long) nextEnabled[1];

                Boolean enabled = null;
                if (enabledCount == size) {
                    enabled = true;
                } else if (enabledCount == 0) {
                    enabled = false;
                }

                definitionEnabledMap.put(definitionId, enabled);
            }
        }

        // Finally, merge everything together.
        List<MeasurementScheduleComposite> composites = new ArrayList<MeasurementScheduleComposite>();
        for (MeasurementDefinition next : definitions) {
            int definitionId = next.getId();
            Boolean enabled = false;
            if (definitionEnabledMap.containsKey(definitionId)) {
                enabled = definitionEnabledMap.get(definitionId);
            }
            long interval = definitionIntervalMap.get(definitionId);

            MeasurementScheduleComposite result = new MeasurementScheduleComposite(next, enabled, interval);
            composites.add(result);
        }
        return new PageList<MeasurementScheduleComposite>(composites, composites.size(), pc);
    }

    private int getResourceCount(EntityContext context) {
        if (context.type == EntityContext.Type.Resource) {
            return 1;
        } else if (context.type == EntityContext.Type.ResourceGroup) {
            return resourceGroupManager.getExplicitGroupMemberCount(context.groupId);
        } else if (context.type == EntityContext.Type.AutoGroup) {
            ResourceCriteria criteria = new ResourceCriteria();
            criteria.addFilterParentResourceId(context.parentResourceId);
            criteria.addFilterResourceTypeId(context.resourceTypeId);
            criteria.setPageControl(PageControl.getSingleRowInstance()); // get one record, then extract totalSize
            PageList<Resource> results = resourceManager
                .findResourcesByCriteria(subjectManager.getOverlord(), criteria);
            return results.getTotalSize();
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    public PageList<MeasurementSchedule> findSchedulesByCriteria(Subject subject, MeasurementScheduleCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        ;
        if (authorizationManager.isInventoryManager(subject) == false) {
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE,
                subject.getId());
        }

        CriteriaQueryRunner<MeasurementSchedule> queryRunner = new CriteriaQueryRunner(criteria, generator,
            entityManager);
        return queryRunner.execute();
    }

    //    public PageList<MeasurementSchedule> getResourceMeasurementSchedulesFromAgent(Subject subject, int resourceId) {
    //        //verifyViewPermissionForMeasurementSchedules(subject, measurementScheduleIds);
    //
    //        AgentClient agentClient = agentManager.getAgentClient(resourceId);
    //        MeasurementAgentService measurementAgentSvc = agentClient.getMeasurementAgentService();
    //
    //        PageList<MeasurementSchedule> schedules = new PageList<MeasurementSchedule>();
    //        for (int scheduleId : measurementAgentSvc.getMeasurementScheduleIdsForResource(resourceId)) {
    //            MeasurementSchedule schedule = getScheduleById(scheduleId);
    //            schedules.add(schedule);
    //        }
    //
    //        return schedules;
    //    }
    //
    //    private void verifyViewPermissionForMeasurementSchedules(Subject subject, int[] measurementScheduleIds) {
    //        for (int id : measurementScheduleIds) {
    //            verifyViewPermission(subject, id);
    //        }
    //    }
    //
    //    private void verifyViewPermission(Subject subject, int scheduleId) {
    //        MeasurementSchedule schedule = entityManager.find(MeasurementSchedule.class, scheduleId);
    //        if (authorizationManager.hasResourcePermission(subject, Permission.MANAGE_MEASUREMENTS, schedule.getResource()
    //            .getId()) == false) {
    //            throw new PermissionException("User[" + subject.getName()
    //                + "] does not have permission to view measurementSchedule[id=" + schedule.getId() + "]");
    //        }
    //    }

}
