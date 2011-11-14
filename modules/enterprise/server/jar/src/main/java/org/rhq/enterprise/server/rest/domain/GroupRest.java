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
    private GroupCategory category;

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
}
