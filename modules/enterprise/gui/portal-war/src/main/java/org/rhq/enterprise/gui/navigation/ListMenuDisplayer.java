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
package org.rhq.enterprise.gui.navigation;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.apache.struts.util.MessageResources;

/**
 * Based on @see net.sf.navigator.displayer.ListMenuDisplayer
 *
 * @author <a href="ccrouch@jboss.com">Charles Crouch</a>
 */
public class ListMenuDisplayer extends net.sf.navigator.displayer.ListMenuDisplayer {
    private static final String STRUTS_MESSAGE_KEY_NOT_FOUND_PREFIX = "???";
    private static final String STRUTS_MESSAGE_KEY_NOT_FOUND_SUFFIX = "???";

    /**
     * Get the title key from the bundle (if it exists). This method is public to expose it to Velocity. Override this
     * method from MessageResourcesMenuDisplayer, until bug 1259076 is fixed:
     * http://sourceforge.net/tracker/index.php?func=detail&aid=1259076&group_id=48726&atid=453974
     *
     * @param key the key
     */
    public String getMessage(String key) {
        String message = null;

        if ((messages != null) && (messages instanceof ResourceBundle)) {
            if (log.isDebugEnabled()) {
                log.debug("Looking up string '" + key + "' in ResourceBundle");
            }

            ResourceBundle bundle = (ResourceBundle) messages;
            try {
                message = bundle.getString(key);
            } catch (MissingResourceException mre) {
                message = null;
            }
        } else if (messages != null) {
            //            if (log.isDebugEnabled()) {
            //                log.debug("Looking up message '" + key + "' in Struts' MessageResources");
            //            }
            // this is here to prevent a non-struts webapp from throwing a NoClassDefFoundError
            if ("org.apache.struts.util.PropertyMessageResources".equals(messages.getClass().getName())) {
                MessageResources resources = (MessageResources) messages;
                try {
                    if (locale != null) {
                        //Method method = clazz.getMethod("getMessage", new Class[] {Locale.class, String.class});
                        message = resources.getMessage(locale, key);
                    } else {
                        message = resources.getMessage(key);
                    }
                } catch (Throwable t) {
                    message = null;
                }

                if ((message != null) && message.startsWith(STRUTS_MESSAGE_KEY_NOT_FOUND_PREFIX)
                    && message.endsWith(STRUTS_MESSAGE_KEY_NOT_FOUND_SUFFIX)) {
                    message = null;
                }
            }
        } else {
            message = key;
        }

        if (message == null) {
            message = key;
        }

        return message;
    }
}