/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

/**
 * These tests assume that there is a jopr server and an agent running on
 * localhost.
 * 
 * The test also requires an available Tomcat Server in inventory. 
 * 
 * TODO The Tomcat restriction should be removed to be able to use an AS server via the 
 * perf plugin script test scenario.
 */

var TestsEnabled = true;

// note, super-user, will not test any security constraints
var subject = rhq.login('rhqadmin', 'rhqadmin');

executeAllTests();

rhq.logout();

function testCRUD() {
   if ( !TestsEnabled ) {
      return;
   }
   
   // delete any existing test channels in the db
   var channels = ChannelManager.findChannels(PageControl.getUnlimitedInstance());
   for (i = 0; (i < channels.size()); ++i) {
      var channel = channels.get(i);
      if (channel.getName().startsWith("test-channel-")) {
         ChannelManager.deleteChannel(channel.getId());
      }
   }

   // ensure test channel does not exist
   var criteria = new ChannelCriteria();
   criteria.caseSensitive = true;
   criteria.addFilterName('test-channel-0');

   channels = ChannelManager.findChannelsByCriteria(criteria);
   Assert.assertTrue(channels.size() == 0, "test channel should not exist.");

   // create a test channel
   var newChannel = new Channel("test-channel-0");
   newChannel.setDescription("description-0");
   var testChannel = ChannelManager.createChannel(newChannel);
   Assert.assertNotNull(testChannel, "test channel should exist");
   Assert.assertEquals("test-channel-0", testChannel.getName());

   channels = ChannelManager.findChannelsByCriteria(criteria);
   Assert.assertTrue(channels.size() == 1, "test channel should exist.");

   // test getter
   testChannel = ChannelManager.getChannel(8888888);
   Assert.assertNull(testChannel, "bogus channel should not exist.");
   testChannel = ChannelManager.getChannel(channels.get(0).getId());
   Assert.assertNotNull(testChannel, "test channel should exist.");
   Assert.assertEquals("test-channel-0", testChannel.getName());
   Assert.assertEquals("description-0", testChannel.getDescription());

   // test update
   testChannel.setDescription("description-1");
   testChannel = ChannelManager.updateChannel(testChannel);
   Assert.assertEquals("description-1", testChannel.getDescription());
   testChannel = ChannelManager.getChannel(testChannel.getId());
   Assert.assertNotNull(testChannel, "test channel should exist.")
   Assert.assertEquals("test-channel-0", testChannel.getName());
   Assert.assertEquals("description-1", testChannel.getDescription());

   // test delete
   ChannelManager.deleteChannel(testChannel.getId());
   testChannel = ChannelManager.getChannel(testChannel.getId());
   Assert.assertNull(testChannel, "channel should not exist.");
   channels = ChannelManager.findChannelsByCriteria(criteria);
   Assert.assertTrue(channels.size() == 0, "test channel should not exist.");
}

