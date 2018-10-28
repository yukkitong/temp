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
        calendar.add(Calendar.DAY_OF_MONTH, -3);
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
        System.out.println(result.size());

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
        TourURL.TourURLBuilder builder = new TourURL.TourURLBuilder();
        builder.url(BASE_URL + "/KorService/areaBasedList")
                .serviceKey(SERVICE_KEY)
                .mobileOs("ETC")
                .mobileApp("TEST")
                .pageNo(1)
                .numOfRows(30);
        return createTourAPICallable(builder, start, today);
    }

    private static Callable<List<Item>> getWithTourServiceCallable() {
        final long today = today().getTime();
        final long start = start().getTime();
        TourURL.TourURLBuilder builder = new TourURL.TourURLBuilder();
        builder.url(BASE_URL + "/KorWithService/areaBasedList")
                .serviceKey(SERVICE_KEY)
                .mobileOs("ETC")
                .mobileApp("TEST")
                .pageNo(1)
                .numOfRows(30);
        // , item -> item.setWithTour(true)
        return createTourAPICallable(builder, start, today);
    }

    private static Callable<List<Item>> getGreenTourServiceCallable() {
        final long today = today().getTime();
        final long start = start().getTime();
        TourURL.TourURLBuilder builder = new TourURL.TourURLBuilder();
        builder.url(BASE_URL + "/GreenTourService/areaBasedList")
                .serviceKey(SERVICE_KEY)
                .mobileOs("ETC")
                .mobileApp("TEST")
                .pageNo(1)
                .numOfRows(30);
        // , item -> item.setGreenTour(true)
        return createTourAPICallable(builder, start, today);
    }

    private static Callable<List<Item>> createTourAPICallable(final TourURL.TourURLBuilder urlBuilder, final long start, final long end) {
        ObjectMapper mapper = new ObjectMapper();
        return () -> {
            ArrayList<Item> filteredList = new ArrayList<>();
            boolean needToFetchMore = true;
            TypeReference<List<Item>> typeReference = new TypeReference<List<Item>>() {};

            while (needToFetchMore) {
                // TODO: totalCount 로 다음 리스트가 있는지 여부 판단하기 Math.ceil((double) totalCount / rows ) => total page no
                // TODO: Refactoring...
                TourURL url = urlBuilder.build();

                JsonNode root = mapper.readTree(url.get());
                int totalCount = root.findPath("totalCount").asInt();
                if ((int) Math.ceil((double) totalCount / url.getNumOfRows()) < url.getPageNo()) {
                    needToFetchMore = false;
                    break;
                }

                String itemString = root.findPath("item").toString();
                List<Item> items = mapper.readValue(itemString, typeReference);

                for (Item item : items) {
                    if (item.getModifiedDate() >= start && item.getModifiedDate() < end) {
                        filteredList.add(item);
                    } else {
                        needToFetchMore = false;
                    }
                }

                if (needToFetchMore) {
                    urlBuilder.pageNo(url.getPageNo() + 1);
                }
            }
            return filteredList;
        };
    }

    interface Callback<T> {
        void on(T item);
    }

}
