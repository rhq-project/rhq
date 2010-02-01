package org.rhq.core.clientapi.agent.configuration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ConfigurationValidationException extends RuntimeException implements Serializable{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    final List<String> validationMessages;

    public ConfigurationValidationException(List<String> validationMessages) {
        super();
        this.validationMessages = new ArrayList<String>(validationMessages);
    }

    public Iterator<String> messageIterator() {
        return validationMessages.iterator();
    }
}
