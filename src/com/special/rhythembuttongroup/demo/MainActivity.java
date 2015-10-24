package com.special.rhythembuttongroup.demo;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;

import com.special.rhythembuttongroup.core.CardBean;
import com.special.rhythembuttongroup.core.RhythmAdapter;
import com.special.rhythembuttongroup.core.RhythmButtonGroup;
import com.special.rhythmbuttongroup.R;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		List<CardBean> list = new ArrayList<CardBean>();
		for (int i = 0; i < 30; i++) {
			CardBean card = new CardBean(R.drawable.ic_launcher, "");
			list.add(card);
		}
		
		RhythmButtonGroup rhythmButtonGroup = (RhythmButtonGroup) findViewById(R.id.rbg);
		RhythmAdapter adapter = new RhythmAdapter(getApplicationContext(), list);
		rhythmButtonGroup.setAdapter(adapter);
		
	}

}
