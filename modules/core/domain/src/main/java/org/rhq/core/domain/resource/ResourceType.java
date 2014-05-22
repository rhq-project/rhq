/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.ResourceTypeBundleConfiguration;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.Summary;

/**
 * Defines a type of {@link Resource} (e.g. a Linux platform, a JBossAS server, or a Datasource service).
 * Unique business key (and therefore equals/hashCode basis) is the (String name, String plugin) combination.
 * This will keep plugin writers from stepping on each other's toes.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
@Entity
@Table(name = ResourceType.TABLE_NAME)
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_RESOURCE_TYPE_ID_SEQ", sequenceName = "RHQ_RESOURCE_TYPE_ID_SEQ")
@NamedQueries( {
    @NamedQuery(name = ResourceType.QUERY_GET_BUNDLE_CONFIG_BY_GROUP_ID, query = "SELECT rg.resourceType.bundleConfiguration FROM ResourceGroup rg WHERE rg.id = :groupId"),
    @NamedQuery(name = ResourceType.QUERY_FIND_BY_PLUGIN, query = "SELECT rt FROM ResourceType AS rt "
        + "WHERE rt.plugin = :plugin AND rt.deleted = false"),
    @NamedQuery(name = ResourceType.QUERY_FIND_BY_NAME_AND_PLUGIN, // TODO: QUERY: names are case-sensitive
    query = "SELECT rt FROM ResourceType AS rt WHERE LOWER(rt.name) = LOWER(:name) AND rt.plugin = :plugin "
        + "AND rt.deleted = false"),
    @NamedQuery(name = ResourceType.QUERY_FIND_ALL, query = "SELECT rt FROM ResourceType AS rt where rt.deleted = false"),
    @NamedQuery(name = ResourceType.QUERY_FIND_BY_PARENT_AND_NAME, // TODO: QUERY: Not looking up by the full key, get rid of this query
    query = "SELECT rt FROM ResourceType AS rt WHERE :parent MEMBER OF rt.parentResourceTypes AND rt.name = :name "
        + "AND rt.deleted = false"),

    /* authz'ed queries for ResourceTypeManagerBean */
    @NamedQuery(name = ResourceType.QUERY_FIND_CHILDREN, query = "SELECT c "
        + "FROM ResourceType rt JOIN rt.childResourceTypes c WHERE rt.id = :resourceTypeId AND rt.deleted = false "
        + "AND c.deleted = false"),
    @NamedQuery(name = ResourceType.FIND_CHILDREN_BY_PARENT, query = "SELECT DISTINCT rt FROM ResourceType AS rt "
        + "JOIN FETCH rt.parentResourceTypes AS pa " + // also fetch parents, as we need them later
        "WHERE rt.deleted = false and pa IN (:resourceType)"),
    @NamedQuery(name = ResourceType.FIND_ALL_TEMPLATE_COUNT_COMPOSITES, query = "" //
        + "SELECT new org.rhq.core.domain.resource.composite.ResourceTypeTemplateCountComposite" //
        + "(" //
        + "  rt," //
        + "  (SELECT COUNT(md) FROM MeasurementDefinition AS md WHERE md.resourceType = rt AND md.defaultOn = TRUE), "//
        + "  (SELECT COUNT(md) FROM MeasurementDefinition AS md WHERE md.resourceType = rt AND md.defaultOn = FALSE), "//
        + "  (SELECT COUNT(ad) FROM AlertDefinition AS ad WHERE ad.resourceType = rt AND ad.deleted = FALSE AND ad.enabled = TRUE), "//
        + "  (SELECT COUNT(ad) FROM AlertDefinition AS ad WHERE ad.resourceType = rt AND ad.deleted = FALSE AND ad.enabled = FALSE), "//
        + "  (SELECT COUNT(ddt) FROM DriftDefinitionTemplate AS ddt WHERE ddt.resourceType = rt AND ddt.isUserDefined = FALSE), "//
        + "  (SELECT COUNT(ddt) FROM DriftDefinitionTemplate AS ddt WHERE ddt.resourceType = rt AND ddt.isUserDefined = TRUE) "//
        + ")" //
        + "FROM ResourceType AS rt WHERE rt.deleted = false"),
    @NamedQuery(name = ResourceType.QUERY_FIND_BY_CATEGORY, query = "SELECT rt FROM ResourceType AS rt "
        + "WHERE rt.category = :category and rt.deleted = false"),
    @NamedQuery(name = ResourceType.QUERY_FIND_UTILIZED_BY_CATEGORY, query = "SELECT DISTINCT res.resourceType "
        + "FROM Resource res, IN (res.implicitGroups) g, IN (g.roles) r, IN (r.subjects) s " //
        + "WHERE s = :subject " //
        + "AND res.resourceType.category = :category " + "AND res.resourceType.deleted = false "
        + "AND (UPPER(res.name) LIKE :nameFilter ESCAPE :escapeChar OR :nameFilter is null) "
        + "AND (res.resourceType.plugin = :pluginName OR :pluginName is null) "
        + "AND (:inventoryStatus = res.inventoryStatus OR :inventoryStatus is null) "
        + "ORDER BY res.resourceType.name "),
    @NamedQuery(name = ResourceType.QUERY_FIND_UTILIZED_BY_CATEGORY_admin, query = "SELECT DISTINCT res.resourceType "
        + "FROM Resource res " //
        + "WHERE res.resourceType.category = :category "
        + "AND (UPPER(res.name) LIKE :nameFilter ESCAPE :escapeChar OR :nameFilter is null) "
        + "AND res.resourceType.deleted = false "
        + "AND (res.resourceType.plugin = :pluginName OR :pluginName is null) "
        + "AND (:inventoryStatus = res.inventoryStatus OR :inventoryStatus is null) "
        + "ORDER BY res.resourceType.name "),
    @NamedQuery(name = ResourceType.QUERY_FIND_UTILIZED_CHILDREN_BY_CATEGORY, query = "SELECT DISTINCT res.resourceType "
        + "FROM Resource res, IN (res.implicitGroups) g, IN (g.roles) r, IN (r.subjects) s "
        + "WHERE s = :subject "
        + "AND res.parentResource = :parentResource "
        + "AND res.resourceType.category = :category "
        + "AND res.resourceType.deleted = false "
        + "AND (:inventoryStatus = res.inventoryStatus OR :inventoryStatus is null) "),
    @NamedQuery(name = ResourceType.QUERY_FIND_UTILIZED_CHILDREN_BY_CATEGORY_admin, query = "SELECT DISTINCT res.resourceType "
        + "FROM Resource res "
        + "WHERE res.parentResource = :parentResource "
        + "AND res.resourceType.deleted = false "
        + "AND res.resourceType.category = :category "
        + "AND (:inventoryStatus = res.inventoryStatus OR :inventoryStatus is null) "),
    @NamedQuery(name = ResourceType.QUERY_FIND_BY_RESOURCE_GROUP, query = "" //
        + "SELECT DISTINCT rt " //
        + "  FROM ResourceGroup rg " //
        + "  JOIN rg.resourceType rt" //
        + "  JOIN rg.roles r JOIN r.subjects s " //
        + " WHERE s = :subject " //
        + "   AND rt.deleted = false " + "   AND ( rt.plugin = :pluginName OR :pluginName is null ) "),
    @NamedQuery(name = ResourceType.QUERY_FIND_BY_RESOURCE_GROUP_admin, query = "" //
        + "SELECT DISTINCT rt " //
        + "  FROM ResourceGroup rg " //
        + "  JOIN rg.resourceType rt" //
        + " WHERE rt.deleted = false AND ( rt.plugin = :pluginName OR :pluginName is null ) "),
    @NamedQuery(name = ResourceType.QUERY_GET_EXPLICIT_RESOURCE_TYPE_COUNTS_BY_GROUP, query = "" //
        + "SELECT type.id, type.name, COUNT(type.id) " //
        + "  FROM ResourceGroup rg JOIN rg.explicitResources res JOIN res.resourceType type " //
        + " WHERE rg.id = :groupId AND res.resourceType.deleted = false AND res.inventoryStatus = 'COMMITTED' " //
        + " GROUP BY type.id, type.name "),
    @NamedQuery(name = ResourceType.QUERY_GET_IMPLICIT_RESOURCE_TYPE_COUNTS_BY_GROUP, query = "" //
        + "SELECT type.id, type.name, COUNT(type.id) " //
        + "  FROM ResourceGroup rg JOIN rg.implicitResources res JOIN res.resourceType type " //
        + " WHERE rg.id = :groupId AND res.resourceType.deleted = false AND res.inventoryStatus = 'COMMITTED' " //
        + " GROUP BY type.id, type.name "),
    @NamedQuery(name = ResourceType.QUERY_FIND_BY_ID_WITH_ALL_OPERATIONS, query = "SELECT DISTINCT rt "
        + "FROM ResourceType rt " + "LEFT JOIN FETCH rt.operationDefinitions def "
        + "LEFT JOIN FETCH def.parametersConfigurationDefinition psDef "
        + "LEFT JOIN FETCH def.resultsConfigurationDefinition rcDef " + "WHERE rt.id = :id AND rt.deleted = false"),
    @NamedQuery(name = ResourceType.QUERY_FIND_RESOURCE_FACETS, query = "" //
        + "SELECT new org.rhq.core.domain.resource.composite.ResourceFacets " //
        + "       ( " //
        + "         rt.id," // the resourceTypeId
        + "         (SELECT COUNT(metricDef) FROM rt.metricDefinitions metricDef)," // measurement
        + "         (SELECT COUNT(eventDef) FROM rt.eventDefinitions eventDef)," // event
        + "         (SELECT COUNT(pluginConfig) FROM rt.pluginConfigurationDefinition pluginConfig)," // pluginConfiguration
        + "         (SELECT COUNT(resConfig) FROM rt.resourceConfigurationDefinition resConfig)," // configuration
        + "         (SELECT COUNT(operationDef) FROM rt.operationDefinitions operationDef)," // operation
        + "         (SELECT COUNT(packageType) FROM rt.packageTypes packageType)," // content
        + "         (SELECT COUNT(metricDef) FROM rt.metricDefinitions metricDef WHERE metricDef.dataType = 3)," // calltime
        + "         (SELECT COUNT(propDef) FROM rt.pluginConfigurationDefinition pluginConfig JOIN pluginConfig.propertyDefinitions propDef WHERE propDef.name = 'snapshotLogEnabled')," //
        + "         (SELECT COUNT(driftDef) FROM rt.driftDefinitionTemplates driftDef)," // drift
        + "         (SELECT COUNT(bundleConfig) FROM rt.bundleConfiguration bundleConfig)" // bundle
        + "       ) " //
        + "  FROM ResourceType rt " //
        + " WHERE rt.deleted = false AND ( rt.id = :resourceTypeId OR :resourceTypeId IS NULL )"),
    @NamedQuery(name = ResourceType.QUERY_FIND_DUPLICATE_TYPE_NAMES, query = "" //
        + "  SELECT rt.name " //
        + "  FROM ResourceType rt " //
        + "  WHERE rt.deleted = false " + "  GROUP BY rt.name " //
        + "  HAVING COUNT(rt.name) > 1"), //
    @NamedQuery(name = ResourceType.QUERY_DYNAMIC_CONFIG_WITH_PLUGIN, query = "" //
        + "SELECT rt.plugin || ' - ' || rt.name, rt.plugin || '-' || rt.name FROM ResourceType rt WHERE rt.deleted = false"), //
    @NamedQuery(name = ResourceType.QUERY_MARK_TYPES_DELETED, query = "UPDATE ResourceType t SET t.deleted = true WHERE t.id IN (:resourceTypeIds)"),
    @NamedQuery(name = ResourceType.QUERY_FIND_IDS_BY_PLUGIN, query = "SELECT t.id FROM ResourceType t WHERE t.plugin = :plugin AND t.deleted = false"),
    @NamedQuery(name = ResourceType.QUERY_FIND_COUNT_BY_PLUGIN, query = "SELECT COUNT(t) FROM ResourceType t WHERE t.plugin = :plugin AND t.deleted = false") })
