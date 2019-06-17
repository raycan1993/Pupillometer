package com.example.pupillometer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context){
        super(context, "Pupillometer.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Measurements ( ID INT PRIMARY KEY, Pupil1 DOUBLE, Pupil2 DOUBLE, Difference DOUBLE, Date DATE, Filepath VARCHAR(255));");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS measurement"); // todo??
    }

    /**
     * Inserts a measurements object into the db
     * @param measurement
     * @return
     */
    public boolean insert(Measurement measurement){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues vals = new ContentValues();

        vals.put("ID", measurement.getId());
        vals.put("Pupil1", measurement.getPupilLeft());
        vals.put("Pupil2", measurement.getPupilRight());
        vals.put("Difference", measurement.getDifference());
        vals.put("Date", measurement.getDate());
        vals.put("Filepath", measurement.getFilepath());

        long result = db.insert("Measurements", null, vals);
        return (result > -1);
    }

    /**
     * Deletes a measurement using the id
     * @param ID
     * @return
     */
    public boolean delete(int ID){
        SQLiteDatabase db = this.getWritableDatabase();
        long result = db.delete("Measurements", "ID=?", new String[]{Integer.toString(ID)});
        return (result > -1);
    }

    /**
     * Returns all measurements
     * @return Cursor with all measurements
     */
    public Cursor getData(){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM Measurements;", null);
        return cursor;
    }
}
