/**
 * 
 */
package org.rhq.test.shrinkwrap;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.asset.Asset;

/**
 * Defines the contract for archives capable of storing the RHQ agent's plugin descriptor.
 * 
 * @author Lukas Krejci
 */
public interface RhqAgentPluginDescriptorContainer<T extends Archive<T>> {

    /**
     * The RHQ plugin archive can carry along another set of plugins that it is
     * dependent upon (these can be determined for example by using {@link #withRequiredPluginsFrom(Collection)}
     * method).
     * <p>
     * These are internally stored under the path returned from this method.
     * When creating a JAR archive file out of the shrinkwrap representation of the archive,
     * it is important to excelude this path from the resulting JAR.
     * <p>
     * You can do this by casting the archive to a {@link FilteredView} for example.
     * 
     * @return the path where the archive internally stores the required libraries if
     * those were collected using the {@link #withRequiredPluginsFrom(Collection)} method.
     */
    ArchivePath getRequiredPluginsPath();
    
    /**
     * Sets the plugin descriptor using the plugin descriptor template resource, making
     * a copy of that resource and replacing templatized variables in that file with
     * replacement values that are passed into this method.
     * The {@link ClassLoader} is used to obtain the plugin descriptor template resource, but
     * it is assumed to be a file on the file system.
     *
     * @param resourceName the name of the plugin descriptor template resource as accessible by the class loader
     * @param replacementValues map with keys of template replacement variable names with their replacement values 
     * @return the archive itself
     * @throws IllegalArgumentException if resourceName or replacementValues is null
     */
    T setPluginDescriptorFromTemplate(String resourceName, Map<String, String> replacementValues)
        throws IllegalArgumentException;

    /**
     * Sets the plugin descriptor using the resource name. The {@link ClassLoader} is
     * used to obtain the resource.
     * 
     * @param resourceName the name of the resource as accessible by the class loader
     * @return the archive itself
     * @throws IllegalArgumentException if resourceName is null
     */
    T setPluginDescriptor(String resourceName) throws IllegalArgumentException;

    /**
     * Sets the plugin descriptor using the given file.
     * 
     * @param file the plugin descriptor file 
     * @return the archive itself
     * @throws IllegalArgumentException if file is null or not readable
     */
    T setPluginDescriptor(File file) throws IllegalArgumentException;

    /**
     * Sets the plugin descriptor using the provided URL.
     * @param url the URL to obtain the plugin descriptor from
     * @return the archive itself
     * @throws IllegalArgumentException if url is null
     */
    T setPluginDescriptor(URL url) throws IllegalArgumentException;

    /**
     * Sets the plugin descriptor using the provided asset.
     * @param asset the asset representing the plugin descriptor
     * @return the archive itself
     * @throws IllegalArgumentException if asset is null
     */
    T setPluginDescriptor(Asset asset) throws IllegalArgumentException;

    /**
     * Using this method, one can supply a single RHQ plugin together with
     * the plugins that the plugin is dependent upon. The list of archives
     * is just a list of archives resolved by some means (i.e. maven resolver or some 
     * other means).
     * <p>
     * This method is only effective after the plugin descriptor has been set using 
     * one of the <code>setPluginDescriptor</code> methods (or by placing the plugin
     * descriptor directly into "META-INF/rhq-plugin.xml" manually).
     * 
     * @param archives the archives to look for the required RHQ plugins in
     * @return the archive itself
     * @throws IllegalArgumentException if the collection is null
     */
    T withRequiredPluginsFrom(Collection<? extends Archive<?>> archives) throws IllegalArgumentException;

    /**
     * An overloaded version of {@link #withRequiredPluginsFrom(Collection)}
     */
    T withRequiredPluginsFrom(Archive<?>... archives) throws IllegalArgumentException;

    /**
     * Returns the list of archives containing the plugins that this plugin is
     * dependent on. The plugins are found during the {@link #withRequiredPluginsFrom(Collection)} call.
     * 
     * @return the list of required plugin archives in the correct deployment order
     *  or null if not set
     */
    List<Archive<?>> getRequiredPlugins();
}
