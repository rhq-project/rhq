/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.enterprise.server.resource.group.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;

import org.testng.annotations.Test;

import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.enterprise.server.resource.group.LdapGroupManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerPluginService;
import org.rhq.enterprise.server.test.ldap.FakeLdapCtxFactory;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Integration tests for methods and operations performed by {@link LdapGroupManagerBean}.
 * which require an LDAP server.
 *
 * When executing these tests, the LDAP source can be changed using the following
 * system properties:
 *
 * <table>
 * <tr><th align="left">Property</th><th align="left">Desc</th><th align="left">Default</th></tr>
 * <tr>
 * <td valign="top">rhq.test.ldap.url</td><td>The URL to use for an LDAP connection.</td><td valign="top">ldap://localhost:389</td>
 * </tr>
 * <tr>
 * <td valign="top">rhq.test.ldap.LDAPFactory</td><td>The ContextFactory class that will create the initial
 * DirContext object for running the tests. This allows a mock or fake directory context to be
 * created by another test class helper. For example, this could be {@link FakeLdapCtxFactory}
 * or if you want to use a real LDAP server, you could specify com.sun.jndi.ldap.LdapCtxFactory
 * which would then perform real LDAP operations with an LDAP server.</td><td valign="top">{@link FakeLdapCtxFactory}</td>
 * </tr>
 * </table>
 *
 * @author loleary
 *
 */
public class LdapGroupManagerBeanTest extends AbstractEJB3Test {

    /**
     * The property name that represents the LDAP URL to use. If using a real
     * LDAP instance, this property's value should be the complete URL of the
     * LDAP server to use for these integration tests.
     */
    public static String RHQ_TEST_LDAP_URL_PROPERTY = "rhq.test.ldap.url";
    /**
     * The property name that represents the LDAP Context Factory to use. If
     * testing against a real LDAP instance, this property's value should be
     * a valid context factory name as provided by a JNDI implementation.
     *
     * If no value is specified for this property, {@link #RHQ_TEST_LDAP_DEFAULT_CONTEXT_FACTORY}
     * is used.
     */
    public static String RHQ_TEST_LDAP_LDAPFACTORY_PROPERTY = "rhq.test.ldap.LDAPFactory";

    /**
     * Default LDAP Context Factory value if a value is not defined by {@link #RHQ_TEST_LDAP_LDAPFACTORY_PROPERTY}
     */
    public static String RHQ_TEST_LDAP_DEFAULT_CONTEXT_FACTORY = FakeLdapCtxFactory.class.getCanonicalName();
    /**
     * Default LDAP URL value if a value is not defined by {@link #RHQ_TEST_LDAP_URL_PROPERTY}
     */
    public static String RHQ_TEST_LDAP_DEFAULT_URL = "ldap://localhost:389";

    private LdapGroupManagerLocal ldapGroupManager = null;
    private SystemManagerLocal systemManager = null;
    private TestServerPluginService testServerPluginService = null;

    @Override
    protected void beforeMethod() throws Exception {
        systemManager = LookupUtil.getSystemManager();
        ldapGroupManager = LookupUtil.getLdapGroupManager();

        //we need this because the drift plugins are referenced from the system settings that we use in our tests
        testServerPluginService = new TestServerPluginService(getTempDir());
        prepareCustomServerPluginService(testServerPluginService);
        testServerPluginService.startMasterPluginContainer();

        // get our Maven properties for LDAP testing
        java.net.URL url = LdapGroupManagerBeanTest.class.getClassLoader().getResource("test-ldap.properties");
        Properties mvnProps = new Properties();
        try {
            if (url == null) {
                throw new IOException("Unable to find test-ldap.properties in test envrionment's class loader");
            }
            mvnProps.load(url.openStream());
        } catch (IOException e) {
            System.err.println(" !!! Unable to load test-ldap.properties - All defaults are in place !!! ");
            e.printStackTrace();
        }

        // Was an LDAP factory given?
        String LDAPFactory = mvnProps.getProperty(RHQ_TEST_LDAP_LDAPFACTORY_PROPERTY);
        if ((LDAPFactory == null) || ("${" + RHQ_TEST_LDAP_LDAPFACTORY_PROPERTY + "}").equals(LDAPFactory)) {
            LDAPFactory = RHQ_TEST_LDAP_DEFAULT_CONTEXT_FACTORY; // none set use the default
        }
        this.setLdapCtxFactory(LDAPFactory);
        System.out.println("!! Initial LDAP Context Factory is " + LDAPFactory + " !!");

        String ldapUrl = mvnProps.getProperty(RHQ_TEST_LDAP_URL_PROPERTY);
        if ((ldapUrl == null) || ("${" + RHQ_TEST_LDAP_URL_PROPERTY + "}").equals(ldapUrl)) {
            ldapUrl = RHQ_TEST_LDAP_DEFAULT_URL; // none set so use default
        }
        this.setLdapUrl(ldapUrl);

        this.setLdapBaseDN("dc=test,dc=rhq,dc=redhat,dc=com");
        this.setLdapBindDN("uid=admin,ou=system");
        this.setLdapBindPassword("secret");
        this.setLdapUserFilter("objectClass=person");
        this.setLdapLoginAttribute("uid");
        this.setLdapGroupFilter("objectClass=groupOfNames");
        this.setLdapGroupMemberAttribute("member");
    }

