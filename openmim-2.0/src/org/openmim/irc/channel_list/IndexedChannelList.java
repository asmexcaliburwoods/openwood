package org.openmim.irc.channel_list;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   IndexedChannelList.java
import java.util.Hashtable;

// Referenced classes of package org.openmim.irc.model:
//      OrderedChannelList, ChannelListItem
public class IndexedChannelList extends OrderedChannelList
{
  private Hashtable channelNameLow2channelItem;

public IndexedChannelList()
{
  channelNameLow2channelItem = new Hashtable();
}
/**
   * @associates <{ChannelListItem}>
   * @supplierCardinality 0..*
   * @supplierRole list items
   */
public int add(ChannelListItem channellistitem)
{
  int i = super.add(channellistitem);
  channelNameLow2channelItem.put(channellistitem.getChannelNameLowercased(), channellistitem);
  return i;
}
public ChannelListItem getItemByLowercasedName(String s)
{
  return (ChannelListItem) channelNameLow2channelItem.get(s);
}
public ChannelListItem getItemByName(String s)
{
  return getItemByLowercasedName(s.toLowerCase());
}
}
