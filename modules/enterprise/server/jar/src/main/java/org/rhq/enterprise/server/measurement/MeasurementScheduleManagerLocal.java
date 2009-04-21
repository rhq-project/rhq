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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.measurement.composite.MeasurementScheduleComposite;
import org.rhq.core.domain.resource.Resource;
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
public interface MeasurementScheduleManagerLocal {
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
    Set<ResourceMeasurementScheduleRequest> getSchedulesForResourceAndItsDescendants(Set<Integer> resourceIds,
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
     * Return a list of MeasurementSchedules for the given ids
     *
     * @param  ids PrimaryKeys of the schedules searched
     *
     * @return a list of Schedules
     */
    List<MeasurementSchedule> getSchedulesByIds(Collection<Integer> ids);

    /**
     * Return a list of MeasurementSchedules for the given definition ids and resource id.
     *
     * @param  definitionIds
     * @param  resourceId
     *
     * @return a list of Schedules
     */
    List<MeasurementSchedule> getSchedulesByDefinitionIdsAndResourceId(int[] definitionIds, int resourceId);

    /**
     * Obtain a MeasurementSchedule by its Id after a check for a valid session
     *
     * @param  subject    a session id that must be valid
     * @param  scheduleId The primary key of the Schedule
     *
     * @return a MeasurementSchedule or null if the session or the scheduleId are invalid
     */
    MeasurementSchedule getMeasurementScheduleById(Subject subject, int scheduleId);

    /**
     * Reattach a Schedule to a PersitenceContext after a successful check for a valid session
     *
     * @param  subject  A session id that must be valid
     * @param  schedule A MeasurementSchedule to persist.
     *
     * @return The updated MeasurementSchedule
     */
    MeasurementSchedule updateMeasurementSchedule(Subject subject, MeasurementSchedule schedule);

    /**
     * Find MeasurementSchedules that are attached to a certain definition and some resources
     *
     * @param  subject      A session id that must be valid
     * @param  definitionId The primary key of a MeasurementDefinition
     * @param  resources    a List of Resources
     *
     * @return a List of MeasurementSchedules
     */
    List<MeasurementSchedule> getMeasurementSchedulesByDefinitionIdAndResources(Subject subject, int definitionId,
        List<Resource> resources);

    /**
     * Find MeasurementSchedules that are attached to a certain definition and some resources
     *
     * @param  subject      A session id that must be valid
     * @param  definitionId The primary key of a MeasurementDefinition
     * @param  resourceIds  primary of Resources wanted
     *
     * @return a List of MeasurementSchedules
     */
    List<MeasurementSchedule> getMeasurementSchedulesByDefinitionIdAndResourceIds(Subject subject, int definitionId,
        Integer[] resourceIds);

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
    MeasurementSchedule getMeasurementSchedule(Subject subject, int definitionId, int resourceId, boolean attachBaseline)
        throws MeasurementNotFoundException;

    /**
     * Retrieves the default metric collection schedules for the given resource type.
     *
     * @param  subject        the current user
     * @param  resourceTypeId a {@link org.rhq.core.domain.resource.ResourceType} id
     * @param  pageControl    the page control for the results
     *
     * @return the default metric collection schedules for the given resource type
     */
    PageList<MeasurementScheduleComposite> getDefaultMeasurementSchedulesForResourceType(Subject subject,
        int resourceTypeId, PageControl pageControl);

    /**
     * Retrieves the metric collection schedules for the given resource.
     *
     * @param  subject     the current user
     * @param  resourceId  a {@link Resource} id
     * @param  dataType    the data type to limit results to, or null to not limit results to a particular data type
     * @param  pageControl the page control for the results
     *
     * @return the metric collection schedules for the given resource
     */
    PageList<MeasurementScheduleComposite> getMeasurementScheduleCompositesForResource(Subject subject, int resourceId,
        @Nullable DataType dataType, PageControl pageControl);

    /**
     * Retrieves the metric collection schedules for the given resource.
     *
     * @param  subject     the current user
     * @param  resourceId  a {@link Resource} id
     * @param  dataType    the data type to limit results to, or null to not limit results to a particular data type
     * @param  dataType    the display type to limit results to, or null to not limit results to a particular display type
     * @param  enable      limit the results by enabled state, or null to not limit results by enabled state
     * @param  pageControl the page control for the results
     *
     * @return the metric collection schedules for the given resource
     */
    PageList<MeasurementSchedule> getMeasurementSchedulesForResource(Subject subject, int resourceId,
        @Nullable DataType dataType, @Nullable DisplayType displayType, @Nullable Boolean enabled,
        PageControl pageControl);

    /**
     * Disables all collection schedules in the given measurement definition IDs. This only disables the "templates", it
     * does not disable actual schedules. For that capability, see
     * {@link #disableMeasurementSchedules(Subject, int[], int)}.
     *
     * @param subject
     * @param measurementDefinitionIds
     * @param updateSchedules TODO
     */
    void disableDefaultCollectionForMeasurementDefinitions(Subject subject, int[] measurementDefinitionIds,
        boolean updateSchedules);

    /**
     * Disables all collection schedules attached to the given resource whose schedules are based off the given
     * definitions. This does not disable the "templates" (aka definitions).
     *
     * @param subject
     * @param measurementDefinitionIds
     * @param resourceId
     */
    void disableMeasurementSchedules(Subject subject, int[] measurementDefinitionIds, int resourceId);

    /**
     * Disables all collection schedules for all measurement definitions. This only disables the "templates", it does
     * not disable actual schedules. For that capability, see {@link #disableAllMeasurementSchedules(Subject)}.
     *
     * <p>This is a highly disruptive method - it turns off monitoring for future resources. The user making this call
     * must have global inventory and setting permissions to execute this.</p>
     *
     * @param subject user that must have global inventory and setting rights
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
     */
    void disableAllMeasurementSchedules(Subject subject);

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
    void updateDefaultCollectionIntervalForMeasurementDefinitions(Subject subject, int[] measurementDefinitionIds,
        long collectionInterval, boolean updateExistingSchedules);

    /**
     * Enables all collection schedules attached to the given resource whose schedules are based off the given
     * definitions. This does not enable the "templates" (aka definitions).
     *
     * @param subject
     * @param measurementDefinitionIds
     * @param resourceId
     * @param collectionInterval
     */
    void updateMeasurementSchedules(Subject subject, int[] measurementDefinitionIds, int resourceId,
        long collectionInterval);

    /**
     * Enables all collection schedules attached to the given compatible group whose schedules are based off the given
     * definitions. This does not enable the "templates" (aka definitions). If the passed group is not compatible or
     * does not exist an Exception is thrown.
     *
     * @param subject                  Subject of the caller
     * @param measurementDefinitionIds the definitions on which the schedules to update are based
     * @param groupId                  ID of the group
     * @param collectionInterval       the new interval
     */
    void updateMeasurementSchedulesForCompatGroup(Subject subject, int[] measurementDefinitionIds, int groupId,
        long collectionInterval);

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
    void updateMeasurementSchedulesForAutoGroup(Subject subject, int[] measurementDefinitionIds, int parentResourceId,
        int childResourceType, long collectionInterval);

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
    List<MeasurementSchedule> getMeasurementSchedulesForResourceAndType(Subject subject, int resourceId,
        DataType dataType, DisplayType displayType, boolean enabledOnly);

    /**
     * @return a rounded count of the average number of metrics that are scheduled per minute
     */
    int getScheduledMeasurementsPerMinute();

    /**
     * Disable the measurement schedules for the passed definitions for the resources of the passed compatible group.
     */
    public void disableMeasurementSchedulesForCompatGroup(Subject subject, int[] measurementDefinitionIds, int groupId);

    /**
     * Disable the measurement schedules for the passed definitions of the rsource ot the passed auto group.
     *
     * @param subject
     * @param measurementDefinitionIds
     * @param parentResourceId
     * @param childResourceType
     */
    public void disableMeasurementSchedulesForAutoGroup(Subject subject, int[] measurementDefinitionIds,
        int parentResourceId, int childResourceType);

    public PageList<MeasurementScheduleComposite> getMeasurementSchedulesForCompatGroup(Subject subject, int groupId,
        PageControl pageControl);

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
        int childType, PageControl pageControl);

    /**
     * Create {@link MeasurementSchedule}s for existing resources hanging on newType.
     * @param type The {@link ResourceType} for which we want to add schedules
     * @param newDefinition The {@link MeasurementDefinition} where we derive the schedules from
     */
    public void createSchedulesForExistingResources(ResourceType type, MeasurementDefinition newDefinition);

    public int insertSchedulesFor(List<Integer> batchIds) throws Exception;

    public int returnSchedulesFor(List<Integer> batchIds, Set<ResourceMeasurementScheduleRequest> allSchedules)
        throws Exception;
}