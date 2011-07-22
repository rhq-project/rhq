package org.rhq.enterprise.server.drift;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;

public class Snapshot {

    private int version;

    private Set<Drift> entries = new TreeSet<Drift>(new Comparator<Drift>() {
        @Override
        public int compare(Drift d1, Drift d2) {
            return d1.getPath().compareTo(d2.getPath());
        }
    });

    public int getVersion() {
        return version;
    }

    public Set<Drift> getEntries() {
        return entries;
    }

    public <D extends Drift> Snapshot add(DriftChangeSet<D> changeSet) {
        for (Drift entry : changeSet.getDrifts()) {
            entries.remove(entry);
            entries.add(entry);
        }
        version = changeSet.getVersion();
        return this;
    }

}
