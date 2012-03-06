package org.rhq.test.shrinkwrap;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.container.ClassContainer;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.container.ManifestContainer;

public interface RhqAgentPluginArchive extends Archive<RhqAgentPluginArchive>,
		ManifestContainer<RhqAgentPluginArchive>,
		ClassContainer<RhqAgentPluginArchive>,
		LibraryContainer<RhqAgentPluginArchive>,
		RhqAgentPluginDescriptorContainer<RhqAgentPluginArchive> {
}
