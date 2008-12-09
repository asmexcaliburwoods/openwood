package openwood.chat;
import java.rmi.Remote;
import java.rmi.RemoteException;

/** On startup, restores the queue from persistent storage */
public interface ChatConnector extends Remote{
	public static interface Listener{
		void messageArrived(String roomId, String senderId, String plainTextMessage)throws RemoteException;
		void agentOnline(String roomId, String senderId)throws RemoteException;
		void agentBusy(String roomId, String senderId)throws RemoteException;
		void agentOffline(String roomId, String senderId)throws RemoteException;
		/** Unknown parameters should be set to null.*/
		void agentName(String roomId, String senderId, String nickName, String firstName, String lastName, String middleName, String realName)throws RemoteException;
	}
	
	void sendMessage(String roomId, String recipientId, String plaintext)throws RemoteException;
	void joinRoom(String roomId)throws RemoteException;
	void leaveRoom(String roomId)throws RemoteException;
	
	/** sets bot's status to Online, on connect, it is DND/busy */
	void kernelStarted(Listener listener)throws RemoteException;
	/** removes the listener; sets bot's status to DND/busy; 
	 *  makes the connector queue all events incoming to the kernel until it is started */
	void kernelRestarting()throws RemoteException;
	
	/** Saves queue to persistent storage, to be restored on startup. */
	void shutdownConnector()throws RemoteException;
}
