package org.openmim.irc.driver;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   IRCUI.java
import java.util.StringTokenizer;

import com.egplab.utils.Lang;
import com.egplab.utils.LocaleUtil;
public class IRCUI
{
  private static java.util.ResourceBundle resourceBundle = java.util.ResourceBundle.getBundle("org.openmim.irc.driver.locale");

public IRCUI()
{
}
public static String encodeCommand(String s)
{
  if (s == null || !s.startsWith("/"))
	return null;
  StringTokenizer stringtokenizer = new StringTokenizer(s, " ", true);
  StringBuffer stringbuffer = new StringBuffer();
  String s1 = null;
  String s2 = "";
  if (stringtokenizer.hasMoreTokens())
  {
	s2 = stringtokenizer.nextToken().substring(1);
	if ("msg".equalsIgnoreCase(s2))
	  s2 = "PRIVMSG";
	stringbuffer.append(s2);
	s2 = s2.toLowerCase();
	if (stringbuffer.length() == 0 || s2.equals("me"))
	  return null;
  }
  for (; stringtokenizer.hasMoreTokens() && (s1 = stringtokenizer.nextToken()).equals(" "); s1 = null)
	stringbuffer.append(s1);
  if (s1 != null)
  {
	stringbuffer.append(s1);
	s1 = null;
  }
  for (; stringtokenizer.hasMoreTokens() && (s1 = stringtokenizer.nextToken()).equals(" "); s1 = null);
  if (s1 != null)
  {
	stringbuffer.append(" ");
	if (s2 == null)
	  s2 = "";
	if (!s2.equals("ns") && !s2.equals("nickserv") && !s2.equals("list") && !s2.equals("mode"))
	  stringbuffer.append(":");
	stringbuffer.append(s1);
  }
  for (; stringtokenizer.hasMoreTokens(); stringbuffer.append(stringtokenizer.nextToken()));
  return stringbuffer.toString();
}
public static String formatIdleTime(int i)
{
  StringBuffer stringbuffer = new StringBuffer();
  if (i == -1)
  {
	stringbuffer.append(getResourceString("unknown"));
  }
  else
  {
	if (i > 0x15180)
	{
	  stringbuffer.append(i / 0x15180).append(" ").append(getResourceString("days"));
	  i = i % 0x15180;
	}
	if (i > 3600)
	{
	  if (stringbuffer.length() > 0)
		stringbuffer.append(" ");
	  stringbuffer.append(i / 3600).append(" ").append(getResourceString("hours"));
	  i %= 3600;
	}
	if (i > 60)
	{
	  if (stringbuffer.length() > 0)
		stringbuffer.append(" ");
	  stringbuffer.append(i / 60).append(" ").append(getResourceString("minutes"));
	  i %= 60;
	}
	if (i > 0)
	{
	  if (stringbuffer.length() > 0)
		stringbuffer.append(" ");
	  stringbuffer.append(i).append(" ").append(getResourceString("seconds"));
	}
	if (stringbuffer.length() == 0)
	  stringbuffer.append("0");
  }
  return stringbuffer.toString();
}
static String getResourceString(String key)
{
  Lang.ASSERT_NOT_NULL(key, "key");
  return resourceBundle.getString(LocaleUtil.prepareKey(key));
}
}
