package org.rhq.bundle.ant.type;

import org.apache.tools.ant.types.DataType;
import org.rhq.bundle.ant.BundleAntProject;

/**
 *
 */
public abstract class AbstractBundleType extends DataType {
    @Override
    public BundleAntProject getProject() {
        return (BundleAntProject)super.getProject();
    }
}
