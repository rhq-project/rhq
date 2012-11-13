package org.rhq.enterprise.server.rest.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

import com.wordnik.swagger.annotations.ApiClass;
import com.wordnik.swagger.annotations.ApiProperty;

/**
 * @author Heiko W. Rupp
 */
@XmlRootElement
@ApiClass(value = "Represents an operation to be scheduled.",
        description = "You use this object to prepare the operation to be scheduled. " +
                "The object is derived from an OperationDefinition. When you are ready preparing," +
                "you need to set 'readyToSubmit' to true.")
public class OperationRest {

    int id;
    String name;
    boolean readyToSubmit;
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

    @ApiProperty("Id of the operation schedule")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ApiProperty("Name of the operation")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiProperty("Is this schedule ready to finally be submitted (and thus be scheduled to run)")
    public boolean isReadyToSubmit() {
        return readyToSubmit;
    }

    public void setReadyToSubmit(boolean readyToSubmit) {
        this.readyToSubmit = readyToSubmit;
    }

    @ApiProperty("The id of the resource the operation should run ")
    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    @ApiProperty("The id of the operation definition this schedule was created from")
    public int getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(int definitionId) {
        this.definitionId = definitionId;
    }

    @ApiProperty("A key/value map of parameters. Keys are strings. The map is populated at the time the " +
            "schedule is created from its definition")
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
