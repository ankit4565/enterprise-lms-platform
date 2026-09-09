package com.enterprise.mediaservice.entity;

public enum MediaContextType {
    COURSE_VIDEO,
    PROMO,
    THUMBNAIL,
    ASSIGNMENT,
    CHAT_ATTACHMENT,
    AVATAR;

    public boolean isVideo() {
        return this == COURSE_VIDEO || this == PROMO;
    }
}
