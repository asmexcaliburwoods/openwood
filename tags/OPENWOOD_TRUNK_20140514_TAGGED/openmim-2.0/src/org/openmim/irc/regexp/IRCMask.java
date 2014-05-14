package org.openmim.irc.regexp;

import squirrel_util.Lang;

/**
 * Insert the type's description here.
 * Creation date: (16 окт 2000 16:04:21)
 * @author:
 */
public class IRCMask
{
  private RegExp regexp;

private IRCMask(RegExp regExp)
{
  Lang.ASSERT_NOT_NULL(regExp, "regExp");
  this.regexp = regExp;
}
/**
First, the mask is transformed using the following rule:

the mask is trim()med.

if the mask does not include '!'
{
if the mask includes '@',
{
it is prepended with "*!"
}
else if the mask includes '.',
{
it is prepended with "*!*@"
}
else
{
it is appended with "!*@*"
}
}
else //'!' in the mask
if the mask does not include '@',
{
"*@" is inserted after '!'
}


Then, the mask is scanned for the characters '*' and '?'.

These special pattern characters have the following meanings:

  *    Matches any string, including the empty string.
  ?    Matches any single character, with the following exception: in the hostname/ip address field, if the field consists of characters `*1-90.?' only, the sequence of  '???' (three question marks) matches any number 0...255.
 */
public IRCMask(String regExpString)
{
  Lang.ASSERT_NOT_NULL_NOR_TRIMMED_EMPTY(regExpString, "regExpString");
  String regExp_s = regExpString.trim();
  regexp = new RegExp(transform(regExp_s));
}
public Object clone()
{
  return new IRCMask((RegExp) regexp.clone());
}
public boolean equals(Object o)
{
  return o != null && o instanceof IRCMask && toString().equalsIgnoreCase(o.toString());
}
public int hashCode()
{
  return toString().toLowerCase().hashCode();
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
  boolean b = regexp.matches(s);
  return b;
}
public String toString()
{
  return regexp.toString();
}
/**
The mask is transformed using the following rule:

if the mask does not include '!'
{
if the mask includes '@',
{
it is prepended with "*!"
}
else if the mask includes '.',
{
it is prepended with "*!*@"
}
else
{
it is appended with "!*@*"
}
}
else //'!' in the mask
if the mask does not include '@',
{
"*@" is inserted after '!'
}
*/
private String transform(String mask)
{
  StringBuffer transformed = new StringBuffer(mask);
  int ex_pos = mask.indexOf('!');
  if (ex_pos == -1)
  {
	if (mask.indexOf('@') > -1)
	{
	  transformed.insert(0, "*!");
	}
	else
	  if (mask.indexOf('.') > -1)
	  {
		transformed.insert(0, "*!*@");
	  }
	  else
	  {
		transformed.append("!*@*");
	  }
  }
  else
  {
	//'!' in the mask
	if (mask.indexOf('@') == -1)
	{
	  transformed.insert(ex_pos + 1, "*@");
	}
  }
  //Lang.ASSERT(mask.indexOf('@') > 0, "'@' must exist after transform");
  //Lang.ASSERT(mask.indexOf('!') > 0, "'!' must exist after transform");
  return transformed.toString();
}
}
