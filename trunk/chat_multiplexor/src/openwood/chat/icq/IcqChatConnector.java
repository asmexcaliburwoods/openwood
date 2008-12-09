package openwood.chat.icq;

import java.rmi.RemoteException;
import java.util.Properties;

import openwood.chat.impl.AbstractChatConnector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmim.UserDetails;
import org.openmim.icq2k.ICQ2KMessagingNetwork;
import org.openmim.icq2k.ICQ2KMessagingNetworkReconnecting;
import org.openmim.mn.MessagingNetwork;
import org.openmim.mn.MessagingNetworkException;
import org.openmim.mn.MessagingNetworkListener;

public class IcqChatConnector extends AbstractChatConnector {
	private static final Log LOG=LogFactory.getLog(IcqChatConnector.class);
	private ICQ2KMessagingNetwork mn;
	private String bot_loginid;
	public IcqChatConnector(String instanceId, Properties properties) throws Throwable {
		super(instanceId,properties);
	}
	
	protected void login() throws MessagingNetworkException{
		mn=new ICQ2KMessagingNetworkReconnecting();
		mn.init();
		mn.addMessagingNetworkListener(new MNL_Impl());
		bot_loginid=properties.getProperty("bot.loginid");
		mn.login(
				bot_loginid, 
				properties.getProperty("bot.password"), 
				new String[]{"10000"}, 
				MessagingNetwork.STATUS_BUSY);//busy until kernel adds a listener
	}

	@Override
	public void joinRoom(String roomId) throws RemoteException {
		throw new UnsupportedOperationException();
	}

	public void setOnline() throws MessagingNetworkException{
		mn.setClientStatus(bot_loginid, MessagingNetwork.STATUS_ONLINE);
	}

	public void setBusy() throws MessagingNetworkException{
		mn.setClientStatus(bot_loginid, MessagingNetwork.STATUS_BUSY);
	}

	@Override
	public void leaveRoom(String roomId) throws RemoteException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sendMessage(String roomId, String recipientId, String plaintext)
			throws RemoteException {
		try {
			mn.sendMessage(bot_loginid, recipientId, plaintext);
		} catch (Throwable e) {
			throw new RemoteException("",e);
		}
	}

	private final class MNL_Impl implements MessagingNetworkListener {
		@Override
		public void authorizationRequest(byte networkId,
				String senderLoginId, String recipientLoginId, String reason) {
			try {
				mn.authorizationResponse(bot_loginid, recipientLoginId, true);
			} catch (Throwable e) {
				LOG.error("",e);
			}			
		}

		@Override
		public void authorizationResponse(byte networkId,
				String senderLoginId, String recipientLoginId, boolean grant) {}

		@Override
		public void contactsReceived(byte networkId, String senderLoginId,
				String recipientLoginId, String[] contactsLoginIds,
				String[] contactsNicks) {}

		@Override
		public void getUserDetailsFailed(byte networkId, long operationId,
				String originalSrcLoginId, String originalDstLoginId,
				MessagingNetworkException ex) {}

		@Override
		public void getUserDetailsSuccess(byte networkId, long operationId,
				String originalSrcLoginId, String originalDstLoginId,
				UserDetails userDetails) {
			if(userDetails==null)return;
			fire_agentName(null, originalDstLoginId, 
					userDetails.getNick(), 
					null, null, null, userDetails.getRealName());
		}

		@Override
		public void messageReceived(byte networkId, String senderLoginId,
				String recipientLoginId, String text) {
			fire_messageArrived(null, senderLoginId, text);
		}

		@Override
		public void sendMessageFailed(byte networkId, long operationId,
				String originalMessageSenderLoginId,
				String originalMessageRecipientLoginId,
				String originalMessageText, MessagingNetworkException ex) {
			LOG.error("sendMessageFailed",ex);
		}

		@Override
		public void sendMessageSuccess(byte networkId, long operationId,
				String originalMessageSenderLoginId,
				String originalMessageRecipientLoginId,
				String originalMessageText) {}

		@Override
		public void setStatusFailed(byte networkId, long operationId,
				String originalSrcLoginId, MessagingNetworkException ex) {
			LOG.error("setStatusFailed",ex);
		}

		@Override
		public void statusChanged(byte networkId, String srcLoginId,
				String dstLoginId, int status, int reasonLogger,
				String reasonMessage, int endUserReasonCode) {
			switch(status){
			case MessagingNetwork.STATUS_ONLINE:
				fire_agentOnline(null, srcLoginId);
				break;
			case MessagingNetwork.STATUS_OFFLINE:
				fire_agentOffline(null, srcLoginId);
				break;
			case MessagingNetwork.STATUS_BUSY:
				fire_agentBusy(null, srcLoginId);
				break;
			default:
				LOG.error("Unknown status: "+status);
				break;
			}
		}
	}
    public void logout(){
    	try{
    		mn.deinit();
    	}catch(Throwable tr){
    		LOG.error("", tr);
    	}
	}
}