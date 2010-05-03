package org.rhq.core.template;

import java.util.TreeMap;

import junit.framework.TestCase;

import org.testng.annotations.Test;

@Test
public class TemplateEngineTest extends TestCase {
    private static final String IPADDR = "192.168.22.153";
    private static final String SUCCESSTOKEN1 = "successtoken1";
    private static final String WINTEMPDIR = "C:\\Users\\JSHAUG~1\\Documents and Settings\\Local\\Temp\\";
    String noTokens = "This string should come through unchanged";
    String justOneToken = "@@rhq.token1@@";
    String oneTokenWhiteSpace = "@@ rhq.token1 @@";
    String multipleLogTokens = "This string has @@rhq.token1@@\n" + "@@ rhq.unsettoken @@\n" + "@@ nmsqt.token1 @@"
        + "It also has @@rhq.platform.ethers.eth1.ipaddress @@";
    String sameTokenTwice = "@@rhq.token1@@@@rhq.token1@@";

    TemplateEngine templateEngine;
    TreeMap<String, String> tokens;

    TreeMap<String, String> getTokens() {
        if (null == tokens) {
            tokens = new TreeMap<String, String>();
            tokens.put("rhq.token1", SUCCESSTOKEN1);
            tokens.put("rhq.platform.ethers.eth1.ipaddress", IPADDR);
            tokens.put("rhq.system.sysprop.java.io.tmpdir", WINTEMPDIR);
        }
        return tokens;
    }

    @Override
    protected void setUp() throws Exception {
        templateEngine = new TemplateEngine(getTokens());
    }

    public void testNoTokens() {
        assertEquals(noTokens, templateEngine.replaceTokens(noTokens));
    }

    public void testOneToken() {
        assertEquals(SUCCESSTOKEN1, templateEngine.replaceTokens(justOneToken));
    }

    public void testWinToken() {
        assertEquals(WINTEMPDIR, templateEngine.replaceTokens("@@rhq.system.sysprop.java.io.tmpdir@@"));
        assertEquals(WINTEMPDIR + "-" + WINTEMPDIR, templateEngine
            .replaceTokens("@@rhq.system.sysprop.java.io.tmpdir@@-@@ rhq.system.sysprop.java.io.tmpdir @@"));
    }

    public void testOneTokenWhiteSpace() {
        assertEquals(SUCCESSTOKEN1, templateEngine.replaceTokens(oneTokenWhiteSpace));
    }

    public void testLongString() {
        assertTrue(templateEngine.replaceTokens(multipleLogTokens).indexOf(SUCCESSTOKEN1) > 0);
        assertTrue(templateEngine.replaceTokens(multipleLogTokens).indexOf(IPADDR) > 0);
    }

    public void testIgnore() {
        assertTrue(templateEngine.replaceTokens(multipleLogTokens).indexOf("@@ rhq.unsettoken @@") > 0);
        assertTrue(templateEngine.replaceTokens(multipleLogTokens).indexOf("@@ nmsqt.token1 @@") > 0);
    }

    public void testSameTokenTwice() {
        assertEquals(SUCCESSTOKEN1 + SUCCESSTOKEN1, templateEngine.replaceTokens(sameTokenTwice));
    }
}
