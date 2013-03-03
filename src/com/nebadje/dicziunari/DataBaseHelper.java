
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

	public String performQuery(String term) {
		
		String result = "";
		
/*
 *                            # de, beugungen, geschl, bereich 
                                            # rum, geschl, bereich.
    cursor.execute("SELECT m, tt, ww, ii,   n, ll, rr FROM dicziunari WHERE m LIKE ? OR n LIKE ? LIMIT 0, ?",
                   ("%%%s%%" % query, "%%%s%%" % query, limit))
    rows = cursor.fetchall()
    res = []
    for row in rows:
        keys = [(u'wort',       u'',  u'' ),
                (u'beugung',    u'',  u'' ),
                (u'geschlecht', u'{', u'}'),
                (u'bereich',    u'[', u']'),
                (u'pled',       u'',  u'' ),
                (u'gener',      u'{', u'}'),
                (u'chomp',      u'[', u']'),]
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
                    
        de = []
        rum = []
        for i in range(4):
            fmt(de, keys[i], row[i])
        for i in range(4, len(row)):
            fmt(rum, keys[i], row[i])
        res.append((" ".join(de), " ".join(rum)))



			    <table class="result">
			    <tr><th class="result">Tudais-ch</th><th class="result">{{ idiom }}</th></tr>
				{% for tud, rum in result %}
					<tr><td class="result">{{tud|safe}}</td><td class="result">{{rum|safe}}<td>
				{% endfor %}
			    </table>
 */
		if (term.length() == 0) {
			return "";
		}
		int limit = 50;
		Cursor cursor = myDataBase.query("dicziunari",
				new String[] { "m", "n" },
				"m like ? OR n like ? LIMIT 0, ?",
				new String[] { "%"+ term +"%", "%"+ term +"%", String.valueOf(limit)},
				null,
				null,
				null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				int wortIdx = cursor.getColumnIndex("m");
				int pledIdx = cursor.getColumnIndex("n");
				do {
					String wort = cursor.getString(wortIdx);
					String pled = cursor.getString(pledIdx);
					
					result += String.format("<tr><td class=\"result\">%s</td><td class=\"result\">%s</td>\n",
							TextUtils.htmlEncode(wort),
							TextUtils.htmlEncode(pled));					
				} while (cursor.moveToNext());
			}
		}
				
		return result;
	}
}
