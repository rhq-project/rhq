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

import java.util.List;
import java.util.Properties;
import java.util.Set;

import twitter4j.Paging;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Tweet;
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
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 * This class represents the resource component for one individual twitter
 * feed - meaning either a search or a users timeline.
 *
 * @author Heiko W. Rupp
 */
public class FeedComponent implements ResourceComponent<TwitterComponent>, MeasurementFacet, DeleteResourceFacet
{
   private final Log log = LogFactory.getLog(this.getClass());

   private static final long NOT_YET_SET = 1;

   public static final String TOPIC_EVENT = "FeedEvent"; // Same as in Plugin-Descriptor
   EventContext eventContext;
   private long lastId = NOT_YET_SET;
   private boolean isSearch = false;
   private String keyword;
   private TwitterEventPoller eventPoller;
   private TwitterFactory tFactory;

    /**
     * Return availability of this resource
     *  @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {
        // TODO supply real implementation
        return AvailabilityType.UP;
    }


    /**
     * Start the resource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext<TwitterComponent> context) throws InvalidPluginConfigurationException, Exception {

        Configuration conf = context.getPluginConfiguration();
        String kind = conf.getSimpleValue("kind","user");
        if (kind.equals("search"))
                isSearch = true;
        keyword = conf.getSimpleValue("keyword","Jopr"); // Jopr is fallback .. just in case

        String serverUrl = context.getParentResourceComponent().getServerUrl();
        String searchBase = context.getParentResourceComponent().getSearchUrl();

        eventContext = context.getEventContext();
        eventPoller = new TwitterEventPoller(TOPIC_EVENT);
        eventContext.registerEventPoller(eventPoller, 63);

        Properties props = new Properties();
        props.put(PropertyConfiguration.SOURCE,"Jopr");
        props.put(PropertyConfiguration.HTTP_USER_AGENT,"Jopr");
        props.put(PropertyConfiguration.SEARCH_BASE_URL, searchBase);
        props.put(PropertyConfiguration.REST_BASE_URL, serverUrl);
        twitter4j.conf.Configuration tconf = new PropertyConfiguration(props);

        tFactory = new TwitterFactory(tconf);


    }


    /**
     * Tear down the rescource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() {


        eventContext.unregisterEventPoller(TOPIC_EVENT);
    }



    /**
     * Gather measurement data
     *  @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport, java.util.Set)
     */
    public  void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

       for (MeasurementScheduleRequest req : metrics) {
          if (req.getName().equals("tweetCount")) {
             Twitter twitter = tFactory.getInstance();
             Paging paging = new Paging();

             MeasurementDataNumeric res;
             if (isSearch) {
                Query q = new Query(keyword);
                q.setSinceId(lastId);
                if (lastId == NOT_YET_SET)
                  q.setRpp(1);
                else
                  q.setRpp(20);
                QueryResult qr = twitter.search(q);
                List<Tweet> tweets = qr.getTweets();
                res = new MeasurementDataNumeric(req, (double) tweets.size());

                eventPoller.addTweets(tweets);
                if (tweets.size()>0)
                   lastId = tweets.get(0).getId();
             }
             else {
                List<Status> statuses;
                if (lastId == NOT_YET_SET) {
                   paging.setCount(1);
                }
                else {
                   paging.setCount(100);
                }
                paging.setSinceId(lastId);
                statuses = twitter.getUserTimeline(keyword,paging);
                res = new MeasurementDataNumeric(req, (double) statuses.size());

                eventPoller.addStatuses(statuses);
                if (statuses.size()>0)
                   lastId = statuses.get(0).getId();

             }
             report.addData(res);
          }
       }
    }

   /**
    * Used to uninventoy children -- this is not really
    * deleting physical resources and so nothing really to do.
    * @throws Exception
    */
   public void deleteResource() throws Exception {
      log.info("deleteResouces called");
   }
}