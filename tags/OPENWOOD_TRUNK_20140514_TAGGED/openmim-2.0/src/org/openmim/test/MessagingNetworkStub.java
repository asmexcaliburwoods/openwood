package org.openmim.test;

import org.openmim.*;
import org.openmim.UserDetailsImpl;
import org.openmim.mn.MessagingNetwork;
import org.openmim.mn.MessagingNetworkListener;
import org.openmim.mn.MessagingNetworkException;
import org.openmim.icq2k.StatusUtil;
import org.openmim.icq2k.IcqUinUtil;
import org.openmim.icq.util.MLang;
import org.openmim.icq.util.joe.*;
import java.util.*;
import java.io.*;
import java.net.*;

public class MessagingNetworkStub implements MessagingNetwork
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(MessagingNetworkStub.class.getName());

  public static int REQPARAM_DELAY_BETWEEN_STATUS_CHANGED_EVENTS_PER_SESSION_MILLIS_MIN;
  public static int REQPARAM_DELAY_BETWEEN_STATUS_CHANGED_EVENTS_PER_SESSION_MILLIS_MAX;

  public static int REQPARAM_DELAY_BETWEEN_MESSAGE_RECEIVED_EVENTS_PER_SESSION_MILLIS_MIN;
  public static int REQPARAM_DELAY_BETWEEN_MESSAGE_RECEIVED_EVENTS_PER_SESSION_MILLIS_MAX;

  public static int REQPARAM_DELAY_BETWEEN_CONTACTS_RECEIVED_EVENTS_PER_SESSION_MILLIS_MIN;
  public static int REQPARAM_DELAY_BETWEEN_CONTACTS_RECEIVED_EVENTS_PER_SESSION_MILLIS_MAX;

  public static int REQPARAM_DELAY_GET_USER_DETAILS_MILLIS_MIN;
  public static int REQPARAM_DELAY_GET_USER_DETAILS_MILLIS_MAX;

  public static int REQPARAM_DELAY_LOGIN_MIN;
  public static int REQPARAM_DELAY_LOGIN_MAX;

  public static int REQPARAM_DELAY_SEND_MESSAGE_MIN;
  public static int REQPARAM_DELAY_SEND_MESSAGE_MAX;

  public static int REQPARAM_DELAY_SEND_CONTACTS_MIN;
  public static int REQPARAM_DELAY_SEND_CONTACTS_MAX;

  // public static int REQPARAM_MESSAGE_RECEIVED_LENGTH_CHARS_MIN;
  // public static int REQPARAM_MESSAGE_RECEIVED_LENGTH_CHARS_MAX;

  public static int REQPARAM_NUMBER_OF_STATUS_CHANGED_EVENTS_PER_SESSION_MAX;
  public static int REQPARAM_NUMBER_OF_MESSAGE_RECEIVED_EVENTS_PER_SESSION_MAX;
  public static int REQPARAM_NUMBER_OF_CONTACTS_RECEIVED_EVENTS_PER_SESSION_MAX;

  private static void checkRange(int min, int max, String prefix)
  {
    if (min < 0)
      throw new RuntimeException(prefix+"_MIN property of "+MessagingNetworkStub.class+" is invalid ("+min+"), must be non-negative.");
    if (max < min)
      throw new RuntimeException(prefix+"_MAX property of "+MessagingNetworkStub.class+" is invalid ("+max+"), must be >= "+prefix+"_MIN ("+min+")");
  }

  static
  {
    AutoConfig.fetchFromClassLocalResourceProperties(MessagingNetworkStub.class, true, false);
    checkRange(
      REQPARAM_DELAY_BETWEEN_STATUS_CHANGED_EVENTS_PER_SESSION_MILLIS_MIN,
      REQPARAM_DELAY_BETWEEN_STATUS_CHANGED_EVENTS_PER_SESSION_MILLIS_MAX,
      "REQPARAM_DELAY_BETWEEN_STATUS_CHANGED_EVENTS_PER_SESSION_MILLIS");
    checkRange(
      REQPARAM_DELAY_BETWEEN_MESSAGE_RECEIVED_EVENTS_PER_SESSION_MILLIS_MIN,
      REQPARAM_DELAY_BETWEEN_MESSAGE_RECEIVED_EVENTS_PER_SESSION_MILLIS_MAX,
      "REQPARAM_DELAY_BETWEEN_MESSAGE_RECEIVED_EVENTS_PER_SESSION_MILLIS");
    checkRange(
      REQPARAM_DELAY_BETWEEN_CONTACTS_RECEIVED_EVENTS_PER_SESSION_MILLIS_MIN,
      REQPARAM_DELAY_BETWEEN_CONTACTS_RECEIVED_EVENTS_PER_SESSION_MILLIS_MAX,
      "REQPARAM_DELAY_BETWEEN_CONTACTS_RECEIVED_EVENTS_PER_SESSION_MILLIS");
    checkRange(
      REQPARAM_DELAY_GET_USER_DETAILS_MILLIS_MIN,
      REQPARAM_DELAY_GET_USER_DETAILS_MILLIS_MAX,
      "REQPARAM_DELAY_GET_USER_DETAILS_MILLIS");
    checkRange(
      REQPARAM_DELAY_LOGIN_MIN,
      REQPARAM_DELAY_LOGIN_MAX,
      "REQPARAM_DELAY_LOGIN");
    checkRange(
      REQPARAM_DELAY_SEND_MESSAGE_MIN,
      REQPARAM_DELAY_SEND_MESSAGE_MAX,
      "REQPARAM_DELAY_SEND_MESSAGE");
    checkRange(
      REQPARAM_DELAY_SEND_CONTACTS_MIN,
      REQPARAM_DELAY_SEND_CONTACTS_MAX,
      "REQPARAM_DELAY_SEND_CONTACTS");
    /*
    checkRange(
      REQPARAM_MESSAGE_RECEIVED_LENGTH_CHARS_MIN,
      REQPARAM_MESSAGE_RECEIVED_LENGTH_CHARS_MAX,
      "REQPARAM_MESSAGE_RECEIVED_LENGTH_CHARS");
    */
  }

  public static final byte STUB_NETWORK_ID = (byte) 1;
  public static final String STUB_NETWORK_NAME = "ICQ";
  private final Vector messagingNetworkListeners = new Vector(1, 1);


  protected final PluginContext context = new PluginContext(this);
  protected final ResourceManager resourceManager = makeResourceManagerInstance();

  public MessagingNetworkStub()
  {
  }

  public boolean isAuthorizationRequired(String srcLoginId, String dstLoginId)
      throws MessagingNetworkException
  {
    throw new MessagingNetworkException("TODO: not implemented", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_NO_ERROR);
  }
  public void authorizationRequest(String srcLoginId, String dstLoginId, String reason)
      throws MessagingNetworkException
  {
    throw new MessagingNetworkException("TODO: not implemented", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_NO_ERROR);
  }
  public void authorizationResponse(String srcLogin, String dstLogin, boolean grant)
      throws MessagingNetworkException
  {
    throw new MessagingNetworkException("TODO: not implemented", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_NO_ERROR);
  }

  public final void addMessagingNetworkListener(MessagingNetworkListener l)
  {
    if ((l) == null) Lang.ASSERT_NOT_NULL(l, "listener");
    synchronized (messagingNetworkListeners)
    {
      messagingNetworkListeners.removeElement(l);
      messagingNetworkListeners.addElement(l);
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
        MessagingNetworkListener l = (MessagingNetworkListener) messagingNetworkListeners.elementAt(i);
        Session ses = getSession0(dstLoginId);
        if ((ses) == null) Lang.ASSERT_NOT_NULL(ses, "session for " + dstLoginId);
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ICQ FIRES EVENT to core: messageReceived: src " + srcLoginId + ", dst " + dstLoginId + ",\r\ntext "+StringUtil.toPrintableString(messageText)+",\r\nlistener: " + l);
        l.messageReceived(getNetworkId(), srcLoginId, dstLoginId, messageText);
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
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ICQ FIRES EVENT to core: contactsReceived: src " + srcLoginId + ", dst " + dstLoginId + ",\r\nnumber of contacts "+contactsLoginIds.length+",\r\nlistener: " + l);
        l.contactsReceived(getNetworkId(), srcLoginId, dstLoginId, contactsLoginIds, contactsNicks);
      }
    }
  }

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
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("fireStatusChanged_Mim_Uncond", new Exception("dumpStack"));

    synchronized (messagingNetworkListeners)
    {
      Session sess = getSession0(srcLoginId);
      if (sess != null)
      {
        if (status_mim == MessagingNetwork.STATUS_OFFLINE && srcLoginId.equals(dstLoginId))
        {
          sess.shutdown(context, reasonLogger, reasonMessage, endUserReasonCode);
          getResourceManager().removeSession(sess);
        }

        for (int i = 0; i < messagingNetworkListeners.size(); i++)
        {
          MessagingNetworkListener l = (MessagingNetworkListener) messagingNetworkListeners.elementAt(i);
          //src can be already logged off; no session
          //synchronized (getSessionLock(srcLoginId)) {
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ICQ FIRES EVENT to core: statusChanged: src " + srcLoginId + " dst " + dstLoginId + ", status: "+StatusUtil.translateStatusMimToString(status_mim)+",\r\nreasonLogger: "+MessagingNetworkException.getLoggerMessage(reasonLogger)+",\r\nendUserMessage: "+StringUtil.toPrintableString(MessagingNetworkException.getEndUserReasonMessage(endUserReasonCode))+",\r\n listener: " + l);
          l.statusChanged(getNetworkId(), srcLoginId, dstLoginId, status_mim, reasonLogger, reasonMessage, endUserReasonCode);
          //}
        }
      }
      else
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("fireSttChg_Uncond: session is null, statusChange to "+StatusUtilMim.translateStatusMimToString(status_mim)+" ignored");
    }
  }
  final ResourceManager getResourceManager()
  {
    return resourceManager;
  }
  protected ResourceManager makeResourceManagerInstance()
  {
    return new ResourceManager(context);
  }
  public int getClientStatus(String srcLoginId)
  throws MessagingNetworkException
  {
      Session session = getSession0(srcLoginId);
      if (session == null)
          return STATUS_OFFLINE;
      synchronized (session)
      {
          return StatusUtil.translateStatusOscarToMim_self(session.getStatus_Oscar());
      }
  }
  public String getComment() {
    return "";
  }
  public String getName()
  {
      return STUB_NETWORK_NAME;
  }
  public byte getNetworkId()
  {
      return STUB_NETWORK_ID;
  }
  /**
    Can return null, srcLoginId should never be null.
  */
  public Session getSession(String loginId)
  {
    return getSession0(loginId);
  }

  private Session getSession0(String loginId)
  {
    if (StringUtil.isNullOrEmpty(loginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(loginId, "loginId");
    return getResourceManager().getSession(loginId);
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
    return StatusUtil.translateStatusOscarToMim_cl_entry(session.getContactStatus_Oscar(dstLoginId, context));
  }

  public void login(String srcLoginId, String password, java.lang.String[] contactList, int statusMim)
  throws MessagingNetworkException
  {
    MLang.EXPECT_IS_MIM_STATUS(statusMim, "loginStatusMim");
    if (!(  statusMim != MessagingNetwork.STATUS_OFFLINE  )) throw new AssertException("cannot call login(..., STATUS_OFFLINE)");

    Session session = getResourceManager().createSession(srcLoginId);
    synchronized (session)
    {
      try
      {
        session.login_Oscar(password, contactList, StatusUtil.translateStatusMimToOscar(statusMim), context);
      }
      finally
      {
        try
        {
          if (session.getStatus_Oscar() == StatusUtil.OSCAR_STATUS_OFFLINE)
            getResourceManager().removeSession(session);
        }
        catch (Exception ex)
        {
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("unexpected exception while login(), exception ignored", ex);
        }
      }
    }
  }

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
        messagingNetworkListeners.removeElement(l);
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
    Session session = getSessionNotNull(srcLoginId, MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_SEND_CONTACTS_WHILE_OFFLINE);
    synchronized(session)
    {
      session.sendContacts(dstLoginId, nicks, loginIds, context);
    }
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
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("setClientStatus", new Exception("dumpStack"));

    if (status_mim == MessagingNetwork.STATUS_OFFLINE)
    {
      session = getSession0(srcLoginId);
      if (session == null)
      {
        if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.WARN)) CAT.warn("icq2k [" + srcLoginId + "] logout: session already logged out, request ignored");
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
        session.setStatus_Oscar_External(StatusUtil.translateStatusMimToOscar(status_mim), context);
      }
    }
  }

  public UserDetails getUserDetails(String srcLoginId, String dstLoginId)
  throws MessagingNetworkException
  {
    Session session = getSessionNotNull(srcLoginId, MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_GET_USER_DETAILS_WHILE_OFFLINE);
    synchronized (session)
    {
      return session.fetchUserDetails(dstLoginId, context);
    }
  }

  /**
    If null is returned, then no users found.
    @see UserSearchResults
  */
  public UserSearchResults searchUsers(
    String srcLoginId,
    String emailSearchPattern,
    String nickSearchPattern,
    String firstNameSearchPattern,
    String lastNameSearchPattern) throws MessagingNetworkException
  {
    throw new MessagingNetworkException("TODO: not implemented", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_NO_ERROR);
  }


  public long startLogin(String srcLoginId, String password, String[] contactList, int status) throws MessagingNetworkException
  {throw new RuntimeException("TODO: not implemented.");}
  public long startSendMessage(String srcLoginId, String dstLoginId, String text) throws MessagingNetworkException
  {throw new RuntimeException("TODO: not implemented.");}
  public long startGetUserDetails(String srcLoginId, String dstLoginId) throws MessagingNetworkException
  {throw new RuntimeException("TODO: not implemented.");}


  public void init()
  {
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("icq2k: init()");
    getResourceManager().init();
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("icq2k: init() done");
  }
  public void deinit()
  {
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("icq2k: deinit()");
    messagingNetworkListeners.clear();
    getResourceManager().deinit();
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("icq2k: deinit() done");
  }
}







