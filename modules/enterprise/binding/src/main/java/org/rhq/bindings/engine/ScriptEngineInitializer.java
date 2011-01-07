/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.bindings.engine;

import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Is able to instatiate a script engine and import packages into the context
 * of the engine. 
 *
 * @author Lukas Krejci
 */
public interface ScriptEngineInitializer {

    String getEngineName();
    
    ScriptEngine instantiate(List<String> packages) throws ScriptException;
}
