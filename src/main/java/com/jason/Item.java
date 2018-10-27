package com.jason;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Item {
    @JsonProperty("modifiedtime")
    @JsonDeserialize(using = Main.DateDeserialize.class)
    private long modifiedDate;
    @JsonProperty("createdtime")
    @JsonDeserialize(using = Main.DateDeserialize.class)
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

    public boolean isOrigin() {
        return this.isGreenTour || this.isWithTour;
    }
}