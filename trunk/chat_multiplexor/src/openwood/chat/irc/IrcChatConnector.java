package openwood.chat.irc;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import openwood.chat.impl.AbstractChatConnector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmim.kernel.Kernel;
import org.openmim.kernel.KernelListener;
import org.openmim.mn2.controller.IMNetwork;
import org.openmim.mn2.model.ChannelContactBean;
import org.openmim.mn2.model.ConfigurationBean;
import org.openmim.mn2.model.GlobalIRCParameters;
import org.openmim.mn2.model.GlobalParameters;
import org.openmim.mn2.model.IMNetworkBean;
import org.openmim.mn2.model.IRCNetworkBean;
import org.openmim.mn2.model.IRCServerBean;
import org.openmim.mn2.model.Query;
import org.openmim.mn2.model.RoleToDisplayBean;
import org.openmim.mn2.model.Room;
import org.openmim.mn2.model.ServerBean;
import org.openmim.mn2.model.StatusRoom;

public class IrcChatConnector extends AbstractChatConnector {
	private final static Log LOG=LogFactory.getLog(IrcChatConnector.class);
	private final class KL_Impl implements KernelListener {
		@Override
		public void onActionMessage(IMNetwork net, Room room,
				String nickName, String text) {
		}

		@Override
		public void onAwayMessage(IMNetwork mn, String nick, String msg) {
		}

		@Override
		public void onConnected(IMNetwork imNetwork) {
		}

		@Override
		public void onConnecting(StatusRoom statusRoom) {
		}

		@Override
		public void onGetCreateStatusRoom(StatusRoom room) {
		}

		@Override
		public void onMeJoined(IMNetwork net, String s) {
		}

		@Override
		public void onMessage(IMNetwork net, Room room, String nickFrom,
				String text) {
			boolean isprivate=room instanceof Query;
			LOG.debug("ONMESSAGE: net: "+net+" isprivate: "+isprivate+" room: ("+room+") from: "+nickFrom);
			String botNick=kernel.getCtx().getMetaNetwork().getIrc().getIMNetworks().get(0).getCurrentLoginId();
			if(!isprivate)if(!text.toLowerCase().startsWith(botNick.toLowerCase()))return;
			text=(text.toLowerCase().startsWith(botNick.toLowerCase()))?text.substring(botNick.length()).trim():text;
			if(text.startsWith(":")||text.startsWith(","))text=text.substring(1).trim();
			fire_messageArrived(isprivate?null:room.toString(), 
					nickFrom, text);
		}

		@Override
		public void onNickChange(IMNetwork net, String s, String s1) {
		}

		@Override
		public void onNoSuchNickChannel(IMNetwork net,
				String nickOrChannel, String comment) {
		}

		@Override
		public void onNotice(StatusRoom room, String nick, String text) {
		}

		@Override
		public void onQuit(IMNetwork net, String s, String s1) {
		}

		@Override
		public void onRegistering(StatusRoom room) {
		}

		@Override
		public void onUserLoggedOn(IMNetwork net, boolean me, String nick,
				String note) {
		}

		@Override
		public void onUserQuit(IMNetwork net, boolean me, String nick,
				String note) {
		}

		@Override
		public void onWelcome(StatusRoom room, String newNick, String text) {
			joinChannels(room.getIMNetwork());
		}
	}

	private Kernel kernel;
	
	public IrcChatConnector(String instanceId, Properties properties) throws Throwable {
		super(instanceId,properties);
	}
	
	public void joinChannels(IMNetwork net) {
		String[] channels=properties.getProperty("auto.network."+net.getKey()+".channel").split(",");
		for(String ch:channels){
			ChannelContactBean leaf=new ChannelContactBean();
			leaf.setNetworkKey(net.getKey());
			leaf.setLoginId(ch);
			leaf.setDisplayName(ch);
			try {
				net.joinRoom(leaf);
			} catch (Throwable e) {
				LOG.error("joinRoom",e);
			}
		}
	}

