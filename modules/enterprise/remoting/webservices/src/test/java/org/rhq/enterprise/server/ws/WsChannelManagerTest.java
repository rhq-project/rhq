package org.rhq.enterprise.server.ws;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.enterprise.server.ws.utility.WsUtility;

/**
 * These tests can not be executed in our standard unit test fashion as they
 * require a running RHQ Server with our web services deployed.
 * 
 * This is still in development and has the current restrictions:
 * - add [dev_root]/modules/enterprise/remoting/webservices/target/rhq-remoting-webservices-{version}.jar 
 *    to TOP of IDE classpath for development/testing. 
 * - Server running on localhost. 
 * - ws-test user defined in database with full permissions 
 * - Non RHQ Server JBossAS in inventory. 
 * - The ws.test.package-path and ws.test.package-version environment 
 *   variables must be defined to a test .war file.
 * 
 * @author Jay Shaughnessy, Simeon Pinder
 */
@Test(groups = "ws")
public class WsChannelManagerTest extends AssertJUnit implements TestPropertiesInterface{

    //Test variables
    private static ObjectFactory WS_OBJECT_FACTORY;
    private static WebservicesRemote WEBSERVICE_REMOTE;
    private static Subject subject = null;
    private static PageControl pc_unlimited = null;

    @BeforeClass
    public void init() throws ClassNotFoundException, MalformedURLException, SecurityException, NoSuchMethodException,
        IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException,
        LoginException_Exception {

        //build reference variable bits
        URL gUrl = WsUtility.generateRemoteWebserviceURL(WebservicesManagerBeanService.class, host, port, useSSL);
        QName gQName = WsUtility.generateRemoteWebserviceQName(WebservicesManagerBeanService.class);
        WebservicesManagerBeanService jws = new WebservicesManagerBeanService(gUrl, gQName);

        WEBSERVICE_REMOTE = jws.getWebservicesManagerBeanPort();
        WS_OBJECT_FACTORY = new ObjectFactory();
        WsSubjectTest.checkForWsTestUserAndRole();
        
        subject = WEBSERVICE_REMOTE.login(credentials, credentials);
        //create unlimited pageControl instance
        pc_unlimited = WS_OBJECT_FACTORY.createPageControl();
        pc_unlimited.setPageNumber(0);
        pc_unlimited.setPageSize(-1);
    }

    @Test(enabled = TESTS_ENABLED)
	void testCRUD() throws ChannelException_Exception {
		   if ( !TESTS_ENABLED ) {
		      return;
		   }
		   
		   // delete any existing test channels in the db
		   List<Channel> channels = WEBSERVICE_REMOTE.findChannels(subject, pc_unlimited);
		   for (int i = 0; (i < channels.size()); ++i) {
		      Channel channel = channels.get(i);
		      if (channel.getName().startsWith("test-channel-")) {
		         WEBSERVICE_REMOTE.deleteChannel(subject, channel.getId());
		      }
		   }

		   // ensure test channel does not exist
		   ChannelCriteria criteria = new ChannelCriteria();
		   criteria.caseSensitive = true;
		   criteria.setFilterName("test-channel-0");

		   channels = WEBSERVICE_REMOTE.findChannelsByCriteria(subject, criteria);
		   assertTrue("test channel should not exist.",channels.size() == 0 );

		   // create a test channel
//		   newChannel = new Channel("test-channel-0");
		   Channel newChannel = WS_OBJECT_FACTORY.createChannel();
		     newChannel.setName("test-channel-0");
		   newChannel.setDescription("description-0");
		   Channel testChannel = WEBSERVICE_REMOTE.createChannel(subject, newChannel);
		   assertNotNull("test channel should exist.",testChannel);
		   assertEquals("test-channel-0", testChannel.getName());

		   channels = WEBSERVICE_REMOTE.findChannelsByCriteria(subject, criteria);
		   assertTrue("test channel should exist.",channels.size() == 1 );

		   // test getter
		   testChannel = WEBSERVICE_REMOTE.getChannel(subject, 8888888);
		   assertNull("bogus channel should not exist.",testChannel );
		   testChannel = WEBSERVICE_REMOTE.getChannel(subject, channels.get(0).getId());
		   assertNotNull("test channel should exist.",testChannel );
		   assertEquals("test-channel-0", testChannel.getName());
		   assertEquals("description-0", testChannel.getDescription());

		   // test update
		   testChannel.setDescription("description-1");
		   testChannel = WEBSERVICE_REMOTE.updateChannel(subject, testChannel);
		   assertEquals("description-1", testChannel.getDescription());
		   testChannel = WEBSERVICE_REMOTE.getChannel(subject, testChannel.getId());
		   assertNotNull("test channel should exist.",testChannel);
		   assertEquals("test-channel-0", testChannel.getName());
		   assertEquals("description-1", testChannel.getDescription());

		   // test delete
		   WEBSERVICE_REMOTE.deleteChannel(subject, testChannel.getId());
		   testChannel = WEBSERVICE_REMOTE.getChannel(subject, testChannel.getId());
		   assertNull("channel should not exist.",testChannel );
		   channels = WEBSERVICE_REMOTE.findChannelsByCriteria(subject, criteria);
		   assertTrue("test channel should not exist.", channels.size() == 0);
		}
    
