package test.ejb;

import java.rmi.RemoteException;
import javax.ejb.CreateException;
import javax.ejb.EJBHome;

/**
 * Stateless session bean - home interface.
 */
public interface StatelessSessionEJBHome extends EJBHome {

    StatelessSessionEJBObject create() throws RemoteException, CreateException;

}
