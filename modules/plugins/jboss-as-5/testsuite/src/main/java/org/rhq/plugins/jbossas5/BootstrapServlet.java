package org.rhq.plugins.jbossas5;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.plugin.SimplePluginFinder;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import java.net.URL;
import java.util.Set;

/**
 * @author Jason Dobies
 */
public class
        BootstrapServlet
        extends HttpServlet
{
    private static final Log LOG = LogFactory.getLog(BootstrapServlet.class);

    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);

        try
        {
            bootstrap();
        }
        catch (Exception e)
        {
            LOG.error("Error bootstrapping AS 5 Plugin Test Suite", e);
        }
    }

    private void bootstrap()
            throws Exception
    {

        ServletContext context = getServletContext();

        SimplePluginFinder finder = new SimplePluginFinder();

        Set<String> pluginStringSet = context.getResourcePaths("/plugins/");
        for (String pluginString : pluginStringSet)
        {
            if (pluginString.endsWith(".jar"))
            {
                URL pluginUrl = context.getResource(pluginString);
                finder.addUrl(pluginUrl);
            }
        }

        PluginContainerConfiguration config = new PluginContainerConfiguration();
        config.setPluginFinder(finder);

        PluginContainer container = PluginContainer.getInstance();
        container.setConfiguration(config);

        //container.initialize();

        //Collection<PluginEnvironment> plugins = container.getPluginManager().getPlugins();
        //for (PluginEnvironment pluginEnvironment : plugins)
        //{
        //   LOG.info("Loaded Plugin: " + pluginEnvironment.getPluginName());
        //}

        //LOG.info("===============================================");
    }
}
