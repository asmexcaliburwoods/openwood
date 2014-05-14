package org.openmim.msn;

import java.util.*;

import org.openmim.*;
import org.openmim.infrastructure.taskmanager.ThreadPool;
import org.openmim.stuff.Defines;


public final class ServeSessionsThread extends Thread
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(ServeSessionsThread.class.getName());
  private PluginContext context;
  private static int count = 1;

  public ServeSessionsThread(PluginContext ctx)
  {
    super("sst"+(count++));
    this.context = ctx;
  }

  private final ThreadPool threadPool = new ThreadPool(
    MSNMessagingNetwork.REQPARAM_INPUT_DATA_HANDLING_THREADCOUNT_MAXIMUM,
    MSNMessagingNetwork.REQPARAM_INPUT_DATA_HANDLING_THREADCOUNT_OPTIMUM);

  public void run()
  {
    try
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("msn sst started");
      for (;;)
      {
        if (Thread.currentThread().isInterrupted())
          return;
        Enumeration e = context.getResourceManager().getSessions();
        while (e.hasMoreElements())
        {
          if (Thread.currentThread().isInterrupted())
            return;
          Session sess = (Session) e.nextElement();
          try
          {
            //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("/0.1/");
            if (sess.isRunning())
            {
              //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("/0.2/");
              if (!sess.isBusy())
              {
                //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("/0.3/");
                if (sess.isTickCallNeeded(context) && sess.lockBusy())
                {
                  //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("tm.execute(sess.getTickTask()), ses.login id="+sess.getLoginId());
                  threadPool.execute(sess.getTickTask(context));
                  //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("/0.7/");
                }
              }
            }
          }
          catch (Exception ex)
          {
            if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.WARN)) CAT.warn("exception while sess.tick()", ex);
          }
        }
        context.getResourceManager().waitForSessionListChange(20);
      }
    }
    catch (Throwable tr)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("exception while msn session loop", tr);
    }
    finally
    {
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("stopping msn inputdata threadpool");
      threadPool.stop(2000);
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("stopped, msn sess thread finished");
    }
  }
}