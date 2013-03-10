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
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

public class MainActivity extends Activity {

	public final static String EXTRA_MESSAGE = "com.nebadje.dicziunari.MESSAGE";

	public enum Idiom {Vallader, Puter};
	
	private DataBaseHelper mDbHelperVallader;
	private DataBaseHelper mDbHelperPuter;
	private Idiom mIdiom;
	private EditTextSearch mEditText;
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
		
		mEditText = (EditTextSearch) findViewById(R.id.edit_message);
		mEditText.setHandler(new EditTextSearchHandler() {
			void onSpeechRecognition() {
				startSpeechRecognition();
			}
			void onKeyboardEnter() {
				performSearch();
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
