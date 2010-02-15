package org.rhq.core.domain.content.transfer;

import java.io.Serializable;

public class SubscribedRepo implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name;

    public SubscribedRepo() {
    }

    public SubscribedRepo(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * @return the repo id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the repo id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the repo name
     */
    public String getName() {
        return name;
    }

    /**
     * @param repoName the repo name to set
     */
    public void setName(String name) {
        this.name = name;
    }
}
