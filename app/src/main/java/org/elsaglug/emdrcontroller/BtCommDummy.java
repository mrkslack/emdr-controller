// Dummy class for bluetooth comunication with EMDR Device (c) 2019 mrkslack <mrkslack@gmail.com>
// released under the GPLv3 license


package org.elsaglug.emdrcontroller;

import android.os.Handler;

import java.util.ArrayList;
import java.util.List;


/**
 *  this class is used for testing UI (replacing the BtComm class) when bluetooth connection is unavailable
 *  See BtComm
 */
public class BtCommDummy {

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
        final Handler h = new Handler();
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
        connected = true;
        statusEvent(0,BtStatus.OK);
        return; // connection bluetooth OK
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
            final Handler h = new Handler();
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
         return cmd;
//        return BtStatus.CMD_ERROR;
    }
    public void Close() {
        connected = false;
    }

    public boolean isConnected() {
        return connected;
    }

}
