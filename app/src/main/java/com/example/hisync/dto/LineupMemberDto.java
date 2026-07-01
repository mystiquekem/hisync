package com.example.hisync.dto;

public class LineupMemberDto {
    private long userId;
    private String displayName;
    private String instrument;

    public LineupMemberDto() {}

    public LineupMemberDto(long userId, String displayName, String instrument) {
        this.userId = userId;
        this.displayName = displayName;
        this.instrument = instrument;
    }

    public long getUserId()        { return userId; }
    public String getDisplayName() { return displayName; }
    public String getInstrument()  { return instrument; }

    public void setUserId(long userId)           { this.userId = userId; }
    public void setDisplayName(String name)      { this.displayName = name; }
    public void setInstrument(String instrument) { this.instrument = instrument; }
}