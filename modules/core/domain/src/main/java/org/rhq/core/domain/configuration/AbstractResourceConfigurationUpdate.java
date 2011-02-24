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
package org.rhq.core.domain.configuration;

import java.io.Serializable;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.configuration.group.AbstractGroupConfigurationUpdate;
import org.rhq.core.domain.resource.Resource;

/**
 * Provides a {@link Configuration configuration} that constitutes a configuration update request. This allows you to
 * maintain a history of a configuration update request - when it was made, who made it and if it was successful or not.
 * The status field indicates if the request is currently in progress (i.e. the plugin is currently processing the
 * request) or if it succeeded or failed. If the request failed, you can examine the error messages in the Configuration
 * properties to find out why the request failed.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 * @author Joseph Marques
 */
@DiscriminatorColumn(name = "DTYPE")
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_CONFIG_UPDATE_ID_SEQ")
@Table(name = "RHQ_CONFIG_UPDATE")
public abstract class AbstractResourceConfigurationUpdate extends AbstractConfigurationUpdate implements Serializable {
    private static final long serialVersionUID = 1L;

    @GeneratedValue(generator = "SEQ", strategy = GenerationType.AUTO)
    @Id
    private int id;

    public abstract Resource getResource();

    public abstract AbstractGroupConfigurationUpdate getAbstractGroupConfigurationUpdate();

    protected AbstractResourceConfigurationUpdate() {
    }

    /**
     * Creates an initial {@link AbstractResourceConfigurationUpdate} with its status initially set to
     * {@link ConfigurationUpdateStatus#INPROGRESS} and a <code>null</code> {@link #getErrorMessage()}.
     *
     * @param config      contains the values for the new configuration
     * @param subjectName the user that is requesting the update
     */
    protected AbstractResourceConfigurationUpdate(Configuration config, String subjectName) {
        super(subjectName);
        //this.configuration = config.deepCopy(false);
        this.configuration = config.deepCopyWithoutProxies();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    protected void appendToStringInternals(StringBuilder str) {
        super.appendToStringInternals(str);
        str.append(", id=").append(this.id);
    }
}