@NamedNativeQueries( {
    // TODO: Add authz conditions to the below query.
    @NamedNativeQuery(name = ResourceType.QUERY_FIND_CHILDREN_BY_CATEGORY, query = "" //
        + "(SELECT crt.id, crt.name, crt.category, crt.creation_data_type, crt.create_delete_policy, crt.singleton, crt.supports_manual_add, crt.description, crt.plugin, crt.ctime, crt.mtime, crt.deleted, crt.subcategory, crt.plugin_config_def_id, crt.res_config_def_id "
        + "FROM RHQ_resource_type crt, RHQ_resource res, RHQ_resource_type rt, RHQ_resource_type_parents rtp "
        + "WHERE res.id = ? "
        + "AND crt.deleted = false "
        + "AND res.resource_type_id = rt.id "
        + "AND rt.id = rtp.parent_resource_type_id "
        + "AND rtp.resource_type_id = crt.id "
        + "AND crt.category = ? "
        +
        //               "ORDER BY crt.name " +
        "UNION "
        + "SELECT DISTINCT crt2.id, crt2.name, crt2.category, crt2.creation_data_type, crt2.create_delete_policy, crt2.singleton, crt2.supports_manual_add, crt2.description, crt2.plugin, crt2.ctime, crt2.mtime, crt2.deleted, crt2.subcategory, crt2.plugin_config_def_id, crt2.res_config_def_id "
        + "FROM RHQ_resource_type crt2 " + "WHERE 1 = "
        + "(SELECT COUNT(res2.id) "
        + "FROM RHQ_resource res2, RHQ_resource_type rt2 "
        + "WHERE res2.id = ? "
        + "AND res2.resource_type_id = rt2.id " + "AND rt2.category = 'PLATFORM') "
        + "AND 0 = "
        + "(SELECT COUNT(rtp2.resource_type_id) "
        + "FROM RHQ_resource_type_parents rtp2 "
        + "WHERE rtp2.resource_type_id = crt2.id) " + "AND crt2.deleted = false "
        + "AND crt2.category = ? "
        + " ) ORDER BY name", resultSetMapping = ResourceType.MAPPING_FIND_CHILDREN_BY_CATEGORY),
    @NamedNativeQuery(name = ResourceType.QUERY_FIND_CHILDREN_BY_CATEGORY_admin, query = "" //
        + "(SELECT crt.id, crt.name, crt.category, crt.creation_data_type, crt.create_delete_policy, crt.singleton, crt.supports_manual_add, crt.description, crt.plugin, crt.ctime, crt.mtime, crt.deleted, crt.subcategory, crt.plugin_config_def_id, crt.res_config_def_id "
        + "FROM RHQ_resource_type crt, RHQ_resource res, RHQ_resource_type rt, RHQ_resource_type_parents rtp "
        + "WHERE res.id = ? "
        + "AND crt.deleted = false "
        + "AND res.resource_type_id = rt.id "
        + "AND rt.id = rtp.parent_resource_type_id "
        + "AND rtp.resource_type_id = crt.id "
        + "AND crt.category = ? "
        +
        //               "ORDER BY crt.name " +
        "UNION "
        + "(SELECT DISTINCT crt2.id, crt2.name, crt2.category, crt2.creation_data_type, crt2.create_delete_policy, crt2.singleton, crt2.supports_manual_add, crt2.description, crt2.plugin, crt2.ctime, crt2.mtime, crt2.deleted, crt2.subcategory, crt2.plugin_config_def_id, crt2.res_config_def_id "
        + "FROM RHQ_resource_type crt2 " + "WHERE 1 = "
        + "(SELECT COUNT(res2.id) "
        + "FROM RHQ_resource res2, RHQ_resource_type rt2 "
        + "WHERE res2.id = ? "
        + "AND res2.resource_type_id = rt2.id " + "AND rt2.category = 'PLATFORM') "
        + "AND 0 = "
        + "(SELECT COUNT(rtp2.resource_type_id) "
        + "FROM RHQ_resource_type_parents rtp2 "
        + "WHERE rtp2.resource_type_id = crt2.id) " + "AND crt2.category = ?" +
        //               "ORDER BY crt2.name" +
        ")) ORDER BY name", resultSetMapping = ResourceType.MAPPING_FIND_CHILDREN_BY_CATEGORY) //
})
@SqlResultSetMapping(name = ResourceType.MAPPING_FIND_CHILDREN_BY_CATEGORY, entities = { @EntityResult(entityClass = ResourceType.class) })
// @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class ResourceType implements Serializable, Comparable<ResourceType> {
    private static final long serialVersionUID = 4L;

    public static final String TABLE_NAME = "RHQ_RESOURCE_TYPE";

    public static final ResourceType ANY_PLATFORM_TYPE = null;

    public static final String QUERY_GET_BUNDLE_CONFIG_BY_GROUP_ID = "ResourceType.getBundleConfigByGroupResourceType";
    public static final String QUERY_GET_EXPLICIT_RESOURCE_TYPE_COUNTS_BY_GROUP = "ResourceType.getExplicitResourceTypeCountsByGroup";
    public static final String QUERY_GET_IMPLICIT_RESOURCE_TYPE_COUNTS_BY_GROUP = "ResourceType.getImplicitResourceTypeCountsByGroup";
    public static final String QUERY_FIND_BY_NAME_AND_PLUGIN = "ResourceType.findByNameAndPlugin";
    public static final String QUERY_FIND_BY_PLUGIN = "ResourceType.findByPlugin";
    public static final String QUERY_FIND_BY_PARENT_AND_NAME = "ResourceType.findByParentAndName";
    public static final String QUERY_FIND_ALL = "ResourceType.findAll";
    public static final String QUERY_FIND_BY_ID_WITH_ALL_OPERATIONS = "ResourceType.findByIdWithAllOperations";
    public static final String QUERY_FIND_BY_CATEGORY = "ResourceType.findByCategory";
    public static final String QUERY_FIND_CHILDREN = "ResourceType.findChildren";
    /** find child resource types for resource :parentResource and category :category */
    public static final String QUERY_FIND_CHILDREN_BY_CATEGORY = "ResourceType.findChildrenByCategory";
    public static final String QUERY_FIND_CHILDREN_BY_CATEGORY_admin = "ResourceType.findChildrenByCategory_admin";
    /** find utilized (i.e. represented in inventory) child resource types for resource :parentResource and category :category */
    public static final String QUERY_FIND_UTILIZED_CHILDREN_BY_CATEGORY = "ResourceType.findUtilizedChildrenByCategory";
    public static final String QUERY_FIND_UTILIZED_CHILDREN_BY_CATEGORY_admin = "ResourceType.findUtilizedChildrenByCategory_admin";
    /** find child resource types for the resource type passed in :resourceType */
    public static final String FIND_CHILDREN_BY_PARENT = "ResourceType.findChildrenByParent";
    public static final String FIND_ALL_TEMPLATE_COUNT_COMPOSITES = "ResourceType.findAllTemplateCountComposites";
    public static final String QUERY_FIND_UTILIZED_BY_CATEGORY = "ResourceType.findUtilizedByCategory";
    public static final String QUERY_FIND_UTILIZED_BY_CATEGORY_admin = "ResourceType.findUtilizedByCategory_admin";
    public static final String QUERY_FIND_BY_RESOURCE_GROUP = "ResourceType.findByResourceGroup";
    public static final String QUERY_FIND_BY_RESOURCE_GROUP_admin = "ResourceType.findByResourceGroup_admin";

    public static final String MAPPING_FIND_CHILDREN_BY_CATEGORY = "ResourceType.findChildrenByCategoryMapping";
    public static final String QUERY_FIND_RESOURCE_FACETS = "ResourceType.findResourceFacets";
    public static final String QUERY_FIND_DUPLICATE_TYPE_NAMES = "ResourceType.findDuplicateTypeNames";

    public static final String QUERY_DYNAMIC_CONFIG_WITH_PLUGIN = "ResourceType.dynamicConfigWithPlugin";

    public static final String QUERY_MARK_TYPES_DELETED = "ResourceType.markTypesDeleted";

    public static final String QUERY_FIND_IDS_BY_PLUGIN = "ResourceType.findIdsByPlugin";
    public static final String QUERY_FIND_COUNT_BY_PLUGIN = "ResourceType.findCountByPlugin";

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_RESOURCE_TYPE_ID_SEQ")
    @Summary(index = 0)
    private int id;

    @Column(name = "NAME", nullable = false)
    @Summary(index = 1)
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "CATEGORY", nullable = false)
    @Enumerated(EnumType.STRING)
    @Summary(index = 2)
    private ResourceCategory category;

    @Column(name = "CREATION_DATA_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceCreationDataType creationDataType = ResourceCreationDataType.CONFIGURATION;

    @Column(name = "CREATE_DELETE_POLICY", nullable = false)
    @Enumerated(EnumType.STRING)
    private CreateDeletePolicy createDeletePolicy = CreateDeletePolicy.BOTH;

    @Column(name = "SUPPORTS_MANUAL_ADD", nullable = false)
    private boolean supportsManualAdd;

    @Column(name = "SINGLETON", nullable = false)
    private boolean singleton;

    @Column(name = "PLUGIN", nullable = false)
    @Summary(index = 3)
    private String plugin;

    @Column(name = "CTIME")
    private Long ctime;

    @Column(name = "MTIME")
    private Long mtime;

    @Column(name = "DELETED")
    private boolean deleted;

    @Column(name = "IGNORED")
    private boolean ignored;

    @Column(name = "UNINVENTORY_MISSING")
    private boolean uninventoryMissing;

    @ManyToMany(mappedBy = "parentResourceTypes", cascade = { CascadeType.REFRESH })
    @OrderBy
    //@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
    private Set<ResourceType> childResourceTypes;

    @ManyToMany(cascade = CascadeType.PERSIST)
    // persist so self-injecting plugins work
    @JoinTable(name = "RHQ_RESOURCE_TYPE_PARENTS", joinColumns = { @JoinColumn(name = "RESOURCE_TYPE_ID") }, inverseJoinColumns = { @JoinColumn(name = "PARENT_RESOURCE_TYPE_ID") })
    @OrderBy
    //@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
    private Set<ResourceType> parentResourceTypes;

    @JoinColumn(name = "PLUGIN_CONFIG_DEF_ID", nullable = true)
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    //@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
    private ConfigurationDefinition pluginConfigurationDefinition;

    @JoinColumn(name = "RES_CONFIG_DEF_ID", nullable = true)
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    //@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
    private ConfigurationDefinition resourceConfigurationDefinition;

    @Column(name = "SUBCATEGORY")
    private String subCategory;

    @OneToMany(mappedBy = "resourceType", cascade = CascadeType.ALL)
    @OrderBy
    // primary key
    //@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
    private Set<MeasurementDefinition> metricDefinitions = new LinkedHashSet<MeasurementDefinition>();

    @OneToMany(mappedBy = "resourceType", cascade = CascadeType.ALL)
    @OrderBy
    // primary key
    //@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
    private Set<EventDefinition> eventDefinitions;

    @OneToMany(mappedBy = "resourceType", cascade = CascadeType.ALL)
    @OrderBy
    // primary key
    private Set<OperationDefinition> operationDefinitions;

    @JoinColumn(name = "RESOURCE_TYPE_ID")
    @OneToMany(cascade = CascadeType.ALL)
    private Set<ProcessScan> processScans;

    @OneToMany(mappedBy = "resourceType", cascade = CascadeType.ALL)
    private Set<PackageType> packageTypes;

    @OneToMany(mappedBy = "resourceType", cascade = CascadeType.REMOVE)
    private List<Resource> resources;

    @OneToMany(mappedBy = "resourceType", cascade = CascadeType.REMOVE)
    private List<ResourceGroup> resourceGroups;

    @OneToMany(mappedBy = "resourceType", cascade = CascadeType.ALL)
    private Set<ProductVersion> productVersions;

    @OneToOne(mappedBy = "resourceType", fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
    private BundleType bundleType;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "resourceType")
    private Set<DriftDefinitionTemplate> driftDefinitionTemplates;

    // note that this is mapped to a Configuration entity, which is what it really is. However, our getter/setter
    // only provides access to this via ResourceTypeBundleConfiguration to encapsulate the innards of this implementation
    // detail, exposing only the more strongly typed methods to obtain bundle-related config properties
    @JoinColumn(name = "BUNDLE_CONFIG_ID", nullable = true)
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private Configuration bundleConfiguration;

    @Transient
    private transient String helpText;

    @Transient
    private transient String helpTextContentType;

    @Transient
    private transient ClassLoaderType classLoaderType;

    /* no-arg constructor required by EJB spec and Externalizable (Externalizable also requires it to be public) */
    public ResourceType() {
        // Intentionally left blank
    }

    public ResourceType(String name, String plugin, ResourceCategory category, ResourceType parentResourceType) {
        if (name == null) {
            throw new IllegalArgumentException("name==null");
        }
        if (plugin == null) {
            throw new IllegalArgumentException("plugin==null");
        }

        // Initialize empty ordered lists...
        this.childResourceTypes = null;
        this.parentResourceTypes = new HashSet<ResourceType>(1);
        this.metricDefinitions = new LinkedHashSet<MeasurementDefinition>();

        this.name = name;
        this.category = category;
        this.plugin = plugin;
        this.mtime = this.ctime = System.currentTimeMillis();

        if (parentResourceType != null) {
            parentResourceType.addChildResourceType(this);
        }
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

    public ResourceCategory getCategory() {
        return this.category;
    }

    public void setCategory(ResourceCategory category) {
        this.category = category;
    }

    public ResourceCreationDataType getCreationDataType() {
        return creationDataType;
    }

    public void setCreationDataType(ResourceCreationDataType creationDataType) {
        if (creationDataType == null)
            throw new IllegalArgumentException("creationDataType cannot be null");

        this.creationDataType = creationDataType;
    }

    public CreateDeletePolicy getCreateDeletePolicy() {
        return createDeletePolicy;
    }

    public void setCreateDeletePolicy(CreateDeletePolicy createDeletePolicy) {
        if (createDeletePolicy == null)
            throw new IllegalArgumentException("createDeletePolicy cannot be null");

        this.createDeletePolicy = createDeletePolicy;
    }

    public boolean isCreatable() {
        return (createDeletePolicy == CreateDeletePolicy.BOTH || createDeletePolicy == CreateDeletePolicy.CREATE_ONLY);
    }

    public boolean isDeletable() {
        return (createDeletePolicy == CreateDeletePolicy.BOTH || createDeletePolicy == CreateDeletePolicy.DELETE_ONLY);
    }

    /**
     * Returns the resource subcategory, if any, which this ResourceType
     * has been tagged with. If the ResourceType has not been tagged with
     * a subcategory, <code>null</code> is returned.
     */
    public String getSubCategory() {
        return this.subCategory;
    }

    /**
     * Tags this ResourceType as being part of the specified subcategory
     */
    public void setSubCategory(String subcategory) {
        this.subCategory = subcategory;
    }

    /**
     * If true, this resource may only ever have one discovered instance per parent resource.
     * @return true if this is a singleton resource
     */
    public boolean isSingleton() {
        return singleton;
    }

    /**
     * @param singleton true if there is only ever one discovered instance per parent resource
     */
    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    public boolean isSupportsManualAdd() {
        return supportsManualAdd;
    }

    public void setSupportsManualAdd(boolean supportsManualAdd) {
        this.supportsManualAdd = supportsManualAdd;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPlugin() {
        return this.plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
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

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    public boolean isUninventoryMissing() {
        return uninventoryMissing;
    }

    public void setUninventoryMissing(boolean uninventoryMissing) {
        this.uninventoryMissing = uninventoryMissing;
    }

    @PreUpdate
    void onUpdate() {
        this.mtime = System.currentTimeMillis();
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public List<ResourceGroup> getResourceGroups() {
        return resourceGroups;
    }

    public void setResourceGroups(List<ResourceGroup> resourceGroups) {
        this.resourceGroups = resourceGroups;
    }

    public Set<ResourceType> getParentResourceTypes() {
        return this.parentResourceTypes;
    }

    public void setParentResourceTypes(Set<ResourceType> parentResourceTypes) {
        this.parentResourceTypes = parentResourceTypes;
    }

    /**
     * Makes this resource type a child of the given parent resource type.
     * @param parentResourceType
     */
    public void addParentResourceType(ResourceType parentResourceType) {
        if (parentResourceType.childResourceTypes==null || parentResourceType.childResourceTypes.equals(Collections.emptySet())) {
            parentResourceType.childResourceTypes = new HashSet<ResourceType>(1);
        }
        parentResourceType.childResourceTypes.add(this);
        this.parentResourceTypes.add(parentResourceType);
    }

    /**
     * Removes the given resource type as a parent of this resource type.
     * @param oldParentResourceType
     */
    public void removeParentResourceType(ResourceType oldParentResourceType) {
        oldParentResourceType.childResourceTypes.remove(this);
        this.parentResourceTypes.remove(oldParentResourceType);
    }

    public Set<ResourceType> getChildResourceTypes() {
        if (this.childResourceTypes==null) {
            return Collections.emptySet();
        }
        return this.childResourceTypes;
    }

    /**
     * Makes this resource type a parent of the given child resource type.
     * @param childResourceType
     */
    public void addChildResourceType(ResourceType childResourceType) {
        if (this.childResourceTypes==null) {
            childResourceTypes = new LinkedHashSet<ResourceType>(1);
        }
        childResourceType.parentResourceTypes.add(this);
        this.childResourceTypes.add(childResourceType);
    }

    /**
     * Removes the given resource type as a child of this resource type.
     * @param oldChildResourceType
     */
    public void removeChildResourceType(ResourceType oldChildResourceType) {
        if (this.childResourceTypes==null) {
            return;
        }
        oldChildResourceType.parentResourceTypes.remove(this);
        this.childResourceTypes.remove(oldChildResourceType);
        if (this.childResourceTypes.isEmpty()) {
            this.childResourceTypes=null;
    }
    }

    public void setChildResourceTypes(Set<ResourceType> childResourceTypes) {
        this.childResourceTypes = childResourceTypes;
    }

    public ConfigurationDefinition getPluginConfigurationDefinition() {
        return pluginConfigurationDefinition;
    }

    public void setPluginConfigurationDefinition(ConfigurationDefinition pluginConfigurationDefinition) {
        this.pluginConfigurationDefinition = pluginConfigurationDefinition;
    }

    public ConfigurationDefinition getResourceConfigurationDefinition() {
        return resourceConfigurationDefinition;
    }

    public void setResourceConfigurationDefinition(ConfigurationDefinition resourceConfigurationDefinition) {
        this.resourceConfigurationDefinition = resourceConfigurationDefinition;
    }

    public ResourceTypeBundleConfiguration getResourceTypeBundleConfiguration() {
        if (this.bundleConfiguration == null) {
            return null;
        } else {
            return new ResourceTypeBundleConfiguration(bundleConfiguration);
        }
    }

    public void setResourceTypeBundleConfiguration(ResourceTypeBundleConfiguration rtbc) {
        if (rtbc == null) {
            this.bundleConfiguration = null;
        } else {
            this.bundleConfiguration = rtbc.getBundleConfiguration();
        }
    }

    @XmlTransient
    public Set<MeasurementDefinition> getMetricDefinitions() {
        return metricDefinitions;
    }

    public void setMetricDefinitions(Set<MeasurementDefinition> metricDefinitions) {
        this.metricDefinitions = metricDefinitions;
    }

    public boolean addMetricDefinition(MeasurementDefinition metricDef) {
        metricDef.setResourceType(this);
        return this.metricDefinitions.add(metricDef);
    }

    @XmlTransient
    public Set<EventDefinition> getEventDefinitions() {
        if (eventDefinitions==null) {
            return Collections.emptySet();
        }
        return eventDefinitions;
    }

    public void setEventDefinitions(Set<EventDefinition> eventDefinitions) {
        this.eventDefinitions = eventDefinitions;
    }

    public void addEventDefinition(EventDefinition eventDefinition) {
        if (this.eventDefinitions==null) {
            this.eventDefinitions = new HashSet<EventDefinition>(1);
        }
        this.eventDefinitions.add(eventDefinition);
    }

    public Set<OperationDefinition> getOperationDefinitions() {
        if (operationDefinitions==null) {
            return Collections.emptySet();
        }
        return operationDefinitions;
    }

    public void setOperationDefinitions(Set<OperationDefinition> operationDefinitions) {
        this.operationDefinitions = operationDefinitions;
    }

    public boolean addOperationDefinition(OperationDefinition operationDefinition) {
        if (operationDefinitions==null) {
            operationDefinitions = new LinkedHashSet<OperationDefinition>(1);
        }
        operationDefinition.setResourceType(this);
        return this.operationDefinitions.add(operationDefinition);
    }

    public Set<ProcessScan> getProcessScans() {
        if (processScans==null) {
            return Collections.emptySet();
        }
        return this.processScans;
    }

    public void setProcessScans(Set<ProcessScan> processScans) {
        this.processScans = processScans;
    }

    public boolean addProcessScan(ProcessScan processMatch) {
        // this is unidirection - no need to set this resource this on process match

        if (this.processScans == null) {
            this.processScans = new HashSet<ProcessScan>(1);
        }
        return this.processScans.add(processMatch);
    }

    public Set<PackageType> getPackageTypes() {
        if (packageTypes==null) {
            return Collections.emptySet();
        }
        return packageTypes;
    }

    public void setPackageTypes(Set<PackageType> packageTypes) {
        this.packageTypes = packageTypes;
    }

    public void addPackageType(PackageType packageType) {
        if (packageTypes==null) {
            packageTypes= new HashSet<PackageType>(1);
        }
        packageType.setResourceType(this);
        packageTypes.add(packageType);
    }

    public void removePackageType(PackageType packageType) {
        packageTypes.remove(packageType);
        packageType.setResourceType(null);
        if (packageTypes.isEmpty()) {
            packageTypes=null;
    }
    }

    public Set<ProductVersion> getProductVersions() {
        if (productVersions==null) {
            return Collections.emptySet();
        }
        return productVersions;
    }

    public void setProductVersions(Set<ProductVersion> productVersions) {
        this.productVersions = productVersions;
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public String getHelpTextContentType() {
        return helpTextContentType;
    }

    public void setHelpTextContentType(String helpTextContentType) {
        this.helpTextContentType = helpTextContentType;
    }

    public ClassLoaderType getClassLoaderType() {
        return classLoaderType;
    }

    public void setClassLoaderType(ClassLoaderType classLoaderType) {
        this.classLoaderType = classLoaderType;
    }

    public BundleType getBundleType() {
        return this.bundleType;
    }

    public void setBundleType(BundleType bundleType) {
        this.bundleType = bundleType;
    }

    public Set<DriftDefinitionTemplate> getDriftDefinitionTemplates() {
        if (driftDefinitionTemplates==null) {
            return Collections.emptySet();
        }
        return driftDefinitionTemplates;
    }

    public void addDriftDefinitionTemplate(DriftDefinitionTemplate template) {
        if (driftDefinitionTemplates == null) {
            driftDefinitionTemplates = new HashSet<DriftDefinitionTemplate>(1);
        }
        template.setResourceType(this);
        driftDefinitionTemplates.add(template);
    }

    public void setDriftDefinitionTemplates(Set<DriftDefinitionTemplate> driftDefinitionTemplates) {
        this.driftDefinitionTemplates = driftDefinitionTemplates;
    }

    @Deprecated
    public List<ResourceSubCategory> getChildSubCategories() {
        return null;
    }

    @Deprecated
    public void setChildSubCategories(List<ResourceSubCategory> subCategories) {
    }

    @Deprecated
    public void addChildSubCategory(ResourceSubCategory subCategory) {
    }

    // NOTE: It's vital that compareTo() is consistent with equals(), otherwise TreeSets containing ResourceTypes, or
    //       TreeMaps with ResourceTypes as keys, will not work reliably. See the Javadoc for Comparable for a precise
    //       definition of "consistent with equals()".
    @Override
    public int compareTo(ResourceType that) {
        if (this.name == null) {
            return (that.name == null) ? 0 : -1;
        }
        int result = (that.name == null) ? 1 : this.name.compareTo(that.name);
        if (result != 0) {
            return result;
        }
        if (this.plugin == null) {
            return (that.plugin == null) ? 0 : -1;
        }
        return (that.plugin == null) ? 1 : this.plugin.compareTo(that.plugin);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || !(obj instanceof ResourceType))
            return false;
        ResourceType that = (ResourceType) obj;
        if (this.name != null ? !this.name.equals(that.name) : that.name != null)
            return false;
        if (this.plugin != null ? !this.plugin.equals(that.plugin) : that.plugin != null)
            return false;
        //only compare id's if they've both been set
        if (this.id != 0 && that.id != 0 && this.id != that.id)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result;
        if (name != null && this.plugin != null) {
            result = (this.name != null ? this.name.hashCode() : 0);
            result = 31 * result + (this.plugin != null ? plugin.hashCode() : 0);
        } else {
            result = 31 * id;
        }
        return result;
    }

    @Override
    public String toString() {
        return "ResourceType[id=" + this.id  + ", name=" + this.name + ", plugin=" + this.plugin +
            ", category=" + this.category + "]";
    }

}
