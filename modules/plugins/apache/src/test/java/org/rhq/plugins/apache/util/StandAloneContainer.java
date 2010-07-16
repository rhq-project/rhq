package org.rhq.plugins.apache.util;

import java.io.File;

import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;

public class StandAloneContainer {

  public PluginContainer getContainer(AugeasTree tree,String pluginPath){
    PluginContainer container=null;
    try {
	   container = PluginContainer.getInstance();
	   File pluginDir = new File(pluginPath);
       PluginContainerConfiguration config = new PluginContainerConfiguration();
	   config.setPluginFinder(new FileSystemPluginFinder(pluginDir));
       config.setPluginDirectory(pluginDir);

       container.setConfiguration(config);
	   container.initialize();
	   container.getInventoryManager().executeServerScanImmediately();
	   container.getInventoryManager().executeServiceScanImmediately();

	   return container;

    }catch(Exception e){
	   e.printStackTrace();
	   if (container!=null)
		  container.shutdown();
    }
    return null;
  }



	public static <T> T getComponent(int resourceId, Class<T> facetInterface, FacetLockType lockType,
	                                 long timeout, boolean daemonThread, boolean onlyIfStarted) throws PluginContainerException {
	    InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
	    ResourceContainer resourceContainer = inventoryManager.getResourceContainer(resourceId);
	    if (resourceContainer == null) {
	        throw new PluginContainerException("Resource component container could not be retrieved for resource: "
	            + resourceId);
	    }
	    return resourceContainer.createResourceComponentProxy(facetInterface, lockType, timeout, daemonThread, onlyIfStarted);
	}

	public static <T> Object getComponentProxy(Resource res,Class<T> facetInterface) throws Exception {
	ClassLoader currenContextClassLoader = Thread.currentThread()
		.getContextClassLoader();
	try {
	   Resource asResource = res;
	   ClassLoader cl = PluginContainer.getInstance().getPluginComponentFactory().
	                    getResourceClassloader(asResource);

	   Class<?> resourceSpecificFacetInterface = Class.forName(
			facetInterface.getName(), true, cl);

	   Thread.currentThread().setContextClassLoader(cl);

	   return ComponentUtil.getComponent(asResource.getId(),resourceSpecificFacetInterface,
	                    FacetLockType.WRITE, 30000, true, true);
	} finally {
	    Thread.currentThread().setContextClassLoader(
			currenContextClassLoader);
	  }
	}
}
