/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.core.domain.configuration.definition;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import org.jetbrains.annotations.NotNull;

/**
 * The definition of a list of properties where each member of the list has the same definition. The definition of the
 * entries may be null in the case of arbitrary lists.
 *
 * @author Greg Hinkle
 */
@DiscriminatorValue("list")
@Entity(name = "PropertyDefinitionList")
public class PropertyDefinitionList extends PropertyDefinition {
    private static final long serialVersionUID = 1L;

    /**
     * See JBNADM-1595
     */
    @Transient
    private int min;

    /**
     * See JBNADM-1595
     */
    @Transient
    private int max;

    @JoinColumn(name = "parent_list_definition_id")
    @OneToOne(cascade = CascadeType.ALL)
    private PropertyDefinition memberDefinition;

    public PropertyDefinitionList(@NotNull
    String name, String description, boolean required, PropertyDefinition memberDefinition) {
        super(name, description, required);
        setMemberDefinition(memberDefinition);
    }

    public PropertyDefinitionList() {
        // JPA use
    }

    public PropertyDefinition getMemberDefinition() {
        return memberDefinition;
    }

    public void setMemberDefinition(PropertyDefinition memberDefinition) {
        this.memberDefinition = memberDefinition;

        if (memberDefinition != null) {
            this.memberDefinition.setParentPropertyListDefinition(this);
        }
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }
}