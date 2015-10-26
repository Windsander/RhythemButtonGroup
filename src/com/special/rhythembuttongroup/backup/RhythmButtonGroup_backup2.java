package com.special.rhythembuttongroup.backup;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.nineoldandroids.view.ViewPropertyAnimator;
import com.special.rhythembuttongroup.core.RhythmAdapter;

@SuppressLint("HandlerLeak")
public class RhythmButtonGroup_backup2 extends HorizontalScrollView {

//参数声明/**************************************************************************************/
	//布局相关变量============================================================
	private Context mContext;
	/** Item存放容器 */
	private LinearLayout mContainer;
	/** 当前布局适配器 */
	private RhythmAdapter mAdapter;
	/** 存放当前界面上显示的Item */
	private List<View> mFixedItem = new ArrayList<View>();
	//TODO 复用暂时不写
	private View mConvertView;
	
	//动画相关变量============================================================
	/** 存放屏幕宽度 */
	private int mScreenWidth;
	/** 存放每个控件宽度 */
	private int mItemWidth;
	/** 存放每个控件的垂直移动最大高度 */
	private int mMaxItemHeight;
	/** 每次位置选项卡纵向移动变化量 */
	private int mPerTranslateY;
	/** 记录当前单屏显示选项卡的数目 */
	private int itemCountOnScreen = 8;
	/** 记录上一个选项卡Id */
	private int preItemId = -1;
	/** 记录选项卡动画持续时间 */
	private long itemAnimationDur = 400;
	/** 标识是否为第一次移动 */
	private static boolean isFirstMove = true;
	/** 选项卡动画插入器 */
	private Interpolator mInterpolator;
	
	//爬梯动画变量============================================================
	/** 记录点击事件开始时间 */
	private long touchStart;
	/** 设置接替动画的触摸准备时长 */
	private static long defaultTouchDur = 250;
	/** 被显示选项卡的起始位置编号 */
	private int firstVisiblePosition;
	/** 被显示选项卡的最后位置编号 */
	private int lastVisiblePosition;
	/** 当选项卡固定时，用于循环检测动画是否应该开启 */
	private Timer timeLooper;
	/** 循环计时器 */
	private Handler handler;
	{
		handler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				startStairAnimation(preItemId);
				handler.sendEmptyMessageDelayed(0, 100);
			}
		};
	}
	
	//状态记录器=============================================================
	/** 记录当前点击状态 */
	private State mState = State.UP;
	
	private static enum State{
		DOWN,MOVE,UP;
	}
