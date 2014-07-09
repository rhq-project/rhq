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
package org.rhq.core.domain.common;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * @author <a href="mailto:ghinkle@jboss.com">Greg Hinkle</a>
 */
@Entity
@NamedQueries({ @NamedQuery(name = SystemConfiguration.QUERY_FIND_ALL, query = "SELECT c FROM SystemConfiguration AS c") })
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_SYSTEM_CONFIG_ID_SEQ", sequenceName = "RHQ_SYSTEM_CONFIG_ID_SEQ")
@Table(name = "RHQ_SYSTEM_CONFIG")
public class SystemConfiguration implements Serializable {
    public static final String QUERY_FIND_ALL = "SystemConfiguration.findAll";
    public static final String FIND_PROPERTY_BY_KEY = "SystemConfiguration.FIND_PROPERTY_BY_KEY";

    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_SYSTEM_CONFIG_ID_SEQ")
    @Id
    private int id;

    @Column(name = "PROPERTY_KEY")
    private String propertyKey;

    @Column(name = "PROPERTY_VALUE")
    private String propertyValue;

    @Column(name = "DEFAULT_PROPERTY_VALUE")
    private String defaultPropertyValue;

    @Column(name = "FREAD_ONLY")
    private Boolean freadOnly;

    protected SystemConfiguration() {
    }

    public SystemConfiguration(String propertyKey, String propertyValue) {
        this.propertyKey = propertyKey;
        this.propertyValue = propertyValue;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public void setPropertyKey(String propertyKey) {
        this.propertyKey = propertyKey;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public String getDefaultPropertyValue() {
        return defaultPropertyValue;
    }

    public void setDefaultPropertyValue(String defaultPropertyValue) {
        this.defaultPropertyValue = defaultPropertyValue;
    }

    public Boolean getFreadOnly() {
        return freadOnly;
    }

    public void setFreadOnly(Boolean freadOnly) {
        this.freadOnly = freadOnly;
    }

    @Override
    public String toString() {
        return "org.rhq.core.domain.common.SystemConfiguration[id=" + id + " , key=" + this.propertyKey + " , value="
            + this.propertyValue + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (!(o instanceof SystemConfiguration))) {
            return false;
        }

        SystemConfiguration that = (SystemConfiguration) o;

        if (!propertyKey.equals(that.propertyKey)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return propertyKey.hashCode();
    }
}