
// http://www.reigndesign.com/blog/using-your-own-sqlite-database-in-android-applications/

package com.nebadje.dicziunari;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

public class DataBaseHelper extends SQLiteOpenHelper {

	private String mDbName = "vallader.db";
	private SQLiteDatabase myDataBase = null;
	private Context myContext = null;

	/**
	 * Constructor Takes and keeps a reference of the passed context in order to
	 * access to the application assets and resources.
	 * 
	 * @param context
	 */
	public DataBaseHelper(Context context, String dbName) {
		super(context, dbName, null, 1);
		mDbName = dbName;
		myContext = context;
	}

	/**
	 * Creates a empty database on the system and rewrites it with your own
	 * database.
	 * */
	private void createDataBase() throws IOException {

		if (checkDataBase()) {
			// do nothing - database already exist
		} else {
			// By calling this method and empty database will be created into
			// the default system path
			// of your application so we are gonna be able to overwrite that
			// database with our database.
			this.getReadableDatabase();

			try {
				copyDataBase();
			} catch (IOException e) {
				throw new Error("Error copying database");
			}
		}

	}
	private String getDbPath() {
		return myContext.getFilesDir().getPath() + "/" + mDbName;
	}
	/**
	 * Check if the database already exist to avoid re-copying the file each
	 * time you open the application.
	 * 
	 * @return true if it exists, false if it doesn't
	 */
	private boolean checkDataBase() {
		try {
			SQLiteDatabase db = SQLiteDatabase.openDatabase(getDbPath(), null,
					SQLiteDatabase.OPEN_READONLY);
			db.close();
		} catch (SQLiteException e) {
			// database does't exist yet.
			return false;
		}
		return true;
	}

	/**
	 * Copies your database from your local assets-folder to the just created
	 * empty database in the system folder, from where it can be accessed and
	 * handled. This is done by transferring bytestream.
	 * */
	private void copyDataBase() throws IOException {

		// Open your local db as the input stream
		InputStream myInput = myContext.getAssets().open(mDbName);

		// Open the empty db as the output stream
		OutputStream myOutput = new FileOutputStream(getDbPath());

		// transfer bytes from the input file to the output file
		byte[] buffer = new byte[1024];
		int length;
		while ((length = myInput.read(buffer)) > 0) {
			myOutput.write(buffer, 0, length);
		}

		// Close the streams
		myOutput.flush();
		myOutput.close();
		myInput.close();
	}

	public void openDataBase() throws SQLException, Error {

		if (myDataBase == null) {
			try {
				createDataBase();
			} catch (IOException e) {
				throw new Error(String.format("Failed to create %s database.", mDbName));
			}
			// Open the database
			myDataBase = SQLiteDatabase.openDatabase(getDbPath(), null,
					SQLiteDatabase.OPEN_READONLY);
		}
	}

	@Override
	public synchronized void close() {

		if (myDataBase != null) {
			myDataBase.close();
			myDataBase = null;
		}
		super.close();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	public static String wrapInternalLink(String linkClass, String linkPrefix, String item, String node) {
		item = item.replaceAll("\"", "");
		item = item.trim();
		try {
			item = URLEncoder.encode(item, "utf-8");
    		node = String.format("<a class=\"%s\" href=\"%s://%s\">%s</a>", linkClass, linkPrefix, item, node);
		} catch (UnsupportedEncodingException e) {
		}
		return node;
	}
	
	public static String fmt(String out, String[] key, Cursor cursor) {
		String inp = cursor.getString(cursor.getColumnIndex(key[0]));
    	if (inp == null) {
    		return out;
    	}
        inp = inp.trim();
        if (inp.length() > 0) {
        	String fmtStr = " <span class=\"%s\">" + key[1] + "</span> ";
            String txt = String.format(fmtStr, key[0], TextUtils.htmlEncode(inp));
            if (key[0] == "wort" || key[0] == "pled") {
            	if (inp.contains("cf. ")) {
            		// rinviamaint
            		String queryTxt = inp.replaceAll("cf\\.\\s", "");
            		txt = wrapInternalLink("xref", "dcznrxr", queryTxt, txt);
            	} else {
            		txt = wrapInternalLink("clipb", "dcznrcb", inp, txt);
            	}
            }
            return out + txt;
        }
        return out;
    }

	private String[][] keysDe = {
			new String[]{"wort",        "%s" },
			new String[]{"beugung",     "%s" },
			new String[]{"geschlecht", "{%s}"},
			new String[]{"bereich",    "[%s]"},
		};
	private String[][] keysRum = {
			new String[]{"pled",        "%s" },
			new String[]{"gener",      "{%s}"},
			new String[]{"chomp",      "[%s]"},
		};

	public String performQuery(String term) {
		
		String result = "";

		if (term.length() == 0) {
			return "";
		}
		int limit = 100;
		Cursor cursor = myDataBase.query("dicziunari",
				new String[] {
					"wort", "beugung", "geschlecht", "bereich",
					"pled", "gener", "chomp" },
				"wort LIKE ? OR pled LIKE ? LIMIT 0, ?",
				new String[] { "%"+ term +"%", "%"+ term +"%", String.valueOf(limit)},
				null,
				null,
				null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {								
				do {
					String de = "";
					for (int i = 0; i < keysDe.length; i++) {
						de = fmt(de, keysDe[i], cursor);
					}

					String rum = "";
					for (int i = 0; i < keysRum.length; i++) {
						rum = fmt(rum, keysRum[i], cursor);
					}
					
					result += String.format("<tr><td class=\"result\">%s</td>",        de);
					result += String.format(    "<td class=\"result\">%s</td></tr>\n", rum);
				} while (cursor.moveToNext());
			}
		}
				
		return result;
	}
	public ArrayList<String> suggestionsForQuery(String query,
			boolean deutsch,
			int max)
	{
		ArrayList<String> suggestions = new ArrayList<String> ();
		if (query.length() == 0) {
			return suggestions;
		}
		String sql;
		if (deutsch) {
			sql = "wort LIKE ? GROUP BY wort LIMIT 0, ?";
		} else {
			sql = "pled LIKE ? GROUP BY pled LIMIT 0, ?";
		}
		String limit = String.valueOf(max);
		String like = query + "%";
		String group = deutsch ? "wort" : "pled";
		Cursor cursor = myDataBase.query("dicziunari",
				deutsch ? new String[] {"wort"} : new String[] {"pled"},
				sql,
				new String[] {like, limit},
				null,
				null,
				null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {								
				do {
					String word;
					word = cursor.getString(cursor.getColumnIndex(deutsch?"wort":"pled"));
					word = word.trim();					
					if (word.length() > 0) {
						suggestions.add(word);
					}
				} while (cursor.moveToNext());
			}
		}
		return suggestions;
	}

}
