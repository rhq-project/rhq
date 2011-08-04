package org.rhq.enterprise.server.drift;

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.Snapshot;
import org.rhq.core.domain.util.PageList;

@Remote
public interface DriftServerRemote {

    PageList<DriftChangeSet> findDriftChangeSetsByCriteria(Subject subject, DriftChangeSetCriteria criteria);

    Snapshot createSnapshot(Subject subject, DriftChangeSetCriteria criteria);

}
