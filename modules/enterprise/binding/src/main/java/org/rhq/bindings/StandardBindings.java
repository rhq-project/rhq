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

package org.rhq.bindings;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.rhq.bindings.client.ResourceClientFactory;
import org.rhq.bindings.client.RhqFacade;
import org.rhq.bindings.export.Exporter;
import org.rhq.bindings.output.TabularWriter;
import org.rhq.bindings.util.ScriptAssert;
import org.rhq.bindings.util.ScriptUtil;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.PageControl;

/**
 * This class encapsulates the standard set of data inserted in the {@link ScriptContext#ENGINE_SCOPE} of the
 * script engines.
 * <p>
 * Upon instantiation this class has all the properties initialized with the default values so that a common base
 * is available for all the users of this class. The user is however able to later modify these defaults to provide
 * use-case specific overrides (for example the CLI might want to supply impls that hook into the console to provide
 * interactivity with user in some workflows, which, generically, is not possible in a common case).
 *
 * @author Lukas Krejci
 */
public class StandardBindings extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    public static final String UNLIMITED_PC = "unlimitedPC";
    public static final String PAGE_CONTROL = "pageControl";
    public static final String EXPORTER = "exporter";
    public static final String SUBJECT = "subject";
    public static final String PRETTY = "pretty";
    public static final String SCRIPT_UTIL = "scriptUtil";
    public static final String PROXY_FACTORY = "ProxyFactory";
    public static final String ASSERT = "Assert";

    private Map<String, Object> managers;

    private static class CastingEntry<T> implements Map.Entry<String, T> {

        private Map.Entry<String, Object> inner;
        private Class<T> clazz;

        public CastingEntry(Map.Entry<String, Object> inner, Class<T> clazz) {
            this.inner = inner;
            this.clazz = clazz;
        }

        public String getKey() {
            return inner.getKey();
        }

        public T getValue() {
            return clazz.cast(inner.getValue());
        }

        public T setValue(T value) {
            return clazz.cast(inner.setValue(value));
        }

    }

    public StandardBindings(PrintWriter output, RhqFacade rhqFacade) {
        PageControl pc = new PageControl();
        pc.setPageNumber(-1);

        managers = rhqFacade.getManagers();

        put(UNLIMITED_PC, pc);
        put(PAGE_CONTROL, PageControl.getUnlimitedInstance());
        put(EXPORTER, new Exporter());
        put(SUBJECT, rhqFacade.getSubject());
        put(PRETTY, new TabularWriter(output));
        put(SCRIPT_UTIL, new ScriptUtil(rhqFacade));
        put(PROXY_FACTORY, new ResourceClientFactory(rhqFacade, output));
        put(ASSERT, new ScriptAssert());

        putAll(managers);

        //I don't think these should belong in the standard bindings
        //because all the controller does is "login/logout" as methods
        //of the "rhq" object. ConfigurationEditor is interactive and
        //thus unusable outside of console bound CLI.
        
        //(the below were originally defined in the ScriptCommand)
        //bindObjectAndGlobalFuctions(new Controller(client), "rhq");
        //bindObjectAndGlobalFuctions(new ConfigurationEditor(client), "configurationEditor");
    }

    public void preInject(ScriptEngine scriptEngine) {
        ((ScriptUtil) get(SCRIPT_UTIL)).init(scriptEngine);
        ((ScriptAssert) get(ASSERT)).init(scriptEngine);
    }

    public void postInject(ScriptEngine scriptEngine) {        
        ScriptEngineFactory.bindIndirectionMethods(scriptEngine, SCRIPT_UTIL, get(SCRIPT_UTIL));
        ScriptEngineFactory.bindIndirectionMethods(scriptEngine, ASSERT, get(ASSERT));
    }
    
    public Map.Entry<String, PageControl> getUnlimitedPC() {
        return castEntry(UNLIMITED_PC, PageControl.class);
    }

    public Map.Entry<String, PageControl> getPageControl() {
        return castEntry(PAGE_CONTROL, PageControl.class);
    }

    public Map.Entry<String, Exporter> getExporter() {
        return castEntry(EXPORTER, Exporter.class);
    }

    public Map.Entry<String, Subject> getSubject() {
        return castEntry(SUBJECT, Subject.class);
    }

    public Map.Entry<String, TabularWriter> getPretty() {
        return castEntry(PRETTY, TabularWriter.class);
    }

    public Map.Entry<String, ScriptUtil> getScriptUtil() {
        return castEntry(SCRIPT_UTIL, ScriptUtil.class);
    }

    public Map.Entry<String, ResourceClientFactory> getProxyFactory() {
        return castEntry(PROXY_FACTORY, ResourceClientFactory.class);
    }

    public Map<String, Object> getManagers() {
        //XXX ideally this should be a projection into our map        
        return managers;
    }

    private Map.Entry<String, Object> getEntry(String key) {
        for (Map.Entry<String, Object> entry : entrySet()) {
            if (key == null && entry.getKey() == null) {
                return entry;
            } else if (key.equals(entry.getKey())) {
                return entry;
            }
        }

        return null;
    }

    private <T> Map.Entry<String, T> castEntry(String key, Class<T> clazz) {
        Map.Entry<String, Object> entry = getEntry(key);
        if (entry == null) {
            return null;
        }

        return new CastingEntry<T>(entry, clazz);
    }
}
