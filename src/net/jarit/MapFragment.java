
package net.jarit;

import net.jarit.MainActivity.MainActivityListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.GestureDetector.OnGestureListener;
import android.view.View.OnTouchListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.viewpagerindicator.CirclePageIndicator;

public class MapFragment extends Fragment {
    private static final String TAG = MapFragment.class.getSimpleName();

    private View view;
    private GoogleMap mMap;
    private MapView mMapView;
    private TextView lonText;
    private TextView latText;
    private TextView mainLonText;
    private TextView mainLatText;
    private Marker currentPositionMarker;
    private MaskedImageView maskedView;
    private ImageView mainButton;
    private ImageView mainButton_bottom;
    private ToolsPagerAdapter pagerAdapter;
    private ViewPager pager;
    private CirclePageIndicator indicator;
    private RelativeLayout dock;
    private RelativeLayout mapContainer;
    private RelativeLayout logo;
    private RelativeLayout infoPanel;

    private float bottom_x;
    private float bottom_y;

    private boolean isWatching = false;

    private MainActivity mainActivity;

    private MainActivityListener listener = new MainActivityListener() {

        @Override
        public void onLocationChanged(Location loc) {
            if (lonText != null && latText != null) {
                lonText.setText(loc.getLongitude() + "");
                latText.setText(loc.getLatitude() + "");
            }
            if (mainLonText != null && mainLatText != null) {
                mainLonText.setText(loc.getLongitude() + "");
                mainLatText.setText(loc.getLatitude() + "");
            }
            if (mMap != null) {
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                        new LatLng(loc.getLatitude(), loc.getLongitude()), 18);
                mMap.animateCamera(cameraUpdate);
                if (currentPositionMarker == null) {
                    currentPositionMarker = mMap.addMarker(new MarkerOptions().position(
                            new LatLng(loc.getLatitude(), loc.getLongitude())).icon(
                            BitmapDescriptorFactory.fromResource(R.drawable.marker)));
                } else {
                    animateMarker(currentPositionMarker,
                            new LatLng(loc.getLatitude(), loc.getLongitude()), false);
                }
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (view == null) {
            mainActivity = MainActivity.getInstance();
            view = inflater.inflate(R.layout.fragment_main, container, false);
            mMapView = (MapView) view.findViewById(R.id.map_main);
            maskedView = (MaskedImageView) view.findViewById(R.id.main_maskedView);

            Display display = ((WindowManager) getActivity().getSystemService(
                    Context.WINDOW_SERVICE))
                    .getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            Point size = new Point();
            display.getSize(size);
            int width = size.x;

            maskedView.setMaskBitmap(Bitmap.createScaledBitmap(
                    BitmapFactory.decodeResource(getResources(),
                            R.drawable.radar_overlap_mask), (int) (width * 0.8),
                    (int) (width * 0.8), false));

            mainButton = (ImageView) view.findViewById(R.id.main_btn);
            mainButton_bottom = (ImageView) view.findViewById(R.id.main_btn_bottom);
            prepareMainButton();
            bottom_x = mainButton_bottom.getX();
            bottom_y = mainButton_bottom.getY();

            mMapView.onCreate(savedInstanceState);
            mMap = mMapView.getMap();
            MapsInitializer.initialize(this.getActivity());

            lonText = (TextView) view.findViewById(R.id.main_lon_label);
            latText = (TextView) view.findViewById(R.id.main_lat_label);
            mainLonText = (TextView) view.findViewById(R.id.main_lon_text);
            mainLatText = (TextView) view.findViewById(R.id.main_lat_text);

            pager = (ViewPager) view.findViewById(R.id.toolPager);
            pagerAdapter = new ToolsPagerAdapter(getFragmentManager());
            pager.setAdapter(pagerAdapter);

            indicator = (CirclePageIndicator) view.findViewById(R.id.toolsPagerIndicator);
            indicator.setViewPager(pager);

            dock = (RelativeLayout) view.findViewById(R.id.dock);

            mapContainer = (RelativeLayout) view.findViewById(R.id.mapContainer);

            logo = (RelativeLayout) view.findViewById(R.id.logo);

            infoPanel = (RelativeLayout) view.findViewById(R.id.info_container);
        }
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            parent.removeView(view);
        }
        return view;
    }

    public void animateMarker(final Marker marker, final LatLng toPosition,
            final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = mMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 250;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }

