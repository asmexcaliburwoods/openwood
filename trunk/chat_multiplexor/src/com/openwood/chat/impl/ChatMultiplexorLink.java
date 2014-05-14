package com.openwood.chat.impl;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openwood.chat.ChatConnector;
import com.openwood.chat.ChatConnector.Listener;

public class ChatMultiplexorLink {
	private static final Log LOG=LogFactory.getLog(ChatMultiplexorLink.class);
	public ChatMultiplexorLink(String propFileName, final Listener listener)throws Throwable{
			final Properties p=new Properties();
			FileInputStream fis=null;
			try{
				fis=new FileInputStream(propFileName);
				p.load(new BufferedInputStream(fis,64*1024));
				new Thread(new Runnable(){public void run(){
					try{
						startAll(p, listener);
					}catch(Throwable tr){
						LOG.error("",tr);
					}
				}}).start();
			}finally{
				if(fis!=null)try{fis.close();}catch(IOException e){LOG.error("",e);}
			}
	}
	private void lookupRMIRegistry(Properties p) throws RemoteException {
		String host=p.getProperty("rmiregistry.host");
		int port=Integer.parseInt(p.getProperty("rmiregistry.port"));
		registry=LocateRegistry.getRegistry(host,port);
	}
	private Registry registry;
	private Collection<ChatConnector> connectors;
	private void startAll(final Properties p, Listener lis) throws Exception {
		LOG.info("Looking up RMI registry");
		lookupRMIRegistry(p);
		LOG.info("RMI registry found.");
		final Listener listener=(Listener) UnicastRemoteObject.exportObject(lis);
		String connectors=p.getProperty("connector.ids.comma.delimited");
		if(connectors==null||connectors.trim().isEmpty())
			throw new Exception("connector.ids.comma.delimited is not specified");
		StringTokenizer st=new StringTokenizer(connectors,", \t\r\n;");
		while(st.hasMoreTokens()){
			final String id=st.nextToken().toLowerCase();
			try{
				final ChatConnector cc=(ChatConnector) registry.lookup(id);
				LOG.info("Emitting kernelStarted message to "+id);
				new Thread(new Runnable(){public void run(){try{
					cc.kernelStarted(listener);
					LOG.info(id+" kernelStarted done.");
				}catch(Throwable tr){LOG.error("",tr);}}}, "kernel started event for "+id).start();
				connectorId2connector.put(id,cc);
			}catch(Throwable tr){
				LOG.error("",tr);
			}
		}
		LOG.info("ChatMultiplexorLink startup done.");
	}
	private Map<String,ChatConnector> connectorId2connector=new HashMap<String,ChatConnector>();
	public void shutdown(){
		Iterator<ChatConnector> it=connectorId2connector.values().iterator();
		for(;it.hasNext();){
			ChatConnector cc=it.next();
			try{
				cc.kernelRestarting();
			}catch(Throwable tr){
				LOG.error("",tr);
			}
		}
	}
	
	public void joinRoom(String connectorId, String networkId, String roomId)
			throws RemoteException {
		ChatConnector connector=lookup(connectorId);
		LOG.debug("JOINING ROOM: "+roomId+(networkId==null?"":"@"+networkId)+"@"+connectorId);
		connector.joinRoom(networkId, roomId);
	}
	public ChatConnector lookup(String connectorId) {
		return connectorId2connector.get(connectorId);
	}
	public Map<String,ChatConnector> getConnectorId2ConnectorMap() {
		return Collections.unmodifiableMap(connectorId2connector);
	}
	public void leaveRoom(String connectorId, String networkId, String roomId)
			throws RemoteException {
		ChatConnector connector=lookup(connectorId);
		LOG.debug("LEAVING ROOM: "+roomId+(networkId==null?"":"@"+networkId)+"@"+connectorId);
		connector.leaveRoom(networkId, roomId);
	}
	public void sendMessage(String connectorId, String networkId, String roomId,
			String recipientId, String plaintext) throws RemoteException {
		ChatConnector connector=lookup(connectorId);
		LOG.debug("SENDING RESPONSE: "+plaintext+" to "+recipientId);
		connector.sendMessage(networkId, roomId, recipientId, plaintext);
	}
}
