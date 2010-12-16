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
package org.rhq.enterprise.server.operation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

import org.rhq.core.clientapi.agent.operation.CancelResults;
import org.rhq.core.clientapi.agent.operation.CancelResults.InterruptedState;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.composite.IntegerOptionItem;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.GroupOperationHistoryCriteria;
import org.rhq.core.domain.criteria.OperationDefinitionCriteria;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.GroupOperationScheduleEntity;
import org.rhq.core.domain.operation.HistoryJobId;
import org.rhq.core.domain.operation.JobId;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.OperationScheduleEntity;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.ResourceOperationScheduleEntity;
import org.rhq.core.domain.operation.ScheduleJobId;
import org.rhq.core.domain.operation.composite.GroupOperationLastCompletedComposite;
import org.rhq.core.domain.operation.composite.GroupOperationScheduleComposite;
import org.rhq.core.domain.operation.composite.ResourceOperationLastCompletedComposite;
import org.rhq.core.domain.operation.composite.ResourceOperationScheduleComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.server.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.exception.ScheduleException;
import org.rhq.enterprise.server.exception.UnscheduleException;
import org.rhq.enterprise.server.jaxb.adapter.ConfigurationAdapter;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupNotFoundException;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

@Stateless
public class OperationManagerBean implements OperationManagerLocal, OperationManagerRemote {
    private static final Log LOG = LogFactory.getLog(OperationManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AgentManagerLocal agentManager;
    @EJB
    private AlertConditionCacheManagerLocal alertConditionCacheManager;
    @EJB
    private AuthorizationManagerLocal authorizationManager;
    @EJB
    private ConfigurationManagerLocal configurationManager;
    @EJB
    private ResourceGroupManagerLocal resourceGroupManager;
    @EJB
    private ResourceManagerLocal resourceManager;
    @EJB
    private SchedulerLocal scheduler;
    @EJB
    private SubjectManagerLocal subjectManager;

    @SuppressWarnings("unchecked")
    public List<IntegerOptionItem> getResourceNameOptionItems(int groupId) {
        String queryName = ResourceGroup.QUERY_FIND_RESOURCE_NAMES_BY_GROUP_ID;

        PageControl pc = PageControl.getUnlimitedInstance();
        pc.addDefaultOrderingField("res.name");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);
        query.setParameter("groupId", groupId);

        List<IntegerOptionItem> results = query.getResultList();

        return results;
    }

    public ResourceOperationSchedule scheduleResourceOperation(Subject subject, int resourceId, String operationName,
        long delay, long repeatInterval, int repeatCount, int timeout,//
        @XmlJavaTypeAdapter(value = ConfigurationAdapter.class)//
        Configuration parameters, String notes) throws ScheduleException {
        try {

            SimpleTrigger trigger = new SimpleTrigger();
            if (delay < 0L) {
                delay = 0L;
            }
            trigger.setRepeatCount((repeatCount < 0) ? SimpleTrigger.REPEAT_INDEFINITELY : repeatCount);
            trigger.setRepeatInterval((repeatInterval < 0L) ? 0L : repeatInterval);
            trigger.setStartTime(new Date(System.currentTimeMillis() + delay));

            // if the user set a timeout, add it to our configuration
            if (timeout > 0L) {
                if (null == parameters) {
                    parameters = new Configuration();
                }

                parameters.put(new PropertySimple(OperationDefinition.TIMEOUT_PARAM_NAME, timeout));
            }

            return scheduleResourceOperation(subject, resourceId, operationName, parameters, trigger, notes);
        } catch (Exception e) {
            throw new ScheduleException(e);
        }
    }

    public ResourceOperationSchedule scheduleResourceOperation(Subject subject, int resourceId, String operationName,
        Configuration parameters, Trigger trigger, String notes) throws SchedulerException {
        Resource resource = getResourceIfAuthorized(subject, resourceId);

        ensureControlPermission(subject, resource);

        String uniqueJobId = createUniqueJobName(resource, operationName);

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(ResourceOperationJob.DATAMAP_STRING_OPERATION_NAME, operationName);
        putDisplayName(jobDataMap, resource.getResourceType().getId(), operationName);

        if (parameters != null) {
            if (parameters.getId() == 0) {
                entityManager.persist(parameters);
            }

            jobDataMap.putAsString(ResourceOperationJob.DATAMAP_INT_PARAMETERS_ID, parameters.getId());
        }

        jobDataMap.putAsString(ResourceOperationJob.DATAMAP_INT_SUBJECT_ID, subject.getId());
        jobDataMap.putAsString(ResourceOperationJob.DATAMAP_INT_RESOURCE_ID, resource.getId());

        JobDetail jobDetail = new JobDetail();
        jobDetail.setName(uniqueJobId);
        jobDetail.setGroup(createJobGroupName(resource));
        jobDetail.setDescription(notes);
        jobDetail.setVolatility(false); // we want it persisted
        jobDetail.setDurability(false);
        jobDetail.setRequestsRecovery(false);
        jobDetail.setJobClass(ResourceOperationJob.class);
        jobDetail.setJobDataMap(jobDataMap);

        trigger.setName(jobDetail.getName());
        trigger.setGroup(jobDetail.getGroup());
        trigger.setJobName(jobDetail.getName());
        trigger.setJobGroup(jobDetail.getGroup());

        // we need to create our own schedule tracking entity
        ResourceOperationScheduleEntity schedule;
        schedule = new ResourceOperationScheduleEntity(jobDetail.getName(), jobDetail.getGroup(), trigger
            .getStartTime(), resource);
        entityManager.persist(schedule);

        // now actually schedule it
        Date next = scheduler.scheduleJob(jobDetail, trigger);
        ResourceOperationSchedule newSchedule = getResourceOperationSchedule(subject, jobDetail);

        LOG.debug("Scheduled resource operation [" + newSchedule + "] - next fire time is [" + next + "]");

        return newSchedule;
    }

    private void putDisplayName(JobDataMap jobDataMap, int resourceTypeId, String operationName) {
        try {
            OperationDefinition operationDefintion = getOperationDefinitionByResourceTypeAndName(resourceTypeId,
                operationName, false);
            jobDataMap.put(OperationJob.DATAMAP_STRING_OPERATION_DISPLAY_NAME, operationDefintion.getDisplayName());
        } catch (OperationDefinitionNotFoundException odnfe) {
            jobDataMap.put(OperationJob.DATAMAP_STRING_OPERATION_DISPLAY_NAME, operationName);
        }
    }

    public GroupOperationSchedule scheduleGroupOperation(Subject subject, int compatibleGroupId,
        int[] executionOrderResourceIds, boolean haltOnFailure, String operationName, Configuration parameters,
        Trigger trigger, String notes) throws SchedulerException {
        ResourceGroup group = getCompatibleGroupIfAuthorized(subject, compatibleGroupId);

        ensureControlPermission(subject, group);

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(GroupOperationJob.DATAMAP_STRING_OPERATION_NAME, operationName);
        putDisplayName(jobDataMap, group.getResourceType().getId(), operationName);

        if (parameters != null) {
            if (parameters.getId() == 0) {
                entityManager.persist(parameters);
            }

            jobDataMap.putAsString(ResourceOperationJob.DATAMAP_INT_PARAMETERS_ID, parameters.getId());
        }

        jobDataMap.putAsString(GroupOperationJob.DATAMAP_INT_SUBJECT_ID, subject.getId());
        jobDataMap.putAsString(GroupOperationJob.DATAMAP_INT_GROUP_ID, group.getId());
        jobDataMap.putAsString(GroupOperationJob.DATAMAP_BOOL_HALT_ON_FAILURE, haltOnFailure);

        if ((executionOrderResourceIds != null) && (executionOrderResourceIds.length > 0)) {
            StringBuilder orderString = new StringBuilder();
            orderString.append(executionOrderResourceIds[0]);
            for (int i = 1; i < executionOrderResourceIds.length; i++) {
                orderString.append(',');
                orderString.append(executionOrderResourceIds[i]);
            }

            jobDataMap.put(GroupOperationJob.DATAMAP_INT_ARRAY_EXECUTION_ORDER, orderString.toString());
        }

        JobDetail jobDetail = new JobDetail();
        jobDetail.setName(createUniqueJobName(group, operationName));
        jobDetail.setGroup(createJobGroupName(group));
        jobDetail.setDescription(notes);
        jobDetail.setVolatility(false); // we want it persisted
        jobDetail.setDurability(false);
        jobDetail.setRequestsRecovery(false);
        jobDetail.setJobClass(GroupOperationJob.class);
        jobDetail.setJobDataMap(jobDataMap);

        trigger.setName(jobDetail.getName());
        trigger.setGroup(jobDetail.getGroup());
        trigger.setJobName(jobDetail.getName());
        trigger.setJobGroup(jobDetail.getGroup());

        // we need to create our own schedule tracking entity
        GroupOperationScheduleEntity schedule;
        schedule = new GroupOperationScheduleEntity(jobDetail.getName(), jobDetail.getGroup(), trigger.getStartTime(),
            group);
        entityManager.persist(schedule);

        // now actually schedule it
        Date next = scheduler.scheduleJob(jobDetail, trigger);
        GroupOperationSchedule newSchedule = getGroupOperationSchedule(subject, jobDetail);

        LOG.debug("Scheduled group operation [" + newSchedule + "] - next fire time is [" + next + "]");

        return newSchedule;
    }

