package io.jafka.ptth;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

/**
 * 强制忽略cookie store
 * 
 * @author adyliu (imxylz@gmail.com)
 * @since 2015年12月29日
 */
class EmptyCookieStore implements CookieStore {

    @Override
    public void addCookie(Cookie cookie) {
    }

    @Override
    public List<Cookie> getCookies() {
        return new ArrayList<>(0);
    }

    @Override
    public boolean clearExpired(Date date) {
        return true;
    }

    @Override
    public void clear() {
    }

}
