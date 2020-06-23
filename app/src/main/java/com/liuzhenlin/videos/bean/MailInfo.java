/*
 * Created on 2018/04/14.
 * Copyright © 2018 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.bean;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import java.util.Arrays;
import java.util.Properties;

/**
 * @author 刘振林
 */
public class MailInfo {

    /** 邮件发送者的服务器IP */
    private String mailServerHost;
    /** 邮件发送者的服务器端口 */
    private String mailServerPort;

    /** 是否需要身份验证 */
    private boolean validate = true;

    /** 邮件发送者的用户名 */
    private String userName;
    /** 邮件发送者的密码 */
    private String password;

    /** 邮件发送者的地址 */
    private String fromAddress;
    /** 邮件接收者的地址 */
    private String toAddress;

    /** 邮件主题 */
    private String title;
    /** 邮件正文 */
    private String text;
    /** 邮件正文所对应图片的路径 */
    private String textRelatedImagePath;
    /** 邮件附件的路径 */
    private String[] attachmentPaths;

    /**
     * 获得邮件会话属性
     */
    public Properties getProperties() {
        Properties p = new Properties();
        p.put("mail.smtp.host", mailServerHost);
        p.put("mail.smtp.port", mailServerPort);
        p.put("mail.smtp.auth", validate ? "true" : "false");
        p.put("mail.smtp.ssl.enable", "true");
        return p;
    }

    public MailInfo() {
    }

    public MailInfo(String mailServerHost, String mailServerPort,
                    boolean validate, String userName, String password,
                    String fromAddress, String toAddress,
                    String title, String text, String textRelatedImagePath, String[] attachmentPaths) {
        this.mailServerHost = mailServerHost;
        this.mailServerPort = mailServerPort;
        this.validate = validate;
        this.userName = userName;
        this.password = password;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.title = title;
        this.text = text;
        this.textRelatedImagePath = textRelatedImagePath;
        this.attachmentPaths = attachmentPaths;
    }

    public String getMailServerHost() {
        return mailServerHost;
    }

    public void setMailServerHost(String mailServerHost) {
        this.mailServerHost = mailServerHost;
    }

    public String getMailServerPort() {
        return mailServerPort;
    }

    public void setMailServerPort(String mailServerPort) {
        this.mailServerPort = mailServerPort;
    }

    public boolean isValidate() {
        return validate;
    }

    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTextRelatedImagePath() {
        return textRelatedImagePath;
    }

    public void setTextRelatedImagePath(String textRelatedImagePath) {
        this.textRelatedImagePath = textRelatedImagePath;
    }

    public String[] getAttachmentPaths() {
        return attachmentPaths;
    }

    public void setAttachmentPaths(String[] attachmentPaths) {
        this.attachmentPaths = attachmentPaths;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        MailInfo mailInfo = (MailInfo) o;
        return validate == mailInfo.validate &&
                ObjectsCompat.equals(mailServerHost, mailInfo.mailServerHost) &&
                ObjectsCompat.equals(mailServerPort, mailInfo.mailServerPort) &&
                ObjectsCompat.equals(userName, mailInfo.userName) &&
                ObjectsCompat.equals(password, mailInfo.password) &&
                ObjectsCompat.equals(fromAddress, mailInfo.fromAddress) &&
                ObjectsCompat.equals(toAddress, mailInfo.toAddress) &&
                ObjectsCompat.equals(title, mailInfo.title) &&
                ObjectsCompat.equals(text, mailInfo.text) &&
                ObjectsCompat.equals(textRelatedImagePath, mailInfo.textRelatedImagePath) &&
                Arrays.equals(attachmentPaths, mailInfo.attachmentPaths);
    }

    @Override
    public int hashCode() {
        int result = ObjectsCompat.hash(
                mailServerHost, mailServerPort,
                validate,
                userName, password,
                fromAddress, toAddress,
                title, text, textRelatedImagePath);
        result = 31 * result + Arrays.hashCode(attachmentPaths);
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "MailInfo{" +
                "mailServerHost='" + mailServerHost + '\'' +
                ", mailServerPort='" + mailServerPort + '\'' +
                ", validate=" + validate +
                ", userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", fromAddress='" + fromAddress + '\'' +
                ", toAddress='" + toAddress + '\'' +
                ", title='" + title + '\'' +
                ", text='" + text + '\'' +
                ", textRelatedImagePath='" + textRelatedImagePath + '\'' +
                ", attachmentPaths=" + Arrays.toString(attachmentPaths) +
                '}';
    }
}
