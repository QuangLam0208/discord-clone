package hcmute.edu.vn.discord.entity.enums;

public enum EPermission {
    // Nhóm General
    ADMIN("ADMIN"),
    VIEW_CHANNELS("VIEW_CHANNELS"),
    MANAGE_SERVER("MANAGE_SERVER"),

    // Nhóm Channel
    MANAGE_CHANNELS("MANAGE_CHANNELS"),
    SEND_MESSAGES("SEND_MESSAGES"),

    // Nhóm Moderation
    KICK_MEMBERS("KICK_MEMBERS"),
    BAN_MEMBERS("BAN_MEMBERS");

    private final String code;

    EPermission(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}