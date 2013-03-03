
// http://www.reigndesign.com/blog/using-your-own-sqlite-database-in-android-applications/

package com.nebadje.dicziunari;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

	/*
    def fmt(out, key, inp):
        inp = inp.strip()
        if inp is not None and len(inp) > 0:
            txt = u'<span class="%s">%s%s%s</span>' % (key[0], key[1], escape(inp), key[2])
            if key[0] in [u'wort', u'pled']:
                # TODO regex "cf. auch: vergleichen" (low prio - only 5 words)
                p = inp.find(u'cf. ')
                if p != -1:
                    newTerm = inp[p+3:].strip()
                    # We should generate the relative path by our app url part
                    # and not hard-code it here
                    txt = '<a class="xref" href="/tschercha/?idiom=%s&term=%s">%s</a>' % \
                           (IDIOM_NAMES[idiom], urlquote(newTerm), txt)
                else:
                    cbTxt = inp.replace("\"", "")
                    txt = '<a class="clipb" href=\'javascript:clipb("%s")\'>%s</a>' % (cbTxt, txt)
            out.append(txt)

	 */
	private static String fmt(String out, String[] key, Cursor cursor) {
		String inp = cursor.getString(cursor.getColumnIndex(key[0]));
    	if (inp == null) {
    		return out;
    	}
        inp = inp.trim();
        if (inp.length() > 0) {
        	String fmtStr = " <span class=\"%s\">" + key[1] + "</span> ";
            String txt = String.format(fmtStr, key[0], TextUtils.htmlEncode(inp));
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
		int limit = 50;
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
}
