/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.legacy.rss;

/**
 * [class description]
 *
 * @author     <a href="mailto:jessica.sant@jboss.com">Jessica Sant</a>
 * @deprecated
 */
@Deprecated
public class RssFeedJobTest //extends TestCase
{
    /*
     *  //TODO: get this to test locally with username/password public static final String GOOD_PATCH_FEED_URL =
     * "https://network.jboss.com/jbossnetwork/restricted/feed/software.html?" +
     * "product=hibernate&downloadType=all&flavor=rss&version="; public static final String GOOD_RSS_URL = ""; public
     * static final String BAD_URL = "https://network.jboss.com/jbossnetwork/badURL"; public static final String
     * LOCAL_URL_NO_AUTH = "http://127.0.0.1:7080/test-feed/jbossNetworkFeed.xml"; public static final String
     * GOOD_USERNAME = "jessica.sant@jboss.com"; public static final String GOOD_PASSWORD = "four11"; public static
     * final String BAD_USERNAME_PASSWORD = "bad";
     *
     * private RssFeedJob feedJob;
     *
     * public void setUp() {     this.feedJob = new RssFeedJob(); }
     *
     */

    /** test extractTitle with no JIRA ID */
    /*
     *  public void testExtractTitle_nullJira() {     String itemTitle = "DISTRIBUTION: Hibernate 3.1 - binary (zip)";
     *   String jiraId = null;     String softwareType = "DISTRIBUTION";     String rawTitle = null;
     *
     *   String expected = "Hibernate 3.1 - binary (zip)";     String actual = feedJob.extractTitle( itemTitle,
     * softwareType, jiraId, rawTitle );
     *
     *   TestCase.assertEquals( actual, expected ); }
     *
     */
    /** tests extractTitle with a blank Jira ID as opposed to a null jira ID */
    /*
     *  public void testExtractTitle_blankJira() {      String itemTitle = "DISTRIBUTION: Hibernate 3.1 - binary (zip)";
     *     String jiraId = "";     String softwareType = "DISTRIBUTION";     String rawTitle = null;
     *
     *   String expected = "Hibernate 3.1 - binary (zip)";     String actual = feedJob.extractTitle( itemTitle,
     * softwareType, jiraId, rawTitle );
     *
     *   TestCase.assertEquals( actual, expected ); }
     *
     */
    /** tests extractTitle with the rawTitle provided */
    /*
     *  public void testExtractTitle_hasRawTitle() {     String itemTitle = "DISTRIBUTION: Hibernate 3.1 - binary
     * (zip)";     String jiraId = null;     String softwareType = "DISTRIBUTION";     String rawTitle = "Hibernate 3.1
     * - binary (zip)";
     *
     *   String expected = "Hibernate 3.1 - binary (zip)";     String actual = feedJob.extractTitle( itemTitle,
     * softwareType, jiraId, rawTitle );
     *
     *   TestCase.assertEquals( actual, expected ); }
     *
     */
    /** tests extractTitel with the JIRA ID provided */
    /*
     *  public void testExtractTtile_withJira() {     String itemTitle = "ENHANCEMENT: JBAS-2375 - Backport the
     * BarrierController service";     String jiraId = "JBAS-2375";     String softwareType = "ENHANCEMENT";     String
     * rawTitle = null;
     *
     *   String expected = "Backport the BarrierController service";     String actual = feedJob.extractTitle(
     * itemTitle, softwareType, jiraId, rawTitle );
     *
     *   TestCase.assertEquals( actual, expected ); }
     *
     */
    /** tests the feed with a good URL,  Username and Password */
    /*
     *  public void testFeed_goodUrlUsernamePassword() {     JobDetail jobDetail = loadJobDetail( GOOD_PATCH_FEED_URL,
     * GOOD_USERNAME, GOOD_PASSWORD );     try     {         this.feedJob.testFeed( jobDetail );     }     catch (
     * RssFeedException e )     {         // should not fail         e.printStackTrace();         TestCase.fail(
     * e.getMessage() );     } }
     *
     */
    /** tests the feed with a good URL but a bud username/password combo */
    /*
     *  public void testFeed_goodUrl_badUsernamePassword() {     JobDetail jobDetail = loadJobDetail(
     * GOOD_PATCH_FEED_URL, BAD_USERNAME_PASSWORD, BAD_USERNAME_PASSWORD );     try     {         this.feedJob.testFeed(
     * jobDetail );         TestCase.fail( "an exception should have been thrown, a bad username/password was sent" );
     *   }     catch ( RssFeedException e )     {         String expected = "Status was [401 - Unauthorized]";
     * String actual = e.getMessage();         TestCase.assertTrue( "should have started with [" + expected + "], but
     * was [" + actual + "]",                 actual.startsWith( expected ) );     } }
     */
    /** tests feed with a bad URL but a good username/password */
    /*
     *  public void testFeed_badUrl_goodUsernamePassword() {     JobDetail jobDetail = loadJobDetail( BAD_URL,
     * GOOD_USERNAME, GOOD_PASSWORD );     try     {         this.feedJob.testFeed( jobDetail );         TestCase.fail(
     * "an exception should have been thrown, a bad username/password was sent" );     }     catch ( RssFeedException e
     * )     {         String expected = "Status was [404 - ";         String actual = e.getMessage();
     * TestCase.assertTrue( "should have started with [" + expected + "], but was [" + actual + "]",
     * actual.startsWith( expected ) );     } }
     */
    /** tests feed with a good url and a blank username/password -- it should fail because it expects a username/password */
    /*
     *  public void testFeed_goodUrl_blankUsernamePassword_fails() {     JobDetail jobDetail = loadJobDetail(
     * GOOD_PATCH_FEED_URL, "", "" );     try     {         this.feedJob.testFeed( jobDetail );         TestCase.fail(
     * "an exception should have been thrown, a bad username/password was sent" );     }     catch ( RssFeedException e
     * )     {         String expected = "Status was [401 - Unauthorized]";         String actual = e.getMessage();
     *    TestCase.assertTrue( "should have started with [" + expected + "], but was [" + actual + "]",
     * actual.startsWith( expected ) );     } }
     *
     */
    /** tests feed with a good url and a blank username/password -- it should pass because it does not need a username/password */
    /*
     *  public void testFeed_goodUrl_blankUsernamePassword_passes() {     JobDetail jobDetail = loadJobDetail(
     * LOCAL_URL_NO_AUTH, "", "" );     try     {         this.feedJob.testFeed( jobDetail );     }     catch (
     * Exception e )     {         // should not fail         e.printStackTrace();         TestCase.fail( e.getMessage()
     * );     } }
     *
     *
     */
    /**
        * TODO Currently this test fails when comparing the createdOn field if not run in the EST timezone.
        *
        *  make sure we can store the feed successfully, without exception
        */
    /*
     * public void test2375FromLocallyLoadedFeed() {   JobDetail jobDetail = loadJobDetail( LOCAL_URL_NO_AUTH, "", "" );
     *     try     {        RssDocument parsedFeed = this.feedJob.testFeed( jobDetail );        this.feedJob.storeFeed(
     * parsedFeed, LOCAL_URL_NO_AUTH, "JBoss Network" );        SoftwareLocalHome softwareLocalHome =
     * SoftwareUtil.getLocalHome();        SoftwareValue actual = softwareLocalHome.findByReference(
     * "https://network.staging.jboss.com/jbossnetwork/restricted/softwareDetail.html?softwareId=a0450000005izHvAAI"
     * ).getSoftwareValue();        SoftwareValue expected = createJBAS_2375();
     *
     *      compare( expected, actual );     }     catch ( Exception e )     {         // should not fail
     * e.printStackTrace();         TestCase.fail( e.getMessage() );     } }
     *
     * private void compare( SoftwareValue expected, SoftwareValue actual ) {   TestCase.assertEquals(
     * expected.getAutomatedDownloadUrl(), actual.getAutomatedDownloadUrl() );   // test that the
     * getAutomatedInstalltionForThisVersion() works   TestCase.assertEquals(
     * expected.getAutomatedInstallation().trim(), actual.getAutomatedInstallationForThisVersion().trim() );
     * TestCase.assertEquals( expected.getCreatedBy(), actual.getCreatedBy() );   TestCase.assertEquals(
     * expected.getCreatedOn(), actual.getCreatedOn() );   TestCase.assertEquals( expected.getDistributionStatus(),
     * actual.getDistributionStatus() );   TestCase.assertEquals( expected.getDownloadUrl(), actual.getDownloadUrl() );
     *  TestCase.assertEquals( expected.getFilename(), actual.getFilename() );   TestCase.assertEquals(
     * expected.getFileSize(), actual.getFileSize());   TestCase.assertEquals( expected.getIssueReference(),
     * actual.getIssueReference() );   TestCase.assertEquals( expected.getLongDescription(), actual.getLongDescription()
     * );   TestCase.assertEquals( expected.getManualInstallation(), actual.getManualInstallation() );
     * TestCase.assertEquals( expected.getMD5(), actual.getMD5() );   TestCase.assertEquals( expected.getReferenceURL(),
     * actual.getReferenceURL() );   TestCase.assertEquals( expected.getSHA256(), actual.getSHA256() );
     * TestCase.assertEquals( expected.getShortDescription(), actual.getShortDescription() );   TestCase.assertEquals(
     * expected.getSoftwareType(), actual.getSoftwareType() );   TestCase.assertEquals( expected.getTitle(),
     * actual.getTitle() );
     *
     * compare( expected.getProductLightValues(), actual.getProductLightValues() ); }
     *
     * private void compare( ProductLightValue[] expected, ProductLightValue[] actual ) {   TestCase.assertEquals(
     * expected.length, actual.length );
     *
     * for( int i = 0; i < expected.length; i++ )   {      TestCase.assertEquals( "item " + i, expected[i].getName(),
     * actual[i].getName() );      TestCase.assertEquals( "item " + i, expected[i].getVersion(), actual[i].getVersion()
     * );      TestCase.assertEquals( "item " + i, expected[i].getJonResourceType(), actual[i].getJonResourceType() );
     *    TestCase.assertEquals( "item " + i, expected[i].getJonResourceVersion(), actual[i].getJonResourceVersion() );
     *  } }
     *
     * private SoftwareValue createJBAS_2375() {   JonSoftware expected = new JonSoftware();
     * expected.setAutomatedDownloadUrl(
     * "https://network.staging.jboss.com/jbossnetwork/secureDownload.html?softwareId=a0450000005izHvAAI" );
     * expected.setAutomatedInstallation(         "<someElement attribute=\"value\">\n" +         "<someInnerElement
     * attribute1=\"value1\"/>\n" +         "<someInnerElement2>\n" +         "text here\n"+
     * "</someInnerElement2>\n" +         "</someElement>" );   expected.setCreatedBy( "Dimitris Andreadis" );
     * expected.setCreatedOn(  new Long( 1153151520000L ) );   expected.setDistributionStatusType(
     * DistributionStatusType.AVAILABLE );   expected.setDownloadUrl(
     * "https://network.staging.jboss.com/jbossnetwork/restricted/softwareDownload.html?softwareId=a0450000005izHvAAI"
     * );   expected.setFileName( "JBAS-2375.jar" );   expected.setFileSize( new Long( 330322L ) );
     * expected.setIssueReference( "JBAS-2375" );   expected.setLastModified( new Long( 1154286720000L ) );
     * expected.setLongDescription( "Allows the user to create complex startup dependencies; backported the
     * BarrierController service to 4.0.0; made Tomcat5 emit the missing \"jboss.tomcat.connectors.started\"
     * notification, so it is known when the tomcat connectors are started.<br><br>\n" +         "\n" +         "MD5's
     * of included files:<br>\n" +         "&nbsp; 5318F937B4CB3725B43E52F7A05F80C4  jboss-system.jar<br>\n" +
     * "&nbsp; F8A78409C10E7F8C1E95B0E54EFEEE5E  tomcat50-service.jar<br>\n" +         "&nbsp;
     * 5D43EEACCA668DC59A017ED2E1CB499B  barrier-example-service.xml" );   expected.setManualInstallation( "Download and
     * unzip the attached JBAS-2375.jar<br>\n" +         "<br>\n" +         "Copy the included jar files to the
     * following locations:<br>\n" +         "&nbsp;        jboss-4.0.0/libjboss-system.jar <br>\n" +         "&nbsp;
     *     jboss-4.0.0/server/&lt;configuration&gt;/deploy/jbossweb-tomcat50.sar/tomcat50-service.jar<br>\n" +
     * "<br>\n" +         "The patch was tested with the included barrier-example-service.xml that starts an additional
     * JNDIView service, but it is not necessary in order to use this patch normally.<br><br>" );   expected.setMd5(
     * "5507D3880F33B5D53310D9C87F060F7D" );   expected.setReferenceURL(
     * "https://network.staging.jboss.com/jbossnetwork/restricted/softwareDetail.html?softwareId=a0450000005izHvAAI" );
     *  expected.setSha256( "ac70fb11dbf635e622f52d2237507e8eaa0e265e5c9176c0b51bfb6cda6ea4fb" );
     * expected.setShortDescription( "Backport the BarrierController service introduced in JBoss 4.0.2 to 4.0.0" );
     * expected.setSoftwareType( SoftwareType.BUG_FIX );   expected.setTitle( "test for download from staging -- 2375"
     * );   expected.addProduct( new JonProduct( null,
     * "https://network.staging.jboss.com/jbossnetwork/rss/product#SOFT-0101",         "Application Server", "4.0.1",
     * "jon App server", "jon 4.0.1" ));   return expected; }
     *
     * private JobDetail loadJobDetail( String url, String username, String password ) {    JobDetail jobDetail = new
     * JobDetail();    JobDataMap dataMap = new JobDataMap();    dataMap.put( RssFeedJob.ID, "0" );    dataMap.put(
     * RssFeedJob.URL, url );    dataMap.put( RssFeedJob.USERNAME, username );    dataMap.put( RssFeedJob.PASSWORD,
     * password );    dataMap.put( RssFeedJob.FEED_NAME, "name" );
     *
     *  jobDetail.setJobDataMap( dataMap );    return jobDetail; }
     */
}