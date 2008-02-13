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
package org.rhq.core.domain.configuration.definition.constraint;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;

/**
 * Base class for constraints on configuration property values. These constraints are part of the
 * ConfigurationDefinition model.
 *
 * @author Jason Dobies
 */
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_CONFIG_PROP_CONSTR_ID_SEQ")
@Table(name = "RHQ_CONFIG_PROP_CONSTR")
public abstract class Constraint implements Serializable {
    private static final long serialVersionUID = 1L;

    protected static final String DELIMITER = "#";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ")
    @Id
    private long id;

    @Column(name = "DETAILS", nullable = false)
    protected String details;

    @JoinColumn(name = "CONFIG_PROP_DEF_ID")
    @ManyToOne
    private PropertyDefinitionSimple propertyDefinitionSimple;

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

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[details=" + getDetails() + "]";
    }
}