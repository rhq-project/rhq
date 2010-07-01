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

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Helper to replace tokens by their values
 * @author Heiko W. Rupp
 */
public class AlertTokenReplacer {

    private final Log log = LogFactory.getLog(AlertTokenReplacer.class);
    public static final String NOT_YET_IMPLEMENTED = " - not yet implemented -";
    protected static final String THE_QUICK_BROWN_FOX_JUMPS_OVER_THE_LAZY_DOG = "TheQuickBrownFoxJumpsOverTheLazyDOg";
    private Alert alert;
    private Pattern pattern;
    private OperationDefinition operationDefinition;
    private Resource targetResource;

    public AlertTokenReplacer(Alert alert, OperationDefinition operationDefinition, Resource targetResource) {
        this.alert = alert;
        this.operationDefinition = operationDefinition;
        this.targetResource = targetResource;
        pattern = Pattern.compile("<%\\s*([a-zA-Z]+\\.[a-zA-Z0-9]+)\\s*%>");
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
            if (log.isDebugEnabled()) {
                log.debug("No tokens found in " + input);
            }
            return input;
        }
        matcher.reset();

        do {
            matcher = pattern.matcher(work);
            if (!matcher.find()) {
                break;
            }
            String replacement = replaceToken(matcher.group(1));
            String s = matcher.replaceFirst(replacement);
            work = s;

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
        if (tc == null) {
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
            ret = replaceAlertToken(token, alert);
            break;
        case RESOURCE:
            ret = replaceResourceToken(token, alert.getAlertDefinition().getResource());
            break;
        case TARGET_RESOURCE:
            // Create a "pseudo" token to feed it into the plain resource replacement code
            String text = "resource." + token.getName();
            Token tok = Token.getByText(text);
            ret = replaceResourceToken(tok, targetResource);
            break;
        case OPERATION:
            ret = replaceOperationToken(token);
            break;
        case TEST:
            switch (token) {
            case TEST_ECHO:
                ret = tokenString;
                break;
            case TEST_FIX:
                ret = THE_QUICK_BROWN_FOX_JUMPS_OVER_THE_LAZY_DOG;
                break;
            case TEST_CAMEL:
                ret = "camel";
                break;
            default:
                ret = NOT_YET_IMPLEMENTED;
            }
            break;
        }
        return ret;
    }

    private String replaceAlertToken(Token token, Alert alert) {

        AlertManagerLocal mgr = LookupUtil.getAlertManager();

        switch (token) {
        case ALERT_ID:
            return String.valueOf(alert.getId());
        case ALERT_FIRE_TIME:
            return new Date(alert.getCtime()).toString(); // TODO use a specific impl here?
        case ALERT_WILL_RECOVER:
            return String.valueOf(alert.getAlertDefinition().getWillRecover());
        case ALERT_WILL_DISABLE:
            return String.valueOf(mgr.willDefinitionBeDisabled(alert));
        case ALERT_DEF_NAME:
            return alert.getAlertDefinition().getName();
        case ALERT_DEF_DESC:
            return alert.getAlertDefinition().getDescription();
        case ALERT_DEF_PRIO:
            return alert.getAlertDefinition().getPriority().getName();
        case ALERT_URL:
            return mgr.prettyPrintAlertURL(alert);
        case ALERT_CONDITIONS:
            return mgr.prettyPrintAlertConditions(alert, false);

        default:
            return NOT_YET_IMPLEMENTED;
        }

    }

    private String replaceResourceToken(Token token, Resource resource) {

        ResourceManagerLocal mgr = LookupUtil.getResourceManager();
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        Resource parent;
        Resource platform = mgr.getPlaformOfResource(overlord, resource.getId());
        if (platform == null)
            platform = resource;

        switch (token) {
        case RESOURCE_ID:
            return String.valueOf(resource.getId());
        case RESOURCE_NAME:
            return resource.getName();
        case RESOURCE_PARENT_ID:
            parent = mgr.getParentResource(resource.getId());
            if (parent == null)
                return "0";
            else
                return String.valueOf(parent.getId());
        case RESOURCE_PARENT_NAME:
            parent = mgr.getParentResource(resource.getId());
            if (parent == null)
                return "0";
            else
                return String.valueOf(parent.getId());
        case RESOURCE_TYPE_ID:
            return String.valueOf(resource.getResourceType().getId());
        case RESOURCE_TYPE_NAME:
            return resource.getResourceType().getName();
        case RESOURCE_PLATFORM_ID:
            return String.valueOf(platform.getId());
        case RESOURCE_PLATFORM_NAME:
            return platform.getName();
        case RESOURCE_PLATFORM_TYPE:
            return platform.getResourceType().getName();

        default:
            return NOT_YET_IMPLEMENTED;
        }
    }

    private String replaceOperationToken(Token token) {

        switch (token) {
        case OPERATION_ID:
            return String.valueOf(operationDefinition.getId());
        case OPERATION_NAME:
            return operationDefinition.getName();

        default:
            return NOT_YET_IMPLEMENTED;
        }
    }
}
