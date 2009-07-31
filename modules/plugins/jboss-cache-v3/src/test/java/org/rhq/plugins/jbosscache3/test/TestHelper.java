package org.rhq.plugins.jbosscache3.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.plugin.PluginManager;

public class TestHelper {

	public static Set<Resource> getResources(ResourceType resourceType) {
		InventoryManager inventoryManager = PluginContainer.getInstance()
				.getInventoryManager();
		Set<Resource> resources = inventoryManager
				.getResourcesWithType(resourceType);
		return resources;
	}

	public static ResourceType getResourceType(String resourceTypeName,
			String pluginName) {
		PluginManager pluginManager = PluginContainer.getInstance()
				.getPluginManager();
		PluginMetadataManager pluginMetadataManager = pluginManager
				.getMetadataManager();
		return pluginMetadataManager.getType(resourceTypeName, pluginName);
	}

	public static void copyFile(File fileA, File fileB) throws Exception {

		InputStream in = new FileInputStream(fileB);

		OutputStream out = new FileOutputStream(fileA);

		byte[] buf = new byte[1024];
		int length;
		while ((length = in.read(buf)) > 0) {
			out.write(buf, 0, length);
		}
		in.close();
		out.close();
	}

}
