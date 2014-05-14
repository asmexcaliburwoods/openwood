package org.openmim.irc.regexp;

import java.util.*;

import com.egplab.utils.Lang;

/**
 * Insert the type's description here.
 * Creation date: (16 ��� 2000 12:22:25)
 * @author:
 */
class RegExp
{
  private String asString;
  private Vector slices = new Vector();

/**
Regular expression <code>regExp</code> is scanned
for the characters '*' and '?'.

These special pattern characters have
the following meanings:

<pre>
  *    Matches any string, including
	  the empty string.
  ?    Matches any single character, with
  the following exception: in the hostname/ip
  address field, if the field consists of
  characters `*1-90.?' only, the sequence of
  '???' (three question marks) matches any
  number 0...255.
</pre>
*/
public RegExp(String regExp)
{
  Lang.ASSERT_NOT_NULL(regExp, "regExp");
  this.asString = regExp;
  parse();
}
private RegExp(String asString, Vector slices)
{
  Lang.ASSERT_NOT_NULL(asString, "asString");
  Lang.ASSERT_NOT_NULL(slices, "slices");
  this.asString = asString;
  this.slices = slices;
}
/**
Returns true if and only if the
<code>asString.substring(hostnameFieldStartPos)</code>
consists of characters `*1-90.?' only.
*/
private boolean checkHostnameField(int hostnameFieldStartPos)
{
  int pos = hostnameFieldStartPos;
  char c;
  String charsAllowed = "*1234567890.?";
  while (pos < asString.length())
  {
	c = asString.charAt(pos++);
	if (charsAllowed.indexOf(c) == -1)
	  return false;
  }
  return true;
}
public Object clone()
{
  return new RegExp(asString, (Vector) slices.clone());
}
private RegExpSlice createSlice(int sliceStart, int sliceAfterEnd)
{
  Lang.ASSERT(sliceStart <= sliceAfterEnd, "sliceStart must be <= sliceAfterEnd");
  Lang.ASSERT(sliceStart >= 0, "must be true: sliceStart >= 0");
  Lang.ASSERT(sliceAfterEnd <= asString.length(), "must be true: sliceAfterEnd <= asString.length()");
  if (sliceStart == sliceAfterEnd)
	return new RegExpSliceString("");
  char c = asString.charAt(sliceStart);
  switch (c)
  {
	case '*' :
	  Lang.ASSERT(sliceStart + 1 <= sliceAfterEnd, "sliceStart+1 must be <= sliceAfterEnd");
	  return new RegExpSliceStar(asString.substring(sliceStart + 1, sliceAfterEnd));
	case '?' :
	  if (sliceStart + 3 == sliceAfterEnd)
	  {
		Lang.ASSERT(asString.charAt(sliceStart + 1) == '?' && asString.charAt(sliceStart + 2) == '?', "must be:asString.charAt(sliceStart+1) == '?' && asString.charAt(sliceStart+2) == '?'");
		return new RegExpSlice3Qu();
	  }
	  else
	  {
		Lang.ASSERT(sliceStart + 1 == sliceAfterEnd, "must be: sliceStart + 1 == sliceAfterEnd");
		return new RegExpSliceQu();
	  }
	default :
	  return new RegExpSliceString(asString.substring(sliceStart, sliceAfterEnd));
  }
}
private int findNextSliceAfterEnd(int sliceStart, boolean threeQRegExpsAreSpecial)
{
  Lang.ASSERT(sliceStart < asString.length(), "must be true: sliceStart < asString.length()");
  int pos = sliceStart + 1;
  char c = asString.charAt(sliceStart);
  switch (c)
  {
	case '?' :
	  int count = 1;
	  if (threeQRegExpsAreSpecial)
	  {
		while (count < 3 && pos < asString.length())
		{
		  if (asString.charAt(pos) != '?')
			break;
		  pos++;
		  count++;
		}
	  }
	  if (count == 3)
		return pos;
	  else
		return sliceStart + 1;
	case '*' :
	default :
	  while (pos < asString.length())
	  {
		c = asString.charAt(pos);
		if (c == '?' || c == '*')
		  break;
		pos++;
	  }
	  return pos;
  }
}
/**
Returns true if and only if this regexp matches
the given string <code>s</code>.
<p>
Regular expression regexp is scanned for
the characters '*' and '?'.
<p>
These special pattern characters have
the following meanings:
<p>
<pre>
 *  Matches any string, including
the empty string.
 ?  Matches any single character, with
the following exception: in the hostname/ip
address field, if the field consists of
characters `*1-90.?' only, the sequence of
'???' (three question marks) matches any
number 0...255.
</pre>
*/
public boolean matches(String s)
{
  boolean b = matchesTail(s, 0, 0);
  return b;
}
/**
Returns true if and only if this regexp matches
the given string <code>s</code>.
<p>
Regular expression regexp is scanned for
the characters '*' and '?'.
<p>
These special pattern characters have
the following meanings:
<p>
<pre>
 *  Matches any string, including
the empty string.
 ?  Matches any single character, with
the following exception: in the hostname/ip
address field, if the field consists of
characters `*1-90.?' only, the sequence of
'???' (three question marks) matches any
number 0...255.
</pre>
*/
private boolean matchesTail(String s, int stringStartPos, int firstSliceNumber)
{
  Lang.ASSERT_NOT_NULL(s, "string to be matched");
  Lang.ASSERT(firstSliceNumber <= slices.size(), "must be true: firstSliceNumber <= slices.size()");
  Lang.ASSERT(stringStartPos <= s.length(), "must be true: stringStartPos <= s.length()");
  if (firstSliceNumber == slices.size())
  {
	//empty regexp tail encountered
	if (stringStartPos == s.length())
	{
	  //empty regexp tail always matches empty string tail
	  return true;
	}
	else
	{
	  return false;
	}
  }
  RegExpSlice rs = (RegExpSlice) slices.elementAt(firstSliceNumber);
  boolean ambiguous = rs instanceof RegExpSliceStar;
  if (!ambiguous)
  {
	int aepos = rs.findRegionEnd(s, stringStartPos);
	if (aepos == -1)
	{
	  //first slice from regexp tail
	  //does not match start of string tail
	  return false;
	}
	return matchesTail(s, aepos, firstSliceNumber + 1);
  }
  else
  {
	//try matching "*xxx" slice, enumerating all of its expansions
	int startPos = stringStartPos;
	int aepos;
	while (startPos <= s.length())
	{
	  aepos = rs.findRegionEnd(s, startPos);
	  if (aepos == -1)
		return false; //no more expansions for current slice exist
	  if (matchesTail(s, aepos, firstSliceNumber + 1))
		return true; //match found!
	  else
		startPos++; //try another expansion for current slice if it exists
	}
	Lang.ASSERT(startPos == s.length() + 1, "must be: startPos == s.length()");
	//current slice matches, but the rest of slices does not match
	//for any expansion of current slice.
	return false;
  }
}
/**
Regular expression regexp is scanned for
the characters '*' and '?'.
<p>
These special pattern characters have
the following meanings:
<p>
<pre>
 *  Matches any string, including
the empty string.
 ?  Matches any single character, with
the following exception: in the hostname/ip
address field, if the field consists of
characters `*1-90.?' only, the sequence of
'???' (three question marks) matches any
number 0...255.
</pre>
*/
private void parse()
{
  int spos = 0;
  int aepos;
  int hostnameFieldStartPos = asString.indexOf('@') + 1;
  boolean threeQRegExpsAreSpecial = false;
  if (hostnameFieldStartPos != 0)
	threeQRegExpsAreSpecial = checkHostnameField(hostnameFieldStartPos);
  while (spos < asString.length())
  {
	aepos = findNextSliceAfterEnd(spos, threeQRegExpsAreSpecial && spos > hostnameFieldStartPos);
	RegExpSlice rs = createSlice(spos, aepos);
	slices.addElement(rs);
	spos = aepos;
  }
  Lang.ASSERT(spos == asString.length(), "spos must be " + asString.length() + ", but it is " + spos);
}
public String toString()
{
  return asString;
}
}
