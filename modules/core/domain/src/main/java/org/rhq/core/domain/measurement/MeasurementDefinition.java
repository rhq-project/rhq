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
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.util.StringUtil;

//@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@Entity
@NamedQueries({
    @NamedQuery(name = MeasurementDefinition.FIND_BY_RESOURCE_TYPE_DATA_TYPE_DISPLAY_TYPE, query = "" //
        + "  SELECT md " //
        + "    FROM MeasurementDefinition md " //
        + "   WHERE md.resourceType.id = :resourceTypeId " //
        + "     AND ( md.dataType = :dataType OR :dataType is null ) " //
        + "     AND ( md.displayType = :displayType OR :displayType is null ) " //
        + "ORDER BY md.displayName"), //
    @NamedQuery(name = MeasurementDefinition.FIND_SCHEDULE_COMPOSITE_FOR_RESOURCE_TYPE, query = "" //
        + "SELECT new org.rhq.core.domain.measurement.composite.MeasurementScheduleComposite(md, md.defaultOn, md.defaultInterval) "
        + "  FROM MeasurementDefinition md " //
        + " WHERE md.resourceType.id = :resourceTypeId"), //
    @NamedQuery(name = MeasurementDefinition.FIND_BY_IDS, query = "" //
        + "SELECT md " //
        + "  FROM MeasurementDefinition md " //
        + " WHERE md.id IN ( :ids )"), //
    @NamedQuery(name = MeasurementDefinition.DISABLE_ALL, query = "" //
        + "UPDATE MeasurementDefinition md " //
        + "   SET md.defaultOn = false"),
    @NamedQuery(name = MeasurementDefinition.FIND_RAW_OR_PER_MINUTE_BY_NAME_AND_RESOURCE_TYPE_NAME, query = "" //
        + " SELECT md FROM MeasurementDefinition md"
        + " WHERE md.name = :name "
        + " AND md.resourceType.name = :resourceTypeName"
        + " AND md.resourceType.plugin = :resourceTypePlugin"
        + " AND ((:perMinute = 1 AND md.rawNumericType IS NOT NULL) OR (:perMinute = 0 AND md.rawNumericType IS NULL))") })
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_MEASUREMENT_DEF_ID_SEQ", sequenceName = "RHQ_MEASUREMENT_DEF_ID_SEQ")
@Table(name = "RHQ_MEASUREMENT_DEF")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class MeasurementDefinition implements Serializable {

    public static final String QUERY_NATIVE_UPDATE_DEFAULT_ON_BY_IDS = "" //
        + "UPDATE RHQ_MEASUREMENT_DEF" //
        + "   SET DEFAULT_ON = ?" //
        + "   WHERE ID IN ( @@DEFINITION_IDS@@ )";
    public static final String QUERY_NATIVE_UPDATE_DEFAULTS_BY_IDS = "" //
        + "UPDATE RHQ_MEASUREMENT_DEF" //
        + "   SET DEFAULT_ON = ?, DEFAULT_INTERVAL = ?" //
        + "   WHERE ID IN ( @@DEFINITION_IDS@@ )";
    public static final String QUERY_NATIVE_UPDATE_SCHEDULES_ENABLE_BY_IDS = "" //
        + "UPDATE RHQ_MEASUREMENT_SCHED" //
        + "   SET ENABLED = ?" //
        + "   WHERE DEFINITION IN ( @@DEFINITION_IDS@@ )";
    public static final String QUERY_NATIVE_UPDATE_SCHEDULES_BY_IDS = "" //
        + "UPDATE RHQ_MEASUREMENT_SCHED" //
        + "   SET ENABLED = ?, COLL_INTERVAL = ?" //
        + "   WHERE DEFINITION IN ( @@DEFINITION_IDS@@ )";

    private static final long serialVersionUID = 1L;

    public static final String FIND_BY_RESOURCE_TYPE_DATA_TYPE_DISPLAY_TYPE = "MeasurementDefinition.findByResourceTypeDataTypeDisplayType";
    public static final String FIND_SCHEDULE_COMPOSITE_FOR_RESOURCE_TYPE = "MeasurementDefinition.findScheduleCompositeForResourceType";
    public static final String FIND_BY_IDS = "MeasurementDefinition.findByIds";
    public static final String DISABLE_ALL = "MeasurementDefinition.disableAll";
    public static final String FIND_RAW_OR_PER_MINUTE_BY_NAME_AND_RESOURCE_TYPE_NAME = "MeasurementDefinition.findRawOrPerMinuteByNameAndResourceTypeName";

    public static final String AVAILABILITY_NAME = "rhq.availability";
    public static final Long AVAILABILITY_DEFAULT_PERIOD_SERVER = 60L * 1000; // 1 minute in ms
    public static final Long AVAILABILITY_DEFAULT_PERIOD_SERVICE = 60L * 1000 * 10; // 10 minutes in ms
    public static final String AVAILABILITY_DESCRIPTION = "The number of seconds between availability checks. The agent honors this setting as best as possible but the actual period can be longer based on agent activity.";
    public static final String AVAILABILITY_DISPLAY_NAME = "Availability";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_MEASUREMENT_DEF_ID_SEQ")
    @Id
    private int id;

    @JoinColumn(name = "RESOURCE_TYPE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private ResourceType resourceType;

    /**
     * Name of this definition
     */
    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "DISPLAY_NAME")
    private String displayName;

    /**
     * Type of this Metric (Availability, Throughput, ...)
     */
    @Enumerated(EnumType.ORDINAL)
    private MeasurementCategory category;

    /**
     * Concrete schedules for this metric
     */
    @OneToMany(mappedBy = "definition")
    List<MeasurementSchedule> schedules = new ArrayList<MeasurementSchedule>();

    /**
     * Measurement unit in which this metric is taken
     */
    @Enumerated(EnumType.ORDINAL)
    private MeasurementUnits units;

    /**
     * How are measurement values going to trend (monotonically increasing, ... )
     */
    @Column(name = "NUMERIC_TYPE")
    @Enumerated(EnumType.ORDINAL)
    private NumericType numericType;

    /**
     * The kind of measurement data being collected
     */
    @Column(name = "DATA_TYPE")
    @Enumerated(EnumType.ORDINAL)
    private DataType dataType;

    /**
     * How are values going to be displayed
     */
    @Column(name = "DISPLAY_TYPE")
    @Enumerated(EnumType.ORDINAL)
    private DisplayType displayType;

    /**
     * Is this metric schedule enabled by default
     */
    @Column(name = "DEFAULT_ON")
    private boolean defaultOn;

    /**
     * What is the default gathering interval
     */
    @Column(name = "DEFAULT_INTERVAL")
    private long defaultInterval;

    @Column(name = "DESCRIPTION")
    private String description;

    /**
     * If this is a per-minute numeric metric, how the corresponding raw metric trends (up or down), or, if this is
     * not a per-minute numeric metric, null.
     */
    @Column(name = "RAW_NUMERIC_TYPE")
    @Enumerated(EnumType.ORDINAL)
    private NumericType rawNumericType;

    @OneToMany(mappedBy = "measurementDefinition", cascade = CascadeType.REMOVE)
    private List<AlertCondition> alertConditions = new ArrayList<AlertCondition>();

    /**
     * When displaying items on screen - in what order should that be? This is filled in from the metadata from the
     * plugin descriptor. A value of 1000 means "don't care".
     */
    @Column(name = "DISPLAY_ORDER")
    private int displayOrder = 1000;

    /**
     * The destination type (e.g. "URL" or "Method Name") - only defined for call-time measurements.
     */
    @Column(name = "DESTINATION_TYPE")
    private String destinationType;

    /**
     * Version for optimistic locking. Don't ever set this yourself
     */
    @SuppressWarnings({ "unused" })
    @Version
    private int version;

    /* no-arg constructor required by EJB spec - not for use by subclasses */
    protected MeasurementDefinition() {
        /* for JPA use only */
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        displayName = StringUtil.trim(displayName, 100);
        description = StringUtil.trim(description, 500);
    }

    /**
     * Creates a definition whose {@link #getDataType() data type} is {@link DataType#MEASUREMENT}. This does not take a
     * resourceType because this is a dependent child type entity of ResourceType, and this object's ResourceType is set
     * when adding it to the parent ResourceType.
     *
     * @param name            The name of this definition. Must be unique
     * @param category        What kind of Metric is this (avail., throughput, ..)
     * @param units           The MetricUnit of this type of Metric
     * @param numericType     Is this dynamic or trendsup/down data
     * @param defaultOn       Shall Schedules created with this definition be enabled by default?
     * @param defaultInterval The default interval in ms between two Measurements
     */
    @Deprecated
    public MeasurementDefinition(String name, MeasurementCategory category, MeasurementUnits units,
        NumericType numericType, boolean defaultOn, long defaultInterval, DisplayType displayType) {
        this(name, category, units, DataType.MEASUREMENT, defaultOn, defaultInterval, displayType);
        this.numericType = numericType;
    }

    public MeasurementDefinition(String name, MeasurementCategory category, MeasurementUnits units, DataType dataType,
        NumericType numericType, boolean defaultOn, long defaultInterval, DisplayType displayType) {
        this(name, category, units, dataType, defaultOn, defaultInterval, displayType);
        this.numericType = numericType;
    }

    /**
     * Creates a definition with the given data type. If the data type is {@link DataType#MEASUREMENT}, consider using
     * the other constructor that takes the {@link NumericType} instead.
     *
     * @param name            The name of this definition. Must be unique
     * @param category        What kind of Metric is this (avail., throughput, ..)
     * @param units           The MetricUnit of this type of Metric
     * @param dataType        what kind of data will this be (measurement? trait?)
     * @param defaultOn       Shall Schedules created with this definition be enabled by default?
     * @param defaultInterval The default interval in ms between two Measurements
     *
     * @see   MeasurementDefinition#MeasurementDefinition(String, MeasurementCategory, MeasurementUnits, NumericType,
     *        boolean, long, DisplayType)
     */
    public MeasurementDefinition(String name, MeasurementCategory category, MeasurementUnits units, DataType dataType,
        boolean defaultOn, long defaultInterval, DisplayType displayType) {
        this.name = name;
        this.category = category;
        this.units = units;
        this.dataType = dataType;
        this.defaultOn = defaultOn;
        this.defaultInterval = defaultInterval;
        this.displayType = displayType;

        if (this.dataType == null) {
            this.dataType = DataType.MEASUREMENT;
        }

        if (DataType.MEASUREMENT.equals(this.dataType)) {
            this.numericType = NumericType.DYNAMIC;
        }
    }

    public MeasurementDefinition(MeasurementDefinition def) {
        this.name = def.name;
        this.displayName = def.displayName;
        this.description = def.description;
        this.category = def.category;
        this.units = def.units;
        this.dataType = def.dataType;
        this.defaultOn = def.defaultOn;
        this.defaultInterval = def.defaultInterval;
        this.displayType = def.displayType;
        this.numericType = def.numericType;
        this.destinationType = def.destinationType;
    }

    public MeasurementDefinition(ResourceType resourceType, @NotNull String name) {
        this.resourceType = resourceType;
        this.name = name;
    }

    /**
     * Helper to add a new Schedule to this definition.
     *
     * @param  ms a Metric Schedule
     *
     * @return Complete List of all schedules including the new one
     */
    public List<MeasurementSchedule> addSchedule(MeasurementSchedule ms) {
        schedules.add(ms);
        return schedules;
    }

    @Override
    public String toString() {
        return "[MeasurementDefinition,id=" + id + ",name=" + name + "]";
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public MeasurementCategory getCategory() {
        return category;
    }

    public void setCategory(MeasurementCategory category) {
        this.category = category;
    }

    public NumericType getNumericType() {
        return numericType;
    }

    public void setNumericType(NumericType type) {
        numericType = type;
    }

    public void setMeasurementType(NumericType numericType) {
        this.numericType = numericType;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public DisplayType getDisplayType() {
        return displayType;
    }

    public void setDisplayType(DisplayType displayType) {
        this.displayType = displayType;
    }

    public long getDefaultInterval() {
        return defaultInterval;
    }

    public void setDefaultInterval(long defaultInterval) {
        this.defaultInterval = defaultInterval;
    }

    public boolean isDefaultOn() {
        return defaultOn;
    }

    public void setDefaultOn(boolean defaultOn) {
        this.defaultOn = defaultOn;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<MeasurementSchedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<MeasurementSchedule> schedules) {
        this.schedules = schedules;
    }

    public MeasurementUnits getUnits() {
        return units;
    }

    public void setUnits(MeasurementUnits units) {
        this.units = units;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isPerMinute() {
        return rawNumericType != null;
    }

    public NumericType getRawNumericType() {
        return rawNumericType;
    }

    public void setRawNumericType(NumericType rawNumericType) {
        this.rawNumericType = rawNumericType;
    }

    /**
     * This method is intended to update an existing measurement definition object with the contents of another object.
     * It is intended for updates to existing connected objects from disconnected and separately loaded objects. The
     * name, its referenced resource type and other referenced objects are not changed in this object as they can not be
     * updated or are part of the identity of this object.
     *
     * @param newDefinition      the definition holding the new contents
     * @param alsoUpdateInterval should the default interval also be updated?
     */
    public void update(MeasurementDefinition newDefinition, boolean alsoUpdateInterval) {
        displayName = newDefinition.getDisplayName();
        category = newDefinition.getCategory();
        units = newDefinition.getUnits();
        numericType = newDefinition.getNumericType();
        dataType = newDefinition.getDataType();
        displayType = newDefinition.getDisplayType();
        defaultOn = newDefinition.isDefaultOn();
        rawNumericType = newDefinition.getRawNumericType();
        if (alsoUpdateInterval) {
            defaultInterval = newDefinition.getDefaultInterval();
        }

        description = newDefinition.getDescription();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (!(o instanceof MeasurementDefinition))) {
            return false;
        }

        MeasurementDefinition that = (MeasurementDefinition) o;

        if (!name.equals(that.name)) {
            return false;
        }

        if ((resourceType != null) ? (!resourceType.equals(that.resourceType)) : (that.resourceType != null)) {
            return false;
        }

        if (rawNumericType != that.rawNumericType) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = ((resourceType != null) ? resourceType.hashCode() : 0);
        result = (31 * result) + ((rawNumericType != null) ? rawNumericType.hashCode() : 0);
        result = (31 * result) + name.hashCode();
        return result;
    }

    /**
     * When displaying items on screen - in what order should that be? This is filled in from the metadata from the
     * plugin descriptor. A value of -1 means "don't care".
     *
     * @return the displayOrder
     */
    public int getDisplayOrder() {
        return displayOrder;
    }

    /**
     * When displaying items on screen - in what order should that be? This is filled in from the metadata from the
     * plugin descriptor. A value of -1 means "don't care".
     *
     * @param i the displayOrder to set
     */
    public void setDisplayOrder(int i) {
        this.displayOrder = i;
    }

    @Nullable
    public String getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(@Nullable String destinationType) {
        this.destinationType = destinationType;
    }

    public List<AlertCondition> getAlertCondition() {
        return alertConditions;
    }

    public void setAlertCondition(List<AlertCondition> alertConditions) {
        this.alertConditions = alertConditions;
    }
}