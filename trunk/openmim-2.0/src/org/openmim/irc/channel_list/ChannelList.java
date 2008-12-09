package org.openmim.irc.channel_list;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   ChannelList.java
import java.util.Enumeration;
import java.util.Vector;

// Referenced classes of package org.openmim.irc.model:
//      ChannelListItem
public class ChannelList
{
  protected Vector items;
  private boolean beingReloaded;

public ChannelList()
{
  items = new Vector();
}
/**
   * @associates <{ChannelListItem}>
   * @supplierCardinality 0..*
   * @supplierRole list items
   */
public synchronized int add(ChannelListItem channellistitem)
{
  int i = items.size();
  items.addElement(channellistitem);
  return i;
}
public final synchronized int add(String s, int i, String s1)
{
  return add(makeChannelListItem(s, i, s1));
}
public synchronized ChannelListItem elementAt(int i)
{
  return (ChannelListItem) items.elementAt(i);
}
public synchronized Enumeration getListItems()
{
  return items.elements();
}
public synchronized int indexOf(ChannelListItem channellistitem)
{
  return items.indexOf(channellistitem);
}
public synchronized boolean isBeingReloaded()
{
  return beingReloaded;
}
public ChannelListItem makeChannelListItem(String s, int i, String s1)
{
  return new ChannelListItem(s, i, s1);
}
public synchronized void removeAll()
{
  items = new Vector();
}
public synchronized void setBeingReloaded(boolean flag)
{
  beingReloaded = flag;
}
public synchronized int size()
{
  return items.size();
}
}
