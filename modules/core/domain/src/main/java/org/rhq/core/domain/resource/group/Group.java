 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.resource.group;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import org.jetbrains.annotations.NotNull;
import org.rhq.core.domain.auth.Subject;

/**
 * This is the abstract base class for groups containing either {@link org.rhq.core.domain.resource.Resource}s or groups
 * of <code>Resource</code>s.
 *
 * @author Greg Hinkle
 */
@MappedSuperclass
public abstract class Group implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "LOCATION")
    private String location; // TODO (ips): I don't think this field makes sense for groups.

    @Column(name = "CTIME")
    private Long ctime;

    @Column(name = "MTIME")
    private Long mtime;

    @JoinColumn(name = "MODIFIED_BY")
    @ManyToOne(fetch = FetchType.LAZY)
    private Subject modifiedBy;

    /* no-arg constructor required by EJB spec */
    protected Group() {
    }

    public Group(@NotNull
    String name) {
        this.name = name;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull
    String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Long getCtime() {
        return ctime;
    }

    public void setCtime(Long ctime) {
        this.ctime = ctime;
    }

    public Long getMtime() {
        return mtime;
    }

    public void setMtime(Long mtime) {
        this.mtime = mtime;
    }

    public Subject getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(Subject modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (!(o instanceof Group))) {
            return false;
        }

        Group group = (Group) o;

        if (!name.equals(group.name)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @PrePersist
    void onPersist() {
        this.mtime = System.currentTimeMillis();
        this.ctime = System.currentTimeMillis();
    }

    @PreUpdate
    void onUpdate() {
        this.mtime = System.currentTimeMillis();
    }
}