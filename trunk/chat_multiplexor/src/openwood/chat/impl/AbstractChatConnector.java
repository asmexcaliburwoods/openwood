package openwood.chat.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import openwood.chat.ChatConnector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractChatConnector implements ChatConnector {
	private static final Log LOG=LogFactory.getLog(AbstractChatConnector.class);
	protected final String instanceId;
	protected final Properties properties;
	private Listener kernel;
	private String persistenceSerFileName;
	protected AbstractChatConnector(String instanceId, Properties properties) throws Throwable {
		this.instanceId=instanceId;
		this.properties=properties;
		persistenceSerFileName=properties.getProperty("persistenceSerFileName");
		if(persistenceSerFileName==null||persistenceSerFileName.trim().isEmpty())throw new RuntimeException("persistenceSerFileName is not specified for icq");
		readPersistence();
		login();
	}
	
	protected abstract void login() throws Throwable;

	private void readPersistence()throws Throwable{
		File f=new File(persistenceSerFileName);
		if(!f.exists())return;
		FileInputStream fis=null;
		try{
			fis=new FileInputStream(f);
			ObjectInputStream ois=new ObjectInputStream(new GZIPInputStream(new BufferedInputStream(fis,64*1024)));
			Persistence p=(Persistence) ois.readObject();
			this.queue=p.queue;
			if(p.persistenceFormatVersion==1){
//				fis.close();
//				fis=null;
//				if(!f.delete())LOG.error("",new Exception("Cannot delete persistence file: "+f.getAbsolutePath()));
				return;//valid version
			}
			throw new AssertionError("unknown persistenceFormatVersion: "+p.persistenceFormatVersion);
		}finally{
			if(fis!=null)try{fis.close();}catch(IOException e){LOG.error("",e);}
		}
	}

	/** removes the listener; sets bot's status to DND/busy; 
	 *  makes the connector queue all events incoming to the kernel until it is started */
	@Override
	public final synchronized void kernelRestarting() throws RemoteException {
		kernelRestartingLocal();
	}

	/** sets bot's status to Online, on connect, it is DND/busy */
	@Override
	public final synchronized void kernelStarted(Listener listener) throws RemoteException {
		this.kernel=listener;
		try {
			setOnline();
		} catch (Throwable e) {
			throw new RemoteException("",e);
		}
		dispatchQueuedEvents();
		try {
			savePersistence();
		} catch (Throwable e) {
			LOG.error("",e);
		}
	}

	protected abstract void setOnline() throws Throwable;

	private void dispatchQueuedEvents() throws AssertionError {
		while(!queue.isEmpty()){
			Event e=queue.remove(0);
			if(e instanceof MessageArrivedEvent){
				MessageArrivedEvent e_=(MessageArrivedEvent) e;
				if(!fire_messageArrived(e_.roomId, e_.senderId, e_.plainTextMessage))break;
			}else
				if(e instanceof AgentOnlineEvent){
					AgentOnlineEvent e_=(AgentOnlineEvent) e;
					if(!fire_agentOnline(e_.roomId, e_.senderId))break;
				}else
					if(e instanceof AgentOfflineEvent){
						AgentOfflineEvent e_=(AgentOfflineEvent) e;
						if(!fire_agentOffline(e_.roomId, e_.senderId))break;
					}else
						if(e instanceof AgentBusyEvent){
							AgentBusyEvent e_=(AgentBusyEvent) e;
							if(!fire_agentBusy(e_.roomId, e_.senderId))break;
						}else
							if(e instanceof AgentNameEvent){
								AgentNameEvent e_=(AgentNameEvent) e;
								if(!fire_agentName(
										e_.roomId, e_.senderId, e_.nickName, e_.firstName, e_.lastName, 
										e_.middleName, e_.realName))break;
							}else
								throw new AssertionError("Unknown event type: "+e.getClass());
		}
	}

	private abstract static class Event implements Serializable{
		private static final long serialVersionUID = 7442736463722941829L;
	}
	private static class MessageArrivedEvent extends Event{
		private static final long serialVersionUID = 2341634361375305108L;
		String roomId;
		String senderId;
		String plainTextMessage;
		public MessageArrivedEvent(String roomId,
				String senderId, String plainTextMessage) {
			this.plainTextMessage = plainTextMessage;
			this.roomId = roomId;
			this.senderId = senderId;
		}
		
	}
	private static class AgentOnlineEvent extends Event{
		private static final long serialVersionUID = 933019730617364357L;
		String roomId;
		String senderId;
		public AgentOnlineEvent(String roomId, String senderId) {
			this.roomId = roomId;
			this.senderId = senderId;
		}
		
	}
	private static class AgentOfflineEvent extends Event{
		private static final long serialVersionUID = 2570888851116493110L;
		String roomId;
		String senderId;
		public AgentOfflineEvent(String roomId, String senderId) {
			this.roomId = roomId;
			this.senderId = senderId;
		}
	}
	private static class AgentBusyEvent extends Event{
		private static final long serialVersionUID = -7781373541603988738L;
		String roomId;
		String senderId;
		public AgentBusyEvent(String roomId, String senderId) {
			this.roomId = roomId;
			this.senderId = senderId;
		}
	}
	private static class AgentNameEvent extends Event{
		private static final long serialVersionUID = -7565431438097397935L;
		String roomId;
		String senderId;
		String nickName;
		String firstName;
		String lastName;
		String middleName;
		String realName;
		public AgentNameEvent(String roomId, String senderId, String nickName,
				String firstName, String lastName, String middleName,
				String realName) {
			this.roomId = roomId;
			this.senderId = senderId;
			this.nickName = nickName;
			this.firstName = firstName;
			this.lastName = lastName;
			this.middleName = middleName;
			this.realName = realName;
		}
		
	}
	
	protected final synchronized boolean fire_messageArrived(String roomId, String senderId, String plainTextMessage){
		if(kernel!=null){
			try{kernel.messageArrived(roomId, senderId, plainTextMessage);return true;}
			catch(RemoteException e){kernelRestartingLocal();LOG.warn("will re-send",e);}
		}
		queue(new MessageArrivedEvent(roomId, senderId, plainTextMessage));
		respondKernelRestarting(roomId,senderId);
		return false;
	}
	
	private void respondKernelRestarting(String roomId, String senderId) {
		try {
			sendMessage(roomId, senderId, "Kernel restarts. Your message is queued and will be processed later.");
		} catch (RemoteException e) {
			LOG.error("while respondKernelRestarting",e.getCause());
		}
	}

	private List<Event> queue=Collections.synchronizedList(new LinkedList<Event>());
	
	private static class Persistence implements Serializable{
		private static final long serialVersionUID = -8936684875105402931L;
		int persistenceFormatVersion=1;
		List<Event> queue;
	}
	
	private void queue(Event event) {
		queue.add(event);
		try {
			savePersistence();//to be failsafe
		} catch (Throwable e) {
			LOG.error("",e);
		}
	}

	private void kernelRestartingLocal(){
		kernel=null;
		try{
			setBusy();
		}catch(Throwable e){
			LOG.error("",e);
		}
	}
	
	protected abstract void setBusy() throws Throwable;

	protected final synchronized boolean fire_agentOnline(String roomId, String senderId){
		if(kernel!=null){
			try{kernel.agentOnline(roomId, senderId);return true;}
			catch(RemoteException e){kernelRestartingLocal();LOG.warn("will re-send",e);}
		}
		queue(new AgentOnlineEvent(roomId, senderId));
		return false;
	}
	protected final synchronized boolean fire_agentBusy(String roomId, String senderId){
		if(kernel!=null){
			try{kernel.agentBusy(roomId, senderId);return true;}
			catch(RemoteException e){kernelRestartingLocal();LOG.warn("will re-send",e);}
		}
		queue(new AgentBusyEvent(roomId, senderId));
		return false;
	}
	protected final synchronized boolean fire_agentOffline(String roomId, String senderId){
		if(kernel!=null){
			try{kernel.agentOffline(roomId, senderId);return true;}
			catch(RemoteException e){kernelRestartingLocal();LOG.warn("will re-send",e);}
		}
		queue(new AgentOfflineEvent(roomId, senderId));
		return false;
	}
	/** Unknown parameters should be set to null.
	 * @return */
	protected final synchronized boolean fire_agentName(String roomId, String senderId, String nickName, String firstName, String lastName, String middleName, String realName){
		if(kernel!=null){
			try{kernel.agentName(roomId, senderId, nickName, firstName, lastName, middleName, realName);return true;}
			catch(RemoteException e){kernelRestartingLocal();LOG.warn("will re-send",e);}
		}
		queue(new AgentNameEvent(roomId, senderId, nickName, firstName, lastName, middleName, realName));
		return false;
	}

	@Override
	public final void shutdownConnector() throws RemoteException {
		logout();
		try {
			savePersistence();
		} catch (Throwable e) {
			throw new RemoteException("",e);
		}
	}

	/** Should report and eat all exceptions */
	protected abstract void logout();

	private synchronized void savePersistence()throws Throwable{
		File BACKUP=new File(persistenceSerFileName+".BACKUP");
		if(BACKUP.exists())BACKUP.delete();
		File f=new File(persistenceSerFileName);
		if(f.exists())f.renameTo(BACKUP);
		LOG.info("Persisting "+queue.size()+" events to "+f.getAbsolutePath());
		if(queue.isEmpty())return;//there's nothing to persist
		FileOutputStream fis=null;
		try{
			fis=new FileOutputStream(f);
			ObjectOutputStream ois=new ObjectOutputStream(new GZIPOutputStream(new BufferedOutputStream(fis,64*1024)));
			Persistence p=new Persistence();
			p.queue=queue;
			ois.writeObject(p);
			ois.close();
		}finally{
			if(fis!=null)try{fis.close();}catch(IOException e){LOG.error("",e);}
		}
		if(BACKUP.exists())BACKUP.delete();
	}
}
