package org.openmim.irc.driver;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   UserInfo.java
// Referenced classes of package org.openmim.irc:
//      LoadableImpl
public class UserInfo extends LoadableImpl
{
  private String nick;
  private String realName;
  private String clientUserName;
  private String clientHostName;
  private String channelsOn;
  private int idleTime;
  private String serverHost;
  private String serverInfo;
  private boolean ircOperator;

public UserInfo()
{
  realName = "";
  clientUserName = "";
  clientHostName = "";
  channelsOn = "";
  idleTime = -1;
  serverHost = "";
  serverInfo = "";
  ircOperator = false;
}
public String getChannelsOn()
{
  return channelsOn;
}
public String getClientHostName()
{
  return clientHostName;
}
public String getClientUserName()
{
  return clientUserName;
}
public int getIdleTime()
{
  return idleTime;
}
public String getNick()
{
  return nick;
}
public String getRealName()
{
  return realName;
}
public String getServerHost()
{
  return serverHost;
}
public String getServerInfo()
{
  return serverInfo;
}
public boolean isIrcOperator()
{
  return ircOperator;
}
public void setChannelsOn(String s)
{
  channelsOn = s;
}
public void setClientHostName(String s)
{
  clientHostName = s;
}
public void setClientUserName(String s)
{
  clientUserName = s;
}
public void setIdleTime(int i)
{
  idleTime = i;
}
public void setIrcOperator(boolean flag)
{
  ircOperator = flag;
}
public void setNick(String s)
{
  nick = s;
}
public void setRealName(String s)
{
  realName = s;
}
public void setServerHost(String s)
{
  serverHost = s;
}
public void setServerInfo(String s)
{
  serverInfo = s;
}
}
