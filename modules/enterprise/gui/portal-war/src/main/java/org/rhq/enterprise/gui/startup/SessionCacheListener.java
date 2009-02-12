package org.rhq.enterprise.gui.startup;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.auth.prefs.SubjectPreferencesCache;

public class SessionCacheListener implements HttpSessionListener {

    public void sessionCreated(HttpSessionEvent event) {
        // ignore the create event
    }

    public void sessionDestroyed(HttpSessionEvent event) {
        WebUser webUser = SessionUtils.getWebUser(event.getSession());
        if (webUser != null) {
            Subject subject = webUser.getSubject();
            if (subject != null) {
                SubjectPreferencesCache.getInstance().clearConfiguration(subject.getId());
            }
        }
    }
}
