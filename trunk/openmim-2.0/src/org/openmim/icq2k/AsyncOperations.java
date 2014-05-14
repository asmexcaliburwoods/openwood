package org.openmim.icq2k;

import org.openmim.*;
import org.openmim.messaging_network.MessagingNetworkException;
import org.openmim.stuff.Defines;
import org.openmim.stuff.UserDetails;
import org.openmim.stuff.UserSearchResults;
import org.openmim.icq.utils.*;

class AsyncOperations
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(AsyncOperations.class.getName());

  static class OpAddContactListItem extends AsyncOperationImpl
  {
    OpAddContactListItem(
      String dstLoginId,
      Session session, PluginContext ctx)
    throws MessagingNetworkException
    {
      super("addi", session, ctx, false);
      this.dstLoginId = dstLoginId;
    }

    public void onStart() throws MessagingNetworkException, InterruptedException
    {
      session.addToContactList(this, dstLoginId, ctx);
    }

    protected final void onFail(MessagingNetworkException mex) {}
    protected final void onSuccess() {}
    protected final void onFinished() {}

    final String dstLoginId;
  }

  static class OpRemoveContactListItem extends AsyncOperationImpl
  {
    OpRemoveContactListItem(
      String dstLoginId,
      Session session, PluginContext ctx)
    throws MessagingNetworkException
    {
      super("remi", session, ctx, false);
      this.dstLoginId = dstLoginId;
    }

    public void onStart() throws MessagingNetworkException, InterruptedException
    {
      session.removeFromContactList(this, dstLoginId, ctx);
    }

    protected final void onFail(MessagingNetworkException mex) {}
    protected final void onSuccess() {}
    protected final void onFinished() {}
    final String dstLoginId;
  }

  static class OpSendContacts extends OpSendGenericMessage
  {
    OpSendContacts(
      String dstLoginId, String[] nicks, String[] loginIds,
      Session session, PluginContext ctx)
      throws MessagingNetworkException
    {
      super("sendc", dstLoginId, session, ctx);
      this.nicks = nicks;
      this.loginIds = loginIds;
    }

    public final void onStartGenericMessageOperation() throws MessagingNetworkException, InterruptedException
    {
      session.sendContacts(this, dstLoginId, nicks, loginIds, ctx);
    }

    protected final void onFail(MessagingNetworkException mex) {}
    protected final void onSuccess() {}

    final String[] nicks;
    final String[] loginIds;
  }

  static class OpGetUserDetails extends AsyncOperationImpl
  {
    OpGetUserDetails(
      String dstLoginId,
      Session session, boolean externalCall, PluginContext ctx)
    throws MessagingNetworkException
    {
      super("geti", session, ctx, false);
      this.dstLoginId = dstLoginId;
      this.externalCall = externalCall;
    }

    public final void onStart() throws MessagingNetworkException, InterruptedException
    {
      AsyncOperationImpl.checkOFL(AsyncOperations.OpGetUserDetails.this);
      session.fetchUserDetails(this, dstLoginId, externalCall, ctx);
    }

    protected final void onFail(MessagingNetworkException mex) {
      if (internalListener != null)
        internalListener.getUserDetailsFailed(
          ICQ2KMessagingNetwork.ICQ2K_NETWORK_ID, getId(),
          session.getLoginId(), dstLoginId, mex);
      else ctx.getPlug().getUserDetailsFailed(this, mex);
    }

    protected final void onSuccess() {
      if (internalListener != null)
      {
        internalListener.getUserDetailsSuccess(
          ICQ2KMessagingNetwork.ICQ2K_NETWORK_ID, getId(),
          session.getLoginId(), dstLoginId, result);
      }
      else
        ctx.getPlug().getUserDetailsSuccess(this, result);
    }

    protected final void onFinished()
    {
      session.getUserDetailsComplete();
    }

    final void errorFetchingReceived()
    {
      fail(new MessagingNetworkException(
            "icq server refuses to return userinfo for "+dstLoginId+".",
            MessagingNetworkException.CATEGORY_STILL_CONNECTED,
            MessagingNetworkException.ENDUSER_MESSAGING_SERVER_REFUSES_TO_RETURN_USERINFO));
    }

    final void userDetailsUnknownFormatReceived()
    {
      fail(new MessagingNetworkException(
            "can't get userinfo for "+dstLoginId+": cannot parse packet.",
            MessagingNetworkException.CATEGORY_STILL_CONNECTED,
            MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF));
    }

    final void userDetailsReceived(UserDetails ud)
    {
      if (ud == null) Lang.ASSERT_NOT_NULL(ud, "ud");
      result = ud;
      success();
    }

    final UserDetails getResult()
    {
      return result;
    }

    private UserDetails result;
    final String dstLoginId;
    private boolean externalCall;
  }

  static class OpSearch extends AsyncOperationImpl
  {
    OpSearch(
      String emailSearchPattern,
      String nickSearchPattern,
      String firstNameSearchPattern,
      String lastNameSearchPattern,
      Session session, PluginContext ctx)
    throws MessagingNetworkException
    {
      super("search", session, ctx, false);
      this.emailSearchPattern = emailSearchPattern;
      this.nickSearchPattern = nickSearchPattern;
      this.firstNameSearchPattern = firstNameSearchPattern;
      this.lastNameSearchPattern = lastNameSearchPattern;
    }

    public final void onStart()
    {
      try
      {
        result = session.searchUsers(
          this,
          emailSearchPattern,
          nickSearchPattern,
          firstNameSearchPattern,
          lastNameSearchPattern,
          ctx);
        success();
      }
      catch (Exception ex)
      {
        fail(ex);
      }
    }

    protected final void onFail(MessagingNetworkException mex) {}
    protected final void onSuccess() {}
    protected final void onFinished() {}

    final UserSearchResults getResult()
    {
      return result;
    }

    private UserSearchResults result;
    private String emailSearchPattern;
    private String nickSearchPattern;
    private String firstNameSearchPattern;
    private String lastNameSearchPattern;
  }

  static class OpIsAuthorizationRequired extends AsyncOperationImpl
  {
    OpIsAuthorizationRequired(
      String dstLoginId,
      Session session, PluginContext ctx)
      throws MessagingNetworkException
    {
      super("isauthreq", session, ctx, false);
      this.dstLoginId = dstLoginId;
    }

    public void onStart() throws MessagingNetworkException, InterruptedException
    {
      session.isAuthorizationRequired(this, dstLoginId, ctx);
    }

    protected final void onFail(MessagingNetworkException mex) {}
    protected final void onSuccess() {}
    protected final void onFinished() {}

    final void resultReceived(boolean result)
    {
      this.result = result;
      success();
    }

    final boolean getResult()
    {
      return result;
    }

    private boolean result = false;
    final String dstLoginId;
  }

  static class OpSendAuthorizationRequest extends OpSendGenericMessage
  {
    OpSendAuthorizationRequest(
      String dstLoginId, String reason,
      Session session, PluginContext ctx)
      throws MessagingNetworkException
    {
      super("authreq", dstLoginId, session, ctx);
      this.reason = reason;
    }

    public final void onStartGenericMessageOperation() throws MessagingNetworkException, InterruptedException
    {
      session.authorizationRequest(this, dstLoginId, reason, ctx);
    }

    protected final void onFail(MessagingNetworkException mex) {}
    protected final void onSuccess() {}

    final String reason;
  }

  static class OpSendAuthorizationResponse extends OpSendGenericMessage
  {
    OpSendAuthorizationResponse(
      String dstLoginId, boolean grant,
      Session session, PluginContext ctx)
      throws MessagingNetworkException
    {
      super("authresp", dstLoginId, session, ctx);
      this.grant = grant;
    }

    public final void onStartGenericMessageOperation() throws MessagingNetworkException, InterruptedException
    {
      session.authorizationResponse(this, dstLoginId, grant, ctx);
    }

    protected final void onFail(MessagingNetworkException mex) {}
    protected final void onSuccess() {}

    final boolean grant;
  }

  static class OpSendMessage extends OpSendGenericMessage
  {
    OpSendMessage(
      String dstLoginId, String text,
      Session session, PluginContext ctx)
      throws MessagingNetworkException
    {
      super("sendm", dstLoginId, session, ctx);
      this.text = text;
    }

    public final void onStartGenericMessageOperation() throws MessagingNetworkException, InterruptedException
    {
      session.sendMessage(this, dstLoginId, text, ctx);
    }

    protected final void onSuccess() { ctx.getPlug().sendMessageSuccess(this); }
    protected final void onFail(MessagingNetworkException mex) { ctx.getPlug().sendMessageFailed(this, mex); }

    final String text;
  }

  static class OpLogin extends OpSetStatus
  {
    OpLogin(
      String password, String[] contactList, int statusMim,
      boolean calledByReconnector,
      Session session, PluginContext ctx)
      throws MessagingNetworkException
    {
      super("login", statusMim, session, ctx, true);
      this.password = password;
      this.contactList = contactList;
      this.calledByReconnector = calledByReconnector;
    }

    public void onStart() throws MessagingNetworkException, InterruptedException
    {
      session.login_Oscar(
        this,
        password,
        contactList,
        StatusUtil.translateStatusMimToOscar(statusMim),
        calledByReconnector,
        ctx
      );
    }
    
    final String password;
    final String[] contactList;
    final boolean calledByReconnector;
  }

  static class OpSetStatusNonOffline extends OpSetStatus
  {
    OpSetStatusNonOffline(
      int statusMim,
      Session session, PluginContext ctx)
      throws MessagingNetworkException
    {
      super("setstatus", statusMim, session, ctx, false);
    }

    public void onStart() throws MessagingNetworkException, InterruptedException
    {
      session.setStatus_Oscar_External(this, StatusUtil.translateStatusMimToOscar(statusMim), ctx);
    }
  }

  abstract static class OpSetStatus extends AsyncOperationImpl
  {
    OpSetStatus(
      String opName,
      final int statusMim,
      Session session, PluginContext ctx, boolean timeoutLogsOff)
      throws MessagingNetworkException
    {
      super(opName, session, ctx, timeoutLogsOff);
      this.statusMim = statusMim;
    }

    private boolean muteFailure = false;
    private final Object muteLock = new Object();
    
    void muteFailureEvent()
    {
      synchronized (muteLock)
      {
        muteFailure = true;
      }
    }

    protected final void onSuccess() { session.setLoggingIn(false); }
    protected final void onFinished() {}
    
    protected final void onFail(MessagingNetworkException mex) 
    { 
      boolean mute;
      synchronized (muteLock)
      {
        mute = muteFailure;
      }
      if (!mute) ctx.getPlug().setStatusFailed(this, mex); 
    }
    
    /** status to be set */
    final int statusMim;
  }

  abstract static class OpSendGenericMessage extends AsyncOperationImpl
  {
    OpSendGenericMessage(
      String opName,
      String dstLoginId,
      Session session, PluginContext ctx)
      throws MessagingNetworkException
    {
      super(opName, session, ctx, false);
      this.dstLoginId = dstLoginId;
    }
    

    protected final void onStart() throws MessagingNetworkException, InterruptedException
    {
      try
      {
        /*
        synchronized (getThisAsyncOperation())
        {
          
          if (isFinished()) return;
          if (attempt > 5)
          {
            throw new MessagingNetworkException(
              "too many ("+(attempt-1)+") resend attempts failed: can't send message to "+dstLoginId+".",
              MessagingNetworkException.CATEGORY_STILL_CONNECTED,
              MessagingNetworkException.ENDUSER_MESSAGING_SERVER_REPORTS_CANNOT_SEND_MESSAGE);
          }
          runAt(System.currentTimeMillis() + retryTime,
            new MessagingTask("resend attempt #"+(attempt++), getThisAsyncOperation())
            {
              public void run() throws Exception
              {
                if (isFinished()) return;
                retry();
              }
            }
          );
          retryTime += retryDelta;
        }
        */
        onStartGenericMessageOperation();
      }
      catch (Exception ex)
      {
        fail(ex);
        session.handleException(ex, ctx);
      }
    }

    protected abstract void onStartGenericMessageOperation()
      throws MessagingNetworkException, InterruptedException;

    private final void retry() throws Exception
    {
      onStart();
    }

    protected final void onFinished() {
    }

    final synchronized void ackReceived() {
      if (isFinished()) return; //mustn't affect session which can already do another operation
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ack received: "+this);
      session.sendGenericMessageComplete();
      success();
    }

    /*
    public final void setRequestServerAck(boolean b)
    {
      this.requestServerAck = b;      
    }
    
    public final void setRequestClientAck(boolean b)
    {
      this.requestClientAck = b;
    }
    */
    
    public final void setWaitForAck(boolean b)
    {
      this.waitForAck = b;
    }
    
    final void msgErrorReceived(int errCode)
    {
      fail(new MessagingNetworkException(
        "icq server reports: error sending message to "+dstLoginId+
        ", errorcode: "+HexUtil.toHexString0x(errCode)+".",
        MessagingNetworkException.CATEGORY_STILL_CONNECTED,
        MessagingNetworkException.ENDUSER_MESSAGING_SERVER_REPORTS_CANNOT_SEND_MESSAGE));
    }

    final void messageBodySent()
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("waiting for an ack: "+waitForAck+" ("+this+")");
      if (!waitForAck) success();
    }

    private boolean waitForAck = false;
    //private boolean requestServerAck = false;
    //private boolean requestClientAck = false;
    final String dstLoginId;
    private int retryTime = 61000;
    private int attempt = 1;
    private static final int retryDelta = 0;
  }
}
