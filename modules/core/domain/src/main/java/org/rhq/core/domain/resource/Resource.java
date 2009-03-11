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
package org.rhq.core.domain.resource;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.PostPersist;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.QueryHint;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.content.Channel;
import org.rhq.core.domain.content.ContentServiceRequest;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.content.ResourceChannel;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.group.ResourceGroup;

/**
 * Represents a JON managed resource (i.e. a platform, server, or service).
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = Resource.QUERY_FIND_PROBLEM_RESOURCES_ALERT_ADMIN, query = "" //
        + "  SELECT DISTINCT new org.rhq.core.domain.resource.composite.ProblemResourceComposite" + "         ( "
        + "         res.id, res.name, avail.availabilityType, COUNT(DISTINCT alert.id)"
        + "         ) " + "    FROM Resource res " + "         LEFT JOIN res.availability avail " + "         LEFT JOIN res.alertDefinitions alertDef "
        + "         LEFT JOIN alertDef.alerts alert  "
        + "   WHERE avail.endTime IS NULL " + "     AND res.inventoryStatus = 'COMMITTED' "
        + "     AND (( avail.availabilityType = 0 AND avail.startTime >= :oldest) "
        + "          OR (alert.ctime >= :oldest)) " + "GROUP BY res.id, res.name, avail.availabilityType "),
    @NamedQuery(name = Resource.QUERY_FIND_PROBLEM_RESOURCES_ALERT, query = "" //
        + "  SELECT DISTINCT new org.rhq.core.domain.resource.composite.ProblemResourceComposite" + "         ( "
        + "         res.id, res.name, avail.availabilityType, COUNT(DISTINCT alert.id)"
        + "         ) " + "    FROM Resource res " + "         LEFT JOIN res.availability avail " + "         LEFT JOIN res.alertDefinitions alertDef "
        + "         LEFT JOIN alertDef.alerts alert  "
        + "   WHERE avail.endTime IS NULL "
        + "     AND res.id IN (SELECT rr.id FROM Resource rr JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s WHERE s = :subject)"
        + "     AND res.inventoryStatus = 'COMMITTED' " + "     AND (( avail.availabilityType = 0 AND avail.startTime >= :oldest) "
        + "          OR (alert.ctime >= :oldest)) "
        + "GROUP BY res.id, res.name, avail.availabilityType "),
    @NamedQuery(name = Resource.QUERY_FIND_PROBLEM_RESOURCES_ALERT_COUNT_ADMIN, query = "" //
        + "  SELECT COUNT( DISTINCT res.id ) " + "    FROM Resource res " + "         LEFT JOIN res.availability avail "
        + "         LEFT JOIN res.alertDefinitions alertDef "
        + "         LEFT JOIN alertDef.alerts alert  " + "   WHERE avail.endTime IS NULL "
        + "     AND res.inventoryStatus = 'COMMITTED' "
        + "     AND (( avail.availabilityType = 0 AND avail.startTime >= :oldest) " + "          OR (alert.ctime >= :oldest)) "),
    @NamedQuery(name = Resource.QUERY_FIND_PROBLEM_RESOURCES_ALERT_COUNT, query = "" //
        + "  SELECT COUNT( DISTINCT res.id ) " + "    FROM Resource res " //
        + "         LEFT JOIN res.availability avail " //
        + "         LEFT JOIN res.alertDefinitions alertDef " //
        + "         LEFT JOIN alertDef.alerts alert  " //
        + "   WHERE avail.endTime IS NULL " //
        + "     AND res.id IN (SELECT rr.id FROM Resource rr JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s WHERE s = :subject)" + "     AND res.inventoryStatus = 'COMMITTED' " //
        + "     AND (( avail.availabilityType = 0 AND avail.startTime >= :oldest) " //
        + "          OR (alert.ctime >= :oldest)) "),

    /* the following three are for auto-group details */
    @NamedQuery(name = Resource.QUERY_FIND_BY_PARENT_AND_TYPE, query = "" //
        + "  SELECT res, a.availabilityType " + "    FROM Resource res " //
        + "    JOIN res.currentAvailability a " + "   WHERE res.parentResource = :parent " //
        + "     AND res.id IN (SELECT rr.id FROM Resource rr JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s WHERE s = :subject)" + "     AND res.resourceType = :type " //
        + "     AND res.inventoryStatus = :inventoryStatus " //
        + "ORDER BY res.name"),
    @NamedQuery(name = Resource.QUERY_FIND_BY_PARENT_AND_TYPE_ADMIN, query = "" //
        + "  SELECT res, a.availabilityType " + "    FROM Resource res " //
        + "    JOIN res.currentAvailability a " //
        + "   WHERE res.parentResource = :parent " //
        + "     AND res.resourceType = :type " //
        + "     AND res.inventoryStatus = :inventoryStatus " //
        + "ORDER BY res.name"),
    @NamedQuery(name = Resource.QUERY_FIND_FOR_AUTOGROUP, query = "" //
        + "SELECT res " //
        + "  FROM Resource res " //
        + " WHERE res.parentResource.id = :parent " //
        + "   AND res.resourceType.id = :type " //
        + "   AND res.inventoryStatus = :inventoryStatus "),
    @NamedQuery(name = Resource.QUERY_FIND_BY_PARENT_AND_INVENTORY_STATUS, query = "" //
        + "SELECT res " + "  FROM Resource res " //
        + " WHERE res.parentResource = :parent " //
        + "   AND res.id IN (SELECT rr.id FROM Resource rr JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s WHERE s = :subject)" + "   AND res.inventoryStatus = :inventoryStatus "),
    @NamedQuery(name = Resource.QUERY_FIND_BY_PARENT_AND_INVENTORY_STATUS_ADMIN, query = "" //
        + "SELECT res " + "  FROM Resource res " //
        + " WHERE res.parentResource = :parent " //
        + "   AND res.inventoryStatus = :inventoryStatus "),
    @NamedQuery(name = Resource.QUERY_FIND_VALID_COMMITTED_RESOURCE_IDS_ADMIN, query = "" //
        + "SELECT res.id " + "  FROM Resource res " //
        + " WHERE res.inventoryStatus = 'COMMITTED' " //
        + "   AND res.id IN ( :resourceIds ) "),
    @NamedQuery(name = Resource.QUERY_FIND_CHILDREN_IDS_ADMIN, query = "" //
        + "SELECT res.id " //
        + "  FROM Resource res " //
        + " WHERE res.parentResource.id = :parentResourceId " //
        + "   AND (:inventoryStatus = res.inventoryStatus OR :inventoryStatus is null)"),
    @NamedQuery(name = Resource.QUERY_FIND_CHILDREN, query = "" //
        + "SELECT res " + "  FROM Resource res " //
        + " WHERE res.parentResource = :parent " //
        + "   AND res.id IN (SELECT rr.id FROM Resource rr JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s WHERE s = :subject)"),
    @NamedQuery(name = Resource.QUERY_FIND_CHILDREN_ADMIN, query = "" //
        + "SELECT res " //
        + "  FROM Resource res " //
        + " WHERE res.parentResource = :parent "),
    @NamedQuery(name = Resource.QUERY_RECENTLY_ADDED_PLATFORMS, query = "" //
        + "  SELECT DISTINCT new org.rhq.core.domain.resource.composite.RecentlyAddedResourceComposite(res.id, res.name, res.itime) "
        + "    FROM Resource res JOIN res.childResources child JOIN res.implicitGroups g JOIN g.roles r JOIN r.subjects s "
        + "   WHERE (res.itime >= :oldestEpochTime OR ((child.itime >= :oldestEpochTime) AND (child.inventoryStatus = 'COMMITTED'))) "
        + "     AND res.resourceType.category = 'PLATFORM' "
        + "     AND res.inventoryStatus = 'COMMITTED' " + "     AND s = :subject " + "ORDER BY res.itime DESC "),
    @NamedQuery(name = Resource.QUERY_RECENTLY_ADDED_PLATFORMS_ADMIN, query = "" //
        + "  SELECT DISTINCT new org.rhq.core.domain.resource.composite.RecentlyAddedResourceComposite(res.id, res.name, res.itime) "
        + "    FROM Resource res JOIN res.childResources child "
        + "   WHERE ((res.itime >= :oldestEpochTime) OR ((child.itime >= :oldestEpochTime) AND (child.inventoryStatus = 'COMMITTED'))) "
        + "     AND res.resourceType.category = 'PLATFORM' "
        + "     AND res.inventoryStatus = 'COMMITTED' " + "ORDER BY res.itime DESC "),
    @NamedQuery(name = Resource.QUERY_RECENTLY_ADDED_SERVERS, query = "" + "  SELECT DISTINCT new org.rhq.core.domain.resource.composite.RecentlyAddedResourceComposite(res.id, res.name, res.itime) "
        + "    FROM Resource res JOIN res.implicitGroups g JOIN g.roles r JOIN r.subjects s " + "   WHERE res.itime >= :oldestEpochTime " + "     AND res.resourceType.category = 'SERVER' "
        + "     AND res.inventoryStatus = 'COMMITTED' " + "     AND res.parentResource.id = :platformId " + "     AND s = :subject " + "ORDER BY res.itime DESC "),
    @NamedQuery(name = Resource.QUERY_RECENTLY_ADDED_SERVERS_ADMIN, query = ""
        + "  SELECT DISTINCT new org.rhq.core.domain.resource.composite.RecentlyAddedResourceComposite(res.id, res.name, res.itime) " + "    FROM Resource res "
        + "   WHERE res.itime >= :oldestEpochTime " + "     AND res.resourceType.category = 'SERVER' " + "     AND res.inventoryStatus = 'COMMITTED' "
        + "     AND res.parentResource.id = :platformId " + "ORDER BY res.itime DESC "),
    @NamedQuery(name = Resource.QUERY_FIND_AVAILABILITY_BY_RESOURCE_ID, query = "" //
        + "SELECT new org.rhq.core.domain.resource.composite.ResourceWithAvailability(res, a.availabilityType) " + "  FROM Resource res " //
        + "  JOIN res.currentAvailability a " //
        + " WHERE res.id IN (SELECT rr.id FROM Resource rr JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s WHERE s = :subject)" + "   AND res.id = :id "),
    @NamedQuery(name = Resource.QUERY_FIND_AVAILABILITY_BY_RESOURCE_ID_ADMIN, query = "" //
        + "SELECT new org.rhq.core.domain.resource.composite.ResourceWithAvailability(res, a.availabilityType) " + "  FROM Resource res JOIN res.currentAvailability a " //
        + " WHERE res.id = :id "),
    @NamedQuery(name = Resource.QUERY_FIND_AVAILABILITY_BY_RESOURCE_IDS, query = "" //
        + "SELECT res, a.availabilityType " + "  FROM Resource res " //
        + "  LEFT OUTER JOIN res.availability a " //
        + " WHERE (a is null OR a.startTime = (SELECT MAX(aa.startTime) FROM Availability aa where res.id = aa.resource.id)) " //
        + "   AND res.id IN (:ids) " //
        + "   AND res.id IN (SELECT rr.id FROM Resource rr JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s WHERE s = :subject)" + "ORDER BY res.name "),
    @NamedQuery(name = Resource.QUERY_FIND_AVAILABILITY_BY_RESOURCE_IDS_ADMIN, query = "" //
        + "SELECT res, a.availabilityType " + "  FROM Resource res " //
        + "  LEFT OUTER JOIN res.availability a " //
        + " WHERE (a is null OR a.startTime = (SELECT MAX(aa.startTime) FROM Availability aa where res.id = aa.resource.id)) " //
        + "   AND res.id IN (:ids) " //
        + "ORDER BY res.name "),
    @NamedQuery(name = Resource.QUERY_FIND_RESOURCE_AUTOGROUP_COMPOSITE, query = ""
        + "  SELECT new org.rhq.core.domain.resource.group.composite.AutoGroupComposite(avg(a.availabilityType), res.parentResource, rt, count(res)) " + "    FROM Resource res "
        + "    JOIN res.resourceType rt LEFT JOIN rt.subCategory JOIN res.currentAvailability a " + "   WHERE res.id = :id "
        + "     AND res.id IN (SELECT rr.id FROM Resource rr JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s WHERE s = :subject)" + "GROUP BY res.parentResource, rt "),
    @NamedQuery(name = Resource.QUERY_FIND_RESOURCE_AUTOGROUPS_COMPOSITE, query = "" //
        + "  SELECT new org.rhq.core.domain.resource.group.composite.AutoGroupComposite(avg(a.availabilityType), res.parentResource, rt, count(res)) "
        + "    FROM Resource res "
        + "    JOIN res.resourceType rt  JOIN res.currentAvailability a "
        + "   WHERE res.id IN ( :ids ) "
        + "     AND res.id IN (SELECT rr.id FROM Resource rr JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s WHERE s = :subject)" + "GROUP BY res.parentResource, rt "),
    @NamedQuery(name = Resource.QUERY_FIND_RESOURCE_AUTOGROUP_COMPOSITE_ADMIN, query = "" //
        + "  SELECT new org.rhq.core.domain.resource.group.composite.AutoGroupComposite(avg(a.availabilityType), res.parentResource, rt, count(res)) "
        + "    FROM Resource res JOIN res.currentAvailability a JOIN res.resourceType rt LEFT JOIN rt.subCategory " + "   WHERE res.id = :id " + "GROUP BY res.parentResource, rt"),
    @NamedQuery(name = Resource.QUERY_FIND_RESOURCE_AUTOGROUPS_COMPOSITE_ADMIN, query = "" //
        + "  SELECT new org.rhq.core.domain.resource.group.composite.AutoGroupComposite(avg(a.availabilityType), res.parentResource, rt, count(res)) "
        + "    FROM Resource res JOIN res.currentAvailability a JOIN res.resourceType rt LEFT JOIN rt.subCategory " + "   WHERE res.id IN ( :ids ) " + "GROUP BY res.parentResource, rt"),
    @NamedQuery(name = Resource.QUERY_FIND_CHILDREN_AUTOGROUP_COMPOSITES, query = ""
        + "  SELECT new org.rhq.core.domain.resource.group.composite.AutoGroupComposite(avg(a.availabilityType), res.parentResource, rt, count(res)) " + "    FROM Resource res "
        + "    JOIN res.resourceType rt LEFT JOIN rt.subCategory JOIN res.currentAvailability a " + "   WHERE res.parentResource = :parent " //
        + "     AND res.id IN (SELECT rr.id FROM Resource rr JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s WHERE s = :subject)" + "     AND res.inventoryStatus = :inventoryStatus " //
        + "GROUP BY res.parentResource, rt "),
    @NamedQuery(name = Resource.QUERY_FIND_CHILDREN_AUTOGROUP_COMPOSITES_ADMIN, query = "" //
        + "   SELECT new org.rhq.core.domain.resource.group.composite.AutoGroupComposite(avg(a.availabilityType), res.parentResource, rt, count(res)) " + "     FROM Resource res " //
        + "     JOIN res.currentAvailability a " //
        + "     JOIN res.resourceType rt LEFT JOIN rt.subCategory " //
        + "    WHERE res.parentResource = :parent " //
        + "      AND res.inventoryStatus = :inventoryStatus GROUP BY res.parentResource, rt"),
    @NamedQuery(name = Resource.QUERY_FIND_CHILDREN_AUTOGROUP_COMPOSITES_BY_TYPE, query = ""
        + "  SELECT new org.rhq.core.domain.resource.group.composite.AutoGroupComposite(avg(a.availabilityType), res.parentResource, rt, count(res)) " + "    FROM Resource res "
        + "    JOIN res.resourceType rt LEFT JOIN rt.subCategory JOIN res.currentAvailability a " + "   WHERE res.parentResource = :parent " //
        + "     AND rt.id IN ( :resourceTypeIds ) " //
        + "     AND res.id IN (SELECT rr.id FROM Resource rr JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s WHERE s = :subject)" + "     AND res.inventoryStatus = :inventoryStatus " //
        + "GROUP BY res.parentResource, rt "),
    @NamedQuery(name = Resource.QUERY_FIND_CHILDREN_AUTOGROUP_COMPOSITES_BY_TYPE_ADMIN, query = "" //
        + "   SELECT new org.rhq.core.domain.resource.group.composite.AutoGroupComposite(avg(a.availabilityType), res.parentResource, rt, count(res)) " + "     FROM Resource res " //
        + "     JOIN res.currentAvailability a " //
        + "     JOIN res.resourceType rt LEFT JOIN rt.subCategory " //
        + "    WHERE res.parentResource = :parent " //
        + "      AND rt.id IN ( :resourceTypeIds ) " //        
        + "      AND res.inventoryStatus = :inventoryStatus GROUP BY res.parentResource, rt"),
    @NamedQuery(name = Resource.QUERY_FIND_CHILDREN_BY_CATEGORY_AND_INVENTORY_STATUS, query = "" //
        + "SELECT res " //
        + "  FROM Resource res " //
        + " WHERE res.parentResource = :parent " //
        + "   AND res.id IN (SELECT rr.id FROM Resource rr JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s WHERE s = :subject)"
        + "   AND res.resourceType.category = :category "
        + "   AND res.inventoryStatus = :status "),
    @NamedQuery(name = Resource.QUERY_FIND_CHILDREN_BY_CATEGORY_AND_INVENTORY_STATUS_ADMIN, query = "" //
        + "SELECT res " //
        + "  FROM Resource res " //
        + " WHERE res.parentResource = :parent " //
        + "   AND res.inventoryStatus = :status " //
        + "   AND res.resourceType.category = :category "),
    @NamedQuery(name = Resource.QUERY_FIND_BY_CATEGORY_AND_INVENTORY_STATUS, query = "" //
        + "SELECT res " //
        + "  FROM Resource res " //
        + " WHERE res.resourceType.category = :category " //
        + "   AND res.id IN (SELECT rr.id FROM Resource rr JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s WHERE s = :subject)"
        + "   AND (:inventoryStatus = res.inventoryStatus OR :inventoryStatus is null) "),
    @NamedQuery(name = Resource.QUERY_FIND_BY_CATEGORY_AND_INVENTORY_STATUS_ADMIN, query = "" //
        + "SELECT res " //
        + "  FROM Resource res " //
        + " WHERE res.resourceType.category = :category " //
        + "   AND (:inventoryStatus = res.inventoryStatus OR :inventoryStatus is null) "),

    // Returns all platforms that is either of a given inventory status itself or
    // one of its top level servers have one of the inventory statuses (for auto-discovery queue).
    // Users will not be able to execute this unless they have global inventory perm,
    // so no need for an corresponding admin query.
    @NamedQuery(name = Resource.QUERY_FIND_QUEUED_PLATFORMS_BY_INVENTORY_STATUS, query = "" //
        + "SELECT res "//
        + "FROM Resource res " //
        + "WHERE " //
        + "   (( " //
        + "      res.id IN " //
        + "      (SELECT res1.id " //
        + "         FROM Resource res1 " //
        + "        WHERE res1.inventoryStatus IN (:inventoryStatuses) " //
        + "          AND res1.resourceType.category = 'PLATFORM' " //
        + "          AND res1.parentResource IS NULL) " //
        + "    ) " //
        + "    OR " //
        + "    ( " //
        + "      res.id IN " //
        + "      (SELECT res2.parentResource.id " //
        + "         FROM Resource res2 " //
        + "        WHERE res2.inventoryStatus IN (:inventoryStatuses) " //
        + "          AND res2.resourceType.category = 'SERVER' " //
        + "          AND res2.parentResource.resourceType.category = 'PLATFORM') " + "    )) "),
    @NamedQuery(name = Resource.QUERY_FIND_BY_TYPE, query = "" //
        + "SELECT res " //
        + "  FROM Resource res " //
        + " WHERE res.resourceType = :type " //
        + "   AND res.id IN (SELECT rr.id FROM Resource rr JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s WHERE s = :subject) "),
    @NamedQuery(name = Resource.QUERY_FIND_BY_TYPE_ADMIN, query = "" //
        + "SELECT res " //
        + "  FROM Resource res " //
        + " WHERE res.resourceType = :type "),
    @NamedQuery(name = Resource.QUERY_FIND_BY_TYPE_AND_IDS, query = "" //
        + "SELECT res " //
        + "  FROM Resource res " //
        + " WHERE res.resourceType = :type " //
        + "   AND res.id IN (SELECT rr.id FROM Resource rr JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s WHERE s = :subject)" + "   AND res.id IN ( :ids ) "),
    @NamedQuery(name = Resource.QUERY_FIND_BY_TYPE_AND_IDS_ADMIN, query = "" //
        + "SELECT res " //
        + "  FROM Resource res " //
        + " WHERE res.resourceType = :type " //
        + "   AND res.id IN ( :ids ) "),
    @NamedQuery(name = Resource.QUERY_FIND_BY_PARENT_AND_KEY, hints = { @QueryHint(name = "cacheable", value = "true") }, query = "" //
        + "SELECT r " //
        + "  FROM Resource AS r " //
        + " WHERE (:parent = r.parentResource OR :parent IS NULL) " //
        + "   AND r.resourceKey = :key " //
        + "   AND r.resourceType.plugin = :plugin " //
        + "   AND r.resourceType.name = :typeName"),
    @NamedQuery(name = Resource.QUERY_FIND_EXPLICIT_IDS_BY_RESOURCE_GROUP_ADMIN, query = "" //
        + "SELECT res.id " //
        + "  FROM ResourceGroup rg, IN (rg.explicitResources) res " //
        + " WHERE rg.id = :groupId "),
    @NamedQuery(name = Resource.QUERY_FIND_FLY_WEIGHTS_BY_RESOURCE_IDS, query = "" //
        + "SELECT new org.rhq.core.domain.resource.composite.ResourceIdFlyWeight(res.id, res.uuid) " //
        + "  FROM Resource res " //
        + " WHERE res.id IN ( :resourceIds ) "),
    @NamedQuery(name = Resource.QUERY_FIND_FLY_WEIGHTS_BY_PARENT_RESOURCE_ID, query = "" //
        + "SELECT new org.rhq.core.domain.resource.composite.ResourceIdFlyWeight(res.id, res.uuid) " //
        + "  FROM Resource res " //
        + " WHERE res.parentResource.id = :parentId " //
        + " AND res.inventoryStatus = :status "),
    @NamedQuery(name = Resource.QUERY_FIND_BY_EXPLICIT_RESOURCE_GROUP, query = "" //
        + "SELECT DISTINCT res " //
        + "  FROM ResourceGroup rg JOIN rg.roles r JOIN r.subjects s JOIN rg.explicitResources res " //
        + " WHERE rg = :group " //
        + "   AND s = :subject"),
    @NamedQuery(name = Resource.QUERY_FIND_BY_EXPLICIT_RESOURCE_GROUP_ADMIN, query = "" //
        + "SELECT res " //
        + "  FROM ResourceGroup rg, IN (rg.explicitResources) res " + " WHERE rg = :group "),
    @NamedQuery(name = Resource.QUERY_FIND_BY_IMPLICIT_RESOURCE_GROUP, query = "" //
        + "SELECT DISTINCT res " //
        + "  FROM ResourceGroup rg JOIN rg.roles r JOIN r.subjects s JOIN rg.implicitResources res " //
        + " WHERE rg = :group " //
        + "   AND s = :subject"),
    @NamedQuery(name = Resource.QUERY_FIND_BY_IMPLICIT_RESOURCE_GROUP_ADMIN, query = "" //
        + "SELECT res " //
        + "  FROM ResourceGroup rg, IN (rg.implicitResources) res " //
        + " WHERE rg = :group "),
    @NamedQuery(name = Resource.QUERY_FIND_EXPLICIT_RESOURCES_WITH_AVAILABILITY_FOR_RESOURCE_GROUP, query = "" //
        + "SELECT new org.rhq.core.domain.resource.composite.ResourceWithAvailability(res, " //
        + " (SELECT parentRes FROM Resource parentRes WHERE parentRes = res.parentResource), " //
        + " a.availabilityType, " //
        + " 1, (SELECT count(iir) FROM rg.implicitResources iir WHERE iir = res)) " //
        + "  FROM ResourceGroup rg JOIN rg.explicitResources res LEFT JOIN res.parentResource parent " //
        + "  LEFT JOIN res.currentAvailability a " //
        + " WHERE rg.id = :groupId " //
        + "   AND rg.id IN (SELECT irg.id FROM ResourceGroup irg JOIN irg.roles r JOIN r.subjects s WHERE s = :subject) " //
        + "   AND res.inventoryStatus = 'COMMITTED' "), //
    @NamedQuery(name = Resource.QUERY_FIND_EXPLICIT_RESOURCES_WITH_AVAILABILITY_FOR_RESOURCE_GROUP_ADMIN, query = "" //
        + "SELECT new org.rhq.core.domain.resource.composite.ResourceWithAvailability(res, " //
        + " (SELECT parentRes FROM Resource parentRes WHERE parentRes = res.parentResource), " //
        + " a.availabilityType, 1, " //
        + "(SELECT count(iir) FROM rg.implicitResources iir WHERE iir = res)) " //
        + "  FROM ResourceGroup rg JOIN rg.explicitResources res LEFT JOIN res.parentResource parent " //
        + "  LEFT JOIN res.currentAvailability a " //
        + " WHERE rg.id = :groupId " + //
        "   AND res.inventoryStatus = 'COMMITTED' "), //
    @NamedQuery(name = Resource.QUERY_FIND_IMPLICIT_RESOURCES_WITH_AVAILABILITY_FOR_RESOURCE_GROUP, query = "" //
        + "SELECT new org.rhq.core.domain.resource.composite.ResourceWithAvailability(res, " //
        + " (SELECT parentRes FROM Resource parentRes WHERE parentRes = res.parentResource), " //
        + " a.availabilityType, " //
        + "(SELECT count(ier) FROM rg.explicitResources ier WHERE ier = res), 1) " //
        + "  FROM ResourceGroup rg JOIN rg.implicitResources res LEFT JOIN res.parentResource parent " //
        + "  LEFT JOIN res.currentAvailability a " //
        + " WHERE rg.id = :groupId " //
        + "   AND rg.id IN (SELECT irg.id FROM ResourceGroup irg JOIN irg.roles r JOIN r.subjects s WHERE s = :subject) " + "   AND res.inventoryStatus = 'COMMITTED' "), //
    @NamedQuery(name = Resource.QUERY_FIND_IMPLICIT_RESOURCES_WITH_AVAILABILITY_FOR_RESOURCE_GROUP_ADMIN, query = "" //
        + "SELECT new org.rhq.core.domain.resource.composite.ResourceWithAvailability(res, " //
        + " (SELECT parentRes FROM Resource parentRes WHERE parentRes = res.parentResource), " //
        + " a.availabilityType, " //
        + "(SELECT count(ier) FROM rg.explicitResources ier WHERE ier = res), 1) " //
        + "  FROM ResourceGroup rg JOIN rg.implicitResources res LEFT JOIN res.parentResource parent " //
        + "  LEFT JOIN res.currentAvailability a " //
        + " WHERE rg.id = :groupId " //
        + "   AND res.inventoryStatus = 'COMMITTED' "), //
    @NamedQuery(name = Resource.QUERY_FIND_IMPLICIT_RESOURCES_FOR_RESOURCE_GROUP_COUNT, query = "" //
        + "SELECT count(DISTINCT res) " //
        + "  FROM ResourceGroup rg JOIN rg.implicitResources res JOIN rg.roles r JOIN r.subjects s " //
        + " WHERE rg.id = :groupId " //
        + "   AND res.inventoryStatus = 'COMMITTED' " //
        + "   AND s = :subject "),
    @NamedQuery(name = Resource.QUERY_FIND_IMPLICIT_RESOURCES_FOR_RESOURCE_GROUP_COUNT_ADMIN, query = "" //
        + "SELECT count(res) " //
        + "  FROM ResourceGroup rg JOIN rg.implicitResources res " //
        + " WHERE rg.id = :groupId " //
        + "   AND res.inventoryStatus = 'COMMITTED' "),
    @NamedQuery(name = Resource.QUERY_FIND_EXPLICIT_RESOURCES_FOR_RESOURCE_GROUP_COUNT, query = "" //
        + "SELECT count(DISTINCT res) " //
        + "  FROM ResourceGroup rg JOIN rg.explicitResources res JOIN rg.roles r JOIN r.subjects s " //
        + " WHERE rg.id = :groupId " //
        + "   AND res.inventoryStatus = 'COMMITTED' " //
        + "   AND s = :subject "),
    @NamedQuery(name = Resource.QUERY_FIND_EXPLICIT_RESOURCES_FOR_RESOURCE_GROUP_COUNT_ADMIN, query = "" //
        + "SELECT count(res) " //
        + "  FROM ResourceGroup rg JOIN rg.explicitResources res " //
        + " WHERE rg.id = :groupId " //
        + "   AND res.inventoryStatus = 'COMMITTED' "),
    @NamedQuery(name = Resource.QUERY_GET_AVAILABLE_RESOURCES_FOR_CHANNEL, query = "" //
        + "  SELECT res " //  
        + "    FROM Resource AS res " //
        + "   WHERE res.id NOT IN " //
        + "       ( SELECT rc.resource.id " //
        + "           FROM ResourceChannel rc " //
        + "          WHERE rc.channel.id = :channelId ) " //
        + "     AND (:category = res.resourceType.category OR :category IS NULL) " //
        + "     AND (res.inventoryStatus = :inventoryStatus) " //
        + "     AND (UPPER(res.name) LIKE :search OR :search is null) "),
    @NamedQuery(name = Resource.QUERY_GET_AVAILABLE_RESOURCES_FOR_RESOURCE_GROUP, query = "" //
        + "SELECT res " //
        + "  FROM Resource AS res " //
        + " WHERE res.id NOT IN " //
        + "       ( SELECT ires.id " //
        + "           FROM Resource ires JOIN ires.explicitGroups AS irg " //
        + "          WHERE irg.id = :groupId ) " //
        + "   AND (:type = res.resourceType OR :type IS NULL) " //
        + "   AND (:category = res.resourceType.category OR :category IS NULL) " //
        + "   AND (res.inventoryStatus = :inventoryStatus) " //
        + "   AND (UPPER(res.name) LIKE :search OR :search is null) "),
    @NamedQuery(name = Resource.QUERY_GET_AVAILABLE_RESOURCES_FOR_RESOURCE_GROUP_WITH_EXCLUDES, query = "" //
        + "SELECT res " //
        + "  FROM Resource AS res " //
        + " WHERE res.id NOT IN " //
        + "       ( SELECT ires.id " //
        + "           FROM Resource ires JOIN ires.explicitGroups AS irg " //
        + "          WHERE irg.id = :groupId ) " //
        + "   AND (:type = res.resourceType OR :type IS NULL) " //
        + "   AND (:category = res.resourceType.category OR :category IS NULL) " //
        + "   AND (res.inventoryStatus = :inventoryStatus) " //
        + "   AND (UPPER(res.name) LIKE :search OR :search is null) " //
        + "   AND res.id NOT IN ( :excludeIds ) "),
    @NamedQuery(name = Resource.QUERY_GET_AVAILABLE_RESOURCES_WITH_PARENT_FOR_RESOURCE_GROUP, query = "" //
        + "SELECT res " //
        + "  FROM Resource AS res " //
        + "  LEFT JOIN FETCH res.parentResource parent " //
        + " WHERE res.id NOT IN " //
        + "       ( SELECT ires.id " //
        + "           FROM Resource ires JOIN ires.explicitGroups AS irg " //
        + "          WHERE irg.id = :groupId ) " //
        + "   AND (:type = res.resourceType OR :type IS NULL) " //
        + "   AND (:category = res.resourceType.category OR :category IS NULL) " //
        + "   AND (res.inventoryStatus = :inventoryStatus) " //
        + "   AND (UPPER(res.name) LIKE :search OR :search is null) "),
    @NamedQuery(name = Resource.QUERY_GET_AVAILABLE_RESOURCES_WITH_PARENT_FOR_RESOURCE_GROUP_WITH_EXCLUDES, query = "" //
        + "SELECT res " //
        + "  FROM Resource AS res " //
        + "  LEFT JOIN FETCH res.parentResource parent " //
        + " WHERE res.id NOT IN " //
        + "       ( SELECT ires.id " //
        + "           FROM Resource ires JOIN ires.explicitGroups AS irg " //
        + "          WHERE irg.id = :groupId ) " //
        + "   AND (:type = res.resourceType OR :type IS NULL) " //
        + "   AND (:category = res.resourceType.category OR :category IS NULL) " //
        + "   AND (res.inventoryStatus = :inventoryStatus) " //
        + "   AND (UPPER(res.name) LIKE :search OR :search is null) " //
        + "   AND res.id NOT IN ( :excludeIds ) "),
    @NamedQuery(name = Resource.QUERY_GET_AVAILABLE_RESOURCES_FOR_DASHBOARD_PORTLET, query = "" //
        + "SELECT res " //
        + "  FROM Resource AS res " //
        + " WHERE (:typeId = res.resourceType.id OR :typeId IS NULL) " //
        + "   AND (:category = res.resourceType.category OR :category IS NULL) " //
        + "   AND (res.inventoryStatus = :inventoryStatus) "),
    @NamedQuery(name = Resource.QUERY_GET_AVAILABLE_RESOURCES_FOR_DASHBOARD_PORTLET_WITH_EXCLUDES, query = "" //
        + "SELECT res " //
        + "  FROM Resource AS res " //
        + " WHERE (:typeId = res.resourceType.id OR :typeId IS NULL) " //
        + "   AND (:category = res.resourceType.category OR :category IS NULL) " //
        + "   AND (res.inventoryStatus = :inventoryStatus) " //
        + "   AND res.id NOT IN ( :excludeIds ) "),
    @NamedQuery(name = Resource.QUERY_FIND_BY_IDS, query = "" //
        + "SELECT res " //
        + "  FROM Resource res " //
        + " WHERE res.id IN ( :ids ) " //
        + "   AND res.id IN (SELECT rr.id FROM Resource rr JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s WHERE s = :subject) "),
    @NamedQuery(name = Resource.QUERY_FIND_BY_IDS_ADMIN, query = "SELECT res FROM Resource res WHERE res.id IN ( :ids )"),
    @NamedQuery(name = Resource.QUERY_FIND_WITH_PARENT_BY_IDS, query = "" //
        + "SELECT res " //
        + "  FROM Resource res " //
        + "  LEFT JOIN FETCH res.parentResource parent " //
        + " WHERE res.id IN ( :ids ) " //
        + "   AND res.id IN (SELECT rr.id FROM Resource rr JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s WHERE s = :subject) "),
    @NamedQuery(name = Resource.QUERY_FIND_WITH_PARENT_BY_IDS_ADMIN, query = "SELECT res FROM Resource res LEFT JOIN FETCH res.parentResource parent WHERE res.id IN ( :ids )"),
    @NamedQuery(name = Resource.QUERY_FIND_COMPOSITE, query = "" //
        + "SELECT new org.rhq.core.domain.resource.composite.ResourceComposite(res, a.availabilityType, " //
        + " (SELECT count(p) FROM res.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s = :subject AND p = 8), " // we want MANAGE_MEASUREMENTS
        + " (SELECT count(p) FROM res.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s = :subject AND p = 4), " // we want MODIFY_RESOURCE (4), not VIEW_RESOURCE (3)
        + " (SELECT count(p) FROM res.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s = :subject AND p = 10), " // we want CONTROL, 10
        + " (SELECT count(p) FROM res.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s = :subject AND p = 7), " // we want MANAGE_ALERTS, 7
        + " (SELECT count(p) FROM res.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s = :subject AND p = 11), " // we want CONFIGURE, 11
        + " (SELECT count(p) FROM res.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s = :subject AND p = 9), " // we want MANAGE_CONTENT, 9
        + " (SELECT count(p) FROM res.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s = :subject AND p = 6), " // we want CREATE_CHILD_RESOURCES, 6
        + " (SELECT count(p) FROM res.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s = :subject AND p = 5)) " // we want DELETE_RESOURCES, 5
        + "FROM Resource res " //
        + "     LEFT JOIN res.currentAvailability a " //
        + "WHERE res.id IN (SELECT rr.id FROM Resource rr JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s WHERE s = :subject)"
        + "  AND (:category = res.resourceType.category OR :category is null) " //
        + "  AND (:parentResource = res.parentResource OR :parentResource is null)" //
        + "  AND (:resourceType = res.resourceType OR :resourceType is null) " //
        + "  AND (res.inventoryStatus = :inventoryStatus OR :inventoryStatus is null) " //
        + "  AND (UPPER(res.name) LIKE :search OR UPPER(res.description) LIKE :search OR :search is null) "),
    @NamedQuery(name = Resource.QUERY_FIND_COMPOSITE_WITH_PARENT, query = "" //
        + "SELECT new org.rhq.core.domain.resource.composite.ResourceComposite(res, " //
        + " (SELECT ires FROM Resource ires WHERE ires = res.parentResource), " //
        + " a.availabilityType, " //
        + " (SELECT count(p) FROM res.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s = :subject AND p = 8), " // we want MANAGE_MEASUREMENTS
        + " (SELECT count(p) FROM res.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s = :subject AND p = 4), " // we want MODIFY_RESOURCE (4), not VIEW_RESOURCE (3)
        + " (SELECT count(p) FROM res.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s = :subject AND p = 10), " // we want CONTROL, 10
        + " (SELECT count(p) FROM res.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s = :subject AND p = 7), " // we want MANAGE_ALERTS, 7
        + " (SELECT count(p) FROM res.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s = :subject AND p = 11), " // we want CONFIGURE, 11
        + " (SELECT count(p) FROM res.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s = :subject AND p = 9), " // we want MANAGE_CONTENT, 9
        + " (SELECT count(p) FROM res.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s = :subject AND p = 6), " // we want CREATE_CHILD_RESOURCES, 6
        + " (SELECT count(p) FROM res.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s = :subject AND p = 5)) " // we want DELETE_RESOURCES, 5
        + "FROM Resource res " //
        + "     LEFT JOIN res.parentResource parent " //
        + "     LEFT JOIN res.currentAvailability a " //
        + "WHERE res.id IN (SELECT rr.id FROM Resource rr JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s WHERE s = :subject)"
        + "  AND (:category = res.resourceType.category OR :category is null) " //
        + "  AND (:parentResource = res.parentResource OR :parentResource is null)" //
        + "  AND (:resourceType = res.resourceType OR :resourceType is null) " //
        + "  AND (res.inventoryStatus = :inventoryStatus OR :inventoryStatus is null) " //
        + "  AND (UPPER(res.name) LIKE :search OR UPPER(res.description) LIKE :search OR :search is null) "),
    @NamedQuery(name = Resource.QUERY_FIND_COMPOSITE_COUNT, query = "SELECT count(res) " //
        + "  FROM Resource res " //
        + " WHERE res.id IN (SELECT rr.id FROM Resource rr JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s WHERE s = :subject)"
        + "   AND (:category = res.resourceType.category OR :category is null) " //
        + "   AND (:parentResource = res.parentResource OR :parentResource is null)" //
        + "   AND (:resourceType = res.resourceType OR :resourceType is null) " //
        + "   AND (res.inventoryStatus = :inventoryStatus OR :inventoryStatus is null) " //
        + "   AND (UPPER(res.name) LIKE :search OR UPPER(res.description) LIKE :search OR :search is null) "),
    @NamedQuery(name = Resource.QUERY_FIND_COMPOSITE_ADMIN, query = "" //
        + "SELECT new org.rhq.core.domain.resource.composite.ResourceComposite(res, a.availabilityType) " //
        + "  FROM Resource res " //
        + "       LEFT JOIN res.currentAvailability a " //
        + " WHERE (:category = res.resourceType.category OR :category is null) " //
        + "   AND (:parentResource = res.parentResource OR :parentResource is null)" //
        + "   AND (:resourceType = res.resourceType OR :resourceType is null) " //
        + "   AND (res.inventoryStatus = :inventoryStatus OR :inventoryStatus is null) " //
        + "   AND (UPPER(res.name) LIKE :search OR UPPER(res.description) LIKE :search OR :search is null) "),
    @NamedQuery(name = Resource.QUERY_FIND_COMPOSITE_WITH_PARENT_ADMIN, query = "" //
        + "SELECT new org.rhq.core.domain.resource.composite.ResourceComposite( res, " //
        + "       (SELECT ires FROM Resource ires WHERE ires = res.parentResource), " //
        + "       a.availabilityType ) " //
        + "  FROM Resource res " //
        + "       LEFT JOIN res.parentResource parent " //
        + "       LEFT JOIN res.currentAvailability a " //
        + " WHERE (:category = res.resourceType.category OR :category is null) " //
        + "   AND (:parentResource = res.parentResource OR :parentResource is null)" //
        + "   AND (:resourceType = res.resourceType OR :resourceType is null) " //
        + "   AND (res.inventoryStatus = :inventoryStatus OR :inventoryStatus is null) " //
        + "   AND (UPPER(res.name) LIKE :search OR UPPER(res.description) LIKE :search OR :search is null) "),
    @NamedQuery(name = Resource.QUERY_FIND_COMPOSITE_COUNT_ADMIN, query = "" //
        + " SELECT count(res) " //
        + "  FROM Resource res " //
        + " WHERE (:category = res.resourceType.category OR :category is null) " //
        + "   AND (:parentResource = res.parentResource OR :parentResource is null)" //
        + "   AND (:resourceType = res.resourceType OR :resourceType is null) " //
        + "   AND (res.inventoryStatus = :inventoryStatus OR :inventoryStatus is null) " //
        + "   AND (UPPER(res.name) LIKE :search OR UPPER(res.description) LIKE :search OR :search is null) "),
    @NamedQuery(name = Resource.QUERY_GET_STATUSES_BY_PARENT, query = "" //
        + "SELECT r.id, r.inventoryStatus " //
        + "  FROM Resource r " //
        + " WHERE r.parentResource.id = :parentResourceId"),
    @NamedQuery(name = Resource.QUERY_GET_RESOURCE_HEALTH_BY_IDS, query = "" //
        + "  SELECT new org.rhq.core.domain.resource.composite.ResourceHealthComposite( " //
        + "          res.id, res.name, res.resourceType.name, avail.availabilityType, COUNT(alert)) " //
        + "    FROM Resource res " //
        + "         LEFT JOIN res.availability avail " //
        + "         LEFT JOIN res.alertDefinitions alertDef " //
        + "         LEFT JOIN alertDef.alerts alert " //
        + "   WHERE res.id IN (:resourceIds) " //
        + "     AND avail.endTime IS NULL " //
        + "GROUP BY res.id, res.name, res.resourceType.name, avail.availabilityType "),
    @NamedQuery(name = Resource.QUERY_FIND_BY_ID_WITH_INSTALLED_PACKAGES, query = "SELECT r FROM Resource AS r LEFT JOIN r.installedPackages ip WHERE r.id = :id"),
    @NamedQuery(name = Resource.QUERY_FIND_BY_ID_WITH_INSTALLED_PACKAGE_HIST, query = "SELECT r FROM Resource AS r LEFT JOIN r.installedPackageHistory ip WHERE r.id = :id"),
    @NamedQuery(name = Resource.QUERY_FIND_PLATFORM_BY_AGENT, query = "SELECT res FROM Resource res WHERE res.resourceType.category = :category AND res.agent = :agent"),
    @NamedQuery(name = Resource.QUERY_FIND_PAREBT_ID, query = "SELECT res.parentResource.id FROM Resource AS res WHERE res.id = :id"),
    @NamedQuery(name = Resource.QUERY_FIND_ROOT_PLATFORM_OF_RESOURCE, query = "" + "SELECT DISTINCT r FROM Resource r " + "WHERE r.parentResource.id is null " + "AND " + "  ( "
        + "    r.id = :resourceId " + "    OR EXISTS (SELECT rr FROM Resource rr WHERE rr.id = :resourceId AND rr.parentResource = r) "
        + "    OR EXISTS (SELECT rr FROM Resource rr WHERE rr.id = :resourceId AND rr.parentResource.parentResource = r) "
        + "    OR EXISTS (SELECT rr FROM Resource rr WHERE rr.id = :resourceId AND rr.parentResource.parentResource.parentResource = r) "
        + "    OR EXISTS (SELECT rr FROM Resource rr WHERE rr.id = :resourceId AND rr.parentResource.parentResource.parentResource.parentResource = r) "
        + "    OR EXISTS (SELECT rr FROM Resource rr WHERE rr.id = :resourceId AND rr.parentResource.parentResource.parentResource.parentResource.parentResource = r) "
        + "    OR EXISTS (SELECT rr FROM Resource rr WHERE rr.id = :resourceId AND rr.parentResource.parentResource.parentResource.parentResource.parentResource.parentResource = r) " + "  )") })