    public void unscheduleResourceOperation(Subject subject, String jobId, int resourceId) throws UnscheduleException {
        try {
            // checks for view permissions
            Resource resource = getResourceIfAuthorized(subject, resourceId);

            ensureControlPermission(subject, resource);

            ResourceOperationSchedule schedule = getResourceOperationSchedule(subject, jobId);
            if (schedule.getParameters() != null) {
                Integer configId = schedule.getParameters().getId();
                Configuration parameters = configurationManager.getConfigurationById(configId);
                entityManager.remove(parameters);
            }

            ScheduleJobId jobIdObject = new ScheduleJobId(jobId);
            String jobName = jobIdObject.getJobName();
            String jobGroup = jobIdObject.getJobGroup();

            boolean deleted = scheduler.deleteJob(jobName, jobGroup);

            if (deleted) {
                deleteOperationScheduleEntity(jobIdObject);
            }
        } catch (Exception e) {
            throw new UnscheduleException(e);
        }

        return;
    }

    public void unscheduleGroupOperation(Subject subject, String jobId, int resourceGroupId) throws UnscheduleException {
        try {
            ResourceGroup group = resourceGroupManager.getResourceGroupById(subject, resourceGroupId,
                GroupCategory.COMPATIBLE);

            ensureControlPermission(subject, group);

            getCompatibleGroupIfAuthorized(subject, resourceGroupId); // just want to do this to check for permissions

            GroupOperationSchedule schedule = getGroupOperationSchedule(subject, jobId);
            if (schedule.getParameters() != null) {
                Integer configId = schedule.getParameters().getId();
                Configuration parameters = configurationManager.getConfigurationById(configId);
                entityManager.remove(parameters);
            }

            ScheduleJobId jobIdObject = new ScheduleJobId(jobId);
            String jobName = jobIdObject.getJobName();
            String jobGroup = jobIdObject.getJobGroup();

            boolean deleted = scheduler.deleteJob(jobName, jobGroup);

            if (deleted) {
                deleteOperationScheduleEntity(jobIdObject);
            }
        } catch (Exception e) {
            throw new UnscheduleException(e);
        }

        return;
    }

    public void deleteOperationScheduleEntity(ScheduleJobId jobId) {
        OperationScheduleEntity doomed = findOperationScheduleEntity(jobId);
        if (doomed != null) {
            LOG.debug("Deleting schedule entity: " + jobId);
            entityManager.remove(doomed);
        } else {
            LOG.info("Asked to delete unknown schedule - ignoring: " + jobId);
        }

        return;
    }

    public void updateOperationScheduleEntity(ScheduleJobId jobId, long nextFireTime) {
        // sched will be managed - just setting the property is enough for it to be committed
        OperationScheduleEntity sched = findOperationScheduleEntity(jobId);
        sched.setNextFireTime(nextFireTime);
        LOG.debug("Scheduled job has a new next-fire-time: " + sched);
        return;
    }

    public List<ResourceOperationSchedule> findScheduledResourceOperations(Subject subject, int resourceId)
        throws Exception {
        Resource resource = getResourceIfAuthorized(subject, resourceId);

        List<ResourceOperationSchedule> operationSchedules = new ArrayList<ResourceOperationSchedule>();

        String groupName = createJobGroupName(resource);
        String[] jobNames = scheduler.getJobNames(groupName);

        for (String jobName : jobNames) {
            JobDetail jobDetail = scheduler.getJobDetail(jobName, groupName);
            ResourceOperationSchedule sched = getResourceOperationSchedule(subject, jobDetail);

            if (resourceId != sched.getResource().getId()) {
                throw new IllegalStateException("Somehow a different resource [" + sched.getResource()
                    + "] was scheduled in the same job group as resource [" + resource + "]");
            }

            operationSchedules.add(sched);
        }

        return operationSchedules;
    }

    public List<GroupOperationSchedule> findScheduledGroupOperations(Subject subject, int groupId) throws Exception {
        ResourceGroup group = getCompatibleGroupIfAuthorized(subject, groupId);

        List<GroupOperationSchedule> operationSchedules = new ArrayList<GroupOperationSchedule>();

        String groupName = createJobGroupName(group);
        String[] jobNames = scheduler.getJobNames(groupName);

        for (String jobName : jobNames) {
            JobDetail jobDetail = scheduler.getJobDetail(jobName, groupName);
            GroupOperationSchedule sched = getGroupOperationSchedule(subject, jobDetail);

            if (groupId != sched.getGroup().getId()) {
                throw new IllegalStateException("Somehow a different group [" + sched.getGroup()
                    + "] was scheduled in the same job group as group [" + group + "]");
            }

            operationSchedules.add(sched);
        }

        return operationSchedules;
    }

    public ResourceOperationSchedule getResourceOperationSchedule(Subject subject, JobDetail jobDetail) {
        JobDataMap jobDataMap = jobDetail.getJobDataMap();

        String description = jobDetail.getDescription();
        String operationName = jobDataMap.getString(ResourceOperationJob.DATAMAP_STRING_OPERATION_NAME);
        String displayName = jobDataMap.getString(ResourceOperationJob.DATAMAP_STRING_OPERATION_DISPLAY_NAME);
        int subjectId = jobDataMap.getIntFromString(ResourceOperationJob.DATAMAP_INT_SUBJECT_ID);
        Configuration parameters = null;

        if (jobDataMap.containsKey(ResourceOperationJob.DATAMAP_INT_PARAMETERS_ID)) {
            int configId = jobDataMap.getIntFromString(ResourceOperationJob.DATAMAP_INT_PARAMETERS_ID);
            parameters = entityManager.find(Configuration.class, configId);
        }

        int resourceId = jobDataMap.getIntFromString(ResourceOperationJob.DATAMAP_INT_RESOURCE_ID);
        Resource resource = getResourceIfAuthorized(subject, resourceId);

        // note that we throw an exception if the subject does not exist!
        // this is by design to avoid a malicious user creating a dummy subject in the database,
        // scheduling a very bad operation, and deleting that subject thus removing all traces
        // of the user.  If the user has been removed from the system, all of that users schedules
        // will need to be deleted and rescheduled.

        ResourceOperationSchedule sched = new ResourceOperationSchedule();
        sched.setJobName(jobDetail.getName());
        sched.setJobGroup(jobDetail.getGroup());
        sched.setResource(resource);
        sched.setOperationName(operationName);
        sched.setOperationDisplayName(displayName);
        sched.setSubject(subjectManager.getSubjectById(subjectId));
        sched.setParameters(parameters);
        sched.setDescription(description);

        return sched;
    }

    public ResourceOperationSchedule getResourceOperationSchedule(Subject subject, String jobId)
        throws SchedulerException {
        JobId jobIdObject = new JobId(jobId);
        JobDetail jobDetail = scheduler.getJobDetail(jobIdObject.getJobName(), jobIdObject.getJobGroup());
        return getResourceOperationSchedule(subject, jobDetail);
    }

    public GroupOperationSchedule getGroupOperationSchedule(Subject subject, JobDetail jobDetail) {
        JobDataMap jobDataMap = jobDetail.getJobDataMap();

        String description = jobDetail.getDescription();
        String operationName = jobDataMap.getString(GroupOperationJob.DATAMAP_STRING_OPERATION_NAME);
        String displayName = jobDataMap.getString(GroupOperationJob.DATAMAP_STRING_OPERATION_DISPLAY_NAME);
        int subjectId = jobDataMap.getIntFromString(GroupOperationJob.DATAMAP_INT_SUBJECT_ID);
        Configuration parameters = null;

        if (jobDataMap.containsKey(GroupOperationJob.DATAMAP_INT_PARAMETERS_ID)) {
            int configId = jobDataMap.getIntFromString(ResourceOperationJob.DATAMAP_INT_PARAMETERS_ID);
            parameters = entityManager.find(Configuration.class, configId);
        }

        int groupId = jobDataMap.getIntFromString(GroupOperationJob.DATAMAP_INT_GROUP_ID);
        ResourceGroup group = getCompatibleGroupIfAuthorized(subject, groupId);

        List<Resource> executionOrder = null;

        if (jobDataMap.containsKey(GroupOperationJob.DATAMAP_INT_ARRAY_EXECUTION_ORDER)) {
            // if this property exists in the data map, we are assured that it has at least one ID in it
            String orderCommaSeparated = jobDataMap.getString(GroupOperationJob.DATAMAP_INT_ARRAY_EXECUTION_ORDER);
            String[] orderArray = orderCommaSeparated.split(",");
            for (String resourceIdString : orderArray) {
                int resourceId = Integer.parseInt(resourceIdString);
                Resource memberResource = entityManager.find(Resource.class, resourceId);

                if (memberResource != null) {
                    if (executionOrder == null) {
                        executionOrder = new ArrayList<Resource>();
                    }

                    if (!executionOrder.contains(memberResource)) {
                        executionOrder.add(memberResource);
                    }
                } else {
                    LOG.debug("Resource [" + resourceId
                        + "] looks like it was deleted and is no longer a member of group [" + group
                        + "] - ignoring it");
                }
            }
        }

        GroupOperationSchedule sched = new GroupOperationSchedule();
        sched.setJobName(jobDetail.getName());
        sched.setJobGroup(jobDetail.getGroup());
        sched.setGroup(group);
        sched.setOperationName(operationName);
        sched.setOperationDisplayName(displayName);
        sched.setSubject(subjectManager.getSubjectById(subjectId));
        sched.setParameters(parameters);
        sched.setExecutionOrder(executionOrder);
        sched.setDescription(description);
        sched.setHaltOnFailure(jobDataMap.getBooleanValueFromString(GroupOperationJob.DATAMAP_BOOL_HALT_ON_FAILURE));

        return sched;
    }

