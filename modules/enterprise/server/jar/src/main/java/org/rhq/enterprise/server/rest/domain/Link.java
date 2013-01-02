package org.rhq.enterprise.server.rest.domain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.wordnik.swagger.annotations.ApiClass;
import com.wordnik.swagger.annotations.ApiProperty;

/**
 * A link between two resources
 * @author Heiko W. Rupp
 */
@ApiClass("Link between two resources")
@XmlRootElement
public class Link {

    private String rel;
    private String href;

    public Link() {
    }

    public Link(String rel, String href) {
        this.rel = rel;
        this.href = href;
    }


    @ApiProperty("Name of the relation")
    @XmlAttribute
    public String getRel() {
        return rel;
    }

    @ApiProperty("Target of the relation")
    @XmlAttribute
    public String getHref() {
        return href;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }

    public void setHref(String href) {
        this.href = href;
    }

    @Override
    public String toString() {
        return
            href + "; "  +
                "rel='" + rel + '\'' ;
    }
}
