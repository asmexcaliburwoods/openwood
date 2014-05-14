package org.openmim.msn;

import java.io.*;
import java.net.*;
import java.util.*;
import org.openmim.icq.util.joe.*;
import org.openmim.icq.util.*;
import org.openmim.*;
import org.openmim.mn.MessagingNetwork;
import org.openmim.mn.MessagingNetworkException;
import org.openmim.wrapper.*;
import org.openmim.infrastructure.taskmanager.Task;

public class Session
implements Constants, org.openmim.wrapper.MessagingNetworkSession
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(Session.class.getName());

  public static String getVersionString()
  {
    String rev = "$Revision: 1.9 $";
    rev = rev.substring("$Revision: ".length(), rev.length() - 2);

    String cvsTag = "$Name:  $";
    cvsTag = cvsTag.substring("$Name: ".length(), cvsTag.length() - 2);

    rev += ", cvs tag: '"+cvsTag+"'";

    return rev;
  }

  private Vector serverConnections = new Vector();

  private MessagingNetworkException lastError = null;

  ///** Fetched from server at the end of login() */
  //private boolean authorizationRequired;

  /**
    When running, Session.tick() method is called periodically
    by the dispatch thread.
    When not running, Session.tick() method is never called.
    <p>
    @see #isRunning()
    */
  private boolean running = false;
  private boolean shuttingDown = false;
  private final Object shuttingDownLock = new Object();

  /**
    Session's login id.
    */
  private final String loginId;

  /**
    Session's contact list items and their current status.
  */
  private Hashtable contactListLoginId2cli;

  /**
    Should never return null for contact list entries.
    Returning non-null means that the ContactListItem is in the contact list
    (possibly, with the offline status).
    Returning null means that the ContactListItem is not in the contact list.
  */
  public MessagingNetworkContactListItem getContactListItem(String dstLoginId)
  {
    return getContactListItem0(dstLoginId);
  }

  private ContactListItem getContactListItem0(String dstLoginId)
  {
    ContactListItem cli = (ContactListItem) contactListLoginId2cli.get(dstLoginId);
    return (cli == null || cli.isIgnored() ? null : cli);
  }

  private int getContactListItemStatus(String dstLoginId)
  {
    ContactListItem cli = getContactListItem0(dstLoginId);
    if (cli == null)
      return StatusUtil.NATIVE_STATUS_OFFLINE;
    else
      return cli.getStatusNative();
  }

  public Enumeration getContactListItems()
  {
    return contactListLoginId2cli.elements();
  }

  /**
    Session's current status.
  */
  private int status_Native = StatusUtil.NATIVE_STATUS_OFFLINE;

  /**
    Lock object that is used to synchronize login and logout operations.
    */
  private final Object logoutLock = new Object();

  /**
    Creates new instance.
    <p>Properties config file is loaded in the static initializer.
  */
  public Session(String loginId)
  throws MessagingNetworkException
  {
    log("new Session("+loginId+")");
    LoginIdUtil.checkValid_Fatal(loginId);
    this.loginId = LoginIdUtil.normalize(loginId);
  }

  public boolean contactListItemExists(String dstLoginId)
  throws MessagingNetworkException
  {
    return (getContactListItem0(dstLoginId) != null);
  }

  public void addToContactList(String dstLoginId, PluginContext ctx)
  throws MessagingNetworkException
  {
    addToContactList(dstLoginId, true, ctx);
  }
  /**
    Adds. Throws MessagingNetworkException if not connected.
  */
  public void addToContactList(String dstLoginId, boolean requireOnline, PluginContext ctx)
  throws MessagingNetworkException
  {
    log("addToContactList()");
    try
    {
      LoginIdUtil.checkValid_Ignorable(dstLoginId);
      dstLoginId = LoginIdUtil.normalize(dstLoginId);
      if (requireOnline)
        ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_ADD_TO_CONTACT_LIST_WHILE_OFFLINE);
      if (this.loginId.equals(dstLoginId))
        throw new MessagingNetworkException(
          "cannot add yourself to a contact list",
          MessagingNetworkException.CATEGORY_STILL_CONNECTED,
          MessagingNetworkException.ENDUSER_CANNOT_ADD_YOURSELF_TO_CONTACT_LIST);
      if (getContactListItem0(dstLoginId) != null)
      {
        log(dstLoginId + " already in a contact list, ignored");
        return;
      }
      boolean ignored = isContactListItemIgnored(dstLoginId);
      ignoreContactListItem(dstLoginId, false, false, ctx);
      if (!ignored)
        getDS().addRemoveSingleContactListItem(dstLoginId, true, Session.this, ctx);
      else
      {
        //fire status of previously ignored entity
        if (this.status_Native != StatusUtil.NATIVE_STATUS_OFFLINE)
        {
          ContactListItem cli = getContactListItem0(dstLoginId);
          int newStatus_Mim = StatusUtil.translateStatusNativeToMim_cl_entry(cli.getStatusNative());
          if (MessagingNetwork.STATUS_OFFLINE != newStatus_Mim)
          {
            fireContactListEntryStatusChangeMim_Uncond(
              dstLoginId, newStatus_Mim, ctx,
              MessagingNetworkException.CATEGORY_STILL_CONNECTED,
              "added to contact list");
          }
        }
      }
    }
    catch (Exception ex)
    {
      handleException(ex, "addToContactList", ctx);
    }
  }

  public boolean isContactListItemIgnored(String dst)
  {
    ContactListItem cli = (ContactListItem) contactListLoginId2cli.get(dst);
    return (cli != null && cli.isIgnored());
  }

  public void ignoreContactListItem(String dst, boolean ignore, boolean remove, PluginContext ctx)
  throws MessagingNetworkException
  {
    try
    {
      dst = LoginIdUtil.normalize(dst);
      //ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_ADD_TO_CONTACT_LIST_WHILE_OFFLINE);
      if(this.loginId.equals(dst)) return;
      if (remove)
        contactListLoginId2cli.remove(dst);
      else
      {
        ContactListItem cli = (ContactListItem) contactListLoginId2cli.get(dst);
        if (cli == null)
          contactListLoginId2cli.put(dst, makeContactListItem(dst, ignore));
        else
          cli.setIgnored(ignore);
      }
    }
    catch (Exception ex)
    {
      handleException(ex, "ignoreCLI", ctx);
    }
  }

  public void removeFromContactList(String dstLoginId, PluginContext ctx)
  throws MessagingNetworkException
  {
    log("removeFromContactList()");
    try
    {
      LoginIdUtil.checkValid_Ignorable(dstLoginId);
      dstLoginId = LoginIdUtil.normalize(dstLoginId);
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_REMOVE_FROM_CONTACT_LIST_WHILE_OFFLINE);

      if (this.loginId.equals(dstLoginId)
      || contactListLoginId2cli.get(dstLoginId) == null)
      {
        log(dstLoginId + " already not in a contact list, ignored");
        return;
      }
      contactListLoginId2cli.remove(dstLoginId);
      getDS().addRemoveSingleContactListItem(dstLoginId, false, Session.this, ctx);
    }
    catch (Exception ex)
    {
      handleException(ex, "removeFromContactList", ctx);
    }
  }

  private ContactListItem makeContactListItem(String dstLoginId, boolean ignored)
  {
    return new ContactListItem(this, dstLoginId, ignored);
  }

  private void ASSERT_LOGGED_IN(int endUserOperationErrorCode)
  throws MessagingNetworkException
  {
    MLang.EXPECT(
      status_Native != StatusUtil.NATIVE_STATUS_OFFLINE,
      "Please login first.  Status cannot be offline to perform this operation.",
      MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
      endUserOperationErrorCode);
  }

  /*
    Converts a byte array into string.
  */
  public static String byteArray2string(byte[] ba)
  {
    return byteArray2string(ba, 0, ba.length);
  }

  private static String fileEncoding = System.getProperty("file.encoding");

  /*
    Converts portion of a byte array into string.
  */
  public static String byteArray2string(byte[] ba, int ofs, int len)
  {
    if (len == 0) return "";

    //changed by Antich to test encoding problems - TEMPORARY SOLUTION!!
    try
    {
      //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(
        //"Constructing message using encoding " + fileEncoding +
        //", msgBytes.length="+len);
      return new String(ba, ofs, len, fileEncoding);
    }
    catch (Exception e)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("Constructing message, reverted to default enc", e);
      return new String(ba, ofs, len);
    }
  }

  public final void handleException(Throwable tr, String processName, PluginContext ctx)
  throws MessagingNetworkException
  {
    handleException(tr, processName, ctx, true);
  }

  public final void eatException(Throwable ex, String processName, PluginContext ctx)
  {
    try
    {
      handleException(ex, processName, ctx, false);
    }
    catch (Exception exx)
    {
    }
  }

  private final void handleException(Throwable tr, String processName, PluginContext ctx, boolean rethrow)
  throws MessagingNetworkException
  {
    handleException(tr, processName, ctx, rethrow, false);
  }

  public final void handleException(Throwable tr, String processName, PluginContext ctx, boolean rethrow, boolean skipShutdown)
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
    if (tr instanceof ArrayIndexOutOfBoundsException)
      ex = new MessagingNetworkException(
        tr.toString(),
        MessagingNetworkException.CATEGORY_STILL_CONNECTED,
        MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF);
    else
      ex = new MessagingNetworkException(
        "unknown error: "+tr.getMessage(),
        MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
        MessagingNetworkException.ENDUSER_MIM_BUG_UNKNOWN_ERROR);

    if (ex.getLogger() != MessagingNetworkException.CATEGORY_STILL_CONNECTED)
    {
      setLastError(ex);
      if (!skipShutdown)
        shutdown(ctx, lastError.getLogger(), lastError.getMessage(), lastError.getEndUserReasonCode());
      if (rethrow)
        throw new MessagingNetworkException(
          "Error while " + processName + ": " + lastError.getMessage(),
          lastError.getLogger(),
          lastError.getEndUserReasonCode());
    }
    else
    {
      if (rethrow)
        throw new MessagingNetworkException(
          "Error while " + processName + ": " + ex.getMessage(),
          ex.getLogger(),
          ex.getEndUserReasonCode());
    }
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

  public String getLastErrorMessage()
  {
    synchronized (lastErrorLock)
    {
      if (lastError == null) return null;
      return lastError.getMessage();
    }
  }

  public int getLastErrorLogger()
  {
    synchronized (lastErrorLock)
    {
      if (lastError == null) return MessagingNetworkException.CATEGORY_NOT_CATEGORIZED;
      return lastError.getLogger();
    }
  }

  public int getLastErrorEndUserReason()
  {
    synchronized (lastErrorLock)
    {
      if (lastError == null) return MessagingNetworkException.ENDUSER_NO_ERROR;
      return lastError.getEndUserReasonCode();
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

  public void throwLastErrorOrCreateThrowLastError(String exceptionMessage, int reasonLogger, int endUserReasonCode)
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

  public void fireSystemNotice(String errorMessage, PluginContext context)
  {
    log("fireSystemNotice: " + errorMessage);
    try
    {
      context.getMSNMessagingNetwork().fireMessageReceived("0", loginId, errorMessage);
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("error while firing system notice to messaging network listeners", ex);
    }
  }

  /*
  private void fireAuthRequestReceived(String from, String reason, PluginContext context)
  {
    log("incoming authrequest: "+reason);
    try
    {
      context.getMSNMessagingNetwork().fireAuthRequestReceived(from, loginId, reason);
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("error while firing AuthRequestReceived to messaging network listeners", ex);
    }
  }

  private void fireAuthReplyReceived(String from, boolean grant, PluginContext context)
  {
    log("incoming auth reply; granted: "+grant);
    try
    {
      context.getMSNMessagingNetwork().fireAuthReplyReceived(from, loginId, grant);
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("error while firing AuthReplyReceived to messaging network listeners", ex);
    }
  }
  */

  private static boolean isSenderValid(String loginId, String infoToBeIgnored)
  {
    try
    {
      LoginIdUtil.checkValid_Ignorable(loginId);
      return true;
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("invalid sender loginId, "+infoToBeIgnored+" ignored", ex);
      return false;
    }
  }

  public void fireMessageReceived(String senderLoginId, String msg, PluginContext ctx)
  {
    if (isSenderValid(senderLoginId, "incoming message="+StringUtil.toPrintableString(msg)))
      ctx.getMSNMessagingNetwork().fireMessageReceived(LoginIdUtil.normalize(senderLoginId), loginId, msg);
  }

  protected static void checkInterrupted()
  throws InterruptedException
  {
    if (Thread.currentThread().isInterrupted())
      throw new InterruptedException();
  }

  public int getContactStatus_Native(String dstLoginId, PluginContext ctx) throws MessagingNetworkException
  {
    log("getContactStatus_Native");
    try
    {
      LoginIdUtil.checkValid_Ignorable(dstLoginId);
      return getContactListItemStatus(dstLoginId);
    }
    catch (Exception ex)
    {
      handleException(ex, "getContactStatus", ctx);
      return StatusUtil.NATIVE_STATUS_OFFLINE;
    }
  }

  public final String getLoginId()
  {
    return loginId;
  }

  public int getStatus_Native()
  {
    return status_Native;
  }

  /**
    This read-only attribute indicates if this Session's
    tick(PluginContext) method should be called by
    ServeSessionsThread's incoming data dispatch loop.
    <p>
    If and only if the Session is running, tick(PluginContext)
    will be called.
    <p>
    This method returns false while login sequence, and
    after session death, and returns true between these
    Session lifetime events.

    @see #tick(PluginContext)
    @see ServeSessionsThread
  */
  public boolean isRunning()
  {
    synchronized (logoutLock)
    {
      return running;
    }
  }

  protected void log(String s)
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("org.openmim.msn [" + loginId + "]: " + s);
  }

  private DS ds;

  public void add(ServerConnection sc)
  {
    synchronized (logoutLock)
    {
      serverConnections.addElement(sc);
    }
    synchronized (shuttingDownLock)
    {
      if (shuttingDown) sc.close(this);
    }
  }

  void setDS(DS ds)
  {
    this.ds = ds;
  }

  public void login_Native(final String password, String[] contactList, int status_Native, PluginContext ctx)
  throws MessagingNetworkException
  {
    log("login() start");
    final String ERRMSG_PREFIX = "Error logging in: ";
    try
    {
      synchronized (shuttingDownLock) { shuttingDown = false; }
      setRunning(false);
      synchronized (lastErrorLock) { lastError = null; }

      MLang.EXPECT_NOT_NULL_NOR_EMPTY(
        password, "password",
        MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
        MessagingNetworkException.ENDUSER_MIM_BUG_PASSWORD_CANNOT_BE_NULL_NOR_EMPTY);
      MLang.EXPECT(
        password.length() < 1024, "password.length() must be < 1024, but it is "+password.length(),
        MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
        MessagingNetworkException.ENDUSER_PASSWORD_IS_TOO_LONG);

      if (!(  status_Native != StatusUtil.NATIVE_STATUS_OFFLINE  )) throw new AssertException("StatusUtil.NATIVE_STATUS_OFFLINE should never happen as a login_Native() argument.");

      if (contactListLoginId2cli == null)
        contactListLoginId2cli = new Hashtable(contactList.length);
      setReconnectData(status_Native, contactListLoginId2cli);

      //setRunning(true);
      DS ds = DS.login(
        MSNMessagingNetwork.REQPARAM_LOGIN_HOST,
        MSNMessagingNetwork.REQPARAM_LOGIN_PORT,
        loginId, password, Session.this, ctx);
      synchronized (shuttingDownLock)
      {
        if (shuttingDown)
        {
          ds.close(this);
          throwLastError();
        }
      }

      //syncContactList MUST be BEFORE before going online, to
      //store contact statuses.
      getDS().syncContactList(this, contactList, ctx);
      setStatus_Native_Internal(status_Native, true, ctx, MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, null, MessagingNetworkException.ENDUSER_NO_ERROR);

      //getDS().syncAccountProperties(this, ctx);

      log("login() finished (success)");
    }
    catch (ArrayIndexOutOfBoundsException ex1)
    {
      log("login() finished (failed)");
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("", ex1);
      handleException(new MessagingNetworkException(
        "unknown error: "+ex1,
        MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
        MessagingNetworkException.ENDUSER_MIM_BUG_UNKNOWN_ERROR), "login", ctx);
    }
    catch (Exception ex)
    {
      log("login() finished (failed)");
      handleException(ex, "login", ctx);
    }
  }

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

  public void fireYouAreAddedToContactList(String senderLoginId, String nick, PluginContext ctx)
  throws MessagingNetworkException, IOException
  {
    fireMessageReceived(senderLoginId, "You are added to a contact list by "+nick, ctx);
  }

  public void sendMessage(String dstLoginId, String text, PluginContext ctx)
  throws MessagingNetworkException
  {
    log("sendMessage() start, dst="+dstLoginId+",\r\ntext="+StringUtil.toPrintableString(text));
    try
    {
      LoginIdUtil.checkValid_Ignorable(dstLoginId);
      dstLoginId = LoginIdUtil.normalize(dstLoginId);
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_SEND_TEXT_MESSAGE_WHILE_OFFLINE);
      if ((text) == null) Lang.ASSERT_NOT_NULL(text, "text");
      if (this.loginId.equals(dstLoginId))
        throw new MessagingNetworkException(
          "cannot send msg to yourself",
          MessagingNetworkException.CATEGORY_STILL_CONNECTED,
          MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF);
/*
      //the following check is removed since:
      //when receipient is not in local contact list,
      //it will appear offline regardless of its real status.

      if (isOffline(dstLoginId, ctx))
        throw new MessagingNetworkException(
          "recipient is offline",
          MessagingNetworkException.CATEGORY_STILL_CONNECTED,
          MessagingNetworkException.ENDUSER_CANNOT_COMPLETE_REQUEST_RECIPIENT_IS_OFFLINE);
*/
      getSSS(dstLoginId, ctx).sendMsg(text); //getSS negotiates (if needed) & returns SS
      log("sendMessage() finished (success)");
    }
    catch (Exception ex)
    {
      log("sendMessage() finished (failed)");
      handleException(ex, "sendMessage", ctx);
    }
  }

  public boolean isOffline(String dst, PluginContext pctx)
  throws MessagingNetworkException
  {
    return (getContactStatus_Native(dst, pctx) == StatusUtil.NATIVE_STATUS_OFFLINE);
  }

  private Hashtable dst2sss = new Hashtable();

  /** Returns a Switchboard Server session for a given dstLoginId. */
  public SSS getSSS(String dst, PluginContext pctx)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    SSS sss = peekSSS(dst);
    if (sss == null)
    {
      sss = getDS().createSSS(this, dst, pctx);
      addSSS(dst, sss);
    }
    return sss;
  }

  public SSS peekSSS(String dst)
  {
    synchronized (logoutLock)
    {
      return (SSS) dst2sss.get(dst);
    }
  }

  public SSS removeSSS(SSS sss)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    synchronized (logoutLock)
    {
      SSS sss1 = (SSS) dst2sss.remove(sss.getDst());
      serverConnections.remove(sss);
      return sss1;
    }
  }

  /** Returns false if SSS for dst already exists */
  public boolean addSSS(String dst, SSS sss)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    synchronized (logoutLock)
    {
      SSS sss1 = (SSS) dst2sss.get(dst);
      if (sss1 == null)
      {
        dst2sss.put(dst, sss);
        add(sss);
        return true;
      }
      return false;
    }
  }

  /**
    Using the connection of srcLoginId loginId,
    retrieves the authorizationRequired property of dstLoginId loginId
    from the msn server.
  */
  public boolean isAuthorizationRequired(String dstLoginId, PluginContext ctx)
  throws MessagingNetworkException
  {
    log("isAuthorizationRequired() start, dst="+dstLoginId);
    MessagingNetworkException.throwOperationNotSupported("isAuthorizationRequired");
    return false;
    /* ###
    try
    {
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_OPERATION_WHILE_OFFLINE);
      int dstLoginId = LoginIdUtil.checkValid_Ignorable(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
      boolean b = isAuthorizationRequired0(dstLoginId, ctx);
      log("isAuthorizationRequired() finished (success), authorizationRequired="+b);
      return b;
    }
    catch (Exception ex)
    {
      log("isAuthorizationRequired() finished (failed)");
      handleException(ex, "isAuthorizationRequired", ctx);
      log("isAuthorizationRequired(): assuming authorizationRequired=false");
      return false;
    }
    */
  }

  /**
    Using the connection of srcLoginId loginId,
    sends an auth request to dstLoginId loginId with reason reason,
    to ask if dstLoginId allows to add himself
    to srcLoginId's contact list.
    Reason can be null.
  */
  public void authorizationRequest(String dstLoginId, String text, PluginContext ctx)
  throws MessagingNetworkException
  {
    MessagingNetworkException.throwOperationNotSupported("authorizationRequest");
    /* ###
    log("authorizationRequest() start, dst="+dstLoginId);
    try
    {
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_OPERATION_WHILE_OFFLINE);
      int dstLoginId = LoginIdUtil.checkValid_Ignorable(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
      if (text == null) text = "";
      authorizationRequest0(dstLoginId, text, ctx);
      log("authorizationRequest() finished (success)");
    }
    catch (Exception ex)
    {
      log("authorizationRequest() finished (failed)");
      handleException(ex, "authorizationRequest", ctx);
    }
    */
  }

  /**
    Using the connection of srcLoginId loginId,
    sends a reply to preceding auth request of dstLoginId loginId,
    to state that srcLoginId grants or denies
    dstLoginId's request to add srcLoginId to dstLoginId's contact list.
  */
  public void authorizationResponse(String dstLoginId, boolean grant, PluginContext ctx)
      throws MessagingNetworkException
  {
    MessagingNetworkException.throwOperationNotSupported("authorizationResponse");
    /* ###
    log("authorizationResponse() start, dst="+dstLoginId);
    try
    {
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_OPERATION_WHILE_OFFLINE);
      int dstLoginId = LoginIdUtil.checkValid_Ignorable(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
      authorizationResponse0(dstLoginId, grant, ctx);
      log("authorizationResponse() finished (success)");
    }
    catch (Exception ex)
    {
      log("authorizationResponse() finished (failed)");
      handleException(ex, "authorizationResponse", ctx);
    }
    */
  }

  public void sendContacts(String dstLoginId, String[] nicks, String[] loginIds, PluginContext ctx)
  throws MessagingNetworkException
  {
    MessagingNetworkException.throwOperationNotSupported("sendContacts");
    /* ###
    log("sendContacts() start");
    try
    {
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_SEND_CONTACTS_WHILE_OFFLINE);
      int dstLoginId = LoginIdUtil.checkValid_Ignorable(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
      if ((nicks) == null) Lang.ASSERT_NOT_NULL(nicks, "nicks");
      if ((loginIds) == null) Lang.ASSERT_NOT_NULL(loginIds, "loginIds");
      sendContacts0(getBosConnNotNull(), dstLoginId, nicks, loginIds, ctx);
      log("sendContacts() finished (success)");
    }
    catch (Exception ex)
    {
      log("sendContacts() finished (failed)");
      handleException(ex, "sendContacts", ctx);
    }
    */
  }


  /** Status of contact list entries */
  public void setContactStatus_Native(String dstLoginId, int newStatus_Native, PluginContext ctx,
    int reasonLogger, String reasonMessage)
  throws MessagingNetworkException
  {
    log("setContactStatus_Native");
    try
    {
      LoginIdUtil.checkValid_Ignorable(dstLoginId);
      dstLoginId = LoginIdUtil.normalize(dstLoginId);
      if (!(  !this.loginId.equals(dstLoginId)  )) throw new AssertException("this.loginId.equals(dstLoginId) must be false here");
      ContactListItem cli = (ContactListItem) contactListLoginId2cli.get(dstLoginId);
      if (cli == null)
      {
        log("setContactStatus_Native(): '" + dstLoginId + "' is not on contact list, statusNative change ignored");
        return;
      }
      int oldStatus_Native = cli.getStatusNative();
      cli.setStatusNative(newStatus_Native); //marks it as NOT obsolete
      if (cli.isIgnored()) return;
      if (oldStatus_Native == newStatus_Native)
        return;
      //ignore any status events if this Session is already logged out
      if (this.status_Native != StatusUtil.NATIVE_STATUS_OFFLINE)
      {
        int oldStatus_Mim = StatusUtil.translateStatusNativeToMim_cl_entry(oldStatus_Native);
        int newStatus_Mim = StatusUtil.translateStatusNativeToMim_cl_entry(newStatus_Native);
        if (oldStatus_Mim != newStatus_Mim)
        {
          fireContactListEntryStatusChangeMim_Uncond(dstLoginId, newStatus_Mim, ctx, reasonLogger, reasonMessage);
        }
        else
          log("setStatus request ignored by msn plugin: attempted to set the same status");
      }
      else
        log("setStatus request ignored by msn plugin: we are offline, hence silent");
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
    ctx.getMSNMessagingNetwork().fireStatusChanged_Mim_Uncond(this.loginId, dstLoginId, newStatus_Mim,
      reasonLogger, reasonMessage, MessagingNetworkException.ENDUSER_NO_ERROR);
  }

  protected void fireSessionStatusChangeMim_Uncond(int newStatus_Mim, PluginContext ctx,
    int reasonLogger, String reasonMessage, int endUserReasonCode)
  throws MessagingNetworkException
  {
    ctx.getMSNMessagingNetwork().fireStatusChanged_Mim_Uncond(this.loginId, this.loginId, newStatus_Mim,
      reasonLogger, reasonMessage, endUserReasonCode);
  }

  void setRunning(boolean newRunning)
  {
    synchronized (logoutLock)
    {
      running = newRunning;
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("setRunning("+newRunning+")");
    }
  }
  /**
    Only works if already logged in, otherwise throws an AssertException.
    <p>
    Does not allow setting an OFFLINE status, since
    MSNMessagingNetwork.logout() should be called instead.
  */
  public void setStatus_Native_External(int newStatus, PluginContext ctx)
  throws MessagingNetworkException
  {
    log("setStatus_Native_External to " + StatusUtil.translateStatusNativeToString(newStatus) + //
      " (current status: " + StatusUtil.translateStatusNativeToString(this.status_Native) + ")");
    try
    {
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_MIM_BUG_LOGIN_FIRST_CANNOT_PERFORM_SET_STATUS_NONOFFLINE_WHILE_OFFLINE);
      if (!(  newStatus != StatusUtil.NATIVE_STATUS_OFFLINE  )) throw new AssertException("NATIVE_STATUS_OFFLINE should never happen here; use Session.logout() instead.");
      setStatus_Native_Internal(newStatus, true, ctx, MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, null, MessagingNetworkException.ENDUSER_NO_ERROR);
    }
    catch (Exception ex)
    {
      handleException(ex, "setStatus_Native_External", ctx);
    }
  }

  /** Status of the session itself */
  public void setStatus_Native_Internal(final int newStatus_Native, boolean sendToServer, PluginContext ctx,
    int reasonLogger, String reasonMessage, int endUserReasonCode)
  throws MessagingNetworkException
  {
    try
    {
      if (sendToServer)
        StatusUtil.EXPECT_IS_NATIVE_STATUS(newStatus_Native);
      int oldStatus_Mim;
      int newStatus_Mim;
      synchronized (logoutLock)
      {
        final int oldStatus_Native = this.status_Native;
        if (oldStatus_Native == newStatus_Native)
          return;
        this.status_Native = newStatus_Native;
        oldStatus_Mim = StatusUtil.translateStatusNativeToMim_self(oldStatus_Native);
        newStatus_Mim = StatusUtil.translateStatusNativeToMim_self(newStatus_Native);
        if (oldStatus_Mim != newStatus_Mim)
        {
          if (oldStatus_Native != StatusUtil.NATIVE_STATUS_OFFLINE)
            setReconnectData(oldStatus_Native, contactListLoginId2cli);
          if (newStatus_Native == StatusUtil.NATIVE_STATUS_OFFLINE)
          {
            //contactListLoginId2cli = null;
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
          log("setStatus request ignored by msn plugin: attempted to set the same status");
      }

      if (newStatus_Native != StatusUtil.NATIVE_STATUS_OFFLINE)
      {
        synchronized (this)
        {
          if (sendToServer)
          {
            getDS().setClientStatus(newStatus_Native, Session.this, ctx);
          }
        }
      }
    }
    catch (Exception ex)
    {
      handleException(ex, "setStatus_Native_Internal", ctx);
    }
  }

  private DS getDS()
  throws IOException
  {
    DS ds = this.ds;
    if (ds == null) throw new IOException("connection closed");
    return ds;
  }

  private DS peekDS()
  {
    return this.ds;
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
    log("shutdown("+reasonMessage+", "+MessagingNetworkException.getEndUserReasonMessage(endUserReasonCode)+")");
    //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("shutdown", new Exception("dumpStack"));
    try
    {
      setRunning(false);
      log("closing all connections");
      synchronized (logoutLock)
      {
        logoutLock.notifyAll();
        Enumeration e = serverConnections.elements();
        while (e.hasMoreElements())
        {
          ServerConnection sc = (ServerConnection) e.nextElement();
          sc.close(this);
        }
        serverConnections.clear();
      }
      log("closing all done.");
      setStatus_Native_Internal(StatusUtil.NATIVE_STATUS_OFFLINE, false, ctx, reasonLogger, reasonMessage, endUserReasonCode);
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ex in shutdown(), ignored", ex);
    }
  }

  private void shutdownAt(long stopTime, String operationDetails, PluginContext ctx)
  throws MessagingNetworkException, InterruptedException
  {
    checkShuttingdown();

    if (System.currentTimeMillis() >= stopTime)
    {
      throw new MessagingNetworkException(
        "msn server operation timed out: " + operationDetails,
        MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
        MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_MESSAGING_OPERATION_TIMEOUT);
    }
  }

/* ###  void sleep(String activityName, long millis)
  throws MessagingNetworkException, InterruptedException
  {
    log(activityName + ": sleeping " + ((millis / 100) / (float) 10) + " sec.");
    synchronized (logoutLock)
    {
      logoutLock.wait(millis);
    }
    checkShuttingdown();
  }
  */

  public static byte[] string2byteArray(String s)
  {
    try
    {
      return s.getBytes(fileEncoding);
    }
    catch (java.io.UnsupportedEncodingException ex)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error(ex.getMessage(), ex);
    }
    return s.getBytes();
  }

  private boolean scheduledSendStatus = false;

  public void setScheduledSendStatus(boolean scheduled)
  {
    synchronized (scheduledSendStatusLock)
    {
      this.scheduledSendStatus = scheduled;
    }
  }

  public boolean isScheduledSendStatus()
  {
    synchronized (scheduledSendStatusLock)
    {
      return scheduledSendStatus;
    }
  }

  private final Object scheduledSendStatusLock = new Object();

  public Object getFireStatusLock()
  {
    return scheduledSendStatusLock;
  }

  public boolean isTickCallNeeded(PluginContext ctx)
  {
    checkSendDeferredStatus(ctx);
    return true;
    /*
    try
    {

      ServerConnection ds = this.ds;
      if (ds == null
      || ds.isClosed())
        return true;

      //### checkSendDeferredStatus(ctx);

      MSNMessagingNetwork plugin = ctx.getMSNMessagingNetwork();

      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ds.available() = "+ds.available());
      return ds.available() > 0
        // ### || plugin.isKeepAlivesUsed()) ...
        ;
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("", ex);
      return true;
    }
    */
  }

  private void checkSendDeferredStatus(PluginContext ctx)
  {
    try {
      synchronized (getFireStatusLock()) {
        if (scheduledSendStatus) {
          scheduledSendStatus = false;
          long now = System.currentTimeMillis();
          Enumeration e = getContactListItems();
          while (e.hasMoreElements())
          {
            ContactListItem cli = (ContactListItem) e.nextElement();
            long cliTime = cli.getScheduledStatusChangeSendTimeMillis();
            if (now >= cliTime)
            {
              if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("status delivery delay expired, delivering.");
              cli.setScheduledStatusChangeSendTimeMillis(Long.MAX_VALUE);
              ctx.getMSNMessagingNetwork().fireStatusChanged_Mim_Uncond(
                this.loginId,
                cli.getDstLoginId(),
                StatusUtil.translateStatusNativeToMim_cl_entry(cli.getStatusNative()),
                MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
                null,
                MessagingNetworkException.ENDUSER_NO_ERROR);
            }
            else
            if (cliTime != Long.MAX_VALUE && now < cliTime)
            {
              scheduledSendStatus = true;
            }
          }
        }
      }
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ex while scheduledSendStatus, ex ignored", ex);
    }
  }


  /**
    Called periodically by TickTask. //ResourceManager thread(s).
    Returns true if and only if this method should be called as soon as possible
    (e.g. if the socket input stream has data waiting).
  */
  boolean tick(PluginContext ctx)
  {
    //log("Session.tick()");
    try
    {
      // synchronized (this)
      //{
        checkShuttingdown();

        getDS().available(); //to check for exceptions

        boolean dataWaiting = false;
        Vector copy = (Vector) serverConnections.clone();

        Vector closed = new Vector(copy.size());
        for (int i = 0; i < copy.size(); ++i)
        {
          ServerConnection sc = (ServerConnection) copy.elementAt(i);
          if (sc.isClosed())
          {
            closed.addElement(sc);
            if (sc instanceof SSS)
            {
              SSS sss = (SSS) sc;
              removeSSS(sss); //remove closed SSS
            }
          }
          else
          {
            try
            {
              if (sc.available() > 0)
              {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                sc.tick(Session.this, ctx);
                if (!isRunning()) return false;
                dataWaiting = dataWaiting || (sc.available() > 0);
              }
              else
              if (sc instanceof SSS)
              {
                SSS sss = (SSS) sc;
                if (sss.closeOnIdleAllowed()
                    &&
                    System.currentTimeMillis() >
                    sss.getLastActivityTime() +
                    MSNMessagingNetwork.REQPARAM_SSS_MAXIMUM_IDLE_TIME_SECONDS * 1000)
                {
                  sss.BYE(Session.this);
                  sss.close("closing idle SSS", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_NO_ERROR);
                  removeSSS(sss); //close idle SSS
                }
              }
            }
            catch (IOException ex)
            {
              if (sc == peekDS())
                throw ex;
              else
              {
                if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(sc.getServerName()+": io exception on non-NS server, closing connection", ex);
                sc.close("io exception on non-NS server: "+ex, MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_NO_ERROR);
              }
            }
          }
        }

        serverConnections.removeAll(closed);
        return dataWaiting;
        //MSNMessagingNetwork plugin = ctx.getMSNMessagingNetwork();
        /* ###if (plugin.isKeepAlivesUsed())
        {
          if (lastKeepaliveMillis + plugin.getKeepAlivesIntervalMillis() <= System.currentTimeMillis())
          {
            sendKeepAlive(bosconn, ctx);
            lastKeepaliveMillis = System.currentTimeMillis();
          }
        } */
      //}
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
      return status_Native != StatusUtil.NATIVE_STATUS_OFFLINE;
    }
  }

  private final TickTask tickTask = new TickTask(this);

  class TickTask implements Task
  {
    private final Session ses;

    PluginContext ctx;


    TickTask(Session ses)
    {
      this.ses = ses;
    }

    public void execute() throws Exception
    {
      try
      {
        //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("tick task enter("+TickTask.this.ses.getLoginId()+")");
        //a task will take one Session instance, {i=0}
        //cycle {it; i++}
        //until {tick() will return false || i > 30}, and then exit.

        int i = 0;
        for (;;)
        {
          if (Thread.currentThread().isInterrupted()
          || ses.tick(ctx) == false
          || i > 30)
            return;
          i++;
        }
      }
      finally
      {
        //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("tick task leave("+TickTask.this.ses.getLoginId()+")");
        TickTask.this.ses.unlockBusy();
      }
    }

    public String getId()
    {
      return "stask";//+(dbgCount++);
    }

    public int getState()
    {
      return CREATED;
    }

    public void terminate()
    {
    }

    public long getStartTime()
    {
      return 0;
    }

    public boolean terminatable()
    {
      return false;
    }
  };

  public final Task getTickTask(PluginContext ctx)
  {
    tickTask.ctx = ctx;
    return tickTask;
  }

  protected boolean isTickRunningException = false;

  public UserDetailsImpl getUserDetails(String dstLoginId, PluginContext ctx)
  throws MessagingNetworkException
  {
    MessagingNetworkException.throwOperationNotSupported("getUserDetails");
    return null;
    /* ###
    log("fetchUserDetails() start, dst "+dstLoginId);
    try
    {
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_GET_USER_DETAILS_WHILE_OFFLINE);

      return fetchUserDetails0(dstLoginId, ctx);
    }
    catch (Exception ex)
    {
      handleException(ex, "fetchUserDetails", ctx);
      //handleException always throws some exception
      throw new AssertException("this point should never be reached");
    }
    finally
    {
      log("fetchUserDetails() finish, dst "+dstLoginId);
    }
    */
  }

  public UserSearchResults searchUsers(
    String emailSearchPattern,
    String nickSearchPattern,
    String firstNameSearchPattern,
    String lastNameSearchPattern,
    PluginContext ctx) throws MessagingNetworkException
  {
    MessagingNetworkException.throwOperationNotSupported("searchUsers");
    return null;
    /**
    if (emailSearchPattern == null) emailSearchPattern = "";
    if (nickSearchPattern == null) nickSearchPattern = "";
    if (firstNameSearchPattern == null) firstNameSearchPattern = "";
    if (lastNameSearchPattern == null) lastNameSearchPattern = "";

    emailSearchPattern=emailSearchPattern.trim();
    nickSearchPattern=nickSearchPattern.trim();
    firstNameSearchPattern=firstNameSearchPattern.trim();
    lastNameSearchPattern=lastNameSearchPattern.trim();

    log("searchUsers() start for "+
          "email '" + emailSearchPattern+
          "' nick '" + nickSearchPattern+
          "' fname '" + firstNameSearchPattern+
          "' lname '" + lastNameSearchPattern+"'");
    try
    {
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_SEARCH_USERS_WHILE_OFFLINE);

      return searchUsers0(
                emailSearchPattern,
                nickSearchPattern,
                firstNameSearchPattern,
                lastNameSearchPattern, ctx);
    }
    catch (Exception ex)
    {
      handleException(ex, "searchUsers", ctx);
      //handleException always throws some exception
      throw new AssertException("this point should never be reached");
    }
    finally
    {
      log("searchUsers() finish for "+
            "email '" + emailSearchPattern+
            "' nick '" + nickSearchPattern+
            "' fname '" + firstNameSearchPattern+
            "' lname '" + lastNameSearchPattern+"'");
    }
    */
  }

  /** Overridden by MSNReconnecting */
  protected void setReconnectData(int prevStatus_Native, Hashtable prevLoginIdInt2cli)
  {
  }

  protected void clearReconnectState()
  {
    contactListLoginId2cli = null;
  }

  private boolean busy = false;
  private final Object busyLock = new Object();

  /**
    Returns false if this Session had already been busy, true if it had not.
    Marks Session as busy.
  */
  public boolean lockBusy()
  {
    synchronized (busyLock)
    {
      if (this.busy) return false;
      this.busy = true;
      return true;
    }
  }

  public void unlockBusy()
  {
    synchronized (busyLock)
    {
      this.busy = false;
    }
  }

  public boolean isBusy()
  {
    synchronized (busyLock)
    {
      return busy;
    }
  }
}