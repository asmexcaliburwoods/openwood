package org.openmim.mn2;

import org.openmim.mn2.model.StatusRoom;

public interface MessagingNetwork2Listener {
    void onCreateStatusRoom(StatusRoom statusRoom);
}
