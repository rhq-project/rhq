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
 */

// skippedTests.push('');

// note, super-user, will not test any security constraints
rhq.login('rhqadmin', 'rhqadmin');

executeAllTests();

rhq.logout();

function testCRUD() {

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
   Assert.assertNotNull(testChannel);
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
   channels = ChannelManager.findChannelsByCriteria(criteria);
   Assert.assertNumberEqualsJS(channels.size(), numRealChannels+3, "empty criteria failed.");
   
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

