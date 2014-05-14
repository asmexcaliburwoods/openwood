package org.openmim.msn;

import java.util.*;
import org.openmim.*;
import org.openmim.mn.MessagingNetwork;
import org.openmim.mn.MessagingNetworkException;
import org.openmim.icq.util.joe.*;
import org.openmim.icq.util.joe.jsync.*;

public class SessionReconnecting
extends Session
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(SessionReconnecting.class.getName());

  protected final static int S_NORMAL = 1;
  protected final static int S_RECONNECTING = 2;

  protected int state = S_NORMAL;
  protected final Object stateChangeLock = new Object();

  //must always be non-offline in the S_RECONNECTING state.
  protected int prevStatusNative = StatusUtil.NATIVE_STATUS_OFFLINE;
  protected Hashtable prevLoginId2cli = null;
  protected String prevPassword = null;
  protected PluginContext prevPluginContext = null;

  int reconnectManagerState;
  long scheduledReconnectTime;

  SessionReconnecting(String srcLoginId) throws MessagingNetworkException
  {
    super(srcLoginId);
    ReconnectManager.initReconnectManagerState(this);
  }

  private void setContactListStatusObsolete(PluginContext ctx) throws MessagingNetworkException
  {
    Hashtable prevLoginId2cli = this.prevLoginId2cli;
    log("setContactListStatusObsolete, ht size="+(prevLoginId2cli==null?null:""+prevLoginId2cli.size()));
    if (prevLoginId2cli == null) return;
    Enumeration e = prevLoginId2cli.elements();
    while (e.hasMoreElements())
    {
      ContactListItem cli = (ContactListItem) e.nextElement();
      cli.setStatusObsolete(true);
    }
  }

  private void fireContactListStatusObsoleteOffline(
    PluginContext ctx,
    int reasonLogger, String reasonMessage) throws MessagingNetworkException
  {
    Hashtable prevLoginId2cli = this.prevLoginId2cli;
    log("fireCLStatusObsoleteOffline, ht size="+(prevLoginId2cli==null?null:""+prevLoginId2cli.size()));
    if (prevLoginId2cli == null) return;
    Enumeration e = prevLoginId2cli.elements();
    while (e.hasMoreElements())
    {
      ContactListItem cli = (ContactListItem) e.nextElement();
      if (cli.isStatusObsolete())
      {
        int smim = StatusUtil.translateStatusNativeToMim_cl_entry(cli.getStatusNative());
        if (smim != MessagingNetwork.STATUS_OFFLINE)
        {
          log("fireClearCL "+cli.getDstLoginId()+" from "+StatusUtil.translateStatusNativeToString(cli.getStatusNative())+" to NATIVE_OFFLINE");
          cli.setStatusNative(StatusUtil.NATIVE_STATUS_OFFLINE);
          super.fireContactListEntryStatusChangeMim_Uncond(cli.getDstLoginId(), MessagingNetwork.STATUS_OFFLINE, ctx,
            reasonLogger, reasonMessage);
        }
        else
          log("fireClearCL "+cli.getDstLoginId()+" is already offline");
      }
      else
        log("fireClearCL "+cli.getDstLoginId()+" is already updated");
    }
  }

  protected void fireSessionStatusChangeMim_Uncond(
    int newStatusMim,
    PluginContext ctx,
    int reasonLogger, String reasonMessage, int endUserReasonCode) throws MessagingNetworkException
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("fireSessionStatusChangeMim_Uncond to "+
      StatusUtilMim.translateStatusMimToString(newStatusMim)+
      ", category="+MessagingNetworkException.getLoggerMessage(reasonLogger)+
      ", S_state="+state+", recMgrState="+reconnectManagerState);
    switch (state)
    {
      case S_NORMAL:
        if (newStatusMim == MessagingNetwork.STATUS_OFFLINE &&
            reasonLogger == MessagingNetworkException.CATEGORY_LOGGED_OFF_DUE_TO_NETWORK_ERROR &&
            isTickRunningException)
        {
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(getLoginId()+" changeSessionStatus event ignored, starting reconnecting");
          this.prevPluginContext = ctx;
          setState(S_RECONNECTING);
          scheduleReconnect(ctx);
        }
        else
          super.fireSessionStatusChangeMim_Uncond(newStatusMim, ctx,
            reasonLogger, reasonMessage, endUserReasonCode);
        break;
      case S_RECONNECTING:
        if (newStatusMim != MessagingNetwork.STATUS_OFFLINE)
        {
          //log("S_RECONN: before sync");
          synchronized (stateChangeLock)
          {
            //log("S_RECONN: in sync");
            int newStatusNative = StatusUtil.translateStatusMimToNative(newStatusMim);
            int prevStatusMim = StatusUtil.translateStatusNativeToMim_self(prevStatusNative);
            if (newStatusMim != prevStatusMim)
            {
              this.prevStatusNative = newStatusNative;
              log("S_RECONN: setStatus assigned, firing");
              super.fireSessionStatusChangeMim_Uncond(newStatusMim, ctx,
                reasonLogger, reasonMessage, endUserReasonCode);
            }
            else
              log("S_RECONN: duplicated setStatus ignored: oldmim="+
                StatusUtilMim.translateStatusMimToString(prevStatusMim)+
                " newmim="+StatusUtilMim.translateStatusMimToString(newStatusMim));
            if (super.getStatus_Native() != StatusUtil.NATIVE_STATUS_OFFLINE)
            {
              setContactListStatusObsolete(ctx);
            }
            else
              log("S_RECONN: setStatusObsolete skipped");
          }
        }
        else
          log("S_RECONN: setStatus OFFLINE ignored");
        break;
      default:
        Lang.ASSERT_FALSE("invalid state: "+state);
    }
  }

  protected void throwStillReconnecting() throws MessagingNetworkException
  {
    throw new MessagingNetworkException(
      "network error occured, please wait for the mim server to reconnect",
      MessagingNetworkException.CATEGORY_STILL_CONNECTED,
      MessagingNetworkException.ENDUSER_NETWORK_ERROR_RECONNECTING);
  }

  public void addToContactList(String dstLoginId, PluginContext ctx) throws MessagingNetworkException
  {
    switch (state)
    {
      case S_NORMAL:
        super.addToContactList(dstLoginId, ctx);
        break;
      case S_RECONNECTING:
        throwStillReconnecting();
      default:
        Lang.ASSERT_FALSE("invalid state: "+state);
    }
  }

  public int getStatus_Native()
  {
    switch (state)
    {
      case S_NORMAL:
        return super.getStatus_Native();
      case S_RECONNECTING:
        return this.prevStatusNative;
      default:
        Lang.ASSERT_FALSE("invalid state: "+state);
        return StatusUtil.NATIVE_STATUS_OFFLINE; //this is never reached
    }
  }

  public int getContactStatus_Native(String dstLoginId, PluginContext ctx) throws MessagingNetworkException
  {
    switch (state)
    {
      case S_NORMAL:
        return super.getContactStatus_Native(dstLoginId, ctx);
      case S_RECONNECTING:
        try
        {
          return getPrevContactListItemStatusNative(dstLoginId);
        }
        catch (Exception ex)
        {
          handleException(ex, "getContactStatus", ctx);
          return StatusUtil.NATIVE_STATUS_OFFLINE; //this is never reached
        }
      default:
        Lang.ASSERT_FALSE("invalid state: "+state);
        return StatusUtil.NATIVE_STATUS_OFFLINE; //this is never reached
    }
  }

  protected int getPrevContactListItemStatusNative(String dstLoginId)
  {
    ContactListItem cli = getPrevContactListItem(dstLoginId);
    if (cli == null)
      return StatusUtil.NATIVE_STATUS_OFFLINE;
    else
      return cli.getStatusNative();
  }

  protected ContactListItem getPrevContactListItem(String dstLoginId)
  {
    return (ContactListItem) prevLoginId2cli.get(dstLoginId);
  }

  public void login_Native(String password, String[] contactList, int statusNative, PluginContext ctx)
  throws MessagingNetworkException
  {
    this.prevPassword = password;
    switch (state)
    {
      case S_NORMAL:
        super.login_Native(password, contactList, statusNative, ctx);
        break;
      case S_RECONNECTING:
        throwStillReconnecting();
      default:
        Lang.ASSERT_FALSE("invalid state: "+state);
    }
  }

  public void removeFromContactList(String dstLoginId, PluginContext ctx) throws MessagingNetworkException
  {
    switch (state)
    {
      case S_NORMAL:
        super.removeFromContactList(dstLoginId, ctx);
        break;
      case S_RECONNECTING:
        throwStillReconnecting();
      default:
        Lang.ASSERT_FALSE("invalid state: "+state);
    }
  }

  public void sendMessage(String dstLoginId, String text, PluginContext ctx) throws MessagingNetworkException
  {
    switch (state)
    {
      case S_NORMAL:
        super.sendMessage(dstLoginId, text, ctx);
        break;
      case S_RECONNECTING:
        throwStillReconnecting();
      default:
        Lang.ASSERT_FALSE("invalid state: "+state);
    }
  }

  /** Change Session status to non-offline is synchronized by Session */
  public void setStatus_Native_External(int statusNative, PluginContext ctx) throws MessagingNetworkException
  {
    if (!(  statusNative != StatusUtil.NATIVE_STATUS_OFFLINE  )) throw new AssertException("statusNative cannot be StatusUtil.NATIVE_STATUS_OFFLINE here, but it is.");
    switch (state)
    {
      case S_NORMAL:
        super.setStatus_Native_External(statusNative, ctx);
        break;
      case S_RECONNECTING:
        int statusMim = StatusUtil.translateStatusNativeToMim_self(statusNative);
        fireSessionStatusChangeMim_Uncond(statusMim, ctx,
          MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
          "caller requested status change",
          MessagingNetworkException.ENDUSER_NO_ERROR);
        break;
      default:
        Lang.ASSERT_FALSE("invalid state: "+state);
    }
  }

  void logout(PluginContext ctx, int reasonLogger, String reasonMessage, int endUserReasonCode)
  throws MessagingNetworkException
  {
    super.clearReconnectState();
    switch (state)
    {
      case S_NORMAL:
        super.logout(ctx, reasonLogger, reasonMessage, endUserReasonCode);
        break;
      case S_RECONNECTING:
        synchronized (stateChangeLock)
        {
          setState(S_NORMAL);
          super.logout(ctx, reasonLogger, reasonMessage, endUserReasonCode);
          fireSessionStatusChangeMim_Uncond(MessagingNetwork.STATUS_OFFLINE, ctx,
            reasonLogger, reasonMessage, endUserReasonCode);
        }
        break;
      default:
        Lang.ASSERT_FALSE("invalid state: "+state);
    }
  }

  protected void setState(int state)
  {
    synchronized (stateChangeLock)
    {
      if (this.state == state) return;

      if ((prevPluginContext) == null) Lang.ASSERT_NOT_NULL(prevPluginContext, "prevPluginContext");

      switch (state)
      {
        case S_NORMAL:
          ReconnectManager.initReconnectManagerState(this);
          prevStatusNative = StatusUtil.NATIVE_STATUS_OFFLINE;
          cancelReconnect(prevPluginContext);
          break;
        case S_RECONNECTING:
          if (!(  prevStatusNative != StatusUtil.NATIVE_STATUS_OFFLINE  )) throw new AssertException("prevStatusNative cannot be StatusUtil.NATIVE_STATUS_OFFLINE here, but it is.");
          if ((prevLoginId2cli) == null) Lang.ASSERT_NOT_NULL(prevLoginId2cli, "prevLoginId2cli");
          if ((prevPassword) == null) Lang.ASSERT_NOT_NULL(prevPassword, "prevPassword");
          //scheduleReconnect(prevPluginContext);
          break;
        default:
          Lang.ASSERT_FALSE("invalid state: "+state);
      }
      this.state = state;
    }
  }

