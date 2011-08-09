package org.rhq.enterprise.server.drift;

import java.io.File;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.Snapshot;
import org.rhq.core.domain.util.PageList;

@Local
public interface DriftServerLocal {

    void saveChangeSet(int resourceId, File changeSetZip) throws Exception;

    void saveChangeSetFiles(File changeSetFilesZip) throws Exception;

    void updateDriftConfiguration(Subject subject, EntityContext entityContext, DriftConfiguration driftConfig);

    DriftConfiguration getDriftConfiguration(Subject subject, EntityContext entityContext, int driftConfigId);

    void detectDrift(Subject subject, EntityContext context, DriftConfiguration driftConfig);

    PageList<DriftChangeSet> findDriftChangeSetsByCriteria(Subject subject, DriftChangeSetCriteria criteria);

    PageList<Drift> findDriftsByCriteria(Subject subject, DriftCriteria criteria);

    PageList<DriftComposite> findDriftCompositesByCriteria(Subject subject, DriftCriteria criteria);

    Snapshot createSnapshot(Subject subject, DriftChangeSetCriteria criteria);

}
