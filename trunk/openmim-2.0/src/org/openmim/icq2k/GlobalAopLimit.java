package org.openmim.icq2k;

import org.openmim.*;
import org.openmim.mn.MessagingNetworkException;
import org.openmim.icq.util.joe.*;

public class GlobalAopLimit
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(GlobalAopLimit.class.getName());

  private static int MAX_AOP_COUNT        = ICQ2KMessagingNetwork.REQPARAM_GLOBAL_MAX_ASYNCOP_COUNT;
  private static boolean OVERFLOW_BLOCKS  = ICQ2KMessagingNetwork.REQPARAM_GLOBAL_MAX_ASYNCOP_COUNT_EXCEEDED_BLOCKS;

  static
  {
    Lang.ASSERT_POSITIVE(MAX_AOP_COUNT, "MAX_AOP_COUNT");
  }
  
  public GlobalAopLimit() {}
  
  private int aopCount = 0;
  
  public final synchronized void addAop(boolean overflowLogsOff)
  throws MessagingNetworkException
  {
    while (aopCount >= MAX_AOP_COUNT)
    {
      if (OVERFLOW_BLOCKS)
      {
        if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.WARN))
          CAT.warn("global aop limit reached: "+aopCount+", waiting");
        try
        {
          wait();
        }
        catch (InterruptedException ix)
        {
          MessagingNetworkException mex = new MessagingNetworkException(
            "(thread interrupted) global aop limit reached, aop rejected",
            (overflowLogsOff ?
              MessagingNetworkException.CATEGORY_NOT_CATEGORIZED :
              MessagingNetworkException.CATEGORY_STILL_CONNECTED),
            (overflowLogsOff ?
              MessagingNetworkException.ENDUSER_OPERATION_REJECTED_SERVER_TOO_BUSY_LOGGED_OFF :
              MessagingNetworkException.ENDUSER_OPERATION_REJECTED_SERVER_TOO_BUSY_NOT_LOGGED_OFF)
          );
          throw mex;
        }
      }
      else
      {
        MessagingNetworkException mex = new MessagingNetworkException(
          "global aop limit reached, aop rejected",
          (overflowLogsOff ?
            MessagingNetworkException.CATEGORY_NOT_CATEGORIZED :
            MessagingNetworkException.CATEGORY_STILL_CONNECTED),
          (overflowLogsOff ?
            MessagingNetworkException.ENDUSER_OPERATION_REJECTED_SERVER_TOO_BUSY_LOGGED_OFF :
            MessagingNetworkException.ENDUSER_OPERATION_REJECTED_SERVER_TOO_BUSY_NOT_LOGGED_OFF)
        );
        throw mex;
      }
    }
    ++aopCount;
    printState();
  }
  
  final synchronized void addForcedAop()
  {
    ++aopCount;
    printState();
  }
  
  public final synchronized void removeAop()
  {
    if (aopCount < 1) 
    { 
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) 
        CAT.error("resetting aopCount to 1", new AssertException("aopCount must be >= 1 here, but it is "+aopCount));
      aopCount = 1;
    }
    notify();
    --aopCount;
    printState();
  }
  
  public synchronized void init()
  {
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("icq2k: gal.init() enter");
    aopCount = 0;
    printState();
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("icq2k: gal.init() leave");
  }

  public void deinit()
  {
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("icq2k: gal.deinit() enter");
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("icq2k: gal.deinit() leave");
  }
  
  private final void printState()
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) 
      if (Defines.DEBUG_DUMPSTACKS_EVERYWHERE)
        CAT.debug("global aop count: "+aopCount, new Exception("dumpstack"));
      else
        CAT.debug("global aop count: "+aopCount);
  }
}