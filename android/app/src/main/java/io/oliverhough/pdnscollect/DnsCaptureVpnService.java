package io.oliverhough.pdnscollect;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import java.io.IOException;

public class DnsCaptureVpnService extends VpnService {

    private ParcelFileDescriptor vpnInterface;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Builder builder = new Builder();
        builder.setSession("DNSCapture");
        builder.addAddress("10.0.0.2", 32);
        builder.addDnsServer("8.8.8.8");

        vpnInterface = builder.establish();

        // TODO: Capture the DNS packets and process them.

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
