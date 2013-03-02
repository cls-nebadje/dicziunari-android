package com.nebadje.dicziunari;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.database.SQLException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class MainActivity extends Activity {

	public final static String EXTRA_MESSAGE = "com.nebadje.dicziunari.MESSAGE";

	private DataBaseHelper myDbHelper;
	private EditText mEditText;
	private WebView mResultWebView;
	private HtmlRenderer mRenderer;
	private String mLastResult = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		myDbHelper = new DataBaseHelper(this);
		try {
			myDbHelper.createDataBase();
		} catch (IOException ioe) {
			throw new Error("Unable to create database");
		}

		try {
			myDbHelper.openDataBase();
		} catch (SQLException sqle) {
			throw sqle;
		}
		mRenderer = new HtmlRenderer(this);
		
		mResultWebView = (WebView) findViewById(R.id.webview_result);
		mResultWebView.getSettings().setJavaScriptEnabled(true);
		mResultWebView.getSettings().setSupportZoom(true);
		mResultWebView.getSettings().setBuiltInZoomControls(true);
		mResultWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
		mResultWebView.setScrollbarFadingEnabled(true);
		mResultWebView.getSettings().setLoadsImagesAutomatically(true);

		// Load the URLs inside the WebView, not in the external web browser
//		mResultWebView.setWebViewClient(new WebViewClient());
		
		// http://stackoverflow.com/questions/4175398/clear-edittext-on-click
		mEditText = (EditText) findViewById(R.id.edit_message);
		mEditText.setOnTouchListener(new OnTouchListener() {
		    @Override
		    public boolean onTouch(View v, MotionEvent event) {
		    	Drawable x = mEditText.getCompoundDrawables()[2]; 
		        if (x == null) {
		            return false;
		        }
		        if (event.getAction() != MotionEvent.ACTION_UP) {
		            return false;
		        }
		        if (event.getX() > mEditText.getWidth() - mEditText.getPaddingRight() - x.getIntrinsicWidth()) {
		        	mEditText.setText("");
		        	mEditText.setCompoundDrawables(null, null, null, null);
		        }
		        return false;
		    }
		});
		mEditText.addTextChangedListener(new TextWatcher() {
		    @Override
		    public void onTextChanged(CharSequence s, int start, int before, int count) {
		    	Drawable x = null;
		    	if (!mEditText.getText().toString().equals("")) {
		    		x = getResources().getDrawable(R.drawable.search_clear30);
		    		x.setBounds(0, 0, x.getIntrinsicWidth(), x.getIntrinsicHeight());		    	
		    	}
	    		mEditText.setCompoundDrawables(null, null, x, null);
		    }
		    @Override
		    public void afterTextChanged(Editable arg0) {
		    }
		    @Override
		    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		    }
		});		
		mEditText.setImeActionLabel("Chatta", KeyEvent.KEYCODE_ENTER);		
		mEditText.setOnEditorActionListener(new OnEditorActionListener() {
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		    	// http://stackoverflow.com/questions/9327458/get-keycode-0-instead-of-66-when-enter-is-pressed-on-my-computer-keyboard
		        if (actionId == EditorInfo.IME_NULL) {
		    		performSearch();
		            return true;
		        }
		        return false;
		    }
		});

		// Load a page
		if (mLastResult == null) {
			// load default page
			mLastResult = mRenderer.render("");
		}
		mResultWebView.loadDataWithBaseURL("file:///android_res/raw/",
				mLastResult,
				"text/html",
                "UTF-8", null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void performSearch() {

		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);

		String query = mEditText.getText().toString();
		
		// Spawn background thread which posts result...
		String result = myDbHelper.performQuery(query);

		mLastResult = mRenderer.render(result);
		mResultWebView.loadDataWithBaseURL("file:///android_res/raw/",
				mLastResult, "text/html", "UTF-8", null);
	}

	/** Called when the user clicks the Send button */
	public void sendMessage(View view) {
		performSearch();
	}

	// @todo http://www.devahead.com/blog/2012/01/preserving-the-state-of-an-android-webview-on-screen-orientation-change/
}
