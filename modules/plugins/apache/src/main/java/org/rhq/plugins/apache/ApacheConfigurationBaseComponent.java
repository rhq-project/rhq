package org.rhq.plugins.apache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.parser.mapping.ApacheAugeasMapping;
import org.rhq.plugins.apache.util.AugeasNodeSearch;

public class ApacheConfigurationBaseComponent implements ApacheConfigurationBase,ConfigurationFacet, DeleteResourceFacet {

    protected ResourceContext<ApacheConfigurationBase> resourceContext;
    private final Log log = LogFactory.getLog(this.getClass());
    protected ApacheConfigurationBase parentComponent;
    
    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {
        resourceContext = context;
        parentComponent = resourceContext.getParentResourceComponent();
        }
    
    public void stop() {
    }

    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    public void deleteResource() throws Exception {
        ApacheDirectiveTree tree = loadParser();        
        ApacheDirective myNode = getNode(tree);
        
        if (myNode != null) {
            myNode.remove();
           
            saveParser(tree);
                       
           //TODO do we want to delete empty file?
           // parentVhost.deleteEmptyFile(tree, myNode);
           conditionalRestart();
        } else {
            log.info("Could find the configuration corresponding to the directory " + resourceContext.getResourceKey() + ". Ignoring.");
        }
    }

    public Configuration loadResourceConfiguration() throws Exception {
        ApacheConfigurationBase parentNode = resourceContext.getParentResourceComponent();
        ApacheDirectiveTree tree = parentNode.loadParser();
        ConfigurationDefinition resourceConfigDef = resourceContext.getResourceType().getResourceConfigurationDefinition();
        ApacheAugeasMapping mapping = new ApacheAugeasMapping(tree);
        return mapping.updateConfiguration(getNode(tree), resourceConfigDef);
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        ApacheConfigurationBase parentNode = (ApacheConfigurationBase) resourceContext.getParentResourceComponent();

        ApacheDirectiveTree tree = null;
        try {
            tree = parentNode.loadParser();
            ConfigurationDefinition resourceConfigDef = resourceContext.getResourceType()
                .getResourceConfigurationDefinition();
            ApacheAugeasMapping mapping = new ApacheAugeasMapping(tree);
            ApacheDirective directoryNode = getNode(tree);
            mapping.updateApache(directoryNode, report.getConfiguration(), resourceConfigDef);
            if (parentNode.saveParser(tree)){                
                report.setStatus(ConfigurationUpdateStatus.SUCCESS);
                log.info("Apache configuration was updated");
                finishConfigurationUpdate(report);
            }else{
                report.setStatus(ConfigurationUpdateStatus.FAILURE);
                log.info("Update of apache configuration failed.");
            }
            
        } catch (Exception e) {
            if (tree != null)
                log.error("Saving of configuration failed.");
            else
                log.error("Saving of configuration failed.", e);
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
        }
   }
    
    public void finishConfigurationUpdate(ConfigurationUpdateReport report) {
        parentComponent.finishConfigurationUpdate(report);
    }
    
    public ApacheDirective getNode(ApacheDirectiveTree tree){
        ApacheDirective node = AugeasNodeSearch.findNodeById(parentComponent.getNode(tree), resourceContext.getResourceKey());        
        return node;
    }
    
    public ApacheDirectiveTree loadParser(){
        return parentComponent.loadParser();
    }
    
    public boolean saveParser(ApacheDirectiveTree tree){
        return parentComponent.saveParser(tree);
    }
    
    public void conditionalRestart() throws Exception{
        parentComponent.conditionalRestart();
    }
}
