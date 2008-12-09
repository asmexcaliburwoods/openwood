package org.openmim.irc.regexp;

/**
 * Insert the type's description here.
 * Creation date: (16 окт 2000 12:41:27)
 * @author:
 */
class RegExpSliceQu implements RegExpSlice
{

/**
 * RegExpSliceStar constructor comment.
 */
public RegExpSliceQu()
{
}
/**
 * findRegionEnd method comment.
 */
public int findRegionEnd(String s, int regionStart)
{
  if (regionStart >= s.length())
	return -1;
  return regionStart + 1;
}
}
