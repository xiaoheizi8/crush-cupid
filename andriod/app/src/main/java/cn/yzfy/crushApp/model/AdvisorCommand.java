package cn.yzfy.crushApp.model;

/** 军师模式子命令（字段与后端 AdvisorDescriptor 对齐） */
public class AdvisorCommand {
    public String name;
    public String trigger;
    public String title;
    public String description;
    public String promptName;
    public boolean requiresCrush;
    /** emoji 图标，如 🤵 */
    public String icon;
    /** 分组：ADVISOR 军师 / MIRROR 照镜子 / SIMULATOR 模拟器 */
    public String group;
    /** 是否需要用户输入材料（聊天记录/草稿/场景） */
    public boolean needsInput;
    /** 输入对话框的提示语 */
    public String inputHint;

    public boolean isMirror() {
        return "MIRROR".equals(group);
    }
}
