/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.startup;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.util.LookupUtil;

public class SessionCacheListener implements HttpSessionListener {

    public void sessionCreated(HttpSessionEvent event) {
        // ignore the create event
    }

    public void sessionDestroyed(HttpSessionEvent event) {
        WebUser webUser = SessionUtils.getWebUser(event.getSession());
        if (webUser != null) {
            Subject subject = webUser.getSubject();
            if (subject != null) {
                LookupUtil.getSubjectPreferencesCache().clearConfiguration(subject.getId());
            }
        }
    }
}
