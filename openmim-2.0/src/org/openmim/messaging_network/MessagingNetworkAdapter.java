package org.openmim.messaging_network;

import org.openmim.messaging_network.MessagingNetworkListener;
import org.openmim.stuff.UserDetails;

public class MessagingNetworkAdapter implements MessagingNetworkListener
{
  protected MessagingNetworkListener getThisMessagingNetworkListener()
  {
    return this;
  }

  public void messageReceived(byte networkId, String srcLoginId, String dstLoginId, String text) {}

  public void contactsReceived(byte networkId, String srcLoginId, String dstLoginId,
      String[] contactsLoginIds, String[] contactsNicks) {}

  public void statusChanged(byte networkId, String srcLoginId, String dstLoginId,
      int status, int reasonLogger, String reasonMessage, int endUserReasonCode) {}

  /**
    Notifies that an auth reply from srcLoginId uin
    to dstLoginId uin is received.
    States that srcLoginId grants or denies
    preceding dstLoginId's request to add srcLoginId
    to dstLoginId's contact list.
  */
  public void authorizationResponse(byte networkId, String srcLoginId, String dstLoginId,
      boolean grant) {}

  /**
    Notifies that an auth request to dstLoginId uin
    originating from srcLoginId uin is arrived.
    In other words, srcLoginId asks
    if dstLoginId allows to add himself
    to srcLoginId's contact list.
    Reason can be null.
  */
  public void authorizationRequest(byte networkId, String srcLoginId, String dstLoginId,
      String reason) {}

  /** @see org.openmim.messaging_network.MessagingNetwork#startSendMessage(String, String, String) */
  public void sendMessageFailed(byte networkId, long operationId,
    String originalMessageSrcLoginId, String originalMessageDstLoginId, String originalMessageText,
    MessagingNetworkException ex) {}

  /** @see org.openmim.messaging_network.MessagingNetwork#startSendMessage(String, String, String) */
  public void sendMessageSuccess(byte networkId, long operationId,
    String originalMessageSrcLoginId, String originalMessageDstLoginId, String originalMessageText) {}

  public void getUserDetailsFailed(byte networkId, long operationId,
    String originalSrcLoginId, String originalDstLoginId,
    MessagingNetworkException ex) {}

  public void getUserDetailsSuccess(byte networkId, long operationId,
    String originalSrcLoginId, String originalDstLoginId,
    UserDetails userDetails) {}

  /** @see org.openmim.messaging_network.MessagingNetwork#startLogin(String, String, String[], int) */
  public void setStatusFailed(byte networkId, long operationId, String originalSrcLoginId,
    MessagingNetworkException ex) {}
}
