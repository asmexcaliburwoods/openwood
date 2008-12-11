package openwood;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.RemoteException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import openwood.RelationExtractor.RelExOptions;
import openwood.chat.ChatConnector;
import openwood.chat.impl.ChatMultiplexorLink;

public class Openwood implements ChatConnector.Listener {
	private static final Log LOG=LogFactory.getLog(Openwood.class);
	ChatMultiplexorLink chatlink;
	public Openwood(String propFileName) throws Throwable{
		RelExOptions options=new RelExOptions();
		options.a_perform_anaphora_resolution=true;
		options.g_use_GATE_entity_detector=true;
		options.f_return_frame_output=true;
		options.n_max_number_of_parses=3;
		options.l_show_parse_links_on_stdout=true;
		RelationExtractor.init(options);
		chatlink=new ChatMultiplexorLink(propFileName,this);
	}
	synchronized void processText(String text){
		RelationExtractor.resetText();
		StringBuilder sb=new StringBuilder();
		sb.append(text.trim());
		//if(!text.endsWith(".")&&!text.endsWith("!")&&!text.endsWith("?"))
			text=text+" .";
		try{RelationExtractor.processText(text);}catch(Throwable tr){LOG.error("",tr);}
	}
	public static void main(String[] args) {
		try{
			final Openwood openwood=new Openwood(args[0]);
			new Thread(new Runnable(){
				public void run() {
					try {
						openwood.consoleShutdownPrompt();
					} catch (Throwable e) {
						LOG.error("",e);
					}
				}},"console shutdown prompt").start();
			LOG.info("System startup done.");
		}catch(Throwable tr){
			LOG.error("",tr);
		}
	}
	@Override
	public void agentBusy(String connectorId, String networkId, String roomId,
			String senderId) throws RemoteException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void agentName(String connectorId, String networkId, String roomId,
			String senderId, String nickName, String firstName,
			String lastName, String middleName, String realName)
			throws RemoteException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void agentOffline(String connectorId, String networkId,
			String roomId, String senderId) throws RemoteException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void agentOnline(String connectorId, String networkId,
			String roomId, String senderId) throws RemoteException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void messageArrived(final String connectorId, final String networkId,
			final String roomId, final String senderId, String plainTextMessage)
			throws RemoteException {
		LOG.debug("MESSAGE_ARRIVED: from "+senderId+(networkId==null?"":"@"+networkId)+"@"+connectorId+": "+plainTextMessage);
		processText(plainTextMessage);
		new Thread(new Runnable(){public void run(){try{
			chatlink.sendMessage(connectorId, networkId, roomId, senderId, 
				"I love silence.");
		}catch(Throwable tr){
			LOG.error("",tr);
		}}},"send response message").start();
	}
	
	public void shutdown(){
		chatlink.shutdown();
	}

	public void consoleShutdownPrompt()throws Throwable{
		BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
		while(true){
			System.out.print("\nCommands: stop -- shuts down the system\nCOMMAND: ");
			String line=br.readLine();
			if(line.equalsIgnoreCase("stop")){System.out.println("SHUTTING DOWN");try{shutdown();}catch(Throwable tr){LOG.error("",tr);}System.out.println("OK");System.exit(0);}
		}
	}
}
