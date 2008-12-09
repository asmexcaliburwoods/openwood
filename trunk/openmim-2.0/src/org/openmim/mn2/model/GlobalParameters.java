package org.openmim.mn2.model;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;

public class GlobalParameters {
    private Font proportionalWidthFont;
    private Font fixedWidthFont;
    private String realNameForIRC="...";
    private boolean realNameForIRCUsed;
    private String firstName="";
    private String lastName="";
    private List<String> nickNameList=new ArrayList<String>();
    {
        nickNameList.add("_Anonymous_");
    }
    private String identdUserName="user";

    public void setIdentdUserName(String identdUserName) {
        this.identdUserName = identdUserName;
    }

    public void setNickNameList(List<String> nickNameList) {
        this.nickNameList = nickNameList;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setRealNameForIRC(String realNameForIRC) {
        this.realNameForIRC = realNameForIRC;
    }

    public void setRealNameForIRCUsed(boolean realNameForIRCUsed) {
        this.realNameForIRCUsed = realNameForIRCUsed;
    }

    public Font getProportionalWidthFont() {
        return proportionalWidthFont;
    }

    public void setProportionalWidthFont(Font proportionalWidthFont) {
        this.proportionalWidthFont = proportionalWidthFont;
    }
    public Font getFixedWidthFont() {
        return fixedWidthFont;
    }

    public void setFixedWidthFont(Font fixedWidthFont) {
        this.fixedWidthFont = fixedWidthFont;
    }

    public String getRealNameForIRC() {
        return realNameForIRC;
    }

    public boolean isRealNameForIRCUsed() {
        return realNameForIRCUsed;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public List<String> getNickNameList() {
        return nickNameList;
    }

    public String getIdentdUserName() {
        return identdUserName;
    }
}