    @Override
    protected void afterMethod() throws Exception {
        unprepareServerPluginService();
    }

    /**
     * Test {@link LdapGroupManagerBean#findAvailableGroupsFor(String)} method
     * using a user who does not exist in the test LDAP instance.
     *
     * The test verifies that no groups are returned for the non-existent user.
     *
     * @throws Throwable
     */
    @Test(groups = "integration.session")
    public void testFindGroupsForNonUser() throws Throwable {
        // non-existent user
        assertEquals(new HashSet<String>(), ldapGroupManager.findAvailableGroupsFor("gsmith"));
    }

    /**
     * Test {@link LdapGroupManagerBean#findAvailableGroupsFor(String)} method
     * using a user who has a no special characters in their cn attribute value.
     *
     * The test verifies that an expected group list is returned for each user.
     *
     * @throws Throwable
     */
    @Test(groups = "integration.session")
    public void testFindGroupsForUserSimpleUseCase() throws Throwable {
        assertEquals(new HashSet<String>(Arrays.asList("RHQ Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("rjosmith"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("jsmith"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Monitor Group")),
            ldapGroupManager.findAvailableGroupsFor("ssmith"));
    }

    /**
     * Test {@link LdapGroupManagerBean#findAvailableGroupsFor(String)} method
     * using a user who has a comma (,) in their cn attribute value.
     *
     * The test verifies that an expected group list is returned for each user.
     *
     * @throws Throwable
     */
    @Test(groups = "integration.session")
    public void testFindGroupsForUserCommaChar() throws Throwable {
        assertEquals(new HashSet<String>(Arrays.asList("RHQ Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("bcannon"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("ghause"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Monitor Group")),
            ldapGroupManager.findAvailableGroupsFor("bwalsh"));
    }

    /**
     * Test {@link LdapGroupManagerBean#findAvailableGroupsFor(String)} method
     * using a user who has a backslash (\) in their cn attribute value.
     *
     * The test verifies that an expected group list is returned for each user.
     *
     * @throws Throwable
     */
    @Test(groups = "integration.session")
    public void testFindGroupsForUserBackslashChar() throws Throwable {
        assertEquals(new HashSet<String>(Arrays.asList("RHQ Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("csamlin"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("cgroober"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Monitor Group")),
            ldapGroupManager.findAvailableGroupsFor("jkirk"));
    }

    /**
     * Test {@link LdapGroupManagerBean#findAvailableGroupsFor(String)} method
     * using a user who has a hash or pound sign (#) in their cn attribute value.
     *
     * The test verifies that an expected group list is returned for each user.
     *
     * @throws Throwable
     */
    @Test(groups = "integration.session")
    public void testFindGroupsForUserHashChar() throws Throwable {
        assertEquals(new HashSet<String>(Arrays.asList("RHQ Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("csellers"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("brogers"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Monitor Group")),
            ldapGroupManager.findAvailableGroupsFor("sphillips"));
    }

