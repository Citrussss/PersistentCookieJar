/*
 * Copyright (C) 2016 Francisco José Montiel Navarro.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.franmontiel.persistentcookiejar;

import com.franmontiel.persistentcookiejar.cache.CookieCache;
import com.franmontiel.persistentcookiejar.persistence.CookiePersistor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

public class PersistentCookieJar implements ClearableCookieJar {

    private final CookieCache cache;
    private final CookiePersistor persistor;

    public PersistentCookieJar(CookieCache cache, CookiePersistor persistor) {
        this.cache = cache;
        this.persistor = persistor;
        this.cache.addAll(persistor.loadAll());
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        synchronized (cache) {
            cache.addAll(cookies);
            persistor.saveAll(filterPersistentCookies(cookies));
        }
    }

    private static List<Cookie> filterPersistentCookies(List<Cookie> cookies) {
        List<Cookie> persistentCookies = new ArrayList<>();

        for (Cookie cookie : cookies) {
            if (cookie.persistent()) {
                persistentCookies.add(cookie);
            }
        }
        return persistentCookies;
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> cookiesToRemove = new ArrayList<>();
        List<Cookie> validCookies = new ArrayList<>();
        synchronized (cache) {
            for (Iterator<Cookie> it = cache.iterator(); it.hasNext(); ) {
                Cookie currentCookie = it.next();

                if (isCookieExpired(currentCookie)) {
                    cookiesToRemove.add(currentCookie);
                    it.remove();

                } else if (currentCookie.matches(url)) {
                    validCookies.add(currentCookie);
                }
            }
            persistor.removeAll(cookiesToRemove);
        }
        return validCookies;
    }

    private static boolean isCookieExpired(Cookie cookie) {
        return cookie.expiresAt() < System.currentTimeMillis();
    }

    @Override
    public void clearSession() {
        synchronized (cache) {
            cache.clear();
            cache.addAll(persistor.loadAll());
        }
    }

    @Override
    public void clear() {
        synchronized (cache) {
            cache.clear();
            persistor.clear();
        }
    }
}
