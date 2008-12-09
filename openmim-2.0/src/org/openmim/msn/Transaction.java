package org.openmim.msn;

import java.io.*;

import org.openmim.*;
import org.openmim.mn.MessagingNetworkException;

public abstract class Transaction
{
  protected final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(Transaction.class.getName());

  protected int trid;
  private long stopTime;
  private boolean finished;
  private Throwable throwable = null;
  private boolean timeoutDoesLogoff;

  protected Transaction()
  {
  }

  protected Transaction getThisTransaction()
  {
    return this;
  }

  public final int getTrID()
  {
    return trid;
  }

  public final void init(int trid)
  {
    //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("t init");
    finished = false;
    this.trid = trid;
    stopTime = System.currentTimeMillis() + 1000 * MSNMessagingNetwork.REQPARAM_SOCKET_TIMEOUT_SECONDS;
  }

  public abstract void clientRequest(TransactionContext ctx)
  throws IOException, InterruptedException, MessagingNetworkException;

  public abstract void serverResponse(String cmd, String args, TransactionContext tctx, Session ses, PluginContext pctx)
  throws IOException, InterruptedException, MessagingNetworkException;

  public void errorServerResponse(ErrorInfo errorInfo, String args, TransactionContext tctx, Session ses, PluginContext pctx)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    //unhandled error responses close the current ServerConnection
    MessagingNetworkException ex = Errors.createException(errorInfo);
    finish(ex);
    if (tctx.isNS())
    {
      if (errorInfo.killNS)
        tctx.closeServerConnection(ex.getMessage(), ex.getLogger(), ex.getEndUserReasonCode());
    }
    else
    {
      if (errorInfo.killSSS)
        tctx.closeServerConnection(ex.getMessage(), ex.getLogger(), ex.getEndUserReasonCode());
    }
    throw Errors.createException(errorInfo);
  }

  /** Emits a line to server */
  protected final void post(String cmd, String args, TransactionContext ctx)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    ctx.post(trid, cmd, args);
  }

  /** Emits a MSG to server.  args do not include msgBody length in bytes */
  protected final void postMSG(String args, String msgBody, TransactionContext ctx)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    ctx.postMSG(trid, args, msgBody);
  }

  public final void finish()
  {
    finish(null);
  }

  //IOException, InterruptedException, MessagingNetworkException
  public final synchronized void finish(Throwable tr)
  {
    if (!finished)
    {
      if (tr != null) throwable = tr;
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("t #"+trid+" finished"+(throwable == null ? "" : ": "+throwable));
      finished = true;
      notifyAll();
    }
  }

  public synchronized boolean isFinished()
  {
    return finished;
  }

  /** Returns true if timed out */
  public final synchronized boolean checkTimeout()
  {
    if (System.currentTimeMillis() >= stopTime)
    {
      if (!finished)
      {
        if (timeoutDoesLogoff)
          finish(new MessagingNetworkException("t #"+trid+" expired",
            MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
            MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_MESSAGING_OPERATION_TIMEOUT));
        else
          finish(new MessagingNetworkException("t #"+trid+" expired",
            MessagingNetworkException.CATEGORY_STILL_CONNECTED,
            MessagingNetworkException.ENDUSER_MESSAGING_OPERATION_TIMED_OUT_NOT_LOGGED_OFF));
      }
      return true;
    }
    return false;
  }

  /** Throws MessagingNetworkException on operation timeout */
  public synchronized final void waitFor(boolean timeoutDoesLogoff, Session ses, PluginContext pctx)
  throws InterruptedException,MessagingNetworkException
  {
    if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
    this.timeoutDoesLogoff = timeoutDoesLogoff;
    if (finished) { endWait(ses, pctx); return; }
    wait();
    checkTimeout();
    endWait(ses, pctx);
  }

  private synchronized void endWait(Session ses, PluginContext pctx)
  throws MessagingNetworkException
  {
    if (throwable != null)
      ses.handleException(throwable, "transaction #"+trid, pctx, true, true);
  }
}