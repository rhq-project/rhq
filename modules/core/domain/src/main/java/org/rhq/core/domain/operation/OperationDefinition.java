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
package org.rhq.core.domain.operation;

import java.io.Serializable;

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
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceType;

/**
 * The definition of an RHQ operation. An operation definition is associated with a particular physical resource type
 * (e.g. JBossAS server) and an optional resource version range.
 */
@Entity
@NamedQueries( //
{
    // find by type methods
    @NamedQuery(name = OperationDefinition.QUERY_FIND_BY_TYPE_AND_NAME, query = "" //
        + "   SELECT od " //
        + "     FROM OperationDefinition AS od " //
        + "    WHERE od.resourceType.id = :resourceTypeId " //
        + "      AND ( od.name = :operationName or :operationName IS NULL )" //
        + " ORDER BY od.displayName"),
    @NamedQuery(name = OperationDefinition.QUERY_FIND_LIGHT_WEIGHT_BY_TYPE_AND_NAME, query = "" //
        + "   SELECT new org.rhq.core.domain.operation.composite.OperationDefinitionLightWeight " //
        + "          ( od.id, od.name, od.resourceVersionRange, od.description, od.timeout, od.displayName ) " //
        + "     FROM OperationDefinition AS od " //
        + "    WHERE od.resourceType.id = :resourceTypeId " //
        + "      AND ( od.name = :operationName or :operationName IS NULL )" //
        + " ORDER BY od.displayName"),

    // find by resources methods
    @NamedQuery(name = OperationDefinition.QUERY_FIND_BY_RESOURCE_AND_NAME, query = "" //
        + "   SELECT od " //
        + "     FROM OperationDefinition AS od, Resource res " //
        + "    WHERE od.resourceType.id = res.resourceType.id " //
        + "      AND res.id = :resourceId " //
        + "      AND ( od.name = :operationName or :operationName IS NULL )" //
        + " ORDER BY od.displayName"),
    @NamedQuery(name = OperationDefinition.QUERY_FIND_LIGHT_WEIGHT_BY_RESOURCE_AND_NAME, query = "" //
        + "   SELECT new org.rhq.core.domain.operation.composite.OperationDefinitionLightWeight " //
        + "          ( od.id, od.name, od.resourceVersionRange, od.description, od.timeout, od.displayName ) " //
        + "     FROM OperationDefinition AS od, Resource res " //
        + "    WHERE od.resourceType.id = res.resourceType.id " //
        + "      AND res.id = :resourceId " //
        + "      AND ( od.name = :operationName or :operationName IS NULL )" //
        + " ORDER BY od.displayName"),

    // find by group methods
    @NamedQuery(name = OperationDefinition.QUERY_FIND_BY_GROUP_AND_NAME, query = "" //
        + "   SELECT od " //
        + "     FROM OperationDefinition AS od, ResourceGroup rg " //
        + "    WHERE od.resourceType.id = rg.resourceType.id " //
        + "      AND rg.id = :groupId " //
        + "      AND ( od.name = :operationName or :operationName IS NULL )" //
        + " ORDER BY od.displayName"),
    @NamedQuery(name = OperationDefinition.QUERY_FIND_LIGHT_WEIGHT_BY_GROUP_AND_NAME, query = "" //
        + "   SELECT new org.rhq.core.domain.operation.composite.OperationDefinitionLightWeight " //
        + "          ( od.id, od.name, od.resourceVersionRange, od.description, od.timeout, od.displayName ) " //
        + "     FROM OperationDefinition AS od, ResourceGroup rg " //
        + "    WHERE od.resourceType.id = rg.resourceType.id " //
        + "      AND rg.id = :groupId " //
        + "      AND ( od.name = :operationName or :operationName IS NULL )" //
        + " ORDER BY od.displayName") })
@SequenceGenerator(name = "id", sequenceName = "RHQ_OPERATION_DEF_ID_SEQ")
@Table(name = "RHQ_OPERATION_DEF")
public class OperationDefinition implements Serializable {
    public static final String QUERY_FIND_BY_TYPE_AND_NAME = "OperationDefinition.findByTypeAndName";
    public static final String QUERY_FIND_LIGHT_WEIGHT_BY_TYPE_AND_NAME = "OperationDefinition.findLightWeightByTypeAndName";

