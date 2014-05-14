package org.openmim.irc.driver;

import org.openmim.messaging_network2.model.User;

//
public class User_
{
  private String nick;
  private boolean voice;
  private boolean operator;
  private User user;

public User_(String s, boolean flag, boolean flag1)
{
  nick = s;
  operator = flag;
  voice = flag1;
}
/**
   *Is null until set by setClient(User_);
   *@see #setClient(org.openmim.messaging_network2.model.User)
   */
public User getUser()
{
  return user;
}
public String getNick()
{
  return nick;
}
public String getNickWithPrefix()
{
  return getPrefix() + getNick();
}
public String getPrefix()
{
  if (isOperator())
	return "@";
  if (isVoice())
	return "+";
  else
	return "";
}
public int getUIOrder()
{
  if (isOperator())
	return 0;
  return !isVoice() ? 2 : 1;
}
public boolean isOperator()
{
  return operator;
}
public boolean isVoice()
{
  return voice;
}
/**
   * Insert the method's description here. Creation date: (16 ��� 2000 18:51:04)
   * @param newUser org.openmim.irc.mvc_impl.model.User_
   */
public void setClient(User newUser)
{
  user = newUser;
}
public void setNick(String s)
{
  nick = s;
}
public void setOperator(boolean flag)
{
  operator = flag;
}
public void setVoice(boolean flag)
{
  voice = flag;
}
public String toString()
{
  return getNickWithPrefix();
}
}
