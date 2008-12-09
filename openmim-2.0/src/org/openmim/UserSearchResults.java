package org.openmim;

import java.util.List;

public interface UserSearchResults
{
  public interface SearchResult
  {
		/** Always returns non-null. */
		public String getLoginId();

		/** Returns null, if no nick specified. */
		public String getNick();

		/** Returns null, if no realname specified. */
		public String getRealName();

		/** Returns null, if no email specified. */
		public String getEmail();
  }
  
  /**
  	Returns a non-empty non-null List of
  	UserSearchResults.SearchResult objects found.
  */
  public List getSearchResults();

  /**
    Returns true, if there are other matches.
    It means that the search results are truncated.
    <p>
    Returns false, if there are no more matches.
  */
  public boolean areTruncated();
}