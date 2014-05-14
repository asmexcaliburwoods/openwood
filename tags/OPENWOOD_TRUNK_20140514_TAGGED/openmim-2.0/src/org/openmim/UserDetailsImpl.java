package org.openmim;

import org.openmim.mn.MessagingNetworkException;

public class UserDetailsImpl implements UserDetails
{
  private final String nick;
  private final String realName;
  private final String email;
  private final String homeCity;
  private final String homeState;
  private final String homePhone;
  private final String homeFax;
  private final String homeStreet;
  private final String cellPhone;
  private final String homeZipcode;
  private final boolean cellPhoneSMSEnabled;
  private final boolean authorizationRequired;
  private final String workPhone;
  private final boolean authorizationSupported;
  

  public UserDetailsImpl(
    String nick,
    String realName,
    String email,
    String homeCity,
    String homeState,
    String homePhone,
    String homeFax,
    String homeStreet,
    String cellPhone,
    boolean isCellPhoneSMSEnabled,
    String homeZipcode,
    boolean authorizationRequired)
  {
    this(
      nick,realName,email,
      homeCity,homeState,homePhone,homeFax,homeStreet,
      cellPhone,isCellPhoneSMSEnabled,
      homeZipcode,
      null /*workPhone*/,
      authorizationRequired,
      true);
  }
  
  public UserDetailsImpl(
    String nick,
    String realName,
    String email,
    String homeCity,
    String homeState,
    String homePhone,
    String homeFax,
    String homeStreet,
    String cellPhone,
    boolean isCellPhoneSMSEnabled,
    String homeZipcode,
    String workPhone,
    boolean authorizationRequired,
    boolean authorizationSupported)
  {
    this.nick = nick;
    this.realName = realName;
    this.email = email;
    this.homeCity = homeCity;
    this.homeState = homeState;
    this.homePhone = homePhone;
    this.homeFax = homeFax;
    this.homeStreet = homeStreet;
    this.cellPhone = cellPhone;
    this.cellPhoneSMSEnabled = isCellPhoneSMSEnabled;
    this.homeZipcode = homeZipcode;
    this.authorizationRequired = authorizationRequired;
    this.workPhone = workPhone;
    this.authorizationSupported = authorizationSupported;
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

  public String getHomeCity()
  { return homeCity; }

  public String getHomeState()
  { return homeState; }

  public String getHomePhone()
  { return homePhone; }

  public String getWorkPhone()
  { return workPhone; }

  public String getHomeFax()
  { return homeFax; }

  public String getHomeStreet()
  { return homeStreet; }

  public String getCellPhone()
  { return cellPhone; }

  public boolean isCellPhoneSMSEnabled() throws MessagingNetworkException
  {
    if (cellPhone == null)
      throw new MessagingNetworkException(
        "Illegal state: cell phone must not be null here, but it is.",
        MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
        MessagingNetworkException.ENDUSER_MIM_BUG);
    return cellPhoneSMSEnabled;
  }

  public String getHomeZipcode()
  { return homeZipcode; }

  public boolean isAuthorizationRequired() throws MessagingNetworkException
  {
    if (authorizationSupported)
      return authorizationRequired;
    else
    {
      MessagingNetworkException.throwOperationNotSupported("isAuthorizationRequired");
      //the following is never executed
      return false;
    }
  }
}