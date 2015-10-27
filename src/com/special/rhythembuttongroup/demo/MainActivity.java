package com.special.rhythembuttongroup.demo;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.special.rhythembuttongroup.core.CardBean;
import com.special.rhythembuttongroup.core.RhythmAdapter;
import com.special.rhythembuttongroup.core.RhythmButtonGroup;
import com.special.rhythembuttongroup.core.RhythmButtonGroup.OnGroupStateListener;
import com.special.rhythembuttongroup.core.RhythmButtonGroup.State;
import com.special.rhythmbuttongroup.R;

public class MainActivity extends Activity {

	private State preState;
	
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
		rhythmButtonGroup.setOnGroupStateListener(new OnGroupStateListener() {

			@Override
			public void onStateChanged(State state) {
				if(preState == state){
					return;
				}else{
					preState = state;
					switch (state) {
					case DOWN:
						showToast("button pressed");
						break;
						
					case MOVE:
						showToast("moving~");
						break;
						
					case STAIRING:
						showToast("stairing~~~");
						break;
						
					case UP:
						showToast("button released~~");
						break;
					}
				}
			}
		});

	}
	
	public void showToast(final String str){
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
			}
		});
	}

}
