package org.rhq.common.drift;

import java.io.IOException;

public interface ChangeSetReader extends Iterable<DirectoryEntry> {

    Headers getHeaders() throws IOException;

    DirectoryEntry readDirectoryEntry() throws IOException;

    void close() throws IOException;

}
