package com.example.hisync.dto;

import java.util.List;

public class LineupResponse {
    private long id;
    private long bandId;
    private String songTitle;
    private String youtubeId;
    private String thumbnailUrl;
    private String createdByName;
    private List<LineupMemberDto> members;

    public long getId()                        { return id; }
    public long getBandId()                    { return bandId; }
    public String getSongTitle()               { return songTitle; }
    public String getYoutubeId()               { return youtubeId; }
    public String getThumbnailUrl()            { return thumbnailUrl; }
    public String getCreatedByName()           { return createdByName; }
    public List<LineupMemberDto> getMembers()  { return members; }
}