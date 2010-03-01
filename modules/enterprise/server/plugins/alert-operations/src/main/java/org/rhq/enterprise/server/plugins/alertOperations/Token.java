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
import java.util.HashSet;
import java.util.Set;

/**
 * Tokens that can be replaced in operation parameters
 * @author Heiko W. Rupp
 */
public enum Token {

    // Alert related tokens
    ALERT_ID(TokenClass.ALERT, "id","The id of this particular alert"),
    ALERT_URL(TokenClass.ALERT, "url","Url to the alert details page"),
    ALERT_FIRE_TIME(TokenClass.ALERT, "firedAt","Time the alert fired"),
    ALERT_WILL_RECOVER(TokenClass.ALERT,"willRecover","?? TODO"),
    ALERT_CONDITIONS(TokenClass.ALERT,"conditions","A text representation of the conditions that led to this alert"),
    ALERT_DEF_NAME(TokenClass.ALERT,"name","Name from the defining alert definition"),
    ALERT_DEF_DESC(TokenClass.ALERT,"description","Description of this alert"),
    ALERT_DEF_PRIO(TokenClass.ALERT,"priority", "Priority of this alert"),
    ALERT_WILL_DISABLE(TokenClass.ALERT,"willBeDisabled","Will the alert definition be disabled after firing?"),


    // resource that triggered the alert related tokens
    RESOURCE_ID(TokenClass.RESOURCE, "id", "Id of the resource"),
    RESOURCE_NAME(TokenClass.RESOURCE, "name", "Name of the resource"),
    RESOURCE_PARENT_ID(TokenClass.RESOURCE, "parentId", "Id of the parent resource"),
    RESOURCE_PARENT_NAME(TokenClass.RESOURCE, "parentName", "Name of the parent resource"),
    RESOURCE_TYPE_ID(TokenClass.RESOURCE,"typeId","Resource type id"),
    RESOURCE_TYPE_NAME(TokenClass.RESOURCE,"typeName","Resource type name"),
    RESOURCE_PLATFORM_ID(TokenClass.RESOURCE,"platformId", "Id of the platform the resource is on"),
    RESOURCE_PLATFORM_NAME(TokenClass.RESOURCE,"platformName", "Name of the platform the resource is on"),
    RESOURCE_PLATFORM_TYPE(TokenClass.RESOURCE,"platformType", "Type of the platform the resource is on"),


    // resource the operation is run on related tokens
    TRESOURCE_ID(TokenClass.TARGET_RESOURCE, "id","Id of the target resource"),
    TRESOURCE_NAME(TokenClass.TARGET_RESOURCE, "name", "Name of the target resource"),
    TRESOURCE_PARENT_ID(TokenClass.TARGET_RESOURCE, "parentId", "Id of the target's parent resource"),
    TRESOURCE_PARENT_NAME(TokenClass.TARGET_RESOURCE, "parentName", "Name of the target's parent resource"),
    TRESOURCE_TYPE_ID(TokenClass.TARGET_RESOURCE,"typeId","Resource type of the target resource id"),
    TRESOURCE_TYPE_NAME(TokenClass.TARGET_RESOURCE,"typeName","Resource type name of the target resource"),
    TRESOURCE_PLATFORM_ID(TokenClass.TARGET_RESOURCE,"platformId", "Id of the platform the target resource is on"),
    TRESOURCE_PLATFORM_NAME(TokenClass.TARGET_RESOURCE,"platformName", "Name of the platform the target resource is on"),
    TRESOURCE_PLATFORM_TYPE(TokenClass.TARGET_RESOURCE,"platformType", "Type of the platform the target resource is on"),


    // Information about the fired operation
    OPERATION_ID(TokenClass.OPERATION,"id","Id of the operation fired"),
    OPERATION_NAME(TokenClass.OPERATION,"name","Name of the operation fired"),

    // only for testing
    TEST_ECHO(TokenClass.TEST,"echo", "Echo the input"),
    TEST_FIX(TokenClass.TEST,"fix", "Return a fixed string");


    private TokenClass tc;
    private String name;
    private String text;
    private String description;

    private Token(TokenClass tc, String name, String description) {

        this.tc = tc;
        this.name = name;
        this.text = tc.getText() + "." + name;
        this.description = description;
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

    public static Set<Token> getByTokenClass(TokenClass tokenClass) {

        Set<Token> tokens = new HashSet<Token>();

        EnumSet<Token> es = EnumSet.allOf(Token.class);
        for (Token t : es) {
            if (t.tc == tokenClass)
                tokens.add(t);
        }

        return tokens;
    }

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }

    public String getDescription() {
        return description;
    }
}
