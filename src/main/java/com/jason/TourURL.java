package com.jason;

import lombok.Builder;

import java.net.MalformedURLException;
import java.net.URL;

@Builder
public class TourURL {
    private String url;
    private String serviceKey;
    private String mobileOs;
    private String mobileApp;
    private int pageNo;
    private int numOfRows;

    public int getPageNo() { return pageNo; }

    public URL get() {
        try {
            return new URL(url + "?ServiceKey=" + serviceKey +
                    "&MobileOS=" + mobileOs + "&MobileApp=" + mobileApp +
                    "&pageNo=" + pageNo + "&numOfRows=" + numOfRows +
                    "&arrange=C&_type=json");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
