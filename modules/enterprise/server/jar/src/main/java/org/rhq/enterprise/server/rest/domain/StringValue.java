package org.rhq.enterprise.server.rest.domain;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Encapsulate a simple string value
 * @author Heiko W. Rupp
 */
@XmlRootElement
public class StringValue {

    String value;

    public StringValue() {
    }

    public StringValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
