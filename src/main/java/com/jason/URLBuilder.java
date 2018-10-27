package com.jason;

import lombok.Builder;

@Builder
public class URLBuilder {
    private String url;
    private String serviceKey;
    private String mobileOs;
    private String mobileApp;
    private int pageNo;
    private int numOfRows;
    private String sort;
    private String accept;


}
