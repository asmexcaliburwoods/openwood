package org.openmim.msn;

public interface Constants
{
  final String OUTCOMING_MESSAGE_HEADER =
      "MIME-Version: 1.0\r\n" +
      "Content-Type: text/plain; charset=UTF-8\r\n" +
      "X-MMS-IM-Format: FN=Tahoma; EF=; CO=0; PF=0\r\n\r\n";
      
  final String PROTOCOL_VERSIONS_STRING = "MSNP2";
  
  final int MSN_DEFAULT_PORT = 1863; //registered at IANA
}