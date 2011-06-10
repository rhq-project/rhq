package org.rhq.core.domain.rest;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * A (partial) resource with some type information
 * @author Heiko W. Rupp
 */
@XmlRootElement
public class ResourceWithType {

    String resourceName;
    int resourceId;
    String typeName;
    int typeId;
    String pluginName;
    int pluginId;
    String parentName;
    int parentId;

    public ResourceWithType() {
    }

    public ResourceWithType(String resourceName, int resourceId, String typeName, int typeId, String pluginName) {
        this.resourceName = resourceName;
        this.resourceId = resourceId;
        this.typeName = typeName;
        this.typeId = typeId;
        this.pluginName = pluginName;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public int getPluginId() {
        return pluginId;
    }

    public void setPluginId(int pluginId) {
        this.pluginId = pluginId;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }
}
