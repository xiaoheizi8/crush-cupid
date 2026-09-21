package cn.yzfy.crushApp.model;

/** 聊天气泡（UI 层） */
public class ChatMessage {
    public enum Role {USER, ASSISTANT}

    public enum Kind {TEXT, STICKER, IMAGE}

    public Role role = Role.ASSISTANT;
    public Kind kind = Kind.TEXT;
    public String text = "";
    public String imageUrl;   // 图片地址（远端或本地）
    public long ts = System.currentTimeMillis();
    public boolean pending;

    public ChatMessage() {
    }

    public static ChatMessage text(Role role, String content) {
        ChatMessage m = new ChatMessage();
        m.role = role;
        m.text = content == null ? "" : content;
        return m;
    }

    public static ChatMessage image(Role role, String url) {
        ChatMessage m = new ChatMessage();
        m.role = role;
        m.kind = Kind.IMAGE;
        m.imageUrl = url;
        return m;
    }

    public static ChatMessage sticker(Role role, String url) {
        ChatMessage m = new ChatMessage();
        m.role = role;
        m.kind = Kind.STICKER;
        m.imageUrl = url;
        return m;
    }

    public String timeLabel() {
        return String.format(java.util.Locale.getDefault(), "%1$tH:%1$tM", new java.util.Date(ts));
    }
}