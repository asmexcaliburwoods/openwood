package org.openmim.icq.util.joe;

public class RandomStringUtil
{
  public static final char[] VOWELS = "euyioa".toCharArray();
  public static final char[] VOWELS_WITH_FREQ = "eeuyiioooaaaa".toCharArray();
  public static final char[] CONSONANTS = "qwrtpsdfghjklzxcvbnm".toCharArray();
  public static final char[] SIGNS = "!?...".toCharArray();
  
  public static char[] randomSyllable()
  {
    //bab
    //a
    //ba
    switch(RandomUtil.random((int) 1, (int) 3))
    {
      case 1:
        return new char[] {
          CONSONANTS[RandomUtil.random((int) 0, CONSONANTS.length-1)],
          VOWELS_WITH_FREQ[RandomUtil.random((int) 0, VOWELS_WITH_FREQ.length-1)],
          CONSONANTS[RandomUtil.random((int) 0, CONSONANTS.length-1)]};
      case 2:
        return new char[] {
          VOWELS_WITH_FREQ[RandomUtil.random((int) 0, VOWELS_WITH_FREQ.length-1)]};
      case 3:
        return new char[] {
          CONSONANTS[RandomUtil.random((int) 0, CONSONANTS.length-1)],
          VOWELS_WITH_FREQ[RandomUtil.random((int) 0, VOWELS_WITH_FREQ.length-1)]};
          
    }
    throw new AssertException("this point should never be reached");
  }
  
  public static char[][] randomWord()
  {
    int nsy = RandomUtil.random(1, 6);
    if (nsy >= 5) nsy = nsy-3; 
    char[][] w = new char[nsy][];
    while(nsy > 0)  w[--nsy] = randomSyllable();
    return w;   
  }
  
  public static String randomString(int minLength, int maxLength)
  {
    if (maxLength < minLength) throw new IllegalArgumentException("maxLength < minLength");
    boolean start = true;
    StringBuffer sb = new StringBuffer(maxLength);
    int len = RandomUtil.random(minLength, maxLength) - 1;
    while (sb.length() < len)
    {
      char[][] w = randomWord();
      int wlen = 0;
      for (int wi = 0; wi < w.length; ++wi) wlen += w[wi].length;
      if (sb.length() + wlen + 2 > maxLength) break;
      if (!start) sb.append(' ');
      else
      {
        w[0][0] = Character.toUpperCase(w[0][0]);
        start = false;
      }
      for (int wi = 0; wi < w.length; ++wi) sb.append(w[wi]);
    }
    char sign = SIGNS[RandomUtil.random(0, SIGNS.length-1)];
    sb.append(sign);
    if (sb.length() <= maxLength - 2 && RandomUtil.random((int)1, (int)4) == 1)
      {sb.append(sign);sb.append(sign);}
    while (sb.length() < minLength) sb.append(' ');
    return sb.toString();      
  }
  
  public static String randomString() { return randomString(7, 40); }
}