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

import java.util.Collection;

/**
 * Iterates over a <code>Collection</code> of {@link ParameterDefinition} objects - only the optional parameters are
 * actually iterated (non-required parameters are ignored).
 *
 * <p>Note that a snapshot of the parameter definitions is taken at the time this iterator is created - concurrent
 * access is allowed on the original set of parameter definitions, however, changes made to the original collection are
 * not reflected by this iterator.</p>
 *
 * @author John Mazzitelli
 */
public class OptionalParameterDefinitionIterator extends AbstractParameterDefinitionIterator {
    /**
     * Constructor for {@link OptionalParameterDefinitionIterator} that provides an iterator over a collection of
     * parameter definitions - of which only the optional ones will be iterated.
     *
     * @param parameterDefinitions full set of parameter definitions
     */
    public OptionalParameterDefinitionIterator(Collection<ParameterDefinition> parameterDefinitions) {
        super(parameterDefinitions, false);
    }

    /**
     * Constructor for {@link OptionalParameterDefinitionIterator} that provides an iterator over an array of parameter
     * definitions - of which only the optional ones will be iterated.
     *
     * @param parameterDefinitions full set of parameter definitions
     */
    public OptionalParameterDefinitionIterator(ParameterDefinition[] parameterDefinitions) {
        super(parameterDefinitions, false);
    }
}