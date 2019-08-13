// Event message used by bluetooth comunication with EMDR Device (c) 2019 mrkslack <mrkslack@gmail.com>
// released under the GPLv3 license


package org.elsaglug.emdrcontroller;

public class BtStatus {
    public static final int OK = 0;
    public static final int NONE = 1;
    public static final int OFF = 2;
    public static final int NODEVICE = 3;
    public static final int NOCOMM = 4;

    public static final int CMD_INIT = 0;
    public static final int CMD_START = 105;
    public static final int CMD_STOP = 115;
    public static final int CMD_ERROR = 122;

    public static final String[] errors = {
            "Connesso",
            "Bluetooth non disponibile",
            "Bluetooth non abilitato",
            "Disposito non trovato",
            "Connessione non riuscita"

    };

    private int msg;
    private int cmd; // 0 -> event related to initial connection
                     // 105 a start command was submitted;
                     // 115 a stop command was submitted;

    public BtStatus(int cmd, int msg) {
        // super(source);
        this.msg = msg;
        this.cmd = cmd;
    }
    public String getMessage() {
        if(cmd == 0)
          return errors[msg];
        return "";
    }
    public int getStatus() {
        return msg;
    }
    public int getCommand() {
        return cmd;
    }
}