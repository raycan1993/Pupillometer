package com.example.pupillometer;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    private PupilDetector pupilDetector;

    private Measurement currentMeasurement;

    private DatabaseHelper db;

    private ConstraintLayout mainLayout;
    private ConstraintLayout detailViewLayout;
    private ConstraintLayout resultLayout;
    private ConstraintLayout instructionLayout;

    private Button uploadImgBtn, startMeasureBtn, deleteMeasurementBtn, saveMeasureBtn, closeDetailViewBtn, confirmBtn;
    private TextView textLeftPupil, textRightPupil, textDifference,
        textLPDetail, textRPDetail, textDiffDetail, textDateDetail, textFileDetail, textResult0, textResult1;
    private PhotoView detailViewImg, resultImg;

    // list view variables
    private ListView listView;
    private ArrayList<Measurement> listMeasurements;
    private MyListAdapter listAdapter;

    private int eyeColorSelection;

    // drop down variables
    private Spinner eyeColorSpinner;
    ArrayAdapter<CharSequence> eyeColorAdapter;

    // vibration feature
    private Vibrator goodVibes;

    private boolean startingActivity = false;


    private static int REQUEST_READ_EXTERNAL_STORAGE = 0;
    private boolean read_external_storage_granted = false;
    private static int REQUEST_WRITE_EXTERNAL_STORAGE = 0;
    private boolean write_external_storage_granted = false;
    private static int REQUEST_CAMERA = 0;
    private boolean camera_permission_granted = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPermissions();

        pupilDetector = new PupilDetector(this);

        db = new DatabaseHelper(this);
        listMeasurements = getMeasurements();

        goodVibes = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Initialize layout elements
        // Layouts
        mainLayout = findViewById(R.id.MainLayout);
        detailViewLayout = findViewById(R.id.DetailViewLayout);
        resultLayout = findViewById(R.id.ResultLayout);
        instructionLayout = findViewById(R.id.InstructionLayout);

        // Buttons
        uploadImgBtn = findViewById(R.id.uploadImgBtn);
        uploadImgBtn.setOnClickListener(this);
        startMeasureBtn = findViewById(R.id.startMeasurementBtn);
        startMeasureBtn.setOnClickListener(this);
        deleteMeasurementBtn = findViewById(R.id.deleteMeasurementBtn);
        deleteMeasurementBtn.setOnClickListener(this);
        saveMeasureBtn = findViewById(R.id.saveMeasurementBtn);
        saveMeasureBtn.setOnClickListener(this);
        closeDetailViewBtn = findViewById(R.id.closeDetailViewBtn);
        closeDetailViewBtn.setOnClickListener(this);
        confirmBtn = findViewById(R.id.confirmBtn);
        confirmBtn.setOnClickListener(this);

        // TextViews
        textLeftPupil = findViewById(R.id.textLeftpupil);
        textRightPupil = findViewById(R.id.textRightPupil);
        textDifference = findViewById(R.id.textDiff);
        textLPDetail = findViewById(R.id.textLeftpupil_detail);
        textRPDetail = findViewById(R.id.textRightPupil_detail);
        textDiffDetail = findViewById(R.id.textDiff_detail);
        textDateDetail = findViewById(R.id.date_detail);
        textFileDetail = findViewById(R.id.filename_detail);
        textResult0 = findViewById(R.id.text_result_0);
        textResult1 = findViewById(R.id.text_result_1);

        // ListView
        listView = findViewById(R.id.listView);
        listAdapter = new MyListAdapter(listMeasurements, db, this);
        listView.setAdapter(listAdapter);

        // Spinner
        eyeColorSpinner = findViewById(R.id.eyeColorSpinner);
        eyeColorAdapter = ArrayAdapter.createFromResource(this,
                R.array.eye_color_array, android.R.layout.simple_spinner_item);
        eyeColorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        eyeColorSpinner.setAdapter(eyeColorAdapter);
        eyeColorSpinner.setOnItemSelectedListener(this);

        // PhotoViews
        detailViewImg = findViewById(R.id.detailViewImage);
        resultImg = findViewById(R.id.resultImage);


        Intent intent = getIntent();
        Bundle bundle = getIntent().getExtras();
        if(bundle != null){
            String filepath = bundle.getString("FILEPATH");
            eyeColorSelection = bundle.getInt("EYE_COLOR");
            currentMeasurement = pupilDetector.doMeasurement(filepath, eyeColorSelection);
            eyeColorSpinner.setSelection(eyeColorSelection);
            displayProcessedImage(currentMeasurement);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.startMeasurementBtn: {
                startCameraActivity();
            }
            break;
            case R.id.deleteMeasurementBtn: {
                AlertDialog.Builder b = new AlertDialog.Builder(view.getContext());
                b.setTitle("Wollen Sie die Messung wirklich verwerfen?");
                b.setPositiveButton("JA", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        hideLayout(resultLayout);
                        Toast.makeText(getApplicationContext(), "Die Messung wurde verworfen.",  Toast.LENGTH_SHORT).show();
                    }
                });
                b.setNegativeButton("NEIN", null);
                b.show();
            }
            break;
            case R.id.uploadImgBtn: {
                Intent uploadImageIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://media/internal/images/media"));
                startActivityForResult(uploadImageIntent, 0);
            }
            break;
            case R.id.saveMeasurementBtn: {
                saveMeasurement(currentMeasurement);
                hideLayout(resultLayout);
            }
            break;
            case R.id.closeDetailViewBtn: {
                hideLayout(detailViewLayout);
            }
            case R.id.confirmBtn:{
                hideLayout(instructionLayout);
            }
            break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            hideLayout(detailViewLayout);
        }
        return true;
    }

    /**
     * Hides a hidden constriant layout with animaiton
     * @param layout
     */
    private void hideLayout(final ConstraintLayout layout){
        if (layout.getVisibility() == View.VISIBLE) {
            layout.animate()
                    .alpha(0.0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            layout.setVisibility(View.GONE);
                        }
                    });
        }
    }

    /**
     * Displays a hidden constriant layout with animaiton
     * @param layout
     */
    public void showLayout(final ConstraintLayout layout){
        if (layout.getVisibility() == View.GONE) {
            layout.setVisibility(View.VISIBLE);
            layout.setAlpha(0.0f);
            layout.animate()
                    .alpha(1.0f)
                    .setListener(null);
        }
    }

    /**
     * OPTIONS MENU ================================================================================
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_options, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.instructions) {
            showLayout(instructionLayout);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * EYECOLOR DROPDOWN ============================================================================
     */
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        parent.getItemAtPosition(pos);
        eyeColorSelection = pos;
    }

    public void onNothingSelected(AdapterView<?> parent) {
    }

    /**
     * LIST VIEW ====================================================================================
     *
     * Gets all measurements from db and returns them as ArrayList
     * @return measurements array list
     */
    private ArrayList<Measurement> getMeasurements() {
        ArrayList<Measurement> measurements = new ArrayList<>();
        Cursor cursor = db.getData();

        if (cursor.getCount() == 0) {
            Toast.makeText(this, "Es wurden noch keine Messungen gemacht.", Toast.LENGTH_SHORT).show();
        } else {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex("ID"));
                double pupil1 = cursor.getDouble(cursor.getColumnIndex("Pupil1"));
                double pupil2 = cursor.getDouble(cursor.getColumnIndex("Pupil2"));
                double difference = cursor.getDouble(cursor.getColumnIndex("Difference"));
                String date = cursor.getString(cursor.getColumnIndex("Date"));
                String filepath = cursor.getString(cursor.getColumnIndex("Filepath"));

                measurements.add(new Measurement(id, pupil1, pupil2, difference, date, filepath));
            }
        }
        Collections.reverse(measurements);
        return measurements;
    }

    /**
     * ListAdapter class which handles list items
     */
    private class MyListAdapter extends BaseAdapter {

        private ArrayList<Measurement> measurements;
        private DatabaseHelper db;
        private Context context;

        private TextView txtPupil1, txtPupil2, txtDiff, txtTimestamp, txtFilename;
        private Button viewBtn, deleteBtn;

        public MyListAdapter(ArrayList<Measurement> measurements, DatabaseHelper db, Context context) {
            this.measurements = measurements;
            this.db = db;
            this.context = context;
        }

        public void updateList(ArrayList<Measurement> measurements) {
            this.measurements = measurements;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return measurements.size();
        }

        @Override
        public Object getItem(int i) {
            return measurements.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        /**
         * Displays information to the detail view
         * @param measurement
         */
        private void displayDetailMeasurement(Measurement measurement){
            goodVibes.vibrate(60);

            String filepath = measurement.getFilepath();
            Bitmap bitmap = translateImgToBitmap(filepath);
            double difference_double = measurement.getDifference();
            DecimalFormat df = new DecimalFormat("#0.000");
            String leftPupil = df.format(measurement.getPupilLeft()) + "mm";
            String rightPupil = df.format(measurement.getPupilRight()) + "mm";
            String difference = df.format(measurement.getDifference()) + "mm";
            String filename = measurement.getFilepath().substring(measurement.getFilepath().lastIndexOf("/")+1);
            DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            Date date = new Date(measurement.getDate());
            String date_string = dateFormat.format(date).toString();

            textLPDetail.setText(leftPupil);
            textRPDetail.setText(rightPupil);
            textDiffDetail.setText(difference);
            textFileDetail.setText(filename);
            textDateDetail.setText(date_string);

            detailViewImg.setImageBitmap(bitmap);

            if(difference_double > 0.0f) textResult0.setText("L > R");
            if(difference_double < 0.0f) textResult0.setText("L < R");
            if(difference_double == 0.0f) textResult0.setText("L = R");

            showLayout(detailViewLayout);
        }

        /**
         * Displays information of measurement to a list view item
         * @param view
         * @param measurement
         */
        private void displayMeasurementItem(View view, Measurement measurement){
            txtPupil1 = view.findViewById(R.id.pupil1);
            txtPupil2 = view.findViewById(R.id.pupil2);
            txtDiff = view.findViewById(R.id.difference);
            txtTimestamp = view.findViewById(R.id.timestamp);
            txtFilename = view.findViewById(R.id.filename);
            viewBtn = view.findViewById(R.id.viewBtn);

            DecimalFormat df = new DecimalFormat("#0.000");
            String leftPupil = df.format(measurement.getPupilLeft()) + "mm";
            String rightPupil = df.format(measurement.getPupilRight()) + "mm";
            String difference = df.format(measurement.getDifference()) + "mm";
            String filename = measurement.getFilepath().substring(measurement.getFilepath().lastIndexOf("/")+1);
            DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            Date date = new Date(measurement.getDate());
            String date_string = dateFormat.format(date).toString();

            txtPupil1.setText(leftPupil);
            txtPupil2.setText(rightPupil);
            txtDiff.setText(difference);
            txtFilename.setText(filename);
            txtTimestamp.setText(date_string);
        }

        /**
         * Deletes item from list view with the corresponding measurement
         * @param position
         * @param measurement
         */
        private void deleteMeasurement(int position, Measurement measurement){
            measurements.remove(position);
            notifyDataSetChanged();
            db.delete(measurement.getId());
            Toast.makeText(getApplicationContext(), "Die Messung wurde gelöscht.",  Toast.LENGTH_SHORT).show();
        }


        @Override
        public View getView(final int position, View view, ViewGroup viewGroup) {
            LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
            view = inflater.inflate(R.layout.measurement_list_item, null);

            final Measurement itemMeasurement = measurements.get(position);

            displayMeasurementItem(view, itemMeasurement);

            // REGULAR PRESS
            viewBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    displayDetailMeasurement(itemMeasurement);
                }
            });

            // LONG PRESS
            viewBtn.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    goodVibes.vibrate(60);

                    AlertDialog.Builder b = new AlertDialog.Builder(view.getContext());

                    DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                    Date date = new Date(itemMeasurement.getDate());
                    String date_str = dateFormat.format(date).toString();

                    b.setTitle("Wollen Sie die Messung vom " + date_str + " wirklich löschen?");
                    b.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            deleteMeasurement(position, itemMeasurement);
                        }
                    }).setNegativeButton("Nein", null);
                    b.show();

                    return true;
                }
            });
            return view;
        }
    }

    /**
     * Handles selected image from image picker and processes it
     * @param requestCode
     * @param resultCode
     * @param data
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // sanity check and see whether the result is coming from the appropriate intent
        if (requestCode == 0 && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();

            // we fetch the column index of the selected image {MediaStore.Images.Media.DATA}
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            // To get the exact path of the image, we make use of the Cursor class
            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            // get measurement
            currentMeasurement = pupilDetector.doMeasurement(picturePath, eyeColorSelection);
            displayProcessedImage(currentMeasurement);
        }
    }

    /**
     * Activity functions  ==================================================================================
     *
     * Stats the CameraActivity and passes the eye color
     */
    private void startCameraActivity(){
        if (checkPermissions() && !startingActivity) {
            startingActivity = true;
            goodVibes.vibrate(200);
            Intent intent = new Intent(this, CameraActivity.class);
            intent.putExtra("EYE_COLOR", eyeColorSelection);
            startActivity(intent);
        }else{
            getPermissions();
        }
    }


    /**
     *  Measurement functions  ==================================================================================
     *
     * Takes a single measurement and displays it onto the result layout
     * @param measurement
     */
    private void displayProcessedImage(Measurement measurement) {
        DecimalFormat df = new DecimalFormat("#0.000");
        String pupilL_str = df.format(measurement.getPupilLeft()) + "mm";
        String pupilR_str = df.format(measurement.getPupilRight()) + "mm";
        String diff_str = df.format(measurement.getDifference()) + "mm";
        double difference = measurement.getDifference();
        String filepath = measurement.getFilepath();
        Bitmap imgBitmap = translateImgToBitmap(filepath);

        textLeftPupil.setText(pupilL_str);
        textRightPupil.setText(pupilR_str);
        textDifference.setText(diff_str);
        if(imgBitmap != null) resultImg.setImageBitmap(imgBitmap);

        if(difference > 0.0f) textResult1.setText("L > R");
        if(difference < 0.0f) textResult1.setText("L < R");
        if(difference == 0.0f) textResult1.setText("L = R");

        showLayout(resultLayout);
    }

    /**
     * Saves measurement into the database
     * @param measurement
     */
    private void saveMeasurement(Measurement measurement){
        if(db.insert(measurement)){
            listMeasurements.add(0, measurement);
            listAdapter.updateList(listMeasurements);
            Toast.makeText(this, "Die Messung wurde gespeichert.", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "Die Messung konnte nicht gespeichert werden.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Decodes image file into a bitmap
     * @param filepath
     * @return Bitmap of inputted image file
     */
    private Bitmap translateImgToBitmap(String filepath){
        File imgFile = new File(filepath);

        if (imgFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

            float imgScale = (float) bitmap.getHeight() / 1620;
            bitmap = Bitmap.createScaledBitmap(bitmap,
                    Math.round(bitmap.getWidth() / imgScale),
                    Math.round(bitmap.getHeight() / imgScale), true);

            return bitmap;

        } else {
            Toast.makeText(getApplicationContext(), "Die Datei wurde nicht gefunden.", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    /**
     *  Permission functions ----------------------------------------------------------
     *
     * Checks if storage and camera permissions were accepted
     * @return boolean whether permissions were accepted
     */
    private boolean checkPermissions() {
        if (!read_external_storage_granted ||
                !write_external_storage_granted ||
                !write_external_storage_granted) {
            Toast.makeText(getApplicationContext(), "Die App braucht Zugang zur Kamera und zum Speicher.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * Requests permission for READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE & CAMERA
     */
    private void getPermissions(){
        // Check READ storage permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("STORAGE PERMISSION", "request READ_EXTERNAL_STORAGE");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            Log.i("STORAGE PERMISSION", "READ_EXTERNAL_STORAGE already granted");
            read_external_storage_granted = true;
        }

        // Check WRITE storage permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("STORAGE PERMISSION", "request WRITE_EXTERNAL_STORAGE");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            Log.i("STORAGE PERMISSION", "WRITE_EXTERNAL_STORAGE already granted");
            write_external_storage_granted = true;
        }

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            Log.i("CAMERA PERMISSION", "request CAMERA permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA);
        } else {
            Log.i("CAMERA PERMISSION", "CAMERA permission already granted");
            camera_permission_granted = true;
        }

    }
}
