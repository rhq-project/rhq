package org.rhq.enterprise.server.search.assist;

import org.rhq.core.domain.auth.Subject;

public abstract class TabAwareSearchAssistant extends AbstractSearchAssistant {
    protected String tab;

    public TabAwareSearchAssistant(Subject subject, String tab) {
        super(subject);
        if (tab != null) {
            tab = tab.trim().toLowerCase();
            if (tab.equals("")) {
                tab = null;
            }
        }
        this.tab = tab;
    }
}
