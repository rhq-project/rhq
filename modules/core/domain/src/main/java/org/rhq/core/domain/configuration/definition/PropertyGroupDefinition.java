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
package org.rhq.core.domain.configuration.definition;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import org.jetbrains.annotations.NotNull;

/**
 * I made this an attribute instead of a content holder because I didn't want to complicate the access procedures for a
 * property definition. Imagine having to look through each group to find the property your trying to validate.
 *
 * @author Greg Hinkle
 */
@Entity
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_CONFIG_PROP_GRP_DEF_ID_SEQ")
@Table(name = "RHQ_CONFIG_PROP_GRP_DEF")
public class PropertyGroupDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "ID")
    @GeneratedValue(generator = "SEQ", strategy = GenerationType.SEQUENCE)
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DISPLAY_NAME")
    private String displayName;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "DEFAULT_HIDDEN")
    private boolean defaultHidden = true;

    @Column(name = "ORDER_INDEX")
    private int order;

    protected PropertyGroupDefinition() {
        // empty, for JPA use only
    }

    public PropertyGroupDefinition(@NotNull
    String name) {
        this.name = name;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    public void setName(@NotNull
    String name) {
        this.name = name;
    }

    @NotNull
    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDefaultHidden() {
        return this.defaultHidden;
    }

    public void setDefaultHidden(boolean defaultHidden) {
        this.defaultHidden = defaultHidden;
    }

    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public String toString() {
        return "PropertyGroupDefinition[id=" + this.id + ", name=" + this.name + "]";
    }

    /**
     * Two groups are the same if they are identical or share the same name TODO ???
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (!(o instanceof PropertyGroupDefinition))) {
            return false;
        }

        PropertyGroupDefinition that = (PropertyGroupDefinition) o;

        if ((name != null) ? (!name.equals(that.name)) : (that.name != null)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = ((name != null) ? name.hashCode() : 0);

        return result;
    }
}