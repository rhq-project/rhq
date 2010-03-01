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
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Print the available tokens to stdout, for documentation generation purposes
 * @author Heiko W. Rupp
 */
public class PrintTokens {

    private final Log log = LogFactory.getLog(PrintTokens.class);
    private static final String CLOSE = "\">\n";

    public static void main(String[] args) throws Exception {

        String text = createTokenDescription();

        System.out.println(text);
    }

    /**
     * Do the work and return an xml structure that lists available token classes and
     * tokens along with their descriptions.
     * @return String with an XML representation of the available tokens
     */
    public static String createTokenDescription() {
        EnumSet<TokenClass> tokenClasses = EnumSet.allOf(TokenClass.class);

        StringBuilder builder = new StringBuilder("<tokenClasses>\n");

        for (TokenClass tc : tokenClasses ) {
            builder.append("  <tokenClass name=\"").append(tc.getText()).
                    append("\"").append(" description=\"").append(tc.getDescription()).
                    append(CLOSE);

            Set<Token> tokens = Token.getByTokenClass(tc);
            for (Token token : tokens) {
                builder.append("    <token name=\"").append(token.getName()).append(CLOSE);
                builder.append("      <fullName>").append(token.getText()).append("</fullName>\n");
                builder.append("      <descr>").append(token.getDescription()).append("</descr>\n");
                builder.append("    </token>\n");

            }

            builder.append("  </tokenClass>\n");

        }

        builder.append("</tokenClasses>\n");
        return builder.toString();
    }
}
