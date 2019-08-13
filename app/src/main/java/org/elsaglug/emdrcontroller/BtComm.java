// Class for bluetooth comunication with EMDR Device (c) 2019 mrkslack <mrkslack@gmail.com>
// released under the GPLv3 license



package org.elsaglug.emdrcontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 *  EMDR Comunicatin Protocol
 *
 *  supported commands:
 *  START           105     device respond echoing received command ( START )
 *  STOP            115     device respond echoing received command ( STOP )
 *  CHECK_STATUS    120     device respond with his status ( START | STOP )
 *  CHECK_SPEED     121     device respond with his spedd ( 0>101 )
 *  SPEED           0 > 101 device respond echoing received speed ( 0<101 )
 *
 *  if device is not stopped when connection is closed or lost it continue running.
 *
 * */
public class BtComm {

    BluetoothSocket mmSocket=null;
    OutputStream outStream=null;
    InputStream inStream=null;

    private String mac = "00:14:03:05:59:D8";  // MODULO HC-06
    private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private boolean connected = false;

    private List<BtListener> listeners = new ArrayList<BtListener>();

    public void addListener(BtListener toAdd) {
        listeners.add(toAdd);
    }

    public void statusEvent(int type, int error) {
        for (BtListener btl : listeners)
            btl.getStatus(new BtStatus(type, error));
    }

    /**
     * Call _connect() with a posticipate handler. This allow caller to update his graphics tool without lags.
     * Rise a BTstatus event with command 0
     *
     */
    public void Connect() {
        final Handler h = new Handler(Looper.getMainLooper());
        Thread t1 = new Thread() {
            public void run() {
                h.post(new Runnable() {
                    @Override
                    public void run () {
                        _connect();
                    }
                });
            }
        };
        t1.start();
    }

    /**
     * Try to connect to device.
     * Rise a BTstatus event with command 0
     *
     */
    private void _connect() {
        BluetoothAdapter mBluetoothAdapter=null;
        BluetoothDevice mmDevice=null;
        connected = false;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter != null) { // check if bluetooth is supported
            if (!mBluetoothAdapter.isEnabled()) { //check if bluetooth  is enabled
                //  bluetooth disabled
                statusEvent(0,BtStatus.OFF);
                return;
            }
            else {
                //  bluetooth enabled
                mmDevice=mBluetoothAdapter.getRemoteDevice(mac); //MAC address of HC-06 module
                try {
                    this.mmSocket=mmDevice.createRfcommSocketToServiceRecord(uuid);
                }
                catch (IOException e) {
                    statusEvent(0, BtStatus.NODEVICE);
                    return;
                }
                try {
                    // connecting with mmSocket
                    mmSocket.connect();
                    outStream = mmSocket.getOutputStream();
                    inStream = mmSocket.getInputStream();
                    connected = true;
                    statusEvent(0,BtStatus.OK);
                    return; // connection bluetooth OK
                }
                catch (IOException closeException) {
                    try {
                        if(outStream !=null)
                            outStream.close();
                        if(inStream !=null)
                            inStream.close();
                        // tey to close socket
                        mmSocket.close();
                    } catch (IOException ceXC) {
                    }
                    statusEvent(0,BtStatus.NOCOMM);
                    return;
                }
            }   // close else of isEnabled
        }  // close else of mBluetoothAdapter == null
        statusEvent(0,BtStatus.NONE);
        return;
    }

    /**
     * Send command to device
     *
     * @param command   command to forward to device
     * @param event     <code>true</code> rise a BtEvent with result
     * @return          if event = <code>false</code> return result replyed from device (see BtEvent.CMD_*) else 0
     */
    public int Send(byte command, boolean event) {
        if(event) {
            final Handler h = new Handler(Looper.getMainLooper());
            final byte cmd = command;
            final boolean ev = event;
            Thread t1 = new Thread() {
                public void run() {
                    h.post(new Runnable() {
                        @Override
                        public void run() {
                            int res = Send(cmd);
                            statusEvent(cmd,res);
                        }
                    });
                }
            };
            t1.start();
            return 0;
        }
        else
            return Send(command);

    }

    /**
     * Send command to device.
     *
     * @param cmd       command to forward to device
     * @return          return result replyed from device (see BtEvent.CMD_*).
     */
    public int Send(byte cmd) {

        int timeout = 0;
        int maxTimeout = 8; // leads to a timeout of 2 seconds
        int available = 0;

        byte[] data = {cmd};

        if (outStream == null)  {
            return BtStatus.CMD_ERROR;
        }
        try {
            outStream.write(data,0,1);  // write command

            // read reply with 2 sec timeout
            while((available = inStream.available()) == 0 && timeout < maxTimeout) {
                timeout++;
                // throws interrupted exception
                try {
                    Thread.sleep(250);
                }
                catch (InterruptedException e) {}
            }
            if(available == 0) {
                return BtStatus.CMD_ERROR;
            }
            byte[] read = new byte[available];
            if(inStream.read(read) == 1)
              return read[0];
        }
        catch (IOException e) {
            return BtStatus.CMD_ERROR;
        }
        return BtStatus.CMD_ERROR;
    }

    public void Close() {
        try {
            if (outStream != null)
                outStream.close();
            if (inStream != null)
                inStream.close();
            if (mmSocket != null)
                mmSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        connected = false;
    }

    public boolean isConnected() {
        return connected;
    }

}
