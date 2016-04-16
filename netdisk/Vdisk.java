/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Scanner;
import net.sf.json.JSONObject;

import org.apache.http.*;
import org.apache.http.impl.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.*;
import org.apache.http.util.*;
/**
 *
 * @author 杨德中
 */
public class Vdisk 
{    
    String app_key = "1780869568";
    String app_secret = "cc1f6659a39f9601209db362014f3635";
    String call_back_url = "http://home.ustc.edu.cn/~gdzjydz";
    String refresh_token = "";
    String access_token = null;
    //2016.3.27 21:30
    //今天的access_token:0afbab6662GCpzz1WwlwI3DIiF5ab19a
    //使用返回的URI打开，授权后可得到access_token，access_token出现在参数里面
    //其实本来按他的API文档是可以获得refresh_token的，但是并没有，坑爹，而且access_token一天就过期了
    public URI authorize(String response_type, String state) throws URISyntaxException
    {
        if(response_type == null)
            response_type = "token";
        if(state == null)
            state = "";
        URI uri = new URIBuilder()
        .setScheme("https")
        .setHost("auth.sina.com.cn/oauth2/authorize")
        .setParameter("client_id", this.app_key)
        .setParameter("redirect_uri", this.call_back_url)
        .setParameter("response_type", response_type)
        .setParameter("state", state)
        .build();
        return uri;
    }
    //输入获得的access_token，方便后续操作
    public void get_access_token()
    {
        Scanner in = new Scanner(System.in);
        this.access_token = in.next();
    }
    //封装的post操作，只需传入URL，返回对应JSONObject
    public JSONObject vdisk_post_operation(URI uri) throws IOException
    {
        CloseableHttpClient postClient = HttpClients.createDefault();
        if(access_token == null)
            this.get_access_token();
        HttpPost httpPost = new HttpPost(uri);
        JSONObject res = null;
        try(CloseableHttpResponse response = postClient.execute(httpPost))
        {
            HttpEntity entity = response.getEntity();
            String info = EntityUtils.toString(entity);
            res = JSONObject.fromObject(info);
        }
        finally
        {
            postClient.close();
        }
        return res;
    }
    //封装的get操作，只需传入URL，返回对应JSONObject
    public JSONObject vdisk_get_operation(URI uri) throws IOException
    {
        CloseableHttpClient getClient = HttpClients.createDefault();
        if(access_token == null)
            this.get_access_token();
        HttpGet httpGet = new HttpGet(uri);
        JSONObject res = null;
        try(CloseableHttpResponse response = getClient.execute(httpGet))
        {
            HttpEntity entity = response.getEntity();
            String info = EntityUtils.toString(entity);
            res = JSONObject.fromObject(info);
        }
        finally
        {
            getClient.close();
        }
        return res;
    }
    
    //获取用户信息
    public JSONObject get_user_info() throws IOException, URISyntaxException
    {
        URI uri = new URIBuilder()
        .setScheme("https")
        .setHost("api.weipan.cn/2/account/info")
        .setParameter("access_token", this.access_token)
        .build();
        return vdisk_post_operation(uri);
    }
    //获得所有文件信息
    public JSONObject get_file_info() throws IOException, URISyntaxException
    {
        URI uri = new URIBuilder()
        .setScheme("https")
        .setHost("api.weipan.cn/2/delta/sandbox")
        .setParameter("access_token", this.access_token)
        .build();
        return vdisk_post_operation(uri);
    }
    //下载文件，上面的路径和根路径
    public void download_file(String filepath, String local_filepath) throws URISyntaxException, IOException
    {
        URI uri = new URIBuilder()
        .setScheme("https")
        .setHost("api.weipan.cn/2/files/sandbox/")
        .setPath(filepath)
        .setParameter("access_token", this.access_token)
        .build();
        CloseableHttpClient getClient = HttpClients.createDefault();
        if(access_token == null)
            this.get_access_token();
        HttpGet httpGet = new HttpGet(uri);
        try(CloseableHttpResponse response = getClient.execute(httpGet))
        {
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == 200)
            {
                File file = new File(local_filepath);
                try (FileOutputStream fout = new FileOutputStream(file)) 
                {
                    String content = EntityUtils.toString(response.getEntity());
                    System.out.println(content.length());
                    byte contents[] = content.getBytes(Charset.forName("ISO-8859-1"));
                    fout.write(contents);
                }
            }
        }
        finally
        {
            getClient.close();
        }
    }
    //上传文件，本地路径和上面的路径，上面的路径以/为根目录
    public void upload_file(String local_filepath, String filepath) throws URISyntaxException, FileNotFoundException, IOException
    {
        File file = new File(local_filepath);
        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost("upload-vdisk.sina.com.cn/2/files/sandbox/")
                .setPath(filepath)
                .setParameter("access_token", this.access_token)
                .build();
        HttpPost httpPost = new HttpPost(uri);        
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addBinaryBody("file", file, ContentType.DEFAULT_BINARY, local_filepath);
        HttpEntity entity = builder.build();
        httpPost.setEntity(entity);
        CloseableHttpClient postClient = HttpClients.createDefault();
        try(CloseableHttpResponse response = postClient.execute(httpPost))
        {
            System.out.println(response);//check result
        }
        finally
        {
            postClient.close();
        }
    }
    //记住加上filepath，要不可能会删掉所有文件。。。不过现在好像删不了，有bug
    public JSONObject delete_file(String filepath) throws URISyntaxException, IOException
    {
        URI uri = new URIBuilder()
        .setScheme("https")
        .setHost("api.weipan.cn/2/fileops/delete")
        .setParameter("root", "sandbox")
        .setParameter("path", filepath)
        .setParameter("access_token", this.access_token)
        .build();
        return vdisk_post_operation(uri);
    }
    
    public Vdisk()
    {
        
    }
}
