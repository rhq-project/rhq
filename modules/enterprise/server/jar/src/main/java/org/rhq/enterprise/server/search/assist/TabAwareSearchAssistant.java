package org.rhq.enterprise.server.search.assist;

public abstract class TabAwareSearchAssistant extends AbstractSearchAssistant {
    protected String tab;

    public TabAwareSearchAssistant(String tab) {
        if (tab != null) {
            tab = tab.trim().toLowerCase();
            if (tab.equals("")) {
                tab = null;
            }
        }
        this.tab = tab;
    }
}
