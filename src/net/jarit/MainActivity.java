
package net.jarit;

import java.util.LinkedList;
import java.util.Locale;

import net.jarit.JarItService.JarItServiceBinder;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.common.collect.Lists;

@SuppressLint("ValidFragment")
public class MainActivity extends FragmentActivity implements ActionBar.TabListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static MainActivity instance;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    public static final int HANDLER_ON_LOCATION_CHANGED = 1;
    public static Handler handler;
    public JarItServiceBinder binder;

    private MapFragment mapFragment;
    private BtnFragment btnFragment;
    private TreasureListFragment treasureListFragment;
    private Location lastLocation;

    private FragmentTabHost mTabHost;
    private LayoutInflater layoutInflater;

    private Class fragmentArray[] = {
            MapFragment.class, TreasureListFragment.class
    };
    private int mImageViewArray[] = {
            R.drawable.tab_home_btn, R.drawable.tab_home_btn
    };
    private String mTextviewArray[] = {
            "﻿宝藏雷达", "我的宝藏"
    };

    private LinkedList<MainActivityListener> listeners = Lists.newLinkedList();

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (JarItService.JarItServiceBinder) service;
            // binder.startWatchingLocation();
        }
    };

    public static interface MainActivityListener {
        public void onLocationChanged(Location loc);
    }

    public void addListener(MainActivityListener l) {
        listeners.add(l);
        if (lastLocation != null) {
            l.onLocationChanged(lastLocation);
        }
    }

    public void removeListener(MainActivityListener l) {
        listeners.remove(l);
    }

    public void createTab(int view, int titleView, String title) {
        ActionBar.Tab tab = getActionBar().newTab();
        tab.setTabListener(this);
        tab.setCustomView(view);
        tab.setTabListener(this);
        getActionBar().addTab(tab);

        ((TextView) findViewById(titleView)).setText(title);
    }

    private View getTabItemView(int index) {
        View view = layoutInflater.inflate(R.layout.tab_item_view, null);

        ImageView imageView = (ImageView) view.findViewById(R.id.imageview);
        imageView.setImageResource(mImageViewArray[index]);

        TextView textView = (TextView) view.findViewById(R.id.textview);
        textView.setText(mTextviewArray[index]);

        return view;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getActionBar();
        // actionBar.setNavigationMode(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.hide();

        instance = this;

        setContentView(R.layout.activity_main);

        layoutInflater = LayoutInflater.from(this);

        mTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);

        int count = fragmentArray.length;

        for (int i = 0; i < count; i++) {
            TabSpec tabSpec = mTabHost.newTabSpec(mTextviewArray[i])
                    .setIndicator(getTabItemView(i));
            mTabHost.addTab(tabSpec, fragmentArray[i], null);
            mTabHost.getTabWidget().getChildAt(i)
                    .setBackgroundResource(R.drawable.selector_tab_background);
        }

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == HANDLER_ON_LOCATION_CHANGED) {
                    Location location = (Location) msg.obj;
                    Log.d(TAG, "Handler got location " + location.toString());
                    lastLocation = location;
                    for (MainActivityListener listener : listeners) {
                        listener.onLocationChanged(location);
                    }
                }
            }
        };

        Intent intent = new Intent(this, JarItService.class);
        startService(intent);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);

        int gmsSupport = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(getApplicationContext());
        if (gmsSupport != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Service is not available.");
        }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    mapFragment = new MapFragment();
                    return mapFragment;
                case 1:
                    btnFragment = new BtnFragment();
                    return btnFragment;
                case 2:
                    treasureListFragment = new TreasureListFragment();
                    return treasureListFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }
    }

    public static class BtnFragment extends Fragment {
        private View view;
        private TextView console;
        private Button checkLocBtn;

        public BtnFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            view = inflater.inflate(R.layout.fragment_btns, container, false);
            console = (TextView) view.findViewById(R.id.console);
            checkLocBtn = (Button) view.findViewById(R.id.checkLoc);
            return view;
        }

        public void printToConsole(String s) {
            console.append(s + "\n");
        }
    }

    public void onStartWatchingClick(View v) {
        binder.startWatchingLocation();
    }

    public void onStopWatchingClick(View v) {
        binder.stopWatchingLocation();
    }

    public void onCheckLocationClick(View v) {
        binder.checkLastLocation();
    }

    public void onShowNewJarFoundBtnClick(View v) {
        Intent i = new Intent(this, NewJarFoundActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    public void onSetNewJar(View v) {
        Intent i = new Intent(this, SetNewJarActivity.class);
        i.putExtra(Constant.LOCATION, lastLocation);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    public static MainActivity getInstance() {
        return instance;
    }
}
