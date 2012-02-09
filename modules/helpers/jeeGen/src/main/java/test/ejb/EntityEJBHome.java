package test.ejb;

import java.rmi.RemoteException;
import java.util.Collection;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;
import javax.ejb.FinderException;

/**
 * Entity bean - home interface.
 */
public interface EntityEJBHome extends EJBHome {

    public EntityEJBObject create(Integer key, String name)
        throws CreateException, RemoteException;

    public EntityEJBObject findByPrimaryKey(Integer key)
        throws FinderException, RemoteException;

    public Collection<EntityEJBObject> findByName(String name)
        throws FinderException, RemoteException;

    public Collection<EntityEJBObject> findAll()
        throws FinderException, RemoteException;

}
