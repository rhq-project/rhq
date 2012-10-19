/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.twitter;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.PropertyConfiguration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;

/**
 * Implementation of the Twitter subsystem functionality.
 * Most work is done in the children. This component allows
 * to post tweets and to monitor the users timeline when username
 * and password are given.
 *
 * @author Heiko W. Rupp
 */
public class TwitterComponent implements ResourceComponent, OperationFacet, MeasurementFacet
{
   private final Log log = LogFactory.getLog(this.getClass());

   private static final String TWIT_EVENT = "TwitterEvent";
   private static final int NOT_YET_SET = -1;

   EventContext eventContext;
   private String username;
   private String password;
   private String serverUrl;
   private String searchBaseUrl;
   private TwitterFactory tFactory;
   private TwitterEventPoller eventPoller;
   private long lastId = NOT_YET_SET;
   private static final String HTTP_TWITTER_COM = "http://twitter.com/";

   /**
     * Return availability of this resource
     *  @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }


    /**
     * Start the resource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext context) throws Exception {

        Configuration conf = context.getPluginConfiguration();
        username = conf.getSimpleValue("user",null);
        password = conf.getSimpleValue("password",null);
        String url = conf.getSimpleValue("baseurl", HTTP_TWITTER_COM);
        if (!url.endsWith("/"))
           url= url+"/";
        try {
           new URL(url);
           serverUrl = url;
        }
        catch (MalformedURLException e) {
           throw new InvalidPluginConfigurationException(e.getMessage());
        }
        url = conf.getSimpleValue("searchBaseUrl", "http://search.twitter.com/");
        if (!url.endsWith("/"))
           url= url+"/";
        try {
           new URL(url);
           searchBaseUrl = url;
        }
        catch (MalformedURLException e) {
           throw new InvalidPluginConfigurationException(e.getMessage());
        }



        eventContext = context.getEventContext();
        eventPoller = new TwitterEventPoller(TWIT_EVENT);
        eventContext.registerEventPoller(eventPoller, 53);
       Properties props = new Properties();
       props.put(PropertyConfiguration.SOURCE,"Jopr");
       props.put(PropertyConfiguration.HTTP_USER_AGENT,"Jopr");
       props.put(PropertyConfiguration.SEARCH_BASE_URL,searchBaseUrl);
       props.put(PropertyConfiguration.REST_BASE_URL,serverUrl);
       twitter4j.conf.Configuration tconf = new PropertyConfiguration(props);

        tFactory = new TwitterFactory(tconf);


    }


    /**
     * Tear down the rescource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() {
      eventContext.unregisterEventPoller(TWIT_EVENT);

    }

    /**
     * Gather measurement data
     *  @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport, java.util.Set)
     */
    public  void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

       for (MeasurementScheduleRequest req : metrics) {
          if (req.getName().equals("tweetCount")) {

             Twitter twitter = tFactory.getInstance(username,password); // TODO server url?
//             Twitter twitter = new Twitter(username,password,serverUrl);
             Paging paging = new Paging();
             if (lastId == NOT_YET_SET) {
                paging.setSinceId(1);
                paging.setCount(1);
             }
             else {
                paging.setSinceId(lastId);
                paging.setCount(100);
             }
             List<Status> statuses;
             statuses = twitter.getFriendsTimeline(paging );
             if (lastId>0) {
                MeasurementDataNumeric res;
                res = new MeasurementDataNumeric(req, (double) statuses.size());

                eventPoller.addStatuses(statuses);
                report.addData(res);
             }
             if (statuses.size()>0)
                lastId = statuses.get(0).getId(); // This is always newest first
          }
          else if (req.getName().equals("followerCount")) {
              Twitter twitter = tFactory.getInstance(username,password); // TODO server url?
              int count = twitter.getFollowersIDs().getIDs().length;
              MeasurementDataNumeric res;
              res = new MeasurementDataNumeric(req,(double)count);
              report.addData(res);
          }
       }
    }


    /**
     * Implementation of the OperationFacet
     * Sends a tweet
     * @param  name       the name of the operation
     * @param  configuration parameters of the operation
     * @return result of the operation
     * @throws Exception If anything goes wrong
     */
    public OperationResult invokeOperation(String name,
                                           Configuration configuration) throws Exception {
        if (name!=null && name.equals("postStatus")) {
            if (username==null || password==null)
                throw new IllegalArgumentException("User or password were not set");

            String message = configuration.getSimpleValue("message",null);

            Twitter twitter = tFactory.getInstance(username,password);
            Status status = twitter.updateStatus(message);
            OperationResult result = new OperationResult("Posted " + status.getText());

            return result;

        }
        throw new UnsupportedOperationException("Operation " + name + " is not valid");
    }


    protected String getServerUrl() {
       return serverUrl;
    }
    protected String getSearchUrl() {
       return searchBaseUrl;
    }
}