@SequenceGenerator(name = "RHQ_RESOURCE_SEQ", sequenceName = "RHQ_RESOURCE_ID_SEQ")
@Table(name = "RHQ_RESOURCE")
@XmlAccessorType(XmlAccessType.FIELD)
public class Resource implements Comparable<Resource>, Externalizable {
    public static final String QUERY_FIND_PROBLEM_RESOURCES_ALERT = "Resource.findProblemResourcesAlert";
    public static final String QUERY_FIND_PROBLEM_RESOURCES_ALERT_ADMIN = "Resource.findProblemResourcesAlert_admin";
    public static final String QUERY_FIND_PROBLEM_RESOURCES_ALERT_COUNT = "Resource.findProblemResourcesAlertCount";
    public static final String QUERY_FIND_PROBLEM_RESOURCES_ALERT_COUNT_ADMIN = "Resource.findProblemResourcesAlertCount_admin";

    public static final String QUERY_FIND_BY_PARENT_AND_TYPE = "Resource.findByParentAndType";
    public static final String QUERY_FIND_BY_PARENT_AND_TYPE_ADMIN = "Resource.findByParentAndType_admin";
    public static final String QUERY_FIND_FOR_AUTOGROUP = "Resource.findForAutogroup";

    public static final String QUERY_FIND_BY_PARENT_AND_INVENTORY_STATUS = "Resource.findByParentAndInventoryStatus";
    public static final String QUERY_FIND_BY_PARENT_AND_INVENTORY_STATUS_ADMIN = "Resource.findByParentAndInventoryStatus_admin";

