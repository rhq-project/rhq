package org.rhq.enterprise.server.drift;

import java.io.File;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.drift.DriftConfiguration;

@Local
public interface DriftServerLocal {

    void saveChangeSet(int resourceId, File changeSetZip) throws Exception;

    void saveChangeSetFiles(File changeSetFilesZip) throws Exception;

    void updateDriftConfiguration(Subject subject, EntityContext entityContext, DriftConfiguration driftConfig);

    DriftConfiguration getDriftConfiguration(Subject subject, EntityContext entityContext, int driftConfigId);

    void detectDrift(Subject subject, EntityContext context, DriftConfiguration driftConfig);

}
