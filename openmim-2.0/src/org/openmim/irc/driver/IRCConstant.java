package org.openmim.irc.driver;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   IRCConstant.java
public class IRCConstant
{
  public static final int ERR_NOSUCHNICK = 401;
  public static final int ERR_NOSUCHCHANNEL = 403;
  public static final int ERR_CANNOTSENDTOCHAN = 404;
  public static final int ERR_TOOMANYCHANNELS = 405;
  public static final int ERR_WASNOSUCHNICK = 406;
  public static final int ERR_ERRONEUSNICKNAME = 432;
  public static final int ERR_NICKNAMEINUSE = 433;
  public static final int ERR_NICKCOLLISION = 436;
  public static final int ERR_USERNOTINCHANNEL = 441;
  public static final int ERR_NOTONCHANNEL = 442;
  public static final int ERR_USERONCHANNEL = 443;
  public static final int ERR_PASSWDMISMATCH = 464;
  public static final int ERR_YOUREBANNEDCREEP = 465;
  public static final int ERR_CHANNELISFULL = 471;
  public static final int ERR_INVITEONLYCHAN = 473;
  public static final int ERR_BANNEDFROMCHAN = 474;
  public static final int ERR_BADCHANNELKEY = 475;
  public static final int ERR_CHANOPRIVSNEEDED = 482;
  public static final int RPL_WHOISUSER = 311;
  public static final int RPL_WHOISSERVER = 312;
  public static final int RPL_WHOISOPERATOR = 313;
  public static final int RPL_WHOISIDLE = 317;
  public static final int RPL_ENDOFWHOIS = 318;
  public static final int RPL_WHOISCHANNELS = 319;
  public static final int RPL_LISTSTART = 321;
  public static final int RPL_LIST = 322;
  public static final int RPL_LISTEND = 323;
  public static final int RPL_CHANNELMODEIS = 324;
  public static final int RPL_NOTOPIC = 331;
  public static final int RPL_TOPIC = 332;
  public static final int RPL_NAMREPLY = 353;
  public static final int RPL_BANLIST = 367;
  public static final int RPL_ENDOFBANLIST = 368;
  public static final int ERR_EXT_COMMAND_UNDEFINED = -1;
  public static final int ERR_EXT_MALFORMED_MESSAGE_FROM_SERVER = -2;
  public static final int ERR_EXT_ERROR = -3;
  public static final int RPL_EXT_PING = -100;
  public static final int RPL_EXT_PRIVMSG = -101;
  public static final int RPL_EXT_NOTICE = -102;
  public static final int RPL_EXT_JOIN = -103;
  public static final int RPL_EXT_NICK = -104;
  public static final int RPL_EXT_QUIT = -105;
  public static final int RPL_EXT_TOPIC = -106;
  public static final int RPL_EXT_PART = -107;
  public static final int RPL_EXT_KICK = -108;
  public static final int RPL_EXT_MODE = -109;
  public static final int RPL_EXT_WELCOME = 1;
  /**
  ":127.0.0.1 303 jj_mirc_foo :"
  */
  public static final int RPL_EXT_ISON = 303;

public IRCConstant()
{
}
}
