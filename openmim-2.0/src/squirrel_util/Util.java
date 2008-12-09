package squirrel_util;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   Util.java

public class Util
{
  private static java.util.ResourceBundle resourceBundle = java.util.ResourceBundle.getBundle("squirrel_util.locale");

public Util()
{
}
static String getResourceString(String key)
{
  Lang.ASSERT_NOT_NULL(key, "key");
  return resourceBundle.getString(LocaleUtil.prepareKey(key));
}
public static long parseLong(String s, long l)
{
  try
  {
	return Long.parseLong(s);
  }
  catch (Exception _ex)
  {
	return l;
  }
}
}
