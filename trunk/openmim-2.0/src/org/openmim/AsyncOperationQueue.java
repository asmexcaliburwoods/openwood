package org.openmim;

import java.util.*;

import org.openmim.icq.util.joe.*;
import org.openmim.icq2k.MessagingTask;
import org.openmim.mn.MessagingNetworkException;

/**
session aopq must prevent dos attacks via fail()ing aops when session aopq is full.
also, remove smq at all.
this way, session aopq & smq will not have wait() calls inside, so hl Scheduler thread will never be blocked.
*/
public class AsyncOperationQueue
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(AsyncOperationQueue.class.getName());

  private final int MAX_QUEUE_SIZE;

  public AsyncOperationQueue(int MAX_QUEUE_SIZE)
  {
    this.MAX_QUEUE_SIZE = MAX_QUEUE_SIZE;
  }

  public final long enqueue(final AsyncOperation op, long startTime, final long timeoutMillis/*timeout after_start*/)
  throws MessagingNetworkException, InterruptedException
  {
    op.setStartTime(Math.max(startTime, System.currentTimeMillis()));
    
    boolean start = false;
    synchronized (this)
    {
      if (isStopped())
      {
        op.fail(error);
        return op.getId();
      }

      if (q.size() >= MAX_QUEUE_SIZE)
      {
        //if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.WARN))
        //  CAT.warn("aopq is full; waiting to execute op: "+op);
        //while (!isStopped() && q.size() >= MAX_QUEUE_SIZE) wait();
        boolean timeoutLogsOff = op.timeoutLogsOff();
        MessagingNetworkException mex = new MessagingNetworkException(
          "session aopq overflow, aop rejected: "+op,
          (timeoutLogsOff ?
            MessagingNetworkException.CATEGORY_NOT_CATEGORIZED :
            MessagingNetworkException.CATEGORY_STILL_CONNECTED),
          (timeoutLogsOff ?
            MessagingNetworkException.ENDUSER_OPERATION_REJECTED_USER_TOO_ACTIVE_LOGGED_OFF :
            MessagingNetworkException.ENDUSER_OPERATION_REJECTED_USER_TOO_ACTIVE_NOT_LOGGED_OFF)
        );
        op.fail(mex);
        op.rethrowExceptionIfFailed();
        return op.getId();
      }


      //startTimeoutTask
      {
        MessagingTask timeoutTask = new MessagingTask("timeout", op.getSession())
        {
          public void run() throws Exception
          {
            boolean timeoutLogsOff = op.timeoutLogsOff();
            MessagingNetworkException mex = new MessagingNetworkException(
              "operation has timed out: "+op,
              (timeoutLogsOff ?
                MessagingNetworkException.CATEGORY_NOT_CATEGORIZED :
                MessagingNetworkException.CATEGORY_STILL_CONNECTED),
              (timeoutLogsOff ?
                MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_MESSAGING_OPERATION_TIMEOUT :
                MessagingNetworkException.ENDUSER_MESSAGING_OPERATION_TIMED_OUT_NOT_LOGGED_OFF)
            );
            op.fail(mex);
          }
        };

        op.registerTask(timeoutTask);
        op.runAt(op.getStartTime()+timeoutMillis, timeoutTask);
      }
      //end startTimeoutTask

      op.rethrowExceptionIfFailed();
      if (op.isFinished()) return op.getId();

      //if (operationInProgress != null && operationInProgress.isFinished()) operationInProgress = null;

      start = (operationInProgress == null);

      if (start)
        prepareNextOperation(op);
      else
      {
        q.add(op);
        printState(true, op);
      }
    }
    if (start)
    {
      org.openmim.icq2k.AsyncOperationImpl.checkOFL(op);
      scheduleStart(op);
    }
    return op.getId();
  }

  public final void runSynchronous(AsyncOperation op, long startTime, long timeout/*after_start*/)
  throws MessagingNetworkException, InterruptedException
  {
    op.disableAsyncEvents();
    enqueue(op, startTime, timeout);
    op.waitUntilFinished();
  }

  private void scheduleStart(final AsyncOperation op)
  {
    long st=op.getStartTime();
    if (st<=System.currentTimeMillis()) start(op);
    else
    {
      //sched to startTime
      MessagingTask t = new MessagingTask("wait for startTime", op.getSession())
      {
        public void run() throws Exception
        {
          start(op);
        }
      };

      try
      {
        op.registerTask(t);
        op.runAt(op.getStartTime(), t);
      }
      catch(Exception ex)
      {
        op.fail(ex);
      }
      //end sched to startTime
    }
  }
  
  private void start(AsyncOperation op)
  {
    if (Defines.DEBUG && CAT.isDebugEnabled())
      CAT.debug("aopq "+this+": next(); aopq size: "+q.size()+"; current op: "+operationInProgress);
    try
    {
      op.start();

      if (this.lastOperationFinishedListener != null && op.isFinished())
      {
        lastOperationFinishedListener.finished();
      }
    }
    catch (Exception ex)
    {
      op.fail(ex);
    }
  }

  /** In this call, all operations fail immediately. */
  private void failAll(Throwable tr)
  {
    AsyncOperation operationInProgress = null;

    for (int i = 0; i < q.size(); ++i)
    {
      AsyncOperation op = (AsyncOperation) q.get(i);
      op.fail(tr);
    }

    q.clear();
    printState(false, null);


    if (this.operationInProgress != null)
    {
      operationInProgress = this.operationInProgress;
      this.operationInProgress = null;
      this.lastOperationFinishedListener = null;
    }

    //must not be synchronized to prevent possible deadlock
    //with OperationFinishedListener;
    //see this.startNextOperation(AsyncOperation) below.
    if (operationInProgress != null)
      operationInProgress.fail(tr);
  }

  private AsyncOperation.OperationFinishedListener lastOperationFinishedListener = null;

  private void prepareNextOperation(AsyncOperation op)
  {
    //if (closed) { op.fail(newClosedException()); return; }

    if (op == null) Lang.ASSERT_NOT_NULL(op, "op");

    operationInProgress = op;

    op.addOperationFinishedListener(
      lastOperationFinishedListener =
      new AsyncOperation.OperationFinishedListener()
      {
        private boolean OperationFinishedListener_finished = false;
        public void finished()
        {
          AsyncOperation opNext = null;
          synchronized (AsyncOperationQueue.this)
          {
            if (OperationFinishedListener_finished) return;
            OperationFinishedListener_finished = true;
            //notify operation waiting to become enqueue()d
            AsyncOperationQueue.this.notify();
            while (true)
            {
              if (q.isEmpty() || AsyncOperationQueue.this.isStopped())
              {
                operationInProgress = null;
                lastOperationFinishedListener = null;
                return;
              }
              else
              {
                opNext = (AsyncOperation) q.remove(0);
                printState(false, opNext);
                if (!opNext.isFinished()) break;
              }
            }
            prepareNextOperation(opNext);
          }
          scheduleStart(opNext);
        }
        public void success() {}
        public void fail(MessagingNetworkException mex) {}
      }
    );
  }

  private final ArrayList q = new ArrayList(1);
  private AsyncOperation operationInProgress = null;
  private Throwable error = null;

  public synchronized void stop(Throwable error)
  {
    this.error = error;
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("aopq "+this+" stopped");
    failAll(error);
    notifyAll();
  }

  public synchronized boolean isStopped()
  {
    return error != null;
  }

  public synchronized void resume()
  {
    this.error = null;
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("aopq "+this+" resumed");
  }

  private void printState(boolean added, AsyncOperation op)
  {
    if (Defines.DEBUG && CAT.isDebugEnabled())
    {
      if (op == null)
      {
        CAT.debug("aopq size: "+q.size()+" (just cleared)");
      }
      else
      {
        CAT.debug("aopq size: "+q.size()+", aop just "+(added?"added":"removed")+": "+op);
      }
    }
  }
}
