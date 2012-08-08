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

import java.io.PrintWriter;
import java.util.List;

import javax.script.ScriptContext;

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
     * The code completor can use the provided script context to find out about possible
     * completions.
     * <p>
     * This method is called once before the first {@link #complete(PrintWriter, String, int, List)} method
     * is called.
     * 
     * @param scriptContext
     */
    void setScriptContext(ScriptContext scriptContext);
    
    /**
     * Provides the code completion implementation with the metadata provider it can use to
     * format more meaningful completion hints.
     * 
     * @param metadataProvider
     */
    void setMetadataProvider(MetadataProvider metadataProvider);

    /**
     * Generates the completion candidates and fills the supplied list with them.
     * 
     * @param output the output the completor can use to write some additional information to convey some
     * additional info to the user
     * @param context the context of the code completion, usually that is the current
     * statement being edited by the user
     * @param cursorPosition the position of the cursor inside the context
     * @param completions this should be a list of strings but is intentionally left raw
     * so that this interface is compatible with jline's <code>Completor</code>.
     * @return the character index in the context for which the completion candidates were
     * actually generated (might be different from the cursorPosition).
     */
    int complete(PrintWriter output, String context, int cursorPosition, @SuppressWarnings("rawtypes") List candidates);
}
