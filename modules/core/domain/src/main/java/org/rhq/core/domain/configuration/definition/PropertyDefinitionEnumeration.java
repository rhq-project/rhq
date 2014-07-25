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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import org.jetbrains.annotations.NotNull;

/**
 * @author Jason Dobies
 */
@Entity
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_CONF_PROP_DEF_ENUM_ID_SEQ", sequenceName = "RHQ_CONF_PROP_DEF_ENUM_ID_SEQ")
@Table(name = "RHQ_CONF_PROP_DEF_ENUM")
@XmlAccessorType(XmlAccessType.FIELD)
public class PropertyDefinitionEnumeration implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_CONF_PROP_DEF_ENUM_ID_SEQ")
    @Id
    private long id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "STRING_VALUE", nullable = false)
    private String value;

    @Column(name = "ORDER_INDEX", nullable = false)
    private int orderIndex = -1;

    @JoinColumn(name = "PROPERTY_DEF_ID")
    @ManyToOne
    @XmlTransient
    private PropertyDefinitionSimple propertyDefinitionSimple;

    protected PropertyDefinitionEnumeration() {
        // empty constructor, JPA use only
    }

    public PropertyDefinitionEnumeration(@NotNull String name, String value) {
        this.name = name;
        this.value = value;
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

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PropertyDefinitionEnumeration that = (PropertyDefinitionEnumeration)o;

        if (!value.equals(that.value)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "PropertyDefinitionEnumeration{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", value='" + value + '\'' +
            ", orderIndex=" + orderIndex +
            '}';
    }
}