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
package org.rhq.enterprise.communications.command.param;

import java.util.List;

/**
 * Provides a fixed set of parameter values that are allowed just like its superclass, however, this subclass allows
 * that set of fixed values to be changed dynamically after this object is instantiated.
 *
 * @author John Mazzitelli
 */
public class DynamicFixedValuesParameterDefinition extends FixedValuesParameterDefinition {
    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * The only difference between this constructor and its super constructor is <code>allowed</code> can be <code>
     * null</code> or empty.
     *
     * @see FixedValuesParameterDefinition#FixedValuesParameterDefinition(String, String, boolean, boolean, boolean,
     *      Object[], String)
     */
    public DynamicFixedValuesParameterDefinition(String name, String type, boolean required, boolean nullable,
        boolean hidden, Object[] allowed, String description) throws IllegalArgumentException {
        super(name, type, required, nullable, hidden, allowed, description);
    }

    /**
     * The only difference between this constructor and its super constructor is <code>allowed</code> can be <code>
     * null</code> or empty.
     *
     * @see FixedValuesParameterDefinition#FixedValuesParameterDefinition(String, String, boolean, List, String)
     */
    public DynamicFixedValuesParameterDefinition(String name, String type, boolean hidden, List<Object> allowed,
        String description) throws IllegalArgumentException {
        super(name, type, hidden, allowed, description);
    }

    /**
     * The only difference between this constructor and its super constructor is <code>allowed</code> can be <code>
     * null</code> or empty.
     *
     * @see FixedValuesParameterDefinition#FixedValuesParameterDefinition(String, String, boolean, List, String,
     *      ParameterRenderingInformation)
     */
    public DynamicFixedValuesParameterDefinition(String name, String type, boolean hidden, List<Object> allowed,
        String description, ParameterRenderingInformation renderingInfo) throws IllegalArgumentException {
        super(name, type, hidden, allowed, description, renderingInfo);
    }

    /**
     * The only difference between this constructor and its super constructor is <code>allowed</code> can be <code>
     * null</code> or empty.
     *
     * @see FixedValuesParameterDefinition#FixedValuesParameterDefinition(String, String, boolean, boolean, boolean,
     *      List, String)
     */
    public DynamicFixedValuesParameterDefinition(String name, String type, boolean required, boolean nullable,
        boolean hidden, List<Object> allowed, String description) throws IllegalArgumentException {
        super(name, type, required, nullable, hidden, allowed, description);
    }

    /**
     * The only difference between this constructor and its super constructor is <code>allowed</code> can be <code>
     * null</code> or empty.
     *
     * @see FixedValuesParameterDefinition#FixedValuesParameterDefinition(String, String, boolean, boolean, boolean,
     *      List, String, ParameterRenderingInformation)
     */
    public DynamicFixedValuesParameterDefinition(String name, String type, boolean required, boolean nullable,
        boolean hidden, List<Object> allowed, String description, ParameterRenderingInformation renderingInfo)
        throws IllegalArgumentException {
        super(name, type, required, nullable, hidden, allowed, description, renderingInfo);
    }

    /**
     * Returns <code>true</code> meaning this class allows the parameter definition's
     * {@link FixedValuesParameterDefinition#getAllowedValues() allowed values} to be empty. This is because this class
     * allows that list of allowed values to be modified dynamically at runtime. You could therefore instantiate this
     * parameter definition with zero allowed values and later on add to that list (in the case when the allowed values
     * aren't known until after this object has been instantiated).
     *
     * @see FixedValuesParameterDefinition#allowZeroFixedValues()
     */
    protected boolean allowZeroFixedValues() {
        return true;
    }

    /**
     * This class allows the parameter definition's fixed values to be changed at runtime via this method (unlike its
     * parent class).
     *
     * @see FixedValuesParameterDefinition#setAllowedValues(List)
     */
    public void setAllowedValues(List<Object> newAllowedValues) throws IllegalArgumentException {
        super.setAllowedValues(newAllowedValues);
    }
}