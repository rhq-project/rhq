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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
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
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;

/**
 * @author Joseph Marques
 */
@Entity
@NamedQueries({ //
    @NamedQuery(name = AlertDefinition.QUERY_FIND_ALL, query = "" //
        + "SELECT a " //
        + "  FROM AlertDefinition a " //
        + " WHERE a.deleted = false " //
        + "   AND a.resource IS NOT NULL"),
    @NamedQuery(name = AlertDefinition.QUERY_FIND_ALL_BY_RECOVERY_DEFINITION_ID, query = "" //
        + "SELECT a " //
        + "  FROM AlertDefinition a " //
        + "  LEFT JOIN FETCH a.conditions " //
        + " WHERE a.deleted = false AND a.enabled = true " //
        + "   AND a.recoveryId = :recoveryDefinitionId"),
    @NamedQuery(name = AlertDefinition.QUERY_FIND_BY_ALERT_TEMPLATE_ID, query = "" //
        + "SELECT a.id " //
        + "  FROM AlertDefinition a " //
        + " WHERE a.parentId = :alertTemplateId " //
        + "   AND a.deleted = false" //
        + "   AND a.readOnly = false"), //
    @NamedQuery(name = AlertDefinition.QUERY_UPDATE_DETACH_PROTECTED_BY_ALERT_TEMPLATE_ID, query = "" //
        + "UPDATE AlertDefinition a " //
        + "   SET a.parentId = 0, a.readOnly = false " //
        + " WHERE a.parentId = :alertTemplateId " //
        + "   AND a.readOnly = true"), //
    @NamedQuery(name = AlertDefinition.QUERY_FIND_BY_GROUP_ALERT_DEFINITION_ID, query = "" //
        + "SELECT a.id " //
        + "  FROM AlertDefinition a " //
        + " WHERE a.groupAlertDefinition.id = :groupAlertDefinitionId " //
        + "   AND a.deleted = false" //
        + "   AND a.readOnly = false"),
    @NamedQuery(name = AlertDefinition.QUERY_UPDATE_DETACH_PROTECTED_BY_GROUP_ALERT_DEFINITION_ID, query = "" //
        + "UPDATE AlertDefinition a " //
        + "   SET a.groupAlertDefinition = null, a.readOnly = false " //
        + " WHERE a.groupAlertDefinition.id = :groupAlertDefinitionId " //
        + "   AND a.readOnly = true"), //
    @NamedQuery(name = AlertDefinition.QUERY_FIND_RESOURCE_IDS_NEEDING_TEMPLATE_APPLICATION, query = "" //
        + "SELECT res.id " //
        + "  FROM Resource res " //
        + " WHERE res.resourceType.id = :resourceTypeId " //
        + "   AND res.inventoryStatus = :inventoryStatus " //
        + "   AND NOT EXISTS ( SELECT ad.id " //
        + "                      FROM AlertDefinition ad " //
        + "                     WHERE ad.parentId = :alertTemplateId " // find the definitions for this template
        + "                       AND ad.resource.id = res.id " // correlated to the resource
        + "                       AND ad.deleted = false ) "), // and not deleted
    @NamedQuery(name = AlertDefinition.QUERY_FIND_OPTION_ITEMS_BY_RESOURCE, query = "" //
        + "  SELECT new org.rhq.core.domain.common.composite.IntegerOptionItem(ad.id, ad.name) " //
        + "    FROM AlertDefinition ad " //
        + "   WHERE ad.resource.id = :resourceId " //
        + "     AND ad.deleted = false"), //
    @NamedQuery(name = AlertDefinition.QUERY_FIND_OPTION_ITEMS_BY_GROUP, query = "" //
        + "  SELECT new org.rhq.core.domain.common.composite.IntegerOptionItem(ad.id, ad.name) " //
        + "    FROM AlertDefinition ad " //
        + "   WHERE ad.group.id = :groupId " //
        + "     AND ad.deleted = false"), //
    @NamedQuery(name = AlertDefinition.QUERY_FIND_BY_RESOURCE, query = "" //
        + "SELECT a " //
        + "  FROM AlertDefinition a " //
        + " WHERE a.resource.id = :id " //
        + "   AND a.deleted = false"), //
    @NamedQuery(name = AlertDefinition.QUERY_FIND_BY_RESOURCE_TYPE, query = "" //
        + "SELECT a " //
        + "  FROM AlertDefinition a " //
        + " WHERE a.resourceType.id = :typeId " //
        + "   AND a.deleted = false"), //
    @NamedQuery(name = AlertDefinition.QUERY_FIND_BY_RESOURCE_GROUP, query = "" //
        + "SELECT a " //
        + "  FROM AlertDefinition a " //
        + " WHERE a.group.id = :groupId " //
        + "   AND a.deleted = false"),
    @NamedQuery(name = AlertDefinition.QUERY_DELETE_BY_RESOURCES, query = "" //
        + "DELETE FROM AlertDefinition ad " //
        + " WHERE ad.resource.id IN ( :resourceIds ) "), //
    @NamedQuery(name = AlertDefinition.QUERY_FIND_UNUSED_DEFINITION_IDS, query = "" //
        + "SELECT ad.id " //
        + "  FROM AlertDefinition ad " //
        + " WHERE ad.deleted = TRUE " //
        + "   AND ad.id NOT IN ( SELECT alertDef.id " //
        + "                        FROM Alert a " //
        + "                        JOIN a.alertDefinition alertDef )"),
    @NamedQuery(name = AlertDefinition.QUERY_FIND_ALL_COMPOSITES_ADMIN, query = "" //
        + "   SELECT new org.rhq.core.domain.alert.composite.AlertDefinitionComposite" //
        + "        ( ad, parent.id, parent.name ) " //
        + "     FROM AlertDefinition ad " //
        + "     JOIN ad.resource res " //
        + "LEFT JOIN res.parentResource parent " //
        /*
         * as much as i want to (for efficiency of the query [namely roundtrips to the db]) i can't use fetching here
         * because, when added, the query parser chokes with "query specified join fetching, but the owner of the
         * fetched association was not present in the select list"...even though it clearly is  ;/
         */
        //+ "     JOIN FETCH ad.conditions ac " //
        + "    WHERE ad.deleted = false " //
        + "      AND (UPPER(res.name) LIKE :resourceFilter ESCAPE :escapeChar OR :resourceFilter IS NULL) " //
        + "      AND (UPPER(parent.name) LIKE :parentFilter ESCAPE :escapeChar OR :parentFilter IS NULL) " //
        + "      AND (ad.ctime > :startTime OR :startTime IS NULL) " //
        + "      AND (ad.ctime < :endTime OR :endTime IS NULL) " //
        + "      AND (ad.id IN ( SELECT aad.id FROM AlertDefinition aad " //
        + "                       JOIN aad.conditions aadc " //
        + "                      WHERE aadc.category = :category ) " //
        + "           OR :category IS NULL) "), //
    @NamedQuery(name = AlertDefinition.QUERY_FIND_ALL_COMPOSITES, query = "" //
        + "   SELECT new org.rhq.core.domain.alert.composite.AlertDefinitionComposite" //
        + "        ( ad, parent.id, parent.name ) " //
        + "     FROM AlertDefinition ad " //
        + "     JOIN ad.resource res " //
        + "LEFT JOIN res.parentResource parent " //
        /*
         * as much as i want to (for efficiency of the query [namely roundtrips to the db]) i can't use fetching here
         * because, when added, the query parser chokes with "query specified join fetching, but the owner of the
         * fetched association was not present in the select list"...even though it clearly is  ;/
         */
        //+ "     JOIN FETCH ad.conditions ac " //
        + "    WHERE ad.deleted = false " //
        + "      AND res.id IN ( SELECT rr.id FROM Resource rr " //
        + "                        JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s " //
        + "                       WHERE s.id = :subjectId ) " //
        + "      AND (UPPER(res.name) LIKE :resourceFilter ESCAPE :escapeChar OR :resourceFilter IS NULL) " //
        + "      AND (UPPER(parent.name) LIKE :parentFilter ESCAPE :escapeChar OR :parentFilter IS NULL) " //
        + "      AND (ad.ctime > :startTime OR :startTime IS NULL) " //
        + "      AND (ad.ctime < :endTime OR :endTime IS NULL) " //
        + "      AND (ad.id IN ( SELECT aad.id FROM AlertDefinition aad " //
        + "                       JOIN aad.conditions aadc " //
        + "                      WHERE aadc.category = :category ) " //
        + "           OR :category IS NULL) "), //
    @NamedQuery(name = AlertDefinition.QUERY_FIND_DEFINITION_ID_BY_CONDITION_ID, query = "" //
        + "SELECT ad.id " //
        + "  FROM AlertCondition ac " //
        + "  JOIN ac.alertDefinition ad" //
        + " WHERE ac.id = :alertConditionId " //
        + "   AND ad.enabled = true "), //
    @NamedQuery(name = AlertDefinition.QUERY_IS_ENABLED, query = "" //
        + "SELECT ad.id " //
        + "  FROM AlertDefinition ad " //
        + " WHERE ad.id = :alertDefinitionId " //
        + "   AND ad.enabled = true "), //
    @NamedQuery(name = AlertDefinition.QUERY_IS_TEMPLATE, query = "" //
        + "SELECT ad.id " //
        + "  FROM AlertDefinition ad " //
        + " WHERE ad.id = :alertDefinitionId " //
        + "   AND ad.resourceType IS NOT NULL "), //
    @NamedQuery(name = AlertDefinition.QUERY_IS_GROUP_ALERT_DEFINITION, query = "" //
        + "SELECT ad.id " //
        + "  FROM AlertDefinition ad " //
        + " WHERE ad.id = :alertDefinitionId " //
        + "   AND ad.group IS NOT NULL "), //
    @NamedQuery(name = AlertDefinition.QUERY_IS_RESOURCE_ALERT_DEFINITION, query = "" //
        + "SELECT ad.id " //
        + "  FROM AlertDefinition ad " //
        + " WHERE ad.id = :alertDefinitionId " //
        + "   AND ad.resource IS NOT NULL "), //
    @NamedQuery(name = AlertDefinition.QUERY_UPDATE_SET_PARENTS_NULL, query = "" //
        + "UPDATE AlertDefinition ad " //
        + "   SET ad.groupAlertDefinition = NULL " //
        + " WHERE ad.id IN ( :childrenDefinitionIds ) "),
    @NamedQuery(name = AlertDefinition.QUERY_FIND_RESOURCE_IDS_NEEDING_GROUP_APPLICATION, query = "" //
        + "SELECT res.id " //
        + "  FROM Resource res " //
        + "  JOIN res.explicitGroups rg " //
        + " WHERE rg.id = :resourceGroupId " //
        + "   AND res.inventoryStatus = :inventoryStatus " //
        + "   AND NOT EXISTS ( SELECT ad.id " //
        + "                      FROM AlertDefinition ad " //
        + "                     WHERE ad.groupAlertDefinition.id = :groupAlertDefinitionId " // find the children for this group alert def
        + "                       AND ad.resource.id = res.id " // correlated to the resource
        + "                       AND ad.deleted = false ) ") // and not deleted
})
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_ALERT_DEFINITION_ID_SEQ", sequenceName = "RHQ_ALERT_DEFINITION_ID_SEQ")
@Table(name = "RHQ_ALERT_DEFINITION")
@XmlAccessorType(XmlAccessType.FIELD)
public class AlertDefinition implements Serializable {
    private static final long serialVersionUID = 2L;

