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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;

/**
 * A JON alert definition.
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = AlertDefinition.QUERY_FIND_ALL, query = "SELECT a " + "  FROM AlertDefinition a "
        + " WHERE a.deleted = false " + "       AND a.resource IS NOT NULL"),
    @NamedQuery(name = AlertDefinition.QUERY_FIND_ALL_WITH_CONDITIONS, query = "SELECT a "
        + "  FROM AlertDefinition a " + "       LEFT JOIN FETCH a.conditions " + " WHERE a.deleted = false "
        + "       AND a.enabled = true AND a.resource IS NOT NULL"),
    @NamedQuery(name = AlertDefinition.QUERY_FIND_ALL_BY_RECOVERY_DEFINITION_ID, query = "SELECT a "
        + "  FROM AlertDefinition a " + "       LEFT JOIN FETCH a.conditions "
        + " WHERE a.deleted = false AND a.enabled = true " + "       AND a.recoveryId = :recoveryDefinitionId"),
    @NamedQuery(name = AlertDefinition.QUERY_FIND_BY_ALERT_TEMPLATE_ID, query = "SELECT a.id "
        + "  FROM AlertDefinition a " + " WHERE a.parentId = :alertTemplateId " + "       AND a.deleted = false"),
    @NamedQuery(name = AlertDefinition.QUERY_FIND_RESOURCE_IDS_WITH_NO_ACTIVE_TEMPLATE_DEFINITION, query = "SELECT res.id "
        + "  FROM Resource res "
        + " WHERE res.resourceType.id = :resourceTypeId "
        + "       AND res.inventoryStatus = :inventoryStatus "
        + "       AND ( res.alertDefinitions IS EMPTY "
        + "             OR 0 = ( SELECT COUNT(ad) "
        + "                        FROM res.alertDefinitions ad "
        + "                       WHERE ad.parentId = :alertTemplateId "
        + "                             AND ad.deleted = FALSE ) )"),
    @NamedQuery(name = AlertDefinition.QUERY_FIND_OPTION_ITEMS_BY_RESOURCE, //
    query = "SELECT new org.rhq.core.domain.common.composite.IntegerOptionItem(ad.id, ad.name) " //
        + "    FROM AlertDefinition ad " //
        + "   WHERE ad.resource.id = :resourceId " //
        + "     AND ad.deleted = false"),
    @NamedQuery(name = AlertDefinition.QUERY_FIND_BY_RESOURCE, query = "SELECT a " + "  FROM AlertDefinition a "
        + " WHERE a.resource.id = :id " + "       AND a.deleted = false"),
    @NamedQuery(name = AlertDefinition.QUERY_FIND_BY_RESOURCE_TYPE, query = "SELECT a " + "  FROM AlertDefinition a "
        + " WHERE a.resourceType.id = :typeId " + "       AND a.deleted = false"),
    @NamedQuery(name = AlertDefinition.QUERY_DELETE_BY_RESOURCES, query = "DELETE FROM AlertDefinition ad WHERE ad.resource IN (:resources))") })
@SequenceGenerator(name = "RHQ_ALERT_DEFINITION_ID_SEQ", sequenceName = "RHQ_ALERT_DEFINITION_ID_SEQ", allocationSize = 10)
@Table(name = "RHQ_ALERT_DEFINITION")
public class AlertDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL = "AlertDefinition.findAll";
    public static final String QUERY_FIND_ALL_WITH_CONDITIONS = "AlertDefinition.findAllWithConditions";
    public static final String QUERY_FIND_ALL_BY_RECOVERY_DEFINITION_ID = "AlertDefinition.findAllByRecoveryDefinitionId";
    public static final String QUERY_FIND_BY_ALERT_TEMPLATE_ID = "AlertDefinition.findByAlertTemplateId";
    public static final String QUERY_FIND_RESOURCE_IDS_WITH_NO_ACTIVE_TEMPLATE_DEFINITION = "AlertDefinition.findResourceIdsWithNoDefinition";
    public static final String QUERY_FIND_OPTION_ITEMS_BY_RESOURCE = "AlertDefinition.findOptionItemsByResource";
    public static final String QUERY_FIND_BY_RESOURCE = "AlertDefinition.findByResource";
    public static final String QUERY_FIND_BY_RESOURCE_TYPE = "AlertDefinition.findByResourceType";
    public static final String QUERY_DELETE_BY_RESOURCES = "AlertDefinition.deleteByResources";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RHQ_ALERT_DEFINITION_ID_SEQ")
    @Id
    @SuppressWarnings( { "unused" })
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "CTIME", nullable = false)
    private long ctime;

    @Column(name = "MTIME", nullable = false)
    private long mtime;

    /**
     * If this field is non-null, then this alert def is a copy of the resource type alert def with the specified id.
     */
    @Column(name = "PARENT_ID", nullable = false)
    private Integer parentId = new Integer(0);

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "PRIORITY", nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertPriority priority;

    @JoinColumn(name = "RESOURCE_TYPE_ID", nullable = true)
    @ManyToOne
    private ResourceType resourceType;

    @JoinColumn(name = "RESOURCE_ID", nullable = true)
    @ManyToOne
    private Resource resource;

    @Column(name = "ENABLED", nullable = false)
    private boolean enabled;

    @Embedded
    private AlertDampening alertDampening;

    /*
     * recoveryId and willRecover==true are mutually exclusive
     *
     * you are either a recovery alert, or an alert to be recovered
     */
    @Column(name = "RECOVERY_ID")
    private Integer recoveryId;

    @Column(name = "WILL_RECOVER", nullable = false)
    private boolean willRecover;

    @Column(name = "NOTIFY_FILTERED", nullable = false)
    private boolean notifyFiltered;

    @Column(name = "CONTROL_FILTERED", nullable = false)
    private boolean controlFiltered;

    @Column(name = "DELETED", nullable = false)
    private boolean deleted;

    @Column(name = "REQUIRED", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private BooleanExpression conditionExpression;

    @OneToMany(mappedBy = "alertDefinition", cascade = CascadeType.ALL)
    @OrderBy
    @org.hibernate.annotations.Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    // primary key
    private Set<AlertCondition> conditions = new LinkedHashSet<AlertCondition>(1); // Most alerts will only have one condition.

    @OneToMany(mappedBy = "alertDefinition", cascade = CascadeType.ALL)
    @org.hibernate.annotations.Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private Set<AlertNotification> alertNotifications = new HashSet<AlertNotification>();

    @JoinColumn(name = "OPERATION_DEF_ID", nullable = true)
    @ManyToOne
    private OperationDefinition operationDefinition;

    /*
     * As of Sept 29, 2007 there is no reason to expose this at the java layer.  However, this is required if we want to
     * be able to cascade delete the AlertDampeningEvents when an AlertDefinition is removed from the db, due to
     * deleting a Resource from inventory.
     */
    @OneToMany(mappedBy = "alertDefinition", cascade = CascadeType.REFRESH)
    @SuppressWarnings("unused")
    @org.hibernate.annotations.Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private Set<AlertDampeningEvent> alertDampeningEvents = new HashSet<AlertDampeningEvent>();

    /**
     * NOTE: This field is currently not accessed, so we don't need cascading for PERSIST/MERGE/REFRESH. However, we
     * still need cascading for REMOVE, so we don't get FK constraint violations when an Alert is removed.
     * AlertDefinitions aren't actually deleted, instead they have their 'deleted' flag set to true. But, if they were
     * actually deleted from persistent storage, we would still want the cascade remove.
     */
    @OneToMany(mappedBy = "alertDefinition", cascade = CascadeType.REMOVE)
    @OrderBy
    // primary key
    private Set<Alert> alerts = new LinkedHashSet<Alert>();

    /**
     * Creates a new alert definition.
     */
    public AlertDefinition() {
        // ctime and mtime will be set upon persist/update.
    }

    /**
     * Creates a skeletal copy of the specified alert definition.
     *
     * @param alertDef the alert definition to be copied
     */
    public AlertDefinition(AlertDefinition alertDef) {
        this();
        this.update(alertDef);
    }

    public void update(AlertDefinition alertDef) {
        /*
         * Don't copy the id, ctime, or mtime.
         */
        this.name = alertDef.name;
        this.description = alertDef.description;
        this.priority = alertDef.priority;
        this.enabled = alertDef.enabled;

        /*
         * parentId, resource, resourceType need to be managed by the calling context
         */

        if (alertDampening == null) {
            // an alert template is being created, so it's creating children definitions for the first time,
            // OR...an alert template is being updated, but this children definition was deleted at some point
            this.alertDampening = new AlertDampening(alertDef.getAlertDampening());
        } else {
            // an alert template is being updated, so it's updated it's existing children definitions
            this.alertDampening.update(alertDef.getAlertDampening());
        }

        this.willRecover = alertDef.willRecover;
        this.notifyFiltered = alertDef.notifyFiltered;
        this.controlFiltered = alertDef.controlFiltered;

        this.deleted = alertDef.deleted;
        this.conditionExpression = alertDef.conditionExpression;

        this.recoveryId = alertDef.recoveryId;

        // copy conditions
        Set<AlertCondition> copiedConditions = new HashSet<AlertCondition>();
        for (AlertCondition oldCondition : alertDef.getConditions()) {
            AlertCondition newCondition = new AlertCondition(oldCondition);
            newCondition.setAlertDefinition(this);
            copiedConditions.add(newCondition);
        }

        removeAllConditions();
        getConditions().addAll(copiedConditions);

        // copy notifications
        Set<AlertNotification> copiedNotifications = new HashSet<AlertNotification>();
        for (AlertNotification oldNotification : alertDef.getAlertNotifications()) {
            AlertNotification newNotification = oldNotification.copy();
            newNotification.setAlertDefinition(this);
            copiedNotifications.add(newNotification);
        }

        removeAllAlertNotifications();
        getAlertNotifications().addAll(copiedNotifications);

        this.operationDefinition = alertDef.operationDefinition;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCtime() {
        return this.ctime;
    }

    @PrePersist
    void onPersist() {
        this.mtime = this.ctime = System.currentTimeMillis();
    }

    public long getMtime() {
        return this.mtime;
    }

    @PreUpdate
    void onUpdate() {
        this.mtime = System.currentTimeMillis();
    }

    public Integer getParentId() {
        return this.parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
        resource.getAlertDefinitions().add(this);
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AlertPriority getPriority() {
        return this.priority;
    }

    public void setPriority(AlertPriority priority) {
        this.priority = priority;
    }

    public boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public AlertDampening getAlertDampening() {
        return alertDampening;
    }

    public void setAlertDampening(AlertDampening alertDampening) {
        this.alertDampening = alertDampening;
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

    public boolean getNotifyFiltered() {
        return this.notifyFiltered;
    }

    public void setNotifyFiltered(boolean notifyFiltered) {
        this.notifyFiltered = notifyFiltered;
    }

    public boolean getControlFiltered() {
        return this.controlFiltered;
    }

    public void setControlFiltered(boolean controlFiltered) {
        this.controlFiltered = controlFiltered;
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

    public boolean getDeleted() {
        return this.deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public BooleanExpression getConditionExpression() {
        return this.conditionExpression;
    }

    public void setConditionExpression(BooleanExpression conditionExpression) {
        this.conditionExpression = conditionExpression;
    }

    public Set<AlertCondition> getConditions() {
        return this.conditions;
    }

    public void setConditions(Set<AlertCondition> conditions) {
        this.conditions.clear();
        for (AlertCondition condition : conditions) {
            addCondition(condition);
        }
    }

    public void addCondition(AlertCondition condition) {
        this.conditions.add(condition);
        condition.setAlertDefinition(this);
    }

    public void removeAllConditions() {
        for (AlertCondition condition : this.conditions) {
            condition.setAlertDefinition(null);
        }

        this.conditions.clear();
    }

    public Set<Alert> getAlerts() {
        return this.alerts;
    }

    public void addAlert(Alert alert) {
        this.alerts.add(alert);
    }

    public Set<AlertNotification> getAlertNotifications() {
        return alertNotifications;
    }

    public void setAlertNotifications(Set<AlertNotification> alertNotifications) {
        this.alertNotifications = alertNotifications;
    }

    public void addAlertNotification(AlertNotification alertNotification) {
        this.alertNotifications.add(alertNotification);
    }

    public void removeAllAlertNotifications() {
        this.alertNotifications.clear();
    }

    public OperationDefinition getOperationDefinition() {
        return operationDefinition;
    }

    public void setOperationDefinition(OperationDefinition operationDefinition) {
        this.operationDefinition = operationDefinition;
    }

    public Set<AlertDampeningEvent> getAlertDampeningEvents() {
        return alertDampeningEvents;
    }

    public boolean removeAlertDampeningEvent(AlertDampeningEvent event) {
        return alertDampeningEvents.remove(event);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof AlertDefinition)) {
            return false;
        }

        AlertDefinition that = (AlertDefinition) obj;
        if (id != that.id) {
            return false;
        }

        if ((name != null) ? (!name.equals(that.name)) : (that.name != null)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = (31 * result) + ((name != null) ? name.hashCode() : 0);
        return result;
    }

    public String toSimpleString() {
        return "AlertDefinition" + "[ " + "id=" + id + ", " + "name=" + name + " ]";
    }

    @Override
    public String toString() {
        return "org.rhq.core.domain.alert.AlertDefinition" + "[ " + "id=" + id + ", " + "name=" + name + ", "
            + "conditionExpression=" + conditionExpression + ", " + "priority=" + priority + ", "
            + ((resource != null) ? ("resourceId=" + resource.getId()) : "")
            + ((resourceType != null) ? ("resourceTypeId=" + resourceType.getId()) : "") + " ]";
    }
}