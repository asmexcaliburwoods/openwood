package org.openmim.irc.channel_list;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   OrderedChannelList.java
import com.egplab.utils.Lang;
import com.egplab.utils.Order;
import com.egplab.utils.Ordered;

// Referenced classes of package org.openmim.irc.model:
//      ChannelList, ChannelListItem
public class OrderedChannelList extends ChannelList implements Ordered
{
  protected Order order;

public OrderedChannelList()
{
  order = new Order(this);
}
/**
   * @associates <{ChannelListItem}>
   * @supplierCardinality 0..*
   * @supplierRole list items
   */
public synchronized int add(ChannelListItem channellistitem)
{
  Lang.ASSERT_NOT_NULL(channellistitem, "channelListItem");
  return order.add(channellistitem);
}
public int compareTo(Object obj, Object obj1)
{
  ChannelListItem channellistitem = (ChannelListItem) obj;
  ChannelListItem channellistitem1 = (ChannelListItem) obj1;
  return channellistitem.getChannelNameLowercased().compareTo(channellistitem1.getChannelNameLowercased());
}
public synchronized Object getItem(int i) throws IndexOutOfBoundsException
{
  return items.elementAt(i);
}
public synchronized int getItemCount()
{
  return size();
}
public synchronized void insertAt(int i, Object obj) throws IndexOutOfBoundsException
{
  items.insertElementAt(obj, i);
}
}
