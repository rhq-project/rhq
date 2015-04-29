/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.operation;

import static javax.ejb.TransactionAttributeType.NEVER;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

import org.rhq.core.clientapi.agent.operation.CancelResults;
import org.rhq.core.clientapi.agent.operation.CancelResults.InterruptedState;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.JobTrigger;
import org.rhq.core.domain.common.composite.IntegerOptionItem;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUtility;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
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
import org.rhq.core.domain.operation.bean.GroupOperationSchedule;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.operation.composite.GroupOperationLastCompletedComposite;
import org.rhq.core.domain.operation.composite.GroupOperationScheduleComposite;
import org.rhq.core.domain.operation.composite.ResourceOperationLastCompletedComposite;
import org.rhq.core.domain.operation.composite.ResourceOperationScheduleComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceAncestryFormat;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.common.ApplicationException;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.exception.ScheduleException;
import org.rhq.enterprise.server.exception.UnscheduleException;
import org.rhq.enterprise.server.measurement.instrumentation.MeasurementMonitor;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupNotFoundException;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.storage.StorageNodeOperationsHandlerLocal;
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
    private OperationManagerLocal operationManager;
    @EJB
    private ResourceGroupManagerLocal resourceGroupManager;
    @EJB
    private ResourceManagerLocal resourceManager;
    @EJB
    private SchedulerLocal scheduler;
    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private StorageNodeOperationsHandlerLocal storageNodeOperationsHandler;

    @Override
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

    @Override
    public ResourceOperationSchedule scheduleResourceOperation(Subject subject, int resourceId, String operationName,
        long delay, long repeatInterval, int repeatCount, int timeout, Configuration parameters, String notes)
        throws ScheduleException {
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

    @Override
    public ResourceOperationSchedule scheduleResourceOperationUsingCron(Subject subject, int resourceId,
        String operationName, String cronExpression, int timeout, Configuration parameters, String description)
        throws ScheduleException {
        // if the user set a timeout, add it to our configuration
        if (timeout > 0L) {
            if (parameters == null) {
                parameters = new Configuration();
            }
            parameters.put(new PropertySimple(OperationDefinition.TIMEOUT_PARAM_NAME, timeout));
        }
        try {
            CronTrigger cronTrigger = new CronTrigger("resource " + resourceId + "_" + operationName, "group",
                cronExpression);
            return scheduleResourceOperation(subject, resourceId, operationName, parameters, cronTrigger, description);
        } catch (Exception e) {
            throw new ScheduleException(e);
        }
    }

    @Override
    public int scheduleResourceOperation(Subject subject, ResourceOperationSchedule schedule) throws ScheduleException {
        JobTrigger jobTrigger = schedule.getJobTrigger();
        Trigger trigger = convertToTrigger(jobTrigger);
        try {
            ResourceOperationSchedule resourceOperationSchedule = scheduleResourceOperation(subject, schedule
                .getResource().getId(), schedule.getOperationName(), schedule.getParameters(), trigger,
                schedule.getDescription());
            return resourceOperationSchedule.getId();
        } catch (SchedulerException e) {
            throw new ScheduleException(e);
        }
    }

    @Override
    public int scheduleGroupOperation(Subject subject, GroupOperationSchedule schedule) throws ScheduleException {
        JobTrigger jobTrigger = schedule.getJobTrigger();
        Trigger trigger = convertToTrigger(jobTrigger);
        try {
            List<Resource> executionOrderResources = schedule.getExecutionOrder();
            int[] executionOrderResourceIds;
            if (executionOrderResources == null) {
                executionOrderResourceIds = null;
            } else {
                executionOrderResourceIds = new int[executionOrderResources.size()];
                for (int i = 0, executionOrderResourcesSize = executionOrderResources.size(); i < executionOrderResourcesSize; i++) {
                    Resource executionOrderResource = executionOrderResources.get(i);
                    executionOrderResourceIds[i] = executionOrderResource.getId();
                }
            }
            GroupOperationSchedule groupOperationSchedule = scheduleGroupOperation(subject,
                schedule.getGroup().getId(), executionOrderResourceIds, schedule.getHaltOnFailure(),
                schedule.getOperationName(), schedule.getParameters(), trigger, schedule.getDescription());
            return groupOperationSchedule.getId();
        } catch (SchedulerException e) {
            throw new ScheduleException(e);
        }
    }

    @Override
    public ResourceOperationSchedule scheduleResourceOperation(Subject subject, int resourceId, String operationName,
        Configuration parameters, Trigger trigger, String notes) throws SchedulerException {
        Resource resource = getResourceIfAuthorized(subject, resourceId);

        ensureControlPermission(subject, resource);

        OperationDefinition opDef = validateOperationNameAndParameters(resource.getResourceType(), operationName,
            parameters);

        String uniqueJobId = createUniqueJobName(resource, operationName);

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(OperationJob.DATAMAP_STRING_OPERATION_NAME, operationName);
        putDisplayName(jobDataMap, resource.getResourceType().getId(), operationName);

        if (parameters != null) {
            if (parameters.getId() == 0) {
                entityManager.persist(parameters);
            }

            jobDataMap.putAsString(OperationJob.DATAMAP_INT_PARAMETERS_ID, parameters.getId());
        }

        jobDataMap.putAsString(OperationJob.DATAMAP_INT_SUBJECT_ID, subject.getId());
        jobDataMap.putAsString(ResourceOperationJob.DATAMAP_INT_RESOURCE_ID, resource.getId());
        jobDataMap.put("description", notes);

        JobDetail jobDetail = new JobDetail();
        jobDetail.setName(uniqueJobId);
        String jobGroupName = createJobGroupName(resource);
        jobDetail.setGroup(jobGroupName);
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

        // We need to create our own schedule tracking entity.
        ResourceOperationScheduleEntity schedule;
        schedule = new ResourceOperationScheduleEntity(jobDetail.getName(), jobDetail.getGroup(),
            trigger.getStartTime(), resource);
        entityManager.persist(schedule);

        // Add the id of the entity bean, so we can easily map the Quartz job to the associated entity bean.
        jobDataMap.put(OperationJob.DATAMAP_INT_ENTITY_ID, String.valueOf(schedule.getId()));

        // Create an IN_PROGRESS item
        // - we need a copy of parameters to avoid constraint violations upon delete
        ResourceOperationHistory history;
        history = new ResourceOperationHistory(uniqueJobId, jobGroupName, subject.getName(), opDef,
            (parameters == null ? null : parameters.deepCopy(false)), schedule.getResource(), null);

        updateOperationHistory(subject, history);

        // Now actually schedule it.
        Date next = scheduler.scheduleJob(jobDetail, trigger);
        ResourceOperationSchedule newSchedule = getResourceOperationSchedule(subject, jobDetail);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Scheduled Resource operation [" + newSchedule + "] - next fire time is [" + next + "]");
        }

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

    @Override
    public GroupOperationSchedule scheduleGroupOperation(Subject subject, int compatibleGroupId,
        int[] executionOrderResourceIds, boolean haltOnFailure, String operationName, Configuration parameters,
        Trigger trigger, String notes) throws SchedulerException {
        ResourceGroup group = getCompatibleGroupIfAuthorized(subject, compatibleGroupId);

        ensureControlPermission(subject, group);

        validateOperationNameAndParameters(group.getResourceType(), operationName, parameters);

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(OperationJob.DATAMAP_STRING_OPERATION_NAME, operationName);
        putDisplayName(jobDataMap, group.getResourceType().getId(), operationName);

        if (parameters != null) {
            if (parameters.getId() == 0) {
                entityManager.persist(parameters);
            }

            jobDataMap.putAsString(OperationJob.DATAMAP_INT_PARAMETERS_ID, parameters.getId());
        }

        jobDataMap.putAsString(OperationJob.DATAMAP_INT_SUBJECT_ID, subject.getId());
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

        // Add the id of the entity bean, so we can easily map the Quartz job to the associated entity bean.
        jobDataMap.put(OperationJob.DATAMAP_INT_ENTITY_ID, String.valueOf(schedule.getId()));

        // now actually schedule it
        Date next = scheduler.scheduleJob(jobDetail, trigger);
        GroupOperationSchedule newSchedule = getGroupOperationSchedule(subject, jobDetail);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Scheduled group operation [" + newSchedule + "] - next fire time is [" + next + "]");
        }

        return newSchedule;
    }

    @Override
    public void unscheduleResourceOperation(Subject subject, String jobId, int resourceId) throws UnscheduleException {
        try {
            // checks for view permissions
            Resource resource = getResourceIfAuthorized(subject, resourceId);

            ensureControlPermission(subject, resource);

            // while unscheduling, be aware that the job could complete at any time
            ResourceOperationSchedule schedule;
            try {
                schedule = getResourceOperationSchedule(subject, jobId);
            } catch (SchedulerException e) {
                // The schedule must have completed, so ignore the request to unschedule it.
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Assuming job [" + jobId
                        + "] has completed, ignoring request to unschedule resource operation for resource ["
                        + resourceId + "]");
                }
                return;
            }

            if (schedule.getParameters() != null) {
                Integer configId = schedule.getParameters().getId();
                Configuration parameters = configurationManager.getConfigurationById(configId);
                if (null != parameters) {
                    entityManager.remove(parameters);
                }
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
    }

    @Override
    public void unscheduleGroupOperation(Subject subject, String jobId, int resourceGroupId) throws UnscheduleException {
        try {
            ResourceGroup group = resourceGroupManager.getResourceGroupById(subject, resourceGroupId,
                GroupCategory.COMPATIBLE);

            ensureControlPermission(subject, group);

            getCompatibleGroupIfAuthorized(subject, resourceGroupId); // just want to do this to check for permissions

            // while unscheduling, be aware that the job could complete at any time
            GroupOperationSchedule schedule;
            try {
                schedule = getGroupOperationSchedule(subject, jobId);
            } catch (SchedulerException e) {
                // The schedule must have completed, so ignore the request to unschedule it.
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Assuming job [" + jobId
                        + "] has completed, ignoring request to unschedule group operation for group ["
                        + resourceGroupId + "]");
                }
                return;
            }

            if (schedule.getParameters() != null) {
                Integer configId = schedule.getParameters().getId();
                Configuration parameters = configurationManager.getConfigurationById(configId);
                if (null != parameters) {
                    entityManager.remove(parameters);
                }
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
    }

    @Override
    public void deleteOperationScheduleEntity(ScheduleJobId jobId) {
        try {
            OperationScheduleEntity doomed = findOperationScheduleEntity(jobId);
            if (doomed != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Deleting schedule entity: " + jobId);
                }
                entityManager.remove(doomed);
            } else {
                LOG.info("Asked to delete unknown schedule - ignoring: " + jobId);
            }
        } catch (NoResultException nre) {
            LOG.info("Asked to delete unknown schedule - ignoring: " + jobId);
        }
    }

    @Override
    public void updateOperationScheduleEntity(ScheduleJobId jobId, long nextFireTime) {
        // sched will be managed - just setting the property is enough for it to be committed
        OperationScheduleEntity sched = findOperationScheduleEntity(jobId);
        sched.setNextFireTime(nextFireTime);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Scheduled job has a new next-fire-time: " + sched);
        }
    }

    @Override
    public List<ResourceOperationSchedule> findScheduledResourceOperations(Subject subject, int resourceId)
        throws Exception {
        Resource resource = getResourceIfAuthorized(subject, resourceId);

        List<ResourceOperationSchedule> operationSchedules = new ArrayList<ResourceOperationSchedule>();

        String groupName = createJobGroupName(resource);
        String[] jobNames = scheduler.getJobNames(groupName);

        for (String jobName : jobNames) {
            JobDetail jobDetail = scheduler.getJobDetail(jobName, groupName);
            ResourceOperationSchedule sched = getResourceOperationSchedule(subject, jobDetail);

            if (sched != null) {
                if (resourceId != sched.getResource().getId()) {
                    throw new IllegalStateException("Somehow a different resource [" + sched.getResource()
                        + "] was scheduled in the same job group as resource [" + resource + "]");
                }

                operationSchedules.add(sched);
            }
        }

        return operationSchedules;
    }

    @Override
    public List<GroupOperationSchedule> findScheduledGroupOperations(Subject subject, int groupId) throws Exception {
        ResourceGroup group = getCompatibleGroupIfAuthorized(subject, groupId);

        List<GroupOperationSchedule> operationSchedules = new ArrayList<GroupOperationSchedule>();

        String groupName = createJobGroupName(group);
        String[] jobNames = scheduler.getJobNames(groupName);

        for (String jobName : jobNames) {
            JobDetail jobDetail = scheduler.getJobDetail(jobName, groupName);
            GroupOperationSchedule sched = getGroupOperationSchedule(subject, jobDetail);

            if (sched != null) {
                if (groupId != sched.getGroup().getId()) {
                    throw new IllegalStateException("Somehow a different group [" + sched.getGroup()
                        + "] was scheduled in the same job group as group [" + group + "]");
                }

                operationSchedules.add(sched);
            }
        }

        return operationSchedules;
    }

    @Override
    public ResourceOperationSchedule getResourceOperationSchedule(Subject whoami, int scheduleId) {
        OperationScheduleEntity operationScheduleEntity = entityManager.find(OperationScheduleEntity.class, scheduleId);
        try {
            ResourceOperationSchedule resourceOperationSchedule = getResourceOperationSchedule(whoami,
                operationScheduleEntity.getJobId().toString());
            return resourceOperationSchedule;
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to retrieve ResourceOperationSchedule with id [" + scheduleId + "].", e);
        }
    }

    @Override
    public GroupOperationSchedule getGroupOperationSchedule(Subject whoami, int scheduleId) {
        OperationScheduleEntity operationScheduleEntity = entityManager.find(OperationScheduleEntity.class, scheduleId);
        try {
            GroupOperationSchedule groupOperationSchedule = getGroupOperationSchedule(whoami, operationScheduleEntity
                .getJobId().toString());
            return groupOperationSchedule;
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to retrieve GroupOperationSchedule with id [" + scheduleId + "].", e);
        }
    }

    @Override
    public ResourceOperationSchedule getResourceOperationSchedule(Subject whoami, JobDetail jobDetail) {
        // if the jobDetail is null assume the job is no longer scheduled
        if (null == jobDetail) {
            return null;
        }

        JobDataMap jobDataMap = jobDetail.getJobDataMap();

        String jobName = jobDetail.getName();
        String jobGroup = jobDetail.getGroup();
        String description = jobDataMap.getString("description");
        String operationName = jobDataMap.getString(OperationJob.DATAMAP_STRING_OPERATION_NAME);
        String displayName = jobDataMap.getString(OperationJob.DATAMAP_STRING_OPERATION_DISPLAY_NAME);
        int subjectId = jobDataMap.getIntFromString(OperationJob.DATAMAP_INT_SUBJECT_ID);
        Configuration parameters = null;

        if (jobDataMap.containsKey(OperationJob.DATAMAP_INT_PARAMETERS_ID)) {
            int configId = jobDataMap.getIntFromString(OperationJob.DATAMAP_INT_PARAMETERS_ID);
            parameters = entityManager.find(Configuration.class, configId);
        }

        int resourceId = jobDataMap.getIntFromString(ResourceOperationJob.DATAMAP_INT_RESOURCE_ID);
        Resource resource = getResourceIfAuthorized(whoami, resourceId);

        Integer entityId = getOperationScheduleEntityId(jobDetail);

        // note that we throw an exception if the subject does not exist!
        // this is by design to avoid a malicious user creating a dummy subject in the database,
        // scheduling a very bad operation, and deleting that subject thus removing all traces
        // of the user.  If the user has been removed from the system, all of that users schedules
        // will need to be deleted and rescheduled.

        ResourceOperationSchedule sched = new ResourceOperationSchedule();
        sched.setId(entityId);
        sched.setJobName(jobName);
        sched.setJobGroup(jobGroup);
        sched.setResource(resource);
        sched.setOperationName(operationName);
        sched.setOperationDisplayName(displayName);
        Subject subject = subjectManager.getSubjectById(subjectId);
        sched.setSubject(subject);
        sched.setParameters(parameters);
        sched.setDescription(description);

        Trigger trigger = getTriggerOfJob(jobDetail);
        if (trigger == null) {
            // The job must have run for the last time - return null to inform the user the job is defunct.
            return null;
        }

        JobTrigger jobTrigger = convertToJobTrigger(trigger);
        sched.setJobTrigger(jobTrigger);
        sched.setNextFireTime(trigger.getNextFireTime());

        return sched;
    }

    private int getOperationScheduleEntityId(JobDetail jobDetail) {
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        Object entityIdObj = jobDataMap.get(OperationJob.DATAMAP_INT_ENTITY_ID);
        int entityId;
        if (entityIdObj != null) {
            // for jobs created using RHQ 4.0 or later, the map will contain an entityId entry
            entityId = Integer.valueOf((String) entityIdObj);
        } else {
            // for jobs created prior to upgrading to RHQ 4.0, the map will not contain an entityId entry,
            // so we'll need to lookup the entity id from the DB.
            String jobName = jobDetail.getName();
            String jobGroup = jobDetail.getGroup();
            ScheduleJobId jobId = new ScheduleJobId(jobName, jobGroup);
            OperationScheduleEntity operationScheduleEntity = findOperationScheduleEntity(jobId);
            entityId = operationScheduleEntity.getId();
        }
        return entityId;
    }

    @Override
    public ResourceOperationSchedule getResourceOperationSchedule(Subject subject, String jobId)
        throws SchedulerException {

        JobId jobIdObject = new JobId(jobId);
        JobDetail jobDetail = scheduler.getJobDetail(jobIdObject.getJobName(), jobIdObject.getJobGroup());
        ResourceOperationSchedule resourceOperationSchedule = getResourceOperationSchedule(subject, jobDetail);
        if (resourceOperationSchedule == null) {
            throw new SchedulerException("The job with ID [" + jobId + "] is no longer scheduled.");
        }
        return resourceOperationSchedule;
    }

    @Override
    public GroupOperationSchedule getGroupOperationSchedule(Subject subject, JobDetail jobDetail) {
        // if the jobDetail is null assume the job is no longer scheduled
        if (null == jobDetail) {
            return null;
        }

        JobDataMap jobDataMap = jobDetail.getJobDataMap();

        String description = jobDetail.getDescription();
        String operationName = jobDataMap.getString(OperationJob.DATAMAP_STRING_OPERATION_NAME);
        String displayName = jobDataMap.getString(OperationJob.DATAMAP_STRING_OPERATION_DISPLAY_NAME);
        int subjectId = jobDataMap.getIntFromString(OperationJob.DATAMAP_INT_SUBJECT_ID);
        Configuration parameters = null;

        if (jobDataMap.containsKey(OperationJob.DATAMAP_INT_PARAMETERS_ID)) {
            int configId = jobDataMap.getIntFromString(OperationJob.DATAMAP_INT_PARAMETERS_ID);
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
                } else if (LOG.isDebugEnabled()) {
                    LOG.debug("Resource [" + resourceId
                        + "] looks like it was deleted and is no longer a member of group [" + group
                        + "] - ignoring it");
                }
            }
        }

        boolean haltOnFailure = jobDataMap.getBooleanValueFromString(GroupOperationJob.DATAMAP_BOOL_HALT_ON_FAILURE);

        Integer entityId = getOperationScheduleEntityId(jobDetail);

        GroupOperationSchedule sched = new GroupOperationSchedule();
        sched.setId(entityId);
        sched.setJobName(jobDetail.getName());
        sched.setJobGroup(jobDetail.getGroup());
        sched.setGroup(group);
        sched.setOperationName(operationName);
        sched.setOperationDisplayName(displayName);
        sched.setSubject(subjectManager.getSubjectById(subjectId));
        sched.setParameters(parameters);
        sched.setExecutionOrder(executionOrder);
        sched.setDescription(description);
        sched.setHaltOnFailure(haltOnFailure);

        Trigger trigger = getTriggerOfJob(jobDetail);
        if (trigger == null) {
            // The job must have run for the last time - return null to inform the user the job is defunct.
            return null;
        }

        JobTrigger jobTrigger = convertToJobTrigger(trigger);
        sched.setJobTrigger(jobTrigger);
        sched.setNextFireTime(trigger.getNextFireTime());

        return sched;
    }

    @Override
    public GroupOperationSchedule getGroupOperationSchedule(Subject subject, String jobId) throws SchedulerException {
        JobId jobIdObject = new JobId(jobId);
        JobDetail jobDetail = scheduler.getJobDetail(jobIdObject.getJobName(), jobIdObject.getJobGroup());
        GroupOperationSchedule groupOperationSchedule = getGroupOperationSchedule(subject, jobDetail);
        if (groupOperationSchedule == null) {
            throw new SchedulerException("The job with ID [" + jobId + "] is no longer scheduled.");
        }
        return groupOperationSchedule;
    }

    @Override
    public OperationHistory getOperationHistoryByHistoryId(Subject subject, int historyId) {
        OperationHistory history = entityManager.find(OperationHistory.class, historyId);

        if (history == null) {
            throw new IllegalArgumentException("Cannot get history - it does not exist: " + historyId);
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public OperationHistory updateOperationHistory(Subject subject, OperationHistory history) {
        /*
         * either the user wants to execute an operation on some resource or some group, or the OperationServerService
         * just got the results of the operation from the agent is needs to update this method.  thus, we only need to
         * ensure control permissions if the user isn't the overlord.
         */
        if (!authorizationManager.isOverlord(subject)) {
            ensureControlPermission(subject, history);
        }

        // The history item may have been created already, so find it in the database and
        // set the new state from our input
        boolean isNewHistory = (0 == history.getId());
        OperationRequestStatus originalStatus = null;
        if (!isNewHistory) {
            OperationHistory existingHistoryItem = entityManager.find(OperationHistory.class, history.getId());
            if (null == existingHistoryItem) {
                throw new IllegalArgumentException(
                    "Can not update operation history, history record not found. This call creates a new operation history record only if the supplied history argument has id set to 0. ["
                        + history + "]");
            }

            originalStatus = existingHistoryItem.getStatus();
            existingHistoryItem.setStatus(history.getStatus());
            existingHistoryItem.setErrorMessage(history.getErrorMessage());
            if (existingHistoryItem.getStartedTime() == 0) {
                existingHistoryItem.setStartedTime(history.getStartedTime());
            }
            if (history instanceof ResourceOperationHistory) {
                ((ResourceOperationHistory) existingHistoryItem).setResults(((ResourceOperationHistory) history)
                    .getResults());
            }
            history = existingHistoryItem;
        }

        // we do not cascade add the param config (we probably can add that but), so let's persist it now if needed
        Configuration parameters = history.getParameters();
        if ((parameters != null) && (parameters.getId() == 0)) {
            entityManager.persist(parameters);
            history.setParameters(parameters);
        }

        // to avoid TransientObjectExceptions during the merge, we need to attach all resource operation histories, if there are any
        if (history instanceof GroupOperationHistory) {
            GroupOperationHistory groupHistory = (GroupOperationHistory) history;
            List<ResourceOperationHistory> roh = groupHistory.getResourceOperationHistories();
            if (roh != null && roh.size() > 0) {
                List<ResourceOperationHistory> attached = new ArrayList<ResourceOperationHistory>(roh.size());
                for (ResourceOperationHistory unattachedHistory : roh) {
                    attached.add(entityManager.getReference(ResourceOperationHistory.class, unattachedHistory.getId()));
                }
                groupHistory.setResourceOperationHistories(attached);
            }
        }

        history = entityManager.merge(history); // merge will persist if it doesn't exist yet

        if (history.getParameters() != null) {
            history.getParameters().getId(); // eagerly reload the parameters
        }

        // we can even alert on In-Progress (an operation just being scheduled) so we need to check the
        // condition manager
        if (isNewHistory || originalStatus != history.getStatus()) {
            notifyAlertConditionCacheManager("updateOperationHistory", history);
        }

        // if this is not the initial create (i.e schedule-time of the operation) it means the
        // operation status has likely been updated.  Notify the storage node to see if it needs
        // to do anything in response to a storage node operation completion.  Don't pass an
        // attached entity to an Asynchronous SLSB method that runs in its own transaction. That
        // can cause locking with the current transaction.  Note we can't pass in just the id, because
        // the updates to the history are not yet committed and the async method needs to see the updated
        // history.
        if (!isNewHistory) {
            entityManager.flush();
            entityManager.detach(history);
            storageNodeOperationsHandler.handleOperationUpdateIfNecessary(history);
        }

        return history;
    }

    @Override
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
            agent = agentManager.getAgentClient(subjectManager.getOverlord(), resourceId);

            // since this method is usually called by the UI, we want to quickly determine if we can even talk to the agent
            if (agent.pingService(5000L)) {
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

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Agent already finished the operation so it cannot be canceled. " + "agent=[" + agent
                            + "], op=[" + doomedHistory + "]");
                    }
                    break;
                }

                case QUEUED: {
                    // If the agent says the interrupted state was QUEUED, this is good.  The agent never even
                    // got a chance to tell its plugin to execute the operation; it just dequeued and threw the op away.
                    // Therefore, we can really say it was canceled.
                    canceled = true;
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Cancel successful. Agent dequeued the operation and will not invoke it. "
                            + "agent=[" + agent + "], op=[" + doomedHistory + "]");
                    }
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
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Agent attempted to cancel the operation - it interrupted the operation while it was running. "
                            + "agent=[" + agent + "], op=[" + doomedHistory + "]");
                    }
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
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Agent does not know about the operation. Nothing to cancel. " + "agent=[" + agent
                            + "], op=[" + doomedHistory + "]");
                    }
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

    @Override
    @TransactionAttribute(NEVER)
    public void deleteOperationHistories(Subject subject, int[] historyIds, boolean deleteEvenIfInProgress) {
        List<String> errors = null;
        for (int id : historyIds) {
            try {
                operationManager.deleteOperationHistory(subject, id, deleteEvenIfInProgress);
            } catch (Exception e) {
                if (null == errors) {
                    errors = new ArrayList<String>();
                }
                errors.add("Could not delete operation history [" + id + "]: " + e.getMessage());
            }
        }
        if (null != errors) {
            throw new ApplicationException("Failed to delete [" + errors.size() + "] of [" + historyIds.length
                + "] operation history records:" + errors);
        }
    }

    @Override
    public void deleteOperationHistory(Subject subject, int historyId, boolean purgeInProgress) {
        OperationHistory doomedHistory = getOperationHistoryByHistoryId(subject, historyId); // this also checks authorization so we don't have to do it again

        ensureControlPermission(subject, doomedHistory);

        if ((doomedHistory.getStatus() == OperationRequestStatus.INPROGRESS) && !purgeInProgress) {
            throw new IllegalStateException(
                "The job is still in the in-progress state. Please wait for it to complete: " + doomedHistory.getId());
        }

        if (doomedHistory instanceof GroupOperationHistory) {
            List<ResourceOperationHistory> resourceHistories = ((GroupOperationHistory) doomedHistory)
                .getResourceOperationHistories();
            for (ResourceOperationHistory child : resourceHistories) {
                deleteOperationHistory(child.getId(), false);
            }
        }

        deleteOperationHistory(doomedHistory.getId(), false);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public int purgeOperationHistory(Date purgeBeforeTime) {
        int totalPurged = 0;
        int batchPurged;
        final int groupLimit = 1;
        final int resourceLimit = 50;
        long startTime = System.currentTimeMillis();

        // first, purge group operations (one at a time, it could be a large group)
        do {
            batchPurged = operationManager.purgeOperationHistoryInNewTransaction(purgeBeforeTime, true, groupLimit);
            totalPurged += batchPurged;

        } while (batchPurged > 0);

        // then, purge resource operations, we'll do 50 at a time since it is still pretty involved
        do {
            batchPurged = operationManager.purgeOperationHistoryInNewTransaction(purgeBeforeTime, false, resourceLimit);
            totalPurged += batchPurged;

        } while (batchPurged > 0);

        MeasurementMonitor.getMBean().incrementPurgeTime(System.currentTimeMillis() - startTime);
        MeasurementMonitor.getMBean().setPurgedEvents(totalPurged);

        return totalPurged;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int purgeOperationHistoryInNewTransaction(Date purgeBeforeTime, boolean isGroupPurge, int limit) {

        String entity = isGroupPurge ? "GroupOperationHistory" : "ResourceOperationHistory";
        String queryString = "SELECT h.id FROM " + entity + " h WHERE h.createdTime < :purgeBeforeTime";
        TypedQuery<Integer> query = entityManager.createQuery(queryString, Integer.class);
        query.setParameter("purgeBeforeTime", purgeBeforeTime.getTime());
        query.setMaxResults(limit);

        List<Integer> idList = query.getResultList();
        for (Integer id : idList) {
            if (isGroupPurge) {
                deleteOperationHistory(subjectManager.getOverlord(), id, true);
            } else {
                deleteOperationHistory(id, false);
            }
        }

        return idList.size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void deleteOperationHistory(int historyId, boolean detachChildHistories) {
        Query historyArgumentsQuery = entityManager
            .createNamedQuery(OperationHistory.QUERY_GET_PARAMETER_CONFIGURATION_IDS);
        Query historyResultsQuery = entityManager.createNamedQuery(OperationHistory.QUERY_GET_RESULT_CONFIGURATION_IDS);
        historyArgumentsQuery.setParameter("historyId", historyId);
        historyResultsQuery.setParameter("historyId", historyId);

        List<Integer> historyArgumentConfigurationIds = historyArgumentsQuery.getResultList();
        List<Integer> historyResultConfigurationIds = historyResultsQuery.getResultList();

        if (detachChildHistories) {
            Query detachChildHistoriesQuery = entityManager
                .createNamedQuery(ResourceOperationHistory.QUERY_DETACH_FROM_GROUP_HISTORY);
            detachChildHistoriesQuery.setParameter("historyId", historyId);
            detachChildHistoriesQuery.executeUpdate();
        }

        Query operationHistoryDeleteQuery = entityManager
            .createNamedQuery(OperationHistory.QUERY_DELETE_BY_HISTORY_IDS);
        operationHistoryDeleteQuery.setParameter("historyId", historyId);
        operationHistoryDeleteQuery.executeUpdate();

        List<Integer> allConfigurationIdsToDelete = new ArrayList<Integer>(historyArgumentConfigurationIds.size()
            + historyResultConfigurationIds.size());
        allConfigurationIdsToDelete.addAll(historyArgumentConfigurationIds);
        allConfigurationIdsToDelete.addAll(historyResultConfigurationIds);
        configurationManager.deleteConfigurations(allConfigurationIdsToDelete);
    }

    @Override
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

    @Override
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

    @Override
    @SuppressWarnings({ "unchecked" })
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

    @Override
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

    @Override
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

    @Override
    public boolean isResourceOperationSupported(Subject subject, int resourceId) {
        Resource resource;

        try {
            resource = getResourceIfAuthorized(subject, resourceId);
        } catch (PermissionException e) {
            // notice we caught this exception before propogating it up to the EJB layer, so
            // our transaction is not rolled back
            if (LOG.isDebugEnabled()) {
                LOG.debug("isOperationSupported: User cannot control resource: " + resourceId);
            }
            return false;
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("isOperationSupported: Resource does not exist: " + resourceId);
            }
            return false;
        }

        Set<OperationDefinition> defs = resource.getResourceType().getOperationDefinitions();

        return (defs != null) && (defs.size() > 0);
    }

    @Override
    public boolean isGroupOperationSupported(Subject subject, int resourceGroupId) {
        ResourceGroup group;

        try {
            group = resourceGroupManager.getResourceGroupById(subject, resourceGroupId, null);
            if (group.getGroupCategory() == GroupCategory.MIXED) {
                return false;
            }
        } catch (ResourceGroupNotFoundException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("isGroupOperationSupported: group does not exist: " + resourceGroupId);
            }
            return false;
        } catch (PermissionException pe) {
            // notice we caught this exception before propogating it up to the EJB layer, so
            // our transaction is not rolled back
            if (LOG.isDebugEnabled()) {
                LOG.debug("isGroupOperationSupported: User cannot view (and thus) control group: " + resourceGroupId);
            }
            return false;
        }

        if (!authorizationManager.hasGroupPermission(subject, Permission.CONTROL, group.getId())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("isGroupOperationSupported: User cannot control group: " + group);
            }
            return false;
        }

        Set<OperationDefinition> defs = group.getResourceType().getOperationDefinitions();

        return (defs != null) && (defs.size() > 0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void checkForTimedOutOperations(Subject subject) {
        LOG.debug("Begin scanning for timed out operation histories");

        if (!authorizationManager.isOverlord(subject)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unauthorized user " + subject + " tried to execute checkForTimedOutOperations: "
                    + "only the overlord may execute this system operation");
            }
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

                // there are two cases we need to check. First, did the resource history start but it took too long? (aka timed out?)
                // Or, second, did the resource history not even start yet, but its been a very long time (1 day) since it was created?
                // In either case, we can't wait forever, so we'll mark them as failed with an appropriate error message.
                long duration = history.getDuration();
                long createdTime = history.getCreatedTime();
                boolean timedOut = (duration > timeout);
                boolean neverStarted = ((System.currentTimeMillis() - createdTime) > (1000 * 60 * 60 * 24));
                if (timedOut || neverStarted) {
                    if (timedOut) {
                        // the operation started, but the agent never told us how it finished prior to exceeding the timeout
                        LOG.info("Operation execution seems to have been orphaned - timing it out: " + history);
                        history.setErrorMessage("Timed out : did not complete after " + duration + " ms"
                            + " (the timeout period was " + timeout + " ms)");
                    } else {
                        // the operation never even started, but its been at least a day since it was created
                        LOG.info("Operation execution seems to have never started - timing it out: " + history);
                        history.setErrorMessage("Failed to start : operation never started");
                    }
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
            }
        } catch (Throwable t) {
            LOG.warn("Failed to check for memberless group operations. Cause: " + t);
        }

        LOG.debug("Finished scanning for timed out operation histories");
    }

    @Override
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

    @Override
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

    @Override
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
                ResourceOperationSchedule sched = getResourceOperationSchedule(subject, composite.getJobId().toString());
                OperationDefinition def = getSupportedResourceOperation(overlord, composite.getResourceId(),
                    sched.getOperationName(), false);
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

    @Override
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
                GroupOperationSchedule sched = getGroupOperationSchedule(subject, composite.getJobId().toString());
                OperationDefinition def = getSupportedGroupOperation(overlord, composite.getGroupId(),
                    sched.getOperationName(), false);
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
    @Override
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
                            "The following resources failed to invoke the operation (see group operation history for details) :\n ");
                    } else {
                        groupErrorMessage.append("\n ");
                    }

                    Resource resource = resourceHistory.getResource();
                    String name = resource.getName();
                    Map<Integer, String> ancestryMap = resourceManager
                        .getResourcesAncestry(subjectManager.getOverlord(), new Integer[] { resource.getId() },
                            ResourceAncestryFormat.SIMPLE);
                    String ancestry = ancestryMap.isEmpty() ? "" : ancestryMap.get(resource.getId());
                    groupErrorMessage.append(name + " [" + ancestry + "] " + resourceHistory.getStatus().name());
                }
            }

            if (!stillInProgress) {
                groupHistory.setErrorMessage((groupErrorMessage == null) ? null : groupErrorMessage.toString());
                groupHistory.setStatus(groupStatus);
                notifyAlertConditionCacheManager("checkForCompletedGroupOperation", groupHistory);
            }
        }
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

    @Override
    @Nullable
    public ResourceOperationHistory getLatestCompletedResourceOperation(Subject subject, int resourceId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Getting latest completed operation for resource [" + resourceId + "]");
        }

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

    @Override
    @Nullable
    public ResourceOperationHistory getOldestInProgressResourceOperation(Subject subject, int resourceId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Getting oldest in-progress operation for resource [" + resourceId + "]");
        }

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

    @Override
    public OperationDefinition getOperationDefinition(Subject subject, int operationId) {
        OperationDefinition operationDefinition = entityManager.find(OperationDefinition.class, operationId);

        if (operationDefinition == null) {
            throw new OperationDefinitionNotFoundException("Cannot get operation definition - it does not exist: "
                + operationId);
        }

        return operationDefinition;
    }

    @Override
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

    // Only call this method if the operation history status has changed
    private void notifyAlertConditionCacheManager(String callingMethod, OperationHistory operationHistory) {
        AlertConditionCacheStats stats = alertConditionCacheManager.checkConditions(operationHistory);
        if (LOG.isDebugEnabled()) {
            LOG.debug(callingMethod + ": " + stats.toString());
        }
    }

    /**
     * Returns a managed entity of the scheduled job.
     *
     * @param  jobId
     *
     * @return a managed entity, attached to this bean's entity manager
     */
    private OperationScheduleEntity findOperationScheduleEntity(ScheduleJobId jobId) {
        Query query = entityManager.createNamedQuery(OperationScheduleEntity.QUERY_FIND_BY_JOB_ID);
        String jobName = jobId.getJobName();
        query.setParameter("jobName", jobName);
        String jobGroup = jobId.getJobGroup();
        query.setParameter("jobGroup", jobGroup);
        OperationScheduleEntity operationScheduleEntity = (OperationScheduleEntity) query.getSingleResult();
        return operationScheduleEntity;
    }

    @Override
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

    @Override
    public GroupOperationSchedule scheduleGroupOperationUsingCron(Subject subject, int groupId,
        int[] executionOrderResourceIds, boolean haltOnFailure, String operationName, Configuration parameters,
        String cronExpression, int timeout, String description) throws ScheduleException {

        // if the user set a timeout, add it to our configuration
        if (timeout > 0L) {
            if (parameters == null) {
                parameters = new Configuration();
            }
            parameters.put(new PropertySimple(OperationDefinition.TIMEOUT_PARAM_NAME, timeout));
        }
        CronTrigger cronTrigger = new CronTrigger();
        try {
            cronTrigger.setCronExpression(cronExpression);
            return scheduleGroupOperation(subject, groupId, executionOrderResourceIds, haltOnFailure, operationName,
                parameters, cronTrigger, description);
        } catch (Exception e) {
            throw new ScheduleException(e);
        }
    }

    @Override
    public PageList<OperationDefinition> findOperationDefinitionsByCriteria(Subject subject,
        OperationDefinitionCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);

        CriteriaQueryRunner<OperationDefinition> queryRunner = new CriteriaQueryRunner<OperationDefinition>(criteria,
            generator, entityManager);
        return queryRunner.execute();
    }

    @Override
    public PageList<ResourceOperationHistory> findResourceOperationHistoriesByCriteria(Subject subject,
        ResourceOperationHistoryCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        if (!authorizationManager.isInventoryManager(subject)) {
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE,
                subject.getId());
        }

        CriteriaQueryRunner<ResourceOperationHistory> queryRunner = new CriteriaQueryRunner<ResourceOperationHistory>(
            criteria, generator, entityManager);
        return queryRunner.execute();
    }

    @Override
    public PageList<GroupOperationHistory> findGroupOperationHistoriesByCriteria(Subject subject,
        GroupOperationHistoryCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        if (!authorizationManager.isInventoryManager(subject)) {
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.GROUP,
                subject.getId());
        }

        CriteriaQueryRunner<GroupOperationHistory> queryRunner = new CriteriaQueryRunner<GroupOperationHistory>(
            criteria, generator, entityManager);
        return queryRunner.execute();
    }

    @Nullable
    private Trigger getTriggerOfJob(JobDetail jobDetail) {
        Trigger[] triggers;
        try {
            triggers = scheduler.getTriggersOfJob(jobDetail.getName(), jobDetail.getGroup());
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to lookup trigger for job [" + jobDetail.getFullName() + "].", e);
        }
        if (triggers.length > 1) {
            throw new IllegalStateException("Job [" + jobDetail.getFullName() + "] has more than one trigger: "
                + Arrays.asList(triggers));
        }
        if (triggers.length == 0) {
            return null;
        }
        return triggers[0];
    }

    private JobTrigger convertToJobTrigger(Trigger trigger) {
        JobTrigger schedule;
        if (trigger instanceof SimpleTrigger) {
            SimpleTrigger simpleTrigger = (SimpleTrigger) trigger;
            Date startTime = simpleTrigger.getStartTime();
            if (startTime != null) {
                // later
                int repeatCount = simpleTrigger.getRepeatCount();
                if (repeatCount == 0) {
                    // non-recurring
                    schedule = JobTrigger.createLaterTrigger(startTime);
                } else {
                    // recurring
                    long repeatInterval = simpleTrigger.getRepeatInterval();
                    if (repeatCount == SimpleTrigger.REPEAT_INDEFINITELY) {
                        Date endTime = simpleTrigger.getEndTime();
                        if (endTime != null) {
                            schedule = JobTrigger.createLaterAndRepeatTrigger(startTime, repeatInterval, endTime);
                        } else {
                            schedule = JobTrigger.createLaterAndRepeatTrigger(startTime, repeatInterval);
                        }
                    } else {
                        schedule = JobTrigger.createLaterAndRepeatTrigger(startTime, repeatInterval, repeatCount);
                    }
                }
            } else {
                // now
                int repeatCount = simpleTrigger.getRepeatCount();
                if (repeatCount == 0) {
                    // non-recurring
                    schedule = JobTrigger.createNowTrigger();
                } else {
                    // recurring
                    long repeatInterval = simpleTrigger.getRepeatInterval();
                    if (repeatCount == SimpleTrigger.REPEAT_INDEFINITELY) {
                        Date endTime = simpleTrigger.getEndTime();
                        if (endTime != null) {
                            schedule = JobTrigger.createNowAndRepeatTrigger(repeatInterval, endTime);
                        } else {
                            schedule = JobTrigger.createNowAndRepeatTrigger(repeatInterval);
                        }
                    } else {
                        schedule = JobTrigger.createNowAndRepeatTrigger(repeatInterval, repeatCount);
                    }
                }
            }
        } else if (trigger instanceof CronTrigger) {
            CronTrigger cronTrigger = (CronTrigger) trigger;
            schedule = JobTrigger.createCronTrigger(cronTrigger.getCronExpression());
        } else {
            throw new IllegalStateException("Unsupported Quartz trigger type: " + trigger.getClass().getName());
        }
        return schedule;
    }

    private Trigger convertToTrigger(JobTrigger jobTrigger) {
        Trigger trigger;
        if (jobTrigger.getRecurrenceType() == JobTrigger.RecurrenceType.CRON_EXPRESSION) {
            CronTrigger cronTrigger = new CronTrigger();
            try {
                cronTrigger.setCronExpression(jobTrigger.getCronExpression());
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            trigger = cronTrigger;
        } else {
            SimpleTrigger simpleTrigger = new SimpleTrigger();
            Date startTime = null;
            switch (jobTrigger.getStartType()) {
            case NOW:
                startTime = new Date();
                break;
            case DATETIME:
                startTime = jobTrigger.getStartDate();
                break;
            }
            simpleTrigger.setStartTime(startTime);
            if (jobTrigger.getRecurrenceType() == JobTrigger.RecurrenceType.REPEAT_INTERVAL) {
                simpleTrigger.setRepeatInterval(jobTrigger.getRepeatInterval());
                if (jobTrigger.getEndType() == JobTrigger.EndType.REPEAT_COUNT) {
                    simpleTrigger.setRepeatCount(jobTrigger.getRepeatCount());
                } else {
                    simpleTrigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
                    if (jobTrigger.getEndType() == JobTrigger.EndType.DATETIME) {
                        simpleTrigger.setEndTime(jobTrigger.getEndDate());
                    }
                }
            }

            trigger = simpleTrigger;
        }

        return trigger;
    }

    private OperationDefinition validateOperationNameAndParameters(ResourceType resourceType, String operationName,
        Configuration parameters) {
        Set<OperationDefinition> operationDefinitions = resourceType.getOperationDefinitions();
        OperationDefinition matchingOperationDefinition = null;
        for (OperationDefinition operationDefinition : operationDefinitions) {
            if (operationDefinition.getName().equals(operationName)) {
                matchingOperationDefinition = operationDefinition;
                break;
            }
        }
        if (matchingOperationDefinition == null) {
            throw new IllegalArgumentException("[" + operationName
                + "] is not a valid operation name for Resources of type [" + resourceType.getName() + "].");
        }
        ConfigurationDefinition parametersDefinition = matchingOperationDefinition
            .getParametersConfigurationDefinition();
        List<String> errors = ConfigurationUtility.validateConfiguration(parameters, parametersDefinition);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Parameters for [" + operationName + "] on Resource of type ["
                + resourceType.getName() + "] are not valid: " + errors);
        }

        return matchingOperationDefinition;
    }

}
