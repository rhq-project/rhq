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
package org.rhq.core.domain.resource.composite;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.resource.ResourceCategory;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class ResourceInstallCount implements Serializable {

    private static final long serialVersionUID = 1L;

    private long count;
    private String typeName;
    private String typePlugin;
    private ResourceCategory category;
    private int typeId;
    private String version;
    private int numDriftTemplates;
    private boolean inCompliance;

    public ResourceInstallCount() {
    }

    public ResourceInstallCount(String typeName, String typePlugin, ResourceCategory category, int typeId, long count) {
        this(typeName, typePlugin, category, typeId, count, null, -1, -1);
    }

    public ResourceInstallCount(String typeName, String typePlugin, ResourceCategory category, int typeId, long count,
        String version) {
        this(typeName, typePlugin, category, typeId, count, version, -1, -1);
    }

    public ResourceInstallCount(String typeName, String typePlugin, ResourceCategory category, int typeId, long count,
        String version, int numDriftTemplates, int inCompliance) {
        this.count = count;
        this.typeName = typeName;
        this.typePlugin = typePlugin;
        this.category = category;
        this.typeId = typeId;
        this.version = version;
        this.numDriftTemplates = numDriftTemplates;
        this.inCompliance = inCompliance == 0;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getTypePlugin() {
        return typePlugin;
    }

    public void setTypePlugin(String typePlugin) {
        this.typePlugin = typePlugin;
    }

    public ResourceCategory getCategory() {
        return category;
    }

    public void setCategory(ResourceCategory category) {
        this.category = category;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getNumDriftTemplates() {
        return numDriftTemplates;
    }

    public void setNumDriftTemplates(int numDriftTemplates) {
        this.numDriftTemplates = numDriftTemplates;
    }

    public boolean isInCompliance() {
        return inCompliance;
    }

    public void setInCompliance(boolean inCompliance) {
        this.inCompliance = inCompliance;
    }
}
