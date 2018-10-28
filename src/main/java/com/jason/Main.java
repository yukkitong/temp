package com.jason;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class Main {

    private static Date today() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        return calendar.getTime();
    }

    private static Date start() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -2);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        return calendar.getTime();
    }

    private static final String BASE_URL = "http://api.visitkorea.or.kr/openapi/service/rest";
    private static final String SERVICE_KEY = "A%2BycgFhk2eYE6mEw%2B6%2FhcCbRDaCPGJf3aLCdYyfzuqRx6iY2b%2F04BmXgnQoTrGhm1FBQ%2BOVA5mbMogKlHFcDgw%3D%3D";

    private static String joinParams(String... str) {
        StringBuilder builder = new StringBuilder(str[0]);
        for (int i = 1; i < str.length; i ++) {
            builder.append("&").append(str[i]);
        }
        return builder.toString();
    }

    public static void main(String[] args) {

        ExecutorService service = Executors.newFixedThreadPool(3);

        Future<List<Item>> korFuture = service.submit(getKorServiceCallable());
        Future<List<Item>> withTourFuture = service.submit(getWithTourServiceCallable());
        Future<List<Item>> greenTourFuture = service.submit(getGreenTourServiceCallable());

        ArrayList<Item> result = new ArrayList<>();
        try {
            List<Item> korTourList = korFuture.get();
            List<Item> withTourList = withTourFuture.get();
            List<Item> greenTourList = greenTourFuture.get();
            for (Item item : withTourList) {
                korTourList.remove(item);
            }

            result.addAll(korTourList);
            result.addAll(withTourList);
            result.addAll(greenTourList);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        System.out.println(result);

        service.shutdown();
        try {
            if (!service.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                service.shutdownNow();
            }
        } catch (InterruptedException e) {
            service.shutdownNow();
        }
    }

    private static Callable<List<Item>> getKorServiceCallable() {
        final long today = today().getTime();
        final long start = start().getTime();
        URL url = null;
        try {
            url = new URL(BASE_URL + "/KorService/areaBasedList?" + joinParams("ServiceKey=" + SERVICE_KEY,
                    "MobileOS=ETC",
                    "MobileApp=TEST",
                    "pageNo=1",
                    "numOfRows=30",
                    "arrange=C",
                    "_type=json"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return createTourAPICallable(url, start, today, item -> {
                // ignore
        });
    }

    private static Callable<List<Item>> getWithTourServiceCallable() {
        final long today = today().getTime();
        final long start = start().getTime();
        URL url = null;
        try {
            url = new URL(BASE_URL + "/KorWithService/areaBasedList?" + joinParams("ServiceKey=" + SERVICE_KEY,
                    "MobileOS=ETC",
                    "MobileApp=TEST",
                    "pageNo=1",
                    "numOfRows=30",
                    "arrange=C",
                    "_type=json"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return createTourAPICallable(url, start, today, item -> item.setWithTour(true));
    }

    private static Callable<List<Item>> getGreenTourServiceCallable() {
        final long today = today().getTime();
        final long start = start().getTime();
        URL url = null;
        try {
            url = new URL(BASE_URL + "/GreenTourService/areaBasedList?" + joinParams("ServiceKey=" + SERVICE_KEY,
                    "MobileOS=ETC",
                    "MobileApp=TEST",
                    "pageNo=1",
                    "numOfRows=30",
                    "arrange=C",
                    "_type=json"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return createTourAPICallable(url, start, today, item -> item.setGreenTour(true));
    }

    private static Callable<List<Item>> createTourAPICallable(final URL url, final long start, final long end, final Callback<Item> callback) {
        return () -> {
            // TODO: 리스트 요청을 더 할지 말지에 대한 판단을 하는 로직이 필요.
            //       1. 요청을 더하기위해서는 URL 이 변해야 하므로 외부에서 URL 빌더를 주입하자.
            //       2. 결과 즉, 전체 리스트 아이템의 개수와 현재 페이지 정보를 URL빌더에 넘겨주어 다음 URL이 있는 지 확인이 필요하다.
            //       3. 정의된 기간을 벗어나는 경우의 아이템이 있다면 더이상 요청을 하지 않도록 하고 리턴하자~~!
            boolean noNeedToFetchMore = true;
            ObjectMapper mapper = new ObjectMapper();
            JsonNode list = mapper.readTree(url).get("response").get("body").get("items").get("item");
            List<Item> result = mapper.readValue(list.toString(), new TypeReference<List<Item>>(){});
            ArrayList<Item> filteredList = new ArrayList<>();
            for (Item item : result) {
                if (item.getModifiedDate() >= start && item.getModifiedDate() < end) {
                    callback.on(item);
                    filteredList.add(item);
                } else {
                    noNeedToFetchMore = false;
                }
            }
            return filteredList;
        };
    }

    interface Callback<T> {
        void on(T item);
    }

}
