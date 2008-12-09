package org.openmim;

import org.openmim.icq.util.joe.*;

public interface AsyncOperationRegistry
{
  /** Generates ids not found in this registry. */
  public Long addOperation(AsyncOperation op);
  /** Recycles ids. 
      Does not throw exceptions if the id is not present. */
  public void removeOperation(AsyncOperation op);
}
