package org.openmim.irc.regexp;

import com.egplab.utils.Lang;

/**
 * Insert the type's description here.
 * Creation date: (16 ��� 2000 12:41:27)
 * @author:
 */
class RegExpSliceString implements RegExpSlice
{
  private String matchingString;

/**
 * RegExpSliceStar constructor comment.
 */
public RegExpSliceString(String matchingString)
{
  Lang.ASSERT_NOT_NULL(matchingString, "matchingString");
  this.matchingString = matchingString;
}
/**
 * findRegionEnd method comment.
 */
public int findRegionEnd(String s, int regionStart)
{
  if (!s.regionMatches(regionStart, matchingString, 0, matchingString.length()))
	return -1;
  else
	return regionStart + matchingString.length();
}
}
