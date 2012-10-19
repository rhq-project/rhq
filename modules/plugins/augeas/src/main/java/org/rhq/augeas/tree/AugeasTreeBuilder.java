package org.rhq.augeas.tree;

import org.rhq.augeas.AugeasProxy;
import org.rhq.augeas.config.AugeasConfiguration;

/**
 * Builds up the in-memory tree representation of the data loaded from Augeas.
 * 
 * @author Filip Drabek
 */
public interface AugeasTreeBuilder {

    /**
     * 
     * @param proxy provides direct access to Augeas should the builder need it
     * @param moduleConfig which Augeas module to use to build the tree 
     * @param name the name of the module to load
     * @param lazy should the tree built lazily or eagerly
     * @return fully built representation of the Augeas data
     * @throws AugeasTreeException
     */
    public AugeasTree buildTree(AugeasProxy proxy, AugeasConfiguration moduleConfig, String name, boolean lazy)
        throws AugeasTreeException;
}
