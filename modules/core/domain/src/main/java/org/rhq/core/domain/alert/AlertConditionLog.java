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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * A log record for a triggered alert condition.
 *
 * @author Joseph Marques
 */
@Entity
@NamedQueries({
    @NamedQuery(name = AlertConditionLog.QUERY_FIND_UNMATCHED_LOG_BY_ALERT_CONDITION_ID, //
    query = "SELECT acl " //
        + "    FROM AlertConditionLog AS acl " //
        + "   WHERE acl.condition.id = :alertConditionId " //
        + "     AND acl.alert IS NULL"),
    @NamedQuery(name = AlertConditionLog.QUERY_FIND_UNMATCHED_LOGS_BY_ALERT_DEFINITION_ID, //
    query = "SELECT acl " //
        + "    FROM AlertConditionLog AS acl " //
        + "   WHERE acl.condition.alertDefinition.id = :alertDefinitionId " //
        + "     AND acl.alert IS NULL"), //
    @NamedQuery(name = AlertConditionLog.QUERY_DELETE_ALL, //
    query = "DELETE AlertConditionLog acl " //
        + "   WHERE acl.alert.id IN ( SELECT alert.id " //
        + "                             FROM Alert alert )"),
    @NamedQuery(name = AlertConditionLog.QUERY_DELETE_BY_ALERT_IDS, //
    query = "DELETE AlertConditionLog acl " //
        + "   WHERE acl.id IN ( SELECT ac.id " //
        + "                       FROM Alert a " //
        + "                       JOIN a.conditionLogs ac" // 
        + "                      WHERE a.id IN ( :alertIds ) )"),
    // This query removes condition logs via the alert def, not alerts, because not every condition log may 
    // yet be associated with an alert. This is required by Resource bulk delete to pick up the condition logs missed
    // by QUERY_DELETE_BY_RESOURCES.  It will not get the logs for AlertConditions detached from the AlertDefinition.  
    @NamedQuery(name = AlertConditionLog.QUERY_DELETE_BY_RESOURCES_BULK_DELETE, //
    query = "DELETE AlertConditionLog acl " //
        + "   WHERE acl.condition.id IN ( SELECT ac.id " //
        + "                             FROM AlertCondition ac " //
        + "                             JOIN ac.alertDefinition ad " //
        + "                            WHERE ad.resource.id IN ( :resourceIds ) ))"),
    // This query will remove condition logs for conditions that are associated with alerts, regardless of whether
    // the AlertCondition has been detached from the AlertDef. This is what you want if you are deleting alerts for
    // a resource.  It does not fully satisfy Resource bulk delete. 
    @NamedQuery(name = AlertConditionLog.QUERY_DELETE_BY_RESOURCES, //
    query = "DELETE AlertConditionLog acl " //
        + "   WHERE acl.alert.id IN ( SELECT alert.id " //
        + "                             FROM AlertDefinition alertDef " //
        + "                             JOIN alertDef.alerts alert " //
        + "                            WHERE alertDef.resource.id IN ( :resourceIds ) ))"),
    @NamedQuery(name = AlertConditionLog.QUERY_DELETE_BY_RESOURCE_TEMPLATE, // 
    query = "DELETE AlertConditionLog acl " //
        + "   WHERE acl.alert.id IN (SELECT alert.id "
        + "                          FROM   AlertDefinition alertDef "
        + "                          JOIN   alertDef.alerts alert "
        + "                          WHERE  alertDef.resourceType.id = :resourceTypeId)"),
    @NamedQuery(name = AlertConditionLog.QUERY_DELETE_BY_RESOURCE_GROUPS, //
    query = "DELETE AlertConditionLog acl " //
        + "   WHERE acl.alert.id IN ( SELECT alert.id " //
        + "                             FROM AlertDefinition ad " //
        + "                             JOIN ad.alerts alert " //
        + "                             JOIN ad.resource res " //
        + "                             JOIN res.implicitGroups rg " //
        + "                            WHERE rg.id IN ( :groupIds ) ))"),
    @NamedQuery(name = AlertConditionLog.QUERY_DELETE_BY_ALERT_CTIME, //
    query = "DELETE AlertConditionLog acl " //
        + "   WHERE acl.id IN ( SELECT iacl.id " //
        + "                       FROM AlertConditionLog iacl " //
        + "                      WHERE iacl.alert.ctime BETWEEN :begin AND :end )"),
    @NamedQuery(name = AlertConditionLog.QUERY_DELETE_UNMATCHED_BY_ALERT_DEFINITION_ID, //
    query = "DELETE AlertConditionLog acl" // 
        + "   WHERE acl.id IN ( SELECT iacl.id " //
        + "                       FROM AlertConditionLog iacl" //
        + "                      WHERE iacl.condition.alertDefinition.id = :alertDefinitionId )" // 
        + "     AND acl.alert IS NULL") })
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_ALERT_CONDITION_LOG_ID_SEQ", sequenceName = "RHQ_ALERT_CONDITION_LOG_ID_SEQ")
@Table(name = "RHQ_ALERT_CONDITION_LOG")
public class AlertConditionLog implements Serializable {
    public static final String QUERY_FIND_UNMATCHED_LOG_BY_ALERT_CONDITION_ID = "AlertConditinLog.findUnmatchedLogByAlertConditionId";
    public static final String QUERY_FIND_UNMATCHED_LOGS_BY_ALERT_DEFINITION_ID = "AlertConditinLog.findUnmatchedLogsByAlertDefinitionId";

