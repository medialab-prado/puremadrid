/*
 * Copyright (C) 2017 Javier Delgado Aylagas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.puremadrid.api.utils;

import com.google.apphosting.api.ApiProxy;
import com.puremadrid.api.services.GetNewData;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.activation.URLDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import static com.puremadrid.api.ApiKeys.EMAIL_RECIPIENT;
import static com.puremadrid.api.ApiKeys.EMAIL_SENDER;


/**
 * Created by Delga on 10/12/2016.
 */

public class EmailUtils {

    private static final Logger mLogger = Logger.getLogger(EmailUtils.class.getName());

    private static final String username = EMAIL_SENDER;

    public static boolean sendEmail(String subject, String email_text) {
        return sendEmail(subject,email_text,null,null);
    }

    public static boolean sendEmail(String subject, String email_text, List<String> images) {
        return sendEmail(subject,email_text,images,null);
    }

    /**
     * Send text by email
     *
     * @param email_text
     */
    public static boolean sendEmail(String subject, String email_text, List<String> images, List<String> urlAttachments) {

        String from = GetNewData.isPureMadrid() ? "[Pure Madrid] " : "[Not Pure Madrid] ";

        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        mLogger.info(email_text);

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(username));
            msg.addRecipient(Message.RecipientType.TO,
                    new InternetAddress(EMAIL_RECIPIENT));
            msg.setSubject(from +  subject);
            msg.setText(email_text);

            // Add Images
            boolean addedTextPart = false;
            MimeMultipart multipartImages = new MimeMultipart("mixed");

            // Add Images
            if (images != null && images.size() >= 2){

                // IMAGE 1
                String image1 = images.get(0);
                URL url = new URL(image1);
                URLDataSource ds = new URLDataSource(url);
                MimeBodyPart imageBodypart1 = new MimeBodyPart();
                imageBodypart1.setDataHandler(new DataHandler(ds));
                imageBodypart1.setHeader("Content-ID", "<myimg1>");
                imageBodypart1.setDisposition(MimeBodyPart.INLINE);
                imageBodypart1.setFileName(image1);

                // IMAGE 2
                String image2 = images.get(1);
                URL url2 = new URL(image2);
                URLDataSource ds2 = new URLDataSource(url2);
                MimeBodyPart imageBodypart2 = new MimeBodyPart();
                imageBodypart2.setDataHandler(new DataHandler(ds2));
                imageBodypart2.setHeader("Content-ID", "<myimg2>");
                imageBodypart2.setDisposition(MimeBodyPart.INLINE);
                imageBodypart2.setFileName(image2);

                // TEXT
                MimeBodyPart textPart = new MimeBodyPart();
                String body = "<html><body>"
                        + "<div>" + image1 + "</div><img src=\"cid:myimg1\" alt=\"myimg1\"/>"
                        + "<div>" + image2 + "</div><img src=\"cid:myimg2\" alt=\"myimg2\"/>"
                        + "</body></html>";
                textPart.setHeader("Content-Type", "text/plain; charset=\"utf-8\"");
                textPart.setContent(body, "text/html; charset=utf-8");
//                textPart.setFileName("textpart.html");
                addedTextPart = true;

                // MULTIPART AND SET CONTENT
                multipartImages.addBodyPart(imageBodypart1);
                multipartImages.addBodyPart(imageBodypart2);
                multipartImages.addBodyPart(textPart);

            }

            // TEXT
            if (!addedTextPart) {
                MimeBodyPart contentPart = new MimeBodyPart();
                contentPart.setHeader("Content-Type", "text/plain; charset=\"utf-8\"");
                contentPart.setContent(email_text, "text/html; charset=utf-8");
                multipartImages.addBodyPart(contentPart);
            }


            // Add Attachments
            if (urlAttachments != null && urlAttachments.size() >= 1) {

                String attachment = urlAttachments.get(0);
                URL url = new URL(attachment);
                URLDataSource ds2 = new URLDataSource(url);
                MimeBodyPart imageBodypart = new MimeBodyPart();
                imageBodypart.setDataHandler(new DataHandler(ds2));
                imageBodypart.setDisposition(MimeBodyPart.INLINE);
                imageBodypart.setFileName(attachment);
                multipartImages.addBodyPart(imageBodypart);

            }

            msg.setContent(multipartImages);
            Transport.send(msg);
            mLogger.info("Email sent to admin: " + from + subject);
            return true;
        } catch (ApiProxy.OverQuotaException e){
            mLogger.info("[EMAIL] Not sent, not enough quota: " + subject);
            return false;
        }
        catch (MessagingException | MalformedURLException e){
            mLogger.warning("ERROR SENDING " + subject + "\n" + e.toString());
            return false;
        }

    }

    /**
     * Returns the InputStream inputStream as a String
     * Used to load raw json into String
     *
     * Works for the local versions
     *
     * @param inputStream: InputStream to parse
     * @return result String
     */
    private static String getString(InputStream inputStream){
        String text;
        int size = 0;

        try {
            size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            text = new String(buffer,"UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }

        return text;
    }


}