    public static final String QUERY_FIND_VALID_COMMITTED_RESOURCE_IDS_ADMIN = "Resource.findValidCommittedResourceIds_admin";
    public static final String QUERY_FIND_CHILDREN_IDS_ADMIN = "Resource.findChildrenIds_admin";
    public static final String QUERY_FIND_CHILDREN = "Resource.findChildren";
    public static final String QUERY_FIND_CHILDREN_ADMIN = "Resource.findChildren_admin";

    public static final String QUERY_RECENTLY_ADDED_PLATFORMS = "Resource.findRecentlyAddedPlatforms";
    public static final String QUERY_RECENTLY_ADDED_PLATFORMS_ADMIN = "Resource.findRecentlyAddedPlatforms_admin";

    public static final String QUERY_RECENTLY_ADDED_SERVERS = "Resource.findRecentlyAddedServers";
    public static final String QUERY_RECENTLY_ADDED_SERVERS_ADMIN = "Resource.findRecentlyAddedServers_admin";

    public static final String QUERY_FIND_AVAILABILITY_BY_RESOURCE_ID = "Resource.findAvailabilityByResourceId";
    public static final String QUERY_FIND_AVAILABILITY_BY_RESOURCE_ID_ADMIN = "Resource.findAvailabilityByResourceId_admin";

