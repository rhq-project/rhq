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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;

/**
 * // TODO: Document this
 * @author Heiko W. Rupp
 */
@Test
public class TokenReplacementTest {

    private final Log log = LogFactory.getLog(TokenReplacementTest.class);
    private static final String TEST_ECHO = "test.echo";
    private static final String FOO_BAR = "fooBar";
    private static final String FOO_DOT_BAR = "foo.bar";
    private static final String ALERT_BAR = "alert.bar";

    public void testSimpleReplacement() throws Exception {

        Alert al = new Alert();
        AlertTokenReplacer tr = new AlertTokenReplacer(al, null, null);


        String res = tr.replaceToken("test.fix");
        assert res != null;
        assert res.equals(AlertTokenReplacer.THE_QUICK_BROWN_FOX_JUMPS_OVER_THE_LAZY_DOG);

        res = tr.replaceToken(TEST_ECHO);
        assert res != null;
        assert res.equals(TEST_ECHO);

        res = tr.replaceToken("test.camelCase");
        assert res != null;
        assert res.equals("camel");
/*
        res = tr.replaceToken("alert.id");
        assert res != null;
        assert res.equals("0");

        Resource r = new Resource(1234);
        r.setName("A resource");
        AlertDefinition def = new AlertDefinition();
        def.setResource(r);
        al = new Alert(def, System.currentTimeMillis());
        tr = new AlertTokenReplacer(al, null, null);

        res = tr.replaceToken("resource.id");
        assert "1234".equals(res);
        res = tr.replaceToken("resource.name");
        assert "A resource".equals(res);
*/
    }

    public void testInvalidTokenString() {

        Alert al = new Alert();
        AlertTokenReplacer tr = new AlertTokenReplacer(al, null, null);


        String res = tr.replaceToken(FOO_BAR);
        assert res != null;
        assert res.equals(FOO_BAR);

        res = tr.replaceToken(FOO_DOT_BAR);
        assert res != null;
        assert res.equals(FOO_DOT_BAR);

        res = tr.replaceToken(ALERT_BAR);
        assert res != null;
        assert res.equals(ALERT_BAR);

    }

    public void testFullTokenSimple() {

        Alert al = new Alert();
        AlertTokenReplacer tr = new AlertTokenReplacer(al, null, null);


        String res = tr.replaceTokens("<%test.fix%>");
        assert res != null;
        assert !res.equals("<%test.fix%>") : "Res was " + res;
        assert res.equals(AlertTokenReplacer.THE_QUICK_BROWN_FOX_JUMPS_OVER_THE_LAZY_DOG);

        String TF2 = "<% test.fix %>";
        res = tr.replaceTokens(TF2);
        assert res != null;
        assert !res.equals(TF2) : "Res was " + res;

        String TF3 = "<% test.fix   %>";
        res = tr.replaceTokens(TF3);
        assert res != null;
        assert !res.equals(TF3) : "Res was " + res;

        String TF4 = "<% test.fix%> xXx <% test.echo%>";
        res = tr.replaceTokens(TF4);
        assert res != null;
        assert !res.equals(TF4) : "Res was " + res;

        res = tr.replaceTokens(FOO_BAR);
        assert res != null;
        assert res.equals(FOO_BAR) : "Res was " + res;

/*
        Resource r = new Resource(1234);
        r.setName("A resource");
        AlertDefinition def = new AlertDefinition();
        def.setResource(r);
        al = new Alert(def, System.currentTimeMillis());

        tr = new AlertTokenReplacer(al, null, null);
        res = tr.replaceTokens("<% test.fix%><%resource.id%>");
        assert res!=null;
        assert res.equals(AlertTokenReplacer.THE_QUICK_BROWN_FOX_JUMPS_OVER_THE_LAZY_DOG+"1234");
*/
    }

    public void testOperationTokens() {
        Resource r = new Resource(1234);
        AlertDefinition def = new AlertDefinition();
        def.setResource(r);
        Alert al = new Alert(def, System.currentTimeMillis());

        AlertTokenReplacer tr;
        OperationDefinition opDef = new OperationDefinition(123,"helloOps",null,"This is a test",0,"Just testing");
        tr = new AlertTokenReplacer(al,opDef, null);
        String res = tr.replaceTokens("<%operation.id%>");
        assert res!=null;
        assert res.equals("123") : "Result is " + res;
        res = tr.replaceTokens("<%operation.name%>");
        assert res!=null;
        assert res.equals("helloOps");
    }

    public void testTokenPrinter() {

        String result = PrintTokens.createTokenDescription();
        assert result != null;
        assert !result.equals("");
        assert result.contains("<token ");
    }

/*
    public void testReplaceAllTokens() {

        EnumSet<Token> tokens = EnumSet.allOf(Token.class);

        Resource r = new Resource(1234);
        AlertDefinition def = new AlertDefinition();
        def.setResource(r);
        Alert al = new Alert(def, System.currentTimeMillis());

        AlertTokenReplacer tr;
        OperationDefinition opDef = new OperationDefinition(123,"helloOps",null,"This is a test",0,"Just testing");
        tr = new AlertTokenReplacer(al,opDef, null);

        for (Token token : tokens) {

            String input = "<%" + token.getText() + "%>";
            String res = tr.replaceTokens(input);
            assert res != null;
            assert !res.isEmpty();
            assert !res.equals(AlertTokenReplacer.NOT_YET_IMPLEMENTED) : "Replacement for token [" + token.getText() + "] is not implemented";
        }
    }
*/
}
