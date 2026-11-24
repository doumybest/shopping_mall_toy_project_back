package com.example.shoppingmall;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import org.springframework.test.context.DynamicPropertyRegistry;

/** SMTP wire fixture bound to loopback only. It never forwards mail to the Internet. */
final class TestSmtpServer {
    static final TestSmtpServer INSTANCE = new TestSmtpServer();
    private final ServerSocket server;
    private final ConcurrentHashMap<String, BlockingQueue<String>> inboxes = new ConcurrentHashMap<>();
    final AtomicBoolean failNext = new AtomicBoolean();

    private TestSmtpServer() {
        try {
            server = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            Thread.ofVirtual().start(() -> {
                while (!server.isClosed()) {
                    try { Socket socket = server.accept(); Thread.ofVirtual().start(() -> handle(socket)); }
                    catch (IOException ignored) { break; }
                }
            });
        } catch (IOException exception) { throw new UncheckedIOException(exception); }
    }

    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> "127.0.0.1");
        registry.add("spring.mail.port", () -> INSTANCE.server.getLocalPort());
        registry.add("spring.mail.properties[mail.smtp.auth]", () -> "false");
        registry.add("spring.mail.properties[mail.smtp.starttls.enable]", () -> "false");
        registry.add("spring.mail.properties[mail.smtp.starttls.required]", () -> "false");
        registry.add("mall.mail-from", () -> "no-reply@test.example");
    }

    String codeFor(String email) throws Exception {
        String body = inboxes.computeIfAbsent(email, key -> new LinkedBlockingQueue<>()).poll(3, TimeUnit.SECONDS);
        if (body == null) throw new AssertionError("SMTP message not received for test recipient");
        var matcher = Pattern.compile("Code: ([0-9]{6})").matcher(body);
        if (!matcher.find()) throw new AssertionError("Verification code missing from SMTP message");
        return matcher.group(1);
    }

    private void handle(Socket socket) {
        try (socket;
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
             var writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII), true)) {
            socket.setSoTimeout(10000);
            reply(writer, "220 localhost test SMTP");
            String recipient = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("EHLO") || line.startsWith("HELO")) reply(writer, "250 localhost");
                else if (line.startsWith("MAIL FROM")) reply(writer, failNext.getAndSet(false) ? "550 rejected by test fixture" : "250 OK");
                else if (line.startsWith("RCPT TO")) { recipient = line.substring(line.indexOf('<') + 1, line.indexOf('>')); reply(writer, "250 OK"); }
                else if (line.equals("DATA")) {
                    reply(writer, "354 End with dot");
                    StringBuilder raw = new StringBuilder();
                    while ((line = reader.readLine()) != null && !line.equals(".")) raw.append(line.startsWith("..") ? line.substring(1) : line).append("\r\n");
                    var message = new MimeMessage(Session.getInstance(new Properties()),
                        new ByteArrayInputStream(raw.toString().getBytes(StandardCharsets.ISO_8859_1)));
                    inboxes.computeIfAbsent(recipient, key -> new LinkedBlockingQueue<>()).add(message.getContent().toString());
                    reply(writer, "250 Accepted");
                } else if (line.equals("QUIT")) { reply(writer, "221 Bye"); break; }
                else reply(writer, "250 OK");
            }
        } catch (Exception ignored) { /* Caller observes the SMTP timeout/failure. */ }
    }

    private static void reply(PrintWriter writer, String line) { writer.print(line + "\r\n"); writer.flush(); }
}
