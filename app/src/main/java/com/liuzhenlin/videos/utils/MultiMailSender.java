/*
 * Created on 2018/04/11.
 * Copyright © 2018 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.liuzhenlin.videos.model.MailInfo;

import java.io.UnsupportedEncodingException;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

/**
 * 多附件发送邮件类
 * <p>
 * 以下是利用JavaMail的API来创建和发送邮件
 *
 * @author 刘振林
 */
public class MultiMailSender {
    private static final String TAG = "MultiMailSender";

    private static final boolean DEBUG_MAIL = false;

    private static final String CHARSET = "utf-8";
    private static final String ENCODING = "B"; // base64

    private final MailInfo mMailInfo;
    private final Message mMessage;

    /**
     * @param mailInfo 待发送的邮件信息
     */
    public MultiMailSender(@NonNull final MailInfo mailInfo) {
        mMailInfo = mailInfo;
        // 根据邮件会话属性和密码验证器构造一个发送邮件的session
        Session session = Session.getDefaultInstance(mailInfo.getProperties(), new Authenticator() {
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailInfo.getFromAddress(), mailInfo.getPassword());
            }
        });
        // 开启Session的debug模式，这样就可以查看到程序发送email的运行状态
        session.setDebug(DEBUG_MAIL);

        mMessage = new MimeMessage(session);
        try {
            // 创建邮件发送者地址
            Address from = new InternetAddress(mMailInfo.getFromAddress());
            // 设置邮件消息的发送者
            mMessage.setFrom(new InternetAddress(MimeUtility.encodeText(
                    mMailInfo.getUserName() + " <" + from + "> ", CHARSET, ENCODING)));
            // 创建邮件的接收者地址，并设置到邮件消息中
            Address to = new InternetAddress(mMailInfo.getToAddress());
            // 设置邮件消息的接收者（Message.RecipientType.TO 属性表示接收者为直接接收邮件的人）
            mMessage.setRecipient(Message.RecipientType.TO, to);
        } catch (MessagingException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送纯文本邮件
     */
    public boolean sendTextMail() {
        try {
            // 设置邮件标题
            mMessage.setSubject(mMailInfo.getTitle());
            // 设置邮件内容
            mMessage.setText(mMailInfo.getText());

            Transport.send(mMessage);
            return true;
        } catch (MessagingException e) {
            Log.e(TAG, "发送纯文本邮件失败");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 发送正文带图片引用的邮件
     */
    public boolean sendImageRelatedMail() {
        try {
            // 设置邮件标题
            mMessage.setSubject(mMailInfo.getTitle());
            // 设置邮件正文，为了避免邮件正文中文乱码问题，指定字符集为UTF-8
            MimeBodyPart text = new MimeBodyPart();
            text.setContent(mMailInfo.getText(), "text/html;charset=" + CHARSET);

            // 设置邮件的图片
            MimeBodyPart image = new MimeBodyPart();
            DataHandler dh = new DataHandler(new FileDataSource(mMailInfo.getTextRelatedImagePath()));
            image.setDataHandler(dh);
            image.setContentID(MimeUtility.encodeText(dh.getName(), CHARSET, ENCODING));

            // 描述数据关系
            MimeMultipart mm = new MimeMultipart();
            mm.addBodyPart(text);
            mm.addBodyPart(image);
            mm.setSubType("related");
            mMessage.setContent(mm);

            Transport.send(mMessage);
            return true;
        } catch (MessagingException | UnsupportedEncodingException e) {
            Log.e(TAG, "发送正文带图片引用的邮件失败");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 发送包含附件的邮件
     */
    public boolean sendAttachmentMail() {
        try {
            // 标题
            mMessage.setSubject(mMailInfo.getTitle());
            // 正文
            MimeBodyPart text = new MimeBodyPart();
            text.setContent(mMailInfo.getText(), "text/html;charset=" + CHARSET);

            final String[] paths = mMailInfo.getAttachmentPaths();
            MimeBodyPart[] attaches = new MimeBodyPart[paths.length];
            for (int i = 0; i < attaches.length; i++) {
                // 邮件附件
                attaches[i] = new MimeBodyPart();
                DataHandler dh = new DataHandler(new FileDataSource(paths[i]));
                attaches[i].setDataHandler(dh);
                attaches[i].setFileName(MimeUtility.encodeText(dh.getName(), CHARSET, ENCODING));
            }

            // 创建容器描述数据关系
            MimeMultipart content = new MimeMultipart();
            content.addBodyPart(text);
            for (MimeBodyPart attach : attaches)
                content.addBodyPart(attach);
            content.setSubType("mixed");
            mMessage.setContent(content);

            Transport.send(mMessage);
            return true;
        } catch (MessagingException | UnsupportedEncodingException e) {
            Log.e(TAG, "发送包含附件的邮件失败");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 发送正文带图片引用且包含附件的邮件
     */
    public boolean sendImageRelatedAndAttachmentMixedMail() {
        try {
            // 标题
            mMessage.setSubject(mMailInfo.getTitle());
            // 正文
            MimeBodyPart text = new MimeBodyPart();
            text.setContent(mMailInfo.getText(), "text/html;charset=" + CHARSET);

            // 图片
            MimeBodyPart image = new MimeBodyPart();
            DataHandler dh = new DataHandler(new FileDataSource(mMailInfo.getTextRelatedImagePath()));
            image.setDataHandler(dh);
            image.setContentID(MimeUtility.encodeText(dh.getName(), CHARSET, ENCODING));

            // 附件
            final String[] attachPaths = mMailInfo.getAttachmentPaths();
            MimeBodyPart[] attaches = new MimeBodyPart[attachPaths.length];
            for (int i = 0; i < attaches.length; i++) {
                // 创建邮件附件
                attaches[i] = new MimeBodyPart();
                DataHandler dh2 = new DataHandler(new FileDataSource(attachPaths[i]));
                attaches[i].setDataHandler(dh2);
                attaches[i].setFileName(MimeUtility.encodeText(dh2.getName(), CHARSET, ENCODING));
            }

            // 描述关系：正文和图片
            MimeMultipart mm_text_image = new MimeMultipart();
            mm_text_image.addBodyPart(text);
            mm_text_image.addBodyPart(image);
            mm_text_image.setSubType("related");

            MimeBodyPart mp_text_image = new MimeBodyPart();
            mp_text_image.setContent(mm_text_image);

            // 描述关系：正文、图片和附件
            MimeMultipart content = new MimeMultipart();
            content.addBodyPart(mp_text_image);
            for (MimeBodyPart attach : attaches)
                content.addBodyPart(attach);
            content.setSubType("mixed");
            mMessage.setContent(content);

            Transport.send(mMessage);
            return true;
        } catch (MessagingException | UnsupportedEncodingException e) {
            Log.e(TAG, "发送正文带图片引用且包含附件的邮件失败");
            e.printStackTrace();
        }
        return false;
    }
}