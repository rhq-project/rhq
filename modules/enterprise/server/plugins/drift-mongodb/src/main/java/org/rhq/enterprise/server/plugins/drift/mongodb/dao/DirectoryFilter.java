package org.rhq.enterprise.server.plugins.drift.mongodb.dao;

import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSetEntry;

class DirectoryFilter implements ChangeSetEntryFilter {

    private String dir;
    
    public DirectoryFilter(String dir) {
        this.dir = dir;
    }
    
    @Override
    public boolean matches(MongoDBChangeSetEntry entry) {
        return entry.getDirectory().equals(dir);
    }
}
