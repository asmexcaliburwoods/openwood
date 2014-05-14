package org.openmim.icq2k;

import java.util.*;

public interface LoginSched
{
  void init();
  void deinit();
  long allocateLoginStartTime();
}