package org.rhq.enterprise.server.rest.domain;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.resource.group.GroupCategory;

/**
 * Representation of a resource group
 * @author Heiko W. Rupp
 */
@XmlRootElement
public class GroupRest {


    int id;
    private String name;
    private Integer resourceTypeId;
    private boolean recursive;
    private GroupCategory category;
    private int dynaGroupDefinitionId;
    private int explicitCount;
    private int implicitCount;

    List<Link> links = new ArrayList<Link>();

    public GroupRest() {
    }

    public GroupRest(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getResourceTypeId() {
        return resourceTypeId;
    }

    public void setResourceTypeId(Integer resourceTypeId) {
        this.resourceTypeId = resourceTypeId;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public GroupCategory getCategory() {
        return category;
    }

    public void setCategory(GroupCategory category) {
        this.category = category;
    }

    @XmlElementRef
    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public int getDynaGroupDefinitionId() {
        return dynaGroupDefinitionId;
    }

    public void setDynaGroupDefinitionId(int dynaGroupDefinitionId) {
        this.dynaGroupDefinitionId = dynaGroupDefinitionId;
    }

    public int getExplicitCount() {
        return explicitCount;
    }

    public void setExplicitCount(int explicitCount) {
        this.explicitCount = explicitCount;
    }

    public int getImplicitCount() {
        return implicitCount;
    }

    public void setImplicitCount(int implicitCount) {
        this.implicitCount = implicitCount;
    }
}
