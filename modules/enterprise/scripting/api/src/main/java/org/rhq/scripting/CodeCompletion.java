/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.scripting;

import java.util.List;

/**
 * An interface for performing code completion in a given language.
 * This inspired by the jline's <code>Completor</code> interface but
 * is defined separately so that we don't introduce a dependency on 
 * jline where it doesn't make sense.
 * 
 * @author Lukas Krejci
 */
public interface CodeCompletion {

    /**
     * Generates the completion candidates and fills the supplied list with them.
     * 
     * @param context the context of the code completion, usually that is the current
     * statement being edited by the user
     * @param cursorPosition the position of the cursor inside the context
     * @param completions this should be a list of strings but is intentionally left raw
     * so that this interface is compatible with jline's <code>Completor</code>.
     * @return the character index in the context for which the completion candidates were
     * actually generated (might be different from the cursorPosition).
     */
    int complete(String context, int cursorPosition, @SuppressWarnings("rawtypes") List candidates);
}