    public static final String QUERY_DELETE_ALL = "AlertConditionLog.deleteByAll";
    public static final String QUERY_DELETE_BY_ALERT_IDS = "AlertConditionLog.deleteByAlertIds";
    public static final String QUERY_DELETE_BY_RESOURCES = "AlertConditionLog.deleteByResources";
    public static final String QUERY_DELETE_BY_RESOURCES_BULK_DELETE = "AlertConditionLog.deleteByResourcesBulkDelete";
    public static final String QUERY_DELETE_BY_RESOURCE_TEMPLATE = "AlertConditionLog.deleteByResourceType";
    public static final String QUERY_DELETE_BY_RESOURCE_GROUPS = "AlertConditionLog.deleteByResourceGroups";
    public static final String QUERY_DELETE_BY_ALERT_CTIME = "AlertConditionLog.deleteByAlertCTime";
    public static final String QUERY_DELETE_UNMATCHED_BY_ALERT_DEFINITION_ID = "AlertConditionLog.deleteUnmatchedByAlertDefinitionId";

    public static final String QUERY_NATIVE_TRUNCATE_SQL = "TRUNCATE TABLE RHQ_ALERT_CONDITION_LOG";

    /**
     * this is a character limit, when stored certain vendors may require the string be clipped to
     * satisfy a byte limit (postgres can store the 4000 chars, oracle only 4000 bytes).
     */
    public static final int MAX_LOG_LENGTH = 4000;

    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_ALERT_CONDITION_LOG_ID_SEQ")
    @Id
    private int id;

    /**
     * Since alert conditions can occur at potentially grossly different times in the system, and since the process for
     * calculating whether an alert should fire based on the states of these independently derived conditions is
     * out-of-band, THIS ctime is now actually more meaningful than the ctime on the alert.
     */
    @Column(name = "CTIME", nullable = false)
    private long ctime;

    @Column(name = "VALUE", nullable = false)
    private String value;

    @JoinColumn(name = "ALERT_ID", referencedColumnName = "ID")
    @ManyToOne
    private Alert alert;

    @JoinColumn(name = "CONDITION_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private AlertCondition condition;

    /**
     * Creates a new alert condition log record. (required by EJB3 spec, but not used)
     */
    protected AlertConditionLog() {
    }

    /**
     * Creates a new log record for the specified alert condition. The alert that triggered the condition will be filled
     * in later by a separate out-of-band process that ensures all requisite conditions have been satisfied on the
     * corresponding alert.
     *
     * @param cond  condition that is being logged
     * @param ctime the time in millis when this condition was known to be true
     */
    public AlertConditionLog(AlertCondition cond, long ctime) {
        this.condition = cond;
        this.ctime = ctime;
    }

    public int getId() {
        return this.id;
    }

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }

    public String getValue() {
        return this.value;
    }

    /**
     * Make sure the value passed to this setter goes through this process:
     * <pre>
     * String value = ...
     * DatabaseType dbType = ...
     * value = dbType.getString(value, AlertConditionLog.MAX_LOG_LENGTH);
     * </pre>
     * 
     * @param value on Postgres shouldn't be longer than <code>MAX_LOG_LENGTH</code> chars,
     *              on Oracle <code>MAX_LOG_LENGTH</code> bytes.
     */
    public void setValue(String value) {
        if ((value != null) && (value.length() >= MAX_LOG_LENGTH)) {
            value = value.substring(0, MAX_LOG_LENGTH);
        }
        this.value = value;
    }

    public Alert getAlert() {
        return this.alert;
    }

    public void setAlert(Alert alert) {
        this.alert = alert;
    }

    public AlertCondition getCondition() {
        return this.condition;
    }

    public void setCondition(AlertCondition condition) {
        this.condition = condition;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof AlertConditionLog)) {
            return false;
        }

        AlertConditionLog that = (AlertConditionLog) obj;
        if (id != that.id) {
            return false;
        }

        if ((value != null) ? (!value.equals(that.value)) : (that.value != null)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = (31 * result) + ((value != null) ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "org.rhq.core.domain.alert.AlertConditionLog" + "[ " + "id=" + id + ", " + "value=" + value + ", "
            + condition + " ]";
    }
}
