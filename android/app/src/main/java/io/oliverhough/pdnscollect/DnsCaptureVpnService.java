package io.oliverhough.pdnscollect;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.system.OsConstants;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import java.util.Formatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
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
                Log.d("DNSData", "Captured packet with length: " + length); // Add this log statement
                processDnsPacket(buffer.array(), length); // Removed the 'socket' parameter
            }
        }
    }

    private void processDnsPacket(byte[] packetData, int length) {
        String packetType;
        String transportProtocol;
        int srcPort = 0;
        int dstPort = 0;
        byte[] udpPayload = new byte[0];

        // Check if it's an IPv4 packet (starts with 0x45)
        if ((packetData[0] & 0xf0) == 0x40) {
            packetType = "IPv4";
            // Check if it's a UDP packet (17)
            if (packetData[9] == 17) {
                transportProtocol = "UDP";
                // Extract the UDP payload
                int udpPayloadOffset = (packetData[0] & 0x0f) * 4 + 20;
                udpPayload = Arrays.copyOfRange(packetData, udpPayloadOffset, length);

                // Extract source and destination ports
                srcPort = ((udpPayload[0] & 0xff) << 8) | (udpPayload[1] & 0xff);
                dstPort = ((udpPayload[2] & 0xff) << 8) | (udpPayload[3] & 0xff);

                Log.d("DNSData", "Captured " + packetType + " packet");
                Log.d("DNSData", "Transport protocol: " + transportProtocol);
                Log.d("DNSData", "Source port: " + srcPort + ", Destination port: " + dstPort);
                Log.d("DNSData", "UDP payload: " + new String(udpPayload));
            } else {
                Log.d("DNSData", "Captured non-UDP packet with length: " + length);
            }
        } else {
            Log.d("DNSData", "Captured non-IPv4 packet with length: " + length);
        }

        // Check if the payload contains a DNS query
        // Check if the payload contains a DNS query
        String payloadString = new String(udpPayload);
        Log.d("DNSData", "Payload string: " + payloadString);

        // Output the payload as a hex dump
        StringBuilder hexDump = new StringBuilder();
        Formatter formatter = new Formatter(hexDump);
        for (byte b : udpPayload) {
            formatter.format("%02x ", b);
        }
        Log.d("DNSData", "Payload hex dump: " + hexDump.toString());

        // Extract the domain name by considering label length
        StringBuilder domainName = new StringBuilder();
        int i = 0;
        while (i < udpPayload.length) {
            int labelLength = udpPayload[i] & 0xFF;
            if (labelLength == 0) {
                break;
            }

            if (i + labelLength >= udpPayload.length) {
                break;
            }

            String label = new String(udpPayload, i + 1, labelLength);
            domainName.append(label);
            i += labelLength + 1;

            if (i < udpPayload.length && udpPayload[i] != 0) {
                domainName.append('.');
            }
        }

        if (domainName.length() > 0) {
            // Remove the trailing dot, if present
            if (domainName.charAt(domainName.length() - 1) == '.') {
                domainName.deleteCharAt(domainName.length() - 1);
            }
            Log.d("DNSData", "DNS query: " + domainName.toString());

            // Send the domain name and timestamp to the API
            sendDnsData(domainName.toString());
        }
    }

    private void sendDnsData(String domain) {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = isoFormat.format(new Date());

        String json = String.format("{\"Domain\": \"%s\", \"Timestamp\": \"%s\"}",
                domain, timestamp);
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