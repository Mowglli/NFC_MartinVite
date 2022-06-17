package com.example.nfc_martin;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity {
    public static final String ERROR_DETECTED = "No NFC tag detected!";
    public static final String WRITE_SUCCESS = "Text written to the NFC tag successfully!";
    public static final String WRITE_ERROR = "Error during writing, is the NFC tag close enough to your device?";

    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];
    boolean writeMode;
    Tag myTag;
    Context context;
    GifImageView guide;
    TextView tvNFCContent,tvNFCContent1, txt_dmxadress, txt_mode, txt_udlæs,txt_opsæt;
    TextView message, message2;
    Button btnWrite;
    ProgressBar progressBar;
    String set1;
    String set2;
    AlertDialog dialog;

    private boolean writePressed = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        context = this;
        setupUI();


        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (!nfcAdapter.isEnabled()) {
            AlertDialog.Builder alertbox = new AlertDialog.Builder(context);
            alertbox.setTitle("NFC er ikke aktiveret på enheden");
            alertbox.setMessage("Tryk på Tænd, for at aktivere NFC på enheden");
            alertbox.setPositiveButton("Tænd", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                        startActivity(intent);
                    }
                }
            });
            alertbox.setNegativeButton("Annuller", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            alertbox.show();

        }
        if (nfcAdapter == null) {
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
        }

        readFromIntent(getIntent());
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[] { tagDetected };
    }

    private void setupUI() {
        guide = findViewById(R.id.guide);
        tvNFCContent = (TextView) findViewById(R.id.txt_read1);
        tvNFCContent1 = (TextView) findViewById(R.id.txt_read2);
        txt_dmxadress = (TextView) findViewById(R.id.txt_dmxadress);
        txt_mode = (TextView) findViewById(R.id.txt_dmxmode);
        txt_opsæt = (TextView) findViewById(R.id.txt_opsæt);
        txt_udlæs = (TextView) findViewById(R.id.txt_udlæs);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        message = (TextView) findViewById(R.id.edt_write);
        message2 = (TextView) findViewById(R.id.edt_write2);
        message2.setVisibility(View.INVISIBLE);
        btnWrite = (Button) findViewById(R.id.btn_write);
        Spinner dropdown = findViewById(R.id.spinner1);
        String[] items = new String[]{"STD", "EXT"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        dropdown.setAdapter(adapter);
        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(items[i] == "STD"){
                    message2.setText("1");
                }
                else if(items[i] == "EXT"){
                    message2.setText("2");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        btnWrite.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                writePressed = true;
                setTimer();
                showGuide();
            }
        });
    }

    private void showGuide() {
                AlertDialog.Builder alertbox = new AlertDialog.Builder(context);
                LayoutInflater inflater = getLayoutInflater();
                View dialogLayout = inflater.inflate(R.layout.guide, null);
                dialog = alertbox.create();
                dialog.setView(dialogLayout);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.show();
    }

    private void setTimer(){
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                    dialog.dismiss();
                    writePressed = false;
                    Toast.makeText(context, "No NFC tag detected. Try again", Toast.LENGTH_SHORT).show();
                //Do something after 7000ms
            }
        }, 7000);
    }

    private void writeSettings(){
            try {
                if (myTag == null) {
                    Toast.makeText(context, ERROR_DETECTED, Toast.LENGTH_LONG).show();
                } else {
                    write(message.getText().toString() + ";" + message2.getText().toString(), myTag);
                    Toast.makeText(context, WRITE_SUCCESS, Toast.LENGTH_LONG).show();
                    writePressed = false;
                    dialog.dismiss();
                    tvNFCContent.setText(message.getText().toString());
                    if(message2.getText().toString().equals("1")){
                        tvNFCContent1.setText("STD");
                    }
                    else if(message2.getText().toString().equals("2")){
                        tvNFCContent1.setText("EXT");
                    }
                }
            } catch (IOException e) {
                Toast.makeText(context, WRITE_ERROR, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            } catch (FormatException e) {
                Toast.makeText(context, WRITE_ERROR, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
    }

    /**********************************Read From NFC Tag***************************/
    private void readFromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            buildTagViews(msgs);
        }
    }

    private void buildTagViews(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) return;

        String text = "";
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; // Get the Text Encoding
        int languageCodeLength = payload[0] & 0063; // Get the Language Code, e.g. "en"

        try {
            // Get the Text
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());
        }

        String[] setList = text.split(";");
        set1 = setList[0];
        set2 = setList[1];
        if(set2.equals("1")){
            tvNFCContent1.setText("STD");
        }
        else if(set2.equals("2")){
            tvNFCContent1.setText("EXT");
        }
        tvNFCContent.setText(set1);
    }

    /**********************************Write to NFC Tag****************************/
    private void write(String text, Tag tag) throws IOException, FormatException {
        long startTime = 0;
        long endTime = 0;
        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);
        // Get an instance of Ndef for the tag.
        Ndef ndef = Ndef.get(tag);
        // Enable I/O

        ndef.connect();
        startTime = System.currentTimeMillis();
        // Write the message
        ndef.writeNdefMessage(message);
        endTime = System.currentTimeMillis();
        // Close the connection
        ndef.close();
        Log.v("NFC", "Time to write in milliseconds is: " + (endTime - startTime));
    }

    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String lang       = "en";
        byte[] textBytes  = text.getBytes();
        byte[] langBytes  = lang.getBytes("US-ASCII");
        int    langLength = langBytes.length;
        int    textLength = textBytes.length;
        byte[] payload    = new byte[1 + langLength + textLength];

        // set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1,              langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,  NdefRecord.RTD_TEXT,  new byte[0], payload);

        return recordNFC;
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (writePressed) {
            writeSettings();
        }
        else {
            setIntent(intent);
            readFromIntent(intent);
            if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
                myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            }
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        WriteModeOff();
    }

    @Override
    public void onResume(){
        super.onResume();
        WriteModeOn();
    }

    /**********************************Enable Write*******************************/
    private void WriteModeOn(){
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }
    /**********************************Disable Write*******************************/
    private void WriteModeOff(){
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }
}