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

import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.measurement.composite.MeasurementScheduleComposite;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.agentclient.AgentClient;

/**
 * A manager for {@link MeasurementSchedule}s.
 *
 * @author Heiko W. Rupp
 */
@Local
public interface MeasurementScheduleManagerLocal extends MeasurementScheduleManagerRemote {
    /**
     * Given a resource ID, this will return all schedule collections for all of the resource's measurements, including
     * all measurements for the resource's children. This will also create schedules for resources if they do not
     * already exist.
     *
     * @param  resourceIds    IDs of the resources whose measurements are to be returned
     * @param  getDescendents if true, descendents will be loaded as well
     *
     * @return the set of resource schedule requests for a subtree or a single resource
     */
    Set<ResourceMeasurementScheduleRequest> findSchedulesForResourceAndItsDescendants(int[] resourceIds,
        boolean getDescendents);

    /**
     * Get the {@link AgentClient} (the connection to the agent) that is associated with the given schedule.
     *
     * @param  sched a {@link MeasurementSchedule} for which we need a connection to the Agent
     *
     * @return an {@link AgentClient} that can be used to communicate with the Agent
     */
    AgentClient getAgentClientForSchedule(MeasurementSchedule sched);

    /**
     * Return a list of MeasurementSchedules for the given definition ids and resource id.
     *
     * @param  definitionIds
     * @param  resourceId
     *
     * @return a list of Schedules
     */
    List<MeasurementSchedule> findSchedulesByResourceIdAndDefinitionIds(Subject subject, int resourceId,
        int[] definitionIds);

    /**
     * Obtain a MeasurementSchedule by its Id after a check for a valid session
     *
     * @param  subject    a session id that must be valid
     * @param  scheduleId The primary key of the Schedule
     *
     * @return a MeasurementSchedule or null if the session or the scheduleId are invalid
     */
    MeasurementSchedule getScheduleById(Subject subject, int scheduleId);

    /**
     * Find MeasurementSchedules that are attached to a certain definition and a resource
     *
     * @param  subject
     * @param  definitionId   The primary key of a MeasurementDefinition
     * @param  resourceId     the id of the resource
     * @param  attachBaseline TODO
     *
     * @return the MeasurementSchedule of the given definition for the given resource
     */
    MeasurementSchedule getSchedule(Subject subject, int resourceId, int definitionId, boolean attachBaseline)
        throws MeasurementNotFoundException;

    /**
     * Disables all collection schedules in the given measurement definition IDs. This only disables the "templates", it
     * does not disable actual schedules. For that capability, see
     * {@link #disableSchedules(Subject, int[], int)}.
     *
     * @param subject
     * @param measurementDefinitionIds
     * @param updateSchedules TODO
     * 
     * @Deprecated portal-war
     */
    void disableDefaultCollectionForMeasurementDefinitions(Subject subject, int[] measurementDefinitionIds,
        boolean updateSchedules);

    /**
     * Disables all collection schedules for all measurement definitions. This only disables the "templates", it does
     * not disable actual schedules. For that capability, see {@link #disableAllMeasurementSchedules(Subject)}.
     *
     * <p>This is a highly disruptive method - it turns off monitoring for future resources. The user making this call
     * must have global inventory and setting permissions to execute this.</p>
     *
     * @param subject user that must have global inventory and setting rights
     * 
     * @deprecated portal-war
     */
    void disableAllDefaultCollections(Subject subject);

    /**
     * Disables all collection schedules attached to all resources. This only disables the currently existing schedules,
     * it does not disable the templates for future resources. For that capability, see
     * {@link #disableAllDefaultCollections(Subject)}.
     *
     * <p>This is a highly disruptive method - it turns off monitoring for existing resources. The user making this call
     * must have global inventory and setting permissions to execute this.</p>
     *
     * @param subject user that must have global inventory and setting rights
     * 
     * @deprecated portal-war
     */
    void disableAllSchedules(Subject subject);

    /**
     * (Re-)Enables all collection schedules in the given measurement definition IDs and sets their collection
     * intervals. This only enables the "templates", it does not enable actual schedules unless updateExistingSchedules
     * is set to true.
     *
     * @param subject                  a valid subject that has the {@link Permission#MANAGE_SETTINGS MANAGE_SETTINGS}
     *                                 global permission
     * @param measurementDefinitionIds the primary keys for the definitions
     * @param collectionInterval       if > 0, enable the metric with this value as the the new collection
     *                                 interval, in milliseconds; if == 0, enable the metric with its current
     *                                 collection interval; if < 0, disable the metric; if >0, the value
     *                                 should also be >=30000, since 30s is the minimum allowed interval; if
     *                                 it is not, 30000 will be used instead of the specified interval
     * @param updateExistingSchedules  if true, then existing schedules for this definition will also be updated.
     * 
     * @deprecated portal-war
     */
    void updateDefaultCollectionIntervalForMeasurementDefinitions(Subject subject, int[] measurementDefinitionIds,
        long collectionInterval, boolean updateExistingSchedules);

