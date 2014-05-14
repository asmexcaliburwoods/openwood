package org.openmim.transport_simpletcp;

import java.util.*;

import org.openmim.*;
import org.openmim.infrastructure.taskmanager.ThreadPool;
import org.openmim.stuff.Defines;

final class Dispatcher extends Thread
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(Dispatcher.class.getName());
  private static int count = 1;

  private ThreadPool threadPool;
  private SocketRegistry sr;

  public Dispatcher(SocketRegistry sr, int maxThreads, int optimumThreads)
  {
    super("simpletcp dispatcher thread #"+(count++));
    threadPool = new ThreadPool(maxThreads, optimumThreads);
    this.sr = sr;
  }

  public void run()
  {
    try
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(this+" started");
      for (;;)
      {
        try
        {
          if (isInterrupted()) return;
          Enumeration e = sr.getSocketWrappers();
          while (e.hasMoreElements())
          {
            if (isInterrupted()) return;
            SocketWrapper w = (SocketWrapper) e.nextElement();
            try
            {
              if (w.isClosed()) sr.remove(w);
              else w.service(threadPool);
            }
            catch (Exception ex)
            {
              if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) 
                CAT.error("unhandled exception while w.service(); ignored", ex);
            }
          }
          sleep(20);
        }
        //interrupted* is never thrown
        catch (Throwable tr)
        {
          if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR))
            CAT.error("unhandled exception while "+this+"; ignored.", tr);
        }
      }
    }
    //interrupted* is never thrown
    catch (Throwable tr)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) 
        CAT.error("unhandled exception while "+this, tr);
    }
    finally
    {
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("stopping transport_simpletcp threadpool");
      threadPool.stop(5000);
      if (Defines.DEBUG && CAT.isInfoEnabled()) 
      {
        CAT.info("stopped transport_simpletcp threadpool");
        CAT.info(this+" finished");
      }
    }
  }
}