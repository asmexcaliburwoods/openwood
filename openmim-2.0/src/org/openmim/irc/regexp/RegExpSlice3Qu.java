package org.openmim.irc.regexp;

/**
 * Insert the type's description here.
 * Creation date: (16 окт 2000 12:41:27)
 * @author:
 */
class RegExpSlice3Qu implements RegExpSlice
{

/**
 * RegExpSliceStar constructor comment.
 */
public RegExpSlice3Qu()
{
}
/**
 * findRegionEnd method comment.
 */
public int findRegionEnd(String s, int regionStart)
{
  int byt = 0;
  int dig;
  int pos = regionStart;
  while (pos <= s.length())
  {
	dig = s.charAt(pos) - '0';
	if (dig < 0 || dig > 9)
	  break;
	byt = 10 * byt + dig;
	if (byt > 255)
	  break;
	pos++;
  }
  if (pos == regionStart)
	return -1;
  else
	return pos;
}
}
