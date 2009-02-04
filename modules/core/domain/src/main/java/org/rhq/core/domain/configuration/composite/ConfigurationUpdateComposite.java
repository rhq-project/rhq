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
package org.rhq.core.domain.configuration.composite;

import java.io.Serializable;

import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;

/**
 * @author Joseph Marques
 */
public class ConfigurationUpdateComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id; //configuration update id
    private final ConfigurationUpdateStatus status;
    private final String errorMessage;
    private final String subjectName;
    private final long createdTime;
    private final long modifiedTime;
    private final int resourceId; // every update belong to SOME object in the system
    private final String resourceName;
    private final Integer parentResourceId; // use object, because it can be null
    private final String parentResourceName;

    public ConfigurationUpdateComposite(int id, ConfigurationUpdateStatus status, String errorMessage,
        String subjectName, long createdTime, long modifiedTime, Integer resourceId, String resourceName) {
        this.id = id;
        this.status = status;
        this.errorMessage = errorMessage;
        this.subjectName = subjectName;
        this.createdTime = createdTime;
        this.modifiedTime = modifiedTime;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.parentResourceId = 0;
        this.parentResourceName = "Not Initialized";
    }

    public ConfigurationUpdateComposite(int id, ConfigurationUpdateStatus status, String errorMessage,
        String subjectName, long createdTime, long modifiedTime, Integer resourceId, String resourceName,
        Integer parentResourceId, String parentResourceName) {
        this.id = id;
        this.status = status;
        this.errorMessage = errorMessage;
        this.subjectName = subjectName;
        this.createdTime = createdTime;
        this.modifiedTime = modifiedTime;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.parentResourceId = parentResourceId;
        this.parentResourceName = parentResourceName;
    }

    public int getId() {
        return id;
    }

    public ConfigurationUpdateStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public long getModifiedTime() {
        return modifiedTime;
    }

    public int getResourceId() {
        return resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public Integer getParentResourceId() {
        return parentResourceId;
    }

    public String getParentResourceName() {
        return parentResourceName;
    }

}
