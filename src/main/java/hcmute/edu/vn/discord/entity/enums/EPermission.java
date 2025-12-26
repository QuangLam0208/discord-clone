package hcmute.edu.vn.discord.entity.enums;

import lombok.Getter;

@Getter
public enum EPermission {
    // General
    ADMIN("Full access"),
    VIEW_CHANNELS("View channels"),
    MANAGE_SERVER("Manage server settings"),

    // Channel
    MANAGE_CHANNELS("Create/update/delete channels"),
    SEND_MESSAGES("Send messages"),

    // Moderation
    KICK_MEMBERS("Kick members"),
    BAN_MEMBERS("Ban members");

    private final String description;

    EPermission(String description) {
        this.description = description;
    }

    public String getCode() {
        return name();
    }

}