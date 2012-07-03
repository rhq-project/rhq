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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This is the service interface for scripting language implementations for RHQ
 * (loaded using the META-INF/services mechanism).
 * <p>
 * Note that the API module provides no factory class that would load the ScriptEngineProvider 
 * implementations present on the classpath. That is because for the script engine to be
 * usable by RHQ, it needs to be initialized with a couple of dependencies that are not
 * appropriate for the API module.
 * <p>
 * The factory is located in the <code>rhq-script-bindings</code> module as 
 * <code>org.rhq.bindings.ScriptEngineFactory</code>.
 * 
 * @author Lukas Krejci
 */
public interface ScriptEngineProvider {

    /**
     * @return the scripting language understood by this provider.
     * This must return the same string as <code>ScriptEngine.getFactory().getParameter(ScriptEngine.NAME)</code>
     * of the script engine that this provider provides (through the initializer).
     */
    @NotNull
    String getSupportedLanguage();

    /**
     * @return an implementation of {@link ScriptEngineInitializer} that can instantiate
     * and initialize a script engine for the supported language for use with RHQ. 
     */
    @NotNull
    ScriptEngineInitializer getInitializer();

    /**
     * @return a {@link CodeCompletion} implementation for the supported language or null
     * if this provider doesn't provide one.
     */
    @Nullable
    CodeCompletion getCodeCompletion();
}
