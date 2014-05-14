package org.openmim.irc.regexp;

/**
 * Insert the type's description here.
 * Creation date: (16 окт 2000 12:30:39)
 * @author:
 */
interface RegExpSlice
{

/**
Tries to match a subregion of
string <code>s</code>
starting from position
<code>regionStart</code>.
<p>
- If this slice matches the
given string <code>s</code>,
returns position of the character
right after the mathed subregion.
<br>
- If this slice does not match <code>s</code>,
returns -1.
*/
int findRegionEnd(String s, int regionStart);
}
