package org.rhq.enterprise.server.rest.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Heiko W. Rupp
 */
@XmlRootElement
public class OperationRest {

    int id;
    String name;
    String state; // TODO Convert to enum
    int resourceId;
    int definitionId;
    Map<String,Object> params = new HashMap<String, Object>();
    List<Link> links = new ArrayList<Link>();

    public OperationRest() {
    }

    public OperationRest(int resourceId, int definitionId) {
        this.resourceId = resourceId;
        this.definitionId = definitionId;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(int definitionId) {
        this.definitionId = definitionId;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    @XmlElementRef
    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public void addLink(Link link) {
        links.add(link);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OperationRest that = (OperationRest) o;

        if (definitionId != that.definitionId) return false;
        if (id != that.id) return false;
        if (resourceId != that.resourceId) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + resourceId;
        result = 31 * result + definitionId;
        return result;
    }
}