//构造方法/**************************************************************************************/
	public RhythmButtonGroup_backup2(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public RhythmButtonGroup_backup2(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public RhythmButtonGroup_backup2(Context context) {
		this(context, null);
	}

//初始化方法/*************************************************************************************/
	/**
	 * 初始化当前布局的关键参数
	 * @param context 
	 */
	private void init(Context context) {
		this.mContext = context;
		WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics outMetrics = new DisplayMetrics();
		windowManager.getDefaultDisplay().getMetrics(outMetrics);
		mScreenWidth = outMetrics.widthPixels;
		
		
	}
	
	/**
	 * 设置选项卡适配器
	 * @param adapter 建议使用 RhythmAdapter 
	 */
	public void setAdapter(RhythmAdapter adapter){
		this.mAdapter = adapter;
		
		initSignificantParameter(); 
		
		mAdapter.setWidth(mItemWidth);
		mAdapter.setMaxItemHeight(mMaxItemHeight);

		//将所有Item填入布局容器中
		if(mContainer == null){
			mContainer = (LinearLayout) getChildAt(0);
			//在此处对非屏幕宽度尺寸的子控件同屏占位，进行动态容器适配
			LayoutParams containerLayout = (LayoutParams) mContainer.getLayoutParams();
			int dynamicMarginSize = (int) ((mScreenWidth - mItemWidth * itemCountOnScreen) / 2 + 0.5);
			containerLayout.setMargins(dynamicMarginSize, 0, dynamicMarginSize, 0);
		}
		for (int i = 0; i < mAdapter.getCount(); i++) {
			mContainer.addView(mAdapter.getView(i, null, null));
			System.out.println("setAdapter addView");
		}
	}

	/**
	 * 初始化关键参数
	 */
	private void initSignificantParameter() {
		//跟新当前itemCountOnScreen，避免无用空间占用
		itemCountOnScreen = Math.min(mAdapter.getCount(), itemCountOnScreen);
		mItemWidth = mScreenWidth / itemCountOnScreen;
		
		//TODO We should dynamicly calculate the mMaxItemHeight for difference itemCountOnScreen value
		// 我们应该动态的在此处计算 对应itemCountOnScreen 的 mMaxItemHeight 值， please wait for my next update
		mPerTranslateY = mItemWidth / itemCountOnScreen;
		mMaxItemHeight = mItemWidth - mPerTranslateY;
	}
	
//重现触摸方法/************************************************************************************/
//	@Override
//	public boolean dispatchTouchEvent(MotionEvent ev) {
//		return false;
//	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mState = State.DOWN;
			updateItemState(ev);
			break;
			
		case MotionEvent.ACTION_MOVE:
			mState = State.MOVE;
			updateItemState(ev);
			break;
			
		case MotionEvent.ACTION_UP:
			mState = State.UP;
			removeLooper(true);
			onFinishAnimation();
			break;
		}
		return true;
	}

	/**
	 * 跟新当前被点击选项卡及容器
	 * @param ev MotionEvent 对象
	 */
	private void updateItemState(MotionEvent ev) {
		int touchPointX = (int) ev.getX();
		getVisibleItem();
		int itemId = (touchPointX + 1) / mItemWidth;
		if(itemId < 0 ){  //健壮性判断条件
			itemId = 0;
		}else if(itemId > mFixedItem.size() - 1){
			itemId = mFixedItem.size() - 1;
		}
		showItemAnimation(itemId);
	}

	/**
	 * 获取当前显示的选项卡集合
	 */
	private void getVisibleItem() {
		mFixedItem.clear();
		firstVisiblePosition = getFirstVisiblePosition();
		lastVisiblePosition = firstVisiblePosition + itemCountOnScreen - 1;
		for (int i = firstVisiblePosition; i <= lastVisiblePosition; i++) {
			mFixedItem.add(mContainer.getChildAt(i));
		}
	}

	/**
	 * 开启对应点击位置选项卡关联动画效果
	 * @param itemId 当前点击的选项卡Id
	 */
	private void showItemAnimation(int itemId) {
		if(preItemId != itemId){
			removeLooper(false);
			touchStart = SystemClock.currentThreadTimeMillis();
			updateStairState(itemId);
		}
		
		switch (mState) {
		case DOWN:
			onStartAnimation(itemId);
			break;

		case MOVE:
			onMoveAnimation(itemId);
			break;
		}
		
	}

	//节奏动画==============================================================
	/**
	 * 被点击时的选项卡动画
	 * @param itemId 被点击选项卡
	 */
	private void onStartAnimation(int itemId) {
		View item = mFixedItem.get(itemId);
		itemAutoMove(item, mPerTranslateY, 2.0f);
		preItemId = itemId;
	}

	/**
	 * 更新当前阶梯动画执行状况
	 * @param itemId 选项卡Id
	 */
	private void updateStairState(final int itemId) {
		timeLooper = new Timer();
		timeLooper.schedule(new TimerTask() {
			//判断当前按钮点击位置，是否位于当前显示的边界位置
			boolean isBound = (mFixedItem.size() - 1 == itemId) || (0 == itemId);
			
			@Override
			public void run() {
				if(isBound){
					//判断是否需要开始发生阶梯动画
					if((touchStart + defaultTouchDur < SystemClock.currentThreadTimeMillis())){
						Log.e("time", isBound+ "       "+(touchStart + defaultTouchDur) + "     " + SystemClock.currentThreadTimeMillis());
						startStairAnimation(itemId);
					}
				}
			}
		}, 100);
		
	}

	/**
	 * 开启阶梯动画
	 * @param itemId 选项卡Id
	 * @param isLeft 位置变量，true：left； false：right
	 */
	private void startStairAnimation(int itemId) {
		if(0 == itemId){                 //To left
			if(firstVisiblePosition > 0){
				RhythmButtonGroup_backup2.this.smoothScrollBy(-mItemWidth, 0);
				onMoveAnimation(itemId - 1);
			}
		}else{                           //To right
			if(lastVisiblePosition < mAdapter.getCount()){
				RhythmButtonGroup_backup2.this.smoothScrollBy(mItemWidth, 0);
				onMoveAnimation(itemId + 1);
			}
		}
		getVisibleItem();
		//记录开始时间，准备阶梯动画
		handler.sendEmptyMessage(0);
	}

	/**
	 * 拖动时的选项卡动画
	 * @param itemId 被操控选项卡
	 */
	private void onMoveAnimation(int itemId) {
		if(itemId != preItemId){
			float tension = 4.0f;
			if(isFirstMove){
				tension = 2.0f;
				isFirstMove = false;
			}
			//左侧动画
			for (int i = itemId - 1; i >= 0; i--) {
				View leftItem = mFixedItem.get(i);
				itemAutoMove(leftItem, mPerTranslateY * (itemId - i + 1), tension);
			}
			//右侧（包括被点击选项卡）动画
			for (int i = itemId; i < mFixedItem.size(); i++) {
				View rightItem = mFixedItem.get(i);
				itemAutoMove(rightItem, mPerTranslateY * (i - itemId + 1), tension);
			}
			preItemId = itemId;
		}
	}
	
	/**
	 * 选项卡的结束动画
	 */
	private void onFinishAnimation() {
		isFirstMove = true;
		for (int i = 0; i < mFixedItem.size(); i++) {
			View item = mFixedItem.get(i);
			itemAutoMove(item, mMaxItemHeight, 2.0f);
		}
	}

	private void itemAutoMove(View item, float dy, float tension) {
		if(mInterpolator != null){
			 ViewPropertyAnimator.animate(item)
				.translationY(dy)
				.setDuration(itemAnimationDur)
				.setInterpolator(mInterpolator);
		}
		ViewPropertyAnimator.animate(item)
			.translationY(dy)
			.setDuration(itemAnimationDur)
			.setInterpolator(new OvershootInterpolator(tension));
	}