    public static final String QUERY_FIND_BY_RESOURCE_AND_NAME = "OperationDefinition.findByResourceAndName";
    public static final String QUERY_FIND_LIGHT_WEIGHT_BY_RESOURCE_AND_NAME = "OperationDefinition.findLightWeightByResourceAndName";

    public static final String QUERY_FIND_BY_GROUP_AND_NAME = "OperationDefinition.findByGroupAndName";
    public static final String QUERY_FIND_LIGHT_WEIGHT_BY_GROUP_AND_NAME = "OperationDefinition.findLightWeightByGroupAndName";

    private static final long serialVersionUID = 1L;

    /**
     * This defines the name of the simple parameter property that is used to define a specific timeout for a specific
     * operation invocation. If an operation invocation passes in a simple property with this name, its value must be an
     * integer specified in seconds and it defines how long the operation is given before it is timed out and assumed to
     * have failed. This parameter never shows up in the operation definition's metadata - it is a "special" parameter
     * that is only used internally. It can be missing entirely or it can be specified for any operation invocation.
     */
    public static final String TIMEOUT_PARAM_NAME = "rhq.timeout";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id")
    @Id
    private int id;

    @JoinColumn(name = "RESOURCE_TYPE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private ResourceType resourceType;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "RESOURCE_VERSION_RANGE")
    private String resourceVersionRange;

    @JoinColumn(name = "PARAMETER_CONFIG_DEF_ID", referencedColumnName = "ID")
    @OneToOne(cascade = CascadeType.ALL)
    private ConfigurationDefinition parametersConfigurationDefinition;

    @JoinColumn(name = "RESULTS_CONFIG_DEF_ID", referencedColumnName = "ID")
    @OneToOne(cascade = CascadeType.ALL)
    private ConfigurationDefinition resultsConfigurationDefinition;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "TIMEOUT")
    private Integer timeout;

    @Column(name = "DISPLAY_NAME")
    private String displayName;

    /* no-arg constructor required by EJB spec - not for use by subclasses */
    protected OperationDefinition() {
    }

    public OperationDefinition(@NotNull String name, String resourceVersionRange, String description) {
        this.name = name;
        this.resourceVersionRange = resourceVersionRange;
        this.description = description;
    }

    public OperationDefinition(ResourceType resourceType, @NotNull String name) {
        this.resourceType = resourceType;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
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

    public String getResourceVersionRange() {
        return resourceVersionRange;
    }

    public void setResourceVersionRange(String resourceVersionRange) {
        this.resourceVersionRange = resourceVersionRange;
    }

    public ConfigurationDefinition getParametersConfigurationDefinition() {
        return parametersConfigurationDefinition;
    }

    public void setParametersConfigurationDefinition(ConfigurationDefinition parametersConfigurationDefinition) {
        this.parametersConfigurationDefinition = parametersConfigurationDefinition;
    }

    public ConfigurationDefinition getResultsConfigurationDefinition() {
        return resultsConfigurationDefinition;
    }

    public void setResultsConfigurationDefinition(ConfigurationDefinition resultsConfigurationDefinition) {
        this.resultsConfigurationDefinition = resultsConfigurationDefinition;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Specifies the default timeout for any invocation of this operation, specified in seconds. Any specific invocation
     * can override this timeout if it passes in a simple property parameter named {@link #TIMEOUT_PARAM_NAME}.
     *
     * @return default timeout, or <code>null</code> if not defined
     */
    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        return "OperationDefinition[id=" + this.id + ", resourceType=" + this.resourceType + ", name=" + this.name
            + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof OperationDefinition)) {
            return false;
        }

        OperationDefinition that = (OperationDefinition) obj;
        return this.name.equals(that.name) && this.resourceType.equals(that.resourceType);
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = this.name.hashCode();
        result = (PRIME * result) + this.resourceType.hashCode();
        return result;
    }
}