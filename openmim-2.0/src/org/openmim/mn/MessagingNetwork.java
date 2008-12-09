package org.openmim.mn;

import org.openmim.mn.MessagingNetworkException;
import org.openmim.UserDetails;
import org.openmim.UserSearchResults;

/**

Synchronous methods (e.g. login()) do never call callback methods xxxxSuccess()/xxxxFailed().

Asynchronous methods (startXxx()) may call callback methods xxxxSuccess()/xxxxFailed().

*/

public interface MessagingNetwork
{
  static int STATUS_ONLINE = 1;
  static int STATUS_BUSY = 2;
  static int STATUS_OFFLINE = 3;

  public String getComment();
  public String getName();
  void addMessagingNetworkListener(MessagingNetworkListener l);
  void removeMessagingNetworkListener(MessagingNetworkListener l) throws MessagingNetworkException;

  /** fast: calls hashtable.get
   * @return
   */
  int getClientStatus(String srcLoginId) throws MessagingNetworkException;

  /** fast: calls hashmap.get twice */
  int getStatus(String srcLoginId, String dstLoginId) throws MessagingNetworkException;

  /** very slow: synchronous */
  void login(String srcLoginId, String password, String[] contactList, int status) throws MessagingNetworkException;

  /**
    fast: asynchronous.

    Returns operation id.

    @see MessagingNetworkListener#setStatusFailed(byte, long, String, MessagingNetworkException)
  */
  long startLogin(String srcLoginId, String password, String[] contactList, int status) throws MessagingNetworkException;

  /**
    fast: asynchronous.

    Returns operation id.

    @see MessagingNetworkListener#sendMessageFailed(byte, long, String, String, String, MessagingNetworkException)
    @see MessagingNetworkListener#sendMessageSucceeded(byte, long, String, String, String)
  */
  long startSendMessage(String srcLoginId, String dstLoginId, String text) throws MessagingNetworkException;

  /** fast: asynchronous */
  void setClientStatus(String srcLoginId, int status)
  throws MessagingNetworkException;

  /** fast: asynchronous */
  void setClientStatus(String srcLoginId, int status, int endUserReason)
  throws MessagingNetworkException;

  /** fast: calls Socket.close() */
  void logout(String srcLoginId) throws MessagingNetworkException;

  /** fast: calls Socket.close() */
  void logout(String srcLoginId, int endUserReason) throws MessagingNetworkException;

  /** slow: packet rate, possibly with sendmsg rate, possibly with msgack wait */
  void addToContactList(String srcLoginId, String dstLoginId) throws MessagingNetworkException;

  /** slow: packet rate */
  void removeFromContactList(String srcLoginId, String dstLoginId) throws MessagingNetworkException;

  /** slow: packet rate with sendmsg rate, possibly with msgack wait */
  void sendContacts(String srcLoginId, String dstLoginId, String[] nicks, String[] loginIds) throws MessagingNetworkException;

  /**
    Postcondition: returns non-null or throws MessagingNetworkException.

    Slow: packet rate with sendmsg rate plus server response wait
  */
  UserDetails getUserDetails(String srcLoginId, String dstLoginId) throws MessagingNetworkException;

  /* async.  returns operation id. */
  long startGetUserDetails(String srcLoginId, String dstLoginId) throws MessagingNetworkException;

  /**
    If null is returned, then no users found.

    Slow: packet rate with sendmsg rate plus server response wait.
    @see org.openmim.UserSearchResults
  */
  UserSearchResults searchUsers(
    String srcLoginId,
    String emailSearchPattern,
    String nickSearchPattern,
    String firstNameSearchPattern,
    String lastNameSearchPattern) throws MessagingNetworkException;

  /**
    Using the connection of srcLoginId uin,
    retrieves the authorizationRequired property of dstLoginId uin
    from the icq server.

    Slow: calls getUserDetails().

    @see #getUserDetails(String,String)
  */
  public boolean isAuthorizationRequired(String srcLoginId, String dstLoginId)
      throws MessagingNetworkException;

  /**
    Using the connection of srcLoginId uin,
    sends an auth request to dstLoginId uin with reason reason,
    to ask if dstLoginId allows to add himself
    to srcLoginId's contact list.
    Reason can be null.

    Slow: packet rate with sendmsg rate, possibly with msgack wait.
  */
  public void authorizationRequest(String srcLoginId, String dstLoginId, String reason)
      throws MessagingNetworkException;

  /**
    Using the connection of srcLoginId uin,
    sends a reply to preceding auth request of dstLoginId uin,
    to state that srcLoginId grants or denies
    dstLoginId's request to add srcLoginId to dstLoginId's contact list.

    Slow: packet rate with sendmsg rate, possibly with msgack wait.
  */
  public void authorizationResponse(String srcLogin, String dstLogin, boolean grant)
      throws MessagingNetworkException;

  void init();
  void deinit();
}
