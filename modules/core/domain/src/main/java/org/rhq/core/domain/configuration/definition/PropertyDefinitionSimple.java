/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.configuration.definition.constraint.Constraint;
import org.rhq.core.domain.measurement.MeasurementUnits;

/**
 * Defines a simple property.
 *
 * @author Jason Dobies
 * @author Greg Hinkle
 * @author Ian Springer
 */
@DiscriminatorValue("property")
@Entity(name = "PropertyDefinitionSimple")
@XmlRootElement(name = "PropertyDefinitionSimple")
@XmlAccessorType(XmlAccessType.FIELD)
public class PropertyDefinitionSimple extends PropertyDefinition {
    private static final long serialVersionUID = 1L;

    @Column(name = "SIMPLE_TYPE")
    @Enumerated(EnumType.STRING)
    private PropertySimpleType type;

    @Column(name = "ALLOW_CUSTOM_ENUM_VALUE")
    private boolean allowCustomEnumeratedValue = false;

    @Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @OneToMany(mappedBy = "propertyDefinitionSimple", cascade = { CascadeType.ALL }, fetch = FetchType.EAGER)
    private Set<Constraint> constraints = new HashSet<Constraint>();

    /**
     * The <options> within <property-options> for a <simple-property>
     */
    @Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @IndexColumn(name = "order_index")
    @OneToMany(mappedBy = "propertyDefinitionSimple", fetch = FetchType.EAGER)
    private List<PropertyDefinitionEnumeration> enumeratedValues = new ArrayList<PropertyDefinitionEnumeration>();

    /**
     * This property's default value. This field should have a non-null value for properties whose
     * {@link PropertyDefinition#required} field is <code>false</code> (i.e. for optional properties). Conversely, this
     * field should be null for properties whose {@link PropertyDefinition#required} field is <code>true</code> (i.e. for
     * required properties).
     *
     * This field is mainly used to help build default templates. You usually do not want to use this
     * default value member variable directly - consider using the configuration definition's default template.
     */
    @Column(name = "DEFAULT_VALUE", length = 2000)
    private String defaultValue;

    /**
     * Measurement unit in which this simple property is taken
     */
    @Enumerated(EnumType.ORDINAL)
    private MeasurementUnits units;

    @Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @OneToMany(mappedBy = "propertyDefinition", fetch = FetchType.EAGER)
    List<PropertyOptionsSource> optionsSource = new ArrayList<PropertyOptionsSource>();

    public PropertyDefinitionSimple(@NotNull String name, String description, boolean required,
        @NotNull PropertySimpleType type) {
        super(name, description, required);
        this.type = type;
    }

    protected PropertyDefinitionSimple() {
        // JPA use
    }

    public PropertySimpleType getType() {
        return type;
    }

    public void setType(PropertySimpleType type) {
        this.type = type;
    }

    /**
     * Return all the constraints on this simple-property no matter how they are grouped together in the XML.
     *
     * @return
     */
    @NotNull
    public Set<Constraint> getConstraints() {
        return this.constraints;
    }

    public void setConstraints(Set<Constraint> constraints) {
        for (Constraint constraint : constraints) {
            getConstraints().add(constraint);
            constraint.setPropertyDefinitionSimple(this);
        }
    }

    public void addConstraints(Constraint... constraintsToAdd) {
        for (Constraint constraint : constraintsToAdd) {
            getConstraints().add(constraint);
            constraint.setPropertyDefinitionSimple(this);
        }
    }

    /**
     * Get the &lt;options&gt; within &lt;property-options&gt; for a &lt;simple-property&gt;
     */
    @NotNull
    public List<PropertyDefinitionEnumeration> getEnumeratedValues() {
        return this.enumeratedValues;
    }

    public void setEnumeratedValues(List<PropertyDefinitionEnumeration> enumeratedValues, boolean allowCustomEnumValue) {
        addEnumeratedValues(enumeratedValues.toArray(new PropertyDefinitionEnumeration[enumeratedValues.size()]));
        this.allowCustomEnumeratedValue = allowCustomEnumValue;
        ensureOrdering();
    }

    public void addEnumeratedValues(PropertyDefinitionEnumeration... enumerations) {
        for (PropertyDefinitionEnumeration enumeration : enumerations) {
            enumeration.setPropertyDefinitionSimple(this);
            getEnumeratedValues().add(enumeration);
        }
        ensureOrdering();
    }

    public void removeEnumeratedValues(PropertyDefinitionEnumeration... enumerations) {
        for (PropertyDefinitionEnumeration enumeration : enumerations) {
            getEnumeratedValues().remove(enumeration);
        }
        ensureOrdering();
    }

    private void ensureOrdering() {
        if (null == this.enumeratedValues) {
            return;
        }
        for (int i = 0, size = getEnumeratedValues().size(); (i < size); ++i) {
            getEnumeratedValues().get(i).setOrderIndex(i);
        }
    }

    /**
     * If <code>false</code> and this simple property has {@link #getEnumeratedValues() enumerated values} defined, then
     * the value of the simple property <b>must</b> be one of the enumerated values. If <code>true</code>, then the
     * value of this simple property is not required to be one of the enumerated values - a user can opt to set the
     * value to some other custom value. This is useful, for example, when a property has an enumerated list of common
     * JDBC drivers but allows a user to enter their own custom JDBC driver if not one of the common drivers given in
     * the enumerated list.
     *
     * <p>Note that this flag has no effect if there are no enumerated values defined for this simple property.</p>
     *
     * @return flag to indicate if the property value is not restricted to one of the enumerated values
     */
    public boolean getAllowCustomEnumeratedValue() {
        return this.allowCustomEnumeratedValue;
    }

    /**
     * See {@link #getAllowCustomEnumeratedValue()} for a description of this flag.
     *
     * @param allowCustomEnumValue
     */
    public void setAllowCustomEnumeratedValue(boolean allowCustomEnumValue) {
        this.allowCustomEnumeratedValue = allowCustomEnumValue;
    }

    /**
     * Returns the default value that this property definition defines for its
     * value. Note, however, you rarely want to use this directly. Under most
     * circumstances, you should be using the configuration definition's
     * {@link ConfigurationDefinition#getDefaultTemplate() default template}.
     *
     * For an example of where this method is useful, see
     * org.rhq.core.clientapi.agent.configuration.ConfigurationUtility.initializeDefaultTemplate(ConfigurationDefinition)
     *
     * @return property default value
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Sets the default value for this property. Note that you normally call this
     * only at times when you plan on building default templates with this
     * property definition later. See {@link #getDefaultValue()} for more details
     * on this default value.
     *
     * @param defaultValue
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public MeasurementUnits getUnits() {
        return units;
    }

    public void setUnits(MeasurementUnits units) {
        this.units = units;
    }

    public PropertyOptionsSource getOptionsSource() {
        if (optionsSource.isEmpty())
            return null;
        return optionsSource.get(0);
    }

    public void setOptionsSource(PropertyOptionsSource source) {
        this.optionsSource.clear();
        if (source==null)
            return;
        source.propertyDefinition = this;
        this.optionsSource.add(source);
    }


    @Override
    public String toString() {
        return "SimpleProperty["
            + getName()
            + "] (Type: "
            + getType()
            + ")"
            + ((getPropertyGroupDefinition() != null) ? ("(Group: " + getPropertyGroupDefinition().getName() + ")")
                : "");
    }
}
