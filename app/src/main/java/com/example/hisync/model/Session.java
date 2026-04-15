package com.example.hisync.model;

import com.google.firebase.Timestamp;

public class Session {
    public String id;
    public String songId;
    public String songTitle;
    public Timestamp date;
    public String createdBy;

    public Session() {}

    public Session(String id, String songId, String songTitle, Timestamp date, String createdBy) {
        this.id = id;
        this.songId = songId;
        this.songTitle = songTitle;
        this.date = date;
        this.createdBy = createdBy;
    }
}