    private void prepareMainButton() {
        final GestureDetector gdt = new GestureDetector(getActivity(), new OnGestureListener() {
            private int MIN_DIST = 100;

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                Log.d(TAG, "onSingleTapUp");
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                    float distanceY) {
                Log.d(TAG, "onScroll");
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                Log.d(TAG, "onLongPress");
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                    float velocityY) {
                Log.d(TAG, "fling");
                double dist = Math.sqrt(Math.pow((e2.getX() - e1.getX()), 2)
                        + Math.pow((e2.getY() - e1.getY()), 2));
                Log.d(TAG, "dist: " + dist);
                if (dist > MIN_DIST) {
                    if (!isWatching) {
                        int offsetX = (int) (mainButton_bottom.getWidth() / dist
                                * ((e2.getX() - e1.getX())));
                        int offsetY = (int) (mainButton_bottom.getHeight() / dist
                                * ((e2.getY() - e1.getY())));

                        final Animation fly = new TranslateAnimation(mainButton_bottom.getX(),
                                mainButton_bottom.getX() + offsetX,
                                mainButton_bottom.getY(), mainButton_bottom.getY() + offsetY);
                        fly.setInterpolator(new AccelerateInterpolator());
                        fly.setDuration(200);
                        fly.setAnimationListener(new AnimationListener() {

                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                mainButton_bottom.setAlpha(0.0f);
                                mainActivity.binder.startWatchingLocation();
                                mainActivity.addListener(listener);
                                isWatching = true;
                                setDockVisibility(true);
                                setInfoPanelVisibility(true);
                            }
                        });

                        Animation pressAnimation = new AlphaAnimation(1, 0);
                        pressAnimation.setInterpolator(new AccelerateInterpolator());
                        pressAnimation.setDuration(200);
                        pressAnimation.setAnimationListener(new AnimationListener() {

                            @Override
                            public void onAnimationStart(Animation animation) {
                                mainButton.setAlpha(1.0f);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                mainButton.setAlpha(0.0f);
                                mainButton_bottom.startAnimation(fly);
                            }
                        });
                        mainButton.startAnimation(pressAnimation);
                    } else {
                        final Animation releaseAnimation = new AlphaAnimation(0.f, 1);
                        releaseAnimation.setInterpolator(new DecelerateInterpolator());
                        releaseAnimation.setDuration(200);
                        releaseAnimation.setAnimationListener(new AnimationListener() {

                            @Override
                            public void onAnimationStart(Animation animation) {
                                mainButton.setAlpha(1.0f);
                                mainButton.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                mainButton.setAlpha(1.0f);
                                mainActivity.binder.stopWatchingLocation();
                                mainActivity.removeListener(listener);
                                isWatching = false;
                                setDockVisibility(false);
                                setInfoPanelVisibility(false);
                            }
                        });

                        int offsetX = (int) (mainButton_bottom.getWidth() / dist
                                * ((e2.getX() - e1.getX())));
                        int offsetY = (int) (mainButton_bottom.getHeight() / dist
                                * ((e2.getY() - e1.getY())));

                        final Animation fly = new TranslateAnimation(bottom_x - offsetX,
                                bottom_x, bottom_y - offsetY, bottom_y);
                        fly.setInterpolator(new DecelerateInterpolator());
                        fly.setDuration(200);
                        fly.setAnimationListener(new AnimationListener() {

                            @Override
                            public void onAnimationStart(Animation animation) {
                                mainButton_bottom.setAlpha(1.0f);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                mainButton.startAnimation(releaseAnimation);
                            }
                        });
                        mainButton_bottom.startAnimation(fly);
                    }
                }
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                Log.d(TAG, "down");
                return false;
            }
        });

        OnTouchListener gestureListener = new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean isHandle = gdt.onTouchEvent(event);
                return true;
            }
        };

        mainButton.setOnTouchListener(gestureListener);
    }

    private void setDockVisibility(final boolean visible) {
        Animation dockAnimation = new TranslateAnimation(dock.getX(),
                dock.getX(), (!visible) ? 0 : dock.getHeight(),
                visible ? 0 : dock.getHeight());

        Animation dockAlphaAnimation = new AlphaAnimation(visible ? 0 : 1, visible ? 1 : 0);

        AnimationSet dockAnimationSet = new AnimationSet(true);
        dockAnimationSet.addAnimation(dockAlphaAnimation);
        dockAnimationSet.addAnimation(dockAnimation);
        dockAnimationSet.setDuration(500);
        // dockAnimationSet.setInterpolator(new
        // AnticipateOvershootInterpolator());
        if (visible) {
            dockAnimationSet.setInterpolator(new DecelerateInterpolator());
        } else {
            dockAnimationSet.setInterpolator(new AccelerateInterpolator());
        }
        dockAnimationSet.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                dock.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (!visible) {
                    dock.setVisibility(View.INVISIBLE);
                } else {
                    dock.setVisibility(View.VISIBLE);
                }
            }
        });

        dock.startAnimation(dockAnimationSet);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        new Handler().postDelayed(new Runnable() {
//
//            @Override
//            public void run() {
//                setEpigramVisibility(false);
//            }
//        }, 3000);
    }

    private void setEpigramVisibility(final boolean visible) {
        Animation logoAnimation = new TranslateAnimation(logo.getX(),
                logo.getX(), (!visible) ? 0 : logo.getHeight(),
                (visible) ? 0 : logo.getHeight());

        Animation logoAlphaAnimation = new AlphaAnimation(!visible ? 1 : 0, !visible ? 0 : 1);

        AnimationSet logoAnimationSet = new AnimationSet(true);
        logoAnimationSet.addAnimation(logoAlphaAnimation);
        // logoAnimationSet.addAnimation(logoAnimation);
        logoAnimationSet.setDuration(500);
        // if (visible) {
        // logoAnimationSet.setInterpolator(new DecelerateInterpolator());
        // } else {
        // logoAnimationSet.setInterpolator(new AccelerateInterpolator());
        // }
        logoAnimationSet.setInterpolator(new LinearInterpolator());
        logoAnimationSet.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                logo.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (!visible) {
                    logo.setVisibility(View.INVISIBLE);
                } else {
                    logo.setVisibility(View.VISIBLE);
                }
            }
        });

        logo.startAnimation(logoAnimationSet);
    }

    private void setInfoPanelVisibility(final boolean visible) {
        Animation infoAnimation = new TranslateAnimation(infoPanel.getX(),
                infoPanel.getX(), (!visible) ? 0 : -infoPanel.getHeight(),
                (visible) ? 0 : -infoPanel.getHeight());

        Animation infoAlphaAnimation = new AlphaAnimation(!visible ? 1 : 0, !visible ? 0 : 1);

        AnimationSet infoAnimationSet = new AnimationSet(true);
        infoAnimationSet.addAnimation(infoAlphaAnimation);
        // logoAnimationSet.addAnimation(logoAnimation);
        infoAnimationSet.setDuration(500);
        // if (visible) {
        // logoAnimationSet.setInterpolator(new DecelerateInterpolator());
        // } else {
        // logoAnimationSet.setInterpolator(new AccelerateInterpolator());
        // }
        infoAnimationSet.setInterpolator(new LinearInterpolator());
        infoAnimationSet.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                infoPanel.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (!visible) {
                    infoPanel.setVisibility(View.INVISIBLE);
                } else {
                    infoPanel.setVisibility(View.VISIBLE);
                }
            }
        });

        infoPanel.startAnimation(infoAnimationSet);
    }

    @Override
    public void onResume() {
        mMapView.onResume();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        mainActivity.removeListener(listener);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    public class ToolsPagerAdapter extends FragmentPagerAdapter {

        public ToolsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new UtilsFragment();
                case 1:
                    return new InfoFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 1;
        }
    }

    public static class InfoFragment extends Fragment {
        private View view;
        private TextView lonText;
        private TextView latText;
        private Typeface font;

        private MainActivityListener listener = new MainActivityListener() {

            @Override
            public void onLocationChanged(Location loc) {
                if (lonText != null && latText != null) {
                    lonText.setText(loc.getLongitude() + "");
                    latText.setText(loc.getLatitude() + "");
                }
            }
        };

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            if (view == null) {
                font = Typeface
                        .createFromAsset(getActivity().getAssets(), "fonts/digital_font.ttf");
                view = inflater.inflate(R.layout.tools_fragment, container, false);
                lonText = (TextView) view.findViewById(R.id.lon_text);
                latText = (TextView) view.findViewById(R.id.lat_text);

                lonText.setTypeface(font);
                latText.setTypeface(font);

                MainActivity.getInstance().addListener(listener);
            }
            return view;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            MainActivity.getInstance().removeListener(listener);
        }
    }

    public static class UtilsFragment extends Fragment {
        private View view;

        public UtilsFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            if (view == null) {
                view = inflater.inflate(R.layout.utils_fragment, container, false);
            }
            return view;
        }
    }

}
