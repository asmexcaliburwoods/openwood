package org.openmim.icq2k;

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

  private int state = S_NORMAL;

  public static final String s_state2string(int s_state)
  {
    switch (s_state)
    {
      case S_NORMAL: 
        return "s_normal";
      case S_RECONNECTING:
        return "s_reconnecting";
      default:
        return "invalid_s_state(s_state=="+s_state+")";
    }
  }
  
  //reconnect lock
  private final Object stateChangeLock = new Object();
  
  //setstate()/getstate() lock
  private final Object stateChangeLockQ = new Object();

  //must always be non-offline in the S_RECONNECTING state.
  protected int prevStatusOscar = StatusUtil.OSCAR_STATUS_OFFLINE;
  protected Hashtable prevUinInt2cli = null;
  protected String prevPassword = null;

  int reconnectManagerState;
  MessagingTask reconnectTask;



  SessionReconnecting(PluginContext ctx, String srcLoginId) throws MessagingNetworkException
  {
    super(ctx, srcLoginId);
    ReconnectManager.initReconnectManagerState(this);
  }

  private void setContactListStatusObsolete(PluginContext ctx) throws MessagingNetworkException
  {
    Hashtable prevUinInt2cli = this.prevUinInt2cli;
    if (Defines.DEBUG && CAT.isDebugEnabled())
      CAT.debug("setContactListStatusObsolete, ht size="+(prevUinInt2cli==null?null:""+prevUinInt2cli.size()));
    if (prevUinInt2cli == null) return;
    Enumeration e = prevUinInt2cli.elements();
    while (e.hasMoreElements())
    {
      ContactListItem cli = (ContactListItem) e.nextElement();
      cli.setStatusObsolete(true);
    }
  }

  private void fireContactListStatusObsoleteOffline(PluginContext ctx,
    int reasonLogger, String reasonMessage) throws MessagingNetworkException
  {
    Hashtable prevUinInt2cli = this.prevUinInt2cli;
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
        ("fireContactListStatusObsoleteOffline, ht size="+(prevUinInt2cli==null?null:""+prevUinInt2cli.size()));
    if (prevUinInt2cli == null) return;
    Enumeration e = prevUinInt2cli.elements();
    while (e.hasMoreElements())
    {
      ContactListItem cli = (ContactListItem) e.nextElement();
      if (cli.isStatusObsolete())
      {
        int smim = StatusUtil.translateStatusOscarToMim_cl_entry(cli.getStatusOscar());
        if (smim != MessagingNetwork.STATUS_OFFLINE)
        {
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
              ("fireClearCL "+cli.getDstLoginId()+" from "+StatusUtil.translateStatusOscarToString(cli.getStatusOscar())+" to OFFLINE");
          cli.setStatusOscar(StatusUtil.OSCAR_STATUS_OFFLINE);
          super.fireContactListEntryStatusChangeMim_Uncond(cli.getDstLoginId(), MessagingNetwork.STATUS_OFFLINE, ctx,
            reasonLogger, reasonMessage);
        }
        else
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
            ("fireClearCL "+cli.getDstLoginId()+" is already offline");
      }
      else
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
          ("fireClearCL "+cli.getDstLoginId()+" is already updated");
    }
  }

  protected final MessagingNetworkException convertErrorAfterShutdown(final MessagingNetworkException ex, PluginContext ctx, boolean reconnecting)
  throws Exception
  {
    /*
    if (reconnecting)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) 
        CAT.debug(
          getLoginId()+
          ": REC: login requested by reconnector, return the exception as is: skipping ex conversion");
      //failed login requested by reconnector, return the exception as is, so that the reconnector 
      //will not think that login has succeeded
      return ex;
    }
    */
    
    int state = getState();
    
    if (Defines.DEBUG && CAT.isDebugEnabled()) 
      CAT.debug(
        getLoginId()+
        ": convertErrorAfterShutdown/1: s_state="+s_state2string(state)+", ex: "+ex);
        
    if (ex.getLogger() != ex.CATEGORY_LOGGED_OFF_DUE_TO_NETWORK_ERROR &&
         (crAttempted ||
           ex.getEndUserReasonCode() != ex.ENDUSER_CANNOT_LOGIN_CONNECT_RATE_EXCEEDED_TRY_10_OR_20_MINUTES_LATER &&
           ex.getEndUserReasonCode() != ex.ENDUSER_CANNOT_LOGIN_CONNECT_RATE_EXCEEDED_TRY_1_OR_2_MINUTES_LATER
         ))
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) 
        CAT.debug(
          getLoginId()+
          ": convertErrorAfterShutdown: ex is NOT subject to conversion, skipping conversion");
      return ex;
    }
    
    //int smim = StatusUtil.translateStatusOscarToMim_self(super.getStatus_Oscar());
    //if (Defines.DEBUG && CAT.isDebugEnabled())
    //  CAT.debug(getLoginId()+": super.status: "+StatusUtilMim.translateStatusMimToString(smim));

    int c = ex.ENDUSER_NETWORK_ERROR_RECONNECTING;
    
    if (isLoggingIn())
    {
      if (state == S_NORMAL)
      {
        //failed login requested by core
        if (Defines.DEBUG && CAT.isDebugEnabled())
          CAT.debug(getLoginId()+": login failed, firing the fake online status");
        fireSessionStatusChangeMim_Uncond(StatusUtil.translateStatusOscarToMim_self(this.prevStatusOscar), ctx, MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, null, MessagingNetworkException.ENDUSER_NO_ERROR);
        
        if (ex.getLogger() == ex.CATEGORY_LOGGED_OFF_DUE_TO_NETWORK_ERROR)
        {
          if (Defines.DEBUG && CAT.isDebugEnabled())
            CAT.debug(getLoginId()+": login failed due to network error, starting reconnecting");
          startReconnecting();
        }
        else
        {
          if (ex.getEndUserReasonCode() == ex.ENDUSER_CANNOT_LOGIN_CONNECT_RATE_EXCEEDED_TRY_1_OR_2_MINUTES_LATER)
          {
            crAttempted = true;
            //c = ex.ENDUSER_CANNOT_LOGIN_CONNECT_RATE_EXCEEDED_WILL_RECONNECT_1_OR_2_MINUTES_LATER;
            if (Defines.DEBUG && CAT.isDebugEnabled())
              CAT.debug(getLoginId()+": login failed due to connect_rate_exceeded_1_or_2_min, starting reconnecting");
            startReconnecting(2*60);
          }
          else
            if (ex.getEndUserReasonCode() == ex.ENDUSER_CANNOT_LOGIN_CONNECT_RATE_EXCEEDED_TRY_10_OR_20_MINUTES_LATER)
            {
              crAttempted = true;
              //c = ex.ENDUSER_CANNOT_LOGIN_CONNECT_RATE_EXCEEDED_WILL_RECONNECT_10_OR_20_MINUTES_LATER;
              if (Defines.DEBUG && CAT.isDebugEnabled())
                CAT.debug(getLoginId()+": login failed due to connect_rate_exceeded_10_or_20_min, starting reconnecting");
              startReconnecting(20*60);
            }
            else
              Lang.ASSERT_FALSE("/abc123/");
        }
        state = getState();
      }
      else
      {
        if (Defines.DEBUG && CAT.isDebugEnabled()) 
          CAT.debug(
            getLoginId()+
            ": login requested by reconnector, return the exception as is: skipping ex conversion");
        //failed login requested by reconnector, return the exception as is, so that the reconnector 
        //will not think that login has succeeded
        return ex;
      }
    }
    else
    {
      if (ex.getLogger() == ex.CATEGORY_LOGGED_OFF_DUE_TO_NETWORK_ERROR)
      {
        if (Defines.DEBUG && CAT.isDebugEnabled())
          CAT.debug(getLoginId()+": login failed due to network error, starting reconnecting");
        startReconnecting();
        state = getState();
      }
      else
        Lang.ASSERT_FALSE("/def456/");
    }
    
    if (state != S_RECONNECTING)
      Lang.ASSERT_EQUAL(state, S_RECONNECTING, "s_state", "s_reconnecting");

    //if (ex.getEndUserReasonCode() != ex.ENDUSER_LOGGED_OFF_DUE_TO_NETWORK_ERROR)
    //  Lang.ASSERT_EQUAL( ex.getEndUserReasonCode(), ex.ENDUSER_LOGGED_OFF_DUE_TO_NETWORK_ERROR, 
    //                    "ex.getEndUserReasonCode()", "ex.ENDUSER_LOGGED_OFF_DUE_TO_NETWORK_ERROR");
    
    MessagingNetworkException e = new MessagingNetworkException(
      "[please wait for the mim server to reconnect] "+ex.getMessage(), 
      ex.CATEGORY_STILL_CONNECTED, 
      c);
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(getLoginId()+": ex converted to "+e);
    return e;
  }
  
  private final void startReconnecting()
  {
    setState(S_RECONNECTING);
    scheduleReconnect(ctx);
  }

  private final void startReconnecting(int approxSeconds)
  {
    setState(S_RECONNECTING);
    scheduleReconnect(ctx, approxSeconds);
  }

  protected void fireSessionStatusChangeMim_Uncond(int newStatusMim, PluginContext ctx,
    int reasonLogger, String reasonMessage, int endUserReasonCode) throws MessagingNetworkException
  {
    int state = getState();
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("fireSessionStatusChangeMim_Uncond to "+
      StatusUtilMim.translateStatusMimToString(newStatusMim)+
      "\r\ncategory="+MessagingNetworkException.getLoggerMessage(reasonLogger)+
      "\r\ns_state="+s_state2string(state)+" r_state="+ReconnectManager.r_state2string(reconnectManagerState));
    switch (state)
    {
      case S_NORMAL:
        if (newStatusMim == MessagingNetwork.STATUS_OFFLINE &&
            reasonLogger == MessagingNetworkException.CATEGORY_LOGGED_OFF_DUE_TO_NETWORK_ERROR)
        {
          if (Defines.DEBUG && CAT.isDebugEnabled()) 
            CAT.debug(getLoginId()+" changeSessionStatus event ignored, starting reconnecting");
          startReconnecting();
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
            int newStatusOscar = StatusUtil.translateStatusMimToOscar(newStatusMim);
            int prevStatusMim = StatusUtil.translateStatusOscarToMim_self(prevStatusOscar);
            if (newStatusMim != prevStatusMim)
            {
              this.prevStatusOscar = newStatusOscar;
              if (Defines.DEBUG && CAT.isDebugEnabled()) 
                CAT.debug("S_RECONN: setStatus assigned, firing");
              super.fireSessionStatusChangeMim_Uncond(newStatusMim, ctx,
                reasonLogger, reasonMessage, endUserReasonCode);
            }
            else
              if (Defines.DEBUG && CAT.isDebugEnabled()) 
                CAT.debug("S_RECONN: duplicate setStatus ignored: oldStatusMim="+
                          StatusUtilMim.translateStatusMimToString(prevStatusMim)+
                          " newStatusMim="+
                          StatusUtilMim.translateStatusMimToString(newStatusMim));
                          
            if (super.getStatus_Oscar() != StatusUtil.OSCAR_STATUS_OFFLINE)
            {
              setContactListStatusObsolete(ctx);
            }
            else
              if (Defines.DEBUG && CAT.isDebugEnabled()) 
                CAT.debug("S_RECONN: setStatusObsolete skipped");
          }
        }
        else
          if (Defines.DEBUG && CAT.isDebugEnabled()) 
            CAT.debug("S_RECONN: setStatus OFFLINE ignored");
        break;
      default:
        Lang.ASSERT_FALSE("invalid state: "+s_state2string(state));
    }
  }

  protected void throwStillReconnecting() throws MessagingNetworkException
  {
    throw new MessagingNetworkException(
      "network error occured, please wait for the mim server to reconnect",
      MessagingNetworkException.CATEGORY_STILL_CONNECTED,
      MessagingNetworkException.ENDUSER_NETWORK_ERROR_RECONNECTING);
  }

  private int getState()
  {
    synchronized(stateChangeLockQ)
    {
      return state;
    }
  }

  public void addToContactList(
    final AsyncOperations.OpAddContactListItem op,
    String dstLoginId, PluginContext ctx)
  throws MessagingNetworkException
  {
    int state = getState();
    switch (state)
    {
      case S_NORMAL:
        super.addToContactList(op, dstLoginId, ctx);
        break;
      case S_RECONNECTING:
        throwStillReconnecting();
      default:
        Lang.ASSERT_FALSE("invalid state: "+s_state2string(state));
    }
  }

  public int getStatus_Oscar()
  {
    int state = getState();
    switch (state)
    {
      case S_NORMAL:
        return super.getStatus_Oscar();
      case S_RECONNECTING:
        //if (isLoggingInRequestedByPluginCaller())
        //  return StatusUtil.OSCAR_STATUS_OFFLINE;
        //else 
        return this.prevStatusOscar;
      default:
        Lang.ASSERT_FALSE("invalid state: "+s_state2string(state));
        return StatusUtil.OSCAR_STATUS_OFFLINE; //this is never reached
    }
  }

  public int getContactStatus_Oscar(String dstLoginId, PluginContext ctx) throws MessagingNetworkException
  {
    int state = getState();
    switch (state)
    {
      case S_NORMAL:
        return super.getContactStatus_Oscar(dstLoginId, ctx);
      case S_RECONNECTING:
        try
        {
          return getPrevContactListItemStatusOscar(IcqUinUtil.parseUin(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_STILL_CONNECTED));
        }
        catch (Exception ex)
        {
          handleException(ex, "getContactStatus", ctx);
          return StatusUtil.OSCAR_STATUS_OFFLINE; //this is never reached
        }
      default:
        Lang.ASSERT_FALSE("invalid state: "+s_state2string(state));
        return StatusUtil.OSCAR_STATUS_OFFLINE; //this is never reached
    }
  }

  protected int getPrevContactListItemStatusOscar(int dstLoginId)
  {
    ContactListItem cli = getPrevContactListItem(dstLoginId);
    if (cli == null)
      return StatusUtil.OSCAR_STATUS_OFFLINE;
    else
      return cli.getStatusOscar();
  }

  protected ContactListItem getPrevContactListItem(int dstLoginId)
  {
    return (ContactListItem) prevUinInt2cli.get(new Integer(dstLoginId));
  }

  public void login_Oscar(
       final AsyncOperations.OpLogin op,
       final String password,
       String[] contactList,
       final int statusOscar,
       boolean calledByReconnector,
       final PluginContext ctx)
  throws MessagingNetworkException
  {
    synchronized (stateChangeLock)
    {
      if (password == null) Lang.ASSERT_NOT_NULL(password, "password");
      this.prevPassword = password;
      if (prevPassword == null) Lang.ASSERT_NOT_NULL(prevPassword, "prevPassword");
    }
    
    int state = getState();
    switch (state)
    {
      case S_NORMAL:
        super.login_Oscar(op, password, contactList, statusOscar, calledByReconnector, ctx);
        break;
      case S_RECONNECTING:
        if (!calledByReconnector) throwStillReconnecting();
        super.login_Oscar(op, password, contactList, statusOscar, calledByReconnector, ctx);
        break;
      default:
        Lang.ASSERT_FALSE("invalid state: "+s_state2string(state));
    }
  }

  public void removeFromContactList(
    final AsyncOperations.OpRemoveContactListItem op,
    String dstLoginId, PluginContext ctx)
  throws MessagingNetworkException
  {
    int state = getState();
    switch (state)
    {
      case S_NORMAL:
        super.removeFromContactList(op, dstLoginId, ctx);
        break;
      case S_RECONNECTING:
        throwStillReconnecting();
      default:
        Lang.ASSERT_FALSE("invalid state: "+s_state2string(state));
    }
  }

  public void sendMessage(
    final AsyncOperations.OpSendMessage op,
    final String dstLoginId, final String text, final PluginContext ctx)
  throws MessagingNetworkException
  {
    int state = getState();
    switch (state)
    {
      case S_NORMAL:
        super.sendMessage(op, dstLoginId, text, ctx);
        break;
      case S_RECONNECTING:
        throwStillReconnecting();
      default:
        Lang.ASSERT_FALSE("invalid state: "+s_state2string(state));
    }
  }

  /** Change Session status to non-offline is synchronized by Session */
  public void setStatus_Oscar_External(
    final AsyncOperations.OpSetStatusNonOffline op,
    int statusOscar, PluginContext ctx)
  throws MessagingNetworkException
  {
    if (!(  statusOscar != StatusUtil.OSCAR_STATUS_OFFLINE  )) throw new AssertException("statusOscar cannot be StatusUtil.OSCAR_STATUS_OFFLINE here, but it is.");
    int state = getState();
    switch (state)
    {
      case S_NORMAL:
        super.setStatus_Oscar_External(op, statusOscar, ctx);
        break;
      case S_RECONNECTING:
        int statusMim = StatusUtil.translateStatusOscarToMim_self(statusOscar);
        fireSessionStatusChangeMim_Uncond(statusMim, ctx,
          MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
          "caller requested status change",
          MessagingNetworkException.ENDUSER_NO_ERROR);
        op.success();
        break;
      default:
        Lang.ASSERT_FALSE("invalid state: "+s_state2string(state));
    }
  }

  void logout(PluginContext ctx, int reasonLogger, String reasonMessage, int endUserReasonCode)
  throws MessagingNetworkException
  {
    synchronized (stateChangeLock)
    {
      super.clearReconnectState();
      int state = getState();
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
          Lang.ASSERT_FALSE("invalid state: "+s_state2string(state));
      }
    }
  }

  protected void setState(int state)
  {
    synchronized (stateChangeLock)
    {
      if (getState() == state) return;

      switch (state)
      {
        case S_NORMAL:
          ReconnectManager.initReconnectManagerState(this);
          prevStatusOscar = StatusUtil.OSCAR_STATUS_OFFLINE;
          cancelReconnect(ctx);
          break;
        case S_RECONNECTING:
          if (prevStatusOscar == StatusUtil.OSCAR_STATUS_OFFLINE) throw new AssertException("prevStatusOscar cannot be StatusUtil.OSCAR_STATUS_OFFLINE here, but it is.");
          if (prevUinInt2cli == null) Lang.ASSERT_NOT_NULL(prevUinInt2cli, "prevUinInt2cli");
          if (prevPassword == null) Lang.ASSERT_NOT_NULL(prevPassword, "prevPassword");
          //scheduleReconnect(prevPluginContext);
          break;
        default:
          Lang.ASSERT_FALSE("invalid state: "+s_state2string(state));
      }
      this.state = state;
      if (Defines.DEBUG && CAT.isDebugEnabled()) 
        CAT.debug("reconnect s_state for "+this+" is now "+s_state2string(state));
    }
  }

  protected void setReconnectData(int prevStatus_Oscar, Hashtable prevUinInt2cli)
  {
    synchronized (stateChangeLock)
    {
      /*
      if (Defines.DEBUG && CAT.isDebugEnabled()) 
        CAT.debug("setReconnectData for "+this+
          ": status="+StatusUtil.translateStatusOscarToString(prevStatus_Oscar)+
          ", contactlist="+prevUinInt2cli,
          new Exception("dumpstack"));
          */
      this.prevStatusOscar = prevStatus_Oscar;
      if (prevUinInt2cli != null)
      {
        //log("setReconnectData, ht size="+(prevUinInt2cli==null?null:""+prevUinInt2cli.size()));
        this.prevUinInt2cli = prevUinInt2cli;
      }
    }
  }

  final void reconnect()
  {
    int state = getState();
    if (state != S_RECONNECTING) 
      Lang.ASSERT_EQUAL(state, S_RECONNECTING, "state", "s_reconnecting");
    try
    {
      synchronized (this)
      {
        try
        {
          resetShuttingDown();
          ctx.getGal().addAop(true);
          AsyncOperationImpl aop = null;
          try
          { 
            runSynchronous(aop = new AsyncOperations.
              OpLogin(
                prevPassword, 
                getPrevContactList(), 
                StatusUtil.translateStatusOscarToMim_self(prevStatusOscar), 
                true,
                this, 
                ctx));
          }
          catch (MessagingNetworkException mex)
          {
            ctx.getPlug().handleEarlyException(aop, mex);
            throw mex;
          }
          reloginSuccess();
        }
        catch (MessagingNetworkException ex)
        {
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("", ex);
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
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(getLoginId()+": reconnect failed", ex);

      if (ex.getLogger() == MessagingNetworkException.CATEGORY_LOGGED_OFF_DUE_TO_NETWORK_ERROR)
      {
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(getLoginId()+": ex ignored, continuing reconnecting", ex);
        synchronized (stateChangeLock)
        {
          setState(S_RECONNECTING);
          //after failed relogin, if the reason was network error,
          //  we should call ev.reloginFailed() and call scheduleEvent(ev)
          //after failed relogin, if the reason was other,
          //  we should send the offline status event to listeners.
          ReconnectManager.reconnectFailed(this);
          if (Thread.currentThread().isInterrupted())
          {
            if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(getLoginId()+": interrupted/2");
            return;
          }
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(getLoginId()+": scheduling reconnect");
          scheduleReconnect(this.ctx);
        }
      }
      else
      {
        synchronized (stateChangeLock)
        {
          if (getState() == S_RECONNECTING)
          {
            if (Defines.DEBUG && CAT.isDebugEnabled()) 
              CAT.debug("non-network exception, stopping reconnecting", ex);
            setState(S_NORMAL);
            registeredRelogin();
            try
            {
              fireSessionStatusChangeMim_Uncond(MessagingNetwork.STATUS_OFFLINE, this.ctx,
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

  private void reloginSuccess() throws MessagingNetworkException
  {
    setLoggingIn(false);
    
    synchronized (this)
    {
      setState(S_NORMAL);
      registeredRelogin();
      if (Defines.DEBUG && CAT.isDebugEnabled()) 
        CAT.debug(getLoginId() + " reconnect login success, firing updated status events");//, sleeping 2 seconds");
      /*
      try
      {
        Thread.currentThread().sleep(2000);
      }
      catch (InterruptedException exi)
      {
        return;
      }
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
        (getLoginId() + " sleep done, firing updated status events");
      */
      fireContactListStatusObsoleteOffline(ctx, MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, null);
    }
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

  ICQ2KMessagingNetworkReconnecting.REQPARAM_NETWORK_CONDITIONS_SWING__MAXIMUM_COUNT_OF_REGISTERED_RELOGINS
  ICQ2KMessagingNetworkReconnecting.REQPARAM_NETWORK_CONDITIONS_SWING__FORGET_TIMEOUT_FOR_REGISTERED_RELOGINS_MINUTES
  ICQ2KMessagingNetworkReconnecting.REQPARAM_NETWORK_CONDITIONS_SWING__SLEEP_TIME_WHEN_MAXIMUM_REACHED_MINUTES
  </pre>
  */
  private void registeredRelogin()
  {
    synchronized (registeredReloginTimeMillis)
    {
      if (registeredReloginTimeMillis.size() >= ICQ2KMessagingNetworkReconnecting.REQPARAM_NETWORK_CONDITIONS_SWING__MAXIMUM_COUNT_OF_REGISTERED_RELOGINS)
      {
        //remove the oldest
        registeredReloginTimeMillis.get();
      }
      registeredReloginTimeMillis.put(new Long(System.currentTimeMillis()));
      if (Defines.DEBUG && CAT.isDebugEnabled()) 
        CAT.debug("["+this+"]: network swing check/registeredRelogin(). Current relogin count: "+(registeredReloginTimeMillis.size() >= ICQ2KMessagingNetworkReconnecting.REQPARAM_NETWORK_CONDITIONS_SWING__MAXIMUM_COUNT_OF_REGISTERED_RELOGINS ? ">=" : "") + registeredReloginTimeMillis.size());
    }
  }


  boolean isRegisteredReloginsMaximumReached()
  {
    synchronized (registeredReloginTimeMillis)
    {
      if (registeredReloginTimeMillis.size() < ICQ2KMessagingNetworkReconnecting.REQPARAM_NETWORK_CONDITIONS_SWING__MAXIMUM_COUNT_OF_REGISTERED_RELOGINS)
        return false;
      Long oldest = (Long) registeredReloginTimeMillis.peek();
      long timePassedAfterOldest = System.currentTimeMillis() - oldest.longValue();
      return timePassedAfterOldest <= ICQ2KMessagingNetworkReconnecting.REQPARAM_NETWORK_CONDITIONS_SWING__FORGET_TIMEOUT_FOR_REGISTERED_RELOGINS_MINUTES*60000L;
    }
  }

  private void scheduleReconnect(PluginContext ctx)
  {
    ((ResourceManagerReconnecting) ctx.getResourceManager()).getReconnectManager().scheduleReconnect(this);
  }

  private void scheduleReconnect(PluginContext ctx, int approxSeconds)
  {
    ((ResourceManagerReconnecting) ctx.getResourceManager()).getReconnectManager().scheduleReconnect(this, approxSeconds);
  }

  private void cancelReconnect(PluginContext ctx)
  {
    ((ResourceManagerReconnecting) ctx.getResourceManager()).getReconnectManager().cancelReconnect(this);
  }

  private String[] getPrevContactList()
  {
    Hashtable cl = (Hashtable) this.prevUinInt2cli.clone();
    if ((cl) == null) Lang.ASSERT_NOT_NULL(cl, "prevUinInt2cli");
    Enumeration e = cl.keys();
    String[] sa = new String[cl.size()];
    int i = 0;
    while (e.hasMoreElements())
    {
      Integer uin = (Integer) e.nextElement();
      sa[i++] = uin.toString();
    }
    return sa;
  }

  public void sendContacts(
    final AsyncOperations.OpSendContacts op,
    String dstLoginId, String[] nicks, String[] loginIds, PluginContext ctx)
  throws MessagingNetworkException
  {
    int state = getState();
    switch (state)
    {
      case S_NORMAL:
        super.sendContacts(op, dstLoginId, nicks, loginIds, ctx);
        break;
      case S_RECONNECTING:
        throwStillReconnecting();
      default:
        Lang.ASSERT_FALSE("invalid state: "+s_state2string(state));
    }
  }

  public void fetchUserDetails(
    final AsyncOperations.OpGetUserDetails op,
    String dstLoginId, boolean externalCall, PluginContext ctx)
  throws MessagingNetworkException
  {
    int state = getState();
    switch (state)
    {
      case S_NORMAL:
        super.fetchUserDetails(op, dstLoginId, externalCall, ctx);
        break;
      case S_RECONNECTING:
        if (externalCall) throwStillReconnecting();
        super.fetchUserDetails(op, dstLoginId, externalCall, ctx);
        break;
      default:
        Lang.ASSERT_FALSE("invalid state: "+s_state2string(state));
    }
  }

  public void checkValid() throws MessagingNetworkException 
  {
    super.checkValid();
    if (getState() == S_RECONNECTING) throwStillReconnecting();
  }
}
