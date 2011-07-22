package org.rhq.enterprise.server.drift;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftFile;

import static org.rhq.core.domain.drift.DriftCategory.FILE_REMOVED;

public class Snapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private int version;

    private Map<String, Drift> entries = new TreeMap<String, Drift>();

    public int getVersion() {
        return version;
    }

    public Collection<Drift> getEntries() {
        return entries.values();
    }

    public <D extends Drift> Snapshot add(DriftChangeSet<D> changeSet) {
        for (Drift entry : changeSet.getDrifts()) {
            entries.remove(entry.getPath());
            if (entry.getCategory() != FILE_REMOVED) {
                entries.put(entry.getPath(), entry);
            }
        }
        version = changeSet.getVersion();
        return this;
    }

    public DiffReport diff(Snapshot right) {
        Snapshot left = this;
        DiffReport<Drift> diff = new DiffReport<Drift>();

        for (Map.Entry<String, Drift> entry : left.entries.entrySet()) {
            if (!right.entries.containsKey(entry.getKey())) {
                diff.elementNotInRight(entry.getValue());
            }
        }

        for (Map.Entry<String, Drift> entry : right.entries.entrySet()) {
            if (!left.entries.containsKey(entry.getKey())) {
                diff.elementNotInLeft(entry.getValue());
            }
        }

        for (Map.Entry<String, Drift> entry : left.entries.entrySet()) {
            Drift rightDrift = right.entries.get(entry.getKey());
            if (rightDrift != null) {
                DriftFile leftFile = entry.getValue().getNewDriftFile();
                DriftFile rightFile = rightDrift.getNewDriftFile();

                if (!leftFile.getHashId().equals(rightFile.getHashId())) {
                    diff.elementInConflict(entry.getValue());
                }
            }
        }

        return diff;
    }

}
