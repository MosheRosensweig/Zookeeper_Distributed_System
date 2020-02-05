package edu.yu.cs.fall2019.intro_to_distributed.stage3;

import java.io.IOException;

public interface Client
{
    //public ClientImpl(Stsring hostName, int hostPort) throws MalformedURLException
    class Response
    {
        private int code;
        private String body;
        public Response(int code, String body)
        {
            this.code = code;
            this.body = body;
        }
        public int getCode()
        {
            return this.code;
        }
        public String getBody()
        {
            return this.body;
        }
    }

    Response compileAndRun(String src) throws IOException;
}