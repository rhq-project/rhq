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
 * Tokens that can be replaced in
 * @author Heiko W. Rupp
 */
public enum Token {

    // Alert related tokens
    ALERT_ID(TokenClass.ALERT, "id"),
    ALERT_URL(TokenClass.ALERT, "url"),

    // resource that triggered the alert related tokens
    RESOURCE_ID(TokenClass.RESOURCE, "id"),
    RESOURCE_NAME(TokenClass.RESOURCE, "name"),


    // resource the operation is run on related tokens
    TRESOURCE_ID(TokenClass.TARGET_RESOURCE, "id"),
    TRESOURCE_NAME(TokenClass.TARGET_RESOURCE, "name"),


    // only for testing
    TEST_ECHO(TokenClass.TEST,"echo"),
    TEST_FIX(TokenClass.TEST,"fix")

    ;


    private String text;

    private Token(TokenClass tc, String text) {

        this.text = tc.getText() + "." + text;
    }

    /**
     * Return the token that matches the input text or null if not found.
     * The token delimiters need to be already stripped from the input
     * @param input a token text like <i>alert.id</i>, which would return the
     * <i>ALERT_ID</i> token.
     * @return The matching token or null if not found
     */
    public static Token getByText(String input) {
        EnumSet<Token> es = EnumSet.allOf(Token.class);
        for (Token t : es) {
            if (t.text.equals(input))
                return t;
        }
        return null;
    }
}