    public GroupOperationSchedule getGroupOperationSchedule(Subject subject, String jobId) throws SchedulerException {
        JobId jobIdObject = new JobId(jobId);
        JobDetail jobDetail = scheduler.getJobDetail(jobIdObject.getJobName(), jobIdObject.getJobGroup());
        return getGroupOperationSchedule(subject, jobDetail);
    }

    public OperationHistory getOperationHistoryByHistoryId(Subject subject, int historyId) {
        OperationHistory history = entityManager.find(OperationHistory.class, historyId);

        if (history == null) {
            throw new RuntimeException("Cannot get history - it does not exist: " + historyId);
        }

        if (history.getParameters() != null) {
            history.getParameters().getId(); // eagerly load it
        }

        if (history instanceof ResourceOperationHistory) {
            ResourceOperationHistory resourceHistory = (ResourceOperationHistory) history;
            if (resourceHistory.getResults() != null) {
                resourceHistory.getResults().getId(); // eagerly load it
            }
        }

        ensureViewPermission(subject, history);

        return history;
    }

    @SuppressWarnings("unchecked")
    public PageList<ResourceOperationHistory> findResourceOperationHistoriesByGroupHistoryId(Subject subject,
        int historyId, PageControl pc) {
        pc.initDefaultOrderingField("h.createdTime", PageOrdering.DESC);

        String queryName = ResourceOperationHistory.QUERY_FIND_BY_GROUP_OPERATION_HISTORY_ID;
        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        queryCount.setParameter("groupHistoryId", historyId);
        query.setParameter("groupHistoryId", historyId);

        long totalCount = (Long) queryCount.getSingleResult();
        List<ResourceOperationHistory> list = query.getResultList();

        PageList<ResourceOperationHistory> pagedResourceHistories;
        pagedResourceHistories = new PageList<ResourceOperationHistory>(list, (int) totalCount, pc);
        return pagedResourceHistories;
    }

    public OperationHistory getOperationHistoryByJobId(Subject subject, String historyJobId) {
        HistoryJobId jobIdObject = new HistoryJobId(historyJobId);

        Query query = entityManager.createNamedQuery(OperationHistory.QUERY_FIND_BY_JOB_ID);
        query.setParameter("jobName", jobIdObject.getJobName());
        query.setParameter("jobGroup", jobIdObject.getJobGroup());
        query.setParameter("createdTime", jobIdObject.getCreatedTime());

        OperationHistory history;

        try {
            history = (OperationHistory) query.getSingleResult();
        } catch (Exception e) {
            history = null;
        }

        if (history == null) {
            throw new RuntimeException("Cannot get history - it does not exist: " + historyJobId);
        }

        ensureViewPermission(subject, history);

        return history;
    }

    @SuppressWarnings("unchecked")
    public PageList<ResourceOperationHistory> findCompletedResourceOperationHistories(Subject subject, int resourceId,
        Long beginDate, Long endDate, PageControl pc) {
        pc.initDefaultOrderingField("h.createdTime", PageOrdering.DESC);

        String queryName = ResourceOperationHistory.QUERY_FIND_BY_RESOURCE_ID_AND_NOT_STATUS;
        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        queryCount.setParameter("resourceId", resourceId);
        query.setParameter("resourceId", resourceId);

        queryCount.setParameter("status", OperationRequestStatus.INPROGRESS);
        query.setParameter("status", OperationRequestStatus.INPROGRESS);

        queryCount.setParameter("beginTime", beginDate);
        query.setParameter("beginTime", beginDate);

        queryCount.setParameter("endTime", endDate);
        query.setParameter("endTime", endDate);

        long totalCount = (Long) queryCount.getSingleResult();

        List<ResourceOperationHistory> list = query.getResultList();

        // don't bother checking permission if there is nothing to see (we wouldn't have the group even if we wanted to)
        // if there is at least one history - get its group and make sure the user has permissions to see
        if ((list != null) && (list.size() > 0)) {
            ResourceOperationHistory resourceHistory = list.get(0);
            ensureViewPermission(subject, resourceHistory);
        }

        PageList<ResourceOperationHistory> pageList;
        pageList = new PageList<ResourceOperationHistory>(list, (int) totalCount, pc);
        return pageList;
    }

    @SuppressWarnings("unchecked")
    public PageList<ResourceOperationHistory> findPendingResourceOperationHistories(Subject subject, int resourceId,
        PageControl pc) {
        pc.initDefaultOrderingField("h.createdTime", PageOrdering.ASC);

        String queryName = ResourceOperationHistory.QUERY_FIND_BY_RESOURCE_ID_AND_STATUS;
        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        queryCount.setParameter("resourceId", resourceId);
        query.setParameter("resourceId", resourceId);

        queryCount.setParameter("status", OperationRequestStatus.INPROGRESS);
        query.setParameter("status", OperationRequestStatus.INPROGRESS);

        long totalCount = (Long) queryCount.getSingleResult();

        List<ResourceOperationHistory> list = query.getResultList();

        // don't bother checking permission if there is nothing to see (we wouldn't have the group even if we wanted to)
        // if there is at least one history - get its group and make sure the user has permissions to see
        if ((list != null) && (list.size() > 0)) {
            ResourceOperationHistory resourceHistory = list.get(0);
            ensureViewPermission(subject, resourceHistory);
        }

        PageList<ResourceOperationHistory> pageList;
        pageList = new PageList<ResourceOperationHistory>(list, (int) totalCount, pc);
        return pageList;
    }

    @SuppressWarnings("unchecked")
    public PageList<GroupOperationHistory> findCompletedGroupOperationHistories(Subject subject, int groupId,
        PageControl pc) {
        pc.initDefaultOrderingField("h.createdTime", PageOrdering.DESC);

        String queryName = GroupOperationHistory.QUERY_FIND_BY_GROUP_ID_AND_NOT_STATUS;
        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        queryCount.setParameter("groupId", groupId);
        query.setParameter("groupId", groupId);

        queryCount.setParameter("status", OperationRequestStatus.INPROGRESS);
        query.setParameter("status", OperationRequestStatus.INPROGRESS);

        long totalCount = (Long) queryCount.getSingleResult();

        List<GroupOperationHistory> list = query.getResultList();

        // don't bother checking permission if there is nothing to see (we wouldn't have the group even if we wanted to)
        // if there is at least one history - get its group and make sure the user has permissions to see
        if ((list != null) && (list.size() > 0)) {
            GroupOperationHistory groupHistory = list.get(0);
            ensureViewPermission(subject, groupHistory);
        }

        PageList<GroupOperationHistory> pageList;
        pageList = new PageList<GroupOperationHistory>(list, (int) totalCount, pc);
        return pageList;
    }

    @SuppressWarnings("unchecked")
    public PageList<GroupOperationHistory> findPendingGroupOperationHistories(Subject subject, int groupId,
        PageControl pc) {
        pc.initDefaultOrderingField("h.createdTime", PageOrdering.ASC);

        String queryName = GroupOperationHistory.QUERY_FIND_BY_GROUP_ID_AND_STATUS;
        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        queryCount.setParameter("groupId", groupId);
        query.setParameter("groupId", groupId);

        queryCount.setParameter("status", OperationRequestStatus.INPROGRESS);
        query.setParameter("status", OperationRequestStatus.INPROGRESS);

        long totalCount = (Long) queryCount.getSingleResult();

        List<GroupOperationHistory> list = query.getResultList();

        // don't bother checking permission if there is nothing to see (we wouldn't have the group even if we wanted to)
        // if there is at least one history - get its group and make sure the user has permissions to see
        if ((list != null) && (list.size() > 0)) {
            GroupOperationHistory groupHistory = list.get(0);
            ensureViewPermission(subject, groupHistory);
        }

        PageList<GroupOperationHistory> pageList;
        pageList = new PageList<GroupOperationHistory>(list, (int) totalCount, pc);
        return pageList;
    }

    public OperationHistory updateOperationHistory(Subject subject, OperationHistory history) {
        /*
         * either the user wants to execute an operation on some resource or some group, or the OperationServerService
         * just got the results of the operation from the agent is needs to update this method.  thus, we only need to
         * ensure control permissions if the user isn't the overlord.
         */
        if (!authorizationManager.isOverlord(subject)) {
            ensureControlPermission(subject, history);
        }

        // we do not cascade add the param config (we probably can add that but), so let's persist it now
        Configuration parameters = history.getParameters();
        if ((parameters != null) && (parameters.getId() == 0)) {
            entityManager.persist(parameters);
            history.setParameters(parameters);
        }

        history = entityManager.merge(history); // merge will persist if it doesn't exist yet

        if (history.getParameters() != null) {
            history.getParameters().getId(); // eagerly reload the parameters
        }

        notifyAlertConditionCacheManager("updateOperationHistory", history);
        return history;
    }

