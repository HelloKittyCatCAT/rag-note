package com.sya.ragnote.client.research;

import com.sya.ragnote.enums.result.ResearchResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

@Component
public class SafePageClient {
    private static final int MAX_BYTES = 1024 * 1024;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .proxy(Proxy.NO_PROXY)
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .connectTimeout(3, TimeUnit.SECONDS)
            .dns(host -> {
                List<InetAddress> addresses = Dns.SYSTEM.lookup(host);
                // 此解析结果直接供 OkHttp 建立连接，避免预检查与连接分别解析。
                if (addresses.isEmpty() || addresses.stream().anyMatch(a -> !publicAddress(a))) {
                    throw new UnknownHostException("Public addresses required");
                }
                return addresses;
            }).build();

    static boolean publicAddress(InetAddress address) {
        if (!(address instanceof Inet4Address) || address.isAnyLocalAddress()
                || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) return false;
        byte[] ip = address.getAddress();
        int a = ip[0] & 255;
        int b = ip[1] & 255;
        int c = ip[2] & 255;
        // 拦截共享地址空间、保留网段和文档示例地址。
        return a != 0 && a < 224 && !(a == 100 && b >= 64 && b <= 127)
                && !(a == 192 && b == 0) && !(a == 192 && b == 88 && c == 99)
                && !(a == 198 && (b == 18 || b == 19 || b == 51))
                && !(a == 203 && b == 0 && c == 113);
    }

    public String read(String url, long deadline) {
        HttpUrl current = HttpUrl.parse(url);
        for (int redirects = 0; redirects <= 3; redirects++) {
            if (current == null || !current.username().isEmpty() || !current.password().isEmpty()
                    || current.host().matches("[0-9.]+") || current.host().contains(":")
                    || current.port() != (current.isHttps() ? 443 : 80)) break;
            var call = client.newCall(new Request.Builder().url(current).get().build());
            call.timeout().timeout(Math.min(TimeUnit.SECONDS.toNanos(10),
                    Math.max(1, deadline - System.nanoTime())), TimeUnit.NANOSECONDS);
            try (Response response = call.execute()) {
                if (response.isRedirect()) {
                    String location = response.header("Location");
                    current = location == null ? null : current.resolve(location);
                    continue;
                }
                var body = response.body();
                var type = body.contentType();
                if (!response.isSuccessful() || type == null || !type.type().equals("text")
                        || !(type.subtype().equals("html") || type.subtype().equals("plain"))) break;
                byte[] bytes = body.byteStream().readNBytes(MAX_BYTES + 1);
                if (bytes.length > MAX_BYTES) break;
                var page = Jsoup.parse(new java.io.ByteArrayInputStream(bytes), null, current.toString());
                page.select("script,style,nav,header,footer,form").remove();
                var main = page.selectFirst("main,article");
                String text = (main == null ? page.body() : main).text();
                if (!text.isBlank()) return text.substring(0, Math.min(8000, text.length()));
                break;
            } catch (IOException failure) {
                throw new ServiceSystemException(ResearchResultEnum.PAGE_UNREADABLE, failure);
            }
        }
        throw new ServiceSystemException(ResearchResultEnum.PAGE_UNREADABLE);
    }
}
