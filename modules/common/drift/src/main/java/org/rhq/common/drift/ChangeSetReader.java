package org.rhq.common.drift;

import java.io.IOException;

public interface ChangeSetReader {

    DirectoryEntry readDirectoryEntry() throws IOException;

    void close() throws IOException;

}
