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
package org.rhq.enterprise.gui.legacy.taglib.display;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to decorate the specified value, formatting it to be viewed in HTML. Author: Jason Dobies
 */
public class HtmlDecorator extends BaseDecorator {
    // --------------------------------------------------------------------
    //   Class Variables

    /**
     * Pattern for matching line feeds.
     */
    private static final Pattern PATTERN_LINE_BREAK = Pattern.compile("\n");

    /**
     * Replacement text for line feed matches.
     */
    private static final String REPLACEMENT_LINE_BREAK = "<br/>\n";

    // --------------------------------------------------------------------
    //   BaseDecorator Implementation

    public String decorate(Object obj) {
        if (obj == null) {
            return null;
        }

        Matcher matcher = PATTERN_LINE_BREAK.matcher(obj.toString());

        String result = matcher.replaceAll(REPLACEMENT_LINE_BREAK);

        return result;
    }
}