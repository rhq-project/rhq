package org.rhq.enterprise.client.utility;

import javax.script.ScriptEngine;

public class ScriptUtil {

    private ScriptEngine scriptEngine;

    public ScriptUtil(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    public boolean isDefined(String identifier) {
        return scriptEngine.get(identifier) != null;    
    }

}
