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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * Defines a source for property options
 * @author Heiko W. Rupp
 */
@Entity
@Table(name="RHQ_PROP_DEF_OPT_SRC")
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_CONFIG_PROP_DEF_OPT_SRC_ID_SEQ")
@XmlAccessorType(XmlAccessType.FIELD)
public class PropertyOptionsSource implements Serializable{

    @Id
    @GeneratedValue(generator = "SEQ", strategy = GenerationType.AUTO)
    private int id;

    @Column(name="LINK_TO_TARGET")
    private boolean linkToTarget;
    private String filter;
    private String expression;
    @Column(name="TARGET_TYPE")
    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    @ManyToOne
    @JoinColumn(name="PROPERTY_DEF_ID" )
    PropertyDefinitionSimple propertyDefinition;

    public PropertyOptionsSource() {
    }

    public void setTarget(String target) {
        targetType = TargetType.fromValue(target);
    }

    public void setLinkToTarget(boolean linkToTarget) {
        this.linkToTarget = linkToTarget;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public boolean isLinkToTarget() {
        return linkToTarget;
    }

    public String getFilter() {
        return filter;
    }

    public String getExpression() {
        return expression;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public enum TargetType {
        GROUP("group"),
        PLUGIN("plugin"),
        RESOURCE_TYPE("resourceType"),
        RESOURCE("resource"),
        CONFIGURATION("configuration");

        private final String value;

        TargetType(String v) {
            value = v;
        }

        public String value() {
            return value;
        }

        public static TargetType fromValue(String v) {
            for (TargetType c: TargetType.values()) {
                if (c.value.equals(v)) {
                    return c;
                }
            }
            return valueOf(v);
//            throw new IllegalArgumentException(v.toString());
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
