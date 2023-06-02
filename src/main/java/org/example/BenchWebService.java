package org.example;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class BenchWebService {
  public HttpURLConnection GetHttpClient() {

    HttpURLConnection conn = null;
    try {
      URL url = new URL("http://echo.jsontest.com/title/ipsum/content/blah");
      conn = (HttpURLConnection) url.openConnection();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return conn;
  }  
  
  public void SendHttpRequest(HttpURLConnection conn) {
  
    try {
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Accept", "application/json");

      if (conn.getResponseCode() != 200) {
          throw new RuntimeException("Failed : HTTP error code : "
                  + conn.getResponseCode());
      }

      BufferedReader br = new BufferedReader(new InputStreamReader(
          (conn.getInputStream())));

      String output;
      System.out.println("Output from Server .... \n");
      while ((output = br.readLine()) != null) {
          System.out.println(output);
      }

      conn.disconnect();
    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}