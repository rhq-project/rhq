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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;

import org.jboss.annotation.IgnoreDependency;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.H2DatabaseType;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementCategory;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.measurement.composite.MeasurementScheduleComposite;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.authz.RequiredPermissions;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.ArrayUtils;
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
public class MeasurementScheduleManagerBean implements MeasurementScheduleManagerLocal {
    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
    private DataSource dataSource;

    @EJB
    @IgnoreDependency
    private AgentManagerLocal agentManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @EJB
    private ResourceManagerLocal resourceManager;

    @EJB
    @IgnoreDependency
    private ResourceGroupManagerLocal resourceGroupManager;

    @EJB
    @IgnoreDependency
    private ResourceTypeManagerLocal resourceTypeManager;

    @EJB
    private MeasurementScheduleManagerLocal measurementScheduleManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    @javax.annotation.Resource
    private SessionContext ctx;

    @EJB
    private SystemManagerLocal systemManager;

    private final Log log = LogFactory.getLog(MeasurementScheduleManagerBean.class);

    public Set<ResourceMeasurementScheduleRequest> getSchedulesForResourceAndItsDescendants(Set<Integer> resourceIds,
        boolean getDescendents) {
        Set<ResourceMeasurementScheduleRequest> allSchedules = new HashSet<ResourceMeasurementScheduleRequest>();
        getSchedulesForResourceAndItsDescendants(resourceIds, allSchedules, getDescendents);
        return allSchedules;
    }

    /**
     * Compute the agents from a List of MetricSchedules and put the Schedules in Clusters by Agent. This is needed, as
     * updates need to be sent to individual agents
     *
     * @param  schedules MetricSchedules to process
     *
     * @return A Map with Agents as Keys and Lists of SchedulingInfo as values
     *
     * @see    ResourceMeasurementScheduleRequest
     */
    private Map<Agent, Set<ResourceMeasurementScheduleRequest>> getAgentsWithSchedulingInfo(
        List<MeasurementSchedule> schedules) {
        Map<Agent, Set<ResourceMeasurementScheduleRequest>> agentMap = new HashMap<Agent, Set<ResourceMeasurementScheduleRequest>>();
        for (MeasurementSchedule sched : schedules) {
            Resource res = sched.getResource();
            Agent agent = res.getAgent();

            MeasurementScheduleRequest sms = new MeasurementScheduleRequest(sched.getId(), sched.getDefinition()
                .getName(), sched.getInterval(), sched.isEnabled(), sched.getDefinition().getDataType(), sched
                .getDefinition().getRawNumericType());

            ResourceMeasurementScheduleRequest info = null; //new ResourceMeasurementScheduleRequest(sms, res.getId()); TODO
            Set<ResourceMeasurementScheduleRequest> infoList = agentMap.get(agent);
            if (infoList == null) {
                infoList = new HashSet<ResourceMeasurementScheduleRequest>();
            }

            infoList.add(info);
        }

        return agentMap;
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
     * Returns a MeasurementSchedule by its pk or null.
     *
     * @param  pk PrimaryKey of the desired schedule
     *
     * @return The MeasurementSchedule or null if not found
     */
    public MeasurementSchedule getScheduleById(int pk) {
        MeasurementSchedule ms;
        try {
            ms = entityManager.find(MeasurementSchedule.class, pk);
        } catch (NoResultException n) {
            ms = null;
        }

        return ms;
    }

    /**
     * Return a list of MeasurementSchedules for the given ids
     *
     * @param  ids PrimaryKeys of the schedules searched
     *
     * @return a list of Schedules
     */
    @SuppressWarnings("unchecked")
    public List<MeasurementSchedule> getSchedulesByIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty())
            return new ArrayList<MeasurementSchedule>();