class ResourceManager
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(ResourceManager.class.getName());
  private final Hashtable loginId2session = new java.util.Hashtable(25, 25);
  private PluginContext context;

  ResourceManager(PluginContext ctx)
  {
    this.context = ctx;
  }

  final Session createSession(String srcLoginId) throws MessagingNetworkException
  {
    Session session = getSession(srcLoginId);
    MLang.EXPECT(
      session == null, "Second login request for " + srcLoginId + " ignored.",
      MessagingNetworkException.CATEGORY_STILL_CONNECTED,
      MessagingNetworkException.ENDUSER_SECOND_LOGIN_REQUEST_IGNORED);

    synchronized (loginId2session)
    {
      session = makeSessionInstance(srcLoginId);
      loginId2session.put(srcLoginId, session);
      Thread sessionThread = new ServeSessionsThread(session, context);
      sessionThread.start();
      return session;
    }
  }

  /** Session factory */
  protected Session makeSessionInstance(String srcLoginId) throws MessagingNetworkException
  {
    return new Session(srcLoginId);
  }

  Session getSession(String loginId)
  {
    if (StringUtil.isNullOrEmpty(loginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(loginId, "loginId");
    return (Session) loginId2session.get(loginId);
  }


  Session getSessionNotNull(String srcLoginId, int endUserOperationErrorCode) throws MessagingNetworkException
  {
    Session session = getSession(srcLoginId);
    MLang.EXPECT(
      session != null, "Please login first. Cannot perform this operation while " + srcLoginId + " is logged out.",
      MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
      endUserOperationErrorCode);
    return session;
  }


  Enumeration getSessions()
  {
    synchronized (loginId2session)
    {
      return ((Hashtable) loginId2session.clone()).elements();
    }
  }


  private void notifyThread()
  {
  }


  void notifyToHandleData()
  {
  }


  void removeSession(Session session)
  {
    if ((session.getStatus_Oscar()) != (StatusUtil.OSCAR_STATUS_OFFLINE)) Lang.ASSERT_EQUAL(session.getStatus_Oscar(), StatusUtil.OSCAR_STATUS_OFFLINE, "status for '" + session.getLoginId() + "'", "StatusUtil.OSCAR_STATUS_OFFLINE");
    synchronized (loginId2session)
    {
      loginId2session.remove(session.getLoginId());
      notifyThread();
    }
  }


  void waitForSessionListChange(long timeoutMillis) throws InterruptedException
  {
    synchronized (loginId2session)
    {
      loginId2session.wait(timeoutMillis);
    }
  }


  public void init()
  {
    synchronized (loginId2session)
    {
      if (loginId2session.size() != 0)
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("BUGGGGG", new AssertException("loginId2session must be empty after deinit()"));
      loginId2session.clear(); //to be more safe.
    }
  }
  public void deinit()
  {
    synchronized (loginId2session)
    {
      Enumeration e = loginId2session.elements();
      while (e.hasMoreElements())
      {
        Session ses = (Session) e.nextElement();
        try
        {
          ses.setLastError(
            new MessagingNetworkException(
              "mim server restarts",
              MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN,
              MessagingNetworkException.ENDUSER_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN));
          ses.logout(context,
            MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN,
            "mim server restarts",
            MessagingNetworkException.ENDUSER_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN);
        }
        catch (Exception ex)
        {
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ex during icq2k.deinit(), ignored", ex);
        }
      }
      if (loginId2session.size() != 0)
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("BUGGGGG", new AssertException("loginId2session must be empty after deinit()"));
      loginId2session.clear(); //to be more safe.
    }
  }
}


class PluginContext
{
  private final MessagingNetworkStub plugin;
  public PluginContext(MessagingNetworkStub plugin)
  {
    if ((plugin) == null) org.openmim.icq.util.joe.Lang.ASSERT_NOT_NULL(plugin, "plugin");
    this.plugin = plugin;
  }

  public final MessagingNetworkStub getICQ2KMessagingNetwork()
  {
    return plugin;
  }
  public final ResourceManager getResourceManager()
  {
    return plugin.getResourceManager();
  }
}





class Session
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(Session.class.getName());

  public static String getVersionString()
  {
    String rev = "$Revision: 1.12 $";
    rev = rev.substring("$Revision: ".length(), rev.length() - 2);

    String cvsTag = "$Name:  $";
    cvsTag = cvsTag.substring("$Name: ".length(), cvsTag.length() - 2);

    if (cvsTag.length() > 0) rev += ", cvs tag "+cvsTag;

    return rev;
  }

  int statusEventsLeft = MessagingNetworkStub.REQPARAM_NUMBER_OF_STATUS_CHANGED_EVENTS_PER_SESSION_MAX;
  int messageEventsLeft = MessagingNetworkStub.REQPARAM_NUMBER_OF_MESSAGE_RECEIVED_EVENTS_PER_SESSION_MAX;
  int contactsEventsLeft = MessagingNetworkStub.REQPARAM_NUMBER_OF_CONTACTS_RECEIVED_EVENTS_PER_SESSION_MAX;

  private MessagingNetworkException lastError = null;

  /**
    a value of 0 => no sleep() call at all.
  */
  private static Random rnd = new Random();

  static void sleep(int minMillis, int maxMillis) throws InterruptedException
  {
    int sleepTime = minMillis + rnd.nextInt(maxMillis - minMillis+1);
    if (sleepTime > 0)
      Thread.currentThread().sleep(sleepTime);
  }

  /**
    When running, Session.tick() method is called periodically
    by the dispatch thread.
    When not running, Session.tick() method is never called.
    <p>
    @see #isRunning()
    */
  private boolean running = false;

  /**
    Session's login id (icq number).
    */
  private final String loginId;

  /**
    ICQ number.
    */
  private final int uin;

  private boolean shuttingDown = false;
  private final Object shuttingDownLock = new Object();

  /**
    Session's contact list items and their current status.
  */
  private Hashtable contactListUinInt2cli;

  public String getRandomContactListEntry()
  {
    Hashtable o = contactListUinInt2cli;
    if (o == null) return null;
    synchronized (o)
    {
      int r = (int) (Math.random() * o.size());
      if (r < 0 || r > o.size() - 1)
        r = 0;
      //if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("cl size = "+contactListUinInt2cli.size()+", r="+r);
      Enumeration e = o.keys();
      while (e.hasMoreElements())
      {
        Integer uin = (Integer) e.nextElement();
        if (r <= 0)
          return uin.toString();
        r--;
      }
      return null;
    }
  }

  /**
    Session's current status.
  */
  private int status_Oscar = StatusUtil.OSCAR_STATUS_OFFLINE;

  public Session(String loginId)
  throws MessagingNetworkException
  {
    super();
    log("new Session");
    uin = IcqUinUtil.parseUin(loginId, "loginId", MessagingNetworkException.CATEGORY_NOT_CATEGORIZED);
    this.loginId = loginId;
  }

  public void addToContactList(String dstLoginId, PluginContext ctx)
  throws MessagingNetworkException
  {
    log("addToContactList()");
    try
    {
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_ADD_TO_CONTACT_LIST_WHILE_OFFLINE);
      //icq2k uin can be 10000...2147483646
      //Integer.MAX_VALUE==2147483647
      //2147483646
      //11111111111
      int dstUin = IcqUinUtil.parseUin(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
      if (this.uin == dstUin)
        throw new MessagingNetworkException(
          "cannot add yourself to a contact list",
          MessagingNetworkException.CATEGORY_STILL_CONNECTED,
          MessagingNetworkException.ENDUSER_CANNOT_ADD_YOURSELF_TO_CONTACT_LIST);
      Integer dstUin_ = new Integer(dstUin);
      if (getContactListItem(dstUin_) != null)
      {
        log(dstLoginId + " already in a contact list, ignored");
        return;
      }
      contactListUinInt2cli.put(dstUin_, makeContactListItem(dstLoginId));
    }
    catch (Exception ex)
    {
      handleException(ex, "addToContactList", ctx);
    }
  }

  private ContactListItem makeContactListItem(String dstLoginId)
  {
    return new ContactListItem(this, dstLoginId);
  }

  public ContactListItem getContactListItem(String dstLoginId)
  {
    return getContactListItem(new Integer(dstLoginId));
  }

  private ContactListItem getContactListItem(Integer dstLoginId)
  {
    return (ContactListItem) contactListUinInt2cli.get(dstLoginId);
  }

  private int getContactListItemStatus(Integer dstLoginId)
  {
    ContactListItem cli = getContactListItem(dstLoginId);
    if (cli == null) return StatusUtil.OSCAR_STATUS_OFFLINE;
    else return cli.getStatusOscar();
  }

  public Enumeration getContactListItems()
  {
    return contactListUinInt2cli.elements();
  }


  private void ASSERT_LOGGED_IN(int endUserOperationErrorCode) throws MessagingNetworkException
  {
    MLang.EXPECT(
      status_Oscar != StatusUtil.OSCAR_STATUS_OFFLINE,
      "Please login first.  Status cannot be offline to perform this operation.",
      MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
      endUserOperationErrorCode);
  }

  public final void handleException(Throwable tr, String processName, PluginContext ctx)
  throws MessagingNetworkException
  {
    if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("error while "+processName, tr);
    MessagingNetworkException ex;

    if (tr instanceof MessagingNetworkException)
    {
      ex = (MessagingNetworkException) tr;
    }
    else
    if (tr instanceof UnknownHostException)
      ex = new MessagingNetworkException(
        "DNS error resolving "+tr.getMessage(),
        MessagingNetworkException.CATEGORY_LOGGED_OFF_DUE_TO_NETWORK_ERROR,
        MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_NETWORK_ERROR);
    else
    if (tr instanceof InterruptedIOException)
      ex = new MessagingNetworkException(
        "mim admin restarts the mim server.",
        MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN,
        MessagingNetworkException.ENDUSER_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN);
    else
    if (tr instanceof InterruptedException)
      ex = new MessagingNetworkException(
        "mim admin restarts the mim server.",
        MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN,
        MessagingNetworkException.ENDUSER_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN);
    else
    if (tr instanceof IOException)
      ex = new MessagingNetworkException(
        "I/O error: "+tr.getMessage(),
        MessagingNetworkException.CATEGORY_LOGGED_OFF_DUE_TO_NETWORK_ERROR,
        MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_NETWORK_ERROR);
    else
    if (tr instanceof AssertException)
      ex = new MessagingNetworkException(
        "bug found: "+tr.getMessage(),
        MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
        MessagingNetworkException.ENDUSER_MIM_BUG);
    else
      ex = new MessagingNetworkException(
        "unknown error: "+tr.getMessage(),
        MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
        MessagingNetworkException.ENDUSER_MIM_BUG_UNKNOWN_ERROR);

    if (ex.getLogger() != MessagingNetworkException.CATEGORY_STILL_CONNECTED)
    {
      setLastError(ex);
      shutdown(ctx, lastError.getLogger(), lastError.getMessage(), lastError.getEndUserReasonCode());
    }

    throw new MessagingNetworkException(
      "Error while " + processName + ": " + ex.getMessage(),
      ex.getLogger(),
      ex.getEndUserReasonCode());
  }

  private final Object lastErrorLock = new Object();

  void setLastError(MessagingNetworkException newEx)
  {
    synchronized (lastErrorLock)
    {
      if (lastError == null)
        lastError = newEx;
    }
  }

  void setLastError(String exceptionMessage, int reasonLogger, int endUserReasonCode)
  {
    synchronized (lastErrorLock)
    {
      if (lastError == null)
        lastError = new MessagingNetworkException(exceptionMessage, reasonLogger, endUserReasonCode);
    }
  }

  void throwLastErrorOrCreateThrowLastError(String exceptionMessage, int reasonLogger, int endUserReasonCode)
  throws MessagingNetworkException
  {
    synchronized (lastErrorLock)
    {
      if (lastError == null)
        lastError = new MessagingNetworkException(exceptionMessage, reasonLogger, endUserReasonCode);
      throw new MessagingNetworkException(lastError.getMessage(), lastError.getLogger(), lastError.getEndUserReasonCode());
    }
  }

  void throwLastError()
  throws MessagingNetworkException
  {
    synchronized (lastErrorLock)
    {
      if ((lastError) == null) Lang.ASSERT_NOT_NULL(lastError, "lastError");
      throw new MessagingNetworkException(lastError.getMessage(), lastError.getLogger(), lastError.getEndUserReasonCode());
    }
  }

  private void fireSystemNotice(String errorMessage, PluginContext context)
  {
    log("fireSystemNotice: " + errorMessage);
    try
    {
      context.getICQ2KMessagingNetwork().fireMessageReceived("0", loginId, errorMessage);
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("error while firing system notice to messaging network listeners", ex);
    }
  }

  private static boolean isSenderValid(String uin, String infoToBeIgnored)
  {
    try
    {
      IcqUinUtil.parseUin(uin, "sender uin", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
      return true;
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("invalid sender uin, "+infoToBeIgnored+" ignored", ex);
      return false;
    }
  }

  private void fireMessageReceived(String senderLoginId, String msg, PluginContext ctx)
  {
    if (isSenderValid(senderLoginId, "incoming message="+StringUtil.toPrintableString(msg)))
      ctx.getICQ2KMessagingNetwork().fireMessageReceived(senderLoginId, loginId, msg);
  }

  protected static void checkInterrupted() throws InterruptedException
  {
    if (Thread.currentThread().isInterrupted())
      throw new InterruptedException();
  }

  public int getContactStatus_Oscar(String dstLoginId, PluginContext ctx) throws MessagingNetworkException
  {
    log("getContactStatus_Oscar");
    try
    {
      int dstUin = IcqUinUtil.parseUin(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
      Integer dstUin_ = new Integer(dstUin);
      return getContactListItemStatus(dstUin_);
    }
    catch (Exception ex)
    {
      handleException(ex, "getContactStatus", ctx);
      return StatusUtil.OSCAR_STATUS_OFFLINE;
    }
  }
  public final java.lang.String getLoginId() {
    return loginId;
  }
  public int getStatus_Oscar()
  {
    return status_Oscar;
  }
  private Object logoutLock = new Object();
  public boolean isRunning()
  {
    synchronized (logoutLock)
    {
      return running;
    }
  }
  protected void log(String s)
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("[" + loginId + "]: " + s);
  }

  public void login_Oscar(final String password, String[] contactList, int status_Oscar, PluginContext ctx)
  throws MessagingNetworkException
  {
    log("login() start");
    final String ERRMSG_PREFIX = "Error logging in: ";
    try
    {
      synchronized (shuttingDownLock)
      {
        shuttingDown = false;
      }
      synchronized (lastErrorLock)
      {
        lastError = null;
      }
      MLang.EXPECT_NOT_NULL_NOR_EMPTY(
        password, "password",
        MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
        MessagingNetworkException.ENDUSER_MIM_BUG_PASSWORD_CANNOT_BE_NULL_NOR_EMPTY);
      MLang.EXPECT(
        password.length() < 256, "password.length() must be < 256, but it is "+password.length(),
        MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
        MessagingNetworkException.ENDUSER_PASSWORD_IS_TOO_LONG);

      if (!(  status_Oscar != StatusUtil.OSCAR_STATUS_OFFLINE  )) throw new AssertException("StatusUtil.OSCAR_STATUS_OFFLINE should never happen as a login_Oscar() argument.");

      if (contactList == null)
      {
        contactList = new String[] {};
      }

      if (contactListUinInt2cli == null)
      {
        contactListUinInt2cli = new Hashtable(contactList.length);
        for (int i = 0; i < contactList.length; i++)
        {
          String dstLoginId = contactList[i];
          if (dstLoginId == null)
          {
            if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("exception ignored, cl entry ignored", new Exception("contactList["+i+"] is null"));
            continue;
          }
          int dstUin = -1;
          try
          {
            dstUin = IcqUinUtil.parseUin(dstLoginId, "contactList[" + i + "]", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
          }
          catch (Exception ex11)
          {
            if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("invalid contact list entry uin ignored, exception ignored", ex11);
            continue;
          }
          if (this.uin == dstUin)
            throw new MessagingNetworkException(
              "cannot login with your own uin in a contact list",
              MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
              MessagingNetworkException.ENDUSER_CANNOT_LOGIN_WITH_YOURSELF_ON_CONTACT_LIST);
          Integer dstUin_ = new Integer(dstUin);
          if (contactListUinInt2cli.put(dstUin_, makeContactListItem(dstLoginId)) != null)
          {
            if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("" + dstLoginId + " already in a contact list, cl entry ignored");
            continue;
          }
        }
        setReconnectData(status_Oscar, contactListUinInt2cli);
      }
      synchronized (logoutLock)
      {
        if ((loginId) == null) Lang.ASSERT_NOT_NULL(loginId, "loginId");
        if ((password) == null) Lang.ASSERT_NOT_NULL(password, "password");
      }

      sleep(MessagingNetworkStub.REQPARAM_DELAY_LOGIN_MIN, MessagingNetworkStub.REQPARAM_DELAY_LOGIN_MAX);

      if (status_Oscar == StatusUtil.OSCAR_STATUS_ONLINE)
        setStatus_Oscar_Internal(status_Oscar, false, ctx, MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, null, MessagingNetworkException.ENDUSER_NO_ERROR);
      else
        setStatus_Oscar_Internal(status_Oscar, true, ctx, MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, null, MessagingNetworkException.ENDUSER_NO_ERROR);

      setRunning(true);
      log("login() finished (success)");
    }
    catch (Exception ex)
    {
      log("login() finished (failed)");
      handleException(ex, "login", ctx);
    }
  }

  private void logInfo(String s)
  {
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("[" + loginId + "]: " + s);
  }

  private final static byte[] xor_table = {//
    (byte) 0xF3, (byte) 0x26, (byte) 0x81, (byte) 0xC4, //
    (byte) 0x39, (byte) 0x86, (byte) 0xDB, (byte) 0x92, //
    (byte) 0x71, (byte) 0xA3, (byte) 0xB9, (byte) 0xE6, //
    (byte) 0x53, (byte) 0x7A, (byte) 0x95, (byte) 0x7C  //
  };

  private void checkShuttingdown() throws MessagingNetworkException, InterruptedException
  {
    checkInterrupted();

    synchronized (shuttingDownLock)
    {
      if (shuttingDown)
        throwLastError();
    }
  }

  void logout(PluginContext ctx, int reasonLogger, String reasonMessage, int endUserReasonCode)
  throws MessagingNetworkException
  {
    log("logout: "+reasonMessage);
    //ASSERT_LOGGED_IN();
    shutdown(ctx, reasonLogger, reasonMessage, endUserReasonCode);
  }

  private static String[] EMPTY_STRING_ARRAY = new String[] {};

  public void removeFromContactList(String dstLoginId, PluginContext ctx)
  throws MessagingNetworkException
  {
    log("removeFromContactList()");
    try
    {
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_REMOVE_FROM_CONTACT_LIST_WHILE_OFFLINE);
      int dstUin = IcqUinUtil.parseUin(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
      Integer dstUin_ = new Integer(dstUin);
      if (contactListUinInt2cli.remove(dstUin_) == null)
      {
        log(dstLoginId + " is not in a contact list, ignored");
        return;
      }
    }
    catch (Exception ex)
    {
      handleException(ex, "removeFromContactList", ctx);
    }
  }

  public void sendMessage(String dstLoginId, String text, PluginContext ctx)
  throws MessagingNetworkException
  {
    log("sendMessage() start, dst="+dstLoginId+",\r\ntext="+StringUtil.toPrintableString(text));
    try
    {
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_SEND_TEXT_MESSAGE_WHILE_OFFLINE);
      int dstUin = IcqUinUtil.parseUin(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
      if ((text) == null) Lang.ASSERT_NOT_NULL(text, "text");
      sleep(MessagingNetworkStub.REQPARAM_DELAY_SEND_MESSAGE_MIN, MessagingNetworkStub.REQPARAM_DELAY_SEND_MESSAGE_MAX);
      log("sendMessage() finished (success)");
    }
    catch (Exception ex)
    {
      log("sendMessage() finished (failed)");
      handleException(ex, "sendMessage", ctx);
    }
  }

  public void sendContacts(String dstLoginId, String[] nicks, String[] loginIds, PluginContext ctx)
  throws MessagingNetworkException
  {
    log("sendContacts() start");
    try
    {
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_SEND_CONTACTS_WHILE_OFFLINE);
      int dstUin = IcqUinUtil.parseUin(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
      if ((nicks) == null) Lang.ASSERT_NOT_NULL(nicks, "nicks");
      if ((loginIds) == null) Lang.ASSERT_NOT_NULL(loginIds, "loginIds");
      sleep(MessagingNetworkStub.REQPARAM_DELAY_SEND_CONTACTS_MIN, MessagingNetworkStub.REQPARAM_DELAY_SEND_CONTACTS_MAX);
      log("sendContacts() finished (success)");
    }
    catch (Exception ex)
    {
      log("sendContacts() finished (failed)");
      handleException(ex, "sendContacts", ctx);
    }
  }

  public void setContactStatus_Oscar(String dstLoginId, int newStatus_Oscar, PluginContext ctx,
    int reasonLogger, String reasonMessage)
  throws MessagingNetworkException
  {
    log("setContactStatus_Oscar");
    try
    {
      if (!(  !this.loginId.equals(dstLoginId)  )) throw new AssertException("this.loginId.equals(dstLoginId) must be false here");
      int dstUin = IcqUinUtil.parseUin(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
      Integer dstUin_ = new Integer(dstUin);
      ContactListItem cli = getContactListItem(dstUin_);
      if (cli == null)
      {
        log("Session.setContactStatus_Oscar(): '" + dstLoginId + "' is not on contact list, statusOscar change ignored");
        return;
      }
      int oldStatus_Oscar = cli.getStatusOscar();
      cli.setStatusOscar(newStatus_Oscar); //marks it as NOT obsolete
      if (oldStatus_Oscar == newStatus_Oscar)
        return;
      //ignore any status events if this Session is already logged out
      if (this.status_Oscar != StatusUtil.OSCAR_STATUS_OFFLINE)
      {
        int oldStatus_Mim = StatusUtil.translateStatusOscarToMim_cl_entry(oldStatus_Oscar);
        int newStatus_Mim = StatusUtil.translateStatusOscarToMim_cl_entry(newStatus_Oscar);
        if (oldStatus_Mim != newStatus_Mim)
        {
          fireContactListEntryStatusChangeMim_Uncond(dstLoginId, newStatus_Mim, ctx, reasonLogger, reasonMessage);
        }
        else
          log("setStatus request ignored by icq2k plugin: attempted to set the same status");
      }
      else
        log("setStatus request ignored by icq2k plugin: we are offline, hence silent");
    }
    catch (Exception ex)
    {
      handleException(ex, "setContactStatus", ctx);
    }
  }

  protected void fireContactListEntryStatusChangeMim_Uncond(String dstLoginId, int newStatus_Mim, PluginContext ctx,
    int reasonLogger, String reasonMessage)
  throws MessagingNetworkException
  {
    ctx.getICQ2KMessagingNetwork().fireStatusChanged_Mim_Uncond(this.loginId, dstLoginId, newStatus_Mim,
      reasonLogger, reasonMessage, MessagingNetworkException.ENDUSER_NO_ERROR);
  }

  protected void fireSessionStatusChangeMim_Uncond(int newStatus_Mim, PluginContext ctx,
    int reasonLogger, String reasonMessage, int endUserReasonCode)
  throws MessagingNetworkException
  {
    ctx.getICQ2KMessagingNetwork().fireStatusChanged_Mim_Uncond(this.loginId, this.loginId, newStatus_Mim,
      reasonLogger, reasonMessage, endUserReasonCode);
  }

  private void setRunning(boolean newRunning)
  {
    synchronized (logoutLock)
    {
      running = newRunning;
    }
  }
  /**
    To be called from plugin user classes via ICQ2KMessagingNetwork;
    should never be called from other plugin classes.
    <p>
    Only works if already logged in, otherwise throws an AssertException.
    <p>
    Does not allow setting an OFFLINE status, since
    ICQ2KMessagingNetwork.logout() should be called instead.
  */
  public void setStatus_Oscar_External(int newStatus, PluginContext ctx)
  throws MessagingNetworkException
  {
    log("setStatus_Oscar_External to " + StatusUtil.translateStatusOscarToString(newStatus) + //
      " (current status: " + StatusUtil.translateStatusOscarToString(this.status_Oscar) + ")");
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("setStatus_Oscar_External", new Exception("dumpStack"));
    try
    {
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_MIM_BUG_LOGIN_FIRST_CANNOT_PERFORM_SET_STATUS_NONOFFLINE_WHILE_OFFLINE);
      if (!(  newStatus != StatusUtil.OSCAR_STATUS_OFFLINE  )) throw new AssertException("OSCAR_STATUS_OFFLINE should never happen here; use Session.logout() instead.");
      setStatus_Oscar_Internal(newStatus, true, ctx, MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, null, MessagingNetworkException.ENDUSER_NO_ERROR);
    }
    catch (Exception ex)
    {
      handleException(ex, "setStatus_Oscar_External", ctx);
    }
  }

  /** Status of the session itself */
  private void setStatus_Oscar_Internal(final int newStatus_Oscar, boolean sendToICQServer, PluginContext ctx,
    int reasonLogger, String reasonMessage, int endUserReasonCode)
  throws MessagingNetworkException
  {
    try
    {
      if (sendToICQServer)
        StatusUtil.EXPECT_IS_OSCAR_STATUS(newStatus_Oscar);
      int oldStatus_Mim;
      int newStatus_Mim;
      synchronized (logoutLock)
      {
        final int oldStatus_Oscar = this.status_Oscar;
        if (oldStatus_Oscar == newStatus_Oscar)
          return;
        this.status_Oscar = newStatus_Oscar;
        oldStatus_Mim = StatusUtil.translateStatusOscarToMim_self(oldStatus_Oscar);
        newStatus_Mim = StatusUtil.translateStatusOscarToMim_self(newStatus_Oscar);
        if (oldStatus_Mim != newStatus_Mim)
        {
          if (oldStatus_Oscar != StatusUtil.OSCAR_STATUS_OFFLINE)
            setReconnectData(oldStatus_Oscar, contactListUinInt2cli);
          if (newStatus_Oscar == StatusUtil.OSCAR_STATUS_OFFLINE)
          {
            //contactListUinInt2cli = null;
            synchronized (lastErrorLock)
            {
              if (lastError == null)
              {
                setLastError(new MessagingNetworkException(reasonMessage, reasonLogger, endUserReasonCode));
              }
            }
          }
          fireSessionStatusChangeMim_Uncond(newStatus_Mim, ctx, reasonLogger, reasonMessage, endUserReasonCode);
        }
        else
          log("setStatus request ignored by icq2k plugin: attempted to set the same status");
      }
    }
    catch (Exception ex)
    {
      handleException(ex, "setStatus0_Oscar", ctx);
    }
  }
  /**
    Shuts the session and everything down and throws no exceptions.
  */
  void shutdown(PluginContext ctx, int reasonLogger, String reasonMessage, int endUserReasonCode)
  {
    synchronized (shuttingDownLock)
    {
      if (shuttingDown)
        return;
      setLastError(
        "shutting down ["+(reasonMessage == null ? "no reason given" : "reason: " + reasonMessage) + "]",
        reasonLogger,
        endUserReasonCode);
      shuttingDown = true;
    }
    log("shutdown("+reasonMessage+")");
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("shutdown", new Exception("dumpStack"));
    try
    {
      setRunning(false);
      setStatus_Oscar_Internal(StatusUtil.OSCAR_STATUS_OFFLINE, false, ctx, reasonLogger, reasonMessage, endUserReasonCode);
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ex in shutdown(), ignored", ex);
    }
  }
  void sleep(String activityName, long millis)
  throws MessagingNetworkException, InterruptedException
  {
    log(activityName + ": sleeping " + ((millis / 100) / (float) 10) + " sec.");
    synchronized (logoutLock)
    {
      logoutLock.wait(millis);
    }
    checkShuttingdown();
  }

  public boolean tick(PluginContext ctx)
  {
    //log("tick()");
    try
    {
      checkShuttingdown();

      return true;
    }
    catch (Exception ex)
    {
      try
      {
        isTickRunningException = this.running;
        handleException(ex, "tick()", ctx);
        isTickRunningException = false;
      }
      catch (Exception exx)
      {
      }
      return status_Oscar != StatusUtil.OSCAR_STATUS_OFFLINE;
    }
  }

  protected boolean isTickRunningException = false;

  public UserDetailsImpl fetchUserDetails(String dstLoginId, PluginContext ctx)
  throws MessagingNetworkException
  {
    log("fetchUserDetails() start, dst "+dstLoginId);
    try
    {
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_GET_USER_DETAILS_WHILE_OFFLINE);

      sleep(MessagingNetworkStub.REQPARAM_DELAY_GET_USER_DETAILS_MILLIS_MIN, MessagingNetworkStub.REQPARAM_DELAY_GET_USER_DETAILS_MILLIS_MAX);

      return new UserDetailsImpl(
        "nick",
        "realname",
        "email@dot.com",
        "homeCity",
        "homeState",
        "+1 (123) 12345-123", //"homePhone",
        "+1 (123) 12345", //"homeFax",
        "homeStreet",
        "+1 (123) 111111", //cell phone
        true, //cell ph is SMS enabled
        "12345", //homeZipcode
        false);

    }
    catch (Exception ex)
    {
      handleException(ex, "fetchUserDetails", ctx);
      //handleException always throws some exception
      throw new AssertException("this point should never be reached");
    }
  }

  protected void setReconnectData(int prevStatus_Oscar, Hashtable prevUinInt2cli)
  {
  }

  protected void clearReconnectState()
  {
    contactListUinInt2cli = null;
  }
}


class ContactListItem
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(ContactListItem.class.getName());
  private final Session session;
  private final String dstLoginId;

  public ContactListItem(Session session, String dstLoginId)
  {
    if ((session) == null) Lang.ASSERT_NOT_NULL(session, "session");
    this.session = session;
    if ((dstLoginId) == null) Lang.ASSERT_NOT_NULL(dstLoginId, "dstLoginId");
    this.dstLoginId = dstLoginId;
  }

  public String getDstLoginId()
  {
    return dstLoginId;
  }

  private int statusOscar = StatusUtil.OSCAR_STATUS_OFFLINE;

  public int getStatusOscar()
  { return statusOscar; }

  public void setStatusOscar(int status)
  {
    statusOscar = status;
  }
}


