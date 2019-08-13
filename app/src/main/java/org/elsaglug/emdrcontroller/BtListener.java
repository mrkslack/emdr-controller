// Interface declaration for event listener during bluetooth comunication with EMDR Device (c) 2019 mrkslack <mrkslack@gmail.com>
// released under the GPLv3 license


package org.elsaglug.emdrcontroller;

public interface BtListener {
    void getStatus(BtStatus e);
}
