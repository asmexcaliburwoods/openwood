package openwood.chat.impl;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import openwood.chat.ChatConnector;
import openwood.chat.ChatConnector.Listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ChatMultiplexorLink {
	private static final Log LOG=LogFactory.getLog(ChatMultiplexorLink.class);
	public ChatMultiplexorLink(String propFileName, Listener listener)throws Throwable{
			Properties p=new Properties();
			FileInputStream fis=null;
			try{
				fis=new FileInputStream(propFileName);
				p.load(new BufferedInputStream(fis,64*1024));
				startAll(p, listener);
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
	private void startAll(final Properties p, Listener lis) throws Exception {
		LOG.info("Looking up RMI registry");
		lookupRMIRegistry(p);
		LOG.info("RMI registry found.");
		Listener listener=(Listener) UnicastRemoteObject.exportObject(lis);
		String connectors=p.getProperty("connector.ids.comma.delimited");
		if(connectors==null||connectors.trim().isEmpty())
			throw new Exception("connector.ids.comma.delimited is not specified");
		StringTokenizer st=new StringTokenizer(connectors,", \t\r\n;");
		while(st.hasMoreTokens()){
			final String id=st.nextToken();
			try{
				ChatConnector cc=(ChatConnector) registry.lookup(id);
				cc.kernelStarted(listener);
				ccs.add(cc);
			}catch(Throwable tr){
				LOG.error("",tr);
			}
		}
		LOG.info("System startup done.");
	}
	private List<ChatConnector> ccs=new ArrayList<ChatConnector>();
	public void closeConnections(){
		for(int i=0;i<ccs.size();i++){
			try{
				ccs.get(i).kernelRestarting();
			}catch(Throwable tr){
				LOG.error("",tr);
			}
		}
	}
}
