package org.rhq.common.drift;

public class Headers {

    private String driftConfigurationName;

    private String basedir;

    private boolean coverageChangeSet;

    public Headers(String driftConfigurationName, String basedir, boolean coverageChangeSet) {
        this.driftConfigurationName = driftConfigurationName;
        this.basedir = basedir;
        this.coverageChangeSet = coverageChangeSet;
    }

    public String getDriftConfigurationName() {
        return driftConfigurationName;
    }

    public String getBasedir() {
        return basedir;
    }

    public boolean isCoverageChangeSet() {
        return coverageChangeSet;
    }
}
