package org.rhq.common.drift;

import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftChangeSetCategory;

public class Headers {

    private String driftConfigurationName;

    private String basedir;

    private DriftChangeSetCategory type;

    public Headers(String driftConfigurationName, String basedir, DriftChangeSetCategory type) {
        this.driftConfigurationName = driftConfigurationName;
        this.basedir = basedir;
        this.type = type;
    }

    public String getDriftConfigurationName() {
        return driftConfigurationName;
    }

    public String getBasedir() {
        return basedir;
    }

    public DriftChangeSetCategory getType() {
        return type;
    }

}