    /**
     * Test {@link LdapGroupManagerBean#findAvailableGroupsFor(String)} method
     * using a user who has a plus sign (+) in their cn attribute value.
     *
     * The test verifies that an expected group list is returned for each user.
     *
     * @throws Throwable
     */
    @Test(groups = "integration.session")
    public void testFindGroupsForUserPlusChar() throws Throwable {
        assertEquals(new HashSet<String>(Arrays.asList("RHQ Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("bbalanger"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("sreed"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Monitor Group")),
            ldapGroupManager.findAvailableGroupsFor("woverture"));
    }

    /**
     * Test {@link LdapGroupManagerBean#findAvailableGroupsFor(String)} method
     * using a user who has a less-than and greater-than sign (<>) in their cn
     * attribute value.
     *
     * The test verifies that an expected group list is returned for each user.
     *
     * @throws Throwable
     */
    @Test(groups = "integration.session")
    public void testFindGroupsForUserGTLTChar() throws Throwable {
        assertEquals(new HashSet<String>(Arrays.asList("RHQ Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("bwallace"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("ltoller"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Monitor Group")),
            ldapGroupManager.findAvailableGroupsFor("callen"));
    }

    /**
     * Test {@link LdapGroupManagerBean#findAvailableGroupsFor(String)} method
     * using a user who has a semicolon (;) in their cn attribute value.
     *
     * The test verifies that an expected group list is returned for each user.
     *
     * @throws Throwable
     */
    @Test(groups = "integration.session")
    public void testFindGroupsForUserSemiColonChar() throws Throwable {
        assertEquals(new HashSet<String>(Arrays.asList("RHQ Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("zbalanger"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("hsimpsonite"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Monitor Group")),
            ldapGroupManager.findAvailableGroupsFor("wfredrick"));
    }

    /**
     * Test {@link LdapGroupManagerBean#findAvailableGroupsFor(String)} method
     * using a user who has a quote (") in their cn attribute value.
     *
     * The test verifies that an expected group list is returned for each user.
     *
     * @throws Throwable
     */
    @Test(groups = "integration.session")
    public void testFindGroupsForUserQuoteChar() throws Throwable {
        assertEquals(new HashSet<String>(Arrays.asList("RHQ Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("acallen"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("jmathers"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Monitor Group")),
            ldapGroupManager.findAvailableGroupsFor("smein"));
    }

    /**
     * Test {@link LdapGroupManagerBean#findAvailableGroupsFor(String)} method
     * using a user who has a equal sign (=) in their cn attribute value.
     *
     * The test verifies that an expected group list is returned for each user.
     *
     * @throws Throwable
     */
    @Test(groups = "integration.session")
    public void testFindGroupsForUserEqualChar() throws Throwable {
        assertEquals(new HashSet<String>(Arrays.asList("RHQ Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("ssmitherson"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("hrein"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Monitor Group")),
            ldapGroupManager.findAvailableGroupsFor("nsadler"));
    }

    /**
     * Test {@link LdapGroupManagerBean#findAvailableGroupsFor(String)} method
     * using a user who has a leading and trailing space (cn= My User ) in their
     * cn attribute value.
     *
     * The test verifies that an expected group list is returned for each user.
     *
     * @throws Throwable
     */
    @Test(groups = "integration.session")
    public void testFindGroupsForUserLTSpaceChar() throws Throwable {
        assertEquals(new HashSet<String>(Arrays.asList("RHQ Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("bkiddough"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("sferguson"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Monitor Group")),
            ldapGroupManager.findAvailableGroupsFor("ssmiley"));
    }

    /**
     * Test {@link LdapGroupManagerBean#findAvailableGroupsFor(String)} method
     * using a user who has slash or forward slash (/) in their cn attribute
     * value.
     *
     * The test verifies that an expected group list is returned for each user.
     *
     * @throws Throwable
     */
    @Test(groups = "integration.session")
    public void testFindGroupsForUserSlashChar() throws Throwable {
        assertEquals(new HashSet<String>(Arrays.asList("RHQ Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("sysapi"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("pscarlson"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Monitor Group")),
            ldapGroupManager.findAvailableGroupsFor("sysapi2"));
    }

