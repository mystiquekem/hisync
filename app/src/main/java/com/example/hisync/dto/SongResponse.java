package com.example.hisync.dto;

public class SongResponse {
    private long id;
    private String youtubeId;
    private String title;
    private String thumbnailUrl;
    private String addedByName;

    public long getId()            { return id; }
    public String getYoutubeId()   { return youtubeId; }
    public String getTitle()       { return title; }
    public String getThumbnailUrl(){ return thumbnailUrl; }
    public String getAddedByName() { return addedByName; }
}