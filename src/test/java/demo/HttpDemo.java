package demo;

import io.jafka.ptth.Request;
import io.jafka.ptth.Response;

/**
 * 
 * @author adyliu (imxylz@gmail.com)
 * @since 2017年7月12日
 */
public class HttpDemo {

    public static void main(String[] args) throws Exception{
        Request req = new Request("https://www.baidu.com");
        req.setUserAgent();
        Response resp = req.get();
        System.out.printf("status_code=%d cost=%d ms\n", resp.getStatusCode(), resp.getTimecost());
        System.out.println(resp.asString());
    }

}