function testFindByCriteria() {
   if ( !TestsEnabled ) {
      return;
   }

   // delete any existing test channels in the db
   var channels = ChannelManager.findChannels(PageControl.getUnlimitedInstance());
   var numRealChannels = channels.size();
   for (i = 0; (i < channels.size()); ++i) {
      var channel = channels.get(i);
      if (channel.getName().startsWith("test-channel-")) {
         ChannelManager.deleteChannel(channel.getId());
         --numRealChannels;
      }
   }

   var newChannel = new Channel("test-channel-xxx");
   newChannel.setDescription("description-0");
   var testChannel = ChannelManager.createChannel(newChannel);

   var newChannel = new Channel("test-channel-yyy");
   newChannel.setDescription("description-1");
   var testChannel = ChannelManager.createChannel(newChannel);

   var newChannel = new Channel("test-channel-xyz");
   newChannel.setDescription("description-2");
   var testChannel = ChannelManager.createChannel(newChannel);

   var criteria = new ChannelCriteria();
   criteria.fetchResourceChannels( true );   
   criteria.fetchChannelContentSources( true );   
   criteria.fetchChannelPackageVersions( true );
   channels = ChannelManager.findChannelsByCriteria(criteria);
   Assert.assertNumberEqualsJS(channels.size(), numRealChannels + 3, "empty criteria failed.");

   criteria.caseSensitive = true;
   criteria.strict = true;

   criteria.addFilterName('test-channel-xyz');
   channels = ChannelManager.findChannelsByCriteria(criteria);
   Assert.assertNumberEqualsJS(channels.size(), 1, "CS/Strict name criteria failed.");

   criteria.addFilterName('TEST-channel-xyz');
   channels = ChannelManager.findChannelsByCriteria(criteria);
   Assert.assertNumberEqualsJS(channels.size(), 0, "CS/Strict name criteria failed.");

   criteria.caseSensitive = false;
   criteria.strict = true;

   criteria.addFilterName('TEST-channel-xyz');
   channels = ChannelManager.findChannelsByCriteria(criteria);
   Assert.assertNumberEqualsJS(channels.size(), 1, "CS/Strict name criteria failed.");

   criteria.caseSensitive = true;
   criteria.strict = false;

   criteria.addFilterName('XXX');
   channels = ChannelManager.findChannelsByCriteria(criteria);
   Assert.assertNumberEqualsJS(channels.size(), 0, "CS/Strict name criteria failed.");

   criteria.caseSensitive = false;
   criteria.strict = false;

   criteria.addFilterName('XXX');
   channels = ChannelManager.findChannelsByCriteria(criteria);
   Assert.assertNumberEqualsJS(channels.size(), 1, "CS/Strict name criteria failed.");

   criteria.addFilterName('test-channel-');
   channels = ChannelManager.findChannelsByCriteria(criteria);
   Assert.assertNumberEqualsJS(channels.size(), 3, "CS/Strict name criteria failed.");

   criteria.addFilterName('-x');
   channels = ChannelManager.findChannelsByCriteria(criteria);
   Assert.assertNumberEqualsJS(channels.size(), 2, "CS/Strict name criteria failed.");

   criteria.addFilterDescription('-2');
   channels = ChannelManager.findChannelsByCriteria(criteria);
   Assert.assertNumberEqualsJS(channels.size(), 1, "CS/Strict name/descrip criteria failed.");

   // delete any existing test channels in the db
   var channels = ChannelManager.findChannels(PageControl.getUnlimitedInstance());
   for (i = 0; (i < channels.size()); ++i) {
      var channel = channels.get(i);
      if (channel.getName().startsWith("test-channel-")) {
         ChannelManager.deleteChannel(channel.getId());
      }
   }
}

