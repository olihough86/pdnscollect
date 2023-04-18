package io.oliverhough.pdnscollect;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.system.OsConstants;

public class DnsCaptureVpnService extends VpnService {

    private static final String API_URL = "http://192.168.1.140:8080/api/dnsdata";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private ParcelFileDescriptor vpnInterface;
    private OkHttpClient httpClient;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("DNSData", "Starting DnsCaptureVpnService");
        Builder builder = new Builder();
        builder.setSession("DNSCapture");
        builder.addAddress("10.0.0.2", 32);
        builder.addDnsServer("8.8.8.8");
        builder.addRoute("0.0.0.0", 0);
        builder.addRoute("::", 0);
        builder.allowFamily(OsConstants.AF_INET);
        builder.allowFamily(OsConstants.AF_INET6);
        try {
            builder.addDisallowedApplication(getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }

        vpnInterface = builder.establish();
        httpClient = new OkHttpClient();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    captureDnsPackets(); // Removed the 'socket' parameter
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();


        return START_NOT_STICKY;
    }

    private void captureDnsPackets() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(32767);
        FileInputStream inputStream = new FileInputStream(vpnInterface.getFileDescriptor());
        Log.d("DNSData", "Trying to capture packets");
        while (true) {
            buffer.clear();
            int length = inputStream.read(buffer.array());
            if (length > 0) {
                buffer.limit(length);
                processDnsPacket(buffer.array(), length); // Removed the 'socket' parameter
            }
        }
    }


    private void processDnsPacket(byte[] data, int length) {
        Log.d("DNSData", "Processing a packet");
        Log.d("DNSData", "First byte: " + String.format("0x%02X", data[0]));

        int version = (data[0] >> 4) & 0xF;

        if (version == 4) {
            processIpv4Packet(data, length);
        } else if (version == 6) {
            processIpv6Packet(data, length);
        } else {
            Log.d("DNSData", "Not an IPv4 or IPv6 packet");
        }
    }

    private void processIpv4Packet(byte[] data, int length) {
        int headerLength = (data[0] & 0xF) * 4;
        int protocol = data[9] & 0xFF;

        if (headerLength < 20 || length < headerLength) {
            Log.d("DNSData", "Not a valid IPv4 packet or invalid length");
            return;
        }

        if (protocol == 6 || protocol == 17) { // TCP or UDP
            int srcPort = ((data[headerLength] & 0xFF) << 8) | (data[headerLength + 1] & 0xFF);
            int dstPort = ((data[headerLength + 2] & 0xFF) << 8) | (data[headerLength + 3] & 0xFF);

            if (srcPort == 53 || dstPort == 53) {
                Log.d("DNSData", "IPv4 DNS packet");
            } else {
                Log.d("DNSData", "IPv4 Non-DNS packet");
            }
        } else {
            Log.d("DNSData", "New Log - IPv6 packet with unsupported protocol: " + protocol);
        }
    }

    private void processIpv6Packet(byte[] data, int length) {
        int headerLength = 40; // IPv6 header is fixed at 40 bytes
        int protocol = data[6] & 0xFF;

        if (length < headerLength) {
            Log.d("DNSData", "Not a valid IPv6 packet or invalid length");
            return;
        }

        if (protocol == 6 || protocol == 17) { // TCP or UDP
            int srcPort = ((data[headerLength] & 0xFF) << 8) | (data[headerLength + 1] & 0xFF);
            int dstPort = ((data[headerLength + 2] & 0xFF) << 8) | (data[headerLength + 3] & 0xFF);

            if (srcPort == 53 || dstPort == 53) {
                Log.d("DNSData", "IPv6 DNS packet");
            } else {
                Log.d("DNSData", "IPv6 Non-DNS packet");
            }
        } else {
            Log.d("DNSData", "New Log - IPv6 packet with unsupported protocol: " + protocol);
        }
    }

    private void sendDnsData(String hostAddress, String ip, String domain) {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = isoFormat.format(new Date());

        String json = String.format("{\"IP\": \"%s\", \"Domain\": \"%s\", \"Timestamp\": \"%s\"}",
                ip, domain, timestamp);
        Log.d("DNSData", "Sending DNS data: " + json);

        RequestBody requestBody = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .build();

        try {
            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                Log.d("DNSData", "DNS data sent successfully");
            } else {
                Log.d("DNSData", "Failed to send DNS data: " + response.code() + " " + response.message());
            }
            response.close();
        } catch (IOException e) {
            Log.e("DNSData", "Error sending DNS data", e);
        }
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
