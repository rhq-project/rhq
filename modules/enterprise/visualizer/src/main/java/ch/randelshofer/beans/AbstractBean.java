package ch.randelshofer.beans;

import java.beans.*;

/**
 * Abstract class for beans which have to support property change listeners.<p>
 * Implements the methods required for adding and removing property change
 * listeners.
 *
 * @author Werner Randelshofer
 * @version 1.1 2004-01-18
 * <br>1.0 2001-08-04
 */
public class AbstractBean extends Object implements java.io.Serializable {
    protected PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }
    /*
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener( propertyName, listener);
    }*/
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }/*
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(propertyName, listener);
    }*/

    protected void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        propertySupport.firePropertyChange(propertyName, new Boolean(oldValue), new Boolean(newValue));
    }
    protected void firePropertyChange(String propertyName, int oldValue, int newValue) {
        propertySupport.firePropertyChange(propertyName, new Integer(oldValue), new Integer(newValue));
    }
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        propertySupport.firePropertyChange(propertyName, oldValue, newValue);
    }
}