        @Test(enabled = TESTS_ENABLED)
		void testFindByCriteria() throws ChannelException_Exception {
		   if ( !TESTS_ENABLED ) {
		      return;
		   }

		   // delete any existing test channels in the db
		   List<Channel> channels = WEBSERVICE_REMOTE.findChannels(subject, pc_unlimited);
		   int numRealChannels = channels.size();
		   Channel channel;
		for (int i = 0; (i < channels.size()); ++i) {
		      channel = channels.get(i);
		      if (channel.getName().startsWith("test-channel-")) {
		         WEBSERVICE_REMOTE.deleteChannel(subject, channel.getId());
		         --numRealChannels;
		      }
		   }

		   Channel newChannel = WS_OBJECT_FACTORY.createChannel();
		     newChannel.setName("test-channel-xxx");
		   newChannel.setDescription("description-0");
		   Channel testChannel = WEBSERVICE_REMOTE.createChannel(subject, newChannel);

		   newChannel = WS_OBJECT_FACTORY.createChannel();
		     newChannel.setName("test-channel-yyy");
		   newChannel.setDescription("description-1");
		   testChannel = WEBSERVICE_REMOTE.createChannel(subject, newChannel);

		   newChannel = WS_OBJECT_FACTORY.createChannel();
		     newChannel.setName("test-channel-xyz");
		   newChannel.setDescription("description-2");
		   testChannel = WEBSERVICE_REMOTE.createChannel(subject, newChannel);

		   ChannelCriteria criteria = new ChannelCriteria();
		   channels = WEBSERVICE_REMOTE.findChannelsByCriteria(subject, criteria);
		   assertEquals("empty criteria failed.",channels.size(), numRealChannels + 3 );

		   criteria.caseSensitive = true;
		   criteria.strict = true;

		   criteria.setFilterName("test-channel-xyz");
		   channels = WEBSERVICE_REMOTE.findChannelsByCriteria(subject, criteria);
		   assertEquals("CS/Strict name criteria failed.",channels.size(), 1);

		   criteria.setFilterName("TEST-channel-xyz");
		   channels = WEBSERVICE_REMOTE.findChannelsByCriteria(subject, criteria);
		   assertEquals("CS/Strict name criteria failed.",channels.size(), 0);

		   criteria.caseSensitive = false;
		   criteria.strict = true;

		   criteria.setFilterName("TEST-channel-xyz");
		   channels = WEBSERVICE_REMOTE.findChannelsByCriteria(subject, criteria);
		   assertEquals("CS/Strict name criteria failed.",channels.size(), 1 );

		   criteria.caseSensitive = true;
		   criteria.strict = false;

		   criteria.setFilterName("XXX");
		   channels = WEBSERVICE_REMOTE.findChannelsByCriteria(subject, criteria);
		   assertEquals("CS/Strict name criteria failed.",channels.size(), 0);

		   criteria.caseSensitive = false;
		   criteria.strict = false;

		   criteria.setFilterName("XXX");
		   channels = WEBSERVICE_REMOTE.findChannelsByCriteria(subject, criteria);
		   assertEquals("CS/Strict name criteria failed.",channels.size(), 1);

		   criteria.setFilterName("test-channel-");
		   channels = WEBSERVICE_REMOTE.findChannelsByCriteria(subject, criteria);
		   assertEquals("CS/Strict name criteria failed.",channels.size(), 3);

		   criteria.setFilterName("-x");
		   channels = WEBSERVICE_REMOTE.findChannelsByCriteria(subject, criteria);
		   assertEquals("CS/Strict name criteria failed.",channels.size(), 2);

		   criteria.setFilterDescription("-2");
		   channels = WEBSERVICE_REMOTE.findChannelsByCriteria(subject, criteria);
		   assertEquals("CS/Strict name/descrip criteria failed.",channels.size(), 1);

		   // delete any existing test channels in the db
		   channels = WEBSERVICE_REMOTE.findChannels(subject, pc_unlimited);
		   for (int i = 0; (i < channels.size()); ++i) {
		      channel = channels.get(i);
		      if (channel.getName().startsWith("test-channel-")) {
		         WEBSERVICE_REMOTE.deleteChannel(subject, channel.getId());
		      }
		   }
		}

