package org.openmim.mn;

import org.openmim.mn.MessagingNetworkException;
import org.openmim.UserDetails;

public interface MessagingNetworkListener
{
  void messageReceived(byte networkId, String senderLoginId, String recipientLoginId, String text);

  void contactsReceived(byte networkId, String senderLoginId, String recipientLoginId,
      String[] contactsLoginIds, String[] contactsNicks);

  void statusChanged(byte networkId, String srcLoginId, String dstLoginId,
      int status, int reasonLogger, String reasonMessage, int endUserReasonCode);

  /**
    Notifies that an auth reply from srcLoginId uin
    to dstLoginId uin is received.
    States that srcLoginId grants or denies
    preceding dstLoginId's request to add srcLoginId
    to dstLoginId's contact list.
  */
  void authorizationResponse(byte networkId, String senderLoginId, String recipientLoginId,
      boolean grant);

  /**
    Notifies that an auth request to dstLoginId uin
    originating from srcLoginId uin is arrived.
    In other words, srcLoginId asks
    if dstLoginId allows to add himself
    to srcLoginId's contact list.
    Reason can be null.
  */
  void authorizationRequest(byte networkId, String senderLoginId, String recipientLoginId,
      String reason);

  /** @see MessagingNetwork#startSendMessage(String, String, String) */
  void sendMessageFailed(byte networkId, 
    long operationId, 
    String originalMessageSenderLoginId, String originalMessageRecipientLoginId, String originalMessageText, 
    MessagingNetworkException ex);
    
  /** @see MessagingNetwork#startSendMessage(String, String, String) */
  void sendMessageSuccess(byte networkId, 
    long operationId, 
    String originalMessageSenderLoginId, String originalMessageRecipientLoginId, String originalMessageText);

  void getUserDetailsFailed(byte networkId, 
    long operationId, 
    String originalSrcLoginId, String originalDstLoginId, 
    MessagingNetworkException ex);
    
  void getUserDetailsSuccess(byte networkId, 
    long operationId, 
    String originalSrcLoginId, String originalDstLoginId, 
    UserDetails userDetails);
    
  /** @see MessagingNetwork#startLogin(String, String, String[], int) */
  void setStatusFailed(byte networkId, 
    long operationId, 
    String originalSrcLoginId, 
    MessagingNetworkException ex);
}