package org.openmim.msn;

import org.openmim.*;
import org.openmim.messaging_network.MessagingNetwork;
import org.openmim.messaging_network.MessagingNetworkException;
import org.openmim.messaging_network.MessagingNetworkListener;
import org.openmim.stuff.Defines;
import org.openmim.stuff.StatusUtilMim;
import org.openmim.stuff.TransportChooser;
import org.openmim.stuff.UserDetails;
import org.openmim.stuff.UserSearchResults;
import org.openmim.wrapper.*;
import org.openmim.icq.util.*;
import org.openmim.icq.utils.*;

import java.util.*;

public class MSNMessagingNetwork implements MessagingNetwork, MessagingNetworkWrappable
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(MSNMessagingNetwork.class.getName());

  static
  {
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("MSN Session.java revision: "+Session.getVersionString());
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("MSN DS.java      revision: "+DS.getVersionString());
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("MSN SSS.java     revision: "+SSS.getVersionString());
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("MSN Errors.java  revision: "+Errors.getVersionString());
  }

  public static String REQPARAM_LOGIN_HOST;
  public static int    REQPARAM_LOGIN_PORT;

  /** session threadpool config */
  public static int REQPARAM_INPUT_DATA_HANDLING_THREADCOUNT_OPTIMUM;
  /** session threadpool config */
  public static int REQPARAM_INPUT_DATA_HANDLING_THREADCOUNT_MAXIMUM;

  /** parameter affecting deinit() */
  public static int REQPARAM_INPUTDATA_TASKMANAGER_STOP_TIME_MILLIS;

  /** parameter affecting deinit() */
  public static int REQPARAM_RECONNECTOR_TASKMANAGER_STOP_TIME_MILLIS;

  public static boolean REQPARAM_RECONNECTOR_USED;

  /** reconnector threadpool config */
  public static int REQPARAM_RECONNECTOR_THREADCOUNT_OPTIMUM;
  /** reconnector threadpool config */
  public static int REQPARAM_RECONNECTOR_THREADCOUNT_MAXIMUM;

  /** @see TransportChooser */
  public static String REQPARAM_TRANSPORTS_ALLOWED;

  /**
    @see AutoConfig
    @see Session#fetchUserDetails(...)
  */
  public static int REQPARAM_SOCKET_TIMEOUT_SECONDS;

  /** SS sessions are closed when their idle time exceeds this */
  public static int REQPARAM_SSS_MAXIMUM_IDLE_TIME_SECONDS;

  /** reconnector param */
  public static int REQPARAM_NETWORK_CONDITIONS_SWING__MAXIMUM_COUNT_OF_REGISTERED_RELOGINS;
  /** reconnector param */
  public static int REQPARAM_NETWORK_CONDITIONS_SWING__FORGET_TIMEOUT_FOR_REGISTERED_RELOGINS_MINUTES;
  /** reconnector param */
  public static int REQPARAM_NETWORK_CONDITIONS_SWING__SLEEP_TIME_WHEN_MAXIMUM_REACHED_MINUTES;

  static
  {
    try
    {
      java.security.MessageDigest.getInstance("MD5");
    }
    catch (java.security.NoSuchAlgorithmException ex)
    {
      throw new AssertException("MSN plugin reports: MD5 security provider is not installed, see java.security.MessageDigest class. MD5 provider is present in JDK1.3.1+.");
    }

    AutoConfig.fetchFromClassLocalResourceProperties(MSNMessagingNetwork.class, true, false);
    if (REQPARAM_SOCKET_TIMEOUT_SECONDS <= 0)
      throw new RuntimeException("REQPARAM_SOCKET_TIMEOUT_SECONDS autoconfig property must be positive, but it is " + REQPARAM_SOCKET_TIMEOUT_SECONDS);

    Class c = StatusFilterUtil.class;
  }

  //
  public static final byte MSN_NETWORK_ID = 2;
  public static final String MSN_NETWORK_NAME = "MSN";
  private final List messagingNetworkListeners = new Vector(1, 1);
  protected final PluginContext context = new PluginContext(this);
  protected final ResourceManager resourceManager = makeResourceManagerInstance();
  protected final TransportChooser transportChooser;

