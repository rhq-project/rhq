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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.Alert;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.plugin.pc.alert.ResultState;
import org.rhq.enterprise.server.plugin.pc.alert.SenderResult;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * The actual alert sender
 * @author Heiko W. Rupp
 */
public class ScriptLangSender extends AlertSender<ScriptLangComponent> {

    private final Log log = LogFactory.getLog(ScriptLangSender.class);

    @Override
    public SenderResult send(Alert alert) {

        String scriptName = alertParameters.getSimpleValue("name", null);
        if (scriptName == null) {
            return new SenderResult(ResultState.FAILURE, "No script given");
        }
        String language = alertParameters.getSimpleValue("language", "jruby");

        ScriptEngine engine = pluginComponent.getEngineByLanguage(language);
//        ScriptEngineManager manager = new ScriptEngineManager(serverPluginEnvironment.getPluginClassLoader());
//        engine = manager.getEngineByName(language);



        if (engine==null) {
            return new SenderResult(ResultState.FAILURE,"Script engine with name [" + language + "] does not exist");
        }

        File file = new File(pluginComponent.baseDir + scriptName);
        if (!file.exists() || !file.canRead()) {
            return new SenderResult(ResultState.FAILURE,
                    "Script [" + scriptName + "] does not exist or is not readable at [" + file.getAbsolutePath()+"]");
        }

        Object result;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));

            Map<String,String> preferencesMap = new HashMap<String, String>();
            for (String key: preferences.getSimpleProperties().keySet())
                preferencesMap.put(key,preferences.getSimple(key).getStringValue());

            Map<String,String> parameterMap = new HashMap<String, String>();
            for (String key: alertParameters.getSimpleProperties().keySet())
                parameterMap.put(key,alertParameters.getSimple(key).getStringValue());

            ScriptContext sc = engine.getContext();
            sc.setAttribute("alertPreferences",preferencesMap,ScriptContext.ENGINE_SCOPE);
            sc.setAttribute("alertParameters",parameterMap,ScriptContext.ENGINE_SCOPE);
            engine.eval(br);

            AlertManagerLocal alertManager = LookupUtil.getAlertManager();

            Object[] args = new Object[3];
            args[0] = alert;
            args[1] = alertManager.prettyPrintAlertURL(alert);
            args[2] = alertManager.prettyPrintAlertConditions(alert);
            result = ((Invocable) engine).invokeFunction("sendAlert", args);

            if (result == null) {
                return new SenderResult(ResultState.FAILURE,"Script ]" + scriptName + "] returned null, so success is unknown");
            }
            if (result instanceof SenderResult)
                return (SenderResult) result;

            return new SenderResult(ResultState.SUCCESS, "Sending via script resulted in " + result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return new SenderResult(ResultState.FAILURE, "Sending via [" + scriptName + "] failed: " + e.getMessage());
        }
    }
}
