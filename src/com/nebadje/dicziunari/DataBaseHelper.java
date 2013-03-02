
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

	// The Android's default system path of your application database.
	// Description	Resource	Path	Location	Type

	private static String DB_NAME = "vallader.db";

	private SQLiteDatabase myDataBase;

	private final Context myContext;

	/**
	 * Constructor Takes and keeps a reference of the passed context in order to
	 * access to the application assets and resources.
	 * 
	 * @param context
	 */
	public DataBaseHelper(Context context) {

		super(context, DB_NAME, null, 1);
		this.myContext = context;
	}

	/**
	 * Creates a empty database on the system and rewrites it with your own
	 * database.
	 * */
	public void createDataBase() throws IOException {

		boolean dbExist = checkDataBase();

		if (dbExist) {
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
		return myContext.getFilesDir().getPath() + "/" + DB_NAME;
	}
	/**
	 * Check if the database already exist to avoid re-copying the file each
	 * time you open the application.
	 * 
	 * @return true if it exists, false if it doesn't
	 */
	private boolean checkDataBase() {

		SQLiteDatabase checkDB = null;

		try {
			checkDB = SQLiteDatabase.openDatabase(getDbPath(), null,
					SQLiteDatabase.OPEN_READONLY);
		} catch (SQLiteException e) {
			// database does't exist yet.
		}

		if (checkDB != null) {
			checkDB.close();
		}

		return checkDB != null ? true : false;
	}

	/**
	 * Copies your database from your local assets-folder to the just created
	 * empty database in the system folder, from where it can be accessed and
	 * handled. This is done by transfering bytestream.
	 * */
	private void copyDataBase() throws IOException {

		// Open your local db as the input stream
		InputStream myInput = myContext.getAssets().open(DB_NAME);

		// Open the empty db as the output stream
		OutputStream myOutput = new FileOutputStream(getDbPath());

		// transfer bytes from the inputfile to the outputfile
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

	public void openDataBase() throws SQLException {

		// Open the database
		myDataBase = SQLiteDatabase.openDatabase(getDbPath(), null,
				SQLiteDatabase.OPEN_READONLY);

	}

	@Override
	public synchronized void close() {

		if (myDataBase != null)
			myDataBase.close();

		super.close();

	}

	@Override
	public void onCreate(SQLiteDatabase db) {

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	public String performQuery(String term) {
		
		String result = "<table>\n";
		
		Cursor cursor = myDataBase.query("dicziunari",
				new String[] { "m", "n" },
				"m like ? OR n like ?",
				new String[] { "%"+ term +"%", "%"+ term +"%"},
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
					
					result += String.format("<tr><td>%s</td><td>%s</td>\n",
							TextUtils.htmlEncode(wort),
							TextUtils.htmlEncode(pled));					
				} while (cursor.moveToNext());
			}
		}
		
		result += "</table>\n";

//		if (c != null) {
//			if (c.moveToFirst()) {
//				do {
//					String firstName = c.getString(c
//							.getColumnIndex("FirstName"));
//					int age = c.getInt(c.getColumnIndex("Age"));
//					results.add("" + firstName + ",Age: " + age);
//				} while (c.moveToNext());
//			}
//		}
		
		return result;
	}


}