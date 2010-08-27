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
package org.rhq.core.domain.configuration.group;

import java.io.Serializable;

import javax.persistence.DiscriminatorColumn;
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

import org.rhq.core.domain.configuration.AbstractConfigurationUpdate;
import org.rhq.core.domain.resource.group.ResourceGroup;

@DiscriminatorColumn(name = "DTYPE")
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_CONFIG_GROUP_UPDATE_ID_SEQ")
@Table(name = "RHQ_CONFIG_GROUP_UPDATE")
public abstract class AbstractGroupConfigurationUpdate extends AbstractConfigurationUpdate implements Serializable {

    private static final long serialVersionUID = 1L;

    @GeneratedValue(generator = "SEQ", strategy = GenerationType.AUTO)
    @Id
    private int id;

    @JoinColumn(name = "GROUP_ID", referencedColumnName = "ID")
    @ManyToOne
    private ResourceGroup group;

    public static final String MIXED_VALUES_MARKER = "~ Mixed Values ~";

    protected AbstractGroupConfigurationUpdate() {
    }

    protected AbstractGroupConfigurationUpdate(ResourceGroup group, String subjectName) {
        super(subjectName);
        this.group = group;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ResourceGroup getGroup() {
        return group;
    }

    public void setGroup(ResourceGroup group) {
        this.group = group;
    }

    @Override
    protected void appendToStringInternals(StringBuilder str) {
        super.appendToStringInternals(str);
        str.append(", id=").append(this.id);
        str.append(", resourceGroup=").append(this.group);
    }
}