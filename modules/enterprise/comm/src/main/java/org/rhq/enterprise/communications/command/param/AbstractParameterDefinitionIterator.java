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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.rhq.enterprise.communications.command.Command;

/**
 * Iterates over a <code>Collection</code> of {@link ParameterDefinition} objects based on whether the parameters are
 * required or not.
 *
 * <p>Note that a snapshot of the parameter definitions is taken at the time this iterator is created - concurrent
 * access is allowed on the original set of parameter definitions, however, changes made to the original collection are
 * not reflected by this iterator.</p>
 *
 * @author John Mazzitelli
 */
public abstract class AbstractParameterDefinitionIterator implements Iterator {
    /**
     * the list of parameter definitions being iterated
     */
    private List<ParameterDefinition> m_parameterDefinitions;

    /**
     * the internal iterator that iterates around <code>m_parameterDefinitions</code>
     */
    private Iterator m_internalIterator;

    /**
     * Constructor for {@link AbstractParameterDefinitionIterator} given the collection of parameter definitions. If
     * <code>required</code> is <code>true</code>, only those parameters defined as
     * {@link ParameterDefinition#isRequired() required} will be iterated. If <code>required</code> is <code>
     * false</code>, only those parameters defined as <i>not</i>{@link ParameterDefinition#isRequired() required} (i.e.
     * optional) will be iterated.
     *
     * <p>If <code>parameterDefinitions</code> is <code>null</code>, the iterator will be valid but will not have a
     * "next" element.</p>
     *
     * @param parameterDefinitions the collection of parameters that are to be iterated (may be <code>null</code>)
     * @param required             if <code>true</code>, only the definitions of required parameters are iterated; if
     *                             <code>false</code>, only the definitions of optional parameters are iterated
     */
    public AbstractParameterDefinitionIterator(Collection<ParameterDefinition> parameterDefinitions, boolean required) {
        m_parameterDefinitions = new ArrayList<ParameterDefinition>();

        if (parameterDefinitions != null) {
            for (Iterator iter = parameterDefinitions.iterator(); iter.hasNext();) {
                ParameterDefinition paramDef = (ParameterDefinition) iter.next();
                if ((required && paramDef.isRequired()) || (!required && !paramDef.isRequired())) {
                    m_parameterDefinitions.add(paramDef);
                }
            }
        }

        m_internalIterator = m_parameterDefinitions.iterator();

        return;
    }

    /**
     * A convienence constructor that allows you to create a new {@link AbstractParameterDefinitionIterator} object
     * given an array of parameter definitions, as opposed to a <code>Collection</code>. This is useful since the method
     * {@link Command#getParameterDefinitions()} returns an array.
     *
     * @param parameterDefinitions array of definitions
     * @param required             if <code>true</code>, only the definitions of required parameters are iterated; if
     *                             <code>false</code>, only the definitions of optional parameters are iterated
     *
     * @see   AbstractParameterDefinitionIterator#AbstractParameterDefinitionIterator(Collection, boolean)
     */
    public AbstractParameterDefinitionIterator(ParameterDefinition[] parameterDefinitions, boolean required) {
        this(buildCollection(parameterDefinitions), required);
    }

    /**
     * Immediately throws <code>java.lang.UnsupportedOperationException</code> - this method is not supported.
     *
     * @see java.util.Iterator#remove()
     */
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return m_internalIterator.hasNext();
    }

    /**
     * @see java.util.Iterator#next()
     */
    public Object next() {
        return m_internalIterator.next();
    }

    /**
     * Puts the given array of parameter definitions inside a <code>Collection</code>. If the given array is <code>
     * null</code>, the returned collection will be empty.
     *
     * @param  parameterDefinitions array of parameter definitions that will be placed in the returned collection (may
     *                              be <code>null</code>)
     *
     * @return a <code>Collection</code> containing the given parameter definitions
     */
    private static Collection<ParameterDefinition> buildCollection(ParameterDefinition[] parameterDefinitions) {
        Collection<ParameterDefinition> collection = new ArrayList<ParameterDefinition>();

        if (parameterDefinitions != null) {
            for (int i = 0; i < parameterDefinitions.length; i++) {
                collection.add(parameterDefinitions[i]);
            }
        }

        return collection;
    }
}