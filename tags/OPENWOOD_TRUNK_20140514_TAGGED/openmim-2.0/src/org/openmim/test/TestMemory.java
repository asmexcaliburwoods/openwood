package org.openmim.test;

import java.util.*;
import org.openmim.icq.util.joe.*;
import org.openmim.*;
import org.openmim.mn.MessagingNetwork;
import org.openmim.mn.MessagingNetworkAdapter;
import org.openmim.mn.MessagingNetworkException;

public class TestMemory
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(TestMemory.class.getName());
  
  public static int     REQPARAM_USER_COUNT;
  public static int     REQPARAM_REPORT_INTERVAL;
  public static boolean REQPARAM_MEASURE_KBYTES_PER_25K_USERS;
  public static boolean REQPARAM_TEST_SYNC_LOGINS;
  public static boolean REQPARAM_CALL_GC;
  public static boolean REQPARAM_SEND_EVENTS;  
  public static boolean REQPARAM_EVENTS_OVERFLOW;  
  public static double  REQPARAM_SEND_MESSAGES_PER_SECOND_PER_CORE;  
  public static double  REQPARAM_SEND_SETSTATUS_PER_SECOND_PER_CORE;  
  public static int     REQPARAM_STOP_SEND_AT_EVENT_COUNT;
  public static String  REQPARAM_PLUGIN_CLASS_NAME;  

  static
  {
    AutoConfig.fetchFromClassLocalResourceProperties(TestMemory.class, true, false);
  }
  
  private static MessagingNetwork plugin;

  private static long freeMemoryOnStartup = 0;
  private static long usedMemoryOnStartup = 0;

  private static long eventSendStartTimeMillis = 0;
  private static long messagesSent = 0;
  private static long setStatusSent = 0;
  private static long messagesTotal = 0;
  private static long setStatusTotal = 0;
  private static boolean stopSendEvents = false;
  
  private static boolean loggingIn = true;
  private static int usersLoggedIn = 0;
  private static int startLoginCalls = 0;
  private static int loginFailures = 0;
  
  private static final Object statLock = new Object();
  
  public static void main(String[] args)
  {
    try
    {
      System.err.println("logging is done using log4j.");
      
      if (REQPARAM_MEASURE_KBYTES_PER_25K_USERS) 
      {
        CAT.info("REQPARAM_MEASURE_KBYTES_PER_25K_USERS is true => USER_COUNT is reset to 1000 users.");
        REQPARAM_USER_COUNT = 1000;
      }
      
      Lang.ASSERT(Defines.ENABLE_FAKE_PLUGIN, "Defines.ENABLE_FAKE_PLUGIN");
      String className = REQPARAM_PLUGIN_CLASS_NAME;
      
      CAT.info("REQPARAM_USER_COUNT:      "+REQPARAM_USER_COUNT);
      CAT.info("REQPARAM_TEST_SYNC_LOGINS: "+REQPARAM_TEST_SYNC_LOGINS);
      long fm = Runtime.getRuntime().freeMemory();
      CAT.info("Runtime.freeMemory(): "+fm+" ("+(fm/1024/1024)+"MB)");
      fm = (long) (fm/REQPARAM_USER_COUNT);
      CAT.info("Runtime.freeMemory() per REQPARAM_USER_COUNT: "+fm+" ("+(fm/1024)+"KB)");
      fm = Runtime.getRuntime().totalMemory();
      CAT.info("Runtime.totalMemory(): "+fm+" ("+(fm/1024/1024)+"MB)");
      
      CAT.info("Instantiating class \"" + className + "\"");
      plugin = (MessagingNetwork) Class.forName(className).newInstance();
      plugin.init();
      
      freeMemoryOnStartup = Runtime.getRuntime().freeMemory();
      usedMemoryOnStartup = Runtime.getRuntime().totalMemory() - freeMemoryOnStartup;
      
      (new Thread("reporter")
      {
        public void run()
        {
          try
          {
            for(;;)
            {
              if (REQPARAM_CALL_GC) System.gc();
              Thread.sleep(5000);
              
              int usersServiced;
              if (REQPARAM_TEST_SYNC_LOGINS)
                usersServiced = usersLoggedIn;
              else
                usersServiced = usersLoggedIn + loginFailures;
                
              //if (!REQPARAM_MEASURE_KBYTES_PER_25K_USERS && usersServiced >= REQPARAM_USER_COUNT) break;
              reportMainStats();
              
              if (loggingIn && usersServiced >= REQPARAM_USER_COUNT)
              {
                synchronized (statLock)
                {
                  loggingIn = false;
                  statLock.notify();
                }
              }
              
              if (!REQPARAM_SEND_EVENTS && !REQPARAM_CALL_GC && usersServiced >= REQPARAM_USER_COUNT) 
              {
                CAT.info("All users are logged in, CALL_GC is turned on.");
                REQPARAM_CALL_GC = true;
              }
              else
              if (!REQPARAM_CALL_GC && stopSendEvents)
              {
                CAT.info("REQPARAM_STOP_SEND_AT_EVENT_COUNT reached, CALL_GC is turned on.");
                REQPARAM_CALL_GC = true;
              }
            }        
            
            //reportMainStats();
            //System.exit(0);
          }
          catch (Throwable tr)
          {
            CAT.error("Error while reporter thread", tr);
            reportMainStats();
            System.exit(2);
          }
        }
      }).start();
      
      CAT.info("Test started");
      
      if (REQPARAM_TEST_SYNC_LOGINS)
      {
        for (usersLoggedIn = 0; usersLoggedIn < REQPARAM_USER_COUNT; ++usersLoggedIn)
        {
          plugin.login(makeLoginId(usersLoggedIn), "pwd"+usersLoggedIn, 
            makeContactList(usersLoggedIn), 
            MessagingNetwork.STATUS_ONLINE);
          if (usersLoggedIn % REQPARAM_REPORT_INTERVAL == 0) report("usersLoggedIn", usersLoggedIn);
        }
      }
      else
      {
        plugin.addMessagingNetworkListener(new MessagingNetworkAdapter()
        {
          public void messageReceived(byte networkId, String srcLoginId, String dstLoginId, String text)
          {
            synchronized (statLock)
            {
              ++messagesTotal;
            }
          }
  
          public void sendMessageFailed(byte networkId, long operationId,
            String originalMessageSrcLoginId, String originalMessageDstLoginId, String originalMessageText,
            MessagingNetworkException ex) 
          {
            synchronized (statLock)
            {
              ++messagesTotal;
            }
          }

          /** @see MessagingNetwork#startSendMessage(String, String, String) */
          public void sendMessageSuccess(byte networkId, long operationId,
            String originalMessageSrcLoginId, String originalMessageDstLoginId, String originalMessageText)
          {
            synchronized (statLock)
            {
              ++messagesTotal;
            }
          }
            
          public void statusChanged(byte networkId, String srcLoginId, String dstLoginId,
            int status, int reasonLogger, String reasonMessage, int endUserReasonCode) 
          {
            synchronized (statLock)
            {
              if (loggingIn)
              {
                if (status != MessagingNetwork.STATUS_OFFLINE && srcLoginId.equals(dstLoginId))
                  ++usersLoggedIn;
              }
              else
                ++setStatusTotal;
            }
            if (endUserReasonCode != MessagingNetworkException.ENDUSER_NO_ERROR)
              CAT.error("logged off", new MessagingNetworkException(reasonMessage, reasonLogger, endUserReasonCode));
          }
          
          public void setStatusFailed(byte networkId, long operationId, String originalSrcLoginId,
            MessagingNetworkException ex)
          {
            synchronized (statLock)
            {
              if (loggingIn)
              {
                ++loginFailures;
                if (loginFailures % REQPARAM_REPORT_INTERVAL == 0) report("loginFailures", loginFailures);
              }
              else
                ++setStatusTotal;
            }
            CAT.error("logged off", ex);
          }
        });
        for (startLoginCalls = 0; startLoginCalls < REQPARAM_USER_COUNT; ++startLoginCalls)
        {
          plugin.startLogin(makeLoginId(startLoginCalls), "pwd"+startLoginCalls, 
            makeContactList(startLoginCalls),
            MessagingNetwork.STATUS_ONLINE);
          //if (startLoginCalls % REQPARAM_REPORT_INTERVAL == 0) report("startLoginCalls", startLoginCalls);
        }
      }
      
      if (REQPARAM_SEND_EVENTS)
      {
        /*
          public static boolean REQPARAM_SEND_EVENTS;  
          public static double  REQPARAM_SEND_MESSAGES_PER_SECOND_PER_CORE;  
          public static double  REQPARAM_SEND_SETSTATUS_PER_SECOND_PER_CORE;  
          private static long messagesSent = 0;
          private static long setStatusSent = 0;
          eventSendStartTimeMillis
        */
        CAT.info("Waiting for login to finish");
        synchronized (statLock)
        {
          while (loggingIn)
          {
            statLock.wait();
          }
          eventSendStartTimeMillis = System.currentTimeMillis();
        }
        CAT.info("Logged in, starting sending events");
        for (;;)
        {
          if (!REQPARAM_EVENTS_OVERFLOW)
          {
            Thread.sleep(1000);
            synchronized (statLock)
            {
              if (messagesTotal + setStatusTotal >= REQPARAM_STOP_SEND_AT_EVENT_COUNT)
              {
                stopSendEvents = true;
                break;
              }
              long secondsElapsed = (System.currentTimeMillis() - eventSendStartTimeMillis) / 1000;
              /*
              if (messagesSent > Long.MAX_VALUE/2 || setStatusSent > Long.MAX_VALUE/2 ) 
              {
                messagesSent = 0;
                setStatusSent = 0;
                eventSendStartTimeMillis = System.currentTimeMillis();
              }
              */
              
              while (secondsElapsed * REQPARAM_SEND_MESSAGES_PER_SECOND_PER_CORE > messagesSent)
              {
                ++messagesSent;
                plugin.startSendMessage(
                  makeLoginId(RandomUtil.random(0, REQPARAM_USER_COUNT-1)), 
                  makeLoginId(RandomUtil.random(0, REQPARAM_USER_COUNT-1)), 
                  "msg "+RandomUtil.random(0, REQPARAM_USER_COUNT-1));
              }
              
              while (secondsElapsed * REQPARAM_SEND_SETSTATUS_PER_SECOND_PER_CORE > setStatusSent)
              {
                ++setStatusSent;
                String id = makeLoginId(RandomUtil.random(0, REQPARAM_USER_COUNT-1));
                plugin.setClientStatus(
                  id, 
                  plugin.getClientStatus(id) == MessagingNetwork.STATUS_ONLINE ? 
                    MessagingNetwork.STATUS_BUSY : 
                    MessagingNetwork.STATUS_ONLINE);
              }
            }
          }
          else
          {
            synchronized (statLock)
            {
              if (messagesTotal + setStatusTotal >= REQPARAM_STOP_SEND_AT_EVENT_COUNT)
              {
                stopSendEvents = true;
                break;
              }
              ++messagesSent;
            }
            
            plugin.startSendMessage(
              makeLoginId(RandomUtil.random(0, REQPARAM_USER_COUNT-1)), 
              makeLoginId(RandomUtil.random(0, REQPARAM_USER_COUNT-1)), 
              "msg "+RandomUtil.random(0, REQPARAM_USER_COUNT-1));
            
            synchronized (statLock)
            {
              ++setStatusSent;
            }
            
            String id = makeLoginId(RandomUtil.random(0, REQPARAM_USER_COUNT-1));
            plugin.setClientStatus(
              id, 
              plugin.getClientStatus(id) == MessagingNetwork.STATUS_ONLINE ? 
                MessagingNetwork.STATUS_BUSY : 
                MessagingNetwork.STATUS_ONLINE);
          }
        }
      }
    }
    catch (Throwable tr)
    {
      CAT.error("Error while mainloop", tr);
      reportMainStats();
      System.exit(1);
    }
  }
  
  static void report(String paramName, int paramValue)
  {
    CAT.info(paramName+": "+paramValue);
  }

  static void reportMainStats()
  {
    synchronized (statLock)
    {
      CAT.info("");
      long fm = Runtime.getRuntime().freeMemory();
      int usersHandled;
      if (loggingIn)
      {
        if (REQPARAM_TEST_SYNC_LOGINS)
          CAT.info("Users logged in: "+usersLoggedIn);
        else
          CAT.info("startLoginCalls: "+startLoginCalls+", loginSuccess: "+usersLoggedIn+", loginFailures: "+loginFailures);
      }
      if (REQPARAM_TEST_SYNC_LOGINS)
        usersHandled = usersLoggedIn;
      else
        usersHandled = usersLoggedIn + loginFailures;
      long fm2 = Runtime.getRuntime().totalMemory();
      long um = fm2 - fm;
      long usermem = um-usedMemoryOnStartup;
      if (usersHandled >= 500)
      {
        CAT.info("Memory needed for 25000 users, approx.: "+(usermem*25000L/usersHandled/1024/1024)+" MB");
        long mpu = usermem/usersHandled;
        CAT.info("Memory per user: "+mpu+" bytes ("+(mpu/1024)+" KB)");
      }
      //CAT.info("Runtime.freeMemory(): "+fm+" ("+(fm/1024/1024)+"MB)");
      //CAT.info("Runtime.totalMemory(): "+fm2+" ("+(fm2/1024/1024)+"MB)");
      CAT.info("Memory used: "+usermem+" ("+(usermem/1024/1024)+" MB)");
      if (loggingIn)
      {
        /*
        if (REQPARAM_TEST_SYNC_LOGINS)
        {
          fm = fm/usersLoggedIn;
          //CAT.info("Runtime.freeMemory() per usersLoggedIn: "+fm+" ("+(fm/1024)+"KB)");
        }
        else
        {
          fm = fm/startLoginCalls;
          //CAT.info("Runtime.freeMemory() per startLoginCalls: "+fm+" ("+(fm/1024)+"KB)");
        }
        */
      }
      else
      {
        long ev = messagesTotal + setStatusTotal;
        if (ev > 0)
        {
          CAT.info("Events total: "+ev);
          long millis = System.currentTimeMillis() - eventSendStartTimeMillis;
          if (millis < 1000) millis = 1000;
          CAT.info("Events per sec per 1 core: "+(((double)ev)/(millis) * 1000));
          long out = (setStatusSent+messagesSent)*100/ev;
          CAT.info("Outgoing events: "+out+"%");
          CAT.info("Incoming events: "+(100-out)+"%");
        }
      }
    }
  }
  
  private static String makeLoginId(int userNumber)
  {
    return Integer.toString(userNumber+1000000);
  }
  
  private static final String[] EMPTY_STRING_ARRAY = new String[] {};
  private static int CONTACT_LIST_LENGTH = org.openmim.icq2k.fake.FakeConnection.REQPARAM_STATUSCHANGE_FACTOR;
  
  private static String[] makeContactList(int userNumber)
  {
    ArrayList cl = new ArrayList(CONTACT_LIST_LENGTH);
    int n = userNumber + 2000000;
    for (int i = 0; i < CONTACT_LIST_LENGTH; ++i)
    {
      cl.add(Integer.toString(n));
      n += 1000000;
    }
    return (String[]) cl.toArray(EMPTY_STRING_ARRAY);
  }
}