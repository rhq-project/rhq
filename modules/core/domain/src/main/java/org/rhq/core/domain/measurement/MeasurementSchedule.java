/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.domain.measurement;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.rhq.core.domain.resource.Resource;

@Entity
@NamedQueries( {
    @NamedQuery(name = MeasurementSchedule.FIND_ALL_FOR_DEFINITIONS, query = "SELECT ms FROM MeasurementSchedule ms WHERE ms.definition IN (:definitions) "),
    /** Find all Schedules for a given Definition */
    @NamedQuery(name = "MeasurementSchedule.findForDefinition", query = "SELECT ms FROM MeasurementSchedule ms WHERE ms.definition = :definition"),
    @NamedQuery(name = "MeasurementSchedule.findByCategory", query = "SELECT ms FROM MeasurementSchedule ms WHERE ms.definition.category = :cat"),
    @NamedQuery(name = "MeasurementSchedule.findByDefinitionAndEnablement", query = "SELECT ms FROM MeasurementSchedule ms WHERE ms.definition = :def and ms.enabled = :enabled"),
    @NamedQuery(name = "MeasurementSchedule.enableSchedules", query = "UPDATE MeasurementSchedule ms SET ms.enabled = :enabled WHERE ms IN(:scheds)"),
    @NamedQuery(name = "MeasurementSchedule.findByIds", query = "SELECT ms FROM MeasurementSchedule ms WHERE ms.id IN ( :ids )"),
    @NamedQuery(name = MeasurementSchedule.FIND_BY_DEFINITION_ID_AND_RESOURCE_IDS, query = "SELECT ms FROM MeasurementSchedule ms WHERE ms.definition.id = :definitionId AND (ms.resource.id IN (:resourceIds))"),
    @NamedQuery(name = MeasurementSchedule.FIND_BY_DEFINITION_ID_AND_RESOURCE_ID, query = "SELECT ms FROM MeasurementSchedule ms WHERE ms.definition.id = :definitionId AND ms.resource.id = :resourceId"),
    @NamedQuery(name = MeasurementSchedule.FIND_ENABLED_BY_RESOURCES_AND_RESOURCE_TYPE, query = "SELECT ms.id, res.id, def.id "
        + "  FROM MeasurementSchedule ms "
        + "  JOIN ms.definition def "
        + "  JOIN ms.resource res "
        + " WHERE def.resourceType = :resourceType "
        + "   AND ms.definition = def "
        + "   AND res IN (:resources) "
        + "   AND ms.enabled = true" + "   AND (def.dataType = :dataType OR :dataType is null )"),
    @NamedQuery(name = MeasurementSchedule.FIND_ENABLED_BY_RESOURCE_IDS_AND_RESOURCE_TYPE_ID, query = "SELECT ms.id, res.id, def.id "
        + "  FROM MeasurementSchedule ms "
        + "  JOIN ms.definition def "
        + "  JOIN ms.resource res "
        + " WHERE def.resourceType.id = :resourceTypeId "
        + "   AND ms.definition = def "
        + "   AND res.id IN (:resourceIds) " + "   AND ms.enabled = true"),
    @NamedQuery(name = "MeasurementSchedule.findByEnablementForResource", query = "SELECT ms FROM MeasurementSchedule ms WHERE ms.enabled = :enabled AND ms.resource = :resource "),
    @NamedQuery(name = MeasurementSchedule.FIND_SCHEDULES_WITH_BASLINES_TO_CALC, query = "SELECT ms FROM MeasurementSchedule ms "
        + "WHERE ms.enabled = true AND ms.definition.numericType = :measType "
        + "AND ms.baseline.computeTime < :ctime AND ms.baseline.userEntered = false"),
    @NamedQuery(name = MeasurementSchedule.FIND_ALL_FOR_RESOURCE_ID, query = "" //
        + "    SELECT ms " //
        + "      FROM MeasurementSchedule ms " //
        + "JOIN FETCH ms.definition " //
        + "     WHERE ms.resource.id = :resourceId " //
        + "       AND (ms.definition.dataType = :dataType OR :dataType is null) "
        + "       AND (ms.definition.displayType = :displayType OR :displayType is null) "
        + "       AND (ms.enabled = :enabled OR :enabled is null) "),
    @NamedQuery(name = MeasurementSchedule.FIND_BY_DEFINITION_IDS_AND_RESOURCE_ID, query = "SELECT ms FROM MeasurementSchedule ms WHERE ms.definition.id IN (:definitionIds) AND ms.resource.id = :resourceId"),
    @NamedQuery(name = MeasurementSchedule.FIND_SCHEDULE_COMPOSITE_FOR_RESOURCE, query = "SELECT new org.rhq.core.domain.measurement.composite.MeasurementScheduleComposite(ms.definition, ms.enabled, ms.interval) "
        + "FROM MeasurementSchedule ms WHERE ms.resource.id = :resourceId "
        + "AND (ms.definition.dataType = :dataType OR :dataType is null) "),
    @NamedQuery(name = MeasurementSchedule.GET_SCHEDULED_MEASUREMENTS_PER_MINUTED, query = "SELECT SUM(1000.0 / ms.interval) * 60.0 FROM MeasurementSchedule ms WHERE ms.enabled = true"),
    @NamedQuery(name = MeasurementSchedule.DISABLE_ALL, query = "UPDATE MeasurementSchedule ms SET ms.enabled = false"),
    @NamedQuery(name = MeasurementSchedule.DELETE_BY_RESOURCES, query = "DELETE MeasurementSchedule ms WHERE ms.resource IN ( :resources )") })
