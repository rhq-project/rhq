/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.LogFactory;

/*
 * @author Jay Shaughnessy
 */
abstract public class AbstractVersionedSubsystemDiscovery extends SubsystemDiscovery {

    /* The matched format is name-VERSION.ext.  Version must minimally be in major.minor format.  Simpler versions,
     * like a single digit, are too possibly part of the actual name.  myapp-1.war and myapp-2.war could easily be
     * different apps (albeit poorly named).  But myapp-1.0.war and myapp-2.0.war are pretty clearly versions of
     * the same app.  The goal was to handle maven-style versioning.
     */
    static private final String PATTERN_DEFAULT = "^(.*?)-([0-9]+\\.[0-9].*)(\\..*)$";
    static private final String PATTERN_DISABLE = "disable";
    static private final String PATTERN_PROP = "rhq.as7.VersionedSubsystemDiscovery.pattern";

    static protected final Matcher MATCHER;
    static protected final String SUBDEPLOYMENT_TYPE = "Subdeployment";
    static protected boolean DISABLED = false;

    static {
        Matcher m = null;
        try {
            String override = System.getProperty(PATTERN_PROP);
            if (null != override) {
                if (override.toLowerCase().startsWith(PATTERN_DISABLE)) {
                    DISABLED = true;

                } else {
                    Pattern p = Pattern.compile(override);
                    m = p.matcher("");
                    if (m.groupCount() != 3) {
                        String msg = "Pattern supplied by system property [" + PATTERN_PROP
                            + "] is invalid. Expected [3] matching groups but found [" + m.groupCount()
                            + "]. Will use default pattern [" + PATTERN_DEFAULT + "].";
                        m = null;
                        LogFactory.getLog(AbstractVersionedSubsystemDiscovery.class).error(msg);
                    }
                }
            }
        } catch (Exception e) {
            String msg = "Pattern supplied by system property [" + PATTERN_PROP
                + "] is invalid. Will use default pattern [" + PATTERN_DEFAULT + "].";
            m = null;
            LogFactory.getLog(AbstractVersionedSubsystemDiscovery.class).error(msg, e);
        }

        MATCHER = (null != m) ? m : Pattern.compile(PATTERN_DEFAULT).matcher("");
    }
}
