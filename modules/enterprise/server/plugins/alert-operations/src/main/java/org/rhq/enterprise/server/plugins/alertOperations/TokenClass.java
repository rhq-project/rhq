/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugins.alertOperations;

import java.util.EnumSet;

/**
 * Class a @See{Token} can be in.
 * @author Heiko W. Rupp
 */
public enum TokenClass {

    ALERT("alert","Information about the alert itself"),
    RESOURCE("resource","Information about the resource that triggered the alert"),
    TARGET_RESOURCE("targetResource","Information about the resource the alert is fired on "),
    OPERATION("operation","Information about the triggered operation"),
    //
    TEST("test","Just some dummies for internal testing purposes");

    private String text;
    private String description;

    private TokenClass(String text,String description) {
        this.text = text;
        this.description = description;
    }

    public String getText() {
        return text;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Return the tokenclass that matches the input text or null if not found.
     * The token delimiters need to be already stripped from the input
     * @param input a token text like <i>alert</i>, which would return the
     * <i>ALERT</i> token class.
     * @return The matching token class or null if not found
     */
    public static TokenClass getByText(String input) {
        EnumSet<TokenClass> es = EnumSet.allOf(TokenClass.class);
        for (TokenClass t : es) {
            if (t.text.equals(input))
                return t;
        }
        return null;
    }
}
