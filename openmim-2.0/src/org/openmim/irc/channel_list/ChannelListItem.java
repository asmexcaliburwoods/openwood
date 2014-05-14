package org.openmim.irc.channel_list;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   ChannelListItem.java
import com.egplab.utils.Lang;

public class ChannelListItem
{
  private String channelName;
  private String channelNameLowercased;
  private int population;
  private String topic;
  private String topicStripped;

public ChannelListItem(String s, int i, String s1)
{
  Lang.ASSERT_NOT_NULL(s, "channelName");
  Lang.ASSERT_NOT_NULL(s1, "topic");
  Lang.ASSERT_POSITIVE(i, "population");
  channelName = s;
  population = i;
  topic = s1;
}
public String getChannelName()
{
  return channelName;
}
public String getChannelNameLowercased()
{
  synchronized (channelName)
  {
	if (channelNameLowercased == null)
	  channelNameLowercased = channelName.toLowerCase();
  }
  return channelNameLowercased;
}
public int getPopulation()
{
  return population;
}
public String getTopic()
{
  return topic;
}
public String getTopicStripped()
{
  synchronized (topic)
  {
	Lang.NOT_IMPLEMENTED();
  }
  return null;
}
}
