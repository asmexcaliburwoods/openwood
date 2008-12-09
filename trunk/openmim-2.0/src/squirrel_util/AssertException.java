package squirrel_util;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   AssertException.java

public class AssertException extends RuntimeException
{
  private static String MUG_REPORT_MSG = Util.getResourceString("Please send a bug report to the software development team.");

private AssertException()
{
}
public AssertException(String s)
{
  super("\n" + s + "\n" + MUG_REPORT_MSG);
}
}
