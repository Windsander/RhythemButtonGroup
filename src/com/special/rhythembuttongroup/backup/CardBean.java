package com.special.rhythembuttongroup.backup;

import com.special.rhythmbuttongroup.R;

public class CardBean {
	private int drawId = R.drawable.ic_launcher;
	private String tab;
	
	public int getId() {
		return drawId;
	}
	public void setId(int drawId) {
		this.drawId = drawId;
	}
	public String getTab() {
		return tab;
	}
	public void setTab(String tab) {
		this.tab = tab;
	}
	
	public CardBean(int drawId, String tab) {
		super();
		this.drawId = drawId;
		this.tab = tab;
	}
	
}
