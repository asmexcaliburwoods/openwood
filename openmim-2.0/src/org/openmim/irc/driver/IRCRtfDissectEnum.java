package org.openmim.irc.driver;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   IRCRtfDissectEnum.java
import java.awt.Color;
import java.util.Enumeration;

// Referenced classes of package org.openmim.irc:
//      IRCRtf, StyledPiece
class IRCRtfDissectEnum implements Enumeration
{
  private String s;
  private char tok;
  private int i;
  private Color fgColor;
  private Color bgColor;
  private boolean useNormal;
  private boolean useBold;
  private boolean useReverse;
  private boolean useUnderline;
  protected static final char EOF = 0;
  protected int len;

public IRCRtfDissectEnum(String s1)
{
  i = 0;
  resetStyle();
  s = s1;
  len = s.length();
  gettok();
}
protected void checkStartCodes()
{
  while (tok < ' ')
  {
	char c = tok;
	gettok();
	switch (c)
	{
	  case 2 :
		/* '\002' */
		useBold = true;
		break;
	  case 15 :
		/* '\017' */
		resetStyle();
		// fall through
	  case 3 :
		/* '\003' */
		if (Character.isDigit(tok))
		{
		  StringBuffer stringbuffer = new StringBuffer();
		  boolean flag = false;
		  boolean flag1 = false;
		  while (tok != 0)
		  {
			boolean flag2 = tok == ',';
			boolean flag3 = Character.isDigit(tok);
			boolean flag4 = tok == ',' || flag3;
			if (flag3 && !flag1)
			  flag1 = true;
			if (flag2 && !flag1)
			  break;
			if (flag2 && !flag)
			{
			  flag = true;
			  flag2 = false;
			}
			if (!flag4 || flag2)
			  break;
			stringbuffer.append(tok);
			gettok();
		  }
		  if (stringbuffer.length() > 0)
		  {
			String s1 = new String(stringbuffer);
			int j = s1.indexOf(",");
			if (j != -1)
			{
			  String s2 = s1.substring(0, j);
			  String s3 = s1.substring(j + 1);
			  try
			  {
				fgColor = IRCRtf.getColor(Integer.parseInt(s2) % 16);
			  }
			  catch (NumberFormatException _ex)
			  {
				fgColor = Color.black;
			  }
			  try
			  {
				bgColor = IRCRtf.getColor(Integer.parseInt(s3) % 16);
			  }
			  catch (NumberFormatException _ex)
			  {
				bgColor = Color.white;
			  }
			}
			else
			{
			  try
			  {
				fgColor = IRCRtf.getColor(Integer.parseInt(s1) % 16);
			  }
			  catch (NumberFormatException _ex)
			  {
				fgColor = Color.black;
			  }
			}
		  }
		}
		break;
	}
  }
}
protected StyledPiece getPiece()
{
  checkStartCodes();
  StringBuffer stringbuffer = new StringBuffer();
  while (tok >= ' ')
  {
	stringbuffer.append(tok);
	gettok();
  }
  StyledPiece styledpiece = new StyledPiece();
  styledpiece.bgColor = bgColor;
  styledpiece.fgColor = fgColor;
  styledpiece.isBold = useBold;
  styledpiece.isReverse = useReverse;
  styledpiece.isUnderlined = useUnderline;
  styledpiece.isNormal = useNormal;
  styledpiece.text = stringbuffer.toString();
  return styledpiece;
}
protected void gettok()
{
  if (i >= len)
	tok = '\u0000';
  else
	tok = s.charAt(i++);
}
public boolean hasMoreElements()
{
  return tok != 0;
}
public Object nextElement()
{
  return getPiece();
}
protected void resetStyle()
{
  fgColor = Color.black;
  bgColor = Color.white;
  useNormal = true;
  useBold = useReverse = useUnderline = false;
}
}
