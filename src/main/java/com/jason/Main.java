package com.jason;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

    private static String join(String del, String... str) {
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
            for (int i = 0, size = withTourList.size(); i < size; i ++) {
                if (korTourList.contains(withTourList.get(i))) {
                    korTourList.remove(withTourList.get(i));
                }
            }

            jsonResult.addAll(korTourList);
            //jsonResult.addAll(withTourList);
            //jsonResult.addAll(greenTourFuture.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        jsonResult = new ArrayList<Item>(new HashSet<Item>(jsonResult));

        System.out.println("start " + start());
        System.out.println("today " + today());

        System.out.println(Arrays.toString(jsonResult.toArray()).replaceAll(",", ",\n"));
        System.out.println(jsonResult.size());

        service.shutdown();
    }

    private static Callable<List<Item>> getKorServiceCallable() {
        final long today = today().getTime();
        final long start = start().getTime();
        return new Callable<List<Item>>() {
            public List<Item> call() throws Exception {
                URL url = new URL(END_POINT + "/KorService/areaBasedList?" + join("&",
                        "ServiceKey=" + SERVICE_KEY,
                        "MobileOS=ETC",
                        "MobileApp=TEST",
                        "pageNo=1",
                        "numOfRows=500",
                        "arrange=C",
                        "_type=json"));

                ObjectMapper mapper = new ObjectMapper();
                JsonNode list = mapper.readTree(url).get("response").get("body").get("items").get("item");
                List<Item> result = mapper.readValue(list.toString(), new TypeReference<List<Item>>(){});
                ArrayList<Item> filteredList = new ArrayList<Item>();
                for (Item item : result) {
                    if (item.getModifiedDate() >= start && item.getModifiedDate() < today) {
                        filteredList.add(item);
                    }
                }
                return filteredList;
            }
        };
    }

    private static Callable<List<Item>> getWithTourServiceCallable() {
        final long today = today().getTime();
        final long start = start().getTime();
        return new Callable<List<Item>>() {
            public List<Item> call() throws Exception {
                URL url = new URL(END_POINT + "/KorWithService/areaBasedList?" + join("&",
                        "ServiceKey=" + SERVICE_KEY,
                        "MobileOS=ETC",
                        "MobileApp=TEST",
                        "pageNo=1",
                        "numOfRows=500",
                        "arrange=C",
                        "_type=json"));

                ObjectMapper mapper = new ObjectMapper();
                JsonNode list = mapper.readTree(url).get("response").get("body").get("items").get("item");
                List<Item> result = mapper.readValue(list.toString(), new TypeReference<List<Item>>(){});
                ArrayList<Item> filteredList = new ArrayList<Item>();
                for (Item item : result) {
                    if (item.getModifiedDate() >= start && item.getModifiedDate() < today) {
                        item.setWithTour(true);
                        filteredList.add(item);
                    }
                }
                return filteredList;
            }
        };
    }

    private static Callable<List<Item>> getGreenTourServiceCallable() {
        final long today = today().getTime();
        final long start = start().getTime();
        return new Callable<List<Item>>() {
            public List<Item> call() throws Exception {
                URL url = new URL(END_POINT + "/GreenTourService/areaBasedList?" + join("&",
                        "ServiceKey=" + SERVICE_KEY,
                        "MobileOS=ETC",
                        "MobileApp=TEST",
                        "pageNo=1",
                        "numOfRows=500",
                        "arrange=C",
                        "_type=json"));

                ObjectMapper mapper = new ObjectMapper();
                JsonNode list = mapper.readTree(url).get("response").get("body").get("items").get("item");
                List<Item> result = mapper.readValue(list.toString(), new TypeReference<List<Item>>(){});
                ArrayList<Item> filteredList = new ArrayList<Item>();
                for (Item item : result) {
                    if (item.getModifiedDate() >= start && item.getModifiedDate() < today) {
                        item.setGreenTour(true);
                        filteredList.add(item);
                    }
                }
                return filteredList;
            }
        };
    }


    public static class DateDeserialize extends JsonDeserializer<Long> {
        private DateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        public Long deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException, JsonProcessingException {
            String dateString = deserializationContext.readValue(jsonParser, String.class);
            try {
                return format.parse(dateString).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return Long.MAX_VALUE;
        }
    }

}