    public void cancelOperationHistory(Subject subject, int historyId, boolean ignoreAgentErrors) {
        OperationHistory doomedHistory = getOperationHistoryByHistoryId(subject, historyId); // this also checks authorization so we don't have to do it again

        ensureControlPermission(subject, doomedHistory);

        // Do different things depending whether this is a group or resource history being canceled.
        // If group history - cancel all individual resource invocations that are part of that group.
        // If resource history - tell the agent to cancel it
        if (doomedHistory instanceof GroupOperationHistory) {
            cancelGroupOperation(subject, (GroupOperationHistory) doomedHistory, ignoreAgentErrors);
        } else {
            cancelResourceOperation(subject, (ResourceOperationHistory) doomedHistory, ignoreAgentErrors);
        }

        return;
    }

    /**
     * Cancels the group operation and puts it in the {@link OperationRequestStatus#CANCELED} state.
     *
     * <p>This will attempt to notify the agent(s) that the operations should be canceled. Any agent cannot be contacted
     * or an exception occurs while trying communicating with the agent, the given history will only be flagged as
     * canceled if <code>ignoreAgentErrors</code> is <code>true</code>. If <code>ignoreAgentErrors</code> is <code>
     * false</code> and an error occurs when communicating with any agent, the given group history will not be flagged
     * as canceled.</p>
     *
     * <p>Note that, if there are no agent errors, this method will always mark the group history as CANCELED,
     * regardless of the number of child resource operations that were actually canceled. If one or more operations had
     * already finished on the agent but not yet notified the server (that is, some resource operations are still
     * INPROGRESS on server-side), the group operation will still be shown as canceled. This is to indicate that at
     * least some, but not necessarily all, resource operations within the group were canceled. Showing CANCELED status
     * will be an audit flag to show you that this group was asked to not complete fully.</p>
     *
     * <p>It is assumed the caller of this private method has already verified that the user is authorized to perform
     * the cancelation.</p>
     *
     * @param  subject            the user who is canceling the operation
     * @param  doomedHistory     identifies the group operation (and all its resource operations) to cancel
     * @param  ignoreAgentErrors indicates how agent errors are handled
     *
     * @throws IllegalStateException if the operation history status is not in the
     *                               {@link OperationRequestStatus#INPROGRESS} state
     */
    private void cancelGroupOperation(Subject subject, GroupOperationHistory doomedHistory, boolean ignoreAgentErrors) {
        if (doomedHistory.getStatus() != OperationRequestStatus.INPROGRESS) {
            throw new IllegalStateException("The group job is no longer in-progress - cannot cancel it: "
                + doomedHistory);
        }

        boolean hadAgentError = false;

        List<ResourceOperationHistory> doomedResourceHistories = doomedHistory.getResourceOperationHistories();
        for (ResourceOperationHistory doomedResourceHistory : doomedResourceHistories) {
            try {
                CancelResults results = cancelResourceOperation(subject, doomedResourceHistory, ignoreAgentErrors);
                if (results == null) {
                    hadAgentError = true;
                }
            } catch (IllegalStateException ignore) {
                // it wasn't in progress - which is fine, nothing to cancel then and its already stopped
            }
        }

        if (!hadAgentError || ignoreAgentErrors) {
            // TODO: we might need to do some optimistic locking - what if the agent updates the history now?
            doomedHistory.setStatus(OperationRequestStatus.CANCELED); // we expect doomedHistory to be jpa attached
            notifyAlertConditionCacheManager("cancelGroupOperation", doomedHistory);
        }

        return;
    }

    /**
     * Cancels the operation history and puts it in the {@link OperationRequestStatus#CANCELED} state.
     *
     * <p>This will attempt to notify the agent that the operation should be canceled. If the agent cannot be contacted
     * or an exception occurs while trying communicating with the agent, the given history will only be flagged as
     * canceled if <code>ignoreAgentErrors</code> is <code>true</code>. If <code>ignoreAgentErrors</code> is <code>
     * false</code> and an error occurs when communicating with the agent, the given history will not be flagged as
     * canceled.</p>
     *
     * <p>It is assumed the caller of this private method has already verified that the user is authorized to perform
     * the cancelation.</p>
     *
     * @param  subject            the user who is canceling the operation
     * @param  doomedHistory     identifies the resource operation to cancel
     * @param  ignoreAgentErrors indicates how agent errors are handled
     *
     * @return the results from the agent which will be <code>null</code> if failed to successfully communicate with the
     *         agent
     *
     * @throws IllegalStateException if the operation history status is not in the
     *                               {@link OperationRequestStatus#INPROGRESS} state
     */
    private CancelResults cancelResourceOperation(Subject subject, ResourceOperationHistory doomedHistory,
        boolean ignoreAgentErrors) throws IllegalStateException {
        if (doomedHistory.getStatus() != OperationRequestStatus.INPROGRESS) {
            throw new IllegalStateException("The job is no longer in-progress - cannot cancel it: " + doomedHistory);
        }

        String jobIdString = doomedHistory.getJobId().toString();
        int resourceId = doomedHistory.getResource().getId();
        CancelResults results = null;
        AgentClient agent = null;
        boolean canceled = false;

        try {
            agent = agentManager.getAgentClient(subject, resourceId);

            // since this method is usually called by the UI, we want to quickly determine if we can even talk to the agent
            if (agent.ping(5000L)) {
                results = agent.getOperationAgentService().cancelOperation(jobIdString);

                InterruptedState interruptedState = results.getInterruptedState();

                switch (interruptedState) {
                case FINISHED: {
                    // If the agent says the interrupted state was FINISHED, we should not set the history state
                    // to canceled because it can't be canceled after its completed.  Besides, the agent will very
                    // shortly send us the "success" message which will set the state to SUCCESS.  Under odd circumstances
                    // (like the agent crashing after it finished the op but before it had a chance to send the "success"
                    // message), it will eventually be flagged as timed-out, but this is extremely rare since the
                    // "success" message is sent with guaranteed delivery - so the crash had to occur in just the right
                    // split second of time for that to occur.

                    LOG.debug("Agent already finished the operation so it cannot be canceled. " + "agent=[" + agent
                        + "], op=[" + doomedHistory + "]");
                    break;
                }

                case QUEUED: {
                    // If the agent says the interrupted state was QUEUED, this is good.  The agent never even
                    // got a chance to tell its plugin to execute the operation; it just dequeued and threw the op away.
                    // Therefore, we can really say it was canceled.
                    canceled = true;
                    LOG.debug("Cancel successful. Agent dequeued the operation and will not invoke it. " + "agent=["
                        + agent + "], op=[" + doomedHistory + "]");
                    break;
                }

                case RUNNING: {
                    // If the agent says the interrupted state was RUNNING, it means it was told to cancel the
                    // operation while it was already being invoked by the plugin.  This means the cancel may or may not have
                    // really worked.  The agent will have tried to interrupt the plugin, but if the plugin ignored
                    // the cancel request (e.g. to avoid putting the resource in an inconsistent state) we'll never know.
                    // We still flag the operation as canceled to indicate that the agent did attempt to cancel it;
                    // hopefully, the plugin did the right thing.
                    canceled = true;
                    LOG
                        .debug("Agent attempted to cancel the operation - it interrupted the operation while it was running. "
                            + "agent=[" + agent + "], op=[" + doomedHistory + "]");
                    break;
                }

                case UNKNOWN: {
                    // If the agent didn't know anything about the operation invocation, its probably because
                    // it crashed after the operation was initially submitted.  I guess it
                    // could also mean that the agent has just finished the operation and erased its memory of its
                    // existence (but it was so recent that the INPROGRESS state has not yet had time to be committed
                    // to one of its terminal states like SUCCESS or FAILURE).
                    // This is going to be a rare interrupted state.  It means the agent doesn't know anything about
                    // the operation.  In this case, we'll allow its state to be canceled since the most probably reason
                    // for this is the agent was recycled and has no idea what this operation is or was.
                    canceled = true;
                    LOG.debug("Agent does not know about the operation. Nothing to cancel. " + "agent=[" + agent
                        + "], op=[" + doomedHistory + "]");
                    break;
                }

                default: {
                    // someone added a constant to the interrupted state enum but didn't update this code
                    throw new RuntimeException("Please report this bug - bad state: " + interruptedState);
                }
                }
            } else {
                LOG.warn("Agent down? Cannot cancel operation. agent=[" + agent + "], op=[" + doomedHistory + "]");
            }
        } catch (Throwable t) {
            LOG.warn("Cannot tell the agent to cancel operation. agent=[" + agent + "], op=[" + doomedHistory + "]", t);
        }

        // if the agent canceled it or we failed to talk to the agent but we are allowed to ignore that failure
        if (canceled || ((results == null) && ignoreAgentErrors)) {
            // TODO: we might need to do some optimistic locking - what if the agent updates the history now?
            doomedHistory.setStatus(OperationRequestStatus.CANCELED); // we expect doomedHistory to be jpa attached
            notifyAlertConditionCacheManager("cancelResourceOperation", doomedHistory);
        }

        return results;
    }

