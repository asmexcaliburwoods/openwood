package openwood.chat.impl;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import openwood.chat.ChatConnector;
import openwood.chat.ChatConnectorFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ChatConnectorLauncher {
	private static final Log LOG=LogFactory.getLog(ChatConnectorLauncher.class);
	private static Registry registry;
	public static void main(String[] args){
		try{
			Properties p=new Properties();
			FileInputStream fis=null;
			try{
				fis=new FileInputStream(args[0]);
				p.load(new BufferedInputStream(fis,64*1024));
				
				startAll(p);
			}finally{
				if(fis!=null)try{fis.close();}catch(IOException e){LOG.error("",e);}
			}
		}catch(Throwable tr){
			LOG.error("", tr);
		}
	}
	private static int connectorsStarted=0;
	private static void startAll(final Properties p) throws Exception {
		LOG.info("Looking up RMI registry");
		lookupRMIRegistry(p);
		LOG.info("RMI registry found.");
		String connectors=p.getProperty("connector.ids.comma.delimited");
		if(connectors==null||connectors.trim().isEmpty())
			throw new Exception("connector.ids.comma.delimited is not specified");
		StringTokenizer st=new StringTokenizer(connectors,", \t\r\n;");
		int connectorsTotal=0;
		while(st.hasMoreTokens()){
			final String id=st.nextToken();
			new Thread(new Runnable(){public void run(){try{
				startConnector(id,p);
			}catch(Throwable tr){
				LOG.error("Connector '"+id+"' failed on startup",tr);
			}finally{
				synchronized (ChatConnectorLauncher.class) {
					++connectorsStarted;
					ChatConnectorLauncher.class.notify();
				}
			}}},"connector '"+id+"'").start();
			connectorsTotal++;
		}
		synchronized (ChatConnectorLauncher.class) {
			while(connectorsStarted<connectorsTotal){
				ChatConnectorLauncher.class.wait();
			}
		}
		new Thread(new Runnable(){
			public void run() {
				try {
					consoleShutdownPrompt();
				} catch (Throwable e) {
					LOG.error("",e);
				}
			}},"console shutdown prompt").start();
		LOG.info("System startup done.");
	}
	
	protected static void consoleShutdownPrompt()throws Throwable{
		BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
		while(true){
			String line=br.readLine();
			if(line.equalsIgnoreCase("stop")){System.out.println("SHUTTING DOWN");shutdown();System.out.println("OK");System.exit(0);}
			System.out.print("\nCommands: stop\nCOMMAND: ");
		}
	}

	public static void shutdown() {
		for(ChatConnector cc:ChatConnectorLauncher.connectors.values()){
			try {
				cc.shutdownConnector();
			} catch (Throwable e) {
				LOG.error("Error during shutdown",e);
			}
		}
	}
	private static void lookupRMIRegistry(Properties p) throws RemoteException {
		String host=p.getProperty("rmiregistry.host");
		int port=Integer.parseInt(p.getProperty("rmiregistry.port"));
		try{
			boolean isLocalhost=InetAddress.getByName(host).isLoopbackAddress();
			if(isLocalhost){
				LOG.info("Creating rmiregistry on localhost");
				registry=LocateRegistry.createRegistry(port);
				LOG.info("Created.");
				return;
			}
		}catch(Exception e){
			LOG.warn("", e);
		}
		registry=LocateRegistry.getRegistry(host,port);
	}

	private static Map<String,ChatConnector> connectors=new HashMap<String,ChatConnector>();
	
	protected static void startConnector(String id, Properties p) throws Throwable {
		LOG.info("Connector '"+id+"' starting");
		Properties secondaryProp=new Properties();
		for(Entry<Object, Object> e:p.entrySet()){
			if(!(e.getKey() instanceof String))continue;
			String k=(String) e.getKey();
			if(!(k.startsWith(id+".")))continue;
			k=k.substring(id.length()+1);
			secondaryProp.put(k, e.getValue());
		}
		String factoryClass=secondaryProp.getProperty("factory");
		if(factoryClass==null||factoryClass.trim().isEmpty())
			throw new Exception(id+".factory is not specified");
		ChatConnectorFactory factory=(ChatConnectorFactory) Class.forName(factoryClass.trim()).newInstance();
		ChatConnector connector=factory.createInstance(id, secondaryProp);
		synchronized (connectors) {
			if(connectors.containsKey(id))throw new Exception("more than one connector with id '"+id+"'");
			connectors.put(id, connector);
		}
		LOG.info("Connector '"+id+"' is registering with RMI");
		registerWithRegistry(id,connector);
		LOG.info("Connector '"+id+"' is registered, started.");
	}
	private static void registerWithRegistry(String id, ChatConnector connector) 
		throws AccessException, RemoteException {
		registry.rebind(id, UnicastRemoteObject.exportObject(connector));
	}
}
