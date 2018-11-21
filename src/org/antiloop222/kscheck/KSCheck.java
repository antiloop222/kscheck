package org.antiloop222.kscheck;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.antiloop222.kscheck.json.Availability;
import org.antiloop222.kscheck.json.DedicatedAvailabilityStruct;
import org.antiloop222.kscheck.json.DedicatedAvailabilityZoneStruct;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class KSCheck {

    private static final String GET_URL = "https://ws.ovh.com/dedicated/r2/ws.dispatcher/getAvailability2";
    private static final String ORDER_URL = "https://www.kimsufi.com/fr/commande/kimsufi.xml?reference=%s&quantity=%d";

    private String getKSAvailability() throws IOException, NoSuchAlgorithmException, KeyManagementException {
        StringBuilder sb = new StringBuilder();
        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }
        };
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        java.net.URL url = new URL(GET_URL);
        HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
        InputStream is = conn.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String inputLine;
        while((inputLine = br.readLine()) != null) {
            sb.append(inputLine);
        }
        return sb.toString();
    }

    private AvailableServer checkKSAvailability() throws NoSuchAlgorithmException, IOException, KeyManagementException {
        String json = getKSAvailability();
        Gson gson = new GsonBuilder().create();
        final Availability availability = gson.fromJson(json, Availability.class);
        for(DedicatedAvailabilityStruct a : availability.answer.availability) {
            if("1801sk13".equals(a.reference)) {
                for(DedicatedAvailabilityZoneStruct z : a.metaZones) {
                    System.out.println(a.reference + " - " + z.zone + " - " + z.availability);
                    if(!"unavailable".equals(z.availability)) {
                        return new AvailableServer(a.reference, z.zone);
                    }
                }
            }
        }
        return null;
    }

    private void startFirefox(AvailableServer server) throws IOException, InterruptedException {
        Runtime r = Runtime.getRuntime();
        Process p = r.exec("C:\\Program Files\\Mozilla Firefox\\firefox.exe -new-tab \"https://www.kimsufi.com/fr/commande/kimsufi.xml?reference=" + server.reference + "&quantity=2\"");
        p.waitFor();
    }

    private void orderKSServer(AvailableServer server) throws InterruptedException {
        FirefoxDriver driver = new FirefoxDriver();
//        driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
        String url = ORDER_URL.format(ORDER_URL, server.reference, 2);
        System.out.println(url);
//        driver.get(ORDER_URL + server.reference);
//        Thread.sleep(10000);
//        WebElement element = driver.findElement(By.id("quantity-2"));
//        System.out.println(element);
        driver.quit();
    }

    private void run() {
        int i = 0;
        while(true) {
            System.out.println("Check availability (try " + (++i) + ")...");
            try {
                AvailableServer server = checkKSAvailability();
                if(server != null) {
                    System.out.println("Found server: " + server);
                    if("fr".equals(server.zone)) {
                        System.out.println("Starting firefox...");
                        startFirefox(server);
                    } else {
                        System.out.println("Ignored server because of inadequate zone");
                    }
//                orderKSServer(server);
                    break;
                } else {
                    System.out.println("No server available");
                }
            } catch(Exception ex) {
                System.err.println(ex.toString());
            }
            System.out.println("Sleeping 30s...");
            try {
                Thread.sleep(30000);
            } catch(InterruptedException iex) {
                System.err.println(iex.toString());
            }
        }
    }

    public static void main(String[] args) {
        System.setProperty("webdriver.gecko.driver", "C:\\Program Files\\Mozilla Firefox\\firefox.exe");
        if(args.length > 1) {
            System.setProperty("http.proxyHost", args[0]);
            System.setProperty("http.proxyPort", args[1]);
            System.setProperty("https.proxyHost", args[0]);
            System.setProperty("https.proxyPort", args[1]);
        }
        new KSCheck().run();
//        new KSCheck().orderKSServer(new AvailableServer("1801sk14", "fra"));
    }
}
