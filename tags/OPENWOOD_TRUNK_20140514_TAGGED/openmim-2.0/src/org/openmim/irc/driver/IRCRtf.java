package org.openmim.irc.driver;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   IRCRtf.java
import java.awt.Color;
import java.util.Enumeration;
import java.util.StringTokenizer;
import squirrel_util.AssertException;
import squirrel_util.Lang;

// Referenced classes of package org.openmim.irc:
//      IRCRtfDissectEnum, IRCProtocol
public class IRCRtf
{
  public static final Color darkGreen = new Color(0, 143, 0);
  public static final Color maroon = new Color(127, 0, 0);
  public static final Color purple = new Color(159, 0, 159);
  public static final Color marine = new Color(0, 0, 127);
  public static final Color aqua = new Color(0, 143, 143);

public IRCRtf()
{
}
public static Enumeration dissect(String s)
{
  return new IRCRtfDissectEnum(s);
}
public static String encodeStyle(int bg, int fg, boolean isBold)
{
  StringBuffer stringbuffer = new StringBuffer();
  if (fg != 1 || bg != 0)
	stringbuffer.append('\003').append(pad2(fg)).append(",").append(pad2(bg));
  if (isBold)
	stringbuffer.append('\002');
  //IRCProtocol.dbg("CCC: " + stringbuffer);
  return stringbuffer.toString();
}
public static Color getColor(int i)
{
  switch (i & 0xf)
  {
	case 0 :
	  /* '\0' */
	  return Color.white;
	case 1 :
	  /* '\001' */
	  return Color.black;
	case 2 :
	  /* '\002' */
	  return marine;
	case 3 :
	  /* '\003' */
	  return darkGreen;
	case 4 :
	  /* '\004' */
	  return Color.red;
	case 5 :
	  /* '\005' */
	  return maroon;
	case 6 :
	  /* '\006' */
	  return purple;
	case 7 :
	  /* '\007' */
	  return Color.orange;
	case 8 :
	  /* '\b' */
	  return Color.yellow;
	case 9 :
	  /* '\t' */
	  return Color.green;
	case 10 :
	  /* '\n' */
	  return aqua;
	case 11 :
	  /* '\013' */
	  return Color.cyan;
	case 12 :
	  /* '\f' */
	  return Color.blue;
	case 13 :
	  /* '\r' */
	  return Color.magenta;
	case 14 :
	  /* '\016' */
	  return Color.gray;
	case 15 :
	  /* '\017' */
	  return Color.lightGray;
  }
  Lang.ASSERT_FALSE();
  return null;
}
public static String getColorName(int i)
{
  String key = "Unknown";
  switch (i)
  {
	case 0 :
	  key = "White";
	  break;
	case 1 :
	  key = "Black";
	  break;
	case 2 :
	  key = "Marine";
	  break;
	case 3 :
	  key = "Dark green";
	  break;
	case 4 :
	  key = "Red";
	  break;
	case 5 :
	  key = "Maroon";
	  break;
	case 6 :
	  key = "Purple";
	  break;
	case 7 :
	  key = "Orange";
	  break;
	case 8 :
	  key = "Yellow";
	  break;
	case 9 :
	  key = "Green";
	  break;
	case 10 :
	  key = "Aqua";
	  break;
	case 11 :
	  key = "Cyan";
	  break;
	case 12 :
	  key = "Blue";
	  break;
	case 13 :
	  key = "Magenta";
	  break;
	case 14 :
	  key = "Grey";
	  break;
	case 15 :
	  key = "Light grey";
	  break;
  }
  return IRCUI.getResourceString(key);
}
private static String pad2(int i)
{
  if (i < 10)
	return "0" + i;
  else
	return Integer.toString(i);
}
public static String removeSubstring(String s, int i, int j)
{
  String s1 = s;
  s1 = s1.substring(0, i) + s1.substring(j);
  return s1;
}
public static String removeSubstring(String s, String s1)
{
  String s2;
  int i;
  for (s2 = s;(i = s2.indexOf(s1)) != -1; s2 = s2.substring(0, i) + s2.substring(i + s1.length()));
  return s2;
}
private static String strip(String s, String s1)
{
  StringBuffer stringbuffer = new StringBuffer();
  StringTokenizer stringtokenizer = new StringTokenizer(s, s1, true);
  String s2;
  for (s2 = stringtokenizer.hasMoreTokens() ? stringtokenizer.nextToken() : null; s2 != null;)
  {
	Lang.ASSERT(s2.length() > 0, "(tok.length()) > 0 violated");
	char c = s2.charAt(0);
	if (s1.indexOf(c) == -1)
	{
	  stringbuffer.append(s2);
	  s2 = stringtokenizer.hasMoreTokens() ? stringtokenizer.nextToken() : null;
	}
	else
	{
	  switch (c)
	  {
		case 2 :
		  /* '\002' */
		case 4 :
		  /* '\004' */
		case 5 :
		  /* '\005' */
		case 15 :
		  /* '\017' */
		case 22 :
		  /* '\026' */
		case 31 :
		  /* '\037' */
		  s2 = stringtokenizer.hasMoreTokens() ? stringtokenizer.nextToken() : null;
		  break;
		case 3 :
		  /* '\003' */
		  s2 = stringtokenizer.hasMoreTokens() ? stringtokenizer.nextToken() : null;
		  if (s2 == null)
			break;
		  int i = 0;
		  char c1 = s2.charAt(i);
		  if (Character.isDigit(c1))
		  {
			char c2;
			if (++i < s2.length())
			  c2 = s2.charAt(i);
			else
			  c2 = 'E';
			if (Character.isDigit(c2))
			  if (++i < s2.length())
				c2 = s2.charAt(i);
			  else
				c2 = 'E';
			if (c2 == ',')
			{
			  char c3;
			  if (++i < s2.length())
				c3 = s2.charAt(i);
			  else
				c3 = 'E';
			  if (!Character.isDigit(c3))
			  {
				i--;
			  }
			  else
			  {
				char c4;
				if (++i < s2.length())
				  c4 = s2.charAt(i);
				else
				  c4 = 'E';
			  if (Character.isDigit(c4))
				i++;
			}
		  }
	  }
	  if (i >= s2.length())
	  {
		s2 = stringtokenizer.hasMoreTokens() ? stringtokenizer.nextToken() : null;
		break;
	  }
	  if (i > 0)
		s2 = s2.substring(i);
	  break;
	default :
	  throw new AssertException("This point should not be reached.");
	}
  }
}
Lang.ASSERT(s2 == null, "(tok == null) vioalted");
return stringbuffer.toString();
}
public static String stripColorCodes(String s)
{
  return strip(s, "\003\017\026\005");
}
public static String stripRichTextCodes(String s)
{
  return strip(s, "\003\017\002\037\026\004\005");
}
}
