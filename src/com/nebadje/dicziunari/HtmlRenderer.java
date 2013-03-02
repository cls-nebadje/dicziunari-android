package com.nebadje.dicziunari;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;

public class HtmlRenderer {
	private String mHtmlTemplate = null;
	private Context mContext;
	
	public HtmlRenderer(Context context) {

		this.mContext = context;
		loadTemplate();

	}
	
	private void loadTemplate() {
		if (mHtmlTemplate != null) {
			return;
		}

		AssetManager am = mContext.getAssets();
		try {
			InputStream is = am.open("result_template.html");
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			int i = is.read();
			while (i != -1) {
				os.write(i);
				i = is.read();
			}
			is.close();
			mHtmlTemplate = os.toString();
		} catch (IOException e) {
			e.printStackTrace();
			mHtmlTemplate = "<html><body> $RESULT </body></html>";
		}
	}

	public String render(String text) {
		
		if (text.length() == 0) {
			return mHtmlTemplate.replaceAll("\\$RESULT", "Ingüns resultats.");
		} 
		
		String html = String.format("<table class=\"result\">\n%s</table>\n", text);

		return mHtmlTemplate.replaceAll("\\$RESULT", html);
	}
}