    public void deleteOperationHistory(Subject subject, int historyId, boolean purgeInProgress) {
        OperationHistory doomedHistory = getOperationHistoryByHistoryId(subject, historyId); // this also checks authorization so we don't have to do it again

        ensureControlPermission(subject, doomedHistory);

        if ((doomedHistory.getStatus() == OperationRequestStatus.INPROGRESS) && !purgeInProgress) {
            throw new IllegalStateException(
                "The job is still in the in-progress state. Please wait for it to complete: " + doomedHistory);
        }

        if (doomedHistory instanceof GroupOperationHistory) {
            List<ResourceOperationHistory> resourceHistories = ((GroupOperationHistory) doomedHistory)
                .getResourceOperationHistories();
            for (ResourceOperationHistory child : resourceHistories) {
                deleteOperationHistory_helper(child.getId());
            }
        }

        deleteOperationHistory_helper(doomedHistory.getId());

        return;
    }

    @SuppressWarnings("unchecked")
    private void deleteOperationHistory_helper(int historyId) {
        Query historyArgumentsQuery = entityManager
            .createNamedQuery(OperationHistory.QUERY_GET_PARAMETER_CONFIGURATION_IDS);
        Query historyResultsQuery = entityManager.createNamedQuery(OperationHistory.QUERY_GET_RESULT_CONFIGURATION_IDS);
        historyArgumentsQuery.setParameter("historyId", historyId);
        historyResultsQuery.setParameter("historyId", historyId);

        List<Integer> historyArgumentConfigurationIds = historyArgumentsQuery.getResultList();
        List<Integer> historyResultConfigurationIds = historyResultsQuery.getResultList();

        Query operationHistoryDeleteQuery = entityManager
            .createNamedQuery(OperationHistory.QUERY_DELETE_BY_HISTORY_IDS);
        operationHistoryDeleteQuery.setParameter("historyId", historyId);
        operationHistoryDeleteQuery.executeUpdate();

        List<Integer> allConfigurationIdsToDelete = new ArrayList<Integer>(historyArgumentConfigurationIds.size()
            + historyResultConfigurationIds.size());
        allConfigurationIdsToDelete.addAll(historyArgumentConfigurationIds);
        allConfigurationIdsToDelete.addAll(historyResultConfigurationIds);
        configurationManager.deleteConfigurations(allConfigurationIdsToDelete);

        return;
    }

    @SuppressWarnings("unchecked")
    public List<OperationDefinition> findSupportedResourceOperations(Subject subject, int resourceId,
        boolean eagerLoaded) {
        if (!authorizationManager.canViewResource(subject, resourceId)) {
            throw new PermissionException("User [" + subject + "] does not have permission to view resource ["
                + resourceId + "]");
        }

        try {
            String queryName = eagerLoaded ? OperationDefinition.QUERY_FIND_BY_RESOURCE_AND_NAME
                : OperationDefinition.QUERY_FIND_LIGHT_WEIGHT_BY_RESOURCE_AND_NAME;

            Query query = entityManager.createNamedQuery(queryName);
            query.setParameter("resourceId", resourceId);
            query.setParameter("operationName", null);

            List<OperationDefinition> results = query.getResultList();
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Cannot get support operations for resource [" + resourceId + "]: " + e, e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<OperationDefinition> findSupportedResourceTypeOperations(Subject subject, int resourceTypeId,
        boolean eagerLoaded) {
        try {
            String queryName = eagerLoaded ? OperationDefinition.QUERY_FIND_BY_TYPE_AND_NAME
                : OperationDefinition.QUERY_FIND_LIGHT_WEIGHT_BY_TYPE_AND_NAME;

            Query query = entityManager.createNamedQuery(queryName);
            query.setParameter("resourceTypeId", resourceTypeId);
            query.setParameter("operationName", null);

            List<OperationDefinition> results = query.getResultList();
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Cannot get support operations for resourceType [" + resourceTypeId + "]: " + e,
                e);
        }
    }

    @SuppressWarnings( { "unchecked" })
    public List<OperationDefinition> findSupportedGroupOperations(Subject subject, int compatibleGroupId,
        boolean eagerLoaded) {
        if (!authorizationManager.canViewGroup(subject, compatibleGroupId)) {
            throw new PermissionException("User [" + subject + "] does not have permission to view group ["
                + compatibleGroupId + "]");
        }

        try {
            String queryName = eagerLoaded ? OperationDefinition.QUERY_FIND_BY_GROUP_AND_NAME
                : OperationDefinition.QUERY_FIND_LIGHT_WEIGHT_BY_GROUP_AND_NAME;

            Query query = entityManager.createNamedQuery(queryName);
            query.setParameter("groupId", compatibleGroupId);
            query.setParameter("operationName", null);

            List<OperationDefinition> results = query.getResultList();
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Cannot get support operations for group [" + compatibleGroupId + "]: " + e, e);
        }
    }

    @SuppressWarnings("unchecked")
    public OperationDefinition getSupportedResourceOperation(Subject subject, int resourceId, String operationName,
        boolean eagerLoaded) {
        if (!authorizationManager.canViewResource(subject, resourceId)) {
            throw new PermissionException("User [" + subject + "] does not have permission to view resource ["
                + resourceId + "]");
        }

        String queryName = eagerLoaded ? OperationDefinition.QUERY_FIND_BY_RESOURCE_AND_NAME
            : OperationDefinition.QUERY_FIND_LIGHT_WEIGHT_BY_RESOURCE_AND_NAME;

        Query query = entityManager.createNamedQuery(queryName);
        query.setParameter("resourceId", resourceId);
        query.setParameter("operationName", operationName);
        List<OperationDefinition> results = query.getResultList();

        if (results.size() != 1) {
            throw new RuntimeException("Found " + results.size() + " operations called [" + operationName
                + "] for resource [" + resourceId + "]: ");
        }

        return results.get(0);
    }

    @SuppressWarnings("unchecked")
    public OperationDefinition getSupportedGroupOperation(Subject subject, int compatibleGroupId, String operationName,
        boolean eagerLoaded) {
        if (!authorizationManager.canViewGroup(subject, compatibleGroupId)) {
            throw new PermissionException("User [" + subject + "] does not have permission to view group ["
                + compatibleGroupId + "]");
        }

        String queryName = eagerLoaded ? OperationDefinition.QUERY_FIND_BY_GROUP_AND_NAME
            : OperationDefinition.QUERY_FIND_LIGHT_WEIGHT_BY_GROUP_AND_NAME;

        Query query = entityManager.createNamedQuery(queryName);
        query.setParameter("groupId", compatibleGroupId);
        query.setParameter("operationName", operationName);
        List<OperationDefinition> results = query.getResultList();

        if (results.size() != 1) {
            throw new RuntimeException("Found " + results.size() + " operations called [" + operationName
                + "] for group [" + compatibleGroupId + "]: ");
        }

        return results.get(0);
    }

    public boolean isResourceOperationSupported(Subject subject, int resourceId) {
        Resource resource;

        try {
            resource = getResourceIfAuthorized(subject, resourceId);
        } catch (PermissionException e) {
            // notice we caught this exception before propogating it up to the EJB layer, so
            // our transaction is not rolled back
            LOG.debug("isOperationSupported: User cannot control resource: " + resourceId);
            return false;
        } catch (Exception e) {
            LOG.debug("isOperationSupported: Resource does not exist: " + resourceId);
            return false;
        }

        Set<OperationDefinition> defs = resource.getResourceType().getOperationDefinitions();

        return (defs != null) && (defs.size() > 0);
    }

    public boolean isGroupOperationSupported(Subject subject, int resourceGroupId) {
        ResourceGroup group;

        try {
            group = resourceGroupManager.getResourceGroupById(subject, resourceGroupId, null);
            if (group.getGroupCategory() == GroupCategory.MIXED) {
                return false;
            }
        } catch (ResourceGroupNotFoundException e) {
            LOG.debug("isGroupOperationSupported: group does not exist: " + resourceGroupId);
            return false;
        } catch (PermissionException pe) {
            // notice we caught this exception before propogating it up to the EJB layer, so
            // our transaction is not rolled back
            LOG.debug("isGroupOperationSupported: User cannot view (and thus) control group: " + resourceGroupId);
            return false;
        }

        if (!authorizationManager.hasGroupPermission(subject, Permission.CONTROL, group.getId())) {
            LOG.debug("isGroupOperationSupported: User cannot control group: " + group);
            return false;
        }

        Set<OperationDefinition> defs = group.getResourceType().getOperationDefinitions();

        return (defs != null) && (defs.size() > 0);
    }

