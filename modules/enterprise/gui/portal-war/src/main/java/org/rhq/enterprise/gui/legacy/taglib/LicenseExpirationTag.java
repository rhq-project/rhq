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
package org.rhq.enterprise.gui.legacy.taglib;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class LicenseExpirationTag extends TagSupport {
    private static long lastRead = 0;
    private static String expiration = null;

    public static final long MILLIS_IN_DAY = 1000 * 60 * 60 * 24;
    public static final long WARN_EXPIRE_DAYS = 30;
    public static final long REFRESH_MILLIS = 1000 * 60 * 60;

    public static final SimpleDateFormat DFORMAT = new SimpleDateFormat("MMMM dd, yyyy");

    public static final String PREFIX = "<tr>" + "\n  <td colspan=\"9\" class=\"FooterRegular\">";
    public static final String SUFFIX = " <a href=\"http://www.jboss.com\">Contact "
        + "JBoss</a> to renew your license!</td>\n</tr>";

    public LicenseExpirationTag() {
        super();
    }

    private String loadExpiration() {
        long now = System.currentTimeMillis();
        if ((expiration != null) && ((now - lastRead) < REFRESH_MILLIS)) {
            return expiration;
        }

        SystemManagerLocal systemManager = LookupUtil.getSystemManager();
        Date expiresDate = systemManager.getExpiration();

        if (expiresDate == null) {
            // No expiration
            return "";
        }

        long numDays = (expiresDate.getTime() - now) / MILLIS_IN_DAY;
        if (numDays > WARN_EXPIRE_DAYS) {
            return "";
        }

        if (numDays <= 0) {
            return PREFIX + "LICENSE EXPIRES TODAY!" + SUFFIX;
        } else {
            return PREFIX + "LICENSE EXPIRES IN " + numDays + " DAYS (on " + DFORMAT.format(expiresDate) + ")" + SUFFIX;
        }
    }

    public final int doStartTag() throws JspException {
        try {
            pageContext.getOut().write(loadExpiration());
        } catch (IOException e) {
            throw new JspTagException(e.toString());
        }

        return SKIP_BODY;
    }
}