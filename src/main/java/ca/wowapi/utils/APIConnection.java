package ca.wowapi.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import ca.wowapi.exceptions.NotModifiedException;

public class APIConnection {

	public static String getStringJSONFromRequest(String url) {
		String string = null;
		try {
			string = getStringJSONFromRequest(url, 0);
		} catch (NotModifiedException e) {
			// won't happen since we aren't passing the lastModified time
		}
		return string;
	}

	public static String getStringJSONFromRequest(String url, long lastModified) throws NotModifiedException {

		String str = null;
		try {
			URL jURL = new URL(url);
			HttpURLConnection urlConnection;
			urlConnection = (HttpURLConnection) jURL.openConnection();

			if (lastModified != 0) {
				urlConnection.setIfModifiedSince(lastModified);
			}

			str = readJSONStream(urlConnection);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return str;
	}

	public static String getStringJSONFromRequestAuth(String url, String publicKey, String privateKey) {
		String string = null;
		try {
			string = getStringJSONFromRequestAuth(url, publicKey, privateKey, 0);
		} catch (NotModifiedException e) {
			// won't happen since we aren't passing the lastModified time
		}
		return string;
	}

	public static String getStringJSONFromRequestAuth(String url, String publicKey, String privateKey, long lastModified) throws NotModifiedException {

		String UrlPath = null;
		if (url.contains("?")) {
			UrlPath = url.substring(url.indexOf("/api"), url.indexOf("?"));
		} else {
			UrlPath = url.substring(url.indexOf("/api"));
		}

		String str = null;
		try {
			URL jURL = new URL(url);
			HttpURLConnection urlConnection;
			urlConnection = (HttpURLConnection) jURL.openConnection();

			String fmtStr = "E, dd MMM yyyy HH:mm:ss";
			java.util.Date myDate = new java.util.Date();
			SimpleDateFormat sdf = new java.text.SimpleDateFormat(fmtStr);
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			String dateStr = sdf.format(myDate) + " GMT";

			String stringToSign = urlConnection.getRequestMethod() + "\n" + dateStr + "\n" + UrlPath + "\n";
			String sig = generateHmacSHA1Signature(stringToSign, privateKey);
			try {
				urlConnection.setRequestProperty("Authorization", "BNET" + " " + publicKey + ":" + sig);
				urlConnection.setRequestProperty("Date", dateStr);

				if (lastModified != 0) {
					urlConnection.setIfModifiedSince(lastModified);
				}
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}

			str = readJSONStream(urlConnection);

		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}

		return str;
	}

	private static String readJSONStream(HttpURLConnection urlConnection) throws IOException, NotModifiedException {
		String str = null;

		final char[] buffer = new char[0x1000];
		StringBuilder out = new StringBuilder();
		Reader in = null;
		if (urlConnection.getResponseCode() == 304) {
			throw new NotModifiedException();
		} else if (urlConnection.getResponseCode() < 400) {
			in = new InputStreamReader(urlConnection.getInputStream(), "UTF-8");
		} else {
			in = new InputStreamReader(urlConnection.getErrorStream(), "UTF-8");
		}

		int read;
		do {
			read = in.read(buffer, 0, buffer.length);
			if (read > 0) {
				out.append(buffer, 0, read);
			}
		} while (read >= 0);

		str = out.toString();
		in.close();
		return str;
	}

	public static String generateHmacSHA1Signature(String data, String key) throws GeneralSecurityException, IOException {
		byte[] hmacData = null;
		SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA1");
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(secretKey);
		hmacData = mac.doFinal(data.getBytes("UTF-8"));
		return Base64Converter.encode(hmacData);
	}
}