    @SuppressWarnings("unchecked")
    public void checkForTimedOutOperations(Subject subject) {
        LOG.debug("Begin scanning for timed out operation histories");

        if (!authorizationManager.isOverlord(subject)) {
            LOG.debug("Unauthorized user " + subject + " tried to execute checkForTimedOutOperations: "
                + "only the overlord may execute this system operation");
            return;
        }

        // the purpose of this method is really to clean up in progress histories when we detect
        // they probably will never move out of the in progress status.  This will occur if the
        // agent dies before it has a chance to report success/failure.  In that case, we'll never
        // get an agent completion message and the history will remain in progress status forever.
        // This method just tried to detect this scenario - if it finds a history that has been
        // in progress for a very long time, we assume we'll never hear from the agent and time out
        // that history item (that is, set its status to FAILURE and set an error string).
        try {
            Query query = entityManager.createNamedQuery(ResourceOperationHistory.QUERY_FIND_ALL_IN_STATUS);
            query.setParameter("status", OperationRequestStatus.INPROGRESS);
            List<ResourceOperationHistory> histories = query.getResultList();
            for (ResourceOperationHistory history : histories) {
                long timeout = getOperationTimeout(history.getOperationDefinition(), history.getParameters());

                long duration = history.getDuration();
                if (duration > timeout) {
                    LOG.info("Operation execution seems to have been orphaned - timing it out: " + history);
                    history.setErrorMessage("Timed out : did not complete after " + duration + " ms"
                        + " (the timeout period was " + timeout + " ms)");
                    history.setStatus(OperationRequestStatus.FAILURE);
                    notifyAlertConditionCacheManager("checkForTimedOutOperations", history);

                    // If it's part of a group request, check if all member requests of the group request have completed,
                    // and, if so, update the group request's status.
                    checkForCompletedGroupOperation(history.getId());
                }
            }
        } catch (Throwable t) {
            LOG.warn("Failed to check for timed out resource operations. Cause: " + t);
        }

        /*
         * since multiple resource operation histories from different agents can report at the same time,
         * it's possible that OperationManagerBean.updateOperationHistory will be called concurrently from
         * two different transactions. when this happens, neither thread sees what the other is doing (due
         * to current transaction isolation level settings), so the handler code that checks to see whether
         * some resourceOperationHistory was the last one in the group to finish might not function the way
         * you'd think it would in a purely serial environment.
         *
         * since CheckForTimedOutOperationsJob is already called on a periodic basis (currently set for 60
         * seconds), if we fix it to also check for this condition - any group operations that are INPROGRESS
         * but which don't have any INPROGRESS resource operation children - we needn't touch anything that
         * concerns the in-band handler code mentioned in the above paragraph.
         */
        try {
            /*
             * the first part of the logic needs to check for group operations that have timed out.  these are
             * operations that are still in progress that *do* have children in progress.  if any children are
             * in progress, they should be canceled (the server may have already submitted the resource-level
             * job down to the agent, in which case the cancel request may or may not be honored).  thus, it's
             * technically possible for a group operation to have timed out, one or more resource operation
             * children to be marked as canceled, and the agent later come back with a successful result.  in the
             * strictest sense, the group operation still timed out, even though all resource operations completed
             * successfully.  the timing on this is extremely slim, but possible.  the group operation error
             * message should make a note of this potentiality.
             */
            Query query = entityManager.createNamedQuery(GroupOperationHistory.QUERY_FIND_ACTIVE_IN_PROGRESS);
            query.setParameter("status", OperationRequestStatus.INPROGRESS);
            List<GroupOperationHistory> groupHistories = query.getResultList();
            for (GroupOperationHistory groupHistory : groupHistories) {

                long timeout = getOperationTimeout(groupHistory.getOperationDefinition(), groupHistory.getParameters());
                if (groupHistory.getDuration() < timeout) {
                    /*
                     * this INPROGRESS group operation has some children that are still INPROGRESS, but since this
                     * group operation hasn't timed out yet continue waiting for the children to complete normally.
                     */
                    continue;
                }

                /*
                 * otherwise, the group operation has timed out, even though some of its children are still INPROGRESS
                 */
                for (ResourceOperationHistory resourceHistory : groupHistory.getResourceOperationHistories()) {
                    if (resourceHistory.getStatus() == OperationRequestStatus.INPROGRESS) {
                        /*
                         * when resource-level operations are still INPROGRESS, let's try to cancel them on a
                         * best-effort basis; however, we don't want to fail and throw exceptions out of this
                         * job if the agent (for one or more target resources) can not be reached; so, we'll
                         * mark the resource-level operations as CANCELED but we'll gracefully ignore agent errors.
                         *
                         * if by some chance the agent happens to later ping back its connected server with updated
                         * data about one or more resource-level operation results, it will override the CANCELED
                         * status, but the group operation will still have a descriptive error message as to why
                         * it FAILED (in this case, timeout) and what happened as a result (cancellation messages
                         * where sent to the remaining INPROGRESS elements).
                         */
                        cancelOperationHistory(subject, resourceHistory.getId(), true);
                        break;
                    }
                }

                groupHistory.setErrorMessage("This group operation timed out "
                    + "before all child resource operations could complete normally, "
                    + "those still in progress will attempt to be canceled.");
                groupHistory.setStatus(OperationRequestStatus.FAILURE);
            }
        } catch (Throwable t) {
            LOG.warn("Failed to check for completed group operations. Cause: " + t);
        }

        try {
            /*
             * the second part of the logic needs to check for abandoned group operations.  these are records
             * whose resource-level children have all reached some terminating state (canceled / failed / success),
             * but the group operation was not marked accordingly because of the possibility that two resource
             * operations were completing at the same time and thought that the other was still processing, thus
             * the update to the containing group operation was erroneously skipped.
             */
            Query query = entityManager.createNamedQuery(GroupOperationHistory.QUERY_FIND_ABANDONED_IN_PROGRESS);
            query.setParameter("status", OperationRequestStatus.INPROGRESS);
            List<GroupOperationHistory> groupHistories = query.getResultList();
            for (GroupOperationHistory groupHistory : groupHistories) {

                /*
                 * assume success at first, override with failure for resource-level operation failures only;
                 * we'll be a little lenient with the logic here, and say that if a group operation hasn't already
                 * been marked for timeout, and if all of its children reach some terminating state, that it
                 * can not be marked for timeout now...it can only be marked as failure if there was in fact a
                 * resource-level operation failure
                 */
                OperationRequestStatus groupStatus = OperationRequestStatus.SUCCESS;

                for (ResourceOperationHistory resourceHistory : groupHistory.getResourceOperationHistories()) {
                    if (resourceHistory.getStatus() != OperationRequestStatus.SUCCESS) {
                        /*
                         * some child was either canceled or failed for some reason, and so the group operation must
                         * also be marked as failed.   remember, group operations are only a success if all resource
                         * operation children succeeded.
                         */
                        groupStatus = OperationRequestStatus.FAILURE;
                        break;
                    }
                }
                // only set the error message if one or more resource operations weren't successful
                if (groupStatus != OperationRequestStatus.SUCCESS) {
                    groupHistory.setErrorMessage("One or more resource operations timed out and/or did not complete");
                }
                // always set the status
                groupHistory.setStatus(groupStatus);
            }
        } catch (Throwable t) {
            LOG.warn("Failed to check for completed group operations. Cause: " + t);
        }

        try {
            Query query = entityManager.createNamedQuery(GroupOperationHistory.QUERY_FIND_MEMBERLESS_IN_PROGRESS);
            query.setParameter("status", OperationRequestStatus.INPROGRESS);
            List<GroupOperationHistory> groupHistories = query.getResultList();

            for (GroupOperationHistory groupHistory : groupHistories) {
                /*
                 * since no children histories ended in the FAILURE state (because there were no resource members at
                 * the time this group operation was kicked off), the group operation was a success by definition
                 */
                groupHistory.setStatus(OperationRequestStatus.SUCCESS);
                continue;
            }
        } catch (Throwable t) {
            LOG.warn("Failed to check for memberless group operations. Cause: " + t);
        }

        LOG.debug("Finished scanning for timed out operation histories");
    }

