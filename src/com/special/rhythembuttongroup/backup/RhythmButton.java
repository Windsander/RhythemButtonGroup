package com.special.rhythembuttongroup.backup;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.special.rhythmbuttongroup.R;

public class RhythmButton extends FrameLayout {
	private LinearLayout item_container;
	private ImageView iv_item_img;
	private TextView tv_item_text;

	public RhythmButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public RhythmButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public RhythmButton(Context context) {
		this(context, null);
	}
	
	private void init() {
		View view = View.inflate(getContext(), R.layout.item_rhythem_button, this);
		item_container = (LinearLayout) view.findViewById(R.id.item_container);
		iv_item_img = (ImageView) view.findViewById(R.id.iv_item_img);
		tv_item_text = (TextView) view.findViewById(R.id.tv_item_text);
	}
	
	/**
	 * 设置选项卡文字
	 * @param name 选项卡文字名
	 */
	public void setText(CharSequence name){
		tv_item_text.setText(name);
	}
	
	/**
	 * 设置选项卡图片
	 * @param resId 选项卡图片id
	 */
	public void setImageResource(int resId){
		iv_item_img.setImageResource(resId);
	}
	
	/**
	 * 设置选项卡背景图片
	 * @param background 背景图片
	 */
	public void setBackground(Drawable background){
		item_container.setBackground(background);
	}
}
