package squirrel_util;

import java.util.*;
public class ReversedStringTokenizer implements Enumeration
{
  private int currentPosition;
  private int maxPosition;
  private String str;
  private String delimiters;
  private boolean retTokens;

public ReversedStringTokenizer(String s)
{
  this(s, " \t\n\r", false);
}
public ReversedStringTokenizer(String s, String s1)
{
  this(s, s1, false);
}
public ReversedStringTokenizer(String s, String delim, boolean returnTokens)
{
  Lang.ASSERT_NOT_NULL(s, "s");
  Lang.ASSERT_NOT_NULL(delim, "delim");
  currentPosition = s.length() - 1;
  str = s;
  maxPosition = 0;
  delimiters = delim;
  retTokens = returnTokens;
}
public int countTokens()
{
  int i = 0;
  for (int j = currentPosition; j > maxPosition;)
  {
	while (!retTokens && j > maxPosition && delimiters.indexOf(str.charAt(j)) >= 0)
	  j--;
	if (j <= maxPosition)
	  break;
	int k = j;
	for (; j > maxPosition && delimiters.indexOf(str.charAt(j)) < 0; j++);
	if (retTokens && k == j && delimiters.indexOf(str.charAt(j)) >= 0)
	  j--;
	i++;
  }
  return i;
}
public boolean hasMoreElements()
{
  return hasMoreTokens();
}
public boolean hasMoreTokens()
{
  skipDelimiters();
  return currentPosition > maxPosition;
}
public Object nextElement()
{
  return nextToken();
}
public String nextToken()
{
  skipDelimiters();
  if (currentPosition <= maxPosition)
	throw new NoSuchElementException();
  int i = currentPosition;
  for (; currentPosition >= maxPosition && delimiters.indexOf(str.charAt(currentPosition)) < 0; currentPosition--);
  if (retTokens && currentPosition >= maxPosition && i == currentPosition && delimiters.indexOf(str.charAt(currentPosition)) >= 0)
	currentPosition--;
  return str.substring(currentPosition + 1, i + 1);
}
public String nextToken(String s)
{
  delimiters = s;
  return nextToken();
}
private void skipDelimiters()
{
  for (; !retTokens && currentPosition > maxPosition && delimiters.indexOf(str.charAt(currentPosition)) >= 0; currentPosition--);
}
}