class ServeSessionsThread extends Thread
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(ServeSessionsThread.class.getName());
  private PluginContext context;
  private final Session ses;
  private static int count = 1;

  public ServeSessionsThread(Session ses, PluginContext ctx)
  {
    super("st"+ses.getLoginId());
    this.context = ctx;
    this.ses=ses;
  }

  private static Random rnd = new Random();

  private static int random(int min, int max)
  {
    int a = min + rnd.nextInt(max - min + 1);
    if (a < min) return min;
    if (a > max) return max;
    return a;
  }

  public void run()
  {
    try
    {
      long now = System.currentTimeMillis();
/*
  private int statusEventsLeft = MessagingNetworkStub.REQPARAM_NUMBER_OF_STATUS_CHANGED_EVENTS_PER_SESSION_MAX;
  private int messageEventsLeft = MessagingNetworkStub.REQPARAM_NUMBER_OF_MESSAGE_RECEIVED_EVENTS_PER_SESSION_MAX;
  private int contactsEventsLeft = MessagingNetworkStub.REQPARAM_NUMBER_OF_CONTACTS_RECEIVED_EVENTS_PER_SESSION_MAX;
*/
      long msgTime = now + random(MessagingNetworkStub.REQPARAM_DELAY_BETWEEN_MESSAGE_RECEIVED_EVENTS_PER_SESSION_MILLIS_MIN, MessagingNetworkStub.REQPARAM_DELAY_BETWEEN_MESSAGE_RECEIVED_EVENTS_PER_SESSION_MILLIS_MAX);
      long cntTime = now + random(MessagingNetworkStub.REQPARAM_DELAY_BETWEEN_CONTACTS_RECEIVED_EVENTS_PER_SESSION_MILLIS_MIN, MessagingNetworkStub.REQPARAM_DELAY_BETWEEN_CONTACTS_RECEIVED_EVENTS_PER_SESSION_MILLIS_MAX);
      long sttTime = now + random(MessagingNetworkStub.REQPARAM_DELAY_BETWEEN_STATUS_CHANGED_EVENTS_PER_SESSION_MILLIS_MIN, MessagingNetworkStub.REQPARAM_DELAY_BETWEEN_STATUS_CHANGED_EVENTS_PER_SESSION_MILLIS_MAX);

      for (;;)
      {
        try
        {
          if (context.getResourceManager().getSession(ses.getLoginId()) == null) return;
          now = System.currentTimeMillis();
          int min = Math.min(100, Math.min(Math.min((int) (msgTime-now), (int) (cntTime-now)), (int) (sttTime-now)));

          if (min < 0) min = 0;

          //if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("delay="+min+"; delays="+(msgTime-now)+"/"+(cntTime-now)+"/"+(sttTime-now));

          ses.sleep(min, min);

          if (context.getResourceManager().getSession(ses.getLoginId()) == null) return;
          now = System.currentTimeMillis();

          //if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("now="+now+" msgTime="+msgTime);

          if (now >= msgTime) { if (ses.messageEventsLeft <= 0) msgTime = Long.MAX_VALUE; else { fireMsg(); msgTime = now + random(MessagingNetworkStub.REQPARAM_DELAY_BETWEEN_MESSAGE_RECEIVED_EVENTS_PER_SESSION_MILLIS_MIN, MessagingNetworkStub.REQPARAM_DELAY_BETWEEN_MESSAGE_RECEIVED_EVENTS_PER_SESSION_MILLIS_MAX); }}
          if (now >= cntTime) { if (ses.contactsEventsLeft <= 0) cntTime = Long.MAX_VALUE; else { fireCnt(); cntTime = now + random(MessagingNetworkStub.REQPARAM_DELAY_BETWEEN_CONTACTS_RECEIVED_EVENTS_PER_SESSION_MILLIS_MIN, MessagingNetworkStub.REQPARAM_DELAY_BETWEEN_CONTACTS_RECEIVED_EVENTS_PER_SESSION_MILLIS_MAX); }}
          if (now >= sttTime) { if (ses.statusEventsLeft <= 0) sttTime = Long.MAX_VALUE; else { fireStt(); sttTime = now + random(MessagingNetworkStub.REQPARAM_DELAY_BETWEEN_STATUS_CHANGED_EVENTS_PER_SESSION_MILLIS_MIN, MessagingNetworkStub.REQPARAM_DELAY_BETWEEN_STATUS_CHANGED_EVENTS_PER_SESSION_MILLIS_MAX); }}

          if (context.getResourceManager().getSession(ses.getLoginId()) == null) return;

          if (ses.isRunning())
            ses.tick(context);
        }
        catch (Exception ex)
        {
          if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.WARN)) CAT.warn("exception while sess.tick()", ex);
        }

        context.getResourceManager().waitForSessionListChange(20);
      }
    }
    catch (Throwable tr)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("Exception while icq2k session loop", tr);
    }
  }

  private void fireMsg()
  {
    if (context.getResourceManager().getSession(ses.getLoginId()) == null) return;
    ses.messageEventsLeft--;
    System.gc();
    context.getICQ2KMessagingNetwork().fireMessageReceived(String.valueOf(random(10000, 11111111)), ses.getLoginId(), "random msg "+rnd.nextFloat());
  }
  private void fireCnt()
  {
    if (context.getResourceManager().getSession(ses.getLoginId()) == null) return;
    ses.contactsEventsLeft--;
    System.gc();
    context.getICQ2KMessagingNetwork().fireContactsReceived(String.valueOf(random(10000, 11111111)), ses.getLoginId(), new String[] {String.valueOf(random(10000, 11111111))}, new String[] {"nick "+rnd.nextFloat()});
  }
  private void fireStt()
  {
    if (context.getResourceManager().getSession(ses.getLoginId()) == null) return;
    String uin = ses.getRandomContactListEntry();
    if (uin == null)
    {
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("fireStt empty contactlist");
      return;
    }
    try
    {

      int s = ses.getContactStatus_Oscar(uin, context);
      int ss = -1;
      switch (s)
      {
        case StatusUtil.OSCAR_STATUS_ONLINE :
          ss = StatusUtil.OSCAR_STATUS_AWAY;
          break;
        case StatusUtil.OSCAR_STATUS_AWAY :
          ss = StatusUtil.OSCAR_STATUS_OFFLINE;
          break;
        case StatusUtil.OSCAR_STATUS_OFFLINE :
          ss = StatusUtil.OSCAR_STATUS_ONLINE;
          break;
        default:
          if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("invalid status, ignored: "+s);
          return;
      }
      ses.statusEventsLeft--;
      ses.setContactStatus_Oscar(uin, ss, context,
        MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, null);
    }
    catch (Exception ex)
    {
      try
      {
        ses.handleException(ex, "change status", context);
      }
      catch (Exception ex1)
      {
        if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("handleException", ex1);
      }
    }
  }
}
