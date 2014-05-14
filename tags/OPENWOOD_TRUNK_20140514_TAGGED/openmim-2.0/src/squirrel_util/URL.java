package squirrel_util;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   URL.java

public class URL
{

public URL()
{
}
public static String getCanonicalURL(String s)
{
  String s1 = s.toLowerCase();
  if (s1.startsWith("www."))
	return "http://" + s;
  if (s1.startsWith("ftp."))
	return "ftp://" + s;
  else
	return s;
}
}
