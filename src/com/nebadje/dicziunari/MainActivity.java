package com.nebadje.dicziunari;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class MainActivity extends Activity {

	public final static String EXTRA_MESSAGE = "com.nebadje.dicziunari.MESSAGE";

	public enum Idiom {Vallader, Puter};
	
	private DataBaseHelper mDbHelperVallader;
	private DataBaseHelper mDbHelperPuter;
	private Idiom mIdiom;
	private EditText mEditText;
	private WebView mResultWebView;
	private HtmlRenderer mRenderer;
	private String mLastResult = null;

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mDbHelperVallader = new DataBaseHelper(this, "vallader.db");
		mDbHelperPuter = new DataBaseHelper(this, "puter.db");
		try {
			mDbHelperVallader.openDataBase();
			mDbHelperPuter.openDataBase();
		} catch (SQLException sqle) {
			throw sqle;
		}
		mIdiom = Idiom.Puter;
		mIdiom = Idiom.Vallader;
		
		mRenderer = new HtmlRenderer(this);
		
		mResultWebView = (WebView) findViewById(R.id.webview_result);
		mResultWebView.getSettings().setJavaScriptEnabled(true);
		mResultWebView.getSettings().setSupportZoom(true);
		mResultWebView.getSettings().setBuiltInZoomControls(true);
		mResultWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
		mResultWebView.setScrollbarFadingEnabled(true);
		mResultWebView.getSettings().setLoadsImagesAutomatically(true);

		mResultWebView.setWebViewClient(new WebViewClient() {
			public boolean shouldOverrideUrlLoading (WebView view, String url) {
				String dicziunariClipboardTag = "dcznrcb://";
				String dicziunariXrefTag = "dcznrxr://";
				if (url.startsWith(dicziunariClipboardTag)) {
					String word = url.substring(dicziunariClipboardTag.length());
					try {
						word = URLDecoder.decode(word, "utf-8");
						word = word.trim();
						if (word.length() > 0) {
							ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
							ClipData clip = ClipData.newPlainText("Dicziunari", word);
							clipboard.setPrimaryClip(clip);
						}
					} catch (UnsupportedEncodingException e) {
					}
				} else if (url.startsWith(dicziunariXrefTag)) {
					String word = url.substring(dicziunariXrefTag.length());
					try {
						word = URLDecoder.decode(word, "utf-8");
						word = word.trim();
						if (word.length() > 0) {
							mEditText.setText(word, TextView.BufferType.EDITABLE);
							search(word);
						}
					} catch (UnsupportedEncodingException e) {
					}
				}
				return true;
			}
			
		});
		
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
		        	Editable e = mEditText.getText();
		        	if (e.length() == 0) {
		        		startSpeechRecognition();
		        	} else {
			        	mEditText.setText("");
		        	}
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
		    	} else {
		    		x = getResources().getDrawable(R.drawable.search_speech30);
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
		Drawable x = getResources().getDrawable(R.drawable.search_speech30);
		x.setBounds(0, 0, x.getIntrinsicWidth(), x.getIntrinsicHeight());
		mEditText.setCompoundDrawables(null, null, x, null);
		
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

	protected void startSpeechRecognition()
	{
	    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
	    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
	            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
	    //... put other settings in the Intent 
	    startActivityForResult(intent, 666);		
	}
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
		if (requestCode == 666 && resultCode == RESULT_OK) {
			ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			if (results.size() > 0) {
				mEditText.setText(results.get(0));
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
    }
    
	public void performSearch() {

		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
		
		String query = mEditText.getText().toString();
		search(query);
	}
	public void search(String query) {
		// Spawn background thread which posts result...
		String result;
		if (mIdiom == Idiom.Vallader) {
			result = mDbHelperVallader.performQuery(query);
		} else {
			result = mDbHelperPuter.performQuery(query);
		}
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
