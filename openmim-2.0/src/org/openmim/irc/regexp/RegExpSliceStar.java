package org.openmim.irc.regexp;

import com.egplab.utils.Lang;

/**
 * Insert the type's description here.
 * Creation date: (16 ��� 2000 12:41:27)
 * @author:
 */
class RegExpSliceStar implements RegExpSlice
{
  private String afterString;

/**
 * RegExpSliceStar constructor comment.
 */
public RegExpSliceStar(String stringWOstarsAndQuotationsAfterStar)
{
  Lang.ASSERT_NOT_NULL(stringWOstarsAndQuotationsAfterStar, "afterString");
  this.afterString = stringWOstarsAndQuotationsAfterStar;
}
/**
 * findRegionEnd method comment.
 */
public int findRegionEnd(String s, int regionStart)
{
  int afterStringPos;
  if (afterString.length() == 0 && regionStart >= 0 && regionStart <= s.length())
	return regionStart;
  else
	afterStringPos = s.indexOf(afterString, regionStart);
  if (afterStringPos == -1)
	return -1;
  return afterStringPos + afterString.length();
}
}
