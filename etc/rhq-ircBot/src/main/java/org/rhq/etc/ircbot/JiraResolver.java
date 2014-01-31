/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.etc.ircbot;

import java.net.URI;
import java.net.URISyntaxException;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.JiraRestClientFactory;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;

/**
 * @author Jirka Kremser
 *
 */
public class JiraResolver implements BugResolver {
    
    public final static String JIRA_URL = "https://issues.jboss.org";
    private JiraRestClient restClient;

    @Override
    public String resolve(String bugIdentifier) {
        Promise<Issue> issuePromise = getRestClient().getIssueClient().getIssue(bugIdentifier);
        return issuePromise.claim().getSummary();
    }
    
    public Promise<Issue> resolveAsync(String bugIdentifier) {
        return getRestClient().getIssueClient().getIssue(bugIdentifier);
    }
    
    
    private JiraRestClient setupJiraClient(String url) throws URISyntaxException {
        JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
        final JiraRestClient restClient = factory.createWithBasicHttpAuthentication(new URI(url), "rhq-bot", "123456");
        return restClient;
    }
    
    private JiraRestClient getRestClient() {
        if (restClient == null) {
            try {
                restClient = setupJiraClient(JIRA_URL);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return restClient;
    }

}
