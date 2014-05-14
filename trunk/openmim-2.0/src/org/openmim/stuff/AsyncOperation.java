package org.openmim.stuff;

import org.openmim.icq2k.MessagingTask;
import org.openmim.icq2k.Session;
import org.openmim.messaging_network.MessagingNetworkException;

public interface AsyncOperation
{
  public interface OperationFinishedListener
  {
    /** Exceuted when finished (successfully or failed). */
    public void finished();
    /** Executed when finished successfully. */
    public void success();
    /** Executed when finished failed. */
    public void fail(MessagingNetworkException mex);
  }

  public void setStartTime(long t);
  public long getStartTime();
  
  /** Enables easy implementation of synchronous equivalents. 
      If the async operation fails, throws its exception.
      @see #disableAsyncEvents()
  */
  public void waitUntilFinished()
  throws MessagingNetworkException, InterruptedException;
  
  /** Enables easy implementation of synchronous equivalents. 
      Asynchronous results events will not be fired. 
      @see #waitUntilFinished()
  */
  public void disableAsyncEvents();
  
  
  /** 
    Call this to start the operation. 
    It is possible that start() is called after fail(...) call, 
    so start() must check if the operation is already finished 
    (e.g. when the operation has timed out before its start()). 
  */
  public void start()
  throws MessagingNetworkException, InterruptedException;
  
  /** Call this to finish the operation abnormally. */
  public void fail(Throwable tr);
  
  /** Call this to finish the operation successfully. */
  public void success();

  /** Currently, implementations are allowed to throw an exception 
      on attempt to set second listener. 
  */
  public void addOperationFinishedListener(OperationFinishedListener l);

  /** Adds a task.  These tasks will be unscheduled (i.e. canceled) 
      when this operation finishes.  Operation should not attempt 
      to run, execute or schedule these tasks. 
      
      Throws MessagingNetworkException if operation is finished.
  */
  public void registerTask(MessagingTask task) 
  throws MessagingNetworkException;
  
  /** Returns operation id.  
      Scope of this id is implementation-defined. */
  public long getId();
  
  /** Returns the same id. */
  public Long getIdLong();
  
  public boolean timeoutLogsOff();


  /** Executes the task in the context of this operation.  
      Throws an exception if the operation is already finished. */
  public void runNow(MessagingTask task)
  throws MessagingNetworkException;
  
  /** Executes the task in the context of this operation.  
      Throws an exception if the operation is already finished. */
  public void runAt(long time, MessagingTask task)
  throws MessagingNetworkException;
  
  /** Cancels the task. */
  public void cancel(MessagingTask task);

  public String getShortName();

  /** 
    If this operation is failed previously, clones last exception & throws it. 
    Otherwise, does nothing. 
  */
  public void rethrowExceptionIfFailed() throws MessagingNetworkException;
  
  /** 
    Returns true iff this operation is completed previously. 
  */
  public boolean isFinished();
  
  public Session getSession();
}