    public static final String QUERY_FIND_ALL = "AlertDefinition.findAll";
    public static final String QUERY_FIND_ALL_BY_RECOVERY_DEFINITION_ID = "AlertDefinition.findAllByRecoveryDefinitionId";
    public static final String QUERY_FIND_BY_ALERT_TEMPLATE_ID = "AlertDefinition.findByAlertTemplateId";
    public static final String QUERY_FIND_BY_GROUP_ALERT_DEFINITION_ID = "AlertDefinition.findByGroupAlertDefinitionId";
    public static final String QUERY_FIND_RESOURCE_IDS_NEEDING_TEMPLATE_APPLICATION = "AlertDefinition.findResourceIdsNeedingTemplateApplication";
    public static final String QUERY_FIND_OPTION_ITEMS_BY_RESOURCE = "AlertDefinition.findOptionItemsByResource";
    public static final String QUERY_FIND_OPTION_ITEMS_BY_GROUP = "AlertDefinition.findOptionItemsByGroup";
    public static final String QUERY_FIND_BY_RESOURCE = "AlertDefinition.findByResource";
    public static final String QUERY_FIND_BY_RESOURCE_TYPE = "AlertDefinition.findByResourceType";
    public static final String QUERY_FIND_BY_RESOURCE_GROUP = "AlertDefinition.findByResourceGroup";
    public static final String QUERY_DELETE_BY_RESOURCES = "AlertDefinition.deleteByResources";
    public static final String QUERY_FIND_UNUSED_DEFINITION_IDS = "AlertDefinition.findUnusedDefinitionIds";
    public static final String QUERY_FIND_DEFINITION_ID_BY_CONDITION_ID = "AlertDefinition.findDefinitionIdByConditionId";
    public static final String QUERY_IS_ENABLED = "AlertDefinition.isEnabled";
    public static final String QUERY_IS_TEMPLATE = "AlertDefinition.isTemplate";
    public static final String QUERY_IS_GROUP_ALERT_DEFINITION = "AlertDefinition.isGroupAlertDefinition";
    public static final String QUERY_IS_RESOURCE_ALERT_DEFINITION = "AlertDefinition.isResourceAlertDefinition";

