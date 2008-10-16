/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.mock.jboss.artifacts;

/**
 * @author Jason Dobies
 */
public class ArtifactHolder {
    // Attributes  --------------------------------------------

    private String key;
    private String name;
    private String type;

    // Constructors  --------------------------------------------

    public ArtifactHolder(String key, String name, String type) {
        this.key = key;
        this.name = name;
        this.type = type;
    }

    // Public  --------------------------------------------

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
