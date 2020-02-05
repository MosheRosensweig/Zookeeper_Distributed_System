package edu.yu.cs.fall2019.intro_to_distributed.stage1;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class ClientImpl implements Client
{
    private String hostName;
    private int port;

    /**
     *
     * @param hostName - ex: http://www.fun.com or http://localhost
     * @param hostPort
     * @throws MalformedURLException
     */
    public ClientImpl(String hostName, int hostPort) throws MalformedURLException
    {
        this.hostName = hostName;
        this.port = hostPort;
    }

    @Override
    public Response compileAndRun(String src) throws IOException
    {
        String url = hostName + ":" + port + "/compileandrun";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        // optional default is GET
        con.setRequestMethod("POST"); //Unnecessary

        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(src);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        String responseStr = conResponsToStr(con, responseCode);

        return new Response(responseCode, responseStr);
    }

    public static String conResponsToStr(HttpURLConnection con, int responseCode) throws IOException
    {
        BufferedReader in;
        if(responseCode == 400){
            in = new BufferedReader(
                    new InputStreamReader(con.getErrorStream()));
        }
        else {
            in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
        }
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }
}
