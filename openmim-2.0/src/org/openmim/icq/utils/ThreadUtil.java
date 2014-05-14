package org.openmim.icq.utils;

import java.util.*;

public final class ThreadUtil
{
  public static void sleep(long millis) throws RuntimeException
  {
    try
    {
      Thread.currentThread().sleep(millis);
    }
    catch (InterruptedException ex)
    {
      Logger.printException(ex);
      throw new RuntimeException("interrupted");
    }
  }

  public static void checkInterrupted() 
  throws InterruptedException
  {
    if (Thread.currentThread().isInterrupted())
      throw new InterruptedException();
  }
}