	public void login()throws Throwable{
		kernel=Kernel.create(createCB());
//		new Thread(new Runnable(){
//			public void run() {
//				try{
					kernel.doAll(new KL_Impl());
//				}catch(Throwable tr){
//					LOG.error("irc",tr);
//				}
//			}},"irc").start();
	}

	private ConfigurationBean createCB() {
		ConfigurationBean cb=new ConfigurationBean();
		GlobalIRCParameters gp=new GlobalIRCParameters();
		gp.setAutoRejoinChannelsOnKick(false);
		gp.setDebug(true);
		cb.setGlobalIRCParameters(gp);
		GlobalParameters g=new GlobalParameters();
		g.setNickNameList((List<String>)Arrays.asList(properties.getProperty("nicks").split("[,;]")));
		g.setRealNameForIRC(properties.getProperty("realname"));
		g.setRealNameForIRCUsed(true);
		cb.setGlobalParameters(g);
		List<IMNetworkBean> networksConfigured=new ArrayList<IMNetworkBean>();
		for(Entry<Object,Object> e:properties.entrySet()){
			String k=(String) e.getKey();
			k=k.trim();
			String v=(String) e.getValue();
			if(k.equals("network")){
				k=instanceId;
				String netName=k.toLowerCase();
				StringTokenizer st=new StringTokenizer(v,",;");
				while(st.hasMoreTokens()){
					String[] kv=st.nextToken().split("=");
					properties.setProperty("auto.network."+netName+"."+(kv[0].trim()), kv[1].trim());
				}
				String hostPort=properties.getProperty("auto.network."+netName+".hostport");
				String[] hps=hostPort.split(":");
				String host=hps[0].trim();
				String portStr=hps.length>1?hps[1].trim():"6667";
				int port=Integer.parseInt(portStr);
				IRCServerBean sb=new IRCServerBean();
				sb.setHostName(host);
				sb.setPort(port);
				IRCNetworkBean imNetwork=new IRCNetworkBean();
				imNetwork.setKey(netName);
				imNetwork.setType(IMNetwork.Type.irc);
				sb.setImNetwork(imNetwork);
				networksConfigured.add(imNetwork);
				imNetwork.setServers(Collections.singletonList((ServerBean)sb));
				break;//only single instance of IMNET per IrcChatConnector
			}
		}
//		cb.setNetworkKeyCanonical2ListOfServersToKeepConnectionWith(servmap);
		cb.setNetworksConfigured(networksConfigured);
		List<RoleToDisplayBean> rolesToDisplay=new ArrayList<RoleToDisplayBean>();
//		RoleToDisplayBean e=new RoleToDisplayBean();
//		e.set
//		rolesToDisplay.add(e);
		cb.setRolesToDisplay(rolesToDisplay);
//		cb.setContactList(null);
		return cb;
	}

	@Override
	public void joinRoom(String ch) throws RemoteException {
		ChannelContactBean leaf=new ChannelContactBean();
		leaf.setNetworkKey(instanceId);
		leaf.setLoginId(ch);
		leaf.setDisplayName(ch);
		try {
			kernel.getCtx().getMetaNetwork().getIrc().getIMNetworks().get(0).joinRoom(leaf);
		} catch (Throwable e) {
			LOG.error("",e);
			throw new RemoteException("joinRoom",e);
		}
	}

	@Override
	public void leaveRoom(String roomId) throws RemoteException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sendMessage(String roomId, String recipientId, String plaintext)
			throws RemoteException {
		try {
			kernel.getCtx().getMetaNetwork().getIrc().getIMNetworks().get(0).
				sendMessage((roomId==null?recipientId:roomId), ""+(roomId==null?"":recipientId+": ")+plaintext);
		} catch (Throwable e) {
			LOG.error("",e);
			throw new RemoteException("sendMsg",e);
		}
	}

	@Override
	protected void logout() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void setBusy() throws Throwable {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void setOnline() throws Throwable {
		// TODO Auto-generated method stub
		
	}
}
