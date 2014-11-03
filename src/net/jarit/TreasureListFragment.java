
package net.jarit;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.java_websocket.util.Base64;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.johnpersano.supertoasts.SuperToast;
import com.github.johnpersano.supertoasts.util.Style;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.SnapshotReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.MapView;
import com.google.common.collect.Lists;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import android.app.ListFragment;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class TreasureListFragment extends Fragment {
    private static final String TAG = TreasureListFragment.class.getSimpleName();

    private View view;
    private ListView listView;
    private GoogleMap mMap;
    private MapView mMapView;
    private ListAdapter mAdapter;

    private MapSnapshotManager msm = new MapSnapshotManager();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.treasure_list_fragment, container, false);
            listView = (ListView) view.findViewById(R.id.treasure_list);

            Ion.with(this)
                    .load(Constant.JARIT_URL.JAR_LIST + "0000")
                    .asJsonArray()
                    .setCallback(new FutureCallback<JsonArray>() {
                        @Override
                        public void onCompleted(Exception e, JsonArray result) {
                            if (e != null) {
                                SuperToast toast = SuperToast.create(
                                        TreasureListFragment.this.getActivity(),
                                        "Error Loading List.", SuperToast.Duration.MEDIUM,
                                        Style.getStyle(Style.ORANGE, SuperToast.Animations.FLYIN));
                                toast.setIcon(SuperToast.Icon.Light.INFO,
                                        SuperToast.IconPosition.LEFT);
                                toast.show();
                                return;
                            }
                            loadList(result);
                        }
                    });
        }
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            parent.removeView(view);
        }
        return view;
    }

    protected void loadList(JsonArray result) {
        List<TreasureItem> list = Lists.newArrayList();
        ObjectMapper mapper = new ObjectMapper();
        for (JsonElement elem : result) {
            JsonObject obj = elem.getAsJsonObject();
            Log.d(TAG, obj.toString());
            TreasureItem ti = null;
            try {
                ti = mapper.readValue(obj.toString(), TreasureItem.class);
                list.add(ti);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mAdapter = new TreasureItemListAdapter(getActivity(), list);
        listView.setAdapter(mAdapter);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    // @Override
    // public void onListItemClick(ListView l, View v, int position, long id) {
    // super.onListItemClick(l, v, position, id);
    // Log.d(TAG, "itemClicked");
    // }

    public static interface SnapshotDoneListener {
        public void onDone(String filePath);
    }

    public class MapSnapshotManager {
        private Marker currentPositionMarker;

        public void captureMapScreen(final Location loc, final int zoomLevel,
                final SnapshotDoneListener doneCallback) {
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                    new LatLng(loc.getLatitude(), loc.getLongitude()), zoomLevel);
            mMap.animateCamera(cameraUpdate);
            if (currentPositionMarker == null) {
                currentPositionMarker = mMap.addMarker(new
                        MarkerOptions().position(
                                new LatLng(loc.getLatitude(), loc.getLongitude())).icon(
                                BitmapDescriptorFactory.fromResource(R.drawable.marker)));
            } else {
                currentPositionMarker
                        .setPosition(new LatLng(loc.getLatitude(), loc.getLongitude()));
            }

            SnapshotReadyCallback callback = new SnapshotReadyCallback() {
                @Override
                public void onSnapshotReady(Bitmap snapshot) {
                    try {
                        String baseStr = loc.getLatitude() + "," + loc.getLongitude() + ","
                                + zoomLevel;
                        String base64Result = Base64.encodeBytes(baseStr.getBytes());
                        String fileName = Environment.getExternalStorageDirectory()
                                + "/" + base64Result + ".png";
                        FileOutputStream out = new FileOutputStream(fileName);
                        snapshot.compress(Bitmap.CompressFormat.PNG, 50, out);
                        doneCallback.onDone(fileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            mMap.snapshot(callback);
        }
    }
}