    @SuppressWarnings("unchecked")
    public PageList<ResourceOperationLastCompletedComposite> findRecentlyCompletedResourceOperations(Subject subject,
        Integer resourceId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("ro.createdTime", PageOrdering.ASC);

        Query query;
        Query count;

        if (authorizationManager.isInventoryManager(subject)) {
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                OperationHistory.QUERY_GET_RECENTLY_COMPLETED_RESOURCE_ADMIN, pageControl);
            count = PersistenceUtility.createCountQuery(entityManager,
                OperationHistory.QUERY_GET_RECENTLY_COMPLETED_RESOURCE_ADMIN);
        } else {
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                OperationHistory.QUERY_GET_RECENTLY_COMPLETED_RESOURCE, pageControl);
            count = PersistenceUtility.createCountQuery(entityManager,
                OperationHistory.QUERY_GET_RECENTLY_COMPLETED_RESOURCE);
            query.setParameter("subject", subject);
            count.setParameter("subject", subject);
        }

        query.setParameter("resourceId", resourceId);
        count.setParameter("resourceId", resourceId);

        int totalCount = ((Number) count.getSingleResult()).intValue();
        List<ResourceOperationLastCompletedComposite> results = query.getResultList();

        return new PageList<ResourceOperationLastCompletedComposite>(results, totalCount, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<GroupOperationLastCompletedComposite> findRecentlyCompletedGroupOperations(Subject subject,
        PageControl pageControl) {
        pageControl.initDefaultOrderingField("go.createdTime", PageOrdering.ASC);

        Query query;
        Query count;

        if (authorizationManager.isInventoryManager(subject)) {
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                OperationHistory.QUERY_GET_RECENTLY_COMPLETED_GROUP_ADMIN, pageControl);
            count = PersistenceUtility.createCountQuery(entityManager,
                OperationHistory.QUERY_GET_RECENTLY_COMPLETED_GROUP_ADMIN);
        } else {
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                OperationHistory.QUERY_GET_RECENTLY_COMPLETED_GROUP, pageControl);
            count = PersistenceUtility.createCountQuery(entityManager,
                OperationHistory.QUERY_GET_RECENTLY_COMPLETED_GROUP);
            query.setParameter("subject", subject);
            count.setParameter("subject", subject);
        }

        int totalCount = ((Number) count.getSingleResult()).intValue();
        List<GroupOperationLastCompletedComposite> results = query.getResultList();

        return new PageList<GroupOperationLastCompletedComposite>(results, totalCount, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<ResourceOperationScheduleComposite> findCurrentlyScheduledResourceOperations(Subject subject,
        PageControl pageControl) {
        pageControl.initDefaultOrderingField("ro.nextFireTime", PageOrdering.DESC);
        Query query;
        Query count;

        if (authorizationManager.isInventoryManager(subject)) {
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                OperationScheduleEntity.QUERY_GET_SCHEDULE_RESOURCE_ADMIN, pageControl);
            count = PersistenceUtility.createCountQuery(entityManager,
                OperationScheduleEntity.QUERY_GET_SCHEDULE_RESOURCE_ADMIN);
        } else {
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                OperationScheduleEntity.QUERY_GET_SCHEDULE_RESOURCE, pageControl);
            count = PersistenceUtility.createCountQuery(entityManager,
                OperationScheduleEntity.QUERY_GET_SCHEDULE_RESOURCE);
            query.setParameter("subject", subject);
            count.setParameter("subject", subject);
        }

        int totalCount = ((Number) count.getSingleResult()).intValue();
        List<ResourceOperationScheduleComposite> results = query.getResultList();

        // TODO: the schedule entity cannot map to operation display name, so the composite needs to
        // get the operation name set separately.  We need to change the data model to
        // link the operation definition ID to the schedule entity.  But what happens if the plugin
        // metadata changes - is it enough just to link with the op def ID or does plugin munging
        // when changing operation names not preserve op def IDs? We may want to do away with
        // the job detail datamap properties and just use the schedule entity to store all the data
        // associated with a scheduled operation. But this extra work we do here in practice will
        // not take long becaues the callers of this method normally limit the number of operations
        // returned to something very small (i.e. less than 10).
        Subject overlord = subjectManager.getOverlord();
        for (ResourceOperationScheduleComposite composite : results) {
            try {
                ResourceOperationSchedule sched = getResourceOperationSchedule(subject, composite.getOperationJobId()
                    .toString());
                OperationDefinition def = getSupportedResourceOperation(overlord, composite.getResourceId(), sched
                    .getOperationName(), false);
                composite.setOperationName((def.getDisplayName() != null) ? def.getDisplayName() : sched
                    .getOperationName());
            } catch (SchedulerException se) {
                LOG.error("A schedule entity is out of sync with the scheduler - there is no job scheduled: "
                    + composite, se);
            } catch (Exception e) {
                LOG.error("A scheduled operation has an invalid name - did a plugin change its operation metadata? : "
                    + composite, e);
            }
        }

        return new PageList<ResourceOperationScheduleComposite>(results, totalCount, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<GroupOperationScheduleComposite> findCurrentlyScheduledGroupOperations(Subject subject,
        PageControl pageControl) {
        pageControl.initDefaultOrderingField("go.nextFireTime", PageOrdering.DESC);
        Query query;
        Query count;

        if (authorizationManager.isInventoryManager(subject)) {
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                OperationScheduleEntity.QUERY_GET_SCHEDULE_GROUP_ADMIN, pageControl);
            count = PersistenceUtility.createCountQuery(entityManager,
                OperationScheduleEntity.QUERY_GET_SCHEDULE_GROUP_ADMIN);
        } else {
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                OperationScheduleEntity.QUERY_GET_SCHEDULE_GROUP, pageControl);
            count = PersistenceUtility
                .createCountQuery(entityManager, OperationScheduleEntity.QUERY_GET_SCHEDULE_GROUP);
            query.setParameter("subject", subject);
            count.setParameter("subject", subject);
        }

        int totalCount = ((Number) count.getSingleResult()).intValue();
        List<GroupOperationScheduleComposite> results = query.getResultList();

        // TODO: the schedule entity cannot map to operation display name, so the composite needs to
        // get the operation name set separately.  We need to change the data model to
        // link the operation definition ID to the schedule entity.  But what happens if the plugin
        // metadata changes - is it enough just to link with the op def ID or does plugin munging
        // when changing operation names not preserve op def IDs? We may want to do away with
        // the job detail datamap properties and just use the schedule entity to store all the data
        // associated with a scheduled operation. But this extra work we do here in practice will
        // not take long becaues the callers of this method normally limit the number of operations
        // returned to something very small (i.e. less than 10).
        Subject overlord = subjectManager.getOverlord();
        for (GroupOperationScheduleComposite composite : results) {
            try {
                GroupOperationSchedule sched = getGroupOperationSchedule(subject, composite.getOperationJobId()
                    .toString());
                OperationDefinition def = getSupportedGroupOperation(overlord, composite.getGroupId(), sched
                    .getOperationName(), false);
                composite.setOperationName((def.getDisplayName() != null) ? def.getDisplayName() : sched
                    .getOperationName());
            } catch (SchedulerException se) {
                LOG.error("A schedule entity is out of sync with the scheduler - there is no job scheduled: "
                    + composite, se);
            } catch (Exception e) {
                LOG.error("A scheduled operation has an invalid name - did a plugin change its operation metadata? : "
                    + composite, e);
            }
        }

        return new PageList<GroupOperationScheduleComposite>(results, totalCount, pageControl);
    }

    /**
     * Given the id of a history object, this will see if it has completed and if it is a resource history that is part
     * of an overall group operation and if so will see if all of its peer resource histories are also complete. If the
     * group members are all done, the group history object will have its status updated to reflect that the group
     * itself is done.
     *
     * @param historyId id of a history object
     */
    public void checkForCompletedGroupOperation(int historyId) {
        OperationHistory history = entityManager.find(OperationHistory.class, historyId);
        if (!(history instanceof ResourceOperationHistory)) {
            // if the history isn't even a resource history, then we have nothing to do
            return;
        }

        if (history.getStatus() == OperationRequestStatus.INPROGRESS) {
            // if this history isn't done, then by definition the overall group isn't done either
            return;
        }

        GroupOperationHistory groupHistory = ((ResourceOperationHistory) history).getGroupOperationHistory();

        // if this was a resource invocation that was part of a group operation
        // see if the rest of the group members are done too, if so, close out the group history
        if (groupHistory != null) {
            List<ResourceOperationHistory> allResourceHistories = groupHistory.getResourceOperationHistories();
            boolean stillInProgress = false; // assume all are finished
            OperationRequestStatus groupStatus = OperationRequestStatus.SUCCESS; // will be FAILURE if at least one resource operation failed
            StringBuilder groupErrorMessage = null; // will be the group error message if at least one resource operation failed

            for (ResourceOperationHistory resourceHistory : allResourceHistories) {
                if (resourceHistory.getStatus() == OperationRequestStatus.INPROGRESS) {
                    stillInProgress = true;
                    break;
                } else if ((resourceHistory.getStatus() == OperationRequestStatus.FAILURE)
                    || (resourceHistory.getStatus() == OperationRequestStatus.CANCELED)) {
                    groupStatus = OperationRequestStatus.FAILURE;
                    if (groupErrorMessage == null) {
                        groupErrorMessage = new StringBuilder(
                            "The following resources failed to invoke the operation: ");
                    } else {
                        groupErrorMessage.append(',');
                    }

                    groupErrorMessage.append(resourceHistory.getResource().getName());
                }
            }

            if (!stillInProgress) {
                groupHistory.setErrorMessage((groupErrorMessage == null) ? null : groupErrorMessage.toString());
                groupHistory.setStatus(groupStatus);
                notifyAlertConditionCacheManager("checkForCompletedGroupOperation", groupHistory);
            }
        }

        return;
    }

    /**
     * Returns the timeout of an operation, in <b>milliseconds</b>. If the <code>parameters</code> is
     * non-<code>null</code>, this first sees if a {@link OperationDefinition#TIMEOUT_PARAM_NAME} simple property is in
     * it and if so, uses it as the timeout. Otherwise, the timeout defined in the operation definition is used. If the
     * definition doesn't define a timeout either, the default timeout as configured in the scheduler will be used.
     *
     * @param  operationDefinition
     * @param  parameters
     *
     * @return the timeout in milliseconds
     */
    private long getOperationTimeout(OperationDefinition operationDefinition, Configuration parameters) {
        Integer timeout = null;

        // see if the caller put the timeout in an invocation parameter
        if (parameters != null) {
            PropertySimple timeoutProperty = parameters.getSimple(OperationDefinition.TIMEOUT_PARAM_NAME);
            if (timeoutProperty != null) {
                timeout = timeoutProperty.getIntegerValue();
            }
        }

        // if nothing in the parameters, then go to the operation definition
        if (timeout == null) {
            timeout = operationDefinition.getTimeout();

            // if the operation definition doesn't tell us what timeout to use, ask the scheduler for a default timeout
            if (timeout == null) {
                timeout = scheduler.getDefaultOperationTimeout();
            }
        }

        // user had N ways to define the timeout and didn't choose to do any... just provide a hardcoded value
        if (timeout == null) {
            timeout = 3600; // 1 hour
        }

        return timeout * 1000L;
    }

    private Resource getResourceIfAuthorized(Subject subject, int resourceId) {
        Resource resource;

        try {
            // resourceManager will test for necessary permissions too
            resource = resourceManager.getResourceById(subject, resourceId);
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException("Cannot get support operations for unknown resource [" + resourceId + "]: " + e,
                e);
        }

        return resource;
    }

    private ResourceGroup getCompatibleGroupIfAuthorized(Subject subject, int compatibleGroupId) {
        ResourceGroup group;

        try {
            // resourceGroupManager will test for necessary permissions too
            group = resourceGroupManager.getResourceGroupById(subject, compatibleGroupId, GroupCategory.COMPATIBLE);
        } catch (ResourceGroupNotFoundException e) {
            throw new RuntimeException("Cannot get support operations for unknown group [" + compatibleGroupId + "]: "
                + e, e);
        }

        return group;
    }

    private void ensureControlPermission(Subject subject, OperationHistory history) throws PermissionException {
        if (history instanceof GroupOperationHistory) {
            ResourceGroup group = ((GroupOperationHistory) history).getGroup();
            ensureControlPermission(subject, group);
        } else {
            Resource resource = ((ResourceOperationHistory) history).getResource();
            ensureControlPermission(subject, resource);
        }
    }

    private void ensureControlPermission(Subject subject, ResourceGroup group) throws PermissionException {
        if (!authorizationManager.hasGroupPermission(subject, Permission.CONTROL, group.getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to control group [" + group + "]");
        }
    }

    private void ensureControlPermission(Subject subject, Resource resource) throws PermissionException {
        if (!authorizationManager.hasResourcePermission(subject, Permission.CONTROL, resource.getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to control resource [" + resource + "]");
        }
    }

    private void ensureViewPermission(Subject subject, OperationHistory history) throws PermissionException {
        if (history instanceof GroupOperationHistory) {
            ResourceGroup group = ((GroupOperationHistory) history).getGroup();
            if (!authorizationManager.canViewGroup(subject, group.getId())) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view operation history for group [" + group + "]");
            }
        } else {
            Resource resource = ((ResourceOperationHistory) history).getResource();
            if (!authorizationManager.canViewResource(subject, resource.getId())) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view operation history for resource [" + resource + "]");
            }
        }
    }

    private String createUniqueJobName(Resource resource, String operationName) {
        return ResourceOperationJob.createUniqueJobName(resource, operationName);
    }

    private String createUniqueJobName(ResourceGroup group, String operationName) {
        return GroupOperationJob.createUniqueJobName(group, operationName);
    }

    private String createJobGroupName(Resource resource) {
        return ResourceOperationJob.createJobGroupName(resource);
    }

    private String createJobGroupName(ResourceGroup group) {
        return GroupOperationJob.createJobGroupName(group);
    }

    @Nullable
    public ResourceOperationHistory getLatestCompletedResourceOperation(Subject subject, int resourceId) {
        LOG.debug("Getting latest completed operation for resource [" + resourceId + "]");

        ResourceOperationHistory result;

        // get the latest operation known to the server (i.e. persisted in the DB)
        try {
            Query query = entityManager
                .createNamedQuery(ResourceOperationHistory.QUERY_FIND_LATEST_COMPLETED_OPERATION);
            query.setParameter("resourceId", resourceId);
            result = (ResourceOperationHistory) query.getSingleResult();
        } catch (NoResultException nre) {
            result = null; // there is no operation history for this resource yet
        }

        return result;
    }

    @Nullable
    public ResourceOperationHistory getOldestInProgressResourceOperation(Subject subject, int resourceId) {
        LOG.debug("Getting oldest in-progress operation for resource [" + resourceId + "]");

        ResourceOperationHistory result;

        // get the latest operation known to the server (i.e. persisted in the DB)
        try {
            Query query = entityManager
                .createNamedQuery(ResourceOperationHistory.QUERY_FIND_OLDEST_INPROGRESS_OPERATION);
            query.setParameter("resourceId", resourceId);
            result = (ResourceOperationHistory) query.getSingleResult();
        } catch (NoResultException nre) {
            result = null; // there is no operation history for this resource yet
        }

        return result;
    }

    public OperationDefinition getOperationDefinition(Subject subject, int operationId) {
        OperationDefinition operationDefinition = entityManager.find(OperationDefinition.class, operationId);

        if (operationDefinition == null) {
            throw new RuntimeException("Cannot get operation definition - it does not exist: " + operationId);
        }

        return operationDefinition;
    }

    @SuppressWarnings("unchecked")
    public OperationDefinition getOperationDefinitionByResourceTypeAndName(int resourceTypeId, String operationName,
        boolean eagerLoaded) throws OperationDefinitionNotFoundException {
        String queryName = eagerLoaded ? OperationDefinition.QUERY_FIND_BY_TYPE_AND_NAME
            : OperationDefinition.QUERY_FIND_LIGHT_WEIGHT_BY_TYPE_AND_NAME;
        Query query = entityManager.createNamedQuery(queryName);
        query.setParameter("resourceTypeId", resourceTypeId);
        query.setParameter("operationName", operationName);
        List<OperationDefinition> results = query.getResultList();

        if (results.size() != 1) {
            throw new OperationDefinitionNotFoundException("There were " + results.size() + " operations called "
                + operationName + " for resourceType[id=" + resourceTypeId + "]");
        }

        return results.get(0);
    }

    private void notifyAlertConditionCacheManager(String callingMethod, OperationHistory operationHistory) {
        AlertConditionCacheStats stats = alertConditionCacheManager.checkConditions(operationHistory);

        LOG.debug(callingMethod + ": " + stats.toString());
    }

    /**
     * Returns a managed entity of the scheduled job.
     *+
     * @param  jobId
     *
     * @return a managed entity, attached to this bean's entity manager
     */
    private OperationScheduleEntity findOperationScheduleEntity(ScheduleJobId jobId) {
        OperationScheduleEntity entity = entityManager.find(OperationScheduleEntity.class, jobId);
        return entity;
    }

    public GroupOperationSchedule scheduleGroupOperation(Subject subject, int groupId, int[] executionOrderResourceIds,
        boolean haltOnFailure, String operationName, Configuration parameters, long delay, long repeatInterval,
        int repeatCount, int timeout, String description) throws ScheduleException {

        try {

            SimpleTrigger trigger = new SimpleTrigger();
            if (delay < 0L) {
                delay = 0L;
            }
            trigger.setRepeatCount((repeatCount < 0) ? SimpleTrigger.REPEAT_INDEFINITELY : repeatCount);
            trigger.setRepeatInterval((repeatInterval < 0L) ? 0L : repeatInterval);
            trigger.setStartTime(new Date(System.currentTimeMillis() + delay));

            // if the user set a timeout, add it to our configuration
            if (timeout > 0L) {
                if (null == parameters) {
                    parameters = new Configuration();
                }

                parameters.put(new PropertySimple(OperationDefinition.TIMEOUT_PARAM_NAME, timeout));
            }

            return scheduleGroupOperation(subject, groupId, executionOrderResourceIds, haltOnFailure, operationName,
                parameters, trigger, description);
        } catch (Exception e) {
            throw new ScheduleException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<OperationDefinition> findOperationDefinitionsByCriteria(Subject subject,
        OperationDefinitionCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        ;

        CriteriaQueryRunner<OperationDefinition> queryRunner = new CriteriaQueryRunner(criteria, generator,
            entityManager);
        return queryRunner.execute();
    }

    @SuppressWarnings("unchecked")
    public PageList<ResourceOperationHistory> findResourceOperationHistoriesByCriteria(Subject subject,
        ResourceOperationHistoryCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        ;
        if (authorizationManager.isInventoryManager(subject) == false) {
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE, subject
                .getId());
        }

        CriteriaQueryRunner<ResourceOperationHistory> queryRunner = new CriteriaQueryRunner(criteria, generator,
            entityManager);
        return queryRunner.execute();
    }

    @SuppressWarnings("unchecked")
    public PageList<GroupOperationHistory> findGroupOperationHistoriesByCriteria(Subject subject,
        GroupOperationHistoryCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        ;
        if (authorizationManager.isInventoryManager(subject) == false) {
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.GROUP, subject
                .getId());
        }

        CriteriaQueryRunner<GroupOperationHistory> queryRunner = new CriteriaQueryRunner(criteria, generator,
            entityManager);
        return queryRunner.execute();
    }
}