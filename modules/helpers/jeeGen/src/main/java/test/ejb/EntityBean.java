package test.ejb;

import javax.ejb.CreateException;
import javax.ejb.EntityContext;

/**
 * Entity bean - impl.
 */
public abstract class EntityBean implements javax.ejb.EntityBean {

    private EntityContext context;

    public abstract Integer getKey();
    public abstract void setKey(Integer key);

    public abstract String getName();
    public abstract void setName(String name);

    public void EmployeeBean() {
        // Empty constructor, don't initialize here but in the create().
        // passivate() may destroy these attributes in the case of pooling
    }

    public Integer ejbCreate(Integer key, String name)
        throws CreateException {
        setKey(key);
        setName(name);
        return key;
    }

    public void ejbPostCreate(Integer key, String name)
        throws CreateException {
        // when bean has just bean created
    }

    public void ejbStore() {
        // when bean persisted
    }

    public void ejbLoad() {
        // when bean loaded
    }

    public void ejbRemove() {
        // when bean removed
    }

    public void ejbActivate() {
        // when bean activated
    }

    public void ejbPassivate() {
        // when bean deactivated
    }

    public void setEntityContext(EntityContext context) {
        this.context = context;
    }

    public void unsetEntityContext() {
        this.context = null;
    }

}
