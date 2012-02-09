package test.ejb.v2;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;

/**
 * Stateful session bean - home interface.
 */
public interface StatefulSessionEJBHome extends EJBHome {

    StatefulSessionEJBObject create() throws RemoteException, CreateException;

}
