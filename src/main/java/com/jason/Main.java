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

    private static final String END_POINT = "http://api.visitkorea.or.kr/openapi/service/rest";

    private static final String SERVICE_KEY = "A%2BycgFhk2eYE6mEw%2B6%2FhcCbRDaCPGJf3aLCdYyfzuqRx6iY2b%2F04BmXgnQoTrGhm1FBQ%2BOVA5mbMogKlHFcDgw%3D%3D";

    private static String joinParams(String del, String... str) {
        StringBuilder builder = new StringBuilder(str[0]);
        for (int i = 1; i < str.length; i ++) {
            builder.append(del).append(str[i]);
        }
        return builder.toString();
    }

    public static void main(String[] args) {

        ExecutorService service = Executors.newFixedThreadPool(3);

        Future<List<Item>> korFuture = service.submit(getKorServiceCallable());
        Future<List<Item>> withTourFuture = service.submit(getWithTourServiceCallable());
        Future<List<Item>> greenTourFuture = service.submit(getGreenTourServiceCallable());

        List<Item> jsonResult = new ArrayList<Item>();
        try {
            List<Item> korTourList = korFuture.get();
            List<Item> withTourList = withTourFuture.get();
            for (Item item : withTourList) {
                if (korTourList.contains(item)) {
                    korTourList.remove(item);
                }
            }

            jsonResult.addAll(korTourList);
            jsonResult.addAll(withTourList);
            jsonResult.addAll(greenTourFuture.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        System.out.println("start " + start());
        System.out.println("today " + today());

        System.out.println(Arrays.toString(jsonResult.toArray()).replaceAll(",", ",\n"));
        System.out.println(jsonResult.size());

        service.shutdown();
        try {
            if (!service.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
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
            url = new URL(END_POINT + "/KorService/areaBasedList?" + joinParams("&",
                    "ServiceKey=" + SERVICE_KEY,
                    "MobileOS=ETC",
                    "MobileApp=TEST",
                    "pageNo=1",
                    "numOfRows=30",
                    "arrange=C",
                    "_type=json"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return createTourAPICallable(url, start, today, new Callback<Item>() {
            public void on(Item item) {
                // ignore
            }
        });
    }

    private static Callable<List<Item>> getWithTourServiceCallable() {
        final long today = today().getTime();
        final long start = start().getTime();
        URL url = null;
        try {
            url = new URL(END_POINT + "/KorWithService/areaBasedList?" + joinParams("&",
                    "ServiceKey=" + SERVICE_KEY,
                    "MobileOS=ETC",
                    "MobileApp=TEST",
                    "pageNo=1",
                    "numOfRows=30",
                    "arrange=C",
                    "_type=json"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return createTourAPICallable(url, start, today, new Callback<Item>() {
            public void on(Item item) {
                item.setWithTour(true);
            }
        });
    }

    private static Callable<List<Item>> getGreenTourServiceCallable() {
        final long today = today().getTime();
        final long start = start().getTime();
        URL url = null;
        try {
            url = new URL(END_POINT + "/GreenTourService/areaBasedList?" + joinParams("&",
                    "ServiceKey=" + SERVICE_KEY,
                    "MobileOS=ETC",
                    "MobileApp=TEST",
                    "pageNo=1",
                    "numOfRows=30",
                    "arrange=C",
                    "_type=json"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return createTourAPICallable(url, start, today, new Callback<Item>() {
            public void on(Item item) {
                item.setGreenTour(true);
            }
        });
    }

    private static Callable<List<Item>> createTourAPICallable(final URL url, final long start, final long end, final Callback<Item> callback) {
        return new Callable<List<Item>>() {
            public List<Item> call() throws Exception {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode list = mapper.readTree(url).get("response").get("body").get("items").get("item");
                List<Item> result = mapper.readValue(list.toString(), new TypeReference<List<Item>>(){});
                ArrayList<Item> filteredList = new ArrayList<Item>();
                for (Item item : result) {
                    if (item.getModifiedDate() >= start && item.getModifiedDate() < end) {
                        callback.on(item);
                        filteredList.add(item);
                    }
                }
                return filteredList;
            }
        };
    }

    interface Callback<T> {
        void on(T item);
    }

}
