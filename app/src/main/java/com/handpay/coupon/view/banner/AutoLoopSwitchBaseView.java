package com.handpay.coupon.view.banner;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Scroller;

import com.handpay.coupon.R;
import com.handpay.coupon.utils.LogT;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


/**
 * 手动自动可以滚动
 *
 * @author ryze
 * @since 1.0  2016/07/18
 */
public abstract class AutoLoopSwitchBaseView extends RelativeLayout implements ViewPager.OnPageChangeListener {

    protected ViewPager mViewPager;
    //    protected PageShowView mPageShowView;

    private LinearLayout indicateLayout;//指示view

    private List<Integer> indicate = new ArrayList<>();//指示view图片资源

    protected View mFailtView;

    private int mCurrentItem = 1;

    protected LoopHandler mHandler;

    protected boolean mIsDragging = false;

    protected AutoLoopSwitchBaseAdapter mPagerAdapter;

    //监听数据变化，用于去除 重试View
    private DataSetObserver mObserver;

    //正在切页
    private boolean isLoopSwitch = false;

    public AutoLoopSwitchBaseView(Context context) {
        super(context);
        initView();
    }

    public AutoLoopSwitchBaseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public AutoLoopSwitchBaseView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }



    /**
     * 页面切换回调
     */
    protected abstract void onSwitch(int index, Object model);

    /**
     * 滚动延时时间
     */
    protected abstract long getDurtion();

    /**
     * 如果需要网络异常处理
     */
    protected abstract View getFailtView();

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
       // boolean mCurrentVisible = visibility == VISIBLE;
    }



    private void initView() {
        mViewPager = new ViewPager(getContext());
        mViewPager.setId(R.id.autoloopswitch_viewpager_id);
        mViewPager.addOnPageChangeListener(this);
        addView(mViewPager, generalLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        controlViewPagerSpeed();
        mHandler = new LoopHandler(this);
    }

    private LayoutParams generalLayoutParams(int w, int h) {
        return new LayoutParams(w, h);
    }

    /**
     * 初始化指示器
     */
    public void initIndicate(List<Integer> indicate) {
        indicateLayout = new LinearLayout(getContext());
        indicateLayout.setId(R.id.autoloopswitch_pagershow_id);
        this.indicate = indicate;

        int datacount = mPagerAdapter.getDataCount();
        if (datacount == 1) {
            indicateLayout.setVisibility(View.GONE);
        } else {
            indicateLayout.setVisibility(View.VISIBLE);
            indicateLayout.removeAllViewsInLayout();

            for (int index = 0; index < datacount; index++) {
                final View indicater = new ImageView(getContext());
                indicateLayout.addView(indicater, index);
            }
        }

        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        LayoutParams params = generalLayoutParams(LayoutParams.WRAP_CONTENT,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 7, displayMetrics));
        params.bottomMargin = 7;
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        addView(indicateLayout, params);
    }

    /**
     * 刷新提示器
     */
    protected void refreshIndicateView(int currentIndex, int datacount) {
        if (datacount > 1) {
            for (int index = 0; index < datacount; index++) {
                if (indicateLayout != null) {
                    ImageView imageView = (ImageView) indicateLayout.getChildAt(index);
                    //圆点间隔
                    imageView.setPadding(6, 0, 10, 0);
                    if (currentIndex == index) {
                        imageView.setImageResource(indicate.get(1));
                    } else {
                        imageView.setImageResource(indicate.get(0));
                    }
                }
            }
        }
    }

    @Override
    public void onPageScrolled(int i, float v, int i1) {

    }

    @Override
    public void onPageSelected(int i) {
        if (isLoopSwitch) {
            isLoopSwitch = false;
            return;
        }

        mCurrentItem = i;
        int datacount = mPagerAdapter.getDataCount();
        if (datacount > 1) {
            int index = mPagerAdapter.getActualIndex(i) % datacount;
            //            mPageShowView.setCurrentView(index, datacount);
            refreshIndicateView(index, datacount);
            onSwitch(index, mPagerAdapter.getItem(index));
        }
    }

    @Override
    public void onPageScrollStateChanged(int i) {
        if (i == ViewPager.SCROLL_STATE_DRAGGING) {
            mIsDragging = true;
        } else if (i == ViewPager.SCROLL_STATE_IDLE) {
            if (mViewPager.getCurrentItem() == 0) {
                isLoopSwitch = true;
                mViewPager.setCurrentItem(mPagerAdapter.getCount() - 2, false);
            } else if (mViewPager.getCurrentItem() == mPagerAdapter.getCount() - 1) {
                isLoopSwitch = true;
                mViewPager.setCurrentItem(1, false);
            }
            mCurrentItem = mViewPager.getCurrentItem();

            if (mIsDragging && mHandler != null) {
                //如果从dragging状态到不是mIsDragging
                mHandler.sendEmptyMessageDelayed(LoopHandler.MSG_UPDATE, getDurtion());
            }
            mIsDragging = false;
        }
    }
    private void notifyDataSetChanged() {
        if (mPagerAdapter != null) {
            int datacount = mPagerAdapter.getDataCount();

            int currentIndex;
            if (datacount > 1) {
                mCurrentItem = mPagerAdapter.getCount() / 2;
                currentIndex = mPagerAdapter.getActualIndex(mCurrentItem) % datacount;
            } else {
                mCurrentItem = 1;
                currentIndex = 0;
            }
            mViewPager.setCurrentItem(mCurrentItem);
            //            mPageShowView.setCurrentView(currentIndex, datacount);
            refreshIndicateView(currentIndex, datacount);

            if (mFailtView != null && datacount > 0) {
                removeView(mFailtView);
                mFailtView = null;
            }

            updateView();
        }
    }

    private void updateView() {
        for (int i = 0; i < mViewPager.getChildCount(); i++) {
            View v = mViewPager.getChildAt(i);
            if (v != null) {
                int position = (Integer) v.getTag();
                mPagerAdapter.updateView(v, position);
            }
        }
    }

    public void setAdapter(AutoLoopSwitchBaseAdapter adapter) {
        if (mPagerAdapter != null) {
            mPagerAdapter.unregisterDataSetObserver(mObserver);
        }
        this.mPagerAdapter = adapter;
        if (mPagerAdapter != null) {
            if (mObserver == null) {
                mObserver = new PagerObserver();
            }
            mPagerAdapter.registerDataSetObserver(mObserver);

            if (mViewPager != null) {
                mViewPager.setAdapter(mPagerAdapter);
            }
            //如果没有数据，同时没有网络的情况
            if (mPagerAdapter.getDataCount() <= 0) {
                mFailtView = getFailtView();
                if (mFailtView != null) {
                    addView(mFailtView, generalLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                }
            }
        }
//        else {
//            throw new NullPointerException("AutoLoopSwitchBaseAdapter can not null");
//        }
    }
    /**
     * 轮播监控
     */
    protected static class LoopHandler extends Handler {
        //请求更新显示的View。
        private static final int MSG_UPDATE = 1;
        //请求暂停轮播。
        public static final int MSG_STOP = 2;
        //请求恢复轮播。
        public static final int MSG_REGAIN = 3;

        private AutoLoopSwitchBaseView mView;

        private boolean mIsStop = false;

        public LoopHandler(AutoLoopSwitchBaseView mView) {
            this.mView = new WeakReference<>(mView).get();
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (mView == null || mView.mHandler == null || mView.mPagerAdapter == null || mView.mIsDragging) {
                return;
            }

            switch (msg.what) {
                case MSG_UPDATE:
                    if (mIsStop || hasMessages(MSG_UPDATE)) {
                        return;
                    }
                    if (mView.mPagerAdapter.getCount() > 1) {
                        mView.mCurrentItem++;
                        mView.mCurrentItem %= mView.mPagerAdapter.getCount();
                        mView.mViewPager.setCurrentItem(mView.mCurrentItem, true);
                        sendEmptyMessageDelayed(MSG_UPDATE, mView.getDurtion());
                    }
                    break;
                case MSG_STOP:
                    if (hasMessages(MSG_UPDATE)) {
                        removeMessages(MSG_UPDATE);
                    }
                    mIsStop = true;
                    break;
                case MSG_REGAIN:
                    if (hasMessages(MSG_UPDATE)) {
                        removeMessages(MSG_UPDATE);
                    }
                    sendEmptyMessageDelayed(MSG_UPDATE, mView.getDurtion());
                    mIsStop = false;
                    break;
            }
        }
    }

    private class PagerObserver extends DataSetObserver {
        private PagerObserver() {
        }

        public void onChanged() {
            notifyDataSetChanged();
        }

        public void onInvalidated() {
            notifyDataSetChanged();
        }
    }



    private void controlViewPagerSpeed() {
        try {
            Field mField;

            mField = ViewPager.class.getDeclaredField("mScroller");
            mField.setAccessible(true);

            FixedSpeedScroller mScroller = new FixedSpeedScroller(getContext(),
                    new DecelerateInterpolator());
            int VIEWPAGER_SCROLL_DURTION = 400;
            mScroller.setmDuration(VIEWPAGER_SCROLL_DURTION);
            mField.set(mViewPager, mScroller);

            mField = ViewPager.class.getDeclaredField("mFlingDistance");
            mField.setAccessible(true);
            mField.set(mViewPager, 20);
        } catch (Exception e) {
            LogT.w("viewpage_speed:"+e.getMessage());

        }
    }

    private class FixedSpeedScroller extends Scroller {

        private int mDuration = 600; // default time is 600ms
        public FixedSpeedScroller(Context context, Interpolator interpolator) {
            super(context, interpolator);
        }
        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            // Ignore received duration, use fixed one instead
            super.startScroll(startX, startY, dx, dy, mDuration);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy) {
            // Ignore received duration, use fixed one instead
            super.startScroll(startX, startY, dx, dy, mDuration);
        }

        /**
         * set animation time
         */
        public void setmDuration(int time) {
            mDuration = time;
        }

    }

}
