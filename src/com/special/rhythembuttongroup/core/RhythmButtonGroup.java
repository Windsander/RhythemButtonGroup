package com.special.rhythembuttongroup.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.nineoldandroids.view.ViewPropertyAnimator;

@SuppressLint("HandlerLeak")
public class RhythmButtonGroup extends HorizontalScrollView {

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
	/** 设置接替动画的触摸准备时长 */
	private static long defaultTouchDur = 2500;
	/** 被显示选项卡的起始位置编号 */
	private int firstVisiblePosition;
	/** 被显示选项卡的最后位置编号 */
	private int lastVisiblePosition;
	/** 当选项卡固定时，用于循环检测动画是否应该开启 */
	private Timer timeLooper;
	/** 更新界面用Handler */
	private Handler handler;
	{
		handler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				startStairAnimation(); 
			}
		};
	}
	
	//状态记录器=============================================================
	/** 记录当前点击状态 */
	private State mState = State.UP;
	private int itemId;
	
	private static enum State{
		DOWN,MOVE,UP;
	}
//构造方法/**************************************************************************************/
	public RhythmButtonGroup(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public RhythmButtonGroup(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public RhythmButtonGroup(Context context) {
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
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		updateItemState(ev);
		showItemAnimation(ev);
		return true;
	}

	/**
	 * 跟新当前被点击选项卡及容器
	 * @param ev MotionEvent 对象
	 */
	private void updateItemState(MotionEvent ev) {
		int touchPointX = (int) ev.getX();
		getVisibleItem();
		itemId = (touchPointX + 1) / mItemWidth;
		if(itemId < 0 ){  //健壮性判断条件
			itemId = 0;
		}else if(itemId > mFixedItem.size() - 1){
			itemId = mFixedItem.size() - 1;
		}
		
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
	private void showItemAnimation(MotionEvent ev) {
		//阶梯动画
		if(preItemId != itemId){
			removeLooper(false);
			updateStairState();
		}
		//节奏动画
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mState = State.DOWN;
			onStartAnimation();
			onStayAnimation();
			break;
			
		case MotionEvent.ACTION_MOVE:
			mState = State.MOVE;
			onMoveAnimation();
			break;
			
		case MotionEvent.ACTION_UP:
			mState = State.UP;
			removeLooper(true);
			onFinishAnimation();
			break;
		}
		
	}
	
	
	// 1-1 //阶梯动画==============================================================
	/**
	 * 更新当前阶梯动画执行状况
	 * @param itemId 选项卡Id
	 */
	private void updateStairState() {
		timeLooper = new Timer();
		timeLooper.schedule(new MyTimerTask(), 0, 200);
		
	}
	
	/**
	 * 自定义计时器Task
	 * @author Windsander
	 */
	private class MyTimerTask extends TimerTask{
		
		private long touchStart;
		private boolean isBound;
		
		public MyTimerTask() {
			super();
			this.touchStart = System.currentTimeMillis();
			//判断当前按钮点击位置，是否位于当前显示的边界位置
			this.isBound = (mFixedItem.size() - 1 == itemId) || (0 == itemId);
		}
		
		@Override
		public void run() {
			if(isBound){
				long duration = System.currentTimeMillis() - touchStart;
				//判断是否需要开始发生阶梯动画
				if( duration > defaultTouchDur ){
					handler.sendEmptyMessage(0);
					this.cancel();
					timeLooper.cancel();
				}
			}
		}
	}
	
	/**
	 * 开启阶梯动画
	 * @param itemId 选项卡Id
	 * @param isLeft 位置变量，true：left； false：right
	 */
	private void startStairAnimation() {
		isFirstMove = true;
		boolean isLeft = (0 == itemId);
		if(isLeft && firstVisiblePosition > 0){                 //To left
			mFixedItem.remove(itemCountOnScreen - 1);
			mFixedItem.add(0, mContainer.getChildAt(firstVisiblePosition - 1));
			stairAnimation(-mItemWidth);
		}else if(!isLeft && lastVisiblePosition < mAdapter.getCount() - 1){                           //To right
			mFixedItem.remove(0);
			mFixedItem.add(itemCountOnScreen - 1, mContainer.getChildAt(lastVisiblePosition + 1));
			stairAnimation(mItemWidth);
		}else{
			removeLooper(false);
			return;
		}
		
		preItemId = itemId - 1;
		
		handler.sendEmptyMessageDelayed(0, itemAnimationDur + 100);
		
	}
	
	private void stairAnimation(int dx) {
		float tension = 4.0f;
		if(isFirstMove){
			tension = 2.0f;
			isFirstMove = false;
		}
		firstVisiblePosition = getFirstVisiblePosition();
		lastVisiblePosition = firstVisiblePosition + itemCountOnScreen - 1;
		if(firstVisiblePosition > 0 && lastVisiblePosition < mAdapter.getCount() - 1){
			mContainer.getChildAt(firstVisiblePosition -1).setTranslationY(mPerTranslateY);
			mContainer.getChildAt(lastVisiblePosition + 1).setTranslationY(mPerTranslateY);
		}
		leftMoveAnimation(tension, 2);
		rightMoveAnimation(tension, 2);
		preItemId = itemId;
		
		RhythmButtonGroup.this.smoothScrollBy(dx, 0);
	}

	// 2-1 //开启节奏动画============================================================
	/**
	 * 被点击时的选项卡动画
	 * @param itemId 被点击选项卡
	 */
	private void onStartAnimation() {
		View item = mFixedItem.get(itemId);
		itemAutoMove(item, mPerTranslateY, 2.0f);
		preItemId = itemId;
	}

	// 2-2 //滑动节奏动画============================================================
	/**
	 * 拖动时的选项卡动画
	 * @param itemId 被操控选项卡
	 */
	private void onMoveAnimation() {
		if(itemId != preItemId){
			float tension = 4.0f;
			if(isFirstMove){
				tension = 2.0f;
				isFirstMove = false;
			}
			leftMoveAnimation(tension, 1);
			rightMoveAnimation(tension, 1);
			preItemId = itemId;
		}
	}

	// 2-3 //维持节奏动画============================================================
	/**
	 * 维持选项卡当前位置的动画
	 */
	private void onStayAnimation() {
		isFirstMove = true;
		for (int i = 0; i < mFixedItem.size(); i++) {
			View item = mFixedItem.get(i);
			if(itemId != i){
				itemAutoMove(item, mMaxItemHeight, 2.0f);
			}else{
				continue;
			}
		}
	}

	// 2-4 //结束滑动动画============================================================
	/**
	 * 选项卡的结束动画
	 */
	private void onFinishAnimation() {
		int middlePositionId = itemCountOnScreen / 2;
		if(itemId <= middlePositionId){
			int dex = Math.min(middlePositionId - itemId, firstVisiblePosition);
			this.smoothScrollBy(-mItemWidth * dex, 0);
		}else if(itemId > middlePositionId){
			int dex = Math.min(mAdapter.getCount() - lastVisiblePosition, itemId - middlePositionId);
			this.smoothScrollBy(mItemWidth * dex, 0);
		}
	}
	
	@Override
	public void computeScroll() {
		// TODO Auto-generated method stub
		super.computeScroll();
	}
	
	// 3-1 //公用动画资源============================================================
	/**
	 * 左侧动画
	 * @param tension 弹性系数
	 * @param extra 额外缩减系数
	 */
	private void leftMoveAnimation(float tension, int extra) {
		for (int i = itemId - 1; i >= 0; i--) {
			View leftItem = mFixedItem.get(i);
			itemAutoMove(leftItem, mPerTranslateY * (itemId - i + extra), tension);
		}
	}
	
	/**
	 * 右侧（包括被点击选项卡）动画
	 * @param tension 弹性系数
	 * @param extra 额外缩减系数
	 */
	private void rightMoveAnimation(float tension, int extra) {
		for (int i = itemId; i < mFixedItem.size(); i++) {
			View rightItem = mFixedItem.get(i);
			itemAutoMove(rightItem, mPerTranslateY * (i - itemId + extra), tension);
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
				if(mContainer.getChildAt(i).getX() + mItemWidth / 2 < this.getScrollX()){
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
		if(handler != null){
			handler.removeCallbacksAndMessages(null);
		}
		if(timeLooper != null){
			timeLooper.cancel();
			timeLooper = null;
		}
		if(isDown){
			preItemId = -1;
		}
	}

//回调接口/**************************************************************************************/
	

}