    public static final String QUERY_FIND_AVAILABILITY_BY_RESOURCE_IDS = "Resource.findAvailabilityByResourceIds";
    public static final String QUERY_FIND_AVAILABILITY_BY_RESOURCE_IDS_ADMIN = "Resource.findAvailabilityByResourceIds_admin";

    public static final String QUERY_FIND_RESOURCE_AUTOGROUP_COMPOSITE = "Resource.findResourceAutogroupComposite";
    public static final String QUERY_FIND_RESOURCE_AUTOGROUP_COMPOSITE_ADMIN = "Resource.findResourceAutogroupComposite_admin";

    public static final String QUERY_FIND_RESOURCE_AUTOGROUPS_COMPOSITE = "Resource.findResourceAutogroupsComposite";
    public static final String QUERY_FIND_RESOURCE_AUTOGROUPS_COMPOSITE_ADMIN = "Resource.findResourceAutogroupsComposite_admin";

    public static final String QUERY_FIND_CHILDREN_AUTOGROUP_COMPOSITES = "Resource.findChildrenAutogroupComposites";
    public static final String QUERY_FIND_CHILDREN_AUTOGROUP_COMPOSITES_ADMIN = "Resource.findChildrenAutogroupComposites_admin";

    public static final String QUERY_FIND_CHILDREN_AUTOGROUP_COMPOSITES_BY_TYPE = "Resource.findChildrenAutogroupCompositesByType";
    public static final String QUERY_FIND_CHILDREN_AUTOGROUP_COMPOSITES_BY_TYPE_ADMIN = "Resource.findChildrenAutogroupCompositesByType_admin";

