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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.resource.Resource;

/**
 * Helper to replace tokens by their values
 * @author Heiko W. Rupp
 */
public class TokenReplacer {

    private final Log log = LogFactory.getLog(TokenReplacer.class);
    private static final String NOT_YET_IMPLEMENTED = " - not yet implemented -";
    protected static final String THE_QUICK_BROWN_FOX_JUMPS_OVER_THE_LAZY_DOG = "TheQuickBrownFoxJumpsOverTheLazyDOg";
    private Alert alert;
    private Pattern pattern;

    public TokenReplacer(Alert alert) {
        this.alert = alert;
        pattern = Pattern.compile("<%\\s*([a-z]+\\.[a-z0-9]+)\\s*%>");
    }

    /**
     * Replace all tokens on the input line. If no tokens are found the input is returned.
     * Tokens have the form '<i><% class.sub %></i>'
     * @param input a line of text
     * @return input with tokens replaced.
     * @see org.rhq.enterprise.server.plugins.alertOperations.Token
     * @see org.rhq.enterprise.server.plugins.alertOperations.TokenClass
     */
    public String replaceTokens(String input) {

        String work = input;
        Matcher matcher = pattern.matcher(work);
        if (!matcher.find()) {
            log.warn("No tokens found in " + input);
            return input;
        }
        matcher.reset();

        do {
//            System.out.println(input);
            matcher = pattern.matcher(work);
            if (!matcher.find()) {
                break;
            }
//            System.out.println(matcher.regionStart() + ":" + matcher.regionEnd() + input.substring(matcher.regionStart(),matcher.regionEnd()));
//            System.out.println(matcher.group(1));
            String replacement = replaceToken(matcher.group(1)                                                                                                                               );
            String s = matcher.replaceFirst(replacement);
//            System.out.println(s);
            work = s;

//            System.out.println("----");
        } while (true);

        return work;
    }



    /**
     * Replace the token string passed (without the token delimiters ) with the actual value
     * @param tokenString Input like alert.id
     * @return replacement string or the input if the token was not valid.
     */
    public String replaceToken(String tokenString) {

        // Ok, we have at least one token. Now split the tokenString and loop over the tokens

        if (!tokenString.contains("."))
            return tokenString;

        String tmp = tokenString.substring(0, tokenString.indexOf("."));
        TokenClass tc = TokenClass.getByText(tmp);
        if (tc==null) {
            log.warn("Unknown token class in [" + tokenString + "], not replacing tokens");
            return tokenString;
        }

        Token token = Token.getByText(tokenString);
        if (token == null) {
            log.warn("No known token found in [" + tokenString + "], not replacing token");
            return tokenString;
        }
        String ret = null;
        switch (tc) {
            case ALERT:
                ret = replaceAlertToken(token,alert);
                break;
            case RESOURCE:
                ret = replaceResourceToken(token,alert.getAlertDefinition().getResource());
                break;
            case TARGET_RESOURCE:
                Resource resource = null; // TODO
                ret = replaceTargetResourceToken(token, resource);
                break;
            case TEST:
                switch (token) {
                case TEST_ECHO:
                    ret = tokenString;
                    break;
                case TEST_FIX:
                    ret = THE_QUICK_BROWN_FOX_JUMPS_OVER_THE_LAZY_DOG;
                    break;
                default:
                    ret = NOT_YET_IMPLEMENTED;
                }
                break;
        }
        return ret;
    }

    private String replaceAlertToken(Token token, Alert alert) {

        switch (token) {
            case ALERT_ID:
                return String.valueOf(alert.getId());
            case ALERT_URL:
                return NOT_YET_IMPLEMENTED;

            default:
                return NOT_YET_IMPLEMENTED;
        }

    }

    private String replaceResourceToken(Token token, Resource resource) {

        switch (token) {
        case RESOURCE_ID:
            return String.valueOf(resource.getId());
        case RESOURCE_NAME:
            return resource.getName();

        default:
            return NOT_YET_IMPLEMENTED;
        }
    }

    private String replaceTargetResourceToken(Token token, Resource resource) {

        switch (token) {
        case TRESOURCE_ID:
            return String.valueOf(resource.getId());
        case TRESOURCE_NAME:
            return resource.getName();

        default:
            return NOT_YET_IMPLEMENTED;
        }
    }

}
