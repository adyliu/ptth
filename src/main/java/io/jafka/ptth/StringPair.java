package io.jafka.ptth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;

/**
 * 参数处理(用于处理HTTP HEADER和HTTP PARAMS）
 * 
 * @author adyliu (imxylz@gmail.com)
 * @since 2013-06-06
 */
public class StringPair extends BasicHeader implements Header, NameValuePair {

    public StringPair(String name, Object value) {
        super(name, String.valueOf(value));
    }

    public static List<StringPair> create(Map<String, ?> m) {
        if (m == null || m.isEmpty()) {
            return Collections.emptyList();
        }
        List<StringPair> ret = new ArrayList<StringPair>();
        for (Map.Entry<String, ?> e : m.entrySet()) {
            ret.add(new StringPair(e.getKey(), e.getValue()));
        }
        return ret;
    }

    public static List<StringPair> create(String... kv) {
        if (kv.length % 2 != 0) {
            throw new IllegalArgumentException("error length of params. " + Arrays.toString(kv));
        }
        List<StringPair> ret = new ArrayList<StringPair>();
        for (int i = 0; i < kv.length; i += 2) {
            ret.add(new StringPair(kv[i], kv[i + 1]));
        }
        return ret;
    }
}
