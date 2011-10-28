package org.rhq.enterprise.server.rest.domain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A link between two resources
 * @author Heiko W. Rupp
 */
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


    @XmlAttribute
    public String getRel() {
        return rel;
    }

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
        return "Link{" +
                "rel='" + rel + '\'' +
                ", href='" + href + '\'' +
                '}';
    }
}
