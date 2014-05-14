package org.openmim.messaging_network2.model;

public class GlobalIRCParameters {
        private boolean debug=true;
        private boolean AutoRejoinChannelsOnKick=true;

        public boolean isDebug() {
            return debug;
        }

        public void setDebug(boolean debug) {
            this.debug = debug;
        }

        public boolean isAutoRejoinChannelsOnKick() {
            return AutoRejoinChannelsOnKick;
        }

        public void setAutoRejoinChannelsOnKick(boolean autoRejoinChannelsOnKick) {
            AutoRejoinChannelsOnKick = autoRejoinChannelsOnKick;
        }
    }
