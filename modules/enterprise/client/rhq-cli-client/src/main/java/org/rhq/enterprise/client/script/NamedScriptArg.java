package org.rhq.enterprise.client.script;

public class NamedScriptArg extends ScriptArg {

    private String name;

    public NamedScriptArg(String name, String value) {
        super(value);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
}
