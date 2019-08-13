// Entry point of EMDR Controller (c) 2019 mrkslack <mrkslack@gmail.com>
// released under the GPLv3 license



package org.elsaglug.emdrcontroller;

import com.akaita.android.circularseekbar.CircularSeekBar;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.CompoundButton;

import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;


public class MainActivity extends AppCompatActivity {

    TextView conn_status;
    RelativeLayout lay_speeder;
    RelativeLayout lay_starter;
    Switch connect;
    CircularSeekBar speeder;
    CircularSeekBar starter;
    boolean status;
    int old_speed;

    BtComm BT;

    private static final byte BT_START = 105;
    private static final byte BT_STOP = 115;
    private static final byte BT_CHECK_STATUS = 120;
    private static final byte BT_CHECK_SPEED = 121;
    private static final byte BT_ERROR = 122;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = false;
        old_speed = 50;

        connect = (Switch) findViewById(R.id.swConnect);

        starter = (CircularSeekBar) findViewById(R.id.seekStart);
        starter.setRingColor(Color.RED);
        speeder = (CircularSeekBar) findViewById(R.id.seekSpeed);
        speeder.setProgressTextFormat(new DecimalFormat("###"));
        speeder.setRingColor(Color.GREEN);


        BT = new BtComm();

        conn_status = findViewById(R.id.textStatus);
        lay_speeder = findViewById(R.id.grpSpeeder);
        lay_starter = findViewById(R.id.grpStart);

        connect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {

                if (isChecked) {
                    conn_status.setText("Tentativo di connessione ...");
                    BT.Connect();
                } else {
//                    if (BT.isConnected()) {
//                        BT.Send(BT_STOP);
//                    }
                    BT.Close();
                    conn_status.setText("Disconnesso");
                    lay_starter.setVisibility(View.INVISIBLE);
                    lay_speeder.setVisibility(View.INVISIBLE);
                    status = false;
                }
            }
        });

        speeder.setOnCenterClickedListener(new CircularSeekBar.OnCenterClickedListener() {
            @Override
            public void onCenterClicked(CircularSeekBar sb, float progress) {
                 BT.Send(BT_STOP, true);
            }
        });

        starter.setOnCenterClickedListener(new CircularSeekBar.OnCenterClickedListener() {
            @Override
            public void onCenterClicked(CircularSeekBar sb, float progress) {
                BT.Send(BT_START, true);
            }
        });
        speeder.setOnCircularSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar seekBar, float progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {
                int value = (int) seekBar.getProgress();
                BT.Send((byte)value,true);
            }
        });

        BT.addListener(new BtListener() {
            public void getStatus(BtStatus bts) {
                if (bts.getCommand() > 0) {

                    // a START command is submitted to device
                    if (bts.getCommand() == BT_START) {
                        if (bts.getStatus() == BT_START) { // device is running
                            lay_speeder.setVisibility(View.VISIBLE);
                            lay_starter.setVisibility(View.INVISIBLE);
                            status = true;
                        } else {  // device not responding after a start command
                            status = false;
                            flashSpeeder();
                            Toast.makeText(getApplicationContext(), "Il dispositivo non risponde", Toast.LENGTH_SHORT).show();
                        }

                        // a STOP command is submitted to device
                    } else if (bts.getCommand() == BT_STOP) {
                        if (bts.getStatus() == BT_STOP) { // device is stopped
                            lay_starter.setVisibility(View.VISIBLE);
                            lay_speeder.setVisibility(View.INVISIBLE);

                            status = false;
                        } else { // device not responding after a stop command
                            status = true;
                            flashSpeeder();
                            Toast.makeText(getApplicationContext(), "Il dispositivo non risponde", Toast.LENGTH_SHORT).show();
                        }

                        // SPEED value is issued to device
                    } else {
                        if (bts.getStatus() >0 && bts.getStatus() < 101) { // device speed is adapted
                            old_speed = bts.getCommand();
                        } else {  // device not responding after a speed command
                            speeder.setProgress((old_speed));
                            flashSpeeder();
                            Toast.makeText(getApplicationContext(), "Il dispositivo non risponde", Toast.LENGTH_SHORT).show();
                        }
                    }

                    // a Connection attempt is submitted
                } else {
                    if (bts.getStatus() > 0) { // connection not done!
                        if (BT.isConnected()) {
                            BT.Close();
                            status = false;
                        }
                        conn_status.setText("Disconnesso");
                        lay_starter.setVisibility(View.INVISIBLE);
                        lay_speeder.setVisibility(View.INVISIBLE);
                        connect.setChecked(false);
                        Toast.makeText(getApplicationContext(), bts.getMessage(), Toast.LENGTH_SHORT).show();
                    } else {
                        // device is Connected
                        int res = BT.Send(BT_CHECK_STATUS);
                        if (res == BT_STOP) { // device speed is adapted
                            status = false;
                            // adapt speed
                        } else if (res == BT_START) { // device speed is adapted
                            status = true;
                            // adapt speed
                        }
                        conn_status.setText("Connesso");
                        if(status) {
                            res = BT.Send(BT_CHECK_SPEED);
                            if(res >0 && res < 101) {
                                old_speed = res;
                                speeder.setProgress(res);
                            }
                            lay_speeder.setVisibility(View.VISIBLE);
                        }
                        else {
                            lay_starter.setVisibility(View.VISIBLE);
                        }
                    }
                }

            }
        });
    }
    /**
     * The ring of speeder flash red to warn for comunication error
     *
     */
    private void flashSpeeder() {
        speeder.setRingColor(Color.RED);
        final Handler h = new Handler(Looper.getMainLooper());
        final CircularSeekBar sp = speeder;
        Thread t1 = new Thread() {
            public void run() {
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            sleep(500);
                        }catch(InterruptedException e){
                            e.printStackTrace();
                        }
                        sp.setRingColor(Color.GREEN);
                    }
                });
            }
        };
        t1.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
//        BT.Send(BT_STOP);
        conn_status.setText("Disconnesso");
        lay_starter.setVisibility(View.INVISIBLE);
        lay_speeder.setVisibility(View.INVISIBLE);

        connect.setChecked(false);
        status = false;
        BT.Close();
    }

    protected void onPause() {
        super.onPause();
//        BT.Send(BT_STOP);
        conn_status.setText("Disconnesso");
        lay_starter.setVisibility(View.INVISIBLE);
        lay_speeder.setVisibility(View.INVISIBLE);
        connect.setChecked(false);
        status = false;
        BT.Close();
    }
    protected void onDiscard() {
//        BT.Send(BT_STOP);
        BT.Close();
    }

}

