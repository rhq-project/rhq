package org.rhq.common.drift;

import java.io.IOException;

public interface ChangeSetWriter {

    void writeDirectoryEntry(DirectoryEntry dirEntry) throws IOException;

    void close() throws IOException;

}
