
package net.jarit;

import com.google.android.gms.maps.model.LatLng;

public class Constant {
    public static final String SERVER_BASE = "server-jarit.rhcloud.com";
    public static final String SERVER_WEBSOCKET_URL = SERVER_BASE + ":8000/socket";

    public static final String JSON_LON = "lon";
    public static final String JSON_LAT = "lat";
    public static final String JSON_TEXT = "content";

    public static final class WebSocketEvent {
        public static final String CHECK_LOCATION = "check_location";
        public static final String NEW_JAR = "new_jar";
    }

    public static final class JARIT_URL {
        private static final String HTTP = "http://";
        public static final String SERVER_BASE = "server-jarit.rhcloud.com";
        public static final String NEW_JAR_POST = HTTP + SERVER_BASE + "/treasure/";
        public static final String JAR_LIST = HTTP + SERVER_BASE + "/list/";
    }

    public static final String LOCATION = "location";

    public static final String getMapImage(LatLng location) {
        return "http://maps.googleapis.com/maps/api/staticmap?center=" + location.latitude + ","
                + location.longitude + "&zoom=16&size=480x240&markers=color:red%7C"
                + location.latitude + "," + location.longitude;
    }
}
