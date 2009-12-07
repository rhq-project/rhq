package org.rhq.enterprise.server.plugin.pc.content;

import org.rhq.core.domain.content.Advisory;

public class AdvisoryBugDetails {
    private Advisory advisory;
    private String bugs;

    public AdvisoryBugDetails(Advisory advisoryIn, String bugIn) {
        advisory = advisoryIn;
        bugs = bugIn;
    }

    public Advisory getAdvisory() {
        return advisory;
    }

    public void setAdvisory(Advisory advisory) {
        this.advisory = advisory;
    }

    public String getBugs() {
        return bugs;
    }

    public void setPkg(String bugsIn) {
        this.bugs = bugsIn;
    }

    public String toString() {
        return "Advisory = " + advisory + ",bugs =" + getBugs();
    }
}
