package org.rhq.enterprise.server.rest.domain;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Wrapper for Exceptions
 * @author Heiko W. Rupp
 */
@XmlRootElement
public class RHQErrorWrapper {

    private String message;

    public RHQErrorWrapper(String message) {
        this.message = message;
    }

    public RHQErrorWrapper() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
