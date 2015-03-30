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
package org.rhq.core.domain.alert;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

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
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.operation.OperationRequestStatus;

/**
 * An alert condition (e.g. ActiveThreads > 100) as configured in an alert definition.
 * 
 * @author Joseph Marques
 */
@Entity
@NamedQueries({
    @NamedQuery(name = "AlertCondition.findByTriggerId", query = "SELECT a FROM AlertCondition AS a WHERE a.triggerId = :tid"),
    @NamedQuery(name = "AlertCondition.findAll", query = "SELECT a FROM AlertCondition AS a"),
    @NamedQuery(name = AlertCondition.QUERY_DELETE_BY_RESOURCES, query = "DELETE FROM AlertCondition ac WHERE ac.alertDefinition IN ( SELECT ad FROM AlertDefinition ad WHERE ad.resource.id IN ( :resourceIds ) )"),
    @NamedQuery(name = AlertCondition.QUERY_BY_CATEGORY_BASELINE, query = "" //
        + "  SELECT new org.rhq.core.domain.alert.composite.AlertConditionBaselineCategoryComposite " //
        + "       ( " //
        + "         ac, " //
        + "         ms.id, " //
        + "         mb.id, " //
        + "         mb.baselineMin, " //
        + "         mb.baselineMean, " //
        + "         mb.baselineMax, " //
        + "         md.dataType " //
        + "       ) " //
        + "    FROM AlertCondition AS ac " //
        + "    JOIN ac.alertDefinition ad " //
        + "    JOIN ad.resource res " //
        + "    JOIN ac.measurementDefinition md, MeasurementSchedule ms JOIN ms.baseline mb " //
        + "   WHERE " + AlertCondition.RECOVERY_CONDITIONAL_EXPRESSION //
        + "     AND ( res.agent.id = :agentId OR :agentId IS NULL ) " //
        + "     AND ad.enabled = TRUE " //
        + "     AND ad.deleted = FALSE " //
        + "     AND ms.definition = md " //
        + "     AND ms.resource = res " //
        + "     AND mb IS NOT NULL " //
        + "     AND ac.category = 'BASELINE' " //
        + "ORDER BY ac.id"), //
    @NamedQuery(name = AlertCondition.QUERY_BY_CATEGORY_COUNT_BASELINE, query = "" //
        + "  SELECT count(ac.id) " //
        + "    FROM AlertCondition AS ac " //
        + "    JOIN ac.alertDefinition ad " //
        + "    JOIN ad.resource res " //
        + "    JOIN ac.measurementDefinition md, MeasurementSchedule ms JOIN ms.baseline mb " //
        + "   WHERE " + AlertCondition.RECOVERY_CONDITIONAL_EXPRESSION //
        + "     AND ( res.agent.id = :agentId OR :agentId IS NULL ) " //
        + "     AND ad.enabled = TRUE " //
        + "     AND ad.deleted = FALSE " //
        + "     AND ms.definition = md " //
        + "     AND ms.resource = res " //
        + "     AND mb IS NOT NULL " //
        + "     AND ac.category = 'BASELINE' "), // 
    @NamedQuery(name = AlertCondition.QUERY_BY_CATEGORY_CHANGE, query = "" //
        + "  SELECT new org.rhq.core.domain.alert.composite.AlertConditionChangesCategoryComposite " //
        + "       ( " //
        + "         ac, " //
        + "         ms.id, " //
        + "         md.dataType " //
        + "       ) " //
        + "    FROM AlertCondition AS ac " //
        + "    JOIN ac.alertDefinition ad " //
        + "    JOIN ad.resource res " //
        + "    JOIN ac.measurementDefinition md, MeasurementSchedule ms " //
        + "   WHERE " + AlertCondition.RECOVERY_CONDITIONAL_EXPRESSION //
        + "     AND ( res.agent.id = :agentId OR :agentId IS NULL ) " //
        + "     AND ad.enabled = TRUE " //
        + "     AND ad.deleted = FALSE " //
        + "     AND ms.definition = md " //
        + "     AND ms.resource = res " //
        + "     AND ac.category = 'CHANGE' " //
        + "ORDER BY ac.id"), //
    @NamedQuery(name = AlertCondition.QUERY_BY_CATEGORY_TRAIT, query = "" //
        + "  SELECT new org.rhq.core.domain.alert.composite.AlertConditionTraitCategoryComposite " //
        + "       ( " //
        + "         ac, " //
        + "         ms.id, " //
        + "         (" //
        + "           SELECT md.value " //
        + "             FROM MeasurementDataTrait md " //
        + "            WHERE md.schedule = ms " //
        + "              AND md.id.timestamp = " //
        + "                ( " //
        + "                  SELECT max(imd.id.timestamp) " //
        + "                    FROM MeasurementDataTrait imd " //
        + "                   WHERE ms.id = imd.schedule.id " //
        + "                ) " //
        + "         ) " //
        + "       ) " //
        + "    FROM AlertCondition AS ac " //
        + "    JOIN ac.alertDefinition ad " //
        + "    JOIN ad.resource res " //
        + "    JOIN ac.measurementDefinition md, MeasurementSchedule ms " //
        + "   WHERE " + AlertCondition.RECOVERY_CONDITIONAL_EXPRESSION //
        + "     AND ( res.agent.id = :agentId OR :agentId IS NULL ) " //
        + "     AND ad.enabled = TRUE " //
        + "     AND ad.deleted = FALSE " //
        + "     AND ms.definition = md " //
        + "     AND ms.resource = res " //
        + "     AND ac.category = 'TRAIT' " //
        + "ORDER BY ac.id"), //
    @NamedQuery(name = AlertCondition.QUERY_BY_CATEGORY_AVAILABILITY, query = "" //
        + "  SELECT new org.rhq.core.domain.alert.composite.AlertConditionAvailabilityCategoryComposite " //
        + "       ( " //
        + "         ac, " //
        + "         ad.id, " // needed for avail duration
        + "         res.id, " //
        + "         (" //
        + "           SELECT max(a.availabilityType) " //
        + "             FROM Availability a " //
        + "             JOIN a.resource ar " //
        + "            WHERE ar = res " //
        + "              AND a.endTime IS NULL " //
        + "         ) " //
        + "       ) " //
        + "    FROM AlertCondition AS ac " //
        + "    JOIN ac.alertDefinition ad " //
        + "    JOIN ad.resource res " //
        + "   WHERE " + AlertCondition.RECOVERY_CONDITIONAL_EXPRESSION //
        + "     AND ( res.agent.id = :agentId OR :agentId IS NULL ) " //
        + "     AND ad.enabled = TRUE " //
        + "     AND ad.deleted = FALSE " //
        + "     AND ac.category = :category " //
        + "ORDER BY ac.id"), //
    @NamedQuery(name = AlertCondition.QUERY_BY_CATEGORY_CONTROL, query = "" //
        + "  SELECT new org.rhq.core.domain.alert.composite.AlertConditionControlCategoryComposite " //
        + "       ( " //
        + "         ac, " //
        + "         res.id, " //
        + "         (" //
        + "           SELECT op.id " //
        + "             FROM OperationDefinition op " //
        + "            WHERE op.resourceType = type " //
        + "              AND op.name = ac.name " //
        + "         ) " //
        + "       ) " //
        + "    FROM AlertCondition AS ac " //
        + "    JOIN ac.alertDefinition ad " //
        + "    JOIN ad.resource res " //
        + "    JOIN res.resourceType type " //
        + "   WHERE " + AlertCondition.RECOVERY_CONDITIONAL_EXPRESSION //
        + "     AND ( res.agent.id = :agentId OR :agentId IS NULL ) " //
        + "     AND ad.enabled = TRUE " //
        + "     AND ad.deleted = FALSE " //
        + "     AND ac.category = 'CONTROL' " //
        + "ORDER BY ac.id"), //
    @NamedQuery(name = AlertCondition.QUERY_BY_CATEGORY_THRESHOLD, query = "" //
        + "  SELECT new org.rhq.core.domain.alert.composite.AlertConditionScheduleCategoryComposite " //
        + "       ( " //
        + "         ac, " //
        + "         ms.id, " //
        + "         md.dataType " //
        + "       ) " //
        + "    FROM AlertCondition AS ac " //
        + "    JOIN ac.alertDefinition ad " //
        + "    JOIN ad.resource res " //
        + "    JOIN ac.measurementDefinition md, MeasurementSchedule ms " //
        + "   WHERE " + AlertCondition.RECOVERY_CONDITIONAL_EXPRESSION //
        + "     AND ( res.agent.id = :agentId OR :agentId IS NULL ) " //
        + "     AND ad.enabled = TRUE " //
        + "     AND ad.deleted = FALSE " //
        + "     AND ms.definition = md " //
        + "     AND ms.resource = res " //
        + "     AND ac.category = 'THRESHOLD' " //
        + "ORDER BY ac.id"), //
    @NamedQuery(name = AlertCondition.QUERY_BY_CATEGORY_EVENT, query = "" //
        + "  SELECT new org.rhq.core.domain.alert.composite.AlertConditionEventCategoryComposite " //
        + "       ( " //
        + "         ac, " //
        + "         res.id " //
        + "       ) " //
        + "    FROM AlertCondition AS ac " //
        + "    JOIN ac.alertDefinition ad " //
        + "    JOIN ad.resource res " //
        + "   WHERE " + AlertCondition.RECOVERY_CONDITIONAL_EXPRESSION //
        + "     AND ( res.agent.id = :agentId OR :agentId IS NULL ) " //
        + "     AND ad.enabled = TRUE " //
        + "     AND ad.deleted = FALSE " //
        + "     AND ac.category = 'EVENT' " //
        + "ORDER BY ac.id"), //
    @NamedQuery(name = AlertCondition.QUERY_BY_CATEGORY_RESOURCE_CONFIG, query = "" //
        + "  SELECT new org.rhq.core.domain.alert.composite.AlertConditionResourceConfigurationCategoryComposite " //
        + "       ( " //
        + "         ac, " //
        + "         res.id, " //
        + "         resConfig " //
        + "       ) " //
        + "    FROM AlertCondition AS ac " //
        + "    JOIN ac.alertDefinition ad " //
        + "    JOIN ad.resource res " //
        + "    LEFT JOIN res.resourceConfiguration resConfig " //
        + "   WHERE " + AlertCondition.RECOVERY_CONDITIONAL_EXPRESSION //
        + "     AND ( res.agent.id = :agentId OR :agentId IS NULL ) " //
        + "     AND ad.enabled = TRUE " //
        + "     AND ad.deleted = FALSE " //
        + "     AND ac.category = 'RESOURCE_CONFIG' " //
        + "ORDER BY ac.id"), //
    @NamedQuery(name = AlertCondition.QUERY_BY_CATEGORY_DRIFT, query = "" //
        + "  SELECT new org.rhq.core.domain.alert.composite.AlertConditionDriftCategoryComposite " //
        + "       ( " //
        + "         ac, " //
        + "         res.id " //
        + "       ) " //
        + "    FROM AlertCondition AS ac " //
        + "    JOIN ac.alertDefinition ad " //
        + "    JOIN ad.resource res " //
        + "   WHERE " + AlertCondition.RECOVERY_CONDITIONAL_EXPRESSION //
        + "     AND ( res.agent.id = :agentId OR :agentId IS NULL ) " //
        + "     AND ad.enabled = TRUE " //
        + "     AND ad.deleted = FALSE " //
        + "     AND ac.category = 'DRIFT' " //
        + "ORDER BY ac.id"), //
    @NamedQuery(name = AlertCondition.QUERY_BY_CATEGORY_RANGE, query = "" //
        + "  SELECT new org.rhq.core.domain.alert.composite.AlertConditionRangeCategoryComposite " //
        + "       ( " //
        + "         ac, " //
        + "         ms.id, " //
        + "         md.dataType " //
        + "       ) " //
        + "    FROM AlertCondition AS ac " //
        + "    JOIN ac.alertDefinition ad " //
        + "    JOIN ad.resource res " //
        + "    JOIN ac.measurementDefinition md, MeasurementSchedule ms " //
        + "   WHERE " + AlertCondition.RECOVERY_CONDITIONAL_EXPRESSION //
        + "     AND ( res.agent.id = :agentId OR :agentId IS NULL ) " //
        + "     AND ad.enabled = TRUE " //
        + "     AND ad.deleted = FALSE " //
        + "     AND ms.definition = md " //
        + "     AND ms.resource = res " //
        + "     AND ac.category = 'RANGE' " //
        + "ORDER BY ac.id"), //
    @NamedQuery(name = AlertCondition.QUERY_BY_CATEGORY_COUNT_PARAMETERIZED, query = "" //
        + "  SELECT count(ac.id) " //
        + "    FROM AlertCondition AS ac " //
        + "    JOIN ac.alertDefinition ad " //
        + "    JOIN ad.resource res " //
        + "   WHERE " + AlertCondition.RECOVERY_CONDITIONAL_EXPRESSION //
        + "     AND ( res.agent.id = :agentId OR :agentId IS NULL ) " //
        + "     AND ad.enabled = TRUE " //
        + "     AND ad.deleted = FALSE " //
        + "     AND ac.category = :category "),
    @NamedQuery(name = AlertCondition.QUERY_FIND_RESOURCE_STATUS_BY_CONDITION_ID, query = "" //
        + "  SELECT res.inventoryStatus " //
        + "    FROM AlertCondition AS ac " //
        + "    JOIN ac.alertDefinition ad " //
        + "    JOIN ad.resource res " //
        + "   WHERE ac.id = :alertConditionId "),
 @NamedQuery(name = AlertCondition.QUERY_DELETE_ORPHANED, query = "" //
        + "  DELETE FROM AlertCondition ac " //
        + "   WHERE ac.alertDefinition IS NULL " //
        + "     AND NOT EXISTS ( SELECT acl FROM AlertConditionLog acl WHERE acl.condition.id = ac.id ) ") })
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_ALERT_CONDITION_ID_SEQ", sequenceName = "RHQ_ALERT_CONDITION_ID_SEQ")
@Table(name = "RHQ_ALERT_CONDITION")
@XmlAccessorType(XmlAccessType.FIELD)
public class AlertCondition implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_DELETE_BY_RESOURCES = "AlertCondition.deleteByResources";
    public static final String QUERY_BY_CATEGORY_BASELINE = "AlertCondition.byCategoryBaseline";
    public static final String QUERY_BY_CATEGORY_CHANGE = "AlertCondition.byCategoryChange";
    public static final String QUERY_BY_CATEGORY_TRAIT = "AlertCondition.byCategoryTrait";
    public static final String QUERY_BY_CATEGORY_AVAILABILITY = "AlertCondition.byCategoryAvailability";
    public static final String QUERY_BY_CATEGORY_CONTROL = "AlertCondition.byCategoryControl";
    public static final String QUERY_BY_CATEGORY_THRESHOLD = "AlertCondition.byCategoryThreshold";
    public static final String QUERY_BY_CATEGORY_EVENT = "AlertCondition.byCategoryEvent";
    public static final String QUERY_BY_CATEGORY_RESOURCE_CONFIG = "AlertCondition.byCategoryResourceConfig";
    public static final String QUERY_BY_CATEGORY_DRIFT = "AlertCondition.byCategoryDrift";
    public static final String QUERY_BY_CATEGORY_RANGE = "AlertCondition.byCategoryRange";

    public static final String QUERY_BY_CATEGORY_COUNT_BASELINE = "AlertCondition.byCategoryCountBaseline";
    public static final String QUERY_BY_CATEGORY_COUNT_PARAMETERIZED = "AlertCondition.byCategoryCountParameterized";

    public static final String QUERY_FIND_RESOURCE_STATUS_BY_CONDITION_ID = "AlertCondition.findResourceStatus";

    public static final String QUERY_DELETE_ORPHANED = "AlertCondition.deleteOrphaned";

    public static final String RECOVERY_CONDITIONAL_EXPRESSION = "" //
        + " ( ad.recoveryId = 0 " //
        + " OR ( ad.recoveryId <> 0 " //
        + "      AND EXISTS ( SELECT iad FROM AlertDefinition iad " //
        + "                    WHERE iad.id = ad.recoveryId " //
        + "                      AND iad.deleted = FALSE " //
        + "                      AND iad.enabled = FALSE " //
        + "                 ) " //
        + "     ) " //
        + "  ) ";
    
    public static final String ADHOC_SEPARATOR = "@@@";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_ALERT_CONDITION_ID_SEQ")
    @Id
    private int id;

    @Column(name = "TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertConditionCategory category;

    @JoinColumn(name = "MEASUREMENT_DEFINITION_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @XmlTransient
    private MeasurementDefinition measurementDefinition;

    @Column(name = "NAME")
    private String name;

    @Column(name = "COMPARATOR")
    private String comparator;

    @Column(name = "THRESHOLD")
    private Double threshold;

    @Column(name = "OPTION_STATUS")
    private String option;

    @Column(name = "TRIGGER_ID")
    private Integer triggerId;

    @JoinColumn(name = "ALERT_DEFINITION_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @XmlTransient
    private AlertDefinition alertDefinition;

    @OneToMany(mappedBy = "condition", cascade = CascadeType.ALL)
    @OrderBy
    // primary key
    private Set<AlertConditionLog> conditionLogs = new LinkedHashSet<AlertConditionLog>();

    /**
     * Creates a new alert condition.
     */
    public AlertCondition() {
    }

    public AlertCondition(AlertDefinition alertDef, AlertConditionCategory type) {
        this.alertDefinition = alertDef;
        this.category = type;
    }

    /**
     * Creates a skeletal copy of the specified alert condition.
     *
     * @param cond the alert condition to be copied
     */
    public AlertCondition(AlertCondition cond) {
        this.category = cond.category;
        this.measurementDefinition = cond.measurementDefinition;
        this.name = cond.name;
        this.comparator = cond.comparator;
        this.threshold = cond.threshold;
        this.option = cond.option;
        this.triggerId = cond.triggerId;
        // Don't copy the condition logs.
    }

    public int getId() {
        return this.id;
    }

    public AlertConditionCategory getCategory() {
        return this.category;
    }

    public void setCategory(AlertConditionCategory category) {
        this.category = category;
    }

    /**
     * Identifies the measurement definition of the metric that is to be compared when determining
     * if the condition is true. This is null if the condition category is not a metric-related one
     * (metric related categories are THRESHOLD, TRAIT, BASELINE and CHANGE; others are not).
     * 
     * @return measurement definition or null
     */
    public MeasurementDefinition getMeasurementDefinition() {
        return this.measurementDefinition;
    }

    public void setMeasurementDefinition(MeasurementDefinition measurementDefinition) {
        this.measurementDefinition = measurementDefinition;
    }

    /**
     * The name of the condition whose semantics are different based on this condition's category:
     * 
     * AVAILABILITY: The relevant Avail AlertConditionOperator name
     * THRESHOLD: the name of the metric (TODO: today its the display name, very bad for i18n purposes)
     * BASELINE: the name of the metric (TODO: today its the display name, very bad for i18n purposes)
     * CHANGE: the name of the metric (TODO: today its the display name, very bad for i18n purposes)
     *         OR (for calltime alert conditions only) this will be the optional regular expression condition
     *         (which may be null)
     * TRAIT: the name of the trait (TODO: today its the display name, very bad for i18n purposes)
     * CONTROL: the name of the operation (not its display name)
     * EVENT: the level of event to compare with (DEBUG, INFO, WARN, ERROR, FATAL)
     * RESOURCE_CONFIG: n/a (null)
     * DRIFT: the name of the drift definition that triggered the drift detection. This is actually a
     *        regex that allows the user to match more than one drift definition if they so choose.
     *        (this value may be null, in which case it doesn't matter which drift definition were the ones
     *         in which the drift was detected) 
     * RANGE: the name of the metric (TODO: today its the display name, very bad for i18n purposes)
     * 
     * @return additional information about the condition
     */
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * THRESHOLD and BASELINE: one of these comparators: "<", ">" or "=" 
     * For calltime alert conditions (i.e. category CHANGE for calltime metric definitions),
     * comparator will be one of these comparators: "HI", "LO", "CH" (where "CH" means "change").
     * RANGE: one of these comparators "<", ">" (meaning inside and outside the range respectively)
     *        or one of these "<=", ">=" (meaning inside and outside inclusive respectively)
     * 
     * Other types of conditions will return <code>null</code> (i.e. this will be
     * null if the condition does not compare values).
     *
     * @return comparator string
     */
    public String getComparator() {
        return this.comparator;
    }

    public void setComparator(String comparator) {
        this.comparator = comparator;
    }

    /**
     * Returns the threshold to compare a measurement value to see if the condition is true.
     * This is only valid for conditions of category THRESHOLD, BASELINE, RANGE and CHANGE (but
     * only where CHANGE is for a calltime metric alert condition). All other
     * condition types will return <code>null</code>.
     * 
     * Note: If RANGE condition, this threshold is always the LOW end of the range.
     *       The high end of the range is in {@link #getOption()}.
     *  
     * @return threshold value or null
     */
    public Double getThreshold() {
        return this.threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    /**
     * The option string is optional and its semantics differ based on the category of this condition:
     * AVAILABILITY: n/a
     * AVAIL_DURATION: the duration, in minutes
     * THRESHOLD: for calltime metric conditions, one of "MIN, "MAX", "AVG" - all others are n/a
     * BASELINE: one of "min", "max" or "mean" - indicates what the threshold is compared to (min/max/avg baseline value)
     * CHANGE: for calltime metric conditions, one of "MIN, "MAX", "AVG" - all others are n/a
     * TRAIT: n/a
     * CONTROL: the {@link OperationRequestStatus} name (SUCCESS, FAILURE, etc).
     * EVENT: the regular expression of the message to match (which may be empty string if not specified)
     * RESOURCE_CONFIG: n/a
     * DRIFT: a regular expression to match files whose content drifted (may be empty string or null if not specified)
     * RANGE: the string form of a double value that is the HIGH end of the range (low end is {@link #getThreshold()})
     *
     * @return additional information about the condition
     */
    public String getOption() {
        return this.option;
    }

    public void setOption(String option) {
        this.option = option;
    }

    public Integer getTriggerId() {
        return this.triggerId;
    }

    public void setTriggerId(Integer triggerId) {
        this.triggerId = triggerId;
    }

    public AlertDefinition getAlertDefinition() {
        return this.alertDefinition;
    }

    public void setAlertDefinition(AlertDefinition alertDef) {
        this.alertDefinition = alertDef;
    }

    public Set<AlertConditionLog> getConditionLogs() {
        return this.conditionLogs;
    }

    public void addConditionLog(AlertConditionLog condLog) {
        this.conditionLogs.add(condLog);
        condLog.setCondition(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof AlertCondition)) {
            return false;
        }

        final AlertCondition other = (AlertCondition) obj;
        if (category == null) {
            if (other.category != null) {
                return false;
            }
        } else if (!category.equals(other.category)) {
            return false;
        }

        if (comparator == null) {
            if (other.comparator != null) {
                return false;
            }
        } else if (!comparator.equals(other.comparator)) {
            return false;
        }

        if (id != other.id) {
            return false;
        }

        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }

        if (option == null) {
            if (other.option != null) {
                return false;
            }
        } else if (!option.equals(other.option)) {
            return false;
        }

        if (threshold == null) {
            if (other.threshold != null) {
                return false;
            }
        } else if (!threshold.equals(other.threshold)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((category == null) ? 0 : category.hashCode());
        result = (prime * result) + ((comparator == null) ? 0 : comparator.hashCode());
        result = (prime * result) + id;
        result = (prime * result) + ((name == null) ? 0 : name.hashCode());
        result = (prime * result) + ((option == null) ? 0 : option.hashCode());
        result = (prime * result) + ((threshold == null) ? 0 : threshold.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "org.rhq.core.domain.alert.AlertCondition" + "[ " + "id=" + id + ", " + "category=" + category + ", "
            + "name=" + name + ", " + "comparator='" + comparator + "', " + "threshold=" + threshold + ", " + "option="
            + option + " ]";
    }
}