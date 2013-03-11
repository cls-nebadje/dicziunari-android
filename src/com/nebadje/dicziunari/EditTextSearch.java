package com.nebadje.dicziunari;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

public class EditTextSearch extends AutoCompleteTextView
{	
	private EditTextSearchHandler mHandler = null;
	
    public EditTextSearch(Context context)
    {
        super(context);
        commonInit();
    }

    public EditTextSearch(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        commonInit();
    }

    public EditTextSearch(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        commonInit();
    }
    
    private void commonInit()
    {
		// http://stackoverflow.com/questions/4175398/clear-edittext-on-click
		setOnTouchListener(new OnTouchListener() {
		    @Override
		    public boolean onTouch(View v, MotionEvent event) {
		    	
		    	Drawable x = getCompoundDrawables()[2]; 
		        if (x == null) {
		            return false;
		        }
		        if (event.getAction() != MotionEvent.ACTION_UP) {
		            return false;
		        }
		        if (event.getX() > getWidth() - getPaddingRight() - x.getIntrinsicWidth()) {
		        	Editable e = getText();
		        	if (e.length() == 0) {
		        		if (mHandler != null) {
		        			mHandler.onSpeechRecognition();
		        		}
		        	} else {
			        	setText("");
		        	}
		        }
		        return false;
		    }
		});
		
		addTextChangedListener(new TextWatcher() {
		    @Override
		    public void onTextChanged(CharSequence s, int start, int before, int count) {
		    	updateCompoundDrawables();
		    }
		    @Override
		    public void afterTextChanged(Editable arg0) {
		    	if (arg0.length() > 0 && mHandler != null) {
		    		mHandler.onTextChange();
		    	}
		    }
		    @Override
		    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		    }
		});
		updateCompoundDrawables();
		
		setImeActionLabel("Chatta", KeyEvent.KEYCODE_ENTER);		
		setOnEditorActionListener(new OnEditorActionListener() {
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		    	// http://stackoverflow.com/questions/9327458/get-keycode-0-instead-of-66-when-enter-is-pressed-on-my-computer-keyboard
		        if (actionId == EditorInfo.IME_NULL) {
		    		if (mHandler != null) {
		    			mHandler.onKeyboardEnter();
		    		}
		            return true;
		        }
		        return false;
		    }
		});
		
		setOnItemClickListener(new OnItemClickListener() {

	        @Override
	        public void onItemClick(AdapterView<?> parent, View arg1, int pos,
	                long id) {
	    		if (mHandler != null) {
	    			mHandler.onKeyboardEnter();
	    		}
	        }
	    });
    }
    private void updateCompoundDrawables()
    {
    	Drawable x = null;
    	if (!getText().toString().equals("")) {
    		x = getResources().getDrawable(R.drawable.search_clear30);
    		x.setBounds(0, 0, x.getIntrinsicWidth(), x.getIntrinsicHeight());		    	
    	} else {
    		x = getResources().getDrawable(R.drawable.search_speech30);
    		x.setBounds(0, 0, x.getIntrinsicWidth(), x.getIntrinsicHeight());		    	
    	}
		setCompoundDrawables(null, null, x, null);    	
    }
    public void setHandler(EditTextSearchHandler handler)
    {
    	mHandler = handler;
    }
    public void setSuggestions(ArrayList<String> suggestions)
    {
		ArrayAdapter<String> a = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_dropdown_item_1line, suggestions);
		setAdapter(a);
		setThreshold(2);
		a.notifyDataSetChanged();
    }
}
