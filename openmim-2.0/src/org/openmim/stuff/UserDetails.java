package org.openmim.stuff;

import org.openmim.messaging_network.MessagingNetworkException;

public interface UserDetails
{
  /** Returns null, if no nick specified. */
  String getNick();

  /** Returns null, if no realname specified. */
  String getRealName();

  /** Returns null, if no email specified. */
  public String getEmail();
  
  /** Returns null, if no cellular phone specified.
      <pre>
      ICQ PLUGIN-ONLY INFORMATION :
        Exact format: "+", contry-code, " (", area-code, ") ", phone-number.
        Example: "+1 (1234) 567890"
      ICQ PLUGIN-ONLY INFORMATION END.
      
      MSN PLUGIN-ONLY INFORMATION :
        Phones are (partially) freetext.
      MSN PLUGIN-ONLY INFORMATION END.
      </pre>
  */
  public String getCellPhone();

  /** This method throws a MessagingNetworkException,
      if getCellPhone() is null.
  */
  public boolean isCellPhoneSMSEnabled() throws MessagingNetworkException;

  /** Returns null, if no home city specified. */  
  public String getHomeCity();

  /** Returns null if not specified. */
  public String getHomeState();

  /** Returns null if not specified.
      <pre>
      ICQ PLUGIN-ONLY INFORMATION :
        Exact format: "+", contry-code, " (", area-code,
                      ") ", phone-number, (optional: "-", phone extension).
        Example: "+376 (123) 232323"
        Example: "+376 (123) 232323-123",
      ICQ PLUGIN-ONLY INFORMATION END.

      MSN PLUGIN-ONLY INFORMATION :
        Phones are (partially) freetext.
      MSN PLUGIN-ONLY INFORMATION END.
      </pre>
  */
  public String getHomePhone();

  /** Returns null if not specified.
      <pre>
      MSN PLUGIN-ONLY INFORMATION :
        Phones are (partially) freetext.
      MSN PLUGIN-ONLY INFORMATION END.
      </pre>
  */
  public String getWorkPhone();

  /** Returns null if not specified.
      <pre>
      ICQ PLUGIN-ONLY INFORMATION :
        Exact format: "+", contry-code, " (", area-code, ") ", phone-number.
        Example: "+376 (123) 232323"
      ICQ PLUGIN-ONLY INFORMATION END.

      MSN PLUGIN-ONLY INFORMATION :
        Phones are (partially) freetext.
      MSN PLUGIN-ONLY INFORMATION END.
      </pre>
  */
  public String getHomeFax();

  /** Returns null if not specified. */
  public String getHomeStreet();

  /** Returns null if not specified. */
  public String getHomeZipcode();
  
  public boolean isAuthorizationRequired() throws MessagingNetworkException;
}