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

    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }

    public void assertEqual(Object lefty, Object righty) {
        assertEqual(lefty, righty, null);
    }

    public void assertEqual(Object lefty, Object righty, String errorMessage) {
        boolean result;
        if (lefty == null) {
            if (righty == null) {
                result = true; // both null
            } else {
                result = false; // only lefty is null
            }
        } else {
            if (righty == null) {
                result = false; // only righty is null
            } else {
                result = lefty.equals(righty) && righty.equals(lefty);
            }
        }
        if (result == false) {
            if (errorMessage == null) {
                throw new AssertionError();
            } else {
                throw new AssertionError(errorMessage);
            }
        }
    }

}
