package org.rhq.enterprise.server.drift;

import java.io.File;

import javax.ejb.Local;

@Local
public interface DriftServerLocal {

    void saveChangeSet(int resourceId, File changeSetZip) throws Exception;

    void saveChangeSetFiles(File changeSetFilesZip) throws Exception;

}
