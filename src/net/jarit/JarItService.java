
package net.jarit;

import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.Map;

import net.jarit.Constant.JARIT_URL;
import net.jarit.Constant.WebSocketEvent;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class JarItService extends Service {
    private static final String TAG = JarItService.class.getSimpleName();

    private LocationManager locationManager;
    private JarItServiceBinder binder = new JarItServiceBinder();
    private SocketIO socket;
    private Location lastLocation;

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Message msg = new Message();
            msg.what = MainActivity.HANDLER_ON_LOCATION_CHANGED;
            msg.obj = location;
            lastLocation = location;
            checkTreasure(location);
            MainActivity.handler.sendMessage(msg);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    private IOCallback ioCallback = new IOCallback() {
        @Override
        public void onMessage(JSONObject json, IOAcknowledge ack) {
            try {
                Log.d(TAG, "Server said:" + json.toString(2));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onMessage(String data, IOAcknowledge ack) {
            Log.d(TAG, "Server said: " + data);
        }

        @Override
        public void onError(SocketIOException socketIOException) {
            Log.d(TAG, "an Error occured");
            socketIOException.printStackTrace();
        }

        @Override
        public void onDisconnect() {
            Log.d(TAG, "Connection terminated.");
        }

        @Override
        public void onConnect() {
            Log.d(TAG, "Connection established");
        }

        @Override
        public void on(String event, IOAcknowledge ack, Object... args) {
            Log.d(TAG, "Server triggered event '" + event + "'");
            try {
                JSONObject json = new JSONObject(args[0].toString());

            } catch (JSONException e) {
            }
        }
    };

    private void checkTreasure(Location location) {
        Log.d(TAG, "Check Treasure @" + location);
        if (location == null) {
            return;
        }
        openSocket();
        JSONObject json = new JSONObject();
        try {
            json.put(Constant.JSON_LON, location.getLongitude());
            json.put(Constant.JSON_LAT, location.getLatitude());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit(WebSocketEvent.CHECK_LOCATION, json);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Log.d(TAG, "Get intent, Action: " + action);
        locationManager = (LocationManager) getApplicationContext().getSystemService(
                Context.LOCATION_SERVICE);
        return START_REDELIVER_INTENT;
    }

    public void stopWatchingLocation() {
        locationManager.removeUpdates(locationListener);
        closeSocket();
        Log.d(TAG, "Watching Stop");
    }

    public synchronized void startWatchingLocation() {
        // openSocket();
        Criteria criteria = new Criteria();
        String bestProvider = locationManager.getBestProvider(criteria, false);
        //String bestProvider = LocationManager.GPS_PROVIDER;
        Log.d(TAG, bestProvider);
        locationManager.requestLocationUpdates(bestProvider, 0, 0, locationListener);
        lastLocation = locationManager.getLastKnownLocation(bestProvider);
        if (lastLocation == null) {
            lastLocation = new Location(bestProvider);
            lastLocation.setLatitude(40.7268477);
            lastLocation.setLongitude(-74.0319463);
        }
        if (lastLocation != null) {
            Log.d(TAG, "Checking LastLocation.");
            locationListener.onLocationChanged(lastLocation);
        }
        Log.d(TAG, "Watching Start");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private synchronized void openSocket() {
        Log.d(TAG, "Open WebSocket.");
        if (socket == null || !socket.isConnected()) {
            try {
                socket = new SocketIO("http://" + Constant.SERVER_WEBSOCKET_URL);
                Log.d(TAG, "connect to " + "http://" + Constant.SERVER_WEBSOCKET_URL);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            socket.connect(ioCallback);
        }
    }

    public void submitNewTreasure(String text, Location location, HttpResponseCallback callback) {
        if (location == null) {
            return;
        }
        JSONObject json = new JSONObject();
        try {
            json.put(Constant.JSON_LON, location.getLongitude());
            json.put(Constant.JSON_LAT, location.getLatitude());
            json.put(Constant.JSON_TEXT, text);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            new HttpPostTask().execute(JARIT_URL.NEW_JAR_POST + "0000", json, callback);
        } catch (Exception e) {
            Log.e(TAG, JARIT_URL.NEW_JAR_POST + " failed.");
            e.printStackTrace();
        }
    }

    public static interface HttpResponseCallback {
        public void onResponse(HttpResponse resp);
    }
    
    private class HttpPostTask extends AsyncTask<Object, Void, HttpResponse> {
        private Exception exception;
        private HttpResponseCallback callback;

        protected HttpResponse doInBackground(Object... objs) {
            try {
                if(objs.length == 3) {
                    callback = (HttpResponseCallback) objs[2];
                }
                DefaultHttpClient httpclient = new DefaultHttpClient();
                HttpPost httpost = new HttpPost((String) objs[0]);
                httpost.setEntity(new StringEntity(objs[1].toString(), HTTP.UTF_8));
                httpost.setHeader("Accept", "application/json");
                httpost.setHeader("Content-type", "application/json");
                ResponseHandler responseHandler = new BasicResponseHandler();
                return httpclient.execute(httpost, responseHandler);
            } catch (Exception e) {
                this.exception = e;
                return null;
            }
        }

        protected void onPostExecute(HttpResponse resp) {
            if(callback != null) {
                callback.onResponse(resp);
            }
        }
    }

    private synchronized void closeSocket() {
        if (socket.isConnected()) {
            socket.disconnect();
        }
    }

    public class JarItServiceBinder extends Binder {
        public void startWatchingLocation() {
            JarItService.this.startWatchingLocation();
        }

        public void stopWatchingLocation() {
            JarItService.this.stopWatchingLocation();
        }

        public void checkLastLocation() {
            JarItService.this.checkTreasure(lastLocation);
        }

        public void submitNewTreasure(String text, Location location, HttpResponseCallback callback) {
            JarItService.this.submitNewTreasure(text, lastLocation, callback);
        }
    }

}
