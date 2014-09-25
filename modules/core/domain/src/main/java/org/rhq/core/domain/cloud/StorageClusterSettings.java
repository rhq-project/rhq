/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

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

    private RegularSnapshots regularSnapshots;

    public void setRegularSnapshots(RegularSnapshots regularSnapshots) {
        this.regularSnapshots = regularSnapshots;
    }

    public RegularSnapshots getRegularSnapshots() {
        return regularSnapshots;
    }

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
        if (regularSnapshots != null && !regularSnapshots.equals(that.regularSnapshots))
            return false;
        if (regularSnapshots == null && that.regularSnapshots != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = cqlPort;
        result = 29 * result + gossipPort;
        result = 29 * result + (automaticDeployment ? 1231 : 1237);
        result = 29 * result + (username == null ? 0 : username.hashCode());
        result = 29 * result + (passwordHash == null ? 0 : passwordHash.hashCode());
        result = 29 * result + (regularSnapshots == null ? 0 : regularSnapshots.hashCode());
        return result;
    }


    @Override
    public String toString() {
        return "StorageClusterSettings[cqlPort=" + cqlPort + ", gossipPort=" + gossipPort + ", automaticDeployment="
            + automaticDeployment + ", username (read-only)=" + username + ", passwordHash=********, regularSnapshots="
            + regularSnapshots + "]";
    }

    public static class RegularSnapshots implements Serializable {

        private static final long serialVersionUID = 1L;
        private Boolean enabled = Boolean.FALSE;
        private String schedule;
        private String retention;
        private int count;
        private String deletion;
        private String location;

        @Override
        public int hashCode() {
            int result = count;
            result = 29 * result + (enabled ? 1231 : 1237);
            result = 29 * result + (schedule == null ? 0 : schedule.hashCode());
            result = 29 * result + (retention == null ? 0 : retention.hashCode());
            result = 29 * result + (deletion == null ? 0 : deletion.hashCode());
            result = 29 * result + (location == null ? 0 : location.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            RegularSnapshots that = (RegularSnapshots) o;

            if (enabled != that.enabled)
                return false;
            if (schedule != that.schedule)
                return false;
            if (retention != that.retention)
                return false;
            if (count != that.count)
                return false;
            if (deletion != that.deletion)
                return false;
            if (location != that.location)
                return false;

            return true;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getSchedule() {
            return schedule;
        }

        public void setSchedule(String schedule) {
            this.schedule = schedule;
        }

        public String getRetention() {
            return retention;
        }

        public void setRetention(String retention) {
            this.retention = retention;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public String getDeletion() {
            return deletion;
        }

        public void setDeletion(String deletion) {
            this.deletion = deletion;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        @Override
        public String toString() {
            return new StringBuilder("RegularSnapshots[enabled=" + enabled)
                .append(", schedule=" + schedule).append(", retention=" + retention)
                .append(", count=" + count).append(", deletion=" + deletion).append(", location=" + location)
                .toString();
        }
    }
}
