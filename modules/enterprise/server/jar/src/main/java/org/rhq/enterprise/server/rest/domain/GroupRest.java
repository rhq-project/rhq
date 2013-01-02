package org.rhq.enterprise.server.rest.domain;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

import com.wordnik.swagger.annotations.ApiClass;
import com.wordnik.swagger.annotations.ApiProperty;

import org.rhq.core.domain.resource.group.GroupCategory;

/**
 * Representation of a resource group
 * @author Heiko W. Rupp
 */
@ApiClass("Representation of a resource group")
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

    @ApiProperty("Id of the group")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ApiProperty("Name of the group")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiProperty("ResourceType id for compatible groups")
    public Integer getResourceTypeId() {
        return resourceTypeId;
    }

    public void setResourceTypeId(Integer resourceTypeId) {
        this.resourceTypeId = resourceTypeId;
    }

    @ApiProperty("True if the group is recursive (i.e. includes child resources")
    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    @ApiProperty(value="Category of the group. ", allowableValues = "COMPATIBLE, MIXED" )
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

    @ApiProperty("Id of a DynaGroup definition if the group was defined by a DynaGroup.")
    public int getDynaGroupDefinitionId() {
        return dynaGroupDefinitionId;
    }

    public void setDynaGroupDefinitionId(int dynaGroupDefinitionId) {
        this.dynaGroupDefinitionId = dynaGroupDefinitionId;
    }

    @ApiProperty("Number of explicitly added resources in the group")
    public int getExplicitCount() {
        return explicitCount;
    }

    public void setExplicitCount(int explicitCount) {
        this.explicitCount = explicitCount;
    }

    @ApiProperty("Number of resources in the group (explict + children for a recursive group")
    public int getImplicitCount() {
        return implicitCount;
    }

    public void setImplicitCount(int implicitCount) {
        this.implicitCount = implicitCount;
    }
}
