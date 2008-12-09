package org.openmim.irc.driver;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   LoadableImpl.java
// Referenced classes of package org.openmim.irc:
//      Loadable
public class LoadableImpl implements Loadable
{
  private boolean beingLoaded;

public LoadableImpl()
{
  beingLoaded = false;
}
public boolean isBeingLoaded()
{
  return beingLoaded;
}
public void setBeingLoaded(boolean flag)
{
  beingLoaded = flag;
}
}
