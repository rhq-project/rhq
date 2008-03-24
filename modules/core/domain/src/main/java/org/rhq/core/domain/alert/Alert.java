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
package org.rhq.core.domain.alert;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.alert.notification.AlertNotificationLog;

/**
 * A JON alert.
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = Alert.QUERY_DASHBOARD_ALL_ADMIN, query = "SELECT a " + "  FROM Alert AS a "
        + " WHERE a.ctime >= :startDate " + "  AND ( :priority = a.alertDefinition.priority OR :priority IS NULL ) "),
    @NamedQuery(name = Alert.QUERY_DASHBOARD_ALL, query = "SELECT a "
        + "  FROM Alert AS a JOIN a.alertDefinition ad JOIN ad.resource res "
        + "  JOIN res.implicitGroups g JOIN g.roles r JOIN r.subjects s " + "  WHERE s.id = :subjectId "
        + "   AND a.ctime >= :startDate " + "   AND ( :priority = a.alertDefinition.priority OR :priority IS NULL ) "),
    @NamedQuery(name = Alert.QUERY_DASHBOARD_BY_RESOURCE_IDS_ADMIN, query = "SELECT a " + "FROM Alert AS a "
        + "WHERE a.ctime >= :startDate " + "AND ( :priority = a.alertDefinition.priority OR :priority IS NULL ) "
        + "AND a.alertDefinition.resource.id IN ( :resourceIds )"),
    @NamedQuery(name = Alert.QUERY_DASHBOARD_BY_RESOURCE_IDS, query = "SELECT a "
        + "  FROM Alert AS a JOIN a.alertDefinition ad JOIN ad.resource res "
        + "  JOIN res.implicitGroups g JOIN g.roles r JOIN r.subjects s " + " WHERE s.id = :subjectId "
        + "   AND a.ctime >= :startDate " + "   AND ( :priority = a.alertDefinition.priority OR :priority IS NULL ) "
        + "   AND res.id IN ( :resourceIds )"),
    @NamedQuery(name = Alert.QUERY_FIND_BY_RESOURCE, query = "SELECT a " + "FROM Alert AS a "
        + "WHERE a.alertDefinition.resource.id = :id"),
    @NamedQuery(name = Alert.QUERY_FIND_BY_MEASUREMENT_DEFINITION_ID, query = "SELECT a " + "  FROM Alert AS a "
        + "  JOIN a.alertDefinition definition " + "  JOIN definition.conditions condition "
        + " WHERE condition.measurementDefinition.id = :measurementDefinitionId "
        + "   AND a.ctime BETWEEN :begin AND :end"),
    @NamedQuery(name = Alert.QUERY_FIND_BY_MEAS_DEF_ID_AND_RESOURCES, query = "SELECT a " + "  FROM Alert AS a "
        + "  JOIN a.alertDefinition definition " + "  JOIN definition.conditions condition "
        + " WHERE condition.measurementDefinition.id = :measurementDefinitionId "
        + "   AND definition.resource IN (:resources) " + "   AND (a.ctime BETWEEN :startDate AND :endDate)"),
    @NamedQuery(name = Alert.QUERY_GET_ALERT_COUNT_FOR_SCHEDULES, query = "SELECT sched.id, count(*) "
        + "  FROM Alert AS a " + "  JOIN a.alertDefinition aDef " + "  JOIN aDef.conditions condition "
        + "  JOIN aDef.resource res" + "  JOIN condition.measurementDefinition mDef " + "  JOIN mDef.schedules sched"
        + " WHERE sched.definition = mDef.id" + "   AND sched.resource = res " + "   AND sched.id IN (:schedIds) "
        + "   AND (a.ctime BETWEEN :startDate AND :endDate)" + "GROUP BY sched.id"),
    @NamedQuery(name = Alert.QUERY_FIND_BY_RESOURCE_DATED, query = "SELECT a " + "FROM Alert AS a "
        + "WHERE a.alertDefinition.resource.id = :id " + "AND (a.ctime BETWEEN :startDate AND :endDate)"),
    @NamedQuery(name = Alert.QUERY_FIND_ALL, query = "SELECT a FROM Alert AS a"),
    @NamedQuery(name = Alert.QUERY_DELETE_BY_CTIME, query = "DELETE FROM Alert AS a WHERE a.ctime BETWEEN :begin AND :end"),
    @NamedQuery(name = Alert.QUERY_DELETE_BY_RESOURCE, query = "DELETE Alert AS alert " + "WHERE alert.id IN "
        + "( SELECT ia.id " + "FROM Alert ia " + "WHERE ia.alertDefinition.resource.id = :resourceId " + ")"),
    @NamedQuery(name = Alert.QUERY_DELETE_BY_RESOURCES, query = "DELETE FROM Alert a WHERE a.alertDefinition IN ( SELECT ad FROM AlertDefinition ad WHERE ad.resource IN (:resources))") })
@SequenceGenerator(name = "RHQ_ALERT_ID_SEQ", sequenceName = "RHQ_ALERT_ID_SEQ", allocationSize = 100)
@Table(name = "RHQ_ALERT")
public class Alert implements Serializable {
    public static final String QUERY_DASHBOARD_ALL = "Alert.DashboardAll";
    public static final String QUERY_DASHBOARD_ALL_ADMIN = "Alert.DashboardAll_admin";
    public static final String QUERY_DASHBOARD_BY_RESOURCE_IDS = "Alert.DashboardByResourceIds";
    public static final String QUERY_DASHBOARD_BY_RESOURCE_IDS_ADMIN = "Alert.DashboardByResourceIds_admin";
    public static final String QUERY_FIND_ALL = "Alert.findAll";
    public static final String QUERY_FIND_BY_RESOURCE = "Alert.findByResource";
    public static final String QUERY_FIND_BY_MEASUREMENT_DEFINITION_ID = "Alert.findByMeasurementDefinitionId";
    public static final String QUERY_FIND_BY_RESOURCE_DATED = "Alert.findByResourceDated";
    public static final String QUERY_DELETE_BY_CTIME = "Alert.deleteByCTime";
    public static final String QUERY_DELETE_BY_RESOURCE = "Alert.deleteByResource";
    public static final String QUERY_DELETE_BY_RESOURCES = "Alert.deleteByResources";
    public static final String QUERY_FIND_BY_MEAS_DEF_ID_AND_RESOURCES = "Alert.findByMeasDefIdAndResources";
    public static final String QUERY_GET_ALERT_COUNT_FOR_SCHEDULES = "Alert.QUERY_GET_ALERT_COUNT_FOR_SCHEDULES";

    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RHQ_ALERT_ID_SEQ")
    @Id
    @SuppressWarnings( { "unused" })
    private int id;

    @Column(name = "CTIME", nullable = false)
    private long ctime;

    @JoinColumn(name = "ALERT_DEFINITION_ID", referencedColumnName = "ID")
    @ManyToOne
    private AlertDefinition alertDefinition;

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL)
    @OrderBy
    @org.hibernate.annotations.Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    // primary key
    private Set<AlertConditionLog> conditionLogs = new LinkedHashSet<AlertConditionLog>();

    @JoinColumn(name = "ALERT_NOTIF_LOG_ID", referencedColumnName = "ID")
    @OneToOne(cascade = { CascadeType.PERSIST, CascadeType.REMOVE })
    private AlertNotificationLog alertNotificationLog;

    @Column(name = "TRIGGERED_OPERATION", nullable = true)
    private String triggeredOperation;

    /**
     * Creates a new alert. (required by EJB3 spec, but not used)
     */
    protected Alert() {
    }

    /**
     * Creates a new alert with the specified definition and creation time.
     *
     * @param alertDefinition the definition
     * @param ctime           the creation time
     */
    public Alert(AlertDefinition alertDefinition, long ctime) {
        this.alertDefinition = alertDefinition;
        setAlertNotificationLog(new AlertNotificationLog(alertDefinition));
        // Do not load the collection side from a one-to-many, This is very slow to load all existing alerts
        // and unnecessary for creating the link
        // alertDefinition.addAlert(this);
        this.ctime = ctime;
        if (alertDefinition.getOperationDefinition() != null) {
            setTriggeredOperation(alertDefinition.getOperationDefinition().getDisplayName());
        }
    }

    public int getId() {
        return this.id;
    }

    public AlertDefinition getAlertDefinition() {
        return this.alertDefinition;
    }

    public void setAlertDefinition(AlertDefinition alertDefinition) {
        this.alertDefinition = alertDefinition;
    }

    public long getCtime() {
        return this.ctime;
    }

    public Set<AlertConditionLog> getConditionLogs() {
        return this.conditionLogs;
    }

    public void addConditionLog(AlertConditionLog conditionLog) {
        this.conditionLogs.add(conditionLog);
        conditionLog.setAlert(this);
    }

    public AlertNotificationLog getAlertNotificationLog() {
        return this.alertNotificationLog;
    }

    public void setAlertNotificationLog(AlertNotificationLog alertNotificationLog) {
        this.alertNotificationLog = alertNotificationLog;
        alertNotificationLog.setAlert(this);
    }

    public String getTriggeredOperation() {
        return triggeredOperation;
    }

    public void setTriggeredOperation(String triggeredOperation) {
        this.triggeredOperation = triggeredOperation;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof Alert)) {
            return false;
        }

        Alert alert = (Alert) obj;
        return (id == alert.id) && (ctime == alert.ctime);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = (31 * result) + (int) (ctime ^ (ctime >>> 32));
        return result;
    }

    public String toSimpleString() {
        return "Alert[id=" + this.id + "]";
    }

    @Override
    public String toString() {
        return "org.rhq.core.domain.alert.Alert[id=" + this.id + ", alertDefinition=" + this.alertDefinition
            + ", ctime=" + this.ctime + "]";
    }
}