        @Test(enabled = TESTS_ENABLED)
		void testDeploy() {

		   // check prequisites

		   // Available Tomcat Server
		   ResourceCriteria criteria = new ResourceCriteria();
		   criteria.strict = true;
		   criteria.setFilterName( "Tomcat (" );
		   criteria.setFilterCurrentAvailability( AvailabilityType.UP );
		   List<Resource> tomcatServers = WEBSERVICE_REMOTE.findResourcesByCriteria( subject, criteria );
		   
		   assertNotNull("Test requires and available Tomcat Server in inventory.", tomcatServers);
		   assertTrue("Test requires and available Tomcat Server in inventory.", (tomcatServers.size() > 0));
		   Resource tomcatServer = tomcatServers.get(0);
		      
		   // delete test-channel-war if in inventory
		   criteria = new ResourceCriteria();
		   criteria.strict = true;
		   criteria.setFilterName( "test-channel-war" );
		   criteria.setFilterResourceTypeName( "Tomcat Web Application (WAR)");
		   List<Resource> wars = WEBSERVICE_REMOTE.findResourcesByCriteria( subject, criteria );
		   Resource war = null;

//		   if (( null != wars ) && !wars.isEmpty() ) {
//			      System.out.println( "\n Deleting existing test-channel-war in order to test create...");
//			      assertEquals("Found more than 1 test-channel-war", wars.size(), 1);
//			      war = wars.get(0);
//			      WEBSERVICE_REMOTE.deleteResource(war.getId());
//			      
//			      // up to 60 seconds to get the job done.
//			      for( i=0; ( i<60 ); ++i ) {
//			         sleep( 1000 );
//
//			         wars = ResourceManager.findResourcesByCriteria( criteria );
//			         if (( null == wars ) || wars.isEmpty() ) {
//			            break;
//			         }
//			      }
//			      
//			      wars = ResourceManager.findResourcesByCriteria( criteria );
//			      Assert.assertTrue(( ( null == wars ) || wars.isEmpty() ), "test-channel-war should not exist" );
//			      print( "\n Done deleting existing test-channel-war in order to test create...")
//			      
//			      // Give Tomcat a few additional seconds to perform its cleanup of the app, just in case the resource is
//			      // gone but TC is still mopping up, before we try and deploy the same exact app
//			      sleep( 10000 );
//			   }
//
//			   // Create the war resource
//			   
//			   // Get the parent
//			   criteria = new ResourceCriteria();
//			   criteria.strict = true;
//			   criteria.addFilterName( "Tomcat VHost (localhost)" );
//			   criteria.addFilterCurrentAvailability( AvailabilityType.UP );
//			   var vhosts = ResourceManager.findResourcesByCriteria( criteria );
//			   Assert.assertNotNull( vhosts, "Test requires Tomcat VHost (localhost) in inventory.");
//			   Assert.assertNumberEqualsJS( vhosts.size(), 1, "Test requires Tomcat VHost (localhost) in inventory.");
//			   var vhost = vhosts.get(0);
//
//			   // get the resource type
//			   var warType = ResourceTypeManager.getResourceTypeByNameAndPlugin( "Tomcat Web Application (WAR)", "Tomcat" );
//			   Assert.assertNotNull( warType, "Test requires Tomcat WAR resource type.");
//			   
//			   // read in the file
//			   var file = new java.io.File("./src/test/resources/test-channel-war.war");   
//			   var inputStream = new java.io.FileInputStream( file );
//			   var fileLength = file.length();
//			   var fileBytes = java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, fileLength);
//			   for (numRead=0, offset=0; ((numRead >= 0) && (offset < fileBytes.length)); offset += numRead ) {
//			     numRead = inputStream.read(fileBytes, offset, fileBytes.length - offset); 
//			   }
//			   Assert.assertNumberEqualsJS( offset, fileLength, "Read bytes not equal to file length");
//			      
//			   inputStream.close();
//			   Assert.assertTrue( (offset == fileBytes.length), "Could not completely read file " + file.getName() );   
//			   
//			   // get package type for both create and update
//			   
//			   var packageTypes = ContentManager.findPackageTypes( "Tomcat Web Application (WAR)", "Tomcat" );
//			   Assert.assertNotNull( packageTypes, "missing package type" );
//			   Assert.assertNumberEqualsJS( packageTypes.size(), 1, "unexpected package type" );   
//			   var packageType = packageTypes.get(0);
//			   
//			   // get package config def
//			   var deployConfigDef = ConfigurationManager.getPackageTypeConfigurationDefinition( packageType.getId() );
//			   Assert.assertNotNull( deployConfigDef, "deployConfigDef should exist." );
//			   var explodeOnDeploy = deployConfigDef.getPropertyDefinitionSimple( "explodeOnDeploy" )
//			   Assert.assertNotNull( explodeOnDeploy, "explodeOnDeploy prop should exist." );
//			   var deployConfig = new Configuration();
//			   var property = new PropertySimple(explodeOnDeploy.getName(), "true");
//			   deployConfig.put( property );
//			   
//			   // null arch -> noarch
//			   // no required plugin config for this resource type
//			   // no required resource config for this resource type
//			   // resource name defaults to war file name / context root
//			   ResourceFactoryManager.createResource( vhost.getId(), warType.getId(), null, null, file.getName(), "1.0", null, deployConfig, fileBytes);
//
//			   criteria = new ResourceCriteria();
//			   criteria.strict = false;
//			   criteria.addFilterName( "test-channel-war" );   
//			   criteria.addFilterResourceTypeName( "Tomcat Web Application (WAR)");
//			   criteria.addFilterCurrentAvailability( AvailabilityType.UP );
//			   
//			   // up to 60 seconds to get the job done.
//			   for( i=0; ( i<60 ); ++i ) {
//			      sleep( 1000 );
//
//			      wars = ResourceManager.findResourcesByCriteria( criteria );
//			      if (( null != wars ) && !wars.isEmpty() ) {
//			         break;
//			      }
//			   }
//
//			   wars = ResourceManager.findResourcesByCriteria( criteria );
//			   war = null;
//			   if (( null != wars ) && !wars.isEmpty() ) {
//			      Assert.assertNumberEqualsJS( wars.size(), 1, "Found more than 1 test-channel-war" );
//			      war = wars.get(0);
//			   }
//			   Assert.assertNotNull( war, "War should have been created" );
//			   
//			   var backingPackage = ContentManager.getBackingPackageForResource( war.getId() );
//			   assertNotNull( backingPackage, "backing package should exist after create" );
//			   print( "\n After Create: Backing Package=" + backingPackage.getId() );
//			   
//			   // delete existing test channel in the db, this will unsubscribe resources and remove orphaned pvs
//			   var criteria = new ChannelCriteria();
//			   criteria.caseSensitive = true;
//			   criteria.strict = true;
//			   criteria.addFilterName('test-channel-0');
//
//			   var channels = channels = ChannelManager.findChannelsByCriteria(criteria);
//			   if ( !channels.isEmpty() ) {
//			      channel = channels.get(0);
//			      ChannelManager.deleteChannel(channel.getId());
//			   }
//
//			   // create a test channel
//			   var newChannel = new Channel("test-channel-0");
//			   newChannel.setDescription("description-0");
//			   channel = ChannelManager.createChannel(newChannel);
//
//			   Assert.assertNotNull( channel, "channel should have existed or been created")
//			   Assert.assertTrue( (channel.getId() > 0), "channel should have existed or been created")   
//
//			   // test channel subscription
//			   var subscribedResources;
//			   subscribedResources = ChannelManager.findSubscribedResources( channel.getId(), PageControl.getUnlimitedInstance() );
//			   Assert.assertTrue(( ( null == subscribedResources ) || subscribedResources.isEmpty()), "test channel should not have resources" );
//
//			   ChannelManager.subscribeResourceToChannels( war.getId(), [channel.getId()] );
//			   
//			   subscribedResources = ChannelManager.findSubscribedResources( channel.getId(), PageControl.getUnlimitedInstance() );
//			   Assert.assertNumberEqualsJS( subscribedResources.size(), 1, "channel should have the test war" );
//			   
//			   ChannelManager.unsubscribeResourceFromChannels( war.getId(), [channel.getId()] );
//
//			   subscribedResources = ChannelManager.findSubscribedResources( channel.getId(), PageControl.getUnlimitedInstance() );
//			   Assert.assertTrue(( ( null == subscribedResources ) || subscribedResources.isEmpty()), "test channel should not have resources" );
//			   
//			   // Create packageVersion in an attempt to upgrade the web-app
//
//			   var pvsInChannel = ChannelManager.findPackageVersionsInChannel( channel.getId(), null, PageControl.getUnlimitedInstance() );
//			   Assert.assertTrue(( ( null == pvsInChannel ) || pvsInChannel.isEmpty()), "test channel should not have pvs" );
//
//			   var architectures = ContentManager.findArchitectures();
//			   Assert.assertNotNull( architectures, "missing architectures" );
//			   Assert.assertTrue( !architectures.isEmpty(), "missing architectures" );
//			   
//			   // read in the package file
//			   file = new java.io.File("./src/test/resources/test-channel-war-2.0.war");
//			   inputStream = new java.io.FileInputStream( file );
//			   fileLength = file.length();
//			   fileBytes = java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, fileLength);
//			   for (numRead=0, offset=0; ((numRead >= 0) && (offset < fileBytes.length)); offset += numRead ) {
//			     numRead = inputStream.read(fileBytes, offset, fileBytes.length - offset); 
//			   }
//			   inputStream.close();
//			   Assert.assertTrue( (offset == fileBytes.length), "Could not completely read file " + file.getName() );
//			   
//			   var pv = ContentManager.createPackageVersion( "test-channel-war-2.0.war", packageType.getId(), "2.0", null, fileBytes);
//			   Assert.assertNotNull( pv, "failed to create packageVersion" );
//			   Assert.assertTrue( (pv.getId() > 0), " Bad PV Id from createPV");
//
//			   ChannelManager.addPackageVersionsToChannel( channel.getId(), [pv.getId()] );
//			   
//			   var pvsInChannel = ChannelManager.findPackageVersionsInChannel( channel.getId(), null, PageControl.getUnlimitedInstance() );
//			   Assert.assertNotNull( pvsInChannel, "pv should be in channel" );
//			   Assert.assertNumberEqualsJS( pvsInChannel.size(), 1, "unexpected pvs" );   
//			   Assert.assertNumberEqualsJS( pvsInChannel.get(0).getId(), pv.getId(), "unexpected pv returned" );
//
//			   // do the update
//			   ContentManager.deployPackages( [war.getId()], [pv.getId()] );
//			      
//			   // Make sure things still look good
//			   
//			   criteria = new ResourceCriteria();
//			   criteria.strict = false;
//			   criteria.addFilterName( "test-channel-war" );   
//			   criteria.addFilterResourceTypeName( "Tomcat Web Application (WAR)");
//			   criteria.addFilterCurrentAvailability( AvailabilityType.UP );
//			   
//			   // up to 60 seconds to get the job done.
//			   for( i=0; ( i<60 ); ++i ) {
//			      sleep( 1000 );
//
//			      wars = ResourceManager.findResourcesByCriteria( criteria );
//			      if (( null != wars ) && !wars.isEmpty() ) {
//			         break;
//			      }
//			   }
//			      
//			   wars = ResourceManager.findResourcesByCriteria( criteria );
//			   war = null;
//			   if (( null != wars ) && !wars.isEmpty() ) {
//			      Assert.assertNumberEqualsJS( wars.size(), 1, "Found more than 1 test-channel-war" );
//			      war = wars.get(0);
//			   }
//			   Assert.assertNotNull( war, "War should have been updated" );
//			   
//			   var newBackingPackage = ContentManager.getBackingPackageForResource( war.getId() );
//			   Assert.assertNotNull( newBackingPackage, "backing package should exist after update." );
//			   print( "\n After Update: BackingPackage=" + newBackingPackage.getId() );
//			   
//			   // TODO: This test may fail due to RHQ-2387, uncomment when fixed 
//			   // Assert.assertTrue( ( backingPackage,getId() != newBackingPackage.getId() ), "Backing ackage should differ after update" );
//			   
//			   // delete any existing test channels in the db
//			   channels = ChannelManager.findChannels(PageControl.getUnlimitedInstance());
//			   for (i = 0; (i < channels.size()); ++i) {
//			      var channel = channels.get(i);
//			      if (channel.getName().startsWith("test-channel-")) {
//			         ChannelManager.deleteChannel(channel.getId());
//			      }
//			   }   
  
        }
		}