package com.example.mailsearch.util;

import com.example.mailsearch.model.AttachmentInfo;
import com.example.mailsearch.model.EmlParseResult;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class EmlParser {
    private final Tika tika = new Tika();

    public EmlParseResult parse(Path emlPath) throws IOException, MessagingException {
        Properties props = new Properties();
        props.setProperty("mail.mime.ignoreunknownencoding", "true");
        Session session = Session.getDefaultInstance(props);
        try (InputStream in = Files.newInputStream(emlPath)) {
            MimeMessage message = new MimeMessage(session, in);
            StringBuilder body = new StringBuilder();
            List<AttachmentInfo> attachments = new ArrayList<>();
            AtomicInteger idx = new AtomicInteger(0);
            extractPart(message, body, attachments, idx);

            return new EmlParseResult(
                    safe(message.getSubject()),
                    joinAddresses(message.getFrom()),
                    joinAddresses(message.getRecipients(Message.RecipientType.TO)),
                    toInstant(message.getSentDate()),
                    body.toString(),
                    attachments
            );
        }
    }

    public AttachmentInfo loadAttachment(Path emlPath, int attachmentIndex) throws IOException, MessagingException {
        EmlParseResult parsed = parse(emlPath);
        return parsed.getAttachments().stream()
                .filter(att -> att.getIndex() == attachmentIndex)
                .findFirst()
                .orElse(null);
    }

    private void extractPart(Part part, StringBuilder body, List<AttachmentInfo> attachments, AtomicInteger idx)
            throws MessagingException, IOException {
        if (part.isMimeType("text/plain")) {
            String text = readTextPart(part);
            if (!text.isBlank()) {
                body.append(text).append("\n\n");
            }
            return;
        }

        if (part.isMimeType("text/html")) {
            String html = readTextPart(part);
            if (!html.isBlank()) {
                String text = html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
                body.append(text).append("\n\n");
            }
            return;
        }

        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (isAttachment(bodyPart)) {
                    attachments.add(toAttachment(bodyPart, idx.getAndIncrement()));
                } else {
                    extractPart(bodyPart, body, attachments, idx);
                }
            }
            return;
        }

        if (isAttachment(part)) {
            attachments.add(toAttachment(part, idx.getAndIncrement()));
        }
    }

    private boolean isAttachment(Part part) throws MessagingException {
        String disposition = part.getDisposition();
        String fileName = part.getFileName();
        return fileName != null
                || Part.ATTACHMENT.equalsIgnoreCase(disposition)
                || Part.INLINE.equalsIgnoreCase(disposition);
    }

    private AttachmentInfo toAttachment(Part part, int index) throws MessagingException, IOException {
        String name = decodeAttachmentName(safe(part.getFileName()));
        if (name.isBlank()) {
            name = "attachment-" + index;
        }

        byte[] bytes = readAllBytes(part);
        String extractedText = "";
        if (bytes.length > 0) {
            try {
                extractedText = tika.parseToString(new ByteArrayInputStream(bytes));
            } catch (Exception ignored) {
                extractedText = new String(bytes, StandardCharsets.UTF_8);
            }
        }

        return new AttachmentInfo(index, name, safe(part.getContentType()), bytes, safe(extractedText));
    }

    private byte[] readAllBytes(Part part) throws IOException, MessagingException {
        try (InputStream in = openDecodedOrRawStream(part); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return out.toByteArray();
        }
    }

    private String readTextPart(Part part) throws MessagingException, IOException {
        try {
            Object content = part.getContent();
            return content == null ? "" : content.toString();
        } catch (MessagingException ex) {
            // Some legacy EMLs use unsupported transfer encodings (e.g. 16BIT).
            try (InputStream in = openRawStream(part)) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    private InputStream openDecodedOrRawStream(Part part) throws MessagingException, IOException {
        try {
            return part.getInputStream();
        } catch (MessagingException ex) {
            return openRawStream(part);
        }
    }

    private InputStream openRawStream(Part part) throws MessagingException, IOException {
        if (part instanceof MimeBodyPart mimeBodyPart) {
            return mimeBodyPart.getRawInputStream();
        }
        if (part instanceof MimeMessage mimeMessage) {
            return mimeMessage.getRawInputStream();
        }
        return part.getInputStream();
    }

    private String joinAddresses(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < addresses.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(addresses[i].toString());
        }
        return sb.toString();
    }

    private Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String decodeAttachmentName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }

        String decoded = fileName;
        try {
            decoded = MimeUtility.decodeText(decoded);
        } catch (Exception ignored) {
            // Keep original when decoding is not applicable.
        }

        // RFC2231 style: charset''url-encoded-name
        int rfc2231Idx = decoded.indexOf("''");
        if (rfc2231Idx > 0 && rfc2231Idx < decoded.length() - 2) {
            String charset = decoded.substring(0, rfc2231Idx).trim();
            String encodedName = decoded.substring(rfc2231Idx + 2);
            try {
                decoded = URLDecoder.decode(encodedName, charset);
            } catch (Exception ignored) {
                try {
                    decoded = URLDecoder.decode(encodedName, StandardCharsets.UTF_8);
                } catch (Exception ignoredAgain) {
                    // Keep best-effort decoded value.
                }
            }
        }

        return decoded;
    }
}
