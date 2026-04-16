package com.example.netassist;

/**
 * 快捷指令数据模型
 */
public class QuickCommand {
    private String name;
    private String data;
    private String description;

    public QuickCommand(String name, String data) {
        this.name = name;
        this.data = data;
    }

    public QuickCommand(String name, String data, String description) {
        this.name = name;
        this.data = data;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
