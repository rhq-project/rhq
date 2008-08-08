package org.rhq.enterprise.server.cluster.instance;

import javax.ejb.Local;

@Local
public interface CacheConsistencyManagerLocal {
    void reloadServerCacheIfNeeded();
}
