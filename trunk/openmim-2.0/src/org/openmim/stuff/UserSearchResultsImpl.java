package org.openmim.stuff;

import java.util.*;

import org.openmim.*;
import org.openmim.icq.utils.*;

public class UserSearchResultsImpl
implements UserSearchResults
{
  private final List searchResults = new ArrayList(40);
  private boolean truncated = false;

  public UserSearchResultsImpl()
  {
  }

  public void addSearchResult(String loginId,
      String nick,
      String realName,
      String email)
  {
    searchResults.add(
      new SearchResultImpl(loginId,
      nick,
      realName,
      email));
  }

  /**
    Returns a non-empty non-null List of
    UserSearchResults.SearchResult objects found.
  */
  public List getSearchResults()
  {
    if ((searchResults) == null) Lang.ASSERT_NOT_NULL(searchResults, "searchResults");
    if ((searchResults.size()) <= 0) Lang.ASSERT_POSITIVE(searchResults.size(), "searchResults.size()");
    return searchResults;
  }

  /**
    Returns true, if there are other matches.
    It means that the search results are truncated.
    <p>
    Returns false, if there are no more matches.
  */
  public boolean areTruncated()
  {
    return truncated;
  }

  public void setTruncated(boolean t)
  {
    truncated = t;
  }

  public class SearchResultImpl implements UserSearchResults.SearchResult
  {
    private final String loginId;
    private final String email;
    private final String nick;
    private final String realName;

    public SearchResultImpl(
      String loginId,
      String nick,
      String realName,
      String email)
    {
      if (StringUtil.isNullOrEmpty(loginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(loginId, "loginId");
      this.loginId = loginId;
      this.nick = nick;
      this.realName = realName;
      this.email = email;
    }

    /** Always returns non-null. */
    public String getLoginId()
    {
      if (StringUtil.isNullOrEmpty(loginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(loginId, "loginId");
      return loginId;
    }

    /** Can return null (if no nick specified) */
    public String getNick()
    {
      return nick;
    }

    /** Can return null (if no realName specified) */
    public String getRealName()
    {
      return realName;
    }

    /** Can return null (if no email specified) */
    public String getEmail()
    {
      return email;
    }
  }
}