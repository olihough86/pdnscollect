package io.oliverhough.pdnscollect;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_VPN_PERMISSION = 1;
    private static final String VPN_ACTION = "io.oliverhough.pdnscollect.START_VPN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestVpnPermission();
    }

    private void requestVpnPermission() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CODE_VPN_PERMISSION);
        } else {
            onActivityResult(REQUEST_CODE_VPN_PERMISSION, Activity.RESULT_OK, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_VPN_PERMISSION) {
            if (resultCode == Activity.RESULT_OK) {
                // Start the VPN service after receiving user permission
                startVpnService();
            } else {
                // Handle the case where the user did not grant permission
                Toast.makeText(this, "VPN permission not granted", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void startVpnService() {
        Intent intent = new Intent(this, DnsCaptureVpnService.class);
        intent.setAction(VPN_ACTION);
        startService(intent);
    }
}
