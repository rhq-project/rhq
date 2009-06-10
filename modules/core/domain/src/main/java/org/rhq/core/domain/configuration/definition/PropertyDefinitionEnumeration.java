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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.jetbrains.annotations.NotNull;

/**
 * @author Jason Dobies
 */
@Entity
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_CONF_PROP_DEF_ENUM_ID_SEQ")
@Table(name = "RHQ_CONF_PROP_DEF_ENUM")
public class PropertyDefinitionEnumeration implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private long id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "STRING_VALUE", nullable = false)
    private String value;

    @Column(name = "ORDER_INDEX", nullable = false)
    private int orderIndex;

    @Column(name = "IS_DEFAULT")
    private boolean isDefault;

    @JoinColumn(name = "PROPERTY_DEF_ID")
    @ManyToOne
    private PropertyDefinitionSimple propertyDefinitionSimple;

    protected PropertyDefinitionEnumeration() {
        // empty constructor, JPA use only
    }

    public PropertyDefinitionEnumeration(@NotNull String name, String value) {
        this(name, value, false);
    }

    public PropertyDefinitionEnumeration(@NotNull String name, String value, boolean isDefault) {
        this.name = name;
        this.value = value;
        this.isDefault = isDefault;
    }

    @PrePersist
    @PreUpdate
    public void updateOrder() {
        this.orderIndex = this.propertyDefinitionSimple.getEnumeratedValues().indexOf(this);
    }

    public PropertyDefinitionSimple getPropertyDefinitionSimple() {
        return propertyDefinitionSimple;
    }

    public void setPropertyDefinitionSimple(PropertyDefinitionSimple propertyDefinitionSimple) {
        this.propertyDefinitionSimple = propertyDefinitionSimple;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    /**
     * Two items are equal if they have the same name and share the same PropertyDefinitionSimple
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (!(o instanceof PropertyDefinitionEnumeration))) {
            return false;
        }

        PropertyDefinitionEnumeration that = (PropertyDefinitionEnumeration) o;

        if ((name != null) ? (!name.equals(that.name)) : (that.name != null)) {
            return false;
        }

        /*if ((propertyDefinitionSimple != null) ? (!propertyDefinitionSimple.equals(that.propertyDefinitionSimple))
            : (that.propertyDefinitionSimple != null)) {
            return false;
        }*/

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = ((name != null) ? name.hashCode() : 0);
        //        result = (31 * result) + ((propertyDefinitionSimple != null) ? propertyDefinitionSimple.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PropertyDefinitionEnumeration: " + value;
    }
}