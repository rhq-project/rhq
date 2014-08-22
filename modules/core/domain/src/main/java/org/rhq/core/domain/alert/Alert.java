/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.alert.notification.AlertNotificationLog;

/**
 * @author Joseph Marques
 */
@Entity
@NamedQueries({
    @NamedQuery(name = Alert.QUERY_FIND_BY_MEASUREMENT_DEFINITION_ID, query = "SELECT a " + "  FROM Alert AS a "
        + "  JOIN a.alertDefinition definition " + "  JOIN definition.conditions condition "
        + " WHERE condition.measurementDefinition.id = :measurementDefinitionId "
        + "   AND a.ctime BETWEEN :begin AND :end"),
    @NamedQuery(name = Alert.QUERY_FIND_BY_MEAS_DEF_ID_AND_RESOURCES, query = "SELECT a " + "  FROM Alert AS a "
        + "  JOIN a.alertDefinition definition " + "  JOIN definition.conditions condition "
        + " WHERE condition.measurementDefinition.id = :measurementDefinitionId "
        + "   AND definition.resource.id IN (:resourceIds) " //
        + "   AND (a.ctime BETWEEN :startDate AND :endDate)"),
    @NamedQuery(name = Alert.QUERY_FIND_BY_MEAS_DEF_ID_AND_RESOURCEGROUP, query = "" //
        + "SELECT a " //
        + "  FROM Alert AS a " //
        + "  JOIN a.alertDefinition definition " //
        + "  JOIN definition.conditions condition " //
        + " WHERE condition.measurementDefinition.id = :measurementDefinitionId " //
        + "   AND definition.resource IN ( SELECT res " //
        + "                                  FROM ResourceGroup rg " //
        + "                                  JOIN rg.explicitResources res " //
        + "                                 WHERE rg.id = :groupId ) " //
        + "   AND ( a.ctime BETWEEN :startDate AND :endDate )"),
    @NamedQuery(name = Alert.QUERY_FIND_BY_MEAS_DEF_ID_AND_AUTOGROUP, query = "" //
        + "SELECT a " //
        + "  FROM Alert AS a " //
        + "  JOIN a.alertDefinition definition " //
        + "  JOIN definition.conditions condition " //
        + " WHERE condition.measurementDefinition.id = :measurementDefinitionId " //
        + "   AND definition.resource IN ( SELECT res " //
        + "                                  FROM Resource res " //
        + "                                 WHERE res.parentResource.id = :parentId " //
        + "                                   AND res.resourceType.id = :typeId ) " //
        + "   AND ( a.ctime BETWEEN :startDate AND :endDate )"),
    @NamedQuery(name = Alert.QUERY_FIND_BY_MEAS_DEF_ID_AND_RESOURCE, query = "" //
        + "SELECT a " //
        + "  FROM Alert AS a " //
        + "  JOIN a.alertDefinition definition " //
        + "  JOIN definition.conditions condition " //
        + " WHERE condition.measurementDefinition.id = :measurementDefinitionId " //
        + "   AND definition.resource.id = ( :resourceId ) " //
        + "   AND ( a.ctime BETWEEN :startDate AND :endDate )"),
    @NamedQuery(name = Alert.QUERY_GET_ALERT_COUNT_FOR_SCHEDULES, query = "SELECT sched.id, count(*) "
        + "  FROM Alert AS a JOIN a.alertDefinition aDef  JOIN aDef.conditions condition "
        + "  JOIN aDef.resource res  JOIN condition.measurementDefinition mDef   JOIN mDef.schedules sched"
        + " WHERE sched.definition = mDef.id   AND sched.resource = res    AND sched.id IN (:schedIds) "
        + "   AND (a.ctime BETWEEN :startDate AND :endDate)" + "GROUP BY sched.id"),
    @NamedQuery(name = Alert.QUERY_DELETE_BY_CTIME, query = "" //
        + "DELETE FROM Alert AS a " //
        + " WHERE a.ctime BETWEEN :begin AND :end"),//
    @NamedQuery(name = Alert.QUERY_RETURN_EXISTING_IDS, query = "" //
        + " SELECT a.id " //
        + "   FROM Alert a " //
        + "  WHERE a.id IN ( :alertIds ) "), //
    @NamedQuery(name = Alert.QUERY_CHECK_PERMISSION_BY_IDS, query = "" //
        + " SELECT COUNT(a) " //
        + "   FROM Alert a " //
        + "   JOIN a.alertDefinition ad " //
        + "   JOIN ad.resource res " //
        + "  WHERE a.id IN ( :alertIds ) " //
        + "    AND res.id IN ( SELECT rr.id FROM Resource rr " //
        + "                      JOIN rr.implicitGroups g JOIN g.roles r JOIN r.permissions p JOIN r.subjects s " //
        + "                     WHERE s.id = :subjectId  " //
        + "                       AND p = :permission ) "), //
    @NamedQuery(name = Alert.QUERY_DELETE_ALL, query = "" //
        + "DELETE FROM Alert a "), //
    @NamedQuery(name = Alert.QUERY_DELETE_BY_IDS, query = "" //
        + "DELETE Alert AS alert " //
        + " WHERE alert.id IN ( :alertIds )"), //
    @NamedQuery(name = Alert.QUERY_DELETE_BY_RESOURCES, query = "" //
        + "DELETE FROM Alert alert " //
        + " WHERE alert.id IN ( SELECT innerA.id " //
        + "                       FROM AlertDefinition ad " //
        + "                       JOIN ad.alerts innerA " //
        + "                      WHERE ad.resource.id IN ( :resourceIds ) )"),
    @NamedQuery(name = Alert.QUERY_DELETE_BY_RESOURCE_TEMPLATE, query = "DELETE FROM Alert alert "
        + "WHERE alert.id IN (SELECT innerAlerts.id " + "                   FROM AlertDefinition alertDef "
        + "                   JOIN alertDef.alerts innerAlerts "
        + "                   WHERE alertDef.resourceType.id = :resourceTypeId)"),
    @NamedQuery(name = Alert.QUERY_DELETE_BY_RESOURCE_GROUPS, query = "" //
        + "DELETE FROM Alert alert " //
        + " WHERE alert.id IN ( SELECT innerA.id " //
        + "                       FROM AlertDefinition ad " //
        + "                       JOIN ad.alerts innerA " //
        + "                       JOIN ad.resource.implicitGroups rg " //
        + "                      WHERE rg.id IN ( :groupIds ) )"),
    @NamedQuery(name = Alert.QUERY_ACKNOWLEDGE_ALL, query = "" //
        + "UPDATE Alert AS alert " //
        + "   SET alert.acknowledgingSubject = :subjectName, " //
        + "       alert.acknowledgeTime = :ackTime " //
        + " WHERE alert.acknowledgingSubject IS NULL "), //
    @NamedQuery(name = Alert.QUERY_ACKNOWLEDGE_BY_IDS, query = "" //
        + "UPDATE Alert AS alert " //
        + "   SET alert.acknowledgingSubject = :subjectName, " //
        + "       alert.acknowledgeTime = :ackTime " //
        + " WHERE alert.id IN ( :alertIds ) " //
        + "   AND alert.acknowledgingSubject IS NULL "), // only ack what hasn't already been ack'ed
    @NamedQuery(name = Alert.QUERY_ACKNOWLEDGE_BY_RESOURCES, query = "" //
        + "UPDATE Alert AS alert " //
        + "   SET alert.acknowledgingSubject = :subjectName, " //
        + "       alert.acknowledgeTime = :ackTime " //
        + " WHERE alert.id IN ( SELECT innerA.id " //
        + "                       FROM AlertDefinition ad " //
        + "                       JOIN ad.alerts innerA " //
        + "                      WHERE ad.resource.id IN ( :resourceIds ) )" //
        + "   AND alert.acknowledgingSubject IS NULL "),
    @NamedQuery(name = Alert.QUERY_ACKNOWLEDGE_BY_RESOURCE_GROUPS, query = "" //
        + "UPDATE Alert AS alert " //
        + "   SET alert.acknowledgingSubject = :subjectName, " //
        + "       alert.acknowledgeTime = :ackTime " //
        + " WHERE alert.id IN ( SELECT innerA.id " //
        + "                       FROM AlertDefinition ad " //
        + "                       JOIN ad.alerts innerA " //
        + "                       JOIN ad.resource.implicitGroups rg " //
        + "                      WHERE rg.id IN ( :groupIds ) )" //
        + "   AND alert.acknowledgingSubject IS NULL "),
    @NamedQuery(name = Alert.QUERY_FIND_ALL_COMPOSITES_ADMIN, query = "" //
        + "   SELECT new org.rhq.core.domain.alert.composite.AlertHistoryComposite" //
        + "        ( a, parent.id, parent.name ) " //
        + "     FROM Alert a " //
        + "     JOIN a.alertDefinition ad " //
        + "     JOIN ad.resource res " //
        + "LEFT JOIN res.parentResource parent " //
        /*
         * as much as i want to (for efficiency of the query [namely roundtrips to the db]) i can't use fetching here
         * because, when added, the query parser chokes with "query specified join fetching, but the owner of the
         * fetched association was not present in the select list"...even though it clearly is  ;/
         */
        //+ "     JOIN FETCH a.conditionLogs acl " //
        + "    WHERE (UPPER(res.name) LIKE :resourceFilter ESCAPE :escapeChar OR :resourceFilter IS NULL) " //
        + "      AND (UPPER(parent.name) LIKE :parentFilter ESCAPE :escapeChar OR :parentFilter IS NULL) " //
        + "      AND (a.ctime > :startTime OR :startTime IS NULL) " //
        + "      AND (a.ctime < :endTime OR :endTime IS NULL) " //
        + "      AND (a.id IN ( SELECT aa.id FROM Alert aa " //
        + "                       JOIN aa.conditionLogs aacl " //
        + "                       JOIN aacl.condition ac " //
        + "                      WHERE ac.category = :category ) " //
        + "           OR :category IS NULL) "), //
    @NamedQuery(name = Alert.QUERY_FIND_ALL_COMPOSITES, query = "" //
        + "   SELECT new org.rhq.core.domain.alert.composite.AlertHistoryComposite" //
        + "        ( a, parent.id, parent.name ) " //
        + "     FROM Alert a " //
        + "     JOIN a.alertDefinition ad " //
        + "     JOIN ad.resource res " //
        + "LEFT JOIN res.parentResource parent " //
        /*
         * as much as i want to (for efficiency of the query [namely roundtrips to the db]) i can't use fetching here
         * because, when added, the query parser chokes with "query specified join fetching, but the owner of the
         * fetched association was not present in the select list"...even though it clearly is  ;/
         */
        //+ "     JOIN FETCH a.conditionLogs acl " //
        + "    WHERE res.id IN ( SELECT rr.id FROM Resource rr " //
        + "                        JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s " //
        + "                       WHERE s.id = :subjectId ) " //
        + "      AND (UPPER(res.name) LIKE :resourceFilter ESCAPE :escapeChar OR :resourceFilter IS NULL) " //
        + "      AND (UPPER(parent.name) LIKE :parentFilter ESCAPE :escapeChar OR :parentFilter IS NULL) " //
        + "      AND (a.ctime > :startTime OR :startTime IS NULL) " //
        + "      AND (a.ctime < :endTime OR :endTime IS NULL) " //
        + "      AND (a.id IN ( SELECT aa.id FROM Alert aa " //
        + "                       JOIN aa.conditionLogs aacl " //
        + "                       JOIN aacl.condition ac " //
        + "                      WHERE ac.category = :category ) " //
        + "           OR :category IS NULL) ") })
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_ALERT_ID_SEQ", sequenceName = "RHQ_ALERT_ID_SEQ")
@Table(name = "RHQ_ALERT")
public class Alert implements Serializable {
    public static final String QUERY_FIND_BY_MEASUREMENT_DEFINITION_ID = "Alert.findByMeasurementDefinitionId";
    /**
     * @deprecated as of RHQ 4.13, no longer used
     */
    @Deprecated
    public static final String QUERY_DELETE_BY_CTIME = "Alert.deleteByCTime";
    public static final String QUERY_RETURN_EXISTING_IDS = "Alert.returnExistingIds";
    public static final String QUERY_CHECK_PERMISSION_BY_IDS = "Alert.checkPermissionByIds";
    public static final String QUERY_DELETE_ALL = "Alert.deleteByAll";
    public static final String QUERY_DELETE_BY_IDS = "Alert.deleteByIds";
    public static final String QUERY_DELETE_BY_RESOURCES = "Alert.deleteByResources";
    public static final String QUERY_DELETE_BY_RESOURCE_TEMPLATE = "Alert.deleteByResourceType";
    public static final String QUERY_DELETE_BY_RESOURCE_GROUPS = "Alert.deleteByResourceGroups";
    public static final String QUERY_ACKNOWLEDGE_ALL = "Alert.acknowledgeByAll";
    public static final String QUERY_ACKNOWLEDGE_BY_IDS = "Alert.acknowledgeByIds";
    public static final String QUERY_ACKNOWLEDGE_BY_RESOURCES = "Alert.acknowledgeByResources";
    public static final String QUERY_ACKNOWLEDGE_BY_RESOURCE_GROUPS = "Alert.acknowledgeByResourceGroups";
    public static final String QUERY_FIND_BY_MEAS_DEF_ID_AND_RESOURCES = "Alert.findByMeasDefIdAndResources";
    public static final String QUERY_FIND_BY_MEAS_DEF_ID_AND_RESOURCEGROUP = "Alert.findByMeasDefIdAndResourceGroup";
    public static final String QUERY_FIND_BY_MEAS_DEF_ID_AND_AUTOGROUP = "Alert.findByMeasDefIdAndAutoGroup";
    public static final String QUERY_FIND_BY_MEAS_DEF_ID_AND_RESOURCE = "Alert.findByMeasDefIdAndResource";
    public static final String QUERY_GET_ALERT_COUNT_FOR_SCHEDULES = "Alert.QUERY_GET_ALERT_COUNT_FOR_SCHEDULES";

