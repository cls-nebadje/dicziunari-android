package com.nebadje.dicziunari;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.database.SQLException;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
//		 DataBaseHelper myDbHelper = new DataBaseHelper();
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
		
		mEditText = (EditText) findViewById(R.id.edit_message);
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
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	
	public void performSearch() {
		
    	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);

		
		String message = mEditText.getText().toString();
		
		message = myDbHelper.performQuery(message);

		
		WebView resultWebView = (WebView) findViewById(R.id.webview_result);
		resultWebView.getSettings().setJavaScriptEnabled(true);		
		resultWebView.loadDataWithBaseURL("file:///android_res/raw/",
				"<html><body>" + message + "</body></html>",
				"text/html",
                "UTF-8", null);
    }
	
	/** Called when the user clicks the Send button */
	public void sendMessage(View view) {
	    // Do something in response to button
//		Intent intent = new Intent(this, DisplayMessageActivity.class);
//		EditText editText = (EditText) findViewById(R.id.edit_message);
//		String message = editText.getText().toString();
//		intent.putExtra(EXTRA_MESSAGE, message);
//		startActivity(intent);
		
		performSearch();
	}
}
