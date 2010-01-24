/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugins.alertScriptlang;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;

/**
 * Component class for scripting languages
 * @author Heiko W. Rupp
 */
public class ScriptLangComponent implements ServerPluginComponent {

    Map<String, ScriptEngine> engineByName = new HashMap<String, ScriptEngine>();

    String baseDir = System.getProperty("jboss.home.dir") + "/../alert-scripts/";

    public void initialize(ServerPluginContext context) throws Exception {
        // TODO: Customise this generated block
    }

    public void start() {
        // TODO: Customise this generated block
    }

    public void stop() {
        // TODO: Customise this generated block
    }

    public void shutdown() {
        // TODO: Customise this generated block
    }

    public ScriptEngine getEngineByLanguage(String language) {

        ScriptEngine engine;
        engine = engineByName.get(language);
        if (engine == null) {
            ScriptEngineManager manager = new ScriptEngineManager();
            engine = manager.getEngineByName(language);
            engineByName.put(language, engine);
        }
        return engine;

    }
}