    /**
     * Using this method one can both update the default collection interval AND enable or disable the 
     * measurement definitions. This method is therefore preferable if you need to do both these things
     * in 1 go.
     * 
     * @param subject the current user
     * @param measurementDefinitionIds the ids of measurement definitions to update
     * @param collectionInterval the default collection interval to set
     * @param enable whether to enable or disable the measurement definition
     * @param updateExistingSchedules whether to accordingly update the existing schedules
     */
    void updateDefaultCollectionIntervalAndEnablementForMeasurementDefinitions(Subject subject,
        int[] measurementDefinitionIds, long collectionInterval, boolean enable, boolean updateExistingSchedules);

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
     */
    void updateSchedulesForAutoGroup(Subject subject, int parentResourceId, int childResourceType,
        int[] measurementDefinitionIds, long collectionInterval);

    /**
     * Determine the Schedules for a Resource and DataType. The data type is used to filter out (numerical) measurement
     * and / or traits. If it is null, then we don't filter by DataType
     *
     * @param  subject     Subject of the caller
     * @param  resourceId  PK of the resource we're interested in
     * @param  dataType    DataType of the desired results use null for no filtering
     * @param  displayType the display type desired or null for no filtering
     * @param  enabledOnly should we restrict the query to certain enablement state? null means "don't care".
     *
     * @return List of MeasuremenSchedules for the given resource
     */
    List<MeasurementSchedule> findSchedulesForResourceAndType(Subject subject, int resourceId, DataType dataType,
        DisplayType displayType, boolean enabledOnly);

    /**
     * @return a rounded count of the average number of metrics that are scheduled per minute
     */
    int getScheduledMeasurementsPerMinute();

    /**
     * Disable the measurement schedules for the passed definitions of the rsource ot the passed auto group.
     *
     * @param subject
     * @param measurementDefinitionIds
     * @param parentResourceId
     * @param childResourceType
     * 
     * @deprecated portal-war
     */
    void disableSchedulesForAutoGroup(Subject subject, int parentResourceId, int childResourceType,
        int[] measurementDefinitionIds);

    /**
     * Enable the measurement schedules for the passed definitions of the resource ot the passed auto group.
     *
     * @param subject
     * @param measurementDefinitionIds
     * @param parentResourceId
     * @param childResourceType
     * 
     * @deprecated portal-war
     */
    void enableSchedulesForAutoGroup(Subject subject, int parentResourceId, int childResourceType,
        int[] measurementDefinitionIds);

    /**
     * Create {@link MeasurementSchedule}s for existing resources hanging on newType.
     * @param type The {@link ResourceType} for which we want to add schedules
     * @param newDefinition The {@link MeasurementDefinition} where we derive the schedules from
     */
    void createSchedulesForExistingResources(ResourceType type, MeasurementDefinition newDefinition);

    int insertSchedulesFor(int[] batchIds) throws Exception;

    int returnSchedulesFor(int[] batchIds, Set<ResourceMeasurementScheduleRequest> allSchedules) throws Exception;

    /**
     * This method should be called when it is determined that the data in the measurement schedule table might be
     * corrupt. This happens when the schedules get a collection interval of less than 30 seconds. Execution of this
     * method will automatically correct that situation, and update the mtime's of the corresponding resources whose
     * schedules were corrupt, to cause the agent to synchronize those schedules.
     */
    void errorCorrectSchedules();

    /**
     * Return a list of MeasurementSchedules for the given ids
     *
     * @param  ids PrimaryKeys of the schedules searched
     *
     * @return a list of Schedules
     */
    List<MeasurementSchedule> findSchedulesByIds(int[] ids);

    /**
     * Find MeasurementSchedules that are attached to a certain definition and some resources
     *
     * @param  subject      A session id that must be valid
     * @param  definitionId The primary key of a MeasurementDefinition
     * @param  resourceIds  primary of Resources wanted
     *
     * @return a List of MeasurementSchedules
     */
    List<MeasurementSchedule> findSchedulesByResourceIdsAndDefinitionId(Subject subject, int[] resourceIds,
        int definitionId);

    /**
     * Return a list of MeasurementSchedules for the given definition ids and resource ids. Note that this method does
     * not take a Subject argument. Security checks are the responsibility of the caller.
     *
     * @param resourceIds The ids of the resource for which schedules are being fetched
     * @param definitionIds The ids of the the measurement definitions
     * @return A list of MeasurementSchedules
     */
    List<MeasurementSchedule> findSchedulesByResourceIdsAndDefinitionIds(int[] resourceIds, int[] definitionIds);

    PageList<MeasurementScheduleComposite> getMeasurementScheduleCompositesByContext(Subject subject,
        EntityContext context, PageControl pc);

    int updateSchedulesForContext(Subject subject, EntityContext context, int[] measurementDefinitionIds,
        long collectionInterval);

    int enableSchedulesForContext(Subject subject, EntityContext context, int[] measurementDefinitionIds);

    int disableSchedulesForContext(Subject subject, EntityContext context, int[] measurementDefinitionIds);

    /**
     * Notifies all agents of measurement schedule changes.
     * @param entityContext the context.
     * @param scheduleSubQuery the subquery indicating which schedules changed
     */
    void notifyAgentsOfScheduleUpdates(EntityContext entityContext, String scheduleSubQuery);

    /**
     * @return The smallest and largest schedule ids. The first element in the array contains the smallest and the
     * second element contains the largest. This method may return null if no schedules exist.
     */
    int[] getMinAndMaxScheduleIds();
}