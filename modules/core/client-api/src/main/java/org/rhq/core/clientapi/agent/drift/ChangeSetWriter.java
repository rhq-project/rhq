package org.rhq.core.clientapi.agent.drift;

import java.io.File;
import java.io.IOException;

public interface ChangeSetWriter {

//    void startDirectory(File dir) throws IOException;
//
//    void endDirectory() throws IOException;
//
//    void addFile(File file) throws IOException;

    void writeDirectoryEntry(DirectoryEntry dirEntry) throws IOException;

    void close() throws IOException;

}
