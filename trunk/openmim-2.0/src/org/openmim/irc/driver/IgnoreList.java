package org.openmim.irc.driver;

import java.util.*;

import org.openmim.irc.regexp.IRCMask;
import org.openmim.messaging_network2.model.User;

import com.egplab.utils.Lang;

public class IgnoreList
{
  private Hashtable mask2mask = new Hashtable();

public IgnoreList()
{
}
public Enumeration elements()
{
  return mask2mask.keys();
}
public boolean isIgnored(User user)
{
  return isIgnored(user.toString());
}
public boolean isIgnored(String clientSpecification)
{
  Enumeration e = elements();
  while (e.hasMoreElements())
  {
	org.openmim.irc.regexp.IRCMask mask = (org.openmim.irc.regexp.IRCMask) e.nextElement();
	if (mask.matches(clientSpecification))
	  return true;
  }
  return false;
}
public void setIgnored(IRCMask mask, boolean ignored)
{
  Lang.ASSERT_NOT_NULL(mask, "mask");
  if (ignored)
	mask2mask.put(mask, mask);
  else
	mask2mask.remove(mask);
}
}
