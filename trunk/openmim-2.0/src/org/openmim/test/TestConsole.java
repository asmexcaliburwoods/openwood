package org.openmim.test;

import java.util.*;
import java.io.*;

import org.openmim.icq.util.joe.*;
import org.openmim.*;
import org.openmim.mn.MessagingNetwork;
import org.openmim.mn.MessagingNetworkException;

/**
The console application that can be used to test
functionality of the ICQ2KMessagingNetwork and
ICQMessagingNetwork plugins.
<p>
@see org.openmim.icq2k.ICQ2KMessagingNetwork
*/
public class TestConsole
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(TestConsole.class.getName());
  public final static String PROPERTIES_RESOURCE_NAME = "/ru/openmim/mim/server/messaging/test/TestApplet.properties";
  private MessagingNetwork plugin;
  static Properties config;
  static String configId;
  String loginId;
  String password;

  {
    try
    {
      final String resName = PROPERTIES_RESOURCE_NAME;
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("Loading resource " + StringUtil.quote(resName) + "...");
      InputStream fis = new BufferedInputStream(getClass().getResourceAsStream(resName), 32 * 1024);
      try
      {
        config = new Properties();
        config.load(fis);
      }
      finally
      {
        try
        {
          fis.close();
        }
        catch (Exception ex)
        {
        }
      }
      configId = PropertyUtil.getRequiredProperty(config, resName + " resource", "config.id").trim();
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("Using config.id=="+StringUtil.quote(configId));
      String className = PropertyUtil.getRequiredProperty(config, resName + " resource", configId+".plugin-class-name").trim();
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("Instantiating class \"" + className + "\"...");
      plugin = (MessagingNetwork) Class.forName(className).newInstance();
      plugin.init();
      loginId = PropertyUtil.getRequiredProperty(config, configId+".login-id property", configId+".login-id");
      password = PropertyUtil.getRequiredProperty(config, configId+".password property", configId+".password");
    }
    catch (Throwable tr)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("", tr);
      System.exit(1);
    }
  }
  static int getInt(String key)
  {
    String s = getString(key);
    try
    {
      return Integer.parseInt(s);
    }
    catch (NumberFormatException ex)
    {
      throw new AssertException(key+" property must be int, but it is "+StringUtil.quote(s));
    }
  }

  static String getString(String key)
  {
    key = configId + '.' + key;
    String s = config.getProperty(key);
    if (s == null)  throw new AssertException(key+" property is not specified");
    s = s.trim();
    return s;
  }
  String contactList = "contact-list-not-yet-loaded";
  {
    String s = getString("contact-list");
    StringTokenizer st = new StringTokenizer(s, ",\t ");
    StringBuffer sb = new StringBuffer(s.length());
    while (st.hasMoreTokens())
    {
      sb.append(st.nextToken()).append("\n");
    }
    contactList = sb.toString();
  }
  String[] getContactList()
  {
    java.util.List cl = new java.util.LinkedList();
    StringTokenizer st = new StringTokenizer(contactList);
    StringBuffer dbg = new StringBuffer("TestConsole contact list: ");
    while (st.hasMoreTokens())
    {
      String loginId = st.nextToken().trim();
      if (loginId.length() == 0)
        continue;
      dbg.append("'" + loginId + "' ");
      cl.add(loginId);
    }
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info(dbg.toString());
    return (String[]) cl.toArray(new String[cl.size()]);
  }
  String getMyLoginId()
  {
    return loginId;
  }
void login()
{
  try
  {
    plugin.login(getMyLoginId(), password, getContactList(), MessagingNetwork.STATUS_ONLINE);
  }
  catch (Throwable tr)
  {
  if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("exception", tr);
  }
}
  void logout()
  {
      try
    {
      plugin.logout(getMyLoginId(), MessagingNetworkException.ENDUSER_NO_ERROR);
        }
      catch (Throwable tr)
      {
    if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("exception", tr);
        }
    }
public static void main(java.lang.String[] args)
{
  try
  {
    System.err.println("logging is done using log4j.");
    new TestConsole().run();
  }
  catch (Throwable tr)
  {
  if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("exception", tr);
    System.exit(1);
  }
}
  public void run()
  {
    try
    {
      login();
    }
    catch (Throwable tr)
    {
    if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("exception", tr);
    }
  }
}