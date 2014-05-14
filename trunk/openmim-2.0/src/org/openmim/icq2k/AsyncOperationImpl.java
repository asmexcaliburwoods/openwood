package org.openmim.icq2k;

import java.util.*;

import org.openmim.*;
import org.openmim.messaging_network.MessagingNetworkException;
import org.openmim.messaging_network.MessagingNetworkListener;
import org.openmim.stuff.AsyncOperation;
import org.openmim.stuff.Defines;
import org.openmim.icq.utils.*;
import org.openmim.icq.util.*;

public abstract class AsyncOperationImpl implements AsyncOperation
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(AsyncOperationImpl.class);

  /** Session cannot be null here */
  protected AsyncOperationImpl(
    String name,
    Session session,
    PluginContext ctx,
    boolean timeoutLogsOff)
  throws MessagingNetworkException
  {
    try
    {
      if (session == null){
          Lang.ASSERT_NOT_NULL(session, "session");
          //unreachable
          throw new AssertionError();
      }
      this.session = session;
      this.ctx = ctx;
      id = ctx.getAsyncOperationRegistry().addOperation(this);
      //shortName = name;
      //this.name = name + ", src=" + session.getLoginId()+", op.id="+id.longValue();
      this.timeoutLogsOff = timeoutLogsOff;
      session.addOperation(this);
    }
    catch (InterruptedException ex)
    {
      session.handleException(ex, ctx);
      throw new AssertException("this point is never reached");
    }
  }

  protected abstract void onStart() throws MessagingNetworkException, InterruptedException;
  protected abstract void onFinished();
  protected abstract void onSuccess();
  protected abstract void onFail(MessagingNetworkException mex);

  private long st;
  public void setStartTime(long t) { st=t; }
  public long getStartTime() { return st; }
  
  //AsyncOperation interface method
  /** Call this to start the operation. */
  public final void start()
  throws MessagingNetworkException, InterruptedException
  {
    if (isFinished()) return;
    checkOFL(this);
    session.addCurrentOperation(this);
    runNow(
      new MessagingTask(getShortName(), this)
      {
        public void run() throws Exception
        {
          if (isFinished()) return;
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("started: "+this);
          onStart();
        }
      }
    );
  }

  public final AsyncOperation getThisAsyncOperation()
  {
    return this;
  }

  public final void waitUntilFinished()
  throws MessagingNetworkException, InterruptedException
  {
    synchronized (finishLock)
    {
      while (!finished) { ThreadUtil.checkInterrupted(); finishLock.wait(); }
      if (failException != null) 
      { 
        if (CAT.isDebugEnabled()) 
          CAT.debug("wuf/fail "+failException);
        throw failException;
      }
      else
      {
        if (CAT.isDebugEnabled()) 
        {
          CAT.debug("wuf/succ");
          if (Defines.DEBUG_DUMPSTACKS_EVERYWHERE) CAT.debug("wuf succ", new Exception("dumpstack"));
        }
      }
    }
  }

  public final void disableAsyncEvents()
  {
    synchronized (this)
    {
      disableAsyncEvents = true;
    }
  }

  private final void complete()
  {
    synchronized (finishLock)
    {
      finishLock.notifyAll();
    }

    OperationFinishedListener l = getOFL();
    if (l != null) l.finished();
    if (!disableAsyncEvents) onFinished();
    session.removeOperation(this);
    ctx.getAsyncOperationRegistry().removeOperation(this);
    galRemove();
  }

  private final void unscheduleTasks(MessagingNetworkException ex)
  {
    Iterator it = tasks.iterator();
    while (it.hasNext())
    {
      MessagingTask t = (MessagingTask) it.next();
      session.cancel(t);
      if (ex != null)
      {
        try
        {
          t.cancel(ex);
        }
        catch (Exception ex1)
        {
          session.eatException(ex1);
        }
      }
    }
    tasks.clear();
  }

  //AsyncOperation interface method
  /** Call this to finish the operation abnormally. */
  public final void fail(Throwable tr)
  {
    synchronized (this)
    {
      //prevent duplicate or late events (late timeouts, late session close, etc.)
      synchronized (finishLock)
      {
        if (finished) return;
        finished = true;

        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("completed/fail: "+this, tr);

        try
        {
          session.handleException(tr, "unk", ctx, true, false/*WRONG!*/); //TODO
        }
        catch (MessagingNetworkException mex)
        {
          failException = mex;
          unscheduleTasks(mex);
          complete();
          OperationFinishedListener l = getOFL();
          if (l == null)
          {
            if (Defines.DEBUG_DUMPSTACKS_EVERYWHERE)
              if (Defines.DEBUG && CAT.isDebugEnabled())
                CAT.debug("OperationFinishedListener is null", new Exception("dumpStack"));
          }
          else l.fail(mex);
          if (!disableAsyncEvents || internalListener != null) onFail(mex);
        }
      }
    }
  }

  private final synchronized OperationFinishedListener getOFL()
  {
    return operationFinishedListener;
  }

  public final static void checkOFL(AsyncOperation op)
  {
    //if (((AsyncOperationImpl)op).getOFL() == null)
    //  CAT.error("", new AssertException("OperationFinishedListener is null!  Will not be able to wake up the next opeartion."));
  }

  /** Call this to finish the operation successfully. */
  public final void success()
  {
    synchronized (this)
    {
      //prevent duplicate or late events (late success)
      synchronized (finishLock)
      {
        if (finished) return;
        finished = true;

        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("completed/success: "+this);
        unscheduleTasks(null);
        complete();
        OperationFinishedListener l = getOFL();
        if (l == null)
        {
          if (Defines.DEBUG_DUMPSTACKS_EVERYWHERE)
            if (Defines.DEBUG && CAT.isDebugEnabled())
              CAT.debug("OperationFinishedListener is null", new Exception("dumpStack"));
        }
        else l.success();
        if (!disableAsyncEvents || internalListener != null)
        {
          onSuccess();
        }
      }
    }
  }

  //AsyncOperation interface method
  /** Currently, implementations are allowed to throw an exception
      on attempt to set second listener.
  */
  public final synchronized void addOperationFinishedListener(
    OperationFinishedListener l)
  {
    if (operationFinishedListener != null)
      throw new AssertException("this.opFinishedListener must be null here, but it is "+operationFinishedListener);
    if (l == null)
      throw new AssertException("`l' must be not be null here, but it is.");
    operationFinishedListener = l;
  }

  //AsyncOperation interface method
  /** Adds a task.  These tasks will be unscheduled (i.e. canceled)
      when this operation finishes.  Operation should not attempt
      to run, execute or schedule these tasks.
      If operation is closed, throws AssertException, and cancels register.
  */
  public final synchronized void registerTask(MessagingTask task)
  throws MessagingNetworkException
  {
    expectAlive();
    tasks.add(task);
  }

  //AsyncOperation interface method
  /** Returns operation id.
      Scope of this id is implementation-defined. */
  public final long getId()
  {
    return id.longValue();
  }

  public final Long getIdLong()
  {
    return id;
  }

  public final Session getSession()
  {
    return session;
  }

  //AsyncOperation interface method
  public final boolean timeoutLogsOff()
  {
    return timeoutLogsOff;
  }

  protected final void expectAlive()
  throws MessagingNetworkException
  {
    if (isFinished())
      MLang.EXPECT(false,
          "mim bug: op must be alive, but it is finished.",
          MessagingNetworkException.CATEGORY_STILL_CONNECTED,
          MessagingNetworkException.ENDUSER_MIM_BUG
        );
  }

  public final synchronized void runNow(MessagingTask task)
  throws MessagingNetworkException
  {
    expectAlive();
    tasks.add(task);
    session.runNow(task);
  }

  public final synchronized void runAt(long time, MessagingTask task)
  throws MessagingNetworkException
  {
    expectAlive();
    tasks.add(task);
    session.runAt(time, task);
  }

  public final synchronized void cancel(MessagingTask task)
  {
    tasks.remove(task);
    session.cancel(task);
  }

  public final void setInternalListener(MessagingNetworkListener l)
  {
    internalListener = l;
  }

  protected final Long id;
  protected final Session session;
  final PluginContext ctx;
  //private final String shortName;
  //private final String aoName;

  public final String getShortName() { return getClass().getName(); } //shortName; }
  public final String toString() { return getShortName() + ", src=" + session.getLoginId()+", op.id="+id.longValue(); } //aoName; }

  protected MessagingNetworkListener internalListener;

  //timeout task, request task
  //(response task is created by flap receiver).
  private final HashSet tasks = new HashSet(2);
  private final boolean timeoutLogsOff;
  private boolean finished = false;
  private final Object finishLock = new Object();
  private boolean disableAsyncEvents = false;
  private MessagingNetworkException failException = null;

  private OperationFinishedListener operationFinishedListener;

  /**
    If this operation is failed previously, clones last exception & throws it.
    Otherwise, does nothing.
  */
  public synchronized void rethrowExceptionIfFailed() throws MessagingNetworkException
  {
    MessagingNetworkException ex = failException;
    if (ex != null) ex.throwCloned();
  }

  /**
    Returns true iff this operation is completed previously.
  */
  public final boolean isFinished()
  {
    synchronized (finishLock)
    {
      return finished;
    }
  }

  private boolean galRemoved = false;

  /** See GlobalAopLimit class. */
  public final synchronized void galRemove()
  {
    if (!galRemoved)
    {
      galRemoved = true;
      ctx.getGal().removeAop();
    }
  }
}