@SequenceGenerator(name = "RHQ_METRIC_SCHED_ID_SEQ", sequenceName = "RHQ_MEASUREMENT_SCHED_ID_SEQ")
@Table(name = "RHQ_MEASUREMENT_SCHED", uniqueConstraints = { @UniqueConstraint(columnNames = { "DEFINITION",
    "RESOURCE_ID" }) })
public class MeasurementSchedule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Finds schedules that have a baseline attached that needs to be recalculated. MeasuremetType is passed in
     * :measType, baseline compute time in :ctime
     */
    public static final String FIND_SCHEDULES_WITH_BASLINES_TO_CALC = "MeasurementSchedule.findSchedulesWithBaselinesToCalculate";

    /**
     * Find all MeasurementSchedules that are attached to the resource passed by its id in :resourceId
     */
    public static final String FIND_ALL_FOR_RESOURCE_ID = "MeasurementSchedule.FIND_ALL_FOR_RESOURCE_ID";

    /**
     * Find schedules by definition and resources. Definition is in :definitionId, resource ids are in :resourceIds
     */
    public static final String FIND_BY_DEFINITION_ID_AND_RESOURCE_IDS = "MeasurementSchedule.findByDefinitionIdAndResourceIds";

    /**
     * Find schedules by definition and resource. Definition is in :definitionId, resource id is in :resourceId
     */
    public static final String FIND_BY_DEFINITION_ID_AND_RESOURCE_ID = "MeasurementSchedule.findByDefinitionIdAndResourceId";

    /**
     * Find schedules by definitions and resource. Definitions are in :definitionIds, resource id is in :resourceId
     */
    public static final String FIND_BY_DEFINITION_IDS_AND_RESOURCE_ID = "MeasurementSchedule.findByDefinitionIdsAndResourceId";

    /**
     * Find MeasureScheduleComposites for a resource. Resource id is passed in :resourceId
     */
    public static final String FIND_SCHEDULE_COMPOSITE_FOR_RESOURCE = "MeasurementDefinition.findScheduleCompositeForResource";

    public static final String GET_SCHEDULED_MEASUREMENTS_PER_MINUTED = "MeasurementSchedule.getScheduledMeasurementsPerMinute";
    public static final String DISABLE_ALL = "MeasurementSchedule.disableAll";
    public static final String FIND_ENABLED_BY_RESOURCES_AND_RESOURCE_TYPE = "MeasurementSchedule.FIND_ENABLED_BY_ResourcesS_AND_RESOURCE_TYPE";
    public static final String FIND_ENABLED_BY_RESOURCE_IDS_AND_RESOURCE_TYPE_ID = "MeasurementSchedule.FIND_ENABLED_BY_ResourceIds_AND_RESOURCE_TYPE";
    public static final String DELETE_BY_RESOURCES = "MeasurementSchedule.deleteByResources";
    public static final String FIND_ALL_FOR_DEFINITIONS = "MeasurementSchedule.FIND_ALL_FOR_DEFINITIONS";

    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RHQ_METRIC_SCHED_ID_SEQ")
    @Id
    private int id;

    /**
     * A possible baseline for this MeasurementSchedule Since baseline will always initially be null for a while, we
     * don't need CascadeType.PERSIST. However, we do need CascadeType.MERGE because the schedule will get its baseline
     * calculated for the first time (a merge-persist cascade), or have it updated at some later point in time (a
     * merge-merge cascade).
     */
    // TODO should this be @OneToMany or OneToOne
    // Remove now performed by bulk delete
    @OneToOne(mappedBy = "schedule", cascade = { CascadeType.MERGE }, fetch = FetchType.LAZY)
    private MeasurementBaseline baseline;

    /**
     * The base definition of this Metric
     */
    @JoinColumn(name = "DEFINITION")
    @ManyToOne(fetch = FetchType.EAGER)
    private MeasurementDefinition definition;

    /**
     * Metrics can be gathered on all levels of physical resources (Platform, Server, Service)
     */
    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Resource resource;

    /**
     * The interval of collecting. Only applicable if gatherMode() is set to MeasurementGathering.NUMERIC
     */
    @Column(name = "COLL_INTERVAL")
    private long interval;

    /**
     * Time the schedule itself was last modified
     */
    @Column(name = "MTIME")
    private Long mtime;

    /**
     * Is this metric schedule enabled
     */
    @Column(name = "ENABLED")
    private boolean enabled;

    public MeasurementSchedule() {
    }

    public long getInterval() {
        return interval;
    }

    /**
     * Construct a new MeasurementSchedule
     *
     * @param definition The definition to apply
     * @param resource   The resource this schedule is for
     */
    public MeasurementSchedule(MeasurementDefinition definition, Resource resource) {
        this.definition = definition;
        this.enabled = true;
        this.resource = resource;
    }

    @Override
    public String toString() {
        return "[MeasurementSchedule, id=" + id + "]";
    }

    /**
     * @return the baseline
     */
    public MeasurementBaseline getBaseline() {
        return baseline;
    }

    /**
     * @param baseline the baseline to set
     */
    public void setBaseline(MeasurementBaseline baseline) {
        this.baseline = baseline;
    }

    /**
     * @return if the schedule is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return the mtime
     */
    public Long getMtime() {
        return mtime;
    }

    /**
     * Let the persistence framework set this for us.
     */
    @PrePersist
    @PreUpdate
    @SuppressWarnings( { "unused" })
    private void setMtime() {
        this.mtime = System.currentTimeMillis();
    }

    /**
     * @return the myDefinition
     */
    public MeasurementDefinition getDefinition() {
        return definition;
    }

    /**
     * @param myDefinition the myDefinition to set
     */
    public void setDefinition(MeasurementDefinition myDefinition) {
        this.definition = myDefinition;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((definition == null) ? 0 : definition.hashCode());
        result = (prime * result) + ((resource == null) ? 0 : resource.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof MeasurementSchedule)) {
            return false;
        }

        final MeasurementSchedule other = (MeasurementSchedule) obj;
        if (definition == null) {
            if (other.definition != null) {
                return false;
            }
        } else if (!definition.equals(other.definition)) {
            return false;
        }

        if (resource == null) {
            if (other.resource != null) {
                return false;
            }
        } else if (!resource.equals(other.resource)) {
            return false;
        }

        return true;
    }
}