    // group/template alert definitions
    public static final String QUERY_UPDATE_SET_PARENTS_NULL = "AlertDefinition.updateSetParentsNull";
    public static final String QUERY_UPDATE_DETACH_PROTECTED_BY_GROUP_ALERT_DEFINITION_ID = "AlertDefinition.updateDetachProtectedGroupAlertDefs";
    public static final String QUERY_UPDATE_DETACH_PROTECTED_BY_ALERT_TEMPLATE_ID = "AlertDefinition.updateDetachProtectedAlertTemplateDefs";
    public static final String QUERY_FIND_RESOURCE_IDS_NEEDING_GROUP_APPLICATION = "AlertDefinition.findResourceIdsNeedingGroupApplication";

    // for subsystem view
    public static final String QUERY_FIND_ALL_COMPOSITES = "AlertDefinition.findAllComposites";
    public static final String QUERY_FIND_ALL_COMPOSITES_ADMIN = "AlertDefinition.findAllComposites_admin";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_ALERT_DEFINITION_ID_SEQ")
    @Id
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

    @JoinColumn(name = "GROUP_ALERT_DEF_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private AlertDefinition groupAlertDefinition;

    // do not cascade remove - group removal will be detaching children alert defs from the group def,
    // and then letting the children be deleted slowly by existing alert def removal mechanisms
    @OneToMany(mappedBy = "groupAlertDefinition", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST })
    @OrderBy
    private Set<AlertDefinition> groupAlertDefinitionChildren = new LinkedHashSet<AlertDefinition>();

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
    @XmlTransient
    private Resource resource;

