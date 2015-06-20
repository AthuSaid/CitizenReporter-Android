package org.wordpress.android.ripoti.ui.main;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.View;

import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

import org.wordpress.android.GCMIntentService;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.networking.SelfSignedSSLCertsManager;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.main.MySiteFragment;
import org.wordpress.android.ui.media.MediaAddFragment;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.BlogPreferencesActivity;
import org.wordpress.android.ui.reader.ReaderEvents;
import org.wordpress.android.ui.reader.ReaderPostListFragment;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AuthenticationDialogUtils;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.util.CoreEvents.UserSignedOutCompletely;
import org.wordpress.android.util.CoreEvents.UserSignedOutWordPressCom;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.SlidingTabLayout;
import org.wordpress.android.widgets.WPMainViewPager;

import de.greenrobot.event.EventBus;
/**
 * Main activity which hosts sites, reader, me and notifications tabs
 */
public class MainActivity extends Activity
        implements ViewPager.OnPageChangeListener,
        SlidingTabLayout.SingleTabClickListener{
    private WPMainViewPager mViewPager;
    private SlidingTabLayout mTabs;
    private RipotiMainTabAdapter mTabAdapter;

    public static final String ARG_OPENED_FROM_PUSH = "opened_from_push";

    /*
     * tab fragments implement this if their contents can be scrolled, called when user
     * requests to scroll to the top
     */
    public interface OnScrollToTopListener {
        void onScrollToTop();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setStatusBarColor();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.ripoti_main_screen);

        mViewPager = (WPMainViewPager) findViewById(R.id.viewpager_main);
        mTabAdapter = new RipotiMainTabAdapter(getFragmentManager());
        mViewPager.setAdapter(mTabAdapter);

        mTabs = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        mTabs.setSelectedIndicatorColors(getResources().getColor(R.color.tab_indicator));

        // tabs are left-aligned rather than evenly distributed in landscape
        mTabs.setDistributeEvenly(!DisplayUtils.isLandscape(this));

        Integer icons[] = {R.drawable.main_tab_sites,
                R.drawable.main_tab_reader};

        mTabs.setCustomTabView(R.layout.tab_icon, R.id.tab_icon, R.id.tab_badge, icons);

        // content descriptions
        mTabs.setContentDescription(RipotiMainTabAdapter.TAB_ASSIGNMENTS, getString(R.string.tabbar_accessibility_label_my_site));
        mTabs.setContentDescription(RipotiMainTabAdapter.TAB_MY_POSTS, getString(R.string.reader));

        mTabs.setViewPager(mViewPager);
        mTabs.setOnSingleTabClickListener(this);

        // page change listener must be set on the tab layout rather than the ViewPager
        mTabs.setOnPageChangeListener(this);

        if (savedInstanceState == null) {
            if (AccountHelper.isSignedIn()) {

                    int position = AppPrefs.getMainTabIndex();
                    if (mTabAdapter.isValidPosition(position) && position != mViewPager.getCurrentItem()) {
                        mViewPager.setCurrentItem(position);
                    }

            } else {
                ActivityLauncher.showSignInForResult(this);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.status_bar_tint));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

    }


    @Override
    public void onPageSelected(int position) {
        // remember the index of this page
        AppPrefs.setMainTabIndex(position);

        switch (position) {
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // noop
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        /*TODO: Do we still need this?
        // fire event if the "My Site" page is being scrolled so the fragment can

        // animate its fab to match
        if (position == WPMainTabAdapter.TAB_MY_SITE) {
            EventBus.getDefault().post(new MainViewPagerScrolled(positionOffset));
        }
         */
    }

    /*
     * user tapped a tab above the viewPager - detect when the active tab is clicked and scroll
     * the fragment to the top if available
     */
    @Override
    public void onTabClick(View view, int position) {
        if (position == mViewPager.getCurrentItem()) {
            Fragment fragment = mTabAdapter.getFragment(position);
            if (fragment instanceof OnScrollToTopListener) {
                ((OnScrollToTopListener) fragment).onScrollToTop();
            }
        }
    }

    @Override
    protected void onPause() {


        super.onPause();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    /*
     * re-create the fragment adapter so all its fragments are also re-created - used when
     * user signs in/out so the fragments reflect the active account
     */
    private void resetFragments() {
        AppLog.i(AppLog.T.MAIN, "main activity > reset fragments");

        // remove the event that determines when followed tags/blogs are updated so they're
        // updated when the fragment is recreated (necessary after signin/disconnect)
        EventBus.getDefault().removeStickyEvent(ReaderEvents.UpdatedFollowedTagsAndBlogs.class);

        // remember the current tab position, then recreate the adapter so new fragments are created
        int position = mViewPager.getCurrentItem();
        mTabAdapter = new RipotiMainTabAdapter(getFragmentManager());
        mViewPager.setAdapter(mTabAdapter);

        // restore previous position
        if (mTabAdapter.isValidPosition(position)) {
            mViewPager.setCurrentItem(position);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /*
        switch (requestCode) {
            case RequestCodes.EDIT_POST:
                if (resultCode == RESULT_OK) {
                    MySiteFragment mySiteFragment = getMySiteFragment();
                    if (mySiteFragment != null) {
                        mySiteFragment.onActivityResult(requestCode, resultCode, data);
                    }
                }
                break;
            case RequestCodes.READER_SUBS:
                break;
            case RequestCodes.ADD_ACCOUNT:
                if (resultCode == RESULT_OK) {
                    WordPress.registerForCloudMessaging(this);
                    resetFragments();
                } else if (!AccountHelper.isSignedIn()) {
                    // can't do anything if user isn't signed in (either to wp.com or self-hosted)
                    finish();
                }
                break;
            case RequestCodes.REAUTHENTICATE:
                if (resultCode == RESULT_CANCELED) {
                    ActivityLauncher.showSignInForResult(this);
                } else {
                    WordPress.registerForCloudMessaging(this);
                }
                break;
            case RequestCodes.NOTE_DETAIL:
                if (resultCode == RESULT_OK && data != null) {
                    moderateCommentOnActivityResult(data);
                }
                break;
            case RequestCodes.PICTURE_LIBRARY:
                FragmentManager fm = getFragmentManager();
                Fragment addFragment = fm.findFragmentByTag(MySiteFragment.ADD_MEDIA_FRAGMENT_TAG);
                if (addFragment != null && data != null) {
                    ToastUtils.showToast(this, R.string.image_added);
                    addFragment.onActivityResult(requestCode, resultCode, data);
                }
                break;
            case RequestCodes.SITE_PICKER:
                if (getMySiteFragment() != null) {
                    getMySiteFragment().onActivityResult(requestCode, resultCode, data);
                }
                break;
            case RequestCodes.BLOG_SETTINGS:
                if (resultCode == BlogPreferencesActivity.RESULT_BLOG_REMOVED) {
                    // user removed the current (self-hosted) blog from blog settings
                    if (!AccountHelper.isSignedIn()) {
                        ActivityLauncher.showSignInForResult(this);
                    } else {
                        MySiteFragment mySiteFragment = getMySiteFragment();
                        if (mySiteFragment != null) {
                            mySiteFragment.setBlog(WordPress.getCurrentBlog());
                        }
                    }
                }
                break;
        }*/
    }

    /*
     * returns the assignments fragment from the sites tab
     */
    public AssignmentsFragment getAssignmentsFragment() {
        return getFragmentByPosition(RipotiMainTabAdapter.TAB_ASSIGNMENTS, AssignmentsFragment.class);
    }

    /*
    * returns the myPosts fragment from the sites tab
    */

    public MySiteFragment getMySiteFragment() {
        return getFragmentByPosition(RipotiMainTabAdapter.TAB_MY_POSTS, MySiteFragment.class);
    }

    private <T> T getFragmentByPosition(int position, Class<T> type) {
        Fragment fragment = mTabAdapter != null ? mTabAdapter.getFragment(position) : null;

        if (fragment != null && type.isInstance(fragment)) {
            return type.cast(fragment);
        }

        return null;
    }

    // Events

    @SuppressWarnings("unused")
    public void onEventMainThread(UserSignedOutWordPressCom event) {
        resetFragments();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(UserSignedOutCompletely event) {
        ActivityLauncher.showSignInForResult(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.InvalidCredentialsDetected event) {
        AuthenticationDialogUtils.showAuthErrorView(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.RestApiUnauthorized event) {
        AuthenticationDialogUtils.showAuthErrorView(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.TwoFactorAuthenticationDetected event) {
        AuthenticationDialogUtils.showAuthErrorView(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.InvalidSslCertificateDetected event) {
        SelfSignedSSLCertsManager.askForSslTrust(this, null);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.LoginLimitDetected event) {
        ToastUtils.showToast(this, R.string.limit_reached, ToastUtils.Duration.LONG);
    }

}