/*
  protected void handleException(Throwable tr, String processName, PluginContext ctx)
  throws MessagingNetworkException
  {
    try
    {
      super.handleException(tr, processName, ctx);
    }
    catch (MessagingNetworkException ex)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(getLoginId() + " reconnect failed", ex);

      if (ex.getLogger() == MessagingNetworkException.CATEGORY_STILL_CONNECTED)
        throw ex;
      else
      if (ex.getLogger() == MessagingNetworkException.CATEGORY_LOGGED_OFF_DUE_TO_NETWORK_ERROR)
      {
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("network error exception ignored, starting/continuing reconnecting", ex);
        this.prevPluginContext = ctx;
        setState(S_RECONNECTING);
        //after failed relogin, if the reason was network error,
        //  we should call ev.reloginFailed() and call scheduleEvent(ev)
        //after failed relogin, if the reason was other,
        //  we should send the offline status event to listeners.
        ReconnectManager.reconnectFailed(this);
        if (Thread.currentThread().isInterrupted())
          return;
        scheduleReconnect(ctx);
      }
      else
      {
        synchronized (stateChangeLock)
        {
          if (state == S_RECONNECTING)
          {
            if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("non-network exception, stopping reconnecting", ex);
            setState(S_NORMAL);
          }
        }
        throw ex;
      }
    }
  }
*/

  protected void setReconnectData(int prevStatus_Native, Hashtable prevLoginId2cli)
  {
    this.prevStatusNative = prevStatus_Native;
    if (prevLoginId2cli != null)
    {
      //log("setReconnectData, ht size="+(prevUinInt2cli==null?null:""+prevUinInt2cli.size()));
      this.prevLoginId2cli = prevLoginId2cli;
    }
  }

  void reconnect()
  {
    if ((state) != (S_RECONNECTING)) Lang.ASSERT_EQUAL(state, S_RECONNECTING, "state", "S_RECONNECTING");
    try
    {
      synchronized (this)
      {
        try
        {
          super.login_Native(prevPassword, getPrevContactList(), prevStatusNative, prevPluginContext);
          reloginSuccess();
        }
        catch (MessagingNetworkException ex)
        {
          if (ex.getLogger() == MessagingNetworkException.CATEGORY_STILL_CONNECTED)
          {
            reloginSuccess();
          }
          throw ex;
        }
      }
    }
    catch (MessagingNetworkException ex)
    {
      if (ex.getLogger() != MessagingNetworkException.CATEGORY_STILL_CONNECTED)
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(getLoginId() + " reconnect failed", ex);

      if (ex.getLogger() == MessagingNetworkException.CATEGORY_LOGGED_OFF_DUE_TO_NETWORK_ERROR)
      {
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ex ignored, continuing reconnecting", ex);
        synchronized (stateChangeLock)
        {
          setState(S_RECONNECTING);
          //after failed relogin, if the reason was network error,
          //  we should call ev.reloginFailed() and call scheduleEvent(ev)
          //after failed relogin, if the reason was other,
          //  we should send the offline status event to listeners.
          ReconnectManager.reconnectFailed(this);
          if (Thread.currentThread().isInterrupted())
            return;
          scheduleReconnect(this.prevPluginContext);
        }
      }
      else
      {
        synchronized (stateChangeLock)
        {
          if (state == S_RECONNECTING)
          {
            if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("non-network exception, stopping reconnecting", ex);
            setState(S_NORMAL);
            registeredRelogin();
            try
            {
              fireSessionStatusChangeMim_Uncond(MessagingNetwork.STATUS_OFFLINE, this.prevPluginContext,
                ex.getLogger(), ex.getMessage(), ex.getEndUserReasonCode());
            }
            catch (MessagingNetworkException exx)
            {
              if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ex ignored", exx);
            }
            super.clearReconnectState();
          }
        }
      }
    }
  }

  private synchronized void reloginSuccess() throws MessagingNetworkException
  {
    setState(S_NORMAL);
    registeredRelogin();
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(getLoginId() + " reconnect login success, sleeping 2 seconds");
    try
    {
      Thread.currentThread().sleep(2000);
    }
    catch (InterruptedException exi)
    {
      return;
    }
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(getLoginId() + " sleep done, firing updated status events");
    fireContactListStatusObsoleteOffline(prevPluginContext, MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, null);
  }

  private final Wheel registeredReloginTimeMillis = new Wheel();

  /**
  <pre>
  # reconnector - 3 parameters:
  #
  # network_conditions_swing is when:
  #   network up -> network down -> network up -> network down -> network up etc. so that
  #   icq plugin reconnector can succesfully login, then get connection error, then succesfully login,
  #   then get connection error, etc etc etc, during some small period of time.
  #
  # - REQPARAM_NETWORK_CONDITIONS_SWING__MAXIMUM_COUNT_OF_REGISTERED_RELOGINS
  #     maximum count of session relogin attempts that are registered by icq server.
  #     Будем называть такие попытки релогина термином 'registered relogins'.
  # - REQPARAM_NETWORK_CONDITIONS_SWING__FORGET_TIMEOUT_FOR_REGISTERED_RELOGINS_MINUTES
  #     session will forget all & any registered relogins, which occured
  #     before (currentTime() - FORGET_TIMEOUT)
  # - REQPARAM_NETWORK_CONDITIONS_SWING__SLEEP_TIME_WHEN_MAXIMUM_REACHED_MINUTES
  #     when maximum count of registered relogins is reached during the FORGET_TIMEOUT period of time,
  #     the session reconnector will sleep during the SLEEP_TIME_WHEN_MAXIMUM_REACHED period of time.
  #     When this period ends, the session reconnector will wake up and continue to reconnect.

  REQPARAM_NETWORK_CONDITIONS_SWING__MAXIMUM_COUNT_OF_REGISTERED_RELOGINS
  REQPARAM_NETWORK_CONDITIONS_SWING__FORGET_TIMEOUT_FOR_REGISTERED_RELOGINS_MINUTES
  REQPARAM_NETWORK_CONDITIONS_SWING__SLEEP_TIME_WHEN_MAXIMUM_REACHED_MINUTES
  </pre>
  */
  private void registeredRelogin()
  {
    synchronized (registeredReloginTimeMillis)
    {
      if (registeredReloginTimeMillis.size() >= MSNMessagingNetwork.REQPARAM_NETWORK_CONDITIONS_SWING__MAXIMUM_COUNT_OF_REGISTERED_RELOGINS)
      {
        //remove the oldest
        registeredReloginTimeMillis.get();
      }
      registeredReloginTimeMillis.put(new Long(System.currentTimeMillis()));
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(
        "["+getLoginId()+"]: registeredRelogin(). Current relogin count: "+
          (registeredReloginTimeMillis.size() >=
            MSNMessagingNetwork.REQPARAM_NETWORK_CONDITIONS_SWING__MAXIMUM_COUNT_OF_REGISTERED_RELOGINS
          ? ">="
          : ""
          ) + registeredReloginTimeMillis.size()
      );
    }
  }


  boolean isRegisteredReloginsMaximumReached()
  {
    synchronized (registeredReloginTimeMillis)
    {
      if (registeredReloginTimeMillis.size() <
        MSNMessagingNetwork.REQPARAM_NETWORK_CONDITIONS_SWING__MAXIMUM_COUNT_OF_REGISTERED_RELOGINS)
        return false;
      Long oldest = (Long) registeredReloginTimeMillis.peek();
      long timePassedAfterOldest = System.currentTimeMillis() - oldest.longValue();
      return timePassedAfterOldest <=
        MSNMessagingNetwork.REQPARAM_NETWORK_CONDITIONS_SWING__FORGET_TIMEOUT_FOR_REGISTERED_RELOGINS_MINUTES
        * 60000L;
    }
  }

  private void scheduleReconnect(PluginContext ctx)
  {
    ((ResourceManagerReconnecting) ctx.getResourceManager()).getReconnectManager().scheduleReconnect(this);
  }

  private void cancelReconnect(PluginContext ctx)
  {
    ((ResourceManagerReconnecting) ctx.getResourceManager()).getReconnectManager().cancelReconnect(this);
  }

  private String[] getPrevContactList()
  {
    Hashtable cl = (Hashtable) this.prevLoginId2cli.clone();
    if ((cl) == null) Lang.ASSERT_NOT_NULL(cl, "prevLoginId2cli");
    Enumeration e = cl.keys();
    String[] sa = new String[cl.size()];
    int i = 0;
    while (e.hasMoreElements())
    {
      String uin = (String) e.nextElement();
      sa[i++] = uin;
    }
    return sa;
  }

  public void sendContacts(String dstLoginId, String[] nicks, String[] loginIds, PluginContext ctx)
  throws MessagingNetworkException
  {
    switch (state)
    {
      case S_NORMAL:
        super.sendContacts(dstLoginId, nicks, loginIds, ctx);
        break;
      case S_RECONNECTING:
        throwStillReconnecting();
      default:
        Lang.ASSERT_FALSE("invalid state: "+state);
    }
  }

  public UserDetailsImpl getUserDetails(String dstLoginId, PluginContext ctx)
  throws MessagingNetworkException
  {
    switch (state)
    {
      case S_NORMAL:
        return super.getUserDetails(dstLoginId, ctx);
      case S_RECONNECTING:
        throwStillReconnecting();
      default:
        Lang.ASSERT_FALSE("invalid state: "+state);
        return null; //never reached, to calm javac down
    }
  }
}