//公共方法/**************************************************************************************/	
	/**
	 * 获取当前最早显示的选项卡
	 */
	public int getFirstVisiblePosition() {
		if(itemCountOnScreen <= mAdapter.getCount()){
			for (int i = 0; i < mAdapter.getCount(); i++) {
				if(mContainer.getChildAt(i).getX() < 0){
					continue;
				}else{
					return i;
				}
			}
		}
		return 0;
	}
	
	/**
	 * 设置动画持续时长
	 * @param itemAnimationDur 动画持续时长
	 */
	public void setItemAnimationDur(long itemAnimationDur){
		if(itemAnimationDur > 0){
			this.itemAnimationDur = itemAnimationDur;
		}
	}
	
	/**
	 * 设置动画插入器，建议使用默认，或 BounceInterpolator
	 * @param mInterpolator
	 */
	public void setIteminterpolator(Interpolator mInterpolator){
		this.mInterpolator = mInterpolator;
	}

	/**
	 * 移除阶梯动画循环
	 */
	private void removeLooper(boolean isDown) {
		if(timeLooper != null){
			timeLooper.cancel();
		}
		if(handler != null){
			handler.removeCallbacksAndMessages(null);
		}
		if(isDown){
			preItemId = -1;
			touchStart = 0;
		}
	}

//回调接口/**************************************************************************************/
	

}
