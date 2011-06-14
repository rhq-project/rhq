package org.rhq.core.pc.drift;

import java.io.InputStream;

public interface ChangeSetManager {

    InputStream getMetadata(int resourceId);

}