    @JoinColumn(name = "RESOURCE_GROUP_ID", nullable = true)
    @ManyToOne
    @XmlTransient
    private ResourceGroup group;

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

    @Column(name = "READ_ONLY", nullable = false)
    private boolean readOnly;

    @Column(name = "REQUIRED", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private BooleanExpression conditionExpression;

    @OneToMany(mappedBy = "alertDefinition", cascade = CascadeType.ALL)
    @OrderBy
    // We used to use DELETE_ORPHAN here. But the problem is that the the AlertCondition record is needed by the
    // associated AlertConditionLog for presentation purposes. So, keep the AlertCondition records even if they
    // are detached from the AlertDefinition (e.g. when the def is updated, see update() below).  They will be
    // cleaned up in the DataPurge job after all relevant alerts have been removed and there are no longer any
    // referencing AlertConditionLog records.
    private Set<AlertCondition> conditions = new LinkedHashSet<AlertCondition>(1); // Most alerts will only have one condition.

    @OneToMany(mappedBy = "alertDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    // Although similar to AlertCondition, we do use DELETE_ORPHAN here.  The reason is because AlertNotificationLog
    // does not refer back to the AlertNotification record and therefore the notification logs are not affected
    // by the loss of the AlertNotification that spawned the notification.
    private List<AlertNotification> alertNotifications = new ArrayList<AlertNotification>();

    /**
     * NOTE: This field is currently not accessed, so we don't need cascading for PERSIST/MERGE/REFRESH. However, we
     * still need cascading for REMOVE, so we don't get FK constraint violations when an Alert is removed.
     * AlertDefinitions aren't actually deleted, instead they have their 'deleted' flag set to true. But, if they were
     * actually deleted from persistent storage, we would still want the cascade remove.
     */
    @OneToMany(mappedBy = "alertDefinition", cascade = CascadeType.REMOVE)
    @OrderBy
    // primary key
    @XmlTransient
    private Set<Alert> alerts = new LinkedHashSet<Alert>();

    private transient AlertDefinitionContext context;

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
        update(alertDef, true);
    }

    /**
     * @param alertDef
     * @param updateConditions set to true if conditions are being updated.  This incurs overhead because the
     * old AlertConditions get detached and replaced for no good reason, becoming a cleanup burden later on. Only
     * set to false if you are sure the conditions are not being updated.  If false the conditions will not be updated.
     */
    public void update(AlertDefinition alertDef, boolean updateConditions) {
        /*
         * Don't copy the id, ctime, or mtime.
         */
        setName(alertDef.name);
        this.description = alertDef.description;
        this.priority = alertDef.priority;
        this.enabled = alertDef.enabled;
        this.readOnly = alertDef.readOnly;

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

        // copy conditions if necessary
        if (updateConditions) {
            List<AlertCondition> copiedConditions = new ArrayList<AlertCondition>();
            for (AlertCondition oldCondition : alertDef.getConditions()) {
                AlertCondition newCondition = new AlertCondition(oldCondition);
                newCondition.setAlertDefinition(this);
                copiedConditions.add(newCondition);
            }
            this.removeAllConditions();
            this.getConditions().addAll(copiedConditions);
        }

        // copy notifications
        List<AlertNotification> copiedNotifications = new ArrayList<AlertNotification>();
        for (AlertNotification oldNotification : alertDef.getAlertNotifications()) {
            AlertNotification newNotification = new AlertNotification(oldNotification);
            newNotification.setAlertDefinition(this);
            copiedNotifications.add(newNotification);
        }
        this.removeAllAlertNotifications();
        this.getAlertNotifications().addAll(copiedNotifications);
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
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

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }

    @PrePersist
    void onPersist() {
        this.mtime = this.ctime = System.currentTimeMillis();
    }

    public long getMtime() {
        return this.mtime;
    }

    public void setMtime(long mtime) {
        this.mtime = mtime;
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

    public AlertDefinition getGroupAlertDefinition() {
        return groupAlertDefinition;
    }

    public void setGroupAlertDefinition(AlertDefinition groupAlertDefinition) {
        this.groupAlertDefinition = groupAlertDefinition;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
        if (this.resource != null) {
            Set<AlertDefinition> alertDefinitions = this.resource.getAlertDefinitions();
            if (alertDefinitions.equals(Collections.emptySet())) {
                alertDefinitions = new HashSet<AlertDefinition>(1);
                alertDefinitions.add(this);
                this.resource.setAlertDefinitions(alertDefinitions);
            } else {
                alertDefinitions.add(this);
            }
        }
    }

    /**
     * @return the group
     * @deprecated use getGroup()
     */
    @Deprecated
    public ResourceGroup getResourceGroup() {
        return group;
    }

    /**
     * @param resourceGroup
     * @deprecated use setGroup(ResourceGroup)
     */
    @Deprecated
    public void setResourceGroup(ResourceGroup resourceGroup) {
        this.group = resourceGroup;
    }

    public ResourceGroup getGroup() {
        return group;
    }

    public void setGroup(ResourceGroup group) {
        this.group = group;
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

    /**
     * A definition is "read-only" with respect to updates that come from the group/template level.
     * If "read only" is true, then changes to the parent group/template alert definition will not
     * change this resource alert def. If read only is false, changes to the parent propagate to the
     * child resource alert.
     *
     * @return read only flag
     */
    public boolean isReadOnly() {
        return this.readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
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
        removeAllConditions();
        if (conditions != null) {
            for (AlertCondition condition : conditions) {
                addCondition(condition);
            }
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

    public List<AlertNotification> getAlertNotifications() {
        return alertNotifications;
    }

    public void setAlertNotifications(List<AlertNotification> alertNotifications) {
        if (alertNotifications == null) {
            alertNotifications = new ArrayList<AlertNotification>();
        }
        this.alertNotifications = alertNotifications;
    }

    public void addAlertNotification(AlertNotification alertNotification) {
        this.alertNotifications.add(alertNotification);
    }

    public void removeAllAlertNotifications() {
        for (AlertNotification notification : this.alertNotifications) {
            notification.prepareForOrphanDelete();
        }

        this.alertNotifications.clear();
    }

    public Set<AlertDampeningEvent> getAlertDampeningEvents() {
        return alertDampening.getAlertDampeningEvents();
    }

    public boolean removeAlertDampeningEvent(AlertDampeningEvent event) {
        return alertDampening.getAlertDampeningEvents().remove(event);
    }

    public void calculateContext() {
        context = AlertDefinitionContext.get(this);
    }

    public void setContext(AlertDefinitionContext context) {
        this.context = context;
    }

    public AlertDefinitionContext getContext() {
        if (context == null) {
            calculateContext();
        }
        return context;
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