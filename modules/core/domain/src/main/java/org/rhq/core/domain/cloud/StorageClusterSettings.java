package org.rhq.core.domain.cloud;

import java.io.Serializable;

/**
 * @author John Sanda
 */
public class StorageClusterSettings implements Serializable {

    private static final long serialVersionUID = 1;

    private int cqlPort;

    private int gossipPort;
    
    private Boolean automaticDeployment;
    
    private String username;
    
    private String passwordHash;

    public int getCqlPort() {
        return cqlPort;
    }

    public void setCqlPort(int cqlPort) {
        this.cqlPort = cqlPort;
    }

    public int getGossipPort() {
        return gossipPort;
    }

    public void setGossipPort(int gossipPort) {
        this.gossipPort = gossipPort;
    }

    public Boolean getAutomaticDeployment() {
        return automaticDeployment;
    }

    public void setAutomaticDeployment(Boolean automaticDeployment) {
        this.automaticDeployment = automaticDeployment;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StorageClusterSettings that = (StorageClusterSettings) o;

        if (cqlPort != that.cqlPort) return false;
        if (gossipPort != that.gossipPort) return false;
        if (automaticDeployment != that.automaticDeployment) return false;
        if (username != that.username) return false;
        if (passwordHash != that.passwordHash) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = cqlPort;
        result = 29 * result + gossipPort;
        result = 29 * result + (automaticDeployment ? 1231 : 1237);
        result = 29 * result + (username == null ? 0 : username.hashCode());
        result = 29 * result + (passwordHash == null ? 0 : passwordHash.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "StorageClusterSettings[cqlPort=" + cqlPort + ", gossipPort=" + gossipPort + ", automaticDeployment="
            + automaticDeployment + ", username=" + username + ", passwordHash=********]";
    }
}
