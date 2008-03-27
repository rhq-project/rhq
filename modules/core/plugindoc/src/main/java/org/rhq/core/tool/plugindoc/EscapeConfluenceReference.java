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
package org.rhq.core.tool.plugindoc;

import org.apache.velocity.app.event.implement.EscapeReference;

/**
 * A velocity {@link org.apache.velocity.app.event.ReferenceInsertionEventHandler} that escapes Confluence special
 * characters. Configure this handler as follows:
 * <code>
 * velocityConfig.setProperty("eventhandler.referenceinsertion.class", EscapeConfluenceReference.class.getName());
 * </code>
 *
 * @author Ian Springer
 */
public class EscapeConfluenceReference extends EscapeReference {
    private static final String CONFLUENCE_SPECIAL_CHARS = "{}[]|*_?-+^~#";
    private static final String MATCH_ATTRIBUTE = "eventhandler.escape.confluence.match";

    protected String escape(Object obj) {
        StringBuilder escapedString = new StringBuilder();
        String origString = obj.toString();
        for (int i = 0; i < origString.length(); i++) {
            char c = origString.charAt(i);
            if (CONFLUENCE_SPECIAL_CHARS.indexOf(c) != -1) {
               escapedString.append('\\');
            }
            escapedString.append(c);
        }
        return escapedString.toString();
    }

    protected String getMatchAttribute() {
        return MATCH_ATTRIBUTE;
    }
}
