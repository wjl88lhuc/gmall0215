package com.atguigu.gmall.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @param
 * @return
 */


public class HttpClientUtil {

    /**
     *  SSLContext sslcontext = SSLContexts.createDefault();
     * 			SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslcontext, new String[] { "TLSv1.2" },
     * 					null,SSLConnectionSocketFactory.getDefaultHostnameVerifier());
     *
     * 			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
     * 			           .register("http", PlainConnectionSocketFactory.getSocketFactory())
     * 			           .register("https", factory)  // 用来配置支持的协议
     * 			           .build();
     * 			// 加个共享连接池
     * 			PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
     * 			connectionManager.setMaxTotal(10);
     * 			connectionManager.setDefaultMaxPerRoute(10);
     * 			httpClient = HttpClientBuilder.create().setConnectionManager(connectionManager).setConnectionManagerShared(true).build();
     * 			// httpClient = HttpClientBuilder.create().setSSLSocketFactory(factory).build()
     * ————————————————
     * 版权声明：本文为CSDN博主「Record Life」的原创文章，遵循 CC 4.0 BY-SA 版权协议，转载请附上原文出处链接及本声明。
     * 原文链接：https://blog.csdn.net/qq_28929589/article/details/88284723
     * @return
     */
    public static CloseableHttpClient  getHttpClient(){
        SSLContext sslcontext = SSLContexts.createDefault();
        SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslcontext, new String[] { "TLSv1.2" },
                null,SSLConnectionSocketFactory.getDefaultHostnameVerifier());
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", factory)  // 用来配置支持的协议
                .build();
        // 加个共享连接池
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connectionManager.setMaxTotal(10);
        connectionManager.setDefaultMaxPerRoute(10);
        return HttpClientBuilder.create().setConnectionManager(connectionManager).setConnectionManagerShared(true).build();
    }

    public static String doGet(String url)   {

        // 创建Httpclient对象
        CloseableHttpClient httpclient = getHttpClient();
        // 创建http GET请求
        HttpGet httpGet = new HttpGet(url);
        CloseableHttpResponse response = null;
        try {
            // 执行请求
            response = httpclient.execute(httpGet);
            // 判断返回状态是否为200
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity, "UTF-8");
                EntityUtils.consume(entity);
                httpclient.close();
                return result;
            }
            httpclient.close();
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }

        return  null;
    }


    public static void download(String url,String fileName)   {

        // 创建Httpclient对象
        CloseableHttpClient httpclient = getHttpClient();
        // 创建http GET请求
        HttpGet httpGet = new HttpGet(url);
        CloseableHttpResponse response = null;
        try {
            // 执行请求
            response = httpclient.execute(httpGet);
            // 判断返回状态是否为200
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();

               // String result = EntityUtils.toString(entity, "UTF-8");
                byte[] bytes = EntityUtils.toByteArray(entity);
                File file =new File(fileName);
               //  InputStream in = entity.getContent();
                FileOutputStream fout = new FileOutputStream(file);
                fout.write(bytes);

                EntityUtils.consume(entity);

                httpclient.close();
                fout.flush();
                fout.close();
                return  ;
            }
            httpclient.close();
        }catch (IOException e){
            e.printStackTrace();
            return  ;
        }

        return   ;
    }
}
