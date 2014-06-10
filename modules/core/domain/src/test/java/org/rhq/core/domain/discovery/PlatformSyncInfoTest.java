package org.rhq.core.domain.discovery;

import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;

/**
 * @author Lukas Krejci
 * @since 4.12
 */
@Test
public class PlatformSyncInfoTest {

    public void testSyntheticResourcesIgnoredInBuilder() {
        ResourceType rt = new ResourceType("fake", "fake", ResourceCategory.SERVER, null);

        Resource platform = new Resource();
        platform.setId(1);
        platform.setResourceType(rt);

        Resource regularChild = new Resource();
        regularChild.setId(2);
        platform.addChildResource(regularChild);
        regularChild.setResourceType(rt);

        Resource syntheticChild = new Resource();
        syntheticChild.setId(2);
        syntheticChild.setSynthetic(true);
        platform.addChildResource(syntheticChild);
        syntheticChild.setResourceType(rt);

        PlatformSyncInfo syncInfo = PlatformSyncInfo.buildPlatformSyncInfo(platform);

        assert syncInfo.getTopLevelServerIds().size() == 1 && syncInfo.getTopLevelServerIds().iterator().next() == 2;
    }
}
