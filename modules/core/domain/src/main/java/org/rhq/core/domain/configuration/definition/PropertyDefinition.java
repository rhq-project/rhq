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
package org.rhq.core.domain.configuration.definition;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

import org.jetbrains.annotations.NotNull;

/**
 * Base type of the definition of a configuration property.
 *
 * @author Greg Hinkle
 */
@DiscriminatorColumn(name = "dtype")
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_CONFIG_PROP_DEF_ID_SEQ")
@Table(name = "RHQ_CONFIG_PROP_DEF")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso( { PropertyDefinitionSimple.class, PropertyDefinitionList.class, PropertyDefinitionMap.class })
@XmlRootElement
public abstract class PropertyDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "id")
    @GeneratedValue(generator = "SEQ", strategy = GenerationType.AUTO)
    @Id
    private int id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "description")
    private String description;

    @Column(name = "required")
    private boolean required;

    @Column(name = "READONLY")
    private boolean readOnly;

    @Column(name = "SUMMARY")
    private boolean summary;

    @Column(name = "activation_policy")
    @Enumerated(EnumType.STRING)
    private ActivationPolicy activationPolicy;

    @JoinColumn(name = "group_id")
    //    @ManyToOne(cascade = CascadeType.ALL, optional = true)
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH }, optional = true)
    private PropertyGroupDefinition propertyGroupDefinition;

    @Column(name = "order_index")
    private int order;

    @Column(name = "version")
    private String version;

    @JoinColumn(name = "config_def_id")
    @ManyToOne
    @XmlTransient
    private ConfigurationDefinition configurationDefinition;

    @OneToOne(mappedBy = "memberDefinition")
    @XmlTransient
    private PropertyDefinitionList parentPropertyListDefinition;

    @JoinColumn(name = "parent_map_definition_id")
    @ManyToOne
    @XmlTransient
    private PropertyDefinitionMap parentPropertyMapDefinition;

    protected PropertyDefinition() {
    }

    protected PropertyDefinition(@NotNull String name, String description, boolean required) {
        this.name = name;
        this.description = description;
        this.required = required;
    }

    public PropertyDefinitionMap getParentPropertyMapDefinition() {
        return parentPropertyMapDefinition;
    }

    public void setParentPropertyMapDefinition(PropertyDefinitionMap parentPropertyMapDefinition) {
        this.parentPropertyMapDefinition = parentPropertyMapDefinition;
    }

    public PropertyDefinitionList getParentPropertyListDefinition() {
        return parentPropertyListDefinition;
    }

    public void setParentPropertyListDefinition(PropertyDefinitionList parentPropertyListDefinition) {
        this.parentPropertyListDefinition = parentPropertyListDefinition;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isSummary() {
        return summary;
    }

    public void setSummary(boolean summary) {
        this.summary = summary;
    }

    public ActivationPolicy getActivationPolicy() {
        return activationPolicy;
    }

    public void setActivationPolicy(ActivationPolicy activationPolicy) {
        this.activationPolicy = activationPolicy;
    }

    public PropertyGroupDefinition getPropertyGroupDefinition() {
        return propertyGroupDefinition;
    }

    public void setPropertyGroupDefinition(PropertyGroupDefinition propertyGroupDefinition) {
        this.propertyGroupDefinition = propertyGroupDefinition;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        return configurationDefinition;
    }

    public void setConfigurationDefinition(ConfigurationDefinition configurationDefinition) {
        this.configurationDefinition = configurationDefinition;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[id=" + this.id + ", name=" + this.name
            + ((this.configurationDefinition != null) ? (", config=" + this.configurationDefinition.getName()) : "")
            + ((this.propertyGroupDefinition != null) ? (", group=" + this.propertyGroupDefinition.getName()) : "")
            + "]";
    }
}