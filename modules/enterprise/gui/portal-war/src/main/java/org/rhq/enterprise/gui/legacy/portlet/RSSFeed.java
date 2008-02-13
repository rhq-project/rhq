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
package org.rhq.enterprise.gui.legacy.portlet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This bean encapsulates the information that go into an RSS feed
 */
public class RSSFeed {
    private static final String PUBTIME_FORMAT = "EEE, d MMM yyyy HH:mm:ss z";
    private String title = "";
    private String baseUrl = "";
    private Date pubDate = null;
    private Date buildDate = null;
    private List items = new ArrayList(10);

    public RSSFeed(String baseUrl) {
        setBaseUrl(baseUrl);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        // Strip out trailing slash if necessary
        if (baseUrl.lastIndexOf('/') == (baseUrl.length() - 1)) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        this.baseUrl = baseUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPubDate() {
        if (pubDate == null) {
            pubDate = new Date();
        }

        SimpleDateFormat fmt = new SimpleDateFormat(PUBTIME_FORMAT);
        return fmt.format(pubDate);
    }

    public void setPubDate(long pubDate) {
        this.pubDate = new Date(pubDate);
    }

    public String getBuildDate() {
        if (buildDate == null) {
            buildDate = new Date();
        }

        SimpleDateFormat fmt = new SimpleDateFormat(PUBTIME_FORMAT);
        return fmt.format(buildDate);
    }

    public void setBuildDate(long buildDate) {
        this.buildDate = new Date(buildDate);
    }

    public List getItems() {
        return items;
    }

    public void setItems(List items) {
        this.items = items;
    }

    public void addItem(String title, String link, String desc, long pubDate) {
        items.add(new RSSItem(title, link, desc, pubDate));
    }

    public class RSSItem {
        private String title;
        private String link;
        private String description;
        private String guid;
        private Date pubDate;

        public RSSItem(String title, String link, String desc, long pubDate) {
            this.title = title;
            this.link = link;
            this.description = desc;
            this.guid = link + '#' + System.currentTimeMillis();
            this.setPubDate(pubDate);
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getGuid() {
            return guid;
        }

        public void setGuid(String guid) {
            this.guid = guid;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getPubDate() {
            if (pubDate == null) {
                pubDate = new Date();
            }

            SimpleDateFormat fmt = new SimpleDateFormat(PUBTIME_FORMAT);
            return fmt.format(pubDate);
        }

        public void setPubDate(long pubDate) {
            this.pubDate = new Date(pubDate);
        }
    }
}