    public static final String QUERY_FIND_CHILDREN_BY_CATEGORY_AND_INVENTORY_STATUS = "Resource.findChildrenByCategoryAndInventoryStatus";
    public static final String QUERY_FIND_CHILDREN_BY_CATEGORY_AND_INVENTORY_STATUS_ADMIN = "Resource.findChildrenByCategoryAndInventoryStatus_admin";

    public static final String QUERY_FIND_BY_CATEGORY_AND_INVENTORY_STATUS = "Resource.findByCategoryAndInventoryStatus";
    public static final String QUERY_FIND_BY_CATEGORY_AND_INVENTORY_STATUS_ADMIN = "Resource.findByCategoryAndInventoryStatus_admin";

    public static final String QUERY_FIND_QUEUED_PLATFORMS_BY_INVENTORY_STATUS = "Resource.findQueuedPlatformsByInventoryStatus";

    public static final String QUERY_FIND_BY_TYPE = "Resource.findByType";
    public static final String QUERY_FIND_BY_TYPE_ADMIN = "Resource.findByType_admin";

    public static final String QUERY_FIND_BY_TYPE_AND_IDS = "Resource.findByTypeAndIds";
    public static final String QUERY_FIND_BY_TYPE_AND_IDS_ADMIN = "Resource.findByTypeAndIds_admin";

