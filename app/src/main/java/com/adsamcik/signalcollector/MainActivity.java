package com.adsamcik.signalcollector;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.adsamcik.signalcollector.classes.DataStore;
import com.adsamcik.signalcollector.classes.SnackMaker;
import com.adsamcik.signalcollector.classes.Success;
import com.adsamcik.signalcollector.fragments.FragmentMain;
import com.adsamcik.signalcollector.fragments.FragmentMap;
import com.adsamcik.signalcollector.fragments.FragmentSettings;
import com.adsamcik.signalcollector.fragments.FragmentStats;
import com.adsamcik.signalcollector.interfaces.ITabFragment;
import com.adsamcik.signalcollector.play.PlayController;
import com.google.firebase.crash.FirebaseCrash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends FragmentActivity {
	public static final String TAG = "Signals";

	private FloatingActionButton fabOne;
	private FloatingActionButton fabTwo;

	private ViewPager viewPager;

	private SnackMaker snackMaker;

	private ViewPager.OnPageChangeListener pageChangeListener;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		DataStore.setContext(this);

		View containerView = findViewById(R.id.container);
		if (containerView != null) {
			PlayController.initializeGamesClient(containerView, this);
			snackMaker = new SnackMaker(containerView);
		} else
			FirebaseCrash.report(new Throwable("container view is null. something is wrong."));

		Success s = PlayController.initializeActivityClient(this);
		if (!s.getSuccess())
			snackMaker.showSnackbar(s.message);

		Assist.initialize(this);

		if (Setting.getPreferences(this).getBoolean(Setting.SCHEDULED_UPLOAD, false))
			DataStore.requestUpload(this, true);

		ColorStateList primary = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.textPrimary));
		ColorStateList secondary = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorAccent));

		fabOne = (FloatingActionButton) findViewById(R.id.fabOne);
		fabOne.setBackgroundTintList(secondary);
		fabOne.setImageTintList(primary);

		fabTwo = (FloatingActionButton) findViewById(R.id.fabTwo);
		fabTwo.setBackgroundTintList(primary);
		fabTwo.setImageTintList(secondary);

		Resources r = getResources();


		if (!Assist.hasNavBar(getWindowManager())) {
			CoordinatorLayout.LayoutParams lp = new CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.WRAP_CONTENT, CoordinatorLayout.LayoutParams.WRAP_CONTENT);
			lp.setMargins(0, 0, 0, 0);
			fabOne.setLayoutParams(lp);
		}

		if (viewPager == null && containerView != null) {
			viewPager = (ViewPager) containerView;
			viewPager.setOffscreenPageLimit(1);

			final ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), this);
			adapter.addFrag(FragmentMain.class, r.getString(R.string.menu_dashboard));
			adapter.addFrag(FragmentMap.class, r.getString(R.string.menu_map));
			adapter.addFrag(FragmentStats.class, r.getString(R.string.menu_stats));
			adapter.addFrag(FragmentSettings.class, r.getString(R.string.menu_settings));
			viewPager.setAdapter(adapter);

			TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
			tabLayout.setupWithViewPager(viewPager);

			final FragmentActivity a = this;

			pageChangeListener = new ViewPager.OnPageChangeListener() {
				ITabFragment prevFragment = adapter.getInstance(viewPager.getCurrentItem());
				int prevFragmentIndex = viewPager.getCurrentItem();

				@Override
				public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				}

				@Override
				public void onPageSelected(int position) {
					if (prevFragment != null)
						prevFragment.onLeave();

					ITabFragment tf = adapter.getInstance(position);
					if (tf == null)
						return;
					Success response = tf.onEnter(a, fabOne, fabTwo);
					if (!response.getSuccess()) {
						final View v = findViewById(R.id.container);
						if (v == null) {
							FirebaseCrash.report(new Exception("Container was not found. Is Activity created?"));
							return;
						}
						//it cannot be null because this is handled in getSuccess
						@SuppressWarnings("ConstantConditions") Snackbar snack = Snackbar.make(v, response.message, 4000);
						View view = snack.getView();
						view.setPadding(0, 0, 0, Assist.getNavBarHeight(a));
						snack.show();
						fabOne.hide();
						fabTwo.hide();
					}

					prevFragmentIndex = position;
					prevFragment = tf;
				}

				@Override
				public void onPageScrollStateChanged(int state) {
				}
			};

			viewPager.addOnPageChangeListener(pageChangeListener);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		for (int grantResult : grantResults) {
			if (grantResult != PackageManager.PERMISSION_GRANTED)
				return;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 9001 && resultCode == -1)
			PlayController.reconnect();
	}

	private class ViewPagerAdapter extends FragmentPagerAdapter {
		private final List<Class<? extends ITabFragment>> mFragmentList = new ArrayList<>(4);
		private final List<String> mFragmentTitleList = new ArrayList<>(4);
		private ITabFragment[] mInstanceList;
		private final FragmentActivity activity;

		private ViewPagerAdapter(FragmentManager manager, FragmentActivity activity) {
			super(manager);
			this.activity = activity;
		}

		@Override
		public Fragment getItem(int position) {
			try {
				return (Fragment) mFragmentList.get(position).newInstance();
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			ITabFragment instance = (ITabFragment) super.instantiateItem(container, position);
			boolean createInstance = mInstanceList == null;
			if (mInstanceList == null) {
				mInstanceList = new ITabFragment[mFragmentList.size()];
			} else if (mFragmentList.size() <= position) {
				mInstanceList = Arrays.copyOf(mInstanceList, mFragmentList.size());
			}

			mInstanceList[position] = instance;

			if(createInstance)
				pageChangeListener.onPageSelected(viewPager.getCurrentItem());


			return instance;
		}

		public ITabFragment getInstance(int position) {
			return mInstanceList == null || position >= mInstanceList.length ? null : mInstanceList[position];
		}

		@Override
		public int getCount() {
			return mFragmentList.size();
		}

		private void addFrag(Class<? extends ITabFragment> fragment, String title) {
			mFragmentList.add(fragment);
			mFragmentTitleList.add(title);
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return mFragmentTitleList.get(position);
		}
	}
}
