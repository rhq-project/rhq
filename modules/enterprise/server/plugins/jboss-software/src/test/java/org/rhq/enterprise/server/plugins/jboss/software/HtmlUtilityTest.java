package org.rhq.enterprise.server.plugins.jboss.software;

import org.testng.annotations.Test;

/**
 * @author Ian Springer
 */
public class HtmlUtilityTest {
    @Test
    public void testUnescapeHTML() throws Exception {
        String result = HtmlUtility.unescapeHTML("  &lt;html&gt;\nblah&nbsp;\n&lt;/html&gt;&foo&bleh;");
        System.out.println("result=[" + result + "]");
        assert result.equals("  <html>\nblah \n</html>&foo&bleh;");
    }
}