    public static final String QUERY_FIND_BY_PARENT_AND_KEY = "Resource.findByParentAndKey";

    public static final String QUERY_FIND_EXPLICIT_IDS_BY_RESOURCE_GROUP_ADMIN = "Resource.findExplicitIdsByResourceGroup_admin";

    public static final String QUERY_FIND_FLY_WEIGHTS_BY_RESOURCE_IDS = "Resource.findFlyWeights";
    public static final String QUERY_FIND_FLY_WEIGHTS_BY_PARENT_RESOURCE_ID = "Resource.findFlyWeightsByResourceParentId";

    public static final String QUERY_FIND_BY_EXPLICIT_RESOURCE_GROUP = "Resource.findByExplicitResourceGroup";
    public static final String QUERY_FIND_BY_EXPLICIT_RESOURCE_GROUP_ADMIN = "Resource.findByExplicitResourceGroup_admin";

    public static final String QUERY_FIND_BY_IMPLICIT_RESOURCE_GROUP = "Resource.findByImplicitResourceGroup";
    public static final String QUERY_FIND_BY_IMPLICIT_RESOURCE_GROUP_ADMIN = "Resource.findByImplicitResourceGroup_admin";

    public static final String QUERY_FIND_EXPLICIT_RESOURCES_WITH_AVAILABILITY_FOR_RESOURCE_GROUP = "ResourceWithAvailability.findExplicitByResourceGroup";
    public static final String QUERY_FIND_EXPLICIT_RESOURCES_WITH_AVAILABILITY_FOR_RESOURCE_GROUP_ADMIN = "ResourceWithAvailability.findExplicitByResourceGroup_admin";
    public static final String QUERY_FIND_IMPLICIT_RESOURCES_WITH_AVAILABILITY_FOR_RESOURCE_GROUP = "ResourceWithAvailability.findImplicitByResourceGroup";
    public static final String QUERY_FIND_IMPLICIT_RESOURCES_WITH_AVAILABILITY_FOR_RESOURCE_GROUP_ADMIN = "ResourceWithAvailability.findImplicitByResourceGroup_admin";
    public static final String QUERY_FIND_IMPLICIT_RESOURCES_FOR_RESOURCE_GROUP_COUNT = "ResourceWithAvailability.findImplicitByResourceGroup_count";
    public static final String QUERY_FIND_IMPLICIT_RESOURCES_FOR_RESOURCE_GROUP_COUNT_ADMIN = "ResourceWithAvailability.findImplicitByResourceGroup_count_admin";
    public static final String QUERY_FIND_EXPLICIT_RESOURCES_FOR_RESOURCE_GROUP_COUNT = "ResourceWithAvailability.findExplicitByResourceGroup_count";
    public static final String QUERY_FIND_EXPLICIT_RESOURCES_FOR_RESOURCE_GROUP_COUNT_ADMIN = "ResourceWithAvailability.findExplicitByResourceGroup_count_admin";

    public static final String QUERY_GET_AVAILABLE_RESOURCES_FOR_CHANNEL = "Resource.getAvailableResourcesForChannel";

    public static final String QUERY_GET_AVAILABLE_RESOURCES_FOR_RESOURCE_GROUP = "Resource.getAvailableResourcesForResourceGroup";
    public static final String QUERY_GET_AVAILABLE_RESOURCES_FOR_RESOURCE_GROUP_WITH_EXCLUDES = "Resource.getAvailableResourcesForResourceGroupWithExcludes";

    public static final String QUERY_GET_AVAILABLE_RESOURCES_WITH_PARENT_FOR_RESOURCE_GROUP = "Resource.getAvailableResourcesWithParentForResourceGroup";
    public static final String QUERY_GET_AVAILABLE_RESOURCES_WITH_PARENT_FOR_RESOURCE_GROUP_WITH_EXCLUDES = "Resource.getAvailableResourcesWithParentForResourceGroupWithExcludes";

    public static final String QUERY_GET_AVAILABLE_RESOURCES_FOR_DASHBOARD_PORTLET = "Resource.getAvailableResourcesForDashboardPortlet";
    public static final String QUERY_GET_AVAILABLE_RESOURCES_FOR_DASHBOARD_PORTLET_WITH_EXCLUDES = "Resource.getAvailableResourcesForDashboardPortletWithExcludes";

    public static final String QUERY_FIND_BY_IDS = "Resource.findByIds";
    public static final String QUERY_FIND_BY_IDS_ADMIN = "Resource.findByIds_admin";

    public static final String QUERY_FIND_WITH_PARENT_BY_IDS = "Resource.findWithParentByIds";
    public static final String QUERY_FIND_WITH_PARENT_BY_IDS_ADMIN = "Resource.findWithParentByIds_admin";

    public static final String QUERY_FIND_COMPOSITE = "Resource.findComposite";
    public static final String QUERY_FIND_COMPOSITE_WITH_PARENT = "Resource.findCompositeWithParent";
    public static final String QUERY_FIND_COMPOSITE_COUNT = "Resource.findComposite_count";
    public static final String QUERY_FIND_COMPOSITE_ADMIN = "Resource.findComposite_admin";
    public static final String QUERY_FIND_COMPOSITE_WITH_PARENT_ADMIN = "Resource.findCompositeWithParent_admin";
    public static final String QUERY_FIND_COMPOSITE_COUNT_ADMIN = "Resource.findComposite_count_admin";

    public static final String QUERY_GET_STATUSES_BY_PARENT = "Resource.getStatusesByParent";

    public static final String QUERY_GET_RESOURCE_HEALTH_BY_IDS = "Resource.getResourceHealthByIds";

    public static final String QUERY_FIND_BY_ID_WITH_INSTALLED_PACKAGES = "Resource.findByIdWithInstalledPackages";
    public static final String QUERY_FIND_BY_ID_WITH_INSTALLED_PACKAGE_HIST = "Resource.findByIdWithInstalledPackageHist";

    public static final String QUERY_FIND_PLATFORM_BY_AGENT = "Resource.findPlatformByAgent";

    public static final String QUERY_FIND_PAREBT_ID = "Resource.findParentId";

    public static final String QUERY_FIND_ROOT_PLATFORM_OF_RESOURCE = "Resource.findRootPlatformOfResource";

    private static final long serialVersionUID = 1L;

    public static final Resource ROOT = null;
    public static final int ROOT_ID = -1;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RHQ_RESOURCE_SEQ")
    @Id
    private int id;

