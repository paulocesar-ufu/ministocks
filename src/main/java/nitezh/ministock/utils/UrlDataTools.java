/*
 The MIT License

 Copyright (c) 2013 Nitesh Patel http://niteshpatel.github.io/ministocks

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */

package nitezh.ministock.utils;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionSpec;
import okhttp3.Dispatcher;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.TlsVersion;
import okhttp3.logging.HttpLoggingInterceptor;


public class UrlDataTools {

    private static final OkHttpClient HTTP =
            new OkHttpClient.Builder()
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

    private UrlDataTools() {
    }

    private static String inputStreamToString(InputStream stream) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(stream));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            builder.append(line).append("\n");
        }
        return builder.toString();
    }

    static String urlToString(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);
        return inputStreamToString(connection.getInputStream());
    }

    private static String getUrlData(String url) {
        Log.i("PCPC", "Getting data " + url);
        try {
            InetAddress[] addrs = InetAddress.getAllByName("gohorse.gafanho.to");
            Log.d("DNS", Arrays.toString(addrs));
        } catch (UnknownHostException e) {
            Log.e("DNS", "Still failing", e);
        }
//        ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
//                .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
//                .allEnabledCipherSuites()          // let CF pick the cipher
//                .build();

        HttpLoggingInterceptor log = new HttpLoggingInterceptor();
        log.setLevel(HttpLoggingInterceptor.Level.HEADERS);

        OkHttpClient client = new OkHttpClient.Builder()
                .followRedirects(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .followSslRedirects(true)
                .addInterceptor(log)
                .build();


        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "*/*")
                .build();

        try (Response res = client.newCall(request).execute()) {
            if (!res.isSuccessful()) throw new IOException("HTTP " + res.code());
            Log.i("TAG", "getUrlData: returning: " + res.body());
            return res.body().string();
        } catch (Exception e) {
            Log.i("TAG", "getUrlData: " + e);
        }
        return null;
    }


    private static String getTradingViewData(List<String> symbols) {
        symbols.remove("^DJI");
        Log.i("PCPC", "Getting data for symbols" + symbols);

        JSONArray data = new JSONArray();
        try {
            JSONObject payload = new JSONObject()
                    .put("symbols", new JSONObject().put("tickers", new JSONArray(symbols)))
                    .put("columns", new JSONArray(Arrays.asList("close", "change")));

            Request request = new Request.Builder()
                    .url("https://scanner.tradingview.com/global/scan")
                    .post(RequestBody.create(
                            payload.toString(),
                            MediaType.parse("application/json")
                            )
                    )
                    .build();

            try (Response res = HTTP.newCall(request).execute()) {
                if (!res.isSuccessful())
                    throw new IOException("HTTP " + res.code() + " – " + res.message());

                String body = res.body().string();
                Log.i("TAG", "getTradingViewData: returning: " + body);
                JSONArray items = new JSONObject(body).getJSONArray("data");
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    JSONArray d = item.getJSONArray("d");
                    double close  = d.getDouble(0);
                    double change = d.getDouble(1);
                    data.put(
                            new JSONObject()
                                    .put("symbol", item.getString("s"))
                                    .put("price", close)
                                    .put("percent", change)
                    );

                }
            } catch (Exception e) {
                Log.i("TAG", "getTradingViewData: " + e);
            }

            if(data.length() > 0) {
                JSONObject result = new JSONObject();
                result.put("quoteResponse", new JSONObject().put("result", data));
                return result.toString();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String getCachedUrlData(String url, Cache cache, Integer ttl) {
        String data;
        if (ttl != null && (data = cache.get(url)) != null) {
            return data;
        }

        data = getUrlData(url);
        if (data != null) {
            cache.put(url, data, ttl);
            return data;
        }
        return "";
    }

    public static String getCachedTradingViewData(List<String> symbols, Cache cache, Integer ttl) {
        if(symbols.isEmpty()) {
            return "";
        }
        String lastSymbol = symbols.get(symbols.size()-1);
        String lastChar = lastSymbol.substring(lastSymbol.length()-1);
        String cacheKey = "tradingview:" + String.join(",", symbols).length() + symbols.get(0).substring(symbols.get(0).length()-1) + lastChar;
        String data;
        if (ttl != null && (data = cache.get(cacheKey)) != null) {
            return data;
        }

        data = getTradingViewData(symbols);
        if (data != null) {
            cache.put(cacheKey, data, ttl);
            return data;
        }
        return "";
    }

}
