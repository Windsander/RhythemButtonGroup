package com.special.rhythembuttongroup.core;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.special.rhythmbuttongroup.R;

public class RhythmAdapter extends BaseAdapter {

	private Context context;
	private List<CardBean> list;
	private int itemWidth;
	private int mMaxItemHeight;
	private int margin = 5;
	
	public RhythmAdapter(Context context, List<CardBean> list) {
		super();
		this.context = context;
		this.list = list;
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return list.get(position).getId();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		RhythmButton view = new RhythmButton(context);
		view.setImageResource(list.get(position).getId());
		//保证选项卡间距
		LinearLayout item_container = (LinearLayout) view.findViewById(R.id.item_container);
		LayoutParams params = (LayoutParams) item_container.getLayoutParams();

		if(itemWidth < params.width){
			//此时，选项卡过大，需要适当缩减布局
			reductionChildLayout(view, params);
		}
		
		excursionChild(view, params);
		view.setTranslationY(mMaxItemHeight);
		return view;
	}

	/**
	 * 动态设置选项卡偏移
	 * @param view 选项卡
	 * @param params 选项卡内部，卡片容器布局参数
	 */
	private void excursionChild(final RhythmButton view, final LayoutParams params) {
		view.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			
			@SuppressWarnings("deprecation")
			@Override
			public void onGlobalLayout() {
				view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				view.getLayoutParams().width = itemWidth;
				margin = (itemWidth-params.width) / 2;
				params.setMargins(margin, 0, margin, 0);
			}
		});
	}

	/**
	 * 缩减选项卡布局
	 * @param view 选项卡
	 * @param params 选项卡内部，卡片容器布局参数
	 */
	private void reductionChildLayout(final RhythmButton view, final LayoutParams params) {
		//计算缩放比例
		int absD = Math.abs(itemWidth - params.width);
		final float scale = absD / (float)params.width;
		final ImageView iv_item_img = (ImageView) view.findViewById(R.id.iv_item_img);
		final TextView tv_item_text = (TextView) view.findViewById(R.id.tv_item_text);
		//改变图片大小
		view.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			
			@SuppressWarnings("deprecation")
			@Override
			public void onGlobalLayout() {
				view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				params.height = (int) (itemWidth * (1 - scale) * 1.5);
				params.width  = (int) (itemWidth * (1 - scale) + 10);
				
				//重新设置图片
				LinearLayout.LayoutParams imageLayout = (android.widget.LinearLayout.LayoutParams) iv_item_img.getLayoutParams();
				imageLayout.height = (int) (params.height * 0.75);
				imageLayout.width  = params.width - 10; 
				imageLayout.setMargins(0, 0, 0, (int)(params.height * 0.15));
				view.getLayoutParams().width = itemWidth;
			}
		});
		//隐藏文字说明
		tv_item_text.setVisibility(View.GONE);
	}
	
	/**
	 * 设置每个控件被分配的屏幕可用位置宽度
	 * @param width 可用宽度
	 */
	public void setWidth(int width){
		this.itemWidth = width;
	}

	public void setMaxItemHeight(int mMaxItemHeight){
		this.mMaxItemHeight = mMaxItemHeight;
	}
}