    @Column(name = "UUID")
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "RESOURCE_KEY")
    private String resourceKey;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "INVENTORY_STATUS")
    @Enumerated(EnumType.STRING)
    private InventoryStatus inventoryStatus = InventoryStatus.NEW;

    @Column(name = "CONNECTED")
    private boolean connected;

    @Column(name = "VERSION")
    private String version;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "CTIME")
    private Long ctime = System.currentTimeMillis();

    @Column(name = "MTIME")
    private Long mtime = System.currentTimeMillis();

    @Column(name = "ITIME")
    private Long itime = System.currentTimeMillis(); // time inventory status changed

    @JoinColumn(name = "MODIFIED_BY")
    @ManyToOne(fetch = FetchType.LAZY)
    private Subject modifiedBy;

    @Column(name = "LOCATION")
    private String location;

    @JoinColumn(name = "RESOURCE_TYPE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private ResourceType resourceType;

    // LAZY fetch otherwise this will recursively call all parents until null is found
    // do not cascade remove - would take forever to delete a full platform hierarchy
    // we will manually delete the children ourselves
    @OneToMany(mappedBy = "parentResource", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST })
    @OrderBy
    // primary key
    private Set<Resource> childResources = new LinkedHashSet<Resource>();

    @JoinColumn(name = "PARENT_RESOURCE_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    @XmlTransient
    private Resource parentResource;

    @JoinColumn(name = "RES_CONFIGURATION_ID", referencedColumnName = "ID")
    @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY)
    private Configuration resourceConfiguration = new Configuration();

    @JoinColumn(name = "PLUGIN_CONFIGURATION_ID", referencedColumnName = "ID")
    @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY)
    private Configuration pluginConfiguration = new Configuration();

    @JoinColumn(name = "AGENT_ID", referencedColumnName = "ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private Agent agent;

    // bulk delete @OneToMany(mappedBy = "resource", cascade = { CascadeType.ALL })
    @OneToMany(mappedBy = "resource", cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
    private Set<AlertDefinition> alertDefinitions = new LinkedHashSet<AlertDefinition>();

    // bulk delete @OneToMany(mappedBy = "resource", cascade = { CascadeType.ALL })
    @OneToMany(mappedBy = "resource", cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
    @OrderBy
    // by primary key which will also put the resource configuration updates in chronological order
    private List<ResourceConfigurationUpdate> resourceConfigurationUpdates = new ArrayList<ResourceConfigurationUpdate>();

    // bulk delete @OneToMany(mappedBy = "resource", cascade = { CascadeType.ALL })
    @OneToMany(mappedBy = "resource", cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
    @OrderBy
    // by primary key which will also put the plugin configuration updates in chronological order
    private List<PluginConfigurationUpdate> pluginConfigurationUpdates = new ArrayList<PluginConfigurationUpdate>();

    // bulk delete
    @ManyToMany(mappedBy = "implicitResources", fetch = FetchType.LAZY)
    private Set<ResourceGroup> implicitGroups = new HashSet<ResourceGroup>();

    // bulk delete 
    @ManyToMany(mappedBy = "explicitResources", fetch = FetchType.LAZY)
    private Set<ResourceGroup> explicitGroups = new HashSet<ResourceGroup>();

    // bulk delete @OneToMany(mappedBy = "resource", cascade = { CascadeType.ALL }, fetch = FetchType.LAZY)
    @OneToMany(mappedBy = "resource", cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.LAZY)
    @OrderBy
    private List<ContentServiceRequest> contentServiceRequests = new ArrayList<ContentServiceRequest>();

    // bulk delete @OneToMany(mappedBy = "parentResource", cascade = { CascadeType.ALL }, fetch = FetchType.LAZY)
    @OneToMany(mappedBy = "parentResource", cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.LAZY)
    @OrderBy
    private List<CreateResourceHistory> createChildResourceRequests = new ArrayList<CreateResourceHistory>();

    // bulk delete @OneToMany(mappedBy = "resource", cascade = { CascadeType.ALL })
    @OneToMany(mappedBy = "resource", cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
    @OrderBy
    private List<DeleteResourceHistory> deleteResourceRequests = new ArrayList<DeleteResourceHistory>();

    // bulk delete @OneToMany(mappedBy = "resource", cascade = { CascadeType.ALL })
    @OneToMany(mappedBy = "resource", cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
    @OrderBy
    // by primary key which will also put the operation histories in chronological order
    private List<ResourceOperationHistory> operationHistories = new ArrayList<ResourceOperationHistory>();

    // bulk delete @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OneToMany(mappedBy = "resource", cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.LAZY)
    private Set<InstalledPackage> installedPackages = new HashSet<InstalledPackage>();

    // bulk delete @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OneToMany(mappedBy = "resource", cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.LAZY)
    private List<InstalledPackageHistory> installedPackageHistory = new ArrayList<InstalledPackageHistory>();

    // bulk delete
    @OneToMany(mappedBy = "resource", fetch = FetchType.LAZY)
    private Set<ResourceChannel> resourceChannels = new HashSet<ResourceChannel>();

    // bulk delete @OneToMany(mappedBy = "resource", cascade = CascadeType.REMOVE)
    @OneToMany(mappedBy = "resource")
    private Set<MeasurementSchedule> schedules = new LinkedHashSet<MeasurementSchedule>();

    //bulk delete @OneToMany(mappedBy = "resource", cascade = CascadeType.REMOVE)
    @OneToMany(mappedBy = "resource")
    @OrderBy("startTime")
    private List<Availability> availability;

    @OneToOne(mappedBy = "resource", cascade = { CascadeType.PERSIST, CascadeType.REMOVE }, fetch = FetchType.LAZY)
    private ResourceAvailability currentAvailability;

    // bulk delete @OneToMany(mappedBy = "resource", fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE })
    @OneToMany(mappedBy = "resource", fetch = FetchType.LAZY)
    private List<ResourceError> resourceErrors = new ArrayList<ResourceError>();

    // bulk delete @OneToMany(mappedBy = "resource", cascade = { CascadeType.REMOVE }, fetch = FetchType.LAZY)
    @OneToMany(mappedBy = "resource", fetch = FetchType.LAZY)
    private Set<EventSource> eventSources = new HashSet<EventSource>();

    @JoinColumn(name = "PRODUCT_VERSION_ID", referencedColumnName = "ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private ProductVersion productVersion;

    public Resource() {
        this.uuid = UUID.randomUUID().toString();
    }

    /**
     * Primarily for deserialization and cases where the resource object is just a reference to the real one in the db.
     * (Key is this avoids the irrelevant UUID generation that has contention problems.
     *
     * @param id
     */
    public Resource(int id) {
        this.id = id;
    }

    public Resource( //
        @NotNull
        String resourceKey, //
        @NotNull
        String name, //
        @NotNull
        ResourceType type) {
        this.resourceKey = resourceKey;
        this.name = name;
        this.resourceType = type;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * The UUID is used to synchronized newly discovered resources between server and agent. The UUID is used to unique
     * identify a new resource that an agent has found before the resource ID has been assigned.
     *
     * @return new resource's unique identifier
     */
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    public void setName(@NotNull
    String name) {
        this.name = name;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    public void setResourceKey(String resourceKey) {
        this.resourceKey = resourceKey;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(InventoryStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    /**
     * When <code>true</code>, it can be assumed the plugin can successfully connect to and manage the actual resource.
     * This means the plugin configuration is successfully set and has the proper values that allow the plugin to
     * connect to the managed resource.
     *
     * @return <code>true</code> if the plugin can actually connect to and manage the resource
     */
    public boolean isConnected() {
        return this.connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * The time that this resource's entity was inserted into the database. This is not necessarily the time that the
     * resource was actually committed (aka imported) into inventory - see {@link #getItime()} for that.
     *
     * @return the time this resource entity was committed to the database
     */
    public long getCtime() {
        return this.ctime;
    }

    @PrePersist
    void onPersist() {
        this.mtime = this.ctime = System.currentTimeMillis();
    }

    @PostPersist
    void afterPersist() {
        initCurrentAvailability();
    }

    /**
     * The time that any part of this resource entity was updated in the database.
     *
     * @return resource entity modified time
     */
    public long getMtime() {
        return this.mtime;
    }

    public void setMtime(long mtime) {
        this.mtime = mtime;
    }

    /**
     * Returns the time when this resource's {@link #getInventoryStatus()} changed. If the inventory status is
     * {@link InventoryStatus#COMMITTED}, this is the time when the resource was committed (aka imported) into
     * inventory.
     *
     * @return epoch millisecond of time when status changed
     */
    public long getItime() {
        return this.itime;
    }

    public void setItime(long inventoryTime) {
        this.itime = inventoryTime;
    }

    @PreUpdate
    void onUpdate() {
        this.mtime = System.currentTimeMillis();
    }

    public Subject getModifiedBy() {
        return this.modifiedBy;
    }

    public void setModifiedBy(Subject modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public String getLocation() {
        return this.location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Set<Resource> getChildResources() {
        return this.childResources;
    }

    public void addChildResource(Resource childResource) {
        childResource.setParentResource(this);
        this.childResources.add(childResource);
    }

    public boolean removeChildResource(Resource childResource) {
        return this.childResources.remove(childResource);
    }

    public void setChildResources(Set<Resource> children) {
        this.childResources = children;
    }

    @Nullable
    public Resource getParentResource() {
        return parentResource;
    }

    public void setParentResource(@Nullable
    Resource parentResource) {
        this.parentResource = parentResource;
    }

    public Configuration getResourceConfiguration() {
        return resourceConfiguration;
    }

    public void setResourceConfiguration(Configuration resourceConfiguration) {
        this.resourceConfiguration = resourceConfiguration;
    }

    public Configuration getPluginConfiguration() {
        return pluginConfiguration;
    }

    public void setPluginConfiguration(Configuration pluginConfiguration) {
        this.pluginConfiguration = pluginConfiguration;
    }

    public List<ResourceConfigurationUpdate> getResourceConfigurationUpdates() {
        return resourceConfigurationUpdates;
    }

    public void setResourceConfigurationUpdates(List<ResourceConfigurationUpdate> updates) {
        this.resourceConfigurationUpdates = updates;
    }

    public void addResourceConfigurationUpdates(ResourceConfigurationUpdate update) {
        update.setResource(this);
        this.resourceConfigurationUpdates.add(update);
    }

    public List<PluginConfigurationUpdate> getPluginConfigurationUpdates() {
        return pluginConfigurationUpdates;
    }

    public void setPluginConfigurationUpdates(List<PluginConfigurationUpdate> updates) {
        this.pluginConfigurationUpdates = updates;
    }

    public void addPluginConfigurationUpdates(PluginConfigurationUpdate update) {
        update.setResource(this);
        this.pluginConfigurationUpdates.add(update);
    }

    public Set<MeasurementSchedule> getSchedules() {
        return schedules;
    }

    public void setSchendules(Set<MeasurementSchedule> schedules) {
        this.schedules = schedules;
    }

    public void addSchedule(MeasurementSchedule schedule) {
        schedules.add(schedule);
    }

    public Set<AlertDefinition> getAlertDefinitions() {
        if (this.alertDefinitions == null) {
            this.alertDefinitions = new LinkedHashSet<AlertDefinition>();
        }

        return alertDefinitions;
    }

    public void setAlertDefinitions(Set<AlertDefinition> alertDefinitions) {
        this.alertDefinitions = alertDefinitions;
    }

    public void addAlertDefinition(AlertDefinition alertDefinition) {
        getAlertDefinitions().add(alertDefinition);
        alertDefinition.setResource(this);
    }

    public List<ContentServiceRequest> getContentServiceRequests() {
        return contentServiceRequests;
    }

    public void setContentServiceRequests(List<ContentServiceRequest> contentServiceRequests) {
        this.contentServiceRequests = contentServiceRequests;
    }

    public void addContentServiceRequest(ContentServiceRequest request) {
        request.setResource(this);
        this.contentServiceRequests.add(request);
    }

    public List<CreateResourceHistory> getCreateChildResourceRequests() {
        return createChildResourceRequests;
    }

    public void setCreateChildResourceRequests(List<CreateResourceHistory> createChildResourceRequests) {
        this.createChildResourceRequests = createChildResourceRequests;
    }

    public void addCreateChildResourceHistory(CreateResourceHistory request) {
        request.setParentResource(this);
        this.createChildResourceRequests.add(request);
    }

    public List<DeleteResourceHistory> getDeleteResourceRequests() {
        return deleteResourceRequests;
    }

    public void setDeleteResourceRequests(List<DeleteResourceHistory> deleteResourceRequests) {
        this.deleteResourceRequests = deleteResourceRequests;
    }

    public void addDeleteResourceHistory(DeleteResourceHistory history) {
        history.setResource(this);
        this.deleteResourceRequests.add(history);
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public Set<ResourceGroup> getImplicitGroups() {
        return implicitGroups;
    }

    public void setImplicitGroups(Set<ResourceGroup> implicitGroups) {
        this.implicitGroups = implicitGroups;
    }

    public void addImplicitGroup(ResourceGroup implicitGroup) {
        this.implicitGroups.add(implicitGroup);
    }

    public void removeImplicitGroup(ResourceGroup implicitGroup) {
        this.implicitGroups.remove(implicitGroup);
    }

    public Set<ResourceGroup> getExplicitGroups() {
        return explicitGroups;
    }

    public void setExplicitGroups(Set<ResourceGroup> explicitGroups) {
        this.explicitGroups = explicitGroups;
    }

    public void addExplicitGroup(ResourceGroup explicitGroup) {
        this.explicitGroups.add(explicitGroup);
    }

    public void removeExplicitGroup(ResourceGroup explicitGroup) {
        this.explicitGroups.remove(explicitGroup);
    }

    @NotNull
    public List<ResourceOperationHistory> getOperationHistories() {
        if (this.operationHistories == null) {
            this.operationHistories = new ArrayList<ResourceOperationHistory>();
        }

        return operationHistories;
    }

    public void setOperationHistories(List<ResourceOperationHistory> operationHistories) {
        this.operationHistories = operationHistories;
    }

    /**
     * Returns the list of all errors of all types encountered by this resource. If you only want the errors of a
     * particular type, use {@link #getResourceErrors(ResourceErrorType)}.
     *
     * @return all errors (may be empty, but never <code>null</code>)
     */
    public List<ResourceError> getResourceErrors() {
        return resourceErrors;
    }

    /**
     * Returns only those errors of the given type.
     *
     * @param  type the type of errors that are to be returned
     *
     * @return list of errors that occurred on this resource that are of the given type (may be empty, but never <code>
     *         null</code>)
     */
    public List<ResourceError> getResourceErrors(ResourceErrorType type) {
        List<ResourceError> errors = new ArrayList<ResourceError>();

        for (ResourceError error : this.resourceErrors) {
            if (error.getErrorType() == type) {
                errors.add(error);
            }
        }

        return errors;
    }

    public void setResourceErrors(List<ResourceError> resourceErrors) {
        if (resourceErrors == null) {
            resourceErrors = new ArrayList<ResourceError>();
        }

        this.resourceErrors = resourceErrors;
    }

    public void addResourceError(ResourceError resourceError) {
        resourceError.setResource(this);
        this.resourceErrors.add(resourceError);
    }

    public List<Availability> getAvailability() {
        return availability;
    }

    public ResourceAvailability getCurrentAvailability() {
        return currentAvailability;
    }

    public void setCurrentAvailability(ResourceAvailability currentAvailability) {
        this.currentAvailability = currentAvailability;
    }

    /**
     * Returns the explicit mapping entities.
     *
     * @return the mapping entities
     *
     * @see    #getResources()
     */
    public Set<ResourceChannel> getResourceChannels() {
        return resourceChannels;
    }

    /**
     * The channels this resource is subscribed to.
     *
     * <p>The returned set is not backed by this entity - if you want to alter the set of associated channels, use
     * {@link #getResourceChannels()} or {@link #addChannel(Channel)}, {@link #removeChannel(Channel)}.</p>
     */
    public Set<Channel> getChannels() {
        HashSet<Channel> channels = new HashSet<Channel>();

        if (resourceChannels != null) {
            for (ResourceChannel rc : resourceChannels) {
                channels.add(rc.getResourceChannelPK().getChannel());
            }
        }

        return channels;
    }

    /**
     * Directly subscribe the resource to a channel.
     *
     * @param  channel
     *
     * @return the mapping that was added
     */
    public ResourceChannel addChannel(Channel channel) {
        if (this.resourceChannels == null) {
            this.resourceChannels = new HashSet<ResourceChannel>();
        }

        ResourceChannel mapping = new ResourceChannel(this, channel);
        this.resourceChannels.add(mapping);
        channel.addResource(this);
        return mapping;
    }

    /**
     * Unsubscribes the resource from a channel, if it exists. If it was already subscribed, the mapping that was
     * removed is returned; if not, <code>null</code> is returned.
     *
     * @param  channel the channel to unsubscribe from
     *
     * @return the mapping that was removed or <code>null</code> if the resource was not subscribed to the channel
     */
    public ResourceChannel removeChannel(Channel channel) {
        if ((this.resourceChannels == null) || (channel == null)) {
            return null;
        }

        ResourceChannel doomed = null;

        for (ResourceChannel rc : this.resourceChannels) {
            if (channel.equals(rc.getResourceChannelPK().getChannel())) {
                doomed = rc;
                channel.removeResource(this);
                break;
            }
        }

        if (doomed != null) {
            this.resourceChannels.remove(doomed);
        }

        return doomed;
    }

    public Set<InstalledPackage> getInstalledPackages() {
        return installedPackages;
    }

    public void addInstalledPackage(InstalledPackage installedPackage) {
        if (this.installedPackages == null) {
            this.installedPackages = new LinkedHashSet<InstalledPackage>();
        }

        this.installedPackages.add(installedPackage);
        installedPackage.setResource(this);
    }

    public void setInstalledPackages(Set<InstalledPackage> installedPackages) {
        this.installedPackages = installedPackages;
    }

    public List<InstalledPackageHistory> getInstalledPackageHistory() {
        return installedPackageHistory;
    }

    public void addInstalledPackageHistory(InstalledPackageHistory history) {
        if (this.installedPackageHistory == null) {
            installedPackageHistory = new ArrayList<InstalledPackageHistory>(1);
        }

        installedPackageHistory.add(history);
        history.setResource(this);
    }

    public void setInstalledPackageHistory(List<InstalledPackageHistory> installedPackageHistory) {
        this.installedPackageHistory = installedPackageHistory;
    }

    public Set<EventSource> getEventSources() {
        return eventSources;
    }

    public ProductVersion getProductVersion() {
        return productVersion;
    }

    public void setProductVersion(ProductVersion productVersion) {
        this.productVersion = productVersion;
    }

    public int compareTo(Resource that) {
        return this.name.compareTo(that.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || !(o instanceof Resource)) {
            return false;
        }

        final Resource resource = (Resource) o;

        if ((uuid != null) ? (!uuid.equals(resource.uuid)) : (resource.uuid != null)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return ((uuid != null) ? uuid.hashCode() : 0);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(Resource.class.getSimpleName()).append("[");
        buffer.append("id=").append(this.id);
        String typeName = (this.resourceType != null) ? this.resourceType.getName() : "<null>";
        buffer.append(", type=").append(typeName);
        buffer.append(", key=").append(this.resourceKey);
        buffer.append(", name=").append(this.name);
        String parentName;
        try
        {
            parentName = (this.parentResource != null) ? this.parentResource.getName() : "<null>";
        }
        catch (RuntimeException e)
        {
            // It may not be possible to get the parent name if this is a detached Entity on the Server side, since
            // this.parentResource is lazily fetched. NOTE: We can't specifically catch LazyInitializationException
            // here, since Hibernate classes do not exist on the Agent side.
            parentName = null;
        }
        if (parentName != null)
            buffer.append(", parent=").append(parentName);
        if (this.version != null && !this.version.equals(""))
            buffer.append(", version=").append(this.version);
        buffer.append("]");
        return buffer.toString();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        // Note that a Resource may have been constructed with id only. Check for uninitialized fields. 
        out.writeInt(id);
        out.writeUTF(uuid);
        out.writeUTF((null == resourceKey) ? "" : resourceKey);
        out.writeUTF((null == name) ? "" : name);
        out.writeInt(inventoryStatus.ordinal());
        out.writeUTF((null == version) ? "" : version);
        out.writeUTF((null == description) ? "" : description);
        out.writeLong(ctime);
        out.writeLong(mtime);
        out.writeLong(itime);

        //Subject modifiedBy;
        //Subject owner;
        //String location;
        out.writeObject(parentResource);

        if (null == resourceType) {
            out.writeUTF("");
            out.writeUTF("");
            out.writeObject(null);
        } else {
            out.writeUTF(resourceType.getName());
            out.writeUTF(resourceType.getPlugin());
            out.writeObject(resourceType.getCategory());
        }

        if (childResources != null && childResources.getClass().getName().contains("hibernate")) {
            out.writeObject(new LinkedHashSet<Resource>(childResources));
        } else {
            out.writeObject(childResources);
        }

        // Don't write plugin configs out if they are a lazy proxy
        if (pluginConfiguration != null && pluginConfiguration.getClass().getName().contains("hibernate")) {
            out.writeObject(null);
        } else {
            out.writeObject(pluginConfiguration);
        }

        //Set<MeasurementSchedule> schedules = new LinkedHashSet<MeasurementSchedule>();
        //Agent agent;
        //Set<ResourceGroup> resourceGroups = new HashSet<ResourceGroup>();
    }

    @SuppressWarnings("unchecked")
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        id = in.readInt();
        uuid = in.readUTF();
        resourceKey = in.readUTF();
        name = in.readUTF();
        inventoryStatus = InventoryStatus.values()[in.readInt()];
        version = in.readUTF();
        description = in.readUTF();
        ctime = in.readLong();
        mtime = in.readLong();
        itime = in.readLong();

        //Subject modifiedBy;
        //Subject owner;
        //String location;
        parentResource = (Resource) in.readObject();
        resourceType = new ResourceType(in.readUTF(), in.readUTF(), (ResourceCategory) in.readObject(), (parentResource != null) ? parentResource.getResourceType() : null);
        childResources = (Set<Resource>) in.readObject();

        pluginConfiguration = (Configuration) in.readObject();
    }

    public void afterUnmarshal(Unmarshaller u, Object parent) {
        this.parentResource = (Resource) parent;
    }

    // this should only ever be called once, during initial persistence
    public void initCurrentAvailability() {
        this.currentAvailability = new ResourceAvailability(this, null);
    }
}
