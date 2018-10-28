package com.jason;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Item {
    @JsonProperty("modifiedtime")
    @JsonDeserialize(using = DateDeserialize.class)
    private long modifiedDate;
    @JsonProperty("createdtime")
    @JsonDeserialize(using = DateDeserialize.class)
    private long createdDate;
    @JsonProperty("contentid")
    private long contentId;
    @JsonProperty("contenttypeid")
    private int contentTypeId;
    private String title;
    @JsonProperty("firstimage")
    private String image;
    @JsonProperty("firstimage2")
    private String thumbnail;
    @JsonProperty("addr1")
    private String address1;
    @JsonProperty("addr2")
    private String address2;
    @JsonProperty("cat1")
    private String category1;
    @JsonProperty("cat2")
    private String category2;
    @JsonProperty("cat3")
    private String category3;


    @JsonIgnore
    private List<String> departments;
    @JsonIgnore
    private List<String> tags;
    @JsonIgnore
    private boolean isWithTour;
    @JsonIgnore
    private boolean isGreenTour;

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof Item)) return false;
        if (this == o) return true;
        return contentId == ((Item) o).getContentId()
                && contentTypeId == ((Item) o).getContentTypeId()
                && title.equals(((Item) o).getTitle());
    }

    static class DateDeserialize extends JsonDeserializer<Long> {
        private DateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        public Long deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException, JsonProcessingException {
            ObjectCodec codec = jsonParser.getCodec();
            JsonNode node = codec.readTree(jsonParser);
            String dateString = node.asText();
            try {
                return format.parse(dateString).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return Long.MAX_VALUE;
        }
    }
}