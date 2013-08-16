package org.rhq.core.domain.cloud;

import java.io.Serializable;

/**
 * @author John Sanda
 */
public class StorageClusterSettings implements Serializable {

    private static final long serialVersionUID = 1;

    private int cqlPort;

    private int gossipPort;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StorageClusterSettings that = (StorageClusterSettings) o;

        if (cqlPort != that.cqlPort) return false;
        if (gossipPort != that.gossipPort) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = cqlPort;
        result = 29 * result + gossipPort;
        return result;
    }

    @Override
    public String toString() {
        return "StorageClusterSettings[cqlPort=" + cqlPort + ", gossipPort=" + gossipPort + "]";
    }
}
