package openwood.chat.xmpp;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import openwood.chat.impl.AbstractChatConnector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

public class XMPPChatConnector extends AbstractChatConnector {
	private static Log LOG=LogFactory.getLog(XMPPChatConnector.class);
	protected XMPPConnection connection;
	protected Map<String,Chat> loginId2Chat=new HashMap<String,Chat>();
	
	public XMPPChatConnector(String instanceId, Properties properties) throws Throwable {
		super(instanceId,properties);
	}

	@Override
	protected void login() throws Throwable {
		// Create a connection to the jabber.org server.
		String host=properties.getProperty("host").trim();
		String serviceName=properties.getProperty("service.name").trim();
		String port=properties.getProperty("port");
		if(port==null)port="5222";else port=port.trim();
		int portInteger=Integer.parseInt(port);
		//new Thread(new Runnable(){public void run(){try{
			ConnectionConfiguration config = new ConnectionConfiguration(host, portInteger,serviceName);
			config.setSASLAuthenticationEnabled(Boolean.valueOf(properties.getProperty("sasl","false")));
			//disabled, enabled, required
			config.setSecurityMode(ConnectionConfiguration.SecurityMode.valueOf(properties.getProperty("tls","disabled")));
			//config.setSendPresence(false);
			//config.setRosterLoadedAtLogin(false);
			LOG.info("SASL: "+config.isSASLAuthenticationEnabled()+" TLS: "+config.getSecurityMode());
			config.setSocketFactory(null);
			connection = new XMPPConnection(config);
			connection.connect();
			// Create a packet filter to listen for new messages from a particular
			// user. We use an AndFilter to combine two other filters.
			PacketFilter filter = new PacketTypeFilter(Message.class);
			
			// Assume we've created an XMPPConnection name "connection".

			// First, register a packet collector using the filter we created.
			//PacketCollector myCollector = 
				connection.createPacketCollector(filter);
			// Normally, you'd do something with the collector, like wait for new packets.

			// Next, create a packet listener. We use an anonymous inner class for brevity.
			PacketListener myListener = new PacketListener() {
			        public void processPacket(Packet packet) {
			            Message message=(Message) packet;
			            Message.Type type=message.getType();
			            if(type!=Message.Type.normal&&type!=Message.Type.chat)return;
			            String from=message.getFrom();
			            String subject=message.getSubject();
			            String body=message.getBody();
			            if(subject==null)subject="";
			            if(body==null)body="";
			            if(!subject.trim().equals("")&&!subject.endsWith(".")&&!subject.endsWith("?")&&!subject.endsWith("!"))subject=subject+".";
			            body=subject+"\n"+body;
			            body=body.trim();
			            LOG.debug("MESSAGE_ARRIVED: "+from+": "+body);
			            fire_messageArrived(null, from, body);
			        }
			    };
			// Register the listener.
			connection.addPacketListener(myListener, filter);
			connection.login(
					properties.getProperty("username").trim(), 
					properties.getProperty("password"), 
					properties.getProperty("resource").trim());
			setBusy();
//		}catch(Throwable tr){
//			LOG.error("",tr);
//		}}},"jabber").start();
	}

	@Override
	protected void logout() {
		try{
			if(connection!=null){connection.disconnect();connection=null;}
		}catch(Throwable tr){
			LOG.error("",tr);
		}
	}

	@Override
	protected void setBusy() throws Throwable {
		setPresenceMode(Presence.Mode.dnd);
	}

	@Override
	protected void setOnline() throws Throwable {
		setPresenceMode(Presence.Mode.available);
	}
	
	protected void setPresenceMode(Presence.Mode mode){
		Presence p=new Presence(Presence.Type.available);
		p.setMode(mode);
		connection.sendPacket(p);
	}

	@Override
	public void joinRoom(String roomId) throws RemoteException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void leaveRoom(String roomId) throws RemoteException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sendMessage(String roomId, String recipientId, String plaintext)
			throws RemoteException {
		synchronized (loginId2Chat) {
			Chat chat=loginId2Chat.get(recipientId);
			if(chat==null){
				ChatManager chatmanager = connection.getChatManager();
				chat = chatmanager.createChat(recipientId, new MessageListener() {
				    public void processMessage(Chat chat, Message message) {
				    }
				});
			}
			try {
				chat.sendMessage(plaintext);
			} catch (XMPPException e) {
				LOG.error("",e);
				throw new RemoteException("",e);
			}
		}
	}
}