    /**
     * Test {@link LdapGroupManagerBean#findAvailableGroupsFor(String)} method
     * using a user who has a hyphen or dash (-) in their cn attribute value.
     *
     * The test verifies that an expected group list is returned for each user.
     *
     * @throws Throwable
     */
    @Test(groups = "integration.session")
    public void testFindGroupsForUserHyphenChar() throws Throwable {
        assertEquals(new HashSet<String>(Arrays.asList("RHQ Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("lecroutche"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("samathers"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Monitor Group")),
            ldapGroupManager.findAvailableGroupsFor("sajeopardy"));
    }

    /**
     * Test {@link LdapGroupManagerBean#findAvailableGroupsFor(String)} method
     * using a user who has an asterisk or star (*) in their cn attribute value.
     *
     * The test verifies that an expected group list is returned for each user.
     *
     * @throws Throwable
     */
    @Test(groups = "integration.session")
    public void testFindGroupsForUserAsteriskChar() throws Throwable {
        assertEquals(new HashSet<String>(Arrays.asList("RHQ Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("sjeopardy"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("lcroutche"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Monitor Group")),
            ldapGroupManager.findAvailableGroupsFor("smathers"));
    }

    /**
     * Test {@link LdapGroupManagerBean#findAvailableGroupsFor(String)} method
     * using a user who has a open and close parenthesis () in their cn
     * attribute value.
     *
     * The test verifies that an expected group list is returned for each user.
     *
     * @throws Throwable
     */
    @Test(groups = "integration.session")
    public void testFindGroupsForUserParenthesisChar() throws Throwable {
        assertEquals(new HashSet<String>(Arrays.asList("RHQ Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("bstrafford"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("kkrawford"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Monitor Group")),
            ldapGroupManager.findAvailableGroupsFor("jwilbright"));
    }

    /**
     * Test {@link LdapGroupManagerBean#findAvailableGroupsFor(String)} method
     * using a user who has an extended ASCII or non 7-bit ASCII character in
     * their cn attribute value.
     *
     * The test verifies that an expected group list is returned for each user.
     *
     * @throws Throwable
     */
    @Test(groups = "integration.session")
    public void testFindGroupsForUser8BitAsciiChar() throws Throwable {
        assertEquals(new HashSet<String>(Arrays.asList("RHQ Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("mmechura"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Admin Group")),
            ldapGroupManager.findAvailableGroupsFor("wsequerl"));
        assertEquals(new HashSet<String>(Arrays.asList("JBoss Monitor Group")),
            ldapGroupManager.findAvailableGroupsFor("pbrady"));
    }

    /*---------------
     * Helper methods
     ---------------*/
    private void setSystemSetting(final SystemSetting setting, final String value) throws Exception {
        systemManager.setSystemSetting(setting, value);
    }

    private void setLdapGroupFilter(String filter) throws Exception {
        setSystemSetting(SystemSetting.LDAP_GROUP_FILTER, filter);
    }

    private void setLdapGroupMemberAttribute(String attributeName) throws Exception {
        setSystemSetting(SystemSetting.LDAP_GROUP_MEMBER, attributeName);
    }

    private void setLdapBaseDN(String dn) throws Exception {
        setSystemSetting(SystemSetting.LDAP_BASE_DN, dn);
    }

    private void setLdapLoginAttribute(String attributeName) throws Exception {
        setSystemSetting(SystemSetting.LDAP_LOGIN_PROPERTY, attributeName);
    }

    private void setLdapBindDN(String dn) throws Exception {
        setSystemSetting(SystemSetting.LDAP_BIND_DN, dn);
    }

    private void setLdapBindPassword(String password) throws Exception {
        setSystemSetting(SystemSetting.LDAP_BIND_PW, password);
    }

    private void setLdapUserFilter(String filter) throws Exception {
        setSystemSetting(SystemSetting.LDAP_FILTER, filter);
    }

    private void setLdapUrl(String url) throws Exception {
        setSystemSetting(SystemSetting.LDAP_NAMING_PROVIDER_URL, url);
    }

    private void setLdapCtxFactory(final String name) throws Exception {
        setSystemSetting(SystemSetting.LDAP_NAMING_FACTORY, name);
    }

}
