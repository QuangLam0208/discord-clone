package hcmute.edu.vn.discord.entity.enums;

public enum EPermission {
    // Nhóm General
    ADMIN,
    VIEW_CHANNELS,
    MANAGE_SERVER,

    // Nhóm Channel
    MANAGE_CHANNELS,
    SEND_MESSAGES,

    // Nhóm Moderation
    KICK_MEMBERS,
    BAN_MEMBERS;

    public String getCode() {
        return name();
    }
}