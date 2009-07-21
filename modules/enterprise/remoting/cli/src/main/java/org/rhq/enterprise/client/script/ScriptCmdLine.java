package org.rhq.enterprise.client.script;

import java.util.List;
import java.util.LinkedList;

public class ScriptCmdLine {
    public ScriptCmdLine() {
    }

    public static enum ArgType {
        INDEXED("indexed"),

        NAMED("named");

        private String value;

        private ArgType(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    private String scriptFileName;

    private ArgType argType = ArgType.INDEXED;

    private List<ScriptArg> args = new LinkedList<ScriptArg>();

    public String getScriptFileName() {
        return scriptFileName;
    }

    public void setScriptFileName(String scriptFileName) {
        this.scriptFileName = scriptFileName;
    }

    public ArgType getArgType() {
        return argType;
    }

    public void setArgType(ArgType argType) {
        this.argType = argType;
    }

    public List<ScriptArg> getArgs() {
        return args;
    }

    public void addArg(ScriptArg arg) {
        args.add(arg);
    }

}