//  final static boolean keepAlivesUsed;
//  final static int keepAlivesIntervalMillis;
//  public final static boolean multiplexorsUsed;

//      keepAlivesUsed = PropertyUtil.getRequiredPropertyBoolean(props, propFileDisplayName, "keepalives.used");
//      if (keepAlivesUsed)
//      {
//        int keepAlivesIntervalSeconds = PropertyUtil.getRequiredPropertyInt(props, propFileDisplayName, "keepalives.interval.seconds");
//        if (keepAlivesIntervalSeconds < 30)
//          throw new RuntimeException("Invalid property value for keepalives.interval.seconds: " + keepAlivesIntervalSeconds + ", must be >= 30.");
//        keepAlivesIntervalMillis = 1000 * keepAlivesIntervalSeconds;
//      }
//      else
//      {
//        keepAlivesIntervalMillis = 0;
//      }

//      multiplexorsUsed = PropertyUtil.getRequiredPropertyBoolean(props, propFileDisplayName, "multiplexors.used");
//      if (!multiplexorsUsed)

  public static String REQPARAM_SOCKS5_HOST;
  public static int    REQPARAM_SOCKS5_PORT;
  public static String REQPARAM_SOCKS5_USERNAME;
  public static String REQPARAM_SOCKS5_PASSWORD;

  public MSNMessagingNetwork()
  {
    boolean noAuth = StringUtil.isNullOrTrimmedEmpty(REQPARAM_SOCKS5_USERNAME);
    transportChooser = new TransportChooser(
      REQPARAM_TRANSPORTS_ALLOWED,
      REQPARAM_SOCKS5_HOST,
      REQPARAM_SOCKS5_PORT,
      ( noAuth ? null : REQPARAM_SOCKS5_USERNAME ),
      ( noAuth ? null : REQPARAM_SOCKS5_PASSWORD ));
  }

  public final void addMessagingNetworkListener(MessagingNetworkListener l)
  {
    if ((l) == null) Lang.ASSERT_NOT_NULL(l, "listener");
    synchronized (messagingNetworkListeners)
    {
      messagingNetworkListeners.remove(l);
      messagingNetworkListeners.add(l);
    }
  }

  public void addToContactList(String srcLoginId, String dstLoginId)
  throws MessagingNetworkException
  {
    Session session = getSessionNotNull(srcLoginId, MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_ADD_TO_CONTACT_LIST_WHILE_OFFLINE);
    synchronized (session)
    {
      session.addToContactList(dstLoginId, context);
    }
  }

  void fireMessageReceived(String srcLoginId, String dstLoginId, String messageText)
  {
    if (StringUtil.isNullOrEmpty(srcLoginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(srcLoginId, "srcLoginId");
    if (StringUtil.isNullOrEmpty(dstLoginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(dstLoginId, "dstLoginId");
    if ((messageText) == null) Lang.ASSERT_NOT_NULL(messageText, "messageText");

    synchronized (messagingNetworkListeners)
    {
      for (int i = 0; i < messagingNetworkListeners.size(); i++)
      {
        MessagingNetworkListener l = (MessagingNetworkListener) messagingNetworkListeners.get(i);
        Session ses = getSession0(dstLoginId);
        if ((ses) == null) Lang.ASSERT_NOT_NULL(ses, "session for " + dstLoginId);
        //synchronized (ses) //commented out because of deadlock bug
        //{
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("MSN FIRES EVENT to core: messageReceived: src " + srcLoginId + ", dst " + dstLoginId + ", text "+StringUtil.toPrintableString(messageText)+", listener: " + l);
        l.messageReceived(getNetworkId(), srcLoginId, dstLoginId, messageText);
        //}
      }
    }
  }

  /*
  void fireAuthRequestReceived(String srcLoginId, String dstLoginId, String messageText)
  {
    if (StringUtil.isNullOrEmpty(srcLoginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(srcLoginId, "srcLoginId");
    if (StringUtil.isNullOrEmpty(dstLoginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(dstLoginId, "dstLoginId");
    if ((messageText) == null) Lang.ASSERT_NOT_NULL(messageText, "messageText");

    //
    synchronized (messagingNetworkListeners)
    {
      for (int i = 0; i < messagingNetworkListeners.size(); i++)
      {
        MessagingNetworkListener l = (MessagingNetworkListener) messagingNetworkListeners.elementAt(i);
        Session ses = getSession0(dstLoginId);
        if ((ses) == null) Lang.ASSERT_NOT_NULL(ses, "session for " + dstLoginId);
        //synchronized (ses) //commented out because of deadlock bug
        //{
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("MSN FIRES EVENT to core: authorizationRequest: src " + srcLoginId + ", dst " + dstLoginId + ", text "+StringUtil.toPrintableString(messageText)+", listener: " + l);
        l.authorizationRequest(getNetworkId(), srcLoginId, dstLoginId, messageText);
        //}
      }
    }
  }

  void fireAuthReplyReceived(String srcLoginId, String dstLoginId, boolean grant)
  {
    if (StringUtil.isNullOrEmpty(srcLoginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(srcLoginId, "srcLoginId");
    if (StringUtil.isNullOrEmpty(dstLoginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(dstLoginId, "dstLoginId");

    //
    synchronized (messagingNetworkListeners)
    {
      for (int i = 0; i < messagingNetworkListeners.size(); i++)
      {
        MessagingNetworkListener l = (MessagingNetworkListener) messagingNetworkListeners.elementAt(i);
        Session ses = getSession0(dstLoginId);
        if ((ses) == null) Lang.ASSERT_NOT_NULL(ses, "session for " + dstLoginId);
        //synchronized (ses) //commented out because of deadlock bug
        //{
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("MSN FIRES EVENT to core: authorizationResponse: src " + srcLoginId + ", dst " + dstLoginId + ", grant=="+grant+", listener: " + l);
        l.authorizationResponse(getNetworkId(), srcLoginId, dstLoginId, grant);
        //}
      }
    }
  }

  void fireContactsReceived(String srcLoginId, String dstLoginId, String[] contactsLoginIds, String[] contactsNicks)
  {
    if (StringUtil.isNullOrEmpty(srcLoginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(srcLoginId, "srcLoginId");
    if (StringUtil.isNullOrEmpty(dstLoginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(dstLoginId, "dstLoginId");
    if ((contactsLoginIds) == null) Lang.ASSERT_NOT_NULL(contactsLoginIds, "contactsLoginIds");
    if ((contactsNicks) == null) Lang.ASSERT_NOT_NULL(contactsNicks, "contactsNicks");
    if ((contactsLoginIds.length) != (contactsNicks.length)) Lang.ASSERT_EQUAL(contactsLoginIds.length, contactsNicks.length, "contactsLoginIds.length", "contactsNicks.length");

    synchronized (messagingNetworkListeners)
    {
      for (int i = 0; i < messagingNetworkListeners.size(); i++)
      {
        MessagingNetworkListener l = (MessagingNetworkListener) messagingNetworkListeners.elementAt(i);
        Session ses = getSession0(dstLoginId);
        if ((ses) == null) Lang.ASSERT_NOT_NULL(ses, "session for " + dstLoginId);
        //synchronized (ses) //commented out because of deadlock bug
        //{
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("MSN FIRES EVENT to core: contactsReceived: src " + srcLoginId + ", dst " + dstLoginId + ", number of contacts "+contactsLoginIds.length+", listener: " + l);
        l.contactsReceived(getNetworkId(), srcLoginId, dstLoginId, contactsLoginIds, contactsNicks);
        //}
      }
    }
  }

  */

  /**
    Unconditionally fires a status change event.
  */
  void fireStatusChanged_Mim_Uncond(String srcLoginId, String dstLoginId, int status_mim,
      int reasonLogger, String reasonMessage, int endUserReasonCode)
      throws MessagingNetworkException
  {
    if (StringUtil.isNullOrEmpty(srcLoginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(srcLoginId, "srcLoginId");
    if (StringUtil.isNullOrEmpty(dstLoginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(dstLoginId, "dstLoginId");
    MLang.EXPECT_IS_MIM_STATUS(status_mim, "status_mim");
    //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("fireStatusChanged_Mim_Uncond", new Exception("dumpStack"));

    Session sess = getSession0(srcLoginId);
    if (sess != null)
    {
      if (status_mim == MessagingNetwork.STATUS_OFFLINE && srcLoginId.equals(dstLoginId))
      {
        sess.shutdown(context, reasonLogger, reasonMessage, endUserReasonCode);
        getResourceManager().removeSession(sess);
      }

      //src can be already logged off; no session
      //synchronized (getSessionLock(srcLoginId)) {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("MSN FIRES EVENT to core: statusChanged: src " + srcLoginId + " dst " + dstLoginId + ", status: "+StatusUtil.translateStatusMimToString(status_mim)+", reasonLogger: "+MessagingNetworkException.getLoggerMessage(reasonLogger)+", endUserMessage: "+StringUtil.toPrintableString(MessagingNetworkException.getEndUserReasonMessage(endUserReasonCode)));
      StatusFilterUtil.fireContactListEntryStatusChange(sess, getNetworkId(), srcLoginId, dstLoginId, status_mim, reasonLogger, reasonMessage, endUserReasonCode, messagingNetworkListeners);
      //}
    }
    else
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("fireSttChg_Uncond: session is null, statusChange to "+StatusUtilMim.translateStatusMimToString(status_mim)+" ignored");
  }

  public int getClientStatus(String srcLoginId)
      throws MessagingNetworkException
  {
    Session session = getSession0(srcLoginId);
    if (session == null)
      return STATUS_OFFLINE;
    synchronized (session)
    {
      return StatusUtil.translateStatusNativeToMim_self(session.getStatus_Native());
    }
  }

  public String getComment()
  {
    return "";
  }

  /** Returns the network name. */
  public String getName()
  {
    return MSN_NETWORK_NAME;
  }

  public byte getNetworkId()
  {
    return MSN_NETWORK_ID;
  }

  final ResourceManager getResourceManager()
  {
    return resourceManager;
  }

  protected ResourceManager makeResourceManagerInstance()
  {
    if (!REQPARAM_RECONNECTOR_USED)
    {
      return new ResourceManager(context);
    }
    else
    {
      return new ResourceManagerReconnecting(context);
    }
  }

  /**
    Can return null, srcLoginId should never be null.
  */
  public MessagingNetworkSession getSession(String loginId)
  {
    return (MessagingNetworkSession) getSession0(loginId);
  }

  private Session getSessionNotNull(String loginId, int endUserOperationErrCode)
      throws MessagingNetworkException
  {
    if (StringUtil.isNullOrEmpty(loginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(loginId, "loginId");
    return getResourceManager().getSessionNotNull(loginId, endUserOperationErrCode);
  }

  public int getStatus(String srcLoginId, String dstLoginId)
      throws MessagingNetworkException
  {
    if (srcLoginId == null || dstLoginId == null)
      throw new AssertException("Unable to get status: invalid (null) method arguments");
    if (srcLoginId.equals(dstLoginId))
      return getClientStatus(srcLoginId);
    Session session = getSession0(srcLoginId);
    if (session == null)
      return STATUS_OFFLINE;
    return StatusUtil.translateStatusNativeToMim_cl_entry(session.getContactStatus_Native(dstLoginId, context));
  }

  public void login(String srcLoginId, String password, java.lang.String[] contactList, int statusMim)
      throws MessagingNetworkException
  {
    MLang.EXPECT_IS_MIM_STATUS(statusMim, "loginStatusMim");
    if (!(  statusMim != MessagingNetwork.STATUS_OFFLINE  )) throw new AssertException("cannot call login(..., STATUS_OFFLINE)");

    //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("login", new Exception("dumpStack"));

    Session session = getResourceManager().createSession(srcLoginId);
    synchronized (session)
    {
      try
      {
        session.login_Native(password, contactList, StatusUtil.translateStatusMimToNative(statusMim), context);
      }
      finally
      {
        try
        {
          if (session.getStatus_Native() == StatusUtil.NATIVE_STATUS_OFFLINE)
            getResourceManager().removeSession(session);
        }
        catch (Exception ex)
        {
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("unexpected exception while login(), exception ignored", ex);
        }
      }
    }
  }

  /*
  public String registerLoginId(String password)
  throws MessagingNetworkException
  {
    if ((password) == null) Lang.ASSERT_NOT_NULL(password, "password");

    Session session = getResourceManager().createAnonymousSession();
    synchronized (session)
    {
      try
      {
        return session.registerLoginId(password, context);
      }
      finally
      {
        try
        {
          session.logout(context, MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, null, MessagingNetworkException.ENDUSER_NO_ERROR);
        }
        catch (Exception ex)
        {
        }
      }
    }
  }
  */

  public void logout(String srcLoginId, int endUserReason)
      throws MessagingNetworkException
  {
    setClientStatus(srcLoginId, MessagingNetwork.STATUS_OFFLINE, endUserReason);
  }

  public void removeFromContactList(String srcLoginId, String dstLoginId)
      throws MessagingNetworkException
  {
    Session session = getSessionNotNull(srcLoginId, MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_REMOVE_FROM_CONTACT_LIST_WHILE_OFFLINE);
    synchronized (session)
    {
      session.removeFromContactList(dstLoginId, context);
    }
  }

  public final void removeMessagingNetworkListener(MessagingNetworkListener l)
  {
    if ((l) == null) Lang.ASSERT_NOT_NULL(l, "listener");
    messagingNetworkListeners.remove(l);
  }

  public void sendMessage(String srcLoginId, String dstLoginId, String text)
      throws MessagingNetworkException
  {
    Session session = getSessionNotNull(srcLoginId, MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_SEND_TEXT_MESSAGE_WHILE_OFFLINE);
    synchronized(session)
    {
      session.sendMessage(dstLoginId, text, context);
    }
  }

  public void sendContacts(String srcLoginId, String dstLoginId, String[] nicks, String[] loginIds)
      throws MessagingNetworkException
  {
    MessagingNetworkException.throwOperationNotSupported("sendContacts");

//    Session session = getSessionNotNull(srcLoginId, MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_SEND_CONTACTS_WHILE_OFFLINE);
//    synchronized(session)
//    {
//      session.sendContacts(dstLoginId, nicks, loginIds, context);
//    }
  }

  public void setClientStatus(String srcLoginId, int status_mim)
      throws MessagingNetworkException
  {
    setClientStatus(srcLoginId, status_mim, MessagingNetworkException.ENDUSER_STATUS_CHANGED_UNDEFINED_REASON);
  }

  public void logout(String srcLoginId)
  throws MessagingNetworkException
  {
    logout(srcLoginId, MessagingNetworkException.ENDUSER_STATUS_CHANGED_UNDEFINED_REASON);
  }

  public void setClientStatus(String srcLoginId, int status_mim, int endUserReason)
  throws MessagingNetworkException
  {
    MLang.EXPECT_IS_MIM_STATUS(status_mim, "loginStatusMim");
    Session session;

    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("[" + srcLoginId + "] setClientStatus to "+StatusUtil.translateStatusMimToString(status_mim));
    //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("setClientStatus", new Exception("dumpStack"));

    if (status_mim == MessagingNetwork.STATUS_OFFLINE)
    {
      session = getSession0(srcLoginId);
      if (session == null)
      {
        if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.WARN)) CAT.warn(LOG_PREFIX+ "[" + srcLoginId + "] logout: session already logged out, request ignored");
        return;
      }
      session.setLastError(
          new MessagingNetworkException(
            "logout requested by caller",
            MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_LOGOUT_CALLER,
            endUserReason)
      );
    }
    else
    {
      session = getSessionNotNull(srcLoginId, MessagingNetworkException.ENDUSER_MIM_BUG_LOGIN_FIRST_CANNOT_PERFORM_SET_STATUS_NONOFFLINE_WHILE_OFFLINE);
    }
    setClientStatus0(session, status_mim, endUserReason);
  }

  private Session getSession0(String loginId)
  {
    if (loginId == null)  throw new AssertException("loginId cannot be null");
    return getResourceManager().getSession(loginId);
  }

  private void setClientStatus0(Session session, int status_mim, int endUserReason)
      throws MessagingNetworkException
  {
    if (status_mim == MessagingNetwork.STATUS_OFFLINE)
    {
      //logoff is not synchronized (almost)
      session.logout(context,
          MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_LOGOUT_CALLER,
          "logout requested by caller",
          endUserReason);

      synchronized (session)
      {
        getResourceManager().removeSession(session);
      }
    }
    else
    {
      //change status to non-offline is synchronized
      synchronized (session)
      {
        session.setStatus_Native_External(StatusUtil.translateStatusMimToNative(status_mim), context);
      }
    }
  }

  public UserDetails getUserDetails(String srcLoginId, String dstLoginId)
      throws MessagingNetworkException
  {
    Session session = getSessionNotNull(srcLoginId, MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_GET_USER_DETAILS_WHILE_OFFLINE);
    synchronized (session)
    {
      return session.getUserDetails(dstLoginId, context);
    }
  }

  public UserSearchResults searchUsers(
      String srcLoginId,
      String emailSearchPattern,
      String nickSearchPattern,
      String firstNameSearchPattern,
      String lastNameSearchPattern)
      throws MessagingNetworkException
  {
    Session session = getSessionNotNull(srcLoginId, MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_SEARCH_USERS_WHILE_OFFLINE);
    synchronized (session)
    {
      return session.searchUsers(
          emailSearchPattern,
          nickSearchPattern,
          firstNameSearchPattern,
          lastNameSearchPattern,
          context);
    }
  }

  /**
    Using the connection of srcLoginId uin,
    retrieves the authorizationRequired property of dstLoginId uin
    from the icq server.
  */
  public boolean isAuthorizationRequired(String srcLoginId, String dstLoginId)
      throws MessagingNetworkException
  {
    //MessagingNetworkException.throwOperationNotSupported("isAuthorizationRequired");
    return false;

//    Session session = getSessionNotNull(srcLoginId, MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_SEARCH_USERS_WHILE_OFFLINE);
//    synchronized (session)
//    {
//      return session.isAuthorizationRequired(
//        dstLoginId,
//        context);
//    }
  }

  /**
    Using the connection of srcLoginId uin,
    sends an auth request to dstLoginId uin with reason reason,
    to ask if dstLoginId allows to add himself
    to srcLoginId's contact list.
    Reason can be null.
  */
  public void authorizationRequest(String srcLoginId, String dstLoginId, String reason)
      throws MessagingNetworkException
  {
    MessagingNetworkException.throwOperationNotSupported("authorizationRequest");
//    Session session = getSessionNotNull(srcLoginId, MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_SEARCH_USERS_WHILE_OFFLINE);
//    synchronized (session)
//    {
//      session.authorizationRequest(
//        dstLoginId, reason,
//        context);
//    }
  }

  /**
    Using the connection of srcLoginId uin,
    sends a reply to preceding auth request of dstLoginId uin,
    to state that srcLoginId grants or denies
    dstLoginId's request to add srcLoginId to dstLoginId's contact list.
  */
  public void authorizationResponse(String srcLoginId, String dstLoginId, boolean grant)
      throws MessagingNetworkException
  {
    MessagingNetworkException.throwOperationNotSupported("authorizationResponse");
//    Session session = getSessionNotNull(srcLoginId, MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_SEARCH_USERS_WHILE_OFFLINE);
//    synchronized (session)
//    {
//      session.authorizationResponse(
//        dstLoginId, grant,
//        context);
//    }
  }

  public TransportChooser getTransportChooser()
  {
    return transportChooser;
  }

  public void init()
  {
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info(getName().toLowerCase()+": init()");
    getResourceManager().init();
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info(getName().toLowerCase()+": init() done");
  }

  public void deinit()
  {
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info(getName().toLowerCase()+": deinit()");
    messagingNetworkListeners.clear();
    getResourceManager().deinit();
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info(getName().toLowerCase()+": deinit() done");
  }

  public static final String LOG_PREFIX = "msn: ";
  
  public long startLogin(String srcLoginId, String password, String[] contactList, int status) throws MessagingNetworkException
  {throw new RuntimeException("TODO: not implemented.");}
  public long startSendMessage(String srcLoginId, String dstLoginId, String text) throws MessagingNetworkException
  {throw new RuntimeException("TODO: not implemented.");}
  public long startGetUserDetails(String srcLoginId, String dstLoginId) throws MessagingNetworkException
  {throw new RuntimeException("TODO: not implemented.");}
}