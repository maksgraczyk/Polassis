/*
Polassis: personal voice assistant for Android devices
Copyright (C) 2018 Maksymilian Graczyk

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.mg.polassis.misc;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NetworkHandler {

    public interface NetworkQueryListener {
        void onBeforeConnection();
        void onConnectionCompleted(int responseCode, Map<String, List<String>> header, byte[] data);
        void onException(Exception exception, int responseCode);
    }

    public static InputStream querySync(String url, String method, @Nullable Map<String, String> parameters,
                                        @Nullable Map<String, String> headerProperties, @Nullable byte[] data) throws IOException
    {
        if (parameters != null && method.equals("GET")) url += "?" + getQuery(parameters);

                    URL newURL = new URL(url);
                    HttpURLConnection connection = (HttpURLConnection) newURL.openConnection();
                    connection.setReadTimeout(5000);
                    connection.setConnectTimeout(5000);
                    connection.setRequestMethod(method);
                    connection.setDoInput(true);

                    if (headerProperties != null) {
                        Set<Map.Entry<String, String>> set = headerProperties.entrySet();

                        for (Map.Entry<String, String> entry : set) {
                            connection.setRequestProperty(entry.getKey(), entry.getValue());
                        }
                    }

                    if (method.equals("POST") && parameters != null) {
                        connection.setDoOutput(true);
                        OutputStream outputStream = connection.getOutputStream();
                        outputStream.write(getQuery(parameters).getBytes("UTF-8"));
                    }

                    if (data != null) {
                        connection.setDoOutput(true);
                        OutputStream outputStream = connection.getOutputStream();
                        outputStream.write(data);
                    }

                    connection.connect();

                    return connection.getInputStream();
    }

    public static void query(final Activity activity, String url, final String method, final NetworkQueryListener listener, final @Nullable Map<String, String> parameters,
                             final @Nullable Map<String, String> headerProperties, final @Nullable byte[] data) {
        listener.onBeforeConnection();

        if (parameters != null && method.equals("GET")) url += "?" + getQuery(parameters);

        final String urlToSend = url;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                int responseCode = -1;
                try {
                    URL newURL = new URL(urlToSend);
                    HttpURLConnection connection = (HttpURLConnection) newURL.openConnection();
                    connection.setReadTimeout(5000);
                    connection.setConnectTimeout(5000);
                    connection.setRequestMethod(method);
                    connection.setDoInput(true);

                    if (headerProperties != null) {
                        Set<Map.Entry<String, String>> set = headerProperties.entrySet();

                        for (Map.Entry<String, String> entry : set) {
                            connection.setRequestProperty(entry.getKey(), entry.getValue());
                        }
                    }

                    if (method.equals("POST") && parameters != null) {
                        connection.setDoOutput(true);
                        OutputStream outputStream = connection.getOutputStream();
                        outputStream.write(getQuery(parameters).getBytes("UTF-8"));
                    }

                    if (data != null) {
                        connection.setDoOutput(true);
                        OutputStream outputStream = connection.getOutputStream();
                        outputStream.write(data);
                    }

                    connection.connect();
                    responseCode = connection.getResponseCode();

                    final int responseCodeToSend = responseCode;
                    final Map<String, List<String>> headerFields = connection.getHeaderFields();
                    final byte[] data = IOUtils.toByteArray(connection.getInputStream());
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onConnectionCompleted(responseCodeToSend, headerFields, data);
                        }
                    });
                } catch (final Exception e) {
                    final int responseCodeToSend = responseCode;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onException(e, responseCodeToSend);
                        }
                    });
                }
            }
        };
        new Thread(runnable).start();
    }

    private static String getQuery(Map<String, String> parameters) {
        String query = "";
        Set<Map.Entry<String, String>> set = parameters.entrySet();

        for (Map.Entry<String, String> entry : set) {
            query += entry.getKey() + "=" + entry.getValue() + "&";
        }

        return query.substring(0, query.length() - 1);
    }
}