        Query q = entityManager.createNamedQuery("MeasurementSchedule.findByIds");
        q.setParameter("ids", ids);
        List<MeasurementSchedule> ret = q.getResultList();
        return ret;
    }

    /**
     * Obtain a MeasurementSchedule by its Id after a check for a valid session
     *
     * @param  subject a session id that must be valid
     * @param  id      The primary key of the Schedule
     *
     * @return a MeasurementSchedule or null, if there is 
     */
    public MeasurementSchedule getMeasurementScheduleById(Subject subject, int id) {
        // TODO: AUTHZ CHECK (VIEW_RESOURCE)
        MeasurementSchedule schedule = entityManager.find(MeasurementSchedule.class, id);
        if (schedule == null)
            return null;

        schedule.getResource().getId();
        schedule.getDefinition().getId();
        return schedule;
    }

    /**
     * Reattach a Schedule to a PersistenceContext after a successful check for a valid session
     *
     * @param  subject  A subject that must be valid
     * @param  schedule A MeasurementSchedule to persist.
     *
     * @return The updated MeasurementSchedule
     */
    public MeasurementSchedule updateMeasurementSchedule(Subject subject, MeasurementSchedule schedule) {
        // TODO: AUTHZ CHECK (MANAGE_METRICS)
        verifyMinimumCollectionInterval(schedule);
        return entityManager.merge(schedule);
    }

    private void verifyMinimumCollectionInterval(MeasurementSchedule schedule) {
        // reset the schedules minimum collection interval if necessary
        schedule.setInterval(verifyMinimumCollectionInterval(schedule.getInterval()));
    }

    private long verifyMinimumCollectionInterval(long collectionInterval) {
        // reset the schedules minimum collection interval if necessary
        if (collectionInterval < MeasurementConstants.MINIMUM_COLLECTION_INTERVAL_MILLIS) {
            return MeasurementConstants.MINIMUM_COLLECTION_INTERVAL_MILLIS;
        }

        return collectionInterval;
    }

    /**
     * Find MeasurementSchedules that are attached to a certain definition and some resources
     *
     * @param  subject      A subject that must be valid
     * @param  definitionId The primary key of a MeasurementDefinition
     * @param  resources    a List of Resources
     *
     * @return a List of MeasurementSchedules
     */
    @SuppressWarnings("unchecked")
    public List<MeasurementSchedule> getMeasurementSchedulesByDefinitionIdAndResources(Subject subject,
        int definitionId, List<Resource> resources) {
        Query q = entityManager.createNamedQuery(MeasurementSchedule.FIND_BY_DEFINITION_ID_AND_RESOURCE_IDS);
        List<Integer> resIds = new ArrayList<Integer>(resources.size());
        for (Resource res : resources) {
            resIds.add(res.getId());
        }

        q.setParameter("definitionId", definitionId);
        q.setParameter("resourceIds", resIds);
        List<MeasurementSchedule> ret = q.getResultList();
        return ret;
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
    @SuppressWarnings("unchecked")
    public List<MeasurementSchedule> getMeasurementSchedulesByDefinitionIdAndResourceIds(Subject subject,
        int definitionId, Integer[] resourceIds) {
        Query q = entityManager.createNamedQuery(MeasurementSchedule.FIND_BY_DEFINITION_ID_AND_RESOURCE_IDS);
        q.setParameter("definitionId", definitionId);
        q.setParameter("resourceIds", Arrays.asList(resourceIds));
        List<MeasurementSchedule> ret = q.getResultList();
        return ret;
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
    public MeasurementSchedule getMeasurementSchedule(Subject subject, int definitionId, int resourceId,
        boolean attachBaseline) throws MeasurementNotFoundException {
        Query query = entityManager.createNamedQuery(MeasurementSchedule.FIND_BY_DEFINITION_ID_AND_RESOURCE_ID);

        query.setParameter("definitionId", definitionId);
        query.setParameter("resourceId", resourceId);

        try {
            MeasurementSchedule schedule = (MeasurementSchedule) query.getSingleResult();
            if (attachBaseline && (schedule.getBaseline() != null)) {
                schedule.getBaseline().getId(); // eagerly load the baseline
            }

            return schedule;
        } catch (NoResultException nre) {
            throw new MeasurementNotFoundException(nre);
        }
    }

    @SuppressWarnings("unchecked")
    public PageList<MeasurementScheduleComposite> getDefaultMeasurementSchedulesForResourceType(Subject subject,
        int resourceTypeId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("md.id");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            MeasurementDefinition.FIND_SCHEDULE_COMPOSITE_FOR_RESOURCE_TYPE, pageControl);
        query.setParameter("resourceTypeId", resourceTypeId);
        List<MeasurementScheduleComposite> results = query.getResultList();
        return new PageList<MeasurementScheduleComposite>(results, pageControl);
    }

    /**
     * Get the MeasurementSchedule composits for an autogroup
     *
     * @param  subject
     * @param  parentId
     * @param  childType
     * @param  pageControl
     *
     * @return
     */
    public PageList<MeasurementScheduleComposite> getMeasurementSchedulesForAutoGroup(Subject subject, int parentId,
        int childType, PageControl pageControl) {
        // pageControl.initDefaultOrderingField(); // this is ignored, as this method eventually uses native queries

        List<Resource> resources = resourceGroupManager.getResourcesForAutoGroup(subject, parentId, childType);
        ResourceType resType;
        try {
            resType = resourceTypeManager.getResourceTypeById(subject, childType);
        } catch (ResourceTypeNotFoundException e) {
            List<MeasurementScheduleComposite> compList = new ArrayList<MeasurementScheduleComposite>();
            PageList<MeasurementScheduleComposite> result = new PageList<MeasurementScheduleComposite>(compList,
                pageControl);
            return result;
        }

        Set<MeasurementDefinition> definitions = resType.getMetricDefinitions();

        return getCurrentMeasurementSchedulesForResourcesAndType(pageControl, resources, resType, definitions);
    }

    /**
     * Get the MeasurementSchedule composites for a compatible group.
     */
    public PageList<MeasurementScheduleComposite> getMeasurementSchedulesForCompatGroup(Subject subject, int groupId,
        PageControl pageControl) {
        // pageControl.initDefaultOrderingField(); // this is ignored, as this method eventually uses native queries

        ResourceGroup group = resourceGroupManager.getResourceGroupById(subject, groupId, GroupCategory.COMPATIBLE);
        Set<Resource> resources = group.getExplicitResources();
        ResourceType resType = group.getResourceType();
        Set<MeasurementDefinition> definitions = resType.getMetricDefinitions();

        return getCurrentMeasurementSchedulesForResourcesAndType(pageControl, resources, resType, definitions);
    }

    /**
     * @param  pageControl
     * @param  resources
     * @param  resType
     * @param  definitions
     *
     * @return
     */
    PageList<MeasurementScheduleComposite> getCurrentMeasurementSchedulesForResourcesAndType(PageControl pageControl,
        Collection<Resource> resources, ResourceType resType, Set<MeasurementDefinition> definitions) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        DatabaseType type = systemManager.getDatabaseType();

        String queryString;
        if (type instanceof PostgresqlDatabaseType || type instanceof H2DatabaseType) {
            queryString = "select defi.id, defi.display_name, defi.description,defi.category,defi.data_type, foo.coMin, foo.coMax, foo.coAny, foo.coAll "
                + " from RHQ_measurement_def defi, "
                + " ( "
                + "   select d.id as did, min(s.coll_interval) as coMin, max(s.coll_interval) as coMax, bool_or(s.enabled) as coAny, bool_and(s.enabled) as coAll "
                + "   from RHQ_MEASUREMENT_SCHED s,  RHQ_measurement_def d "
                + "   where s.definition = d.id "
                + "     and d.id IN (@@DEFINITIONS@@) "
                + "     and s.resource_id IN (@@RESOURCES@@) "
                + "   group by d.id " + " ) as foo " + " where defi.id = foo.did order by defi.display_name";
        } else if (type instanceof OracleDatabaseType) {
            queryString = "select defi.id, defi.display_name, defi.description, defi.category, defi.data_type, coMin, coMax, coAny, coAll "
                + " from RHQ_measurement_def defi, "
                + " ( "
                + "   select d.id as did,  min(s.coll_interval) as coMin, max(s.coll_interval) as coMax, min(s.enabled) as coAny, max(s.enabled) as coAll "
                + "   from RHQ_MEASUREMENT_SCHED s,  RHQ_measurement_def d "
                + "   where s.definition = d.id "
                + "     and d.id IN (@@DEFINITIONS@@) "
                + "     and s.resource_id IN (@@RESOURCES@@) "
                + "   group by d.id " + " ) " + " where defi.id = did order by defi.display_name";
        } else {
            throw new IllegalArgumentException("unknown database type, imlement this: " + type.toString());
        }

        int numDefs = definitions.size();
        queryString = JDBCUtil.transformQueryForMultipleInParameters(queryString, "@@DEFINITIONS@@", numDefs);
        int numResources = resources.size();
        if (numResources > 1000) //  if collection time is the same for 1000, assume that for all of them
        {
            numResources = 1000;
        }

        queryString = JDBCUtil.transformQueryForMultipleInParameters(queryString, "@@RESOURCES@@", numResources);

        int[] defIds = new int[numDefs];
        int i = 0;
        for (MeasurementDefinition def : definitions) {
            defIds[i++] = def.getId();
        }

        int[] resIds = new int[numResources];
        i = 0;
        for (Resource res : resources) {
            resIds[i++] = res.getId();
        }

        List<MeasurementScheduleComposite> compList = new ArrayList<MeasurementScheduleComposite>(numDefs);

        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(queryString);
            JDBCUtil.bindNTimes(ps, defIds, 1);
            JDBCUtil.bindNTimes(ps, resIds, numDefs + 1);
            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                int defId = resultSet.getInt(1);
                String defDisName = resultSet.getString(2);
                String defDescr = resultSet.getString(3);
                int category = resultSet.getInt(4);
                int dataType = resultSet.getInt(5);
                int maxInterval = resultSet.getInt(6);
                int minInterval = resultSet.getInt(7);
                int collectionInterval = (maxInterval == minInterval) ? maxInterval : 0; // 0 will be flagged as "DIFFERENT"
                Boolean collectionEnabled = null;
                if (type instanceof PostgresqlDatabaseType || type instanceof H2DatabaseType) {
                    boolean bOr = resultSet.getBoolean(8);
                    boolean bAnd = resultSet.getBoolean(9);
                    if (bOr == bAnd) {
                        collectionEnabled = (bOr) ? true : false;
                    } else {
                        collectionInterval = 0; // will be flagged as "DIFFERENT"
                    }
                } else if (type instanceof OracleDatabaseType) {
                    int boAny = resultSet.getInt(8);
                    int boAll = resultSet.getInt(9);
                    if (boAny == boAll) {
                        collectionEnabled = (boAny == 1) ? true : false;
                    } else {
                        collectionInterval = 0; // will be flagged as "DIFFERENT"
                    }
                } else {
                    throw new IllegalArgumentException("Unimplemented db type: " + type.toString());
                }

                MeasurementDefinition dummy = new MeasurementDefinition(resType, defDisName);
                dummy.setId(defId);
                dummy.setDescription(defDescr);
                dummy.setDisplayName(defDisName);
                dummy.setCategory(MeasurementCategory.values()[category]);
                dummy.setDataType(DataType.values()[dataType]);
                MeasurementScheduleComposite comp = new MeasurementScheduleComposite(dummy, collectionEnabled,
                    collectionInterval);
                compList.add(comp);
            }
        } catch (SQLException e) {
            log.error("Can not retrieve results: " + e.getMessage() + ", code= " + e.getErrorCode());
            compList = new ArrayList<MeasurementScheduleComposite>();
        } finally {
            JDBCUtil.safeClose(conn, ps, resultSet);
        }

        PageList<MeasurementScheduleComposite> result = new PageList<MeasurementScheduleComposite>(compList,
            pageControl);

        return result;
    }

    @SuppressWarnings("unchecked")
    public PageList<MeasurementScheduleComposite> getMeasurementScheduleCompositesForResource(Subject subject,
        int resourceId, @Nullable DataType dataType, PageControl pageControl) {
        pageControl.addDefaultOrderingField("ms.id");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            MeasurementSchedule.FIND_SCHEDULE_COMPOSITE_FOR_RESOURCE, pageControl);
        query.setParameter("resourceId", resourceId);
        query.setParameter("dataType", dataType);
        List<MeasurementScheduleComposite> results = query.getResultList();
        return new PageList<MeasurementScheduleComposite>(results, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<MeasurementSchedule> getMeasurementSchedulesForResource(Subject subject, int resourceId,
        @Nullable DataType dataType, @Nullable DisplayType displayType, @Nullable Boolean enabled,
        PageControl pageControl) {
        pageControl.addDefaultOrderingField("ms.id");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            MeasurementSchedule.FIND_ALL_FOR_RESOURCE_ID, pageControl);
        query.setParameter("resourceId", resourceId);
        query.setParameter("dataType", dataType);
        query.setParameter("displayType", displayType);
        query.setParameter("enabled", enabled);
        List<MeasurementSchedule> results = query.getResultList();
        return new PageList<MeasurementSchedule>(results, pageControl);
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void disableDefaultCollectionForMeasurementDefinitions(Subject subject, int[] measurementDefinitionIds,
        boolean updateSchedules) {

        modifyDefaultCollectionIntervalForMeasurementDefinitions(subject, measurementDefinitionIds, -1, updateSchedules);
        return;
    }

    public void disableMeasurementSchedules(Subject subject, int[] measurementDefinitionIds, int resourceId) {
        Resource resource = resourceManager.getResourceById(subject, resourceId);
        if (!authorizationManager.hasResourcePermission(subject, Permission.MANAGE_MEASUREMENTS, resourceId)) {
            throw new PermissionException("You do not have permission to change resource [" + resource
                + "]'s metric collection schedules");
        }

        List<MeasurementSchedule> measurementSchedules = getSchedulesByDefinitionIdsAndResourceId(
            measurementDefinitionIds, resourceId);
        ResourceMeasurementScheduleRequest resourceMeasurementScheduleRequest = new ResourceMeasurementScheduleRequest(
            resourceId);
        for (MeasurementSchedule measurementSchedule : measurementSchedules) {
            measurementSchedule.setEnabled(false);
            MeasurementScheduleRequest measurementScheduleRequest = new MeasurementScheduleRequest(measurementSchedule);
            resourceMeasurementScheduleRequest.addMeasurementScheduleRequest(measurementScheduleRequest);
        }

        Set<ResourceMeasurementScheduleRequest> resourceMeasurementScheduleRequests = new HashSet<ResourceMeasurementScheduleRequest>();
        resourceMeasurementScheduleRequests.add(resourceMeasurementScheduleRequest);
        boolean synced = sendUpdatedSchedulesToAgent(resource.getAgent(), resourceMeasurementScheduleRequests);
        if (!synced) {
            resource.setAgentSynchronizationNeeded();
        }
        entityManager.merge(resource);

        return;
    }

    @RequiredPermissions( { @RequiredPermission(Permission.MANAGE_INVENTORY),
        @RequiredPermission(Permission.MANAGE_SETTINGS) })
    public void disableAllDefaultCollections(Subject subject) {
        entityManager.createNamedQuery(MeasurementDefinition.DISABLE_ALL).executeUpdate();
    }

    @RequiredPermissions( { @RequiredPermission(Permission.MANAGE_INVENTORY),
        @RequiredPermission(Permission.MANAGE_SETTINGS) })
    public void disableAllMeasurementSchedules(Subject subject) {
        entityManager.createNamedQuery(MeasurementSchedule.DISABLE_ALL).executeUpdate();
        // TODO: how do we ensure the agents sync their schedules now so they turn off everything?
    }

    public void createSchedulesForExistingResources(ResourceType type, MeasurementDefinition newDefinition) {
        long now = System.currentTimeMillis();
        List<Resource> resources = type.getResources();
        if (resources != null) {
            for (Resource res : resources) {
                res.setMtime(now); // changing MTime tells the agent this resource needs to be synced
                MeasurementSchedule sched = new MeasurementSchedule(newDefinition, res);
                sched.setInterval(newDefinition.getDefaultInterval());
                entityManager.persist(sched);
            }
        }
        return;
    }

    /**
    * (Re-)Enables all collection schedules in the given measurement definition IDs and sets their collection
    * intervals. This only enables the "templates", it does not enable actual schedules unless updateExistingSchedules
    * is set to true.
    *
    * @param subject                  a valid subject that has Permission.MANAGE_SETTINGS
    * @param measurementDefinitionIds The primary keys for the definitions
    * @param collectionInterval       the new interval in millisconds for collection
    * @param updateExistingSchedules  If true, then existing schedules for this definition will also be updated.
    */
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void updateDefaultCollectionIntervalForMeasurementDefinitions(Subject subject,
        int[] measurementDefinitionIds, long collectionInterval, boolean updateExistingSchedules) {

        collectionInterval = verifyMinimumCollectionInterval(collectionInterval);
        modifyDefaultCollectionIntervalForMeasurementDefinitions(subject, measurementDefinitionIds, collectionInterval,
            updateExistingSchedules);
    }

    private void modifyDefaultCollectionIntervalForMeasurementDefinitions(Subject subject,
        int[] measurementDefinitionIds, long collectionInterval, boolean updateExistingSchedules) {

        if (measurementDefinitionIds == null || measurementDefinitionIds.length == 0) {
            log.debug("update metric template: no definitions supplied (interval = " + collectionInterval);
            return;
        }

        List<MeasurementDefinition> measurementDefinitions = getDefinitionsByIds(measurementDefinitionIds);
        for (MeasurementDefinition measurementDefinition : measurementDefinitions) {
            if (collectionInterval > 0) {
                measurementDefinition.setDefaultOn(true);
                measurementDefinition.setDefaultInterval(collectionInterval);
            } else {
                measurementDefinition.setDefaultOn(false);
            }
        }

        Map<Integer, ResourceMeasurementScheduleRequest> reqMap = new HashMap<Integer, ResourceMeasurementScheduleRequest>();
        if (updateExistingSchedules) {
            for (MeasurementDefinition measurementDefinition : measurementDefinitions) {
                // check if schedules need to be updated as well

                List<MeasurementSchedule> schedules = measurementDefinition.getSchedules();
                for (MeasurementSchedule sched : schedules) {
                    if (collectionInterval > 0) {
                        sched.setEnabled(true);
                        sched.setInterval(collectionInterval);
                    } else {
                        sched.setEnabled(false);
                    }

                    // Create update requests to feed to the agents
                    int resourceId = sched.getResource().getId();
                    if (!reqMap.containsKey(resourceId)) {
                        ResourceMeasurementScheduleRequest req = new ResourceMeasurementScheduleRequest(resourceId);
                        reqMap.put(resourceId, req);
                    }

                    ResourceMeasurementScheduleRequest req = reqMap.get(resourceId);
                    MeasurementScheduleRequest msr = new MeasurementScheduleRequest(sched);
                    req.addMeasurementScheduleRequest(msr);
                }
            }

            Map<Agent, Set<ResourceMeasurementScheduleRequest>> agentUpdates = null;
            agentUpdates = new HashMap<Agent, Set<ResourceMeasurementScheduleRequest>>();

            for (Integer resourceId : reqMap.keySet()) {
                Agent agent = agentManager.getAgentByResourceId(resourceId);

                Set<ResourceMeasurementScheduleRequest> agentUpdate = agentUpdates.get(agent);
                if (agentUpdate == null) {
                    agentUpdate = new HashSet<ResourceMeasurementScheduleRequest>();
                    agentUpdates.put(agent, agentUpdate);
                }

                agentUpdate.add(reqMap.get(resourceId));
            }

            // convert the int[] to Integer[], incase we need to set
            List<Integer> measurementDefinitionList = convertIntArrayToListOfIntegers(measurementDefinitionIds);
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
                    setAgentSynchronizationNeededByDefinitionsForAgent(agentId, measurementDefinitionList);
                }
            }
        }
    }

    private void setAgentSynchronizationNeededByDefinitionsForAgent(int agentId, List<Integer> measurementDefinitionIds) {
        String updateSQL = "" //
            + "UPDATE Resource res " //
            + "   SET res.mtime = :now " //
            + " WHERE res.agent.id = :agentId " //
            + "       res.resourceType.id IN ( SELECT md.resourceType.id " //
            + "                                  FROM MeasurementDefinition md " //
            + "                                 WHERE md.id IN ( :definitionIds ) )";
        Query updateQuery = entityManager.createQuery(updateSQL);
        updateQuery.setParameter("now", System.currentTimeMillis());
        updateQuery.setParameter("agentId", agentId);
        updateQuery.setParameter("definitionIds", measurementDefinitionIds);
        int updateCount = updateQuery.executeUpdate();
        log.info("" + updateCount + " resources mtime fields were updated as a result of this metric template update");
    }

    public void updateMeasurementSchedules(Subject subject, int[] measurementDefinitionIds, int resourceId,
        long collectionInterval) {
        collectionInterval = verifyMinimumCollectionInterval(collectionInterval);
        Resource resource = resourceManager.getResourceById(subject, resourceId);
        if (!authorizationManager.hasResourcePermission(subject, Permission.MANAGE_MEASUREMENTS, resourceId)) {
            throw new PermissionException("You do not have permission to change resource [" + resource
                + "]'s metric collection schedules");
        }

        List<MeasurementSchedule> measurementSchedules = getSchedulesByDefinitionIdsAndResourceId(
            measurementDefinitionIds, resourceId);
        ResourceMeasurementScheduleRequest resourceMeasurementScheduleRequest = new ResourceMeasurementScheduleRequest(
            resourceId);
        for (MeasurementSchedule measurementSchedule : measurementSchedules) {
            measurementSchedule.setEnabled(true);
            measurementSchedule.setInterval(collectionInterval);
            MeasurementScheduleRequest measurementScheduleRequest = new MeasurementScheduleRequest(measurementSchedule);
            resourceMeasurementScheduleRequest.addMeasurementScheduleRequest(measurementScheduleRequest);
        }

        Set<ResourceMeasurementScheduleRequest> resourceMeasurementScheduleRequests = new HashSet<ResourceMeasurementScheduleRequest>();
        resourceMeasurementScheduleRequests.add(resourceMeasurementScheduleRequest);
        boolean synced = sendUpdatedSchedulesToAgent(resource.getAgent(), resourceMeasurementScheduleRequests);
        if (!synced) {
            resource.setAgentSynchronizationNeeded();
        }
        entityManager.merge(resource);

        return;
    }

    /**
     * Enables all collection schedules attached to the given auto group whose schedules are based off the given
     * definitions. This does not enable the "templates" (aka definitions). If the passed group does not exist an
     * Exception is thrown.
     *
     * @param subject                  Subject of the caller
     * @param measurementDefinitionIds the definitions on which the schedules to update are based
     * @param parentResourceId         the Id of the parent resource
     * @param childResourceType        the ID of the {@link ResourceType} of the children that form the autogroup
     * @param collectionInterval       the new interval
     *
     * @see   org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal#updateMeasurementSchedulesForAutoGroup(org.rhq.core.domain.auth.Subject,
     *        int[], int, int, long)
     */
    public void updateMeasurementSchedulesForAutoGroup(Subject subject, int[] measurementDefinitionIds,
        int parentResourceId, int childResourceType, long collectionInterval) {
        // don't verify minimum collection interval here, it will be caught by updateMeasurementSchedules callee
        List<Resource> resources = resourceGroupManager.getResourcesForAutoGroup(subject, parentResourceId,
            childResourceType);
        for (Resource resource : resources) {
            updateMeasurementSchedules(subject, measurementDefinitionIds, resource.getId(), collectionInterval);
        }
    }

    /**
     * Enables all collection schedules attached to the given compatible group whose schedules are based off the given
     * definitions. This does not enable the "templates" (aka definitions). If the passed group is not compatible or
     * does not exist an Exception is thrown.
     *
     * @param subject                  Subject of the caller
     * @param measurementDefinitionIds the definitions on which the schedules to update are based
     * @param groupId                  ID of the group
     * @param collectionInterval       the new interval
     *
     * @see   org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal#updateMeasurementSchedulesForCompatGroup(org.rhq.core.domain.auth.Subject,
     *        int[], int, long)
     */
    public void updateMeasurementSchedulesForCompatGroup(Subject subject, int[] measurementDefinitionIds, int groupId,
        long collectionInterval) {
        // don't verify minimum collection interval here, it will be caught by updateMeasurementSchedules callee
        ResourceGroup group = resourceGroupManager.getResourceGroupById(subject, groupId, GroupCategory.COMPATIBLE);
        Set<Resource> resources = group.getExplicitResources();

        for (Resource resource : resources) {
            updateMeasurementSchedules(subject, measurementDefinitionIds, resource.getId(), collectionInterval);
        }
    }

    /**
     * Disable the measurement schedules for the passed definitions for the resources of the passed compatible group.
     */
    public void disableMeasurementSchedulesForCompatGroup(Subject subject, int[] measurementDefinitionIds, int groupId) {
        ResourceGroup group = resourceGroupManager.getResourceGroupById(subject, groupId, GroupCategory.COMPATIBLE);
        Set<Resource> resources = group.getExplicitResources();

        for (Resource resource : resources) {
            disableMeasurementSchedules(subject, measurementDefinitionIds, resource.getId());
        }
    }

    /**
     * Disable the measurement schedules for the passed definitions of the rsource ot the passed auto group.
     *
     * @param subject
     * @param measurementDefinitionIds
     * @param parentResourceId
     * @param childResourceType
     */
    public void disableMeasurementSchedulesForAutoGroup(Subject subject, int[] measurementDefinitionIds,
        int parentResourceId, int childResourceType) {
        List<Resource> resources = resourceGroupManager.getResourcesForAutoGroup(subject, parentResourceId,
            childResourceType);
        for (Resource resource : resources) {
            disableMeasurementSchedules(subject, measurementDefinitionIds, resource.getId());
        }
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
    public List<MeasurementSchedule> getMeasurementSchedulesForResourceAndType(Subject subject, int resourceId,
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
                log.debug("Won't send MeasurementSchedules to offline Agent[id=" + agent.getId() + "]");
                return false;
            }
            agentClient.getMeasurementAgentService().updateCollection(resourceMeasurementScheduleRequest);
            return true; // successfully sync'ed schedules down to the agent
        } catch (Throwable t) {
            log.error("Error updating MeasurementSchedules for Agent[id=" + agent.getId() + "]: " + t);
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
    @SuppressWarnings("unchecked")
    public List<MeasurementSchedule> getSchedulesByDefinitionIdsAndResourceId(int[] definitionIds, int resourceId) {
        Query query = entityManager.createNamedQuery(MeasurementSchedule.FIND_BY_DEFINITION_IDS_AND_RESOURCE_ID);
        List<Integer> definitionIdsList = convertIntArrayToListOfIntegers(definitionIds);
        query.setParameter("definitionIds", definitionIdsList);
        query.setParameter("resourceId", resourceId);
        List<MeasurementSchedule> results = query.getResultList();
        return results;
    }

    /**
     * Returns the list of MeasurementDefinitions with the given id's
     *
     * @param  ids id's of the desired definitions
     *
     * @return the list of MeasurementDefinitions for the list of id's
     */
    @SuppressWarnings("unchecked")
    private List<MeasurementDefinition> getDefinitionsByIds(int[] ids) {
        Query q = entityManager.createNamedQuery("MeasurementDefinition.findByIds");
        List<Integer> idsList = convertIntArrayToListOfIntegers(ids);
        q.setParameter("ids", idsList);
        List<MeasurementDefinition> results = q.getResultList();
        return results;
    }

    private List<Integer> convertIntArrayToListOfIntegers(int[] ints) {
        List<Integer> list = new ArrayList<Integer>();
        for (int x : ints) {
            list.add(x);
        }

        return list;
    }

    /**
     * This gets measurement schedules for a resource and optionally its dependents. It creates them as necessary.
     *
     * @param resourceIds    The ids of the resources to retrieve schedules for
     * @param allSchedules   The set to which the schedules should be added
     * @param getDescendents If true, schedules for all descendent resources will also be loaded
     */
    private void getSchedulesForResourceAndItsDescendants(Set<Integer> resourceIds,
        Set<ResourceMeasurementScheduleRequest> allSchedules, boolean getDescendents) {

        if (resourceIds == null || resourceIds.size() == 0) {
            // no work to do
            return;
        }

        try {
            Integer[] resourceIdArray = resourceIds.toArray(new Integer[resourceIds.size()]);
            for (int batchIndex = 0; batchIndex < resourceIdArray.length; batchIndex += 1000) {
                Integer[] batchIdArray = ArrayUtils.copyOfRange(resourceIdArray, batchIndex, batchIndex + 1000);
                List<Integer> batchIds = Arrays.asList(batchIdArray);

                /* 
                 * need to use a native query solution for both the insertion and returning the results because if we
                 * go through Hibernate to return the results it will not see the effects of the insert statement
                 */
                measurementScheduleManager.insertSchedulesFor(batchIds);
                measurementScheduleManager.returnSchedulesFor(batchIds, allSchedules);

                if (getDescendents) {
                    // recursively get all the default schedules for all children of the resource
                    Set<Integer> batchChildrenIds = getChildrenIdByParentIds(batchIds);
                    getSchedulesForResourceAndItsDescendants(batchChildrenIds, allSchedules, getDescendents);
                }
            }
        } catch (Throwable t) {
            log.warn("problem creating schedules for resourceIds [" + resourceIds + "]", t);
        }

        return;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int insertSchedulesFor(List<Integer> batchIds) throws Exception {
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
            } else {
                insertQueryString = MeasurementSchedule.NATIVE_QUERY_INSERT_SCHEDULES_ORACLE;
            }

            insertQueryString = JDBCUtil.transformQueryForMultipleInParameters(insertQueryString, "@@RESOURCES@@",
                batchIds.size());
            insertStatement = conn.prepareStatement(insertQueryString);

            int[] resourceIds = new int[batchIds.size()];
            for (int i = 0; i < resourceIds.length; i++) {
                resourceIds[i] = batchIds.get(i);
            }
            JDBCUtil.bindNTimes(insertStatement, resourceIds, 1);

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
    public int returnSchedulesFor(List<Integer> batchIds, Set<ResourceMeasurementScheduleRequest> allSchedules)
        throws Exception {
        Connection conn = null;
        PreparedStatement resultsStatement = null;

        int created = -1;
        try {
            conn = dataSource.getConnection();

            String resultsQueryString = MeasurementSchedule.NATIVE_QUERY_REPORTING_RESOURCE_MEASUREMENT_SCHEDULE_REQUEST;
            resultsQueryString = JDBCUtil.transformQueryForMultipleInParameters(resultsQueryString, "@@RESOURCES@@",
                batchIds.size());
            resultsStatement = conn.prepareStatement(resultsQueryString);

            int[] resourceIds = new int[batchIds.size()];
            for (int i = 0; i < resourceIds.length; i++) {
                resourceIds[i] = batchIds.get(i);
            }
            JDBCUtil.bindNTimes(resultsStatement, resourceIds, 1);

            Map<Integer, ResourceMeasurementScheduleRequest> scheduleRequestMap = new HashMap<Integer, ResourceMeasurementScheduleRequest>();
            ResultSet results = resultsStatement.executeQuery();
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
    private Set<Integer> getChildrenIdByParentIds(List<Integer> batchIds) {
        Query query = entityManager.createNamedQuery(Resource.QUERY_FIND_CHILDREN_IDS_BY_PARENT_IDS);
        query.setParameter("parentIds", batchIds);
        List<Integer> batchChildrenIds = query.getResultList();
        Set<Integer> batchChildrenIdSet = new HashSet<Integer>(batchChildrenIds);
        return batchChildrenIdSet;
    }

    public int getScheduledMeasurementsPerMinute() {
        Number rate = 0;
        try {
            rate = (Number) entityManager.createNamedQuery(MeasurementSchedule.GET_SCHEDULED_MEASUREMENTS_PER_MINUTED)
                .getSingleResult();
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
                updateMeasurementSchedulesForResources(updatedResourceIds);

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

    private void updateMeasurementSchedulesForResources(List<Integer> resourceIds) {
        if (resourceIds.size() == 0) {
            return;
        }

        // first get all the resources, which is needed to get the agent mappings
        Subject overlord = subjectManager.getOverlord();
        Integer[] resourceIdArray = resourceIds.toArray(new Integer[resourceIds.size()]);
        PageList<Resource> resources = resourceManager.getResourceByIds(overlord, resourceIdArray, false, PageControl
            .getUnlimitedInstance());

        // then get all the requests
        Set<Integer> resourceIdSet = new HashSet<Integer>(resourceIds);
        Set<ResourceMeasurementScheduleRequest> requests = getSchedulesForResourceAndItsDescendants(resourceIdSet,
            false);

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
}