    /**
     * @deprecated as of RHQ 4.13, no longer used
     */
    @Deprecated
    public static final String QUERY_NATIVE_TRUNCATE_SQL = "TRUNCATE TABLE RHQ_ALERT";

    // for subsystem view
    public static final String QUERY_FIND_ALL_COMPOSITES = "Alert.findAllComposites";
    public static final String QUERY_FIND_ALL_COMPOSITES_ADMIN = "Alert.findAllComposites_admin";

    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_ALERT_ID_SEQ")
    @Id
    private int id;

    @Column(name = "CTIME", nullable = false)
    private long ctime;

    @JoinColumn(name = "ALERT_DEFINITION_ID", referencedColumnName = "ID")
    @ManyToOne
    private AlertDefinition alertDefinition;

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy
    // primary key
    private Set<AlertConditionLog> conditionLogs = new LinkedHashSet<AlertConditionLog>();

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL)
    private List<AlertNotificationLog> alertNotificationLogs = new ArrayList<AlertNotificationLog>();

    /*
     * recoveryId and willRecover==true are mutually exclusive
     *
     * you are either a recovery alert, or an alert to be recovered
     */
    @Column(name = "RECOVERY_ID")
    private Integer recoveryId;

    @JoinColumn(name = "RECOVERY_ID", referencedColumnName = "ID", insertable = false, updatable = false, nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private AlertDefinition recoveryAlertDefinition;

    @Column(name = "WILL_RECOVER", nullable = false)
    private boolean willRecover;

    @Column(name = "ACK_TIME")
    private Long acknowledgeTime = -1L;

    @Column(name = "ACK_SUBJECT")
    private String acknowledgingSubject;

    /**
     * Creates a new alert. (required by EJB3 spec, but not used)
     */
    public Alert() {
    }

    /**
     * Creates a new alert with the specified definition and creation time.
     *
     * @param alertDefinition the definition
     * @param ctime           the creation time
     */
    public Alert(AlertDefinition alertDefinition, long ctime) {
        this.alertDefinition = alertDefinition;
        this.recoveryId = alertDefinition.getRecoveryId();
        this.willRecover = alertDefinition.getWillRecover();
        // Do not load the collection side from a one-to-many, This is very slow to load all existing alerts
        // and unnecessary for creating the link
        // alertDefinition.addAlert(this);
        this.ctime = ctime;
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

    public List<AlertNotificationLog> getAlertNotificationLogs() {
        return alertNotificationLogs;
    }

    public void setAlertNotificationLogs(List<AlertNotificationLog> alertNotificationLogs) {
        this.alertNotificationLogs = alertNotificationLogs;
    }

    public void addAlertNotificatinLog(AlertNotificationLog log) {
        this.alertNotificationLogs.add(log);
    }

    public Long getAcknowledgeTime() {
        return acknowledgeTime;
    }

    public void setAcknowledgeTime(Long acknowledgeTime) {
        this.acknowledgeTime = acknowledgeTime;
    }

    public String getAcknowledgingSubject() {
        return acknowledgingSubject;
    }

    public void setAcknowledgingSubject(String acknowledgingSubject) {
        this.acknowledgingSubject = acknowledgingSubject;
    }

    public boolean getWillRecover() {
        return this.willRecover;
    }

    public void setWillRecover(boolean willRecover) {
        if (willRecover && getRecoveryId() != 0) {
            throw new IllegalStateException(
                "An alert definition can either be a recovery definition or a definition to-be-recovered, but not both.");
        }
        this.willRecover = willRecover;
    }

    public Integer getRecoveryId() {
        return this.recoveryId;
    }

    public void setRecoveryId(Integer actOnTriggerId) {
        if (getWillRecover() && actOnTriggerId != 0) {
            throw new IllegalStateException(
                "An alert definition can either be a recovery definition or a definition to-be-recovered, but not both.");
        }
        this.recoveryId = actOnTriggerId;
    }

    public AlertDefinition getRecoveryAlertDefinition() {
        return this.recoveryAlertDefinition;
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
