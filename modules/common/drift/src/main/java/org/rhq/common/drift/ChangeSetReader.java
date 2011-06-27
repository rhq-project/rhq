package org.rhq.common.drift;

import java.io.IOException;

public interface ChangeSetReader {

    Headers getHeaders() throws IOException;

    DirectoryEntry readDirectoryEntry() throws IOException;

    void close() throws IOException;

}
