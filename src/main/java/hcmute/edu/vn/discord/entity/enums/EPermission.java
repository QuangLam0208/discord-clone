package hcmute.edu.vn.discord.entity.enums;

import lombok.Getter;

@Getter
public enum EPermission {
    // General server permissions
    ADMIN("Full access"),
    VIEW_CHANNELS("Xem kênh (không tính kênh riêng tư)."),
    MANAGE_CHANNELS("Tạo, chỉnh sửa, hoặc xóa kênh."),
    MANAGE_ROLES("Cho phép thành viên tạo vai trò mới và chỉnh sửa hoặc xóa vai trò " +
            "thấp hơn vai trò cao nhất của họ. Đồng thời cho phép thành viên thay đổi quyền hạn" +
            " của các kênh riêng lẻ mà họ có quyền truy cập."),
    CREATE_EXPRESSIONS("Thêm emoji, sticker và âm thanh tùy chỉnh trong máy chủ."),
    MANAGE_EXPRESSIONS("Cho phép thành viên chỉnh sửa hoặc xóa emoji, " +
            "sticker và âm thanh tùy chỉnh trong máy chủ này."),
    VIEW_AUDIT_LOG("Xem lịch sử thay đổi trong máy chủ này."),
    MANAGE_SERVER("Thay đổi tên máy chủ, xem tất cả các lời mời."),

    // Membership permissions
    CREATE_INVITE("Mời người mới vào máy chủ này."),
    CHANGE_NICKNAME("Cho phép thành viên tùy ý thay đổi biệt trong máy chủ này."),
    MANAGE_NICKNAME("Cho phép thành viên thay đổi biệt danh của thành viên khác."),
    KICK_APPROVE_REJECT_MEMBERS("Đuổi sẽ xóa các thành viên khác khỏi máy chủ này. " +
            "Thành viên bị đuổi có thể tham gia lại nếu nhận được lời mời khác. " +
            "Nếu máy chủ kích hoạt MEMBER_REQUIREMENTS, quyền này cho phép chấp thuận hoặc " +
            "từ chối các thành viên yêu cầu tham gia vào máy chủ."),
    BAN_MEMBERS("Cấm vĩnh viễn và xóa lịch sử tin nhắn của các thành viên khác từ máy chủ này."),
    TIME_OUT_MEMBERS("Khi bạn đặt một người dùng về trạng thái chờ (timeout) " +
            "thì họ sẽ không thể gửi tin nhắn trò chuyện, trả lời chủ để thảo luận, tương tác với tin nhắn, " +
            "hoặc nói trong kênh thoại hoặc kênh stage."),

    // Text channel permissions
    // Channel
    SEND_MESSAGES("Gửi tin nhắn trong các kênh văn bản."),
    EMBED_LINK("Hiển thị nội dung nhúng của liên kết do thành viên chia sẻ trong các kênh chat."),
    ATTACH_FILES("Tải lên tệp hoặc tệp media trong kênh chat."),
    ADD_REACTIONS("Cho phép thành viên thêm hiệu ứng tương tác emoji mới vào tin nhắn. " +
            "Nếu không cấp quyền này, thành viên vẫn có thể sử dụng các tương tác có sẵn trong tin nhắn."),
    // Moderation
    MENTION_EVERYONE_HERE_ALLROLES("Dùng @everyone (tất cả thành viên trong máy chủ) hoặc " +
            "@here (chỉ các thành viên đang trực tuyến trong kênh đó). " +
            "Thành viên cũng có thể sử dụng @mention tất cả các vai trò, " +
            "ngay cả khi quyền \"ALLOW_ANYONE_TO_MENTION_THIS_ROLE\" của vai trò không được cấp."),
    MANAGE_MESSAGES("Xóa tin nhắn của các thành viên khác."),
    PIN_MESSAGES("Ghim hoặc bỏ ghim tin nhắn bất kỳ."),
    READ_MESSAGE_HISTORY("Đọc các tin nhắn đã được gửi trước đó trong kênh. " +
            "Nếu không, sẽ chỉ nhìn thấy các tin nhắn được gửi khi đang trực tuyến.");

    private final String description;

    EPermission(String description) {
        this.description = description;
    }

    public String getCode() {
        return name();
    }

}