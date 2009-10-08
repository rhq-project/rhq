/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.communications.util.prefs;

import java.io.PrintWriter;
import java.util.prefs.Preferences;
import org.jboss.remoting.InvokerLocator;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * Checks the validity of a JBoss/Remoting {@link InvokerLocator} URI parameters. This means the string is really a URI
 * query string (e.g. <code>name1=value1&foo&hello=world</code>).
 *
 * @author John Mazzitelli
 */
public class RemotingLocatorUriParamsValidityChecker implements SetupValidityChecker {
    /**
     * @see SetupValidityChecker#checkValidity(String, String, Preferences, PrintWriter)
     */
    public boolean checkValidity(String pref_name, String value_to_check, Preferences preferences, PrintWriter out) {
        try {
            if ((value_to_check != null) && (value_to_check.trim().length() > 0)) {
                new InvokerLocator("socket://127.0.0.1/?" + value_to_check);
            }

            return true;
        } catch (Exception e) {
            out.println(CommI18NFactory.getMsg().getMsg(CommI18NResourceKeys.SETUP_NOT_LOCATOR_URI_PARAMS,
                value_to_check, e));
            return false;
        }
    }
}