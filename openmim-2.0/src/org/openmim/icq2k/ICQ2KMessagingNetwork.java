package org.openmim.icq2k;

import org.openmim.stuff.AsyncOperation;
import org.openmim.stuff.AsyncOperationRegistry;
import org.openmim.stuff.Defines;
import org.openmim.stuff.StatusUtilMim;
import org.openmim.stuff.UserDetails;
import org.openmim.stuff.UserSearchResults;
import org.openmim.transport_simpletcp.*;
import org.openmim.*;
import org.openmim.messaging_network.MessagingNetwork;
import org.openmim.messaging_network.MessagingNetworkException;
import org.openmim.messaging_network.MessagingNetworkListener;
import org.openmim.infrastructure.scheduler.Scheduler;
import org.openmim.wrapper.*;
import org.openmim.icq.util.*;
import org.openmim.icq.utils.*;

import java.util.*;
import java.net.*;

/*
  speed with ftp.icq.com: 26.33 KBytes/sec.

  keepalives: 100 bytes / 1 IP packet * 1 packet/120 seconds * 10 000 users = ~8KBytes/second

  tick network/server timeout: 10000 users * timeout of 5 minutes / 60 min per hour = ~833 hours
*/

/**
  Provides connectivity with the public ICQ service of America Online, Inc.,
  using the protocol of ICQ2000a/b clients, called AOL OSCAR protocol.

  <p>
    <b>Basics of OSCAR.</b>

  <p>
    Login stage.
      <ul>
      <li>The plugin connects to the authorization/login
      service of AOL, usually located at login.icq.com:5190.
      <li>Plugin sends the icq number and password.
      <li>Auth service sends either some authorization error, or,
      altenatively,
      <li>it sends host and port of <i>basic OSCAR service</i>
      ("BOS"), and a 256-byte authorization cookie.
      <li>Here, either plugin or server can close the auth connection socket.
      </ul>

  <p>
    Main stage: handshake.
      <ul>
      <li>The plugin connects to the BOS service using
      the host and port received.
      <li>Plugin sends the authorization cookie.
      <li>Plugin sends a contact list.
      <li>Plugin performs some necessary actions: it sends and receives
      some packets of unknown purpose.
      (Otherwise it will be disconnected.)
      <li>Plugin sets its online status.
      <li>Plugin is now connected.
      </ul>

  <p>
    Main stage: life.
      <ul>
      <li>Plugin can send plaintext messages to BOS.
      <li>Plugin can send client status change messages to BOS.
      <li>Plugin can add or remove contact list items,
      and send messages about that to BOS.
      <li>The BOS server can send plaintext messages to a plugin.
      <li>The BOS server can send contact list items status change
      messages to a plugin.
      <li>The BOS server can send error messages to a plugin.
      (They are converted into plain text messages of
      MessagingNetwork.)
      </ul>

  <p>
    Main stage: death.

    <ul>
      <li>To disconnect from any AOL service connection (including
        auth one, and BOS one), it is safe to just close
        the TCP/IP socket.
      <li>The TCP/IP socket can be closed.
        This means session death.
      <li>There can be any IO error in the TCP/IP socket.
        This means session death, too.
    </ul>

  <p>
    For more information about the OSCAR protocol, see
    the <a href=http://www.zigamorph.net/faim/protocol/>libfaim
    documentation</a>.  The same documentation should be in javaCard CVS.

  <p>
    Clause <code>implements AsyncOperationRegistry</code>
    means that (long) asynchronous operations ids are unique in the
    scope of this instance of ICQ2KMessagingNetwork.

  <p>
    @see Session
    @see ResourceManager
    @see SNACFamilies
*/

public class ICQ2KMessagingNetwork
implements
  MessagingNetwork,
  MessagingNetworkWrappable,
  AsyncOperationRegistry
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(ICQ2KMessagingNetwork.class.getName());

  static
  {
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("ICQ2K Session.java revision: "+Session.getVersionString());
  }

  static final boolean ENFORCE_ICQ_VALIDATION = false;

  /** Enables/disables the reconnector */
  public static boolean REQPARAM_RECONNECTOR_USED;


  public static int     REQPARAM_SESSION_ASYNCOP_QUEUE_SIZE;

  public static int     REQPARAM_GLOBAL_MAX_ASYNCOP_COUNT;

  /** if false, then new asyncops will fail immediately. */
  public static boolean REQPARAM_GLOBAL_MAX_ASYNCOP_COUNT_EXCEEDED_BLOCKS;

  /** network error reconnector threadpool config */
  public static int REQPARAM_RECONNECTOR_THREADCOUNT_OPTIMUM;
  /** network error reconnector threadpool config */
  public static int REQPARAM_RECONNECTOR_THREADCOUNT_MAXIMUM;

  /** session threadpool config */
  public static int REQPARAM_INPUT_DATA_HANDLING_THREADCOUNT_OPTIMUM;
  /** session threadpool config */
  public static int REQPARAM_INPUT_DATA_HANDLING_THREADCOUNT_MAXIMUM;

  /** async scheduler threadpool config */
  public static int REQPARAM_ASYNC_SCHEDULER_THREADCOUNT_OPTIMUM;
  /** async scheduler threadpool config */
  public static int REQPARAM_ASYNC_SCHEDULER_THREADCOUNT_MAXIMUM;

  public static int REQPARAM_NETWORK_CONDITIONS_SWING__MAXIMUM_COUNT_OF_REGISTERED_RELOGINS;
  public static int REQPARAM_NETWORK_CONDITIONS_SWING__FORGET_TIMEOUT_FOR_REGISTERED_RELOGINS_MINUTES;
  public static int REQPARAM_NETWORK_CONDITIONS_SWING__SLEEP_TIME_WHEN_MAXIMUM_REACHED_MINUTES;

  /**
   @see TransportChooser
   */
  public static String REQPARAM_TRANSPORTS_ALLOWED;

  public static int REQPARAM_SERVER_RESPONSE_TIMEOUT1_SECONDS;

  /**
   500 is a good value for this config parameter.
   @see AutoConfig
   @see Session#sendMessage0(Aim_conn_t, String, String)
   */
  public static long REQPARAM_ADVANCED_RATECONTROL_SENDPACKET_MILLIS;

  /**
   60000 is a good value for this config parameter.
   @see AutoConfig
   @see Session#sendMessage0(Aim_conn_t, String, String)
   */
  public static long REQPARAM_ADVANCED_RATECONTROL_RATE2_PERIOD_MILLIS;

  /**
   10 is a good value for this config parameter.
   @see AutoConfig
   @see Session#sendMessage0(Aim_conn_t, String, String)
   */
  public static int REQPARAM_ADVANCED_RATECONTROL_RATE2_MAXIMUM_MSGCOUNT;

  /**
   30 is good value for this config param.
   @see AutoConfig
   @see Session#sendMessage0(Aim_conn_t, String, String)
   */
  public static int REQPARAM_SENDTEXTMSG_SERVER_RESPONSE_TIMEOUT_SECONDS;

  /** 
    see cvs/im/Artifacts/Architecture/icq new features.doc
    <br>
    default: 1 
  */
  public static int REQPARAM_LOGIN_SCHEDULER_QUEUE_COUNT;

  /**
    see cvs/im/Artifacts/Architecture/icq new features.doc
    <br>
    default: 20000
  */
  public static long REQPARAM_LOGIN_SCHEDULER_TIME_DISTANCE_MILLIS;
  
  public static boolean REQPARAM_LOGIN_SCHEDULER_DISABLED;
  
  private static final LoginSched loginSched;
  
  static
  {
    AutoConfig.fetchFromClassLocalResourceProperties(ICQ2KMessagingNetwork.class, true, false);
  
    if (REQPARAM_LOGIN_SCHEDULER_DISABLED)
      loginSched=null;
    else
      loginSched=new LoginSchedImpl(REQPARAM_LOGIN_SCHEDULER_QUEUE_COUNT, REQPARAM_LOGIN_SCHEDULER_TIME_DISTANCE_MILLIS);

    if (REQPARAM_ADVANCED_RATECONTROL_SENDPACKET_MILLIS <= 0)
      throw new RuntimeException("REQPARAM_ADVANCED_RATECONTROL_SENDPACKET_MILLIS autoconfig property must be positive, but it is " + REQPARAM_ADVANCED_RATECONTROL_SENDPACKET_MILLIS);
    if (REQPARAM_ADVANCED_RATECONTROL_RATE2_MAXIMUM_MSGCOUNT <= 0)
      throw new RuntimeException("REQPARAM_ADVANCED_RATECONTROL_RATE2_MAXIMUM_MSGCOUNT autoconfig property must be positive, but it is " + REQPARAM_ADVANCED_RATECONTROL_RATE2_MAXIMUM_MSGCOUNT);
    if (REQPARAM_ADVANCED_RATECONTROL_RATE2_PERIOD_MILLIS <= 0)
      throw new RuntimeException("REQPARAM_ADVANCED_RATECONTROL_RATE2_PERIOD_MILLIS autoconfig property must be positive, but it is " + REQPARAM_ADVANCED_RATECONTROL_RATE2_PERIOD_MILLIS);
    if (REQPARAM_SENDTEXTMSG_SERVER_RESPONSE_TIMEOUT_SECONDS <= 0)
      throw new RuntimeException("REQPARAM_SENDTEXTMSG_SERVER_RESPONSE_TIMEOUT_SECONDS autoconfig property must be positive, but it is " + REQPARAM_SENDTEXTMSG_SERVER_RESPONSE_TIMEOUT_SECONDS);

    if (REQPARAM_NETWORK_CONDITIONS_SWING__MAXIMUM_COUNT_OF_REGISTERED_RELOGINS < 1)
      throw new RuntimeException("REQPARAM_NETWORK_CONDITIONS_SWING__MAXIMUM_COUNT_OF_REGISTERED_RELOGINS autoconfig property must be >= 1, but it is " + REQPARAM_NETWORK_CONDITIONS_SWING__MAXIMUM_COUNT_OF_REGISTERED_RELOGINS);
    if (REQPARAM_NETWORK_CONDITIONS_SWING__FORGET_TIMEOUT_FOR_REGISTERED_RELOGINS_MINUTES <= 0)
      throw new RuntimeException("REQPARAM_NETWORK_CONDITIONS_SWING__FORGET_TIMEOUT_FOR_REGISTERED_RELOGINS_MINUTES autoconfig property must be positive, but it is " + REQPARAM_NETWORK_CONDITIONS_SWING__FORGET_TIMEOUT_FOR_REGISTERED_RELOGINS_MINUTES);
    if (REQPARAM_NETWORK_CONDITIONS_SWING__SLEEP_TIME_WHEN_MAXIMUM_REACHED_MINUTES <= 0)
      throw new RuntimeException("REQPARAM_NETWORK_CONDITIONS_SWING__SLEEP_TIME_WHEN_MAXIMUM_REACHED_MINUTES autoconfig property must be positive, but it is " + REQPARAM_NETWORK_CONDITIONS_SWING__SLEEP_TIME_WHEN_MAXIMUM_REACHED_MINUTES);
  }

  public static final byte ICQ2K_NETWORK_ID = 1;
  public static final String ICQ2K_NETWORK_NAME = "ICQ";
  private final Vector messagingNetworkListeners = new Vector(1, 1);
  private final static int loginServerPort;
  protected final PluginContext ctx = new PluginContext(this);
  protected final ResourceManager resourceManager = makeResourceManagerInstance();
  private final static InetAddress loginServerInetAddress;

  final static boolean keepAlivesUsed;
  final static int keepAlivesIntervalMillis;
  final static long serverResponseTimeoutMillis;
  public final static long socketTimeoutMillis;
  private final static Properties props;

  static
  {
    try
    {
      final String name = "/" + ICQ2KMessagingNetwork.class.getName().replace('.', '/') + ".properties";
      String propFileDisplayName = "\"" + name + "\" resource";
      java.io.InputStream props_is = ICQ2KMessagingNetwork.class.getResourceAsStream(name);
      if (props_is == null)
        throw new RuntimeException(propFileDisplayName + " must be present in classpath, but the resource does not exist.");
      props = new Properties();
      try
      {
        props.load(props_is);
      }
      catch (java.io.IOException iex)
      {
        if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("ioerror while loading properties resource file " + name, iex);
        throw new RuntimeException("Error while loading properties from " + propFileDisplayName + ":\r\n" + iex);
      }
      finally
      {
        try
        {
          props_is.close();
        }
        catch (Exception ex)
        {
        }
      }
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("" + ICQ2KMessagingNetwork.class.getName() + " class startup properties: " + props);
      String loginServerHost = PropertyUtil.getRequiredProperty(props, propFileDisplayName, "icq.server.login.host");
      loginServerPort = PropertyUtil.getRequiredPropertyInt(props, propFileDisplayName, "icq.server.login.port");
      loginServerInetAddress = java.net.InetAddress.getByName(loginServerHost);
      serverResponseTimeoutMillis = PropertyUtil.getRequiredPropertyInt(props, propFileDisplayName, "server.response.timeout.seconds") * 1000;
      if (serverResponseTimeoutMillis <= 0)
        throw new RuntimeException("Invalid property value for server.response.timeout.seconds: " + (int) (serverResponseTimeoutMillis / 1000) + ", must be positive.");
      socketTimeoutMillis = PropertyUtil.getRequiredPropertyInt(props, propFileDisplayName, "socket.timeout.seconds") * 1000;
      if (socketTimeoutMillis < 0)
        throw new RuntimeException("Invalid property value for socket.timeout.seconds: " + (int) (socketTimeoutMillis / 1000) + ", must be non-negative.");

      keepAlivesUsed = PropertyUtil.getRequiredPropertyBoolean(props, propFileDisplayName, "keepalives.used");
      if (keepAlivesUsed)
      {
        int keepAlivesIntervalSeconds = PropertyUtil.getRequiredPropertyInt(props, propFileDisplayName, "keepalives.interval.seconds");
        if (keepAlivesIntervalSeconds < 30)
          throw new RuntimeException("Invalid property value for keepalives.interval.seconds: " + keepAlivesIntervalSeconds + ", must be >= 30.");
        keepAlivesIntervalMillis = 1000 * keepAlivesIntervalSeconds;
      }
      else
      {
        keepAlivesIntervalMillis = 0;
      }

      {
        //normal tcp connections
        TransportChooser.setTransportsAllowed(REQPARAM_TRANSPORTS_ALLOWED);

        if (TransportChooser.isSocks5Allowed())
        {
          InetAddress socks5Host = null;
          int socks5Port = 0;
          String socks5UserName = null;
          String socks5Password = null;

          String socks5Host_s = PropertyUtil.getRequiredProperty(props, propFileDisplayName, "socks5.proxy.host");
          socks5Host = InetAddress.getByName(socks5Host_s);
          socks5Port = PropertyUtil.getRequiredPropertyInt(props, propFileDisplayName, "socks5.proxy.port");
          boolean socksProxyUsernameAuthUsed = PropertyUtil.getRequiredPropertyBoolean(props, propFileDisplayName, "socks5.proxy.username.auth.used");
          if (socksProxyUsernameAuthUsed)
          {
            socks5UserName = PropertyUtil.getRequiredProperty(props, propFileDisplayName, "socks5.proxy.username");
            socks5Password = PropertyUtil.getRequiredProperty(props, propFileDisplayName, "socks5.proxy.password");
          }
          TransportChooser.setSocks5ProxyDetails(socks5Host, socks5Port, socks5UserName, socks5Password);
        }
      }
    }
    catch (RuntimeException ex)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("invalid resource properties for icq2k", ex);
      throw ex;
    }
    catch (java.net.UnknownHostException ex2)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("cannot resolve host specified in resource properties for icq2k", ex2);
      throw new RuntimeException("" + ex2);
    }
  }

  final GlobalAopLimit gal = new GlobalAopLimit();
  final SimpleTcp socketRegistry;

  public ICQ2KMessagingNetwork()
  {
    if (Defines.ENABLE_FAKE_PLUGIN)
      socketRegistry = null;
    else
      socketRegistry = new SimpleTcp(
        REQPARAM_INPUT_DATA_HANDLING_THREADCOUNT_MAXIMUM,
        REQPARAM_INPUT_DATA_HANDLING_THREADCOUNT_OPTIMUM);
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
        //synchronized (ses) //commented out because of deadlock bug
        //{
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ICQ FIRES EVENT to core: messageReceived: src " + srcLoginId + ", dst " + dstLoginId + ", text "+StringUtil.toPrintableString(messageText)+", listener: " + l);
        l.messageReceived(getNetworkId(), srcLoginId, dstLoginId, messageText);
        //}
      }
    }
  }

  void fireAuthRequestReceived(String srcLoginId, String dstLoginId, String messageText)
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
        //synchronized (ses) //commented out because of deadlock bug
        //{
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ICQ FIRES EVENT to core: authorizationRequest: src " + srcLoginId + ", dst " + dstLoginId + ", text "+StringUtil.toPrintableString(messageText)+", listener: " + l);
        l.authorizationRequest(getNetworkId(), srcLoginId, dstLoginId, messageText);
        //}
      }
    }
  }

  void fireAuthReplyReceived(String srcLoginId, String dstLoginId, boolean grant)
  {
    if (StringUtil.isNullOrEmpty(srcLoginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(srcLoginId, "srcLoginId");
    if (StringUtil.isNullOrEmpty(dstLoginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(dstLoginId, "dstLoginId");

    synchronized (messagingNetworkListeners)
    {
      for (int i = 0; i < messagingNetworkListeners.size(); i++)
      {
        MessagingNetworkListener l = (MessagingNetworkListener) messagingNetworkListeners.elementAt(i);
        Session ses = getSession0(dstLoginId);
        if ((ses) == null) Lang.ASSERT_NOT_NULL(ses, "session for " + dstLoginId);
        //synchronized (ses) //commented out because of deadlock bug
        //{
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ICQ FIRES EVENT to core: authorizationResponse: src " + srcLoginId + ", dst " + dstLoginId + ", grant=="+grant+", listener: " + l);
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
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ICQ FIRES EVENT to core: contactsReceived: src " + srcLoginId + ", dst " + dstLoginId + ", number of contacts "+contactsLoginIds.length+", listener: " + l);
        l.contactsReceived(getNetworkId(), srcLoginId, dstLoginId, contactsLoginIds, contactsNicks);
        //}
      }
    }
  }

  /**
    Unconditionally fires a status change event.
    TODO/TONEVERDO: replace with more safe method.
  */
  void fireStatusChanged_Mim_Uncond(String srcLoginId, String dstLoginId, int status_mim,
      int reasonLogger, String reasonMessage, int endUserReasonCode)
    throws MessagingNetworkException
  {
    if (StringUtil.isNullOrEmpty(srcLoginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(srcLoginId, "srcLoginId");
    if (StringUtil.isNullOrEmpty(dstLoginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(dstLoginId, "dstLoginId");
    MLang.EXPECT_IS_MIM_STATUS(status_mim, "status_mim");

    synchronized (messagingNetworkListeners)
    {
      Session sess = getSession0(srcLoginId);
      if (sess != null)
      {
        if (status_mim == MessagingNetwork.STATUS_OFFLINE && srcLoginId.equals(dstLoginId))
        {
          sess.shutdown(ctx, reasonLogger, reasonMessage, endUserReasonCode);
          getResourceManager().removeSession(sess);
        }

        for (int i = 0; i < messagingNetworkListeners.size(); i++)
        {
          MessagingNetworkListener l = (MessagingNetworkListener) messagingNetworkListeners.elementAt(i);
          //src may already be logged off; no session
          //synchronized (getSessionLock(srcLoginId)) {
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ICQ FIRES EVENT to core: statusChanged: src " + srcLoginId + " dst " + dstLoginId + ", status: "+StatusUtil.translateStatusMimToString(status_mim)+", reasonLogger: "+MessagingNetworkException.getLoggerMessage(reasonLogger)+", endUserMessage: "+StringUtil.toPrintableString(MessagingNetworkException.getEndUserReasonMessage(endUserReasonCode))+", listener: " + l);
          l.statusChanged(getNetworkId(), srcLoginId, dstLoginId, status_mim, reasonLogger, reasonMessage, endUserReasonCode);
          //}
        }
      }
      else
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("fireSttChg_Uncond: session is null, statusChange to "+StatusUtilMim.translateStatusMimToString(status_mim)+" ignored");
    }
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

  /** Get comment for network. */
  public final String getComment() { return ""; }

  /** Returns the network name. */
  public String getName() { return ICQ2K_NETWORK_NAME; }
  public byte getNetworkId() { return ICQ2K_NETWORK_ID; }

  static final int getKeepAlivesIntervalMillis() { return keepAlivesIntervalMillis; }
  public final long getServerResponseTimeoutMillis() { return serverResponseTimeoutMillis; }
  public long getSocketTimeoutMillis() { return socketTimeoutMillis; }
  final static boolean isKeepAlivesUsed() { return keepAlivesUsed; }

  /** Authorization server host.  Specified in resource properties. */
  static final java.net.InetAddress getLoginServerInetAddress() { return loginServerInetAddress; }

  /** Authorization server port.  Specified in resource properties. */
  static final int getLoginServerPort() { return loginServerPort; }

  final ResourceManager getResourceManager() { return resourceManager; }

  protected ResourceManager makeResourceManagerInstance()
  { return new ResourceManager(ctx); }

  /** Can return null, srcLoginId should never be null. */
  public MessagingNetworkSession getSession(String loginId) { return getSession0(loginId); }

  private Session getSession0(String loginId)
  {
    if (StringUtil.isNullOrEmpty(loginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(loginId, "loginId");
    return getResourceManager().getSession(loginId);
  }

  private Session getSessionNotNull(String loginId, int endUserOperationErrCode)
    throws MessagingNetworkException
  {
    if (StringUtil.isNullOrEmpty(loginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(loginId, "loginId");
    Session ses = getResourceManager().getSessionNotNull(loginId, endUserOperationErrCode);
    ses.checkValid();
    return ses;
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
    return StatusUtil.translateStatusOscarToMim_cl_entry(session.getContactStatus_Oscar(dstLoginId, ctx));
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
        return session.registerLoginId(password, ctx);
      }
      finally
      {
        try
        {
          session.logout(ctx, MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, null, MessagingNetworkException.ENDUSER_NO_ERROR);
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

  public void addToContactList(String srcLoginId, String dstLoginId)
    throws MessagingNetworkException
  {
    gal.addAop(false);
    AsyncOperationImpl aop = null;
    try
    {
      Session session = getSessionNotNull(srcLoginId,
          MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_ADD_TO_CONTACT_LIST_WHILE_OFFLINE);
      session.runSynchronous(aop = new AsyncOperations.
        OpAddContactListItem(dstLoginId, session, ctx));
    }
    catch (MessagingNetworkException mex)
    {
      handleEarlyException(aop, mex);
    }
  }

  public void removeFromContactList(String srcLoginId, final String dstLoginId)
  throws MessagingNetworkException
  {
    gal.addAop(false);
    AsyncOperationImpl aop = null;
    try
    {
      Session session = getSessionNotNull(srcLoginId,
          MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_REMOVE_FROM_CONTACT_LIST_WHILE_OFFLINE);
      session.runSynchronous(aop = new AsyncOperations.
        OpRemoveContactListItem(dstLoginId, session, ctx));
    }
    catch (MessagingNetworkException mex)
    {
      handleEarlyException(aop, mex);
    }
  }

  public final void removeMessagingNetworkListener(MessagingNetworkListener l)
  { if (l == null) Lang.ASSERT_NOT_NULL(l, "listener");
    messagingNetworkListeners.removeElement(l);
  }

  public void sendContacts(String srcLoginId, final String dstLoginId, final String[] nicks, final String[] loginIds)
    throws MessagingNetworkException
  {
    gal.addAop(false);
    AsyncOperationImpl aop = null;
    try
    {
      Session session = getSessionNotNull(srcLoginId,
          MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_SEND_CONTACTS_WHILE_OFFLINE);
      session.runSynchronous(aop = new AsyncOperations
        .OpSendContacts(dstLoginId, nicks, loginIds, session, ctx));
    }
    catch (MessagingNetworkException mex)
    {
      handleEarlyException(aop, mex);
    }
  }

  /** fast: asynchronous */
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

  /** fast: asynchronous */
  public void setClientStatus(String srcLoginId, final int status_mim, final int endUserReason)
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

  private void setClientStatus0(final Session session, final int status_mim, final int endUserReason)
  throws MessagingNetworkException
  {
    if (status_mim == MessagingNetwork.STATUS_OFFLINE)
    {
      //logoff is not synchronized (almost)
      session.logout(ctx,
        MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_LOGOUT_CALLER,
        "logout requested by plugin caller",
        endUserReason);
    }
    else
    {
      gal.addAop(false);
      AsyncOperationImpl aop = null;
      try
      {
        session.enqueue(aop = new AsyncOperations
            .OpSetStatusNonOffline(status_mim, session, ctx));
      }
      catch (MessagingNetworkException mex)
      {
        handleEarlyException(aop, mex);
      }
    }
  }

  public UserDetails getUserDetails(String srcLoginId, String dstLoginId)
  throws MessagingNetworkException
  {
    gal.addAop(false);
    AsyncOperationImpl aop = null;
    try
    {
      Session session = getSessionNotNull(srcLoginId,
          MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_GET_USER_DETAILS_WHILE_OFFLINE);
      AsyncOperations.OpGetUserDetails op;
      session.runSynchronous(aop = op = new AsyncOperations
        .OpGetUserDetails(dstLoginId, session, true, ctx));
      //CAT.debug("", new Exception("1dumpstack!!!!!!!REMOVE THIS!!!!!!"));
      return op.getResult();
    }
    catch (MessagingNetworkException mex)
    {
      //CAT.debug("", new Exception("2dumpstack!!!!!!!REMOVE THIS!!!!!!"));
      handleEarlyException(aop, mex);
      //CAT.debug("", new Exception("3dumpstack!!!!!!!REMOVE THIS!!!!!!"));
      return null;
    }
  }

  /* async.  returns operation id. */
  public long startGetUserDetails(String srcLoginId, String dstLoginId)
  throws MessagingNetworkException
  {
    gal.addAop(false);
    AsyncOperationImpl aop = null;
    try
    {
      Session session = getSessionNotNull(srcLoginId,
          MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_GET_USER_DETAILS_WHILE_OFFLINE);
      return session.enqueue(aop = new AsyncOperations
        .OpGetUserDetails(dstLoginId, session, true, ctx));
    }
    catch (MessagingNetworkException mex)
    {
      handleEarlyException(aop, mex);
      return -1;
    }
  }

  /* async.  does NOT call MessagingNetworkListeners on ICQ2KMessagingNetwork instance. */
  public void startGetUserDetails(String srcLoginId, String dstLoginId, MessagingNetworkListener l)
  throws MessagingNetworkException
  {
    gal.addAop(false);
    AsyncOperationImpl aop = null;
    try
    {
      Session session = getSessionNotNull(srcLoginId,
          MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_GET_USER_DETAILS_WHILE_OFFLINE);
      session.enqueue(aop = new AsyncOperations
        .OpGetUserDetails(dstLoginId, session, false, ctx), l);
    }
    catch (MessagingNetworkException mex)
    {
      handleEarlyException(aop, mex);
    }
  }

  public UserSearchResults searchUsers(
    String srcLoginId,
    String emailSearchPattern,
    String nickSearchPattern,
    String firstNameSearchPattern,
    String lastNameSearchPattern) throws MessagingNetworkException
  {
    gal.addAop(false);
    AsyncOperationImpl aop = null;
    try
    {
      Session session = getSessionNotNull(srcLoginId,
          MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_SEARCH_USERS_WHILE_OFFLINE);
      AsyncOperations.OpSearch op;
      session.runSynchronous(aop = op = new AsyncOperations
        .OpSearch(emailSearchPattern, nickSearchPattern,
                  firstNameSearchPattern, lastNameSearchPattern,
                  session, ctx));
      return op.getResult();
    }
    catch (MessagingNetworkException mex)
    {
      handleEarlyException(aop, mex);
      return null;
    }
  }

  /*
  public long startSearchUsers(
    String srcLoginId,
    String emailSearchPattern,
    String nickSearchPattern,
    String firstNameSearchPattern,
    String lastNameSearchPattern)
  throws MessagingNetworkException
  {
    Session session = getSessionNotNull(srcLoginId,
        MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_SEARCH_USERS_WHILE_OFFLINE);
    return session.enqueue(new AsyncOperations
      .OpSearch(emailSearchPattern, nickSearchPattern,
                firstNameSearchPattern, lastNameSearchPattern,
                session, ctx));
  }
  */


  /**
   Using the connection of srcLoginId uin,
   retrieves the authorizationRequired property of dstLoginId uin
   from the icq server.
   */
  public boolean isAuthorizationRequired(String srcLoginId, String dstLoginId)
    throws MessagingNetworkException
  {
    gal.addAop(false);
    AsyncOperationImpl aop = null;
    try
    {
      Session session = getSessionNotNull(srcLoginId,
          MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_OPERATION_WHILE_OFFLINE);
      AsyncOperations.OpIsAuthorizationRequired op;
      session.runSynchronous(aop = op = new AsyncOperations
        .OpIsAuthorizationRequired(dstLoginId, session, ctx));
      return op.getResult();
    }
    catch (MessagingNetworkException mex)
    {
      handleEarlyException(aop, mex);
      return false;
    }
  }

  public long startIsAuthorizationRequired(String srcLoginId, String dstLoginId)
    throws MessagingNetworkException
  {
    gal.addAop(false);
    AsyncOperationImpl aop = null;
    try
    {
      Session session = getSessionNotNull(srcLoginId,
          MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_OPERATION_WHILE_OFFLINE);
      return session.enqueue(aop = new AsyncOperations
        .OpIsAuthorizationRequired(dstLoginId, session, ctx));
    }
    catch (MessagingNetworkException mex)
    {
      handleEarlyException(aop, mex);
      return -1;
    }
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
    gal.addAop(false);
    AsyncOperationImpl aop = null;
    try
    {
      Session session = getSessionNotNull(srcLoginId,
          MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_OPERATION_WHILE_OFFLINE);
      session.runSynchronous(aop = new AsyncOperations
        .OpSendAuthorizationRequest(dstLoginId, reason, session, ctx));
    }
    catch (MessagingNetworkException mex)
    {
      handleEarlyException(aop, mex);
    }
  }

  /*
  public long startAuthorizationRequest(String srcLoginId, String dstLoginId, String reason)
    throws MessagingNetworkException
  {
    Session session = getSessionNotNull(srcLoginId,
             MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_OPERATION_WHILE_OFFLINE);
    return session.enqueue(new AsyncOperations
      .OpSendAuthorizationRequest(dstLoginId, reason, session, ctx));
  }
  */

  /**
   Using the connection of srcLoginId uin,
   sends a reply to preceding auth request of dstLoginId uin,
   to state that srcLoginId grants or denies
   dstLoginId's request to add srcLoginId to dstLoginId's contact list.
   */
  public void authorizationResponse(String srcLoginId, String dstLoginId, boolean grant)
    throws MessagingNetworkException
  {
    gal.addAop(false);
    AsyncOperationImpl aop = null;
    try
    {
      Session session = getSessionNotNull(srcLoginId,
          MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_OPERATION_WHILE_OFFLINE);
      session.runSynchronous(aop = new AsyncOperations
        .OpSendAuthorizationResponse(dstLoginId, grant, session, ctx));
    }
    catch (MessagingNetworkException mex)
    {
      handleEarlyException(aop, mex);
    }
  }

  public long startAuthorizationResponse(String srcLoginId, String dstLoginId, boolean grant)
    throws MessagingNetworkException
  {
    gal.addAop(false);
    AsyncOperationImpl aop = null;
    try
    {
      Session session = getSessionNotNull(srcLoginId,
          MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_OPERATION_WHILE_OFFLINE);
      return session.enqueue(aop = new AsyncOperations
        .OpSendAuthorizationResponse(dstLoginId, grant, session, ctx));
    }
    catch (MessagingNetworkException mex)
    {
      handleEarlyException(aop, mex);
      return -1;
    }
  }

  private Scheduler asyncScheduler;

  /**
    fast: asynchronous.

    Returns operation id.

    @see MessagingNetworkListener#setStatusFailed(byte, long, String, MessagingNetworkException)
  */
  public long startLogin(
      final String srcLoginId,
      String password, String[] contactList, int statusMim)
  throws MessagingNetworkException
  {
    if (!(  statusMim != MessagingNetwork.STATUS_OFFLINE  ))
      throw new AssertException("cannot call startLogin(..., STATUS_OFFLINE)");
    MLang.EXPECT_IS_MIM_STATUS(statusMim, "loginStatusMim");

    gal.addAop(true);
    AsyncOperationImpl aop = null;
    try
    {
      Session session = getResourceManager().getCreateSession(srcLoginId);
      return session.enqueue(aop = new AsyncOperations
        .OpLogin(password, contactList, statusMim, false, session, ctx),
        calcLoginStartTime(session));
    }
    catch (MessagingNetworkException mex)
    {
      handleEarlyException(aop, mex);
      return -1;
    }
  }
  
  private long calcLoginStartTime(Session s)
  {
    if (loginSched!=null && s.getStatus_Oscar()==StatusUtil.OSCAR_STATUS_OFFLINE) 
      return loginSched.allocateLoginStartTime();
    else
      return 0;
  }

  /** synchronous */
  public void login(String srcLoginId, String password, java.lang.String[] contactList, int statusMim)
    throws MessagingNetworkException
  {
    if (statusMim == MessagingNetwork.STATUS_OFFLINE)
      throw new AssertException("invalid call: login(..., MessagingNetwork.STATUS_OFFLINE)");
    MLang.EXPECT_IS_MIM_STATUS(statusMim, "statusMim");

    gal.addAop(true);
    AsyncOperationImpl aop = null;
    MessagingNetworkException mexmex = null;
    try
    {
      Session session = getResourceManager().getCreateSession(srcLoginId);
      //synchronized (session)
      {
        try
        {
          session.runSynchronous(aop = new AsyncOperations
            .OpLogin(password, contactList, statusMim, false, session, ctx),
            calcLoginStartTime(session));
          CAT.debug("no mexmex1");
        }
        catch (MessagingNetworkException mex)
        {
          mexmex = mex;
          CAT.debug("mexmex1: "+mexmex);
        }
        
        if (session.getStatus_Oscar() == StatusUtil.OSCAR_STATUS_OFFLINE)
        {
          try
          {
            session.shutdown(ctx,
              MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
              "just to make sure",
              MessagingNetworkException.ENDUSER_UNDEFINED);
          }
          catch (Exception ex)
          {
            if (Defines.DEBUG) CAT.error("unexpected exception while login(), exception ignored", ex);
          }
          if (Defines.DEBUG)
          {
            int status = session.getStatus_Oscar();
            if (status != StatusUtil.OSCAR_STATUS_OFFLINE)
              CAT.error("", new AssertException("session.getStatus_Oscar() must be StatusUtil.OSCAR_STATUS_OFFLINE after failed login, but it is not: statusOscar="+status));
          }
          getResourceManager().removeSession(session);
        }
      }
    }
    catch (MessagingNetworkException mex)
    {
      mexmex = mex;
    }
    
    if (mexmex != null)
    {
      handleEarlyException(aop, mexmex);
    }
    else
    {
    }
  }

  /**
    fast: asynchronous.

    Returns operation id.

    @see MessagingNetworkListener#sendMessageFailed(byte, long, String, String, String, MessagingNetworkException)
    @see MessagingNetworkListener#sendMessageSucceeded(byte, long, String, String, String)
  */
  public long startSendMessage(final String srcLoginId, String dstLoginId, String text)
  throws MessagingNetworkException
  {
    gal.addAop(false);
    AsyncOperationImpl aop = null;
    try
    {
      Session session = getSessionNotNull(srcLoginId,
          MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_SEND_TEXT_MESSAGE_WHILE_OFFLINE);
      return session.enqueue(aop = new AsyncOperations.
        OpSendMessage(dstLoginId, text, session, ctx));
    }
    catch (MessagingNetworkException mex)
    {
      handleEarlyException(aop, mex);
      return -1;
    }
  }

  /**
    slow: synchronous.
  */
  public void sendMessage(final String srcLoginId, String dstLoginId, String text)
  throws MessagingNetworkException
  {
    gal.addAop(false);
    AsyncOperationImpl aop = null;
    try
    {
      Session session = getSessionNotNull(srcLoginId,
          MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_SEND_TEXT_MESSAGE_WHILE_OFFLINE);
      session.runSynchronous(aop = new AsyncOperations.
        OpSendMessage(dstLoginId, text, session, ctx));
    }
    catch (MessagingNetworkException mex)
    {
      handleEarlyException(aop, mex);
    }
  }

  private final Object initLock = new Object();

  public Scheduler getScheduler()
  {
    synchronized (initLock)
    {
      if (asyncScheduler == null) throw new AssertException("call init() first");
      return asyncScheduler;
    }
  }


  /* Async operation registry stuff start */


  private long nextOperationId = 1;
  private final HashMap operationIds = new HashMap();

  private Long generateOperationId(AsyncOperation op)
  {
    synchronized (operationIds)
    {
      long loops = 0;
      long newCID = -1;
      for (;;)
      {
        newCID = nextOperationId;
        if (nextOperationId == Long.MAX_VALUE)
        {
          nextOperationId = 1;
        }
        else
          ++nextOperationId;

        Long n = new Long(newCID);
        if (operationIds.get(n) != null)
        {
          if (++loops == 0xffffFFFF) throw new RuntimeException("too many operations: "+loops);
          continue;
        }
        operationIds.put(n, op);
        return n;
      }
    }
  }

  /** Generates ids not found in this registry. */
  public Long addOperation(AsyncOperation op)
  {
    return generateOperationId(op);
  }

  /** Recycles ids.
      Does not throw exceptions if the id is not present. */
  public void removeOperation(AsyncOperation op)
  {
    synchronized (operationIds)
    {
      operationIds.remove(op.getIdLong());
    }
  }


  /* Async operation registry stuff end */


  //async fire methods
  void getUserDetailsSuccess(AsyncOperations.OpGetUserDetails op, UserDetails ud)
  {
    synchronized (messagingNetworkListeners) {
      for (int i = 0; i < messagingNetworkListeners.size(); ++i) {
        MessagingNetworkListener l = (MessagingNetworkListener) messagingNetworkListeners.elementAt(i);
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
            ("ICQ FIRES EVENT to core: getUserDetailsSuccess: src " + op.session.getLoginId() +
              ", dst " + op.dstLoginId);
        l.getUserDetailsSuccess(getNetworkId(), op.getId(), op.session.getLoginId(),
            op.dstLoginId, op.getResult());
      }
    }
  }

  void getUserDetailsFailed(AsyncOperations.OpGetUserDetails op, MessagingNetworkException ex)
  {
    synchronized (messagingNetworkListeners) {
      for (int i = 0; i < messagingNetworkListeners.size(); ++i) {
        MessagingNetworkListener l = (MessagingNetworkListener) messagingNetworkListeners.elementAt(i);
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
            ("ICQ FIRES EVENT to core: getUserDetailsFailed: src " + op.session.getLoginId() +
              ", dst " + op.dstLoginId, ex);
        l.getUserDetailsFailed(getNetworkId(), op.getId(), op.session.getLoginId(),
            op.dstLoginId, ex);
      }
    }
  }

  void sendMessageSuccess(AsyncOperations.OpSendMessage op)
  {
    synchronized (messagingNetworkListeners) {
      for (int i = 0; i < messagingNetworkListeners.size(); ++i) {
        MessagingNetworkListener l = (MessagingNetworkListener) messagingNetworkListeners.elementAt(i);
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
            ("ICQ FIRES EVENT to core: sendMessageSuccess: src " + op.session.getLoginId() +
              ", dst " + op.dstLoginId+
              ", text "+ StringUtil.toPrintableString(op.text));
        l.sendMessageSuccess(getNetworkId(), op.getId(), op.session.getLoginId(),
            op.dstLoginId, op.text);
      }
    }
  }

  void sendMessageFailed(AsyncOperations.OpSendMessage op, MessagingNetworkException ex)
  {
    synchronized (messagingNetworkListeners) {
      for (int i = 0; i < messagingNetworkListeners.size(); ++i) {
        MessagingNetworkListener l = (MessagingNetworkListener) messagingNetworkListeners.elementAt(i);
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
            ("ICQ FIRES EVENT to core: sendMessageFailed: src " + op.session.getLoginId() +
              ", dst " + op.dstLoginId+
              ", text "+ StringUtil.toPrintableString(op.text), ex);
        l.sendMessageFailed(getNetworkId(), op.getId(), op.session.getLoginId(),
            op.dstLoginId, op.text, ex);
      }
    }
  }

  void setStatusFailed(AsyncOperations.OpSetStatus op, MessagingNetworkException ex)
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("setStatusFailed, op: " + op);
    if (op instanceof AsyncOperations.OpLogin)
    {
      //if (op.session.getStatus_Oscar() != StatusUtil.OSCAR_STATUS_OFFLINE) {
      op.session.shutdown(ctx, ex.getLogger(), ex.getMessage(), ex.getEndUserReasonCode());
      //} else {
      getResourceManager().removeSession(op.session);
    }

    synchronized (messagingNetworkListeners)
    {
      for (int i = 0; i < messagingNetworkListeners.size(); ++i)
      {
        MessagingNetworkListener l = (MessagingNetworkListener) messagingNetworkListeners.elementAt(i);
        if (Defines.DEBUG && CAT.isDebugEnabled())
          CAT.debug("ICQ FIRES EVENT to core: setStatusFailed: src " + op.session.getLoginId(), ex);
        l.setStatusFailed(getNetworkId(), op.getId(), op.session.getLoginId(), ex);
      }
    }
    //}
  }

  public final GlobalAopLimit getGal()
  {
    return gal;
  }

  final void handleEarlyException(AsyncOperationImpl aop, MessagingNetworkException mex)
  throws MessagingNetworkException
  {
    galRemove(aop);
    throw mex;
  }

  private final void galRemove(AsyncOperationImpl aop)
  throws MessagingNetworkException
  {
    if (aop != null) aop.galRemove();
    else gal.removeAop();
  }

  public void init()
  {
    synchronized (initLock)
    {
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("icq2k: init() enter");

      loginSched.init();
      gal.init();
      asyncScheduler = new Scheduler(
        REQPARAM_ASYNC_SCHEDULER_THREADCOUNT_MAXIMUM,
        REQPARAM_ASYNC_SCHEDULER_THREADCOUNT_OPTIMUM);
      asyncScheduler.init();
      getResourceManager().init();
      if (socketRegistry != null) socketRegistry.init();
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("icq2k: init() leave");
    }
  }

  public void deinit()
  {
    synchronized (initLock)
    {
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("icq2k: deinit() enter");
      messagingNetworkListeners.clear();
      gal.deinit();
      if (asyncScheduler != null)
      {
        asyncScheduler.deinit();
        asyncScheduler = null;
      }
      getResourceManager().deinit();
      if (socketRegistry != null) socketRegistry.deinit();
      loginSched.deinit();
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("icq2k: deinit() leave");
    }
  }
}