function testDeploy() {
   if ( !TestsEnabled ) {
      return;
   }

   // check prequisites

   // Available Tomcat Server
   var criteria = new ResourceCriteria();
   criteria.strict = false;
   criteria.addFilterName( "Tomcat (" );
   criteria.addFilterCurrentAvailability( AvailabilityType.UP );
   var tomcatServers = ResourceManager.findResourcesByCriteria( criteria );
   
   Assert.assertNotNull( tomcatServers, "Test requires and available Tomcat Server in inventory.");
   Assert.assertTrue( (tomcatServers.size() > 0),  "Test requires and available Tomcat Server in inventory.");
   var tomcatServer = tomcatServers.get(0);
      
   // delete test-channel-war if in inventory
   criteria = new ResourceCriteria();
   criteria.strict = false;
   criteria.addFilterName( "test-channel-war" );
   criteria.addFilterResourceTypeName( "Tomcat Web Application (WAR)");
   var wars = ResourceManager.findResourcesByCriteria( criteria );
   var war = null;
   
   if (( null != wars ) && !wars.isEmpty() ) {
      print( "\n Deleting existing test-channel-war in order to test create...")
      Assert.assertNumberEqualsJS( wars.size(), 1, "Found more than 1 test-channel-war");
      war = wars.get(0);
      ResourceFactoryManager.deleteResource(war.getId());
      
      // up to 60 seconds to get the job done.
      for( i=0; ( i<60 ); ++i ) {
         sleep( 1000 );

         wars = ResourceManager.findResourcesByCriteria( criteria );
         if (( null == wars ) || wars.isEmpty() ) {
            break;
         }
      }
      
      wars = ResourceManager.findResourcesByCriteria( criteria );
      Assert.assertTrue(( ( null == wars ) || wars.isEmpty() ), "test-channel-war should not exist" );
      print( "\n Done deleting existing test-channel-war in order to test create...")
      
      // Give Tomcat a few additional seconds to perform its cleanup of the app, just in case the resource is
      // gone but TC is still mopping up, before we try and deploy the same exact app
      sleep( 10000 );
   }

   // Create the war resource
   
   // Get the parent
   criteria = new ResourceCriteria();
   criteria.strict = true;
   criteria.addFilterName( "Tomcat VHost (localhost)" );
   criteria.addFilterCurrentAvailability( AvailabilityType.UP );
   var vhosts = ResourceManager.findResourcesByCriteria( criteria );
   Assert.assertNotNull( vhosts, "Test requires Tomcat VHost (localhost) in inventory.");
   Assert.assertNumberEqualsJS( vhosts.size(), 1, "Test requires Tomcat VHost (localhost) in inventory.");
   var vhost = vhosts.get(0);

   // get the resource type
   var warType = ResourceTypeManager.getResourceTypeByNameAndPlugin( "Tomcat Web Application (WAR)", "Tomcat" );
   Assert.assertNotNull( warType, "Test requires Tomcat WAR resource type.");
   
   // read in the file
   var file = new java.io.File("./src/test/resources/test-channel-war.war");   
   var inputStream = new java.io.FileInputStream( file );
   var fileLength = file.length();
   var fileBytes = java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, fileLength);
   for (numRead=0, offset=0; ((numRead >= 0) && (offset < fileBytes.length)); offset += numRead ) {
     numRead = inputStream.read(fileBytes, offset, fileBytes.length - offset); 
   }
   Assert.assertNumberEqualsJS( offset, fileLength, "Read bytes not equal to file length");
      
   inputStream.close();
   Assert.assertTrue( (offset == fileBytes.length), "Could not completely read file " + file.getName() );   
   
   // get package type for both create and update
   
   var packageTypes = ContentManager.findPackageTypes( "Tomcat Web Application (WAR)", "Tomcat" );
   Assert.assertNotNull( packageTypes, "missing package type" );
   Assert.assertNumberEqualsJS( packageTypes.size(), 1, "unexpected package type" );   
   var packageType = packageTypes.get(0);
   
   // get package config def
   var deployConfigDef = ConfigurationManager.getPackageTypeConfigurationDefinition( packageType.getId() );
   Assert.assertNotNull( deployConfigDef, "deployConfigDef should exist." );
   var explodeOnDeploy = deployConfigDef.getPropertyDefinitionSimple( "explodeOnDeploy" )
   Assert.assertNotNull( explodeOnDeploy, "explodeOnDeploy prop should exist." );
   var deployConfig = new Configuration();
   var property = new PropertySimple(explodeOnDeploy.getName(), "true");
   deployConfig.put( property );
   
   // null arch -> noarch
   // no required plugin config for this resource type
   // no required resource config for this resource type
   // resource name defaults to war file name / context root
   ResourceFactoryManager.createPackageBackedResource( vhost.getId(), warType.getId(), null, null, file.getName(), "1.0", null, deployConfig, fileBytes);

   criteria = new ResourceCriteria();
   criteria.strict = false;
   criteria.addFilterName( "test-channel-war" );   
   criteria.addFilterResourceTypeName( "Tomcat Web Application (WAR)");
   criteria.addFilterCurrentAvailability( AvailabilityType.UP );
   
   // up to 60 seconds to get the job done.
   for( i=0; ( i<60 ); ++i ) {
      sleep( 1000 );

      wars = ResourceManager.findResourcesByCriteria( criteria );
      if (( null != wars ) && !wars.isEmpty() ) {
         break;
      }
   }

   wars = ResourceManager.findResourcesByCriteria( criteria );
   war = null;
   if (( null != wars ) && !wars.isEmpty() ) {
      Assert.assertNumberEqualsJS( wars.size(), 1, "Found more than 1 test-channel-war" );
      war = wars.get(0);
   }
   Assert.assertNotNull( war, "War should have been created" );
      
   // The backing package (InstalledPackage) for a new PBR may not exist immediately. It
   // requires an agent-side content discovery. Wait for up to 120 seconds (to take care of initial delay
   // and schedule interval timing.
   var backingPackage;
   for( i=0; ( i<120 ); ++i ) {
      sleep( 1000 );
      
      backingPackage = ContentManager.getBackingPackageForResource( war.getId() );
      if ( null != backingPackage ) {
         break;
      }
   }

   backingPackage = ContentManager.getBackingPackageForResource( war.getId() );
   Assert.assertNotNull( backingPackage, "backing package should exist after create" );
   print( "\n After Create: Backing Package=" + backingPackage.getId() );
   
   // compare bytes to make sure we're good
   // Due to issues in PBR creation the bytes will be pulled from the plugin. There is a 30 second
   // timeout on this (see ContentManagerBean) Also, this means we may not get back
   // exactly what we put in as the plugin may have exploded the .war and then re-zipped. So, exact match
   // is not appropriate.
   var packageBytes = ContentManager.getPackageBytes( war.getId(), backingPackage.getId() );
   Assert.assertNotNull( packageBytes, "package bytes should have been returned");
   Assert.assertTrue( packageBytes.length > (fileBytes.length * 0.2), "not enough package bytes");
   Assert.assertTrue( packageBytes.length < (fileBytes.length * 1.2), "too many package bytes");   
   
   // throw in some criteria tests
   criteria = new InstalledPackageCriteria();
   criteria.addFilterId( backingPackage.getId() );
   criteria.addFilterInstallationTimeMinimum( 0 );
   criteria.addFilterInstallationTimeMaximum( new Date().getTime() );
   criteria.addFilterResourceId( war.getId() );
   // omit this criteria, it's not being set today
   // criteria.addFilterSubjectId( subject.getId() );
   criteria.fetchPackageVersion( true );
   criteria.fetchResource( true );
   criteria.fetchSubject( true );
   criteria.addSortInstallationTime( PageOrdering.DESC );
   
   var installedPackages = ContentManager.findInstalledPackagesByCriteria(criteria);
   Assert.assertNotNull( installedPackages, "installedPackage should exist" );
   Assert.assertNumberEqualsJS( installedPackages.size(), 1, "unexpected number of iPkgs" );
   Assert.assertNumberEqualsJS( installedPackages.get(0).getId(), backingPackage.getId(), "unexpected iPkg returned" );
   
   // negative test
   criteria.addFilterPackageVersionId( -81 );
   installedPackages = ContentManager.findInstalledPackagesByCriteria(criteria);
   Assert.assertTrue( (( null == pvsInChannel ) || pvsInChannel.isEmpty() ), "pv should not be returned" );   
   
   // delete existing test channel in the db, this will unsubscribe resources and remove orphaned pvs
   var criteria = new ChannelCriteria();
   criteria.caseSensitive = true;
   criteria.strict = true;
   criteria.addFilterName('test-channel-0');

   var channels = channels = ChannelManager.findChannelsByCriteria(criteria);
   if ( !channels.isEmpty() ) {
      channel = channels.get(0);
      ChannelManager.deleteChannel(channel.getId());
   }

   // create a test channel
   var newChannel = new Channel("test-channel-0");
   newChannel.setDescription("description-0");
   channel = ChannelManager.createChannel(newChannel);

   Assert.assertNotNull( channel, "channel should have existed or been created")
   Assert.assertTrue( (channel.getId() > 0), "channel should have existed or been created")   

   // test channel subscription
   var subscribedResources;
   subscribedResources = ChannelManager.findSubscribedResources( channel.getId(), PageControl.getUnlimitedInstance() );
   Assert.assertTrue(( ( null == subscribedResources ) || subscribedResources.isEmpty()), "test channel should not have resources" );

   ChannelManager.subscribeResourceToChannels( war.getId(), [channel.getId()] );
   
   subscribedResources = ChannelManager.findSubscribedResources( channel.getId(), PageControl.getUnlimitedInstance() );
   Assert.assertNumberEqualsJS( subscribedResources.size(), 1, "channel should have the test war" );
   
   ChannelManager.unsubscribeResourceFromChannels( war.getId(), [channel.getId()] );

   subscribedResources = ChannelManager.findSubscribedResources( channel.getId(), PageControl.getUnlimitedInstance() );
   Assert.assertTrue(( ( null == subscribedResources ) || subscribedResources.isEmpty()), "test channel should not have resources" );
   
   // Create packageVersion in an attempt to upgrade the web-app

   var pvsInChannel = ChannelManager.findPackageVersionsInChannel( channel.getId(), null, PageControl.getUnlimitedInstance() );
   Assert.assertTrue(( ( null == pvsInChannel ) || pvsInChannel.isEmpty()), "test channel should not have pvs" );

   var architectures = ContentManager.findArchitectures();
   Assert.assertNotNull( architectures, "missing architectures" );
   Assert.assertTrue( !architectures.isEmpty(), "missing architectures" );
   
   // read in the package file
   file = new java.io.File("./src/test/resources/test-channel-war-2.0.war");
   inputStream = new java.io.FileInputStream( file );
   fileLength = file.length();
   fileBytes = java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, fileLength);
   for (numRead=0, offset=0; ((numRead >= 0) && (offset < fileBytes.length)); offset += numRead ) {
     numRead = inputStream.read(fileBytes, offset, fileBytes.length - offset); 
   }
   inputStream.close();
   Assert.assertTrue( (offset == fileBytes.length), "Could not completely read file " + file.getName() );
   
   var pv = ContentManager.createPackageVersion( "test-channel-war", packageType.getId(), "2.0", null, fileBytes);
   Assert.assertNotNull( pv, "failed to create packageVersion" );
   Assert.assertTrue( (pv.getId() > 0), " Bad PV Id from createPV");

   ChannelManager.addPackageVersionsToChannel( channel.getId(), [pv.getId()] );
   
   // throw in some criteria search tests while we have a pv in play...
   criteria = new PackageVersionCriteria();
   criteria.addFilterPackageTypeId( packageType.getId() );
   criteria.fetchGeneralPackage( true );
   criteria.fetchArchitecture( true );
   criteria.fetchExtraProperties( true );
   criteria.fetchChannelPackageVersions( true );
   criteria.fetchInstalledPackages( true );
   criteria.fetchInstalledPackageHistory( true );
   criteria.fetchProductVersionPackageVersions( true );
   criteria.addSortDisplayName(PageOrdering.ASC);
   var pvsInChannel = ContentManager.findPackageVersionsByCriteria( criteria );
   Assert.assertNotNull( pvsInChannel, "pvs should exist" );
   Assert.assertTrue( (pvsInChannel.size() >= 2), "unexpected number of pvs" );
   
   // Now let's try with other restriction that should reduce to only the 2.0 version
   // 1.0 package is not associated with the channel.
   criteria.addFilterId( pv.getId());
   criteria.addFilterChannelId( channel.getId() );
   criteria.addFilterDisplayName( "test-channel-war" );
   criteria.addFilterVersion( "2.0" );         
   pvsInChannel = ChannelManager.findPackageVersionsInChannelByCriteria( criteria );
   Assert.assertNotNull( pvsInChannel, "pv should exist" );
   Assert.assertNumberEqualsJS( pvsInChannel.size(), 1, "unexpected number of pvs" );
   Assert.assertNumberEqualsJS( pvsInChannel.get(0).getId(), pv.getId(), "unexpected pv returned" );
   Assert.assertEquals( pvsInChannel.get(0).getVersion(), pv.getVersion(), "unexpected pv returned" );
      
   // negative test
   criteria.addFilterVersion( "1.0" );
   pvsInChannel = ChannelManager.findPackageVersionsInChannelByCriteria( criteria );
   Assert.assertTrue( (( null == pvsInChannel ) || pvsInChannel.isEmpty() ), "pv should not be returned" );

   // do a non-criteria find and continue from there...
   pvsInChannel = ChannelManager.findPackageVersionsInChannel( channel.getId(), null, PageControl.getUnlimitedInstance() );
   Assert.assertNotNull( pvsInChannel, "pv should be in channel" );
   Assert.assertNumberEqualsJS( pvsInChannel.size(), 1, "unexpected pvs" );   
   Assert.assertNumberEqualsJS( pvsInChannel.get(0).getId(), pv.getId(), "unexpected pv returned" );

   // do the update
   ContentManager.deployPackages( [war.getId()], [pv.getId()] );
      
   // Make sure things still look good
   
   criteria = new ResourceCriteria();
   criteria.strict = false;
   criteria.addFilterName( "test-channel-war" );   
   criteria.addFilterResourceTypeName( "Tomcat Web Application (WAR)");
   criteria.addFilterCurrentAvailability( AvailabilityType.UP );
   
   // up to 60 seconds to get the job done.
   for( i=0; ( i<60 ); ++i ) {
      sleep( 1000 );

      wars = ResourceManager.findResourcesByCriteria( criteria );
      if (( null != wars ) && !wars.isEmpty() ) {
         break;
      }
   }
      
   wars = ResourceManager.findResourcesByCriteria( criteria );
   war = null;
   if (( null != wars ) && !wars.isEmpty() ) {
      Assert.assertNumberEqualsJS( wars.size(), 1, "Found more than 1 test-channel-war" );
      war = wars.get(0);
   }
   Assert.assertNotNull( war, "War should have been updated" );
   
   var newBackingPackage = ContentManager.getBackingPackageForResource( war.getId() );
   Assert.assertNotNull( newBackingPackage, "backing package should exist after update." );
   print( "\n After Update: BackingPackage=" + newBackingPackage.getId() );
   
   // TODO: This test may fail due to RHQ-2387, uncomment when fixed 
   // Assert.assertTrue( ( backingPackage,getId() != newBackingPackage.getId() ), "Backing ackage should differ after update" );
   
   // delete any existing test channels in the db
   channels = ChannelManager.findChannels(PageControl.getUnlimitedInstance());
   for (i = 0; (i < channels.size()); ++i) {
      var channel = channels.get(i);
      if (channel.getName().startsWith("test-channel-")) {
         ChannelManager.deleteChannel(channel.getId());
      }
   }   
}
