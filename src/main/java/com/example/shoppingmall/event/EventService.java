package com.example.shoppingmall.event;

import com.example.shoppingmall.auth.MemberService;
import jakarta.validation.constraints.*;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class EventService {
    static final int MAX_IMAGE_DATA_LENGTH = 699100;
    private static final int MAX_IMAGE_BYTES = 512 * 1024;

    public record EventInput(@NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 500) String description, @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime endsAt, @Min(0) @Max(1000000) long discount, boolean enabled,
        @Size(max = MAX_IMAGE_DATA_LENGTH) String imageData) {
        public EventInput(String title, String description, LocalDateTime startsAt, LocalDateTime endsAt, long discount, boolean enabled) {
            this(title, description, startsAt, endsAt, discount, enabled, null);
        }
    }
    public record Event(long id, String title, String description, LocalDateTime startsAt,
        LocalDateTime endsAt, long discount, boolean enabled, boolean hasImage) {}
    public record Banner(long id, String title, String description, LocalDateTime endsAt, long discount, boolean hasImage) {}
    public record Coupon(long eventId, String title, LocalDateTime expiresAt, long discount) {}
    public record EventImage(String mediaType, byte[] bytes) {}

    private static final String ACTIVE = "deleted_at IS NULL AND enabled = 1 AND starts_at <= ? AND ends_at > ?";
    private static final String IMAGE_FLAG = "CASE WHEN image_data IS NULL THEN 0 ELSE 1 END AS has_image";
    private static final String BANNERS = "SELECT id, title, description, ends_at, discount, " + IMAGE_FLAG
        + " FROM mall_events WHERE " + ACTIVE;
    private static final RowMapper<Event> EVENT = (row, index) -> new Event(
        row.getLong("id"), row.getString("title"), row.getString("description"),
        row.getTimestamp("starts_at").toLocalDateTime(), row.getTimestamp("ends_at").toLocalDateTime(),
        row.getLong("discount"), row.getInt("enabled") == 1, row.getInt("has_image") == 1);
    private static final RowMapper<Banner> BANNER = (row, index) -> new Banner(
        row.getLong("id"), row.getString("title"), row.getString("description"),
        row.getTimestamp("ends_at").toLocalDateTime(), row.getLong("discount"), row.getInt("has_image") == 1);
    private static final String AVAILABLE = """
        SELECT e.id, e.title, e.ends_at, c.discount
        FROM mall_event_coupons c JOIN mall_events e ON e.id = c.event_id
        WHERE c.member_id = ? AND e.ends_at > ?
          AND NOT EXISTS (SELECT 1 FROM mall_orders o
            WHERE o.member_id = c.member_id AND o.coupon_event_id = c.event_id AND o.status = 'PAID')
        """;
    private final JdbcTemplate jdbc;
    private final MemberService members;

    public EventService(JdbcTemplate jdbc, MemberService members) {
        this.jdbc = jdbc;
        this.members = members;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Tokyo"));
    }

    public List<Event> list() {
        return jdbc.query("SELECT id, title, description, starts_at, ends_at, discount, enabled, "
            + IMAGE_FLAG + " FROM mall_events WHERE deleted_at IS NULL ORDER BY id DESC", EVENT);
    }

    public List<Banner> active() {
        var now = now();
        return jdbc.query(BANNERS + " ORDER BY id DESC", BANNER, now, now);
    }

    public Banner detail(long id) {
        var now = now();
        return jdbc.query(BANNERS + " AND id = ?", BANNER, now, now, id).stream()
            .findFirst().orElseThrow(EventService::notFound);
    }

    public EventImage image(long id) {
        var now = now();
        return jdbc.query("SELECT image_data FROM mall_events WHERE " + ACTIVE + " AND id = ? AND image_data IS NOT NULL",
            (row, index) -> decodeImage(row.getString("image_data")), now, now, id)
            .stream().findFirst().orElseThrow(EventService::notFound);
    }

    public List<Long> claimedEventIds(long memberId) {
        return jdbc.queryForList("SELECT event_id FROM mall_event_coupons WHERE member_id = ?", Long.class, memberId);
    }

    @Transactional
    public void save(Long id, EventInput input) {
        if (!input.endsAt().isAfter(input.startsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "終了日時は開始日時より後にしてください。");
        }
        String image = input.imageData();
        if (image != null && !image.isEmpty()) decodeImage(image);
        if (id == null) {
            jdbc.update(connection -> {
                var statement = connection.prepareStatement("""
                    INSERT INTO mall_events (title, description, starts_at, ends_at, discount, enabled, image_data)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """);
                bindEvent(statement, input);
                bindImage(statement, 7, image);
                return statement;
            });
            return;
        }
        int changed = jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                UPDATE mall_events SET title = ?, description = ?, starts_at = ?, ends_at = ?, discount = ?, enabled = ?
                """ + (image == null ? "" : ", image_data = ? ") + " WHERE id = ? AND deleted_at IS NULL");
            bindEvent(statement, input);
            if (image != null) bindImage(statement, 7, image);
            statement.setLong(image == null ? 7 : 8, id);
            return statement;
        });
        if (changed == 0) throw notFound();
    }

    @Transactional
    public void delete(long id) {
        int changed = jdbc.update("UPDATE mall_events SET deleted_at = ? WHERE id = ? AND deleted_at IS NULL", now(), id);
        if (changed == 0) throw notFound();
    }

    private static void bindEvent(PreparedStatement statement, EventInput input) throws SQLException {
        statement.setString(1, input.title().trim());
        statement.setString(2, input.description().trim());
        statement.setObject(3, input.startsAt());
        statement.setObject(4, input.endsAt());
        statement.setLong(5, input.discount());
        statement.setInt(6, input.enabled() ? 1 : 0);
    }

    private static void bindImage(PreparedStatement statement, int index, String data) throws SQLException {
        if (data == null || data.isEmpty()) statement.setNull(index, Types.CLOB);
        else statement.setClob(index, new StringReader(data), data.length());
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "イベントが見つかりません。");
    }

    private static EventImage decodeImage(String data) {
        int separator = data.indexOf(',');
        String type = switch (separator < 0 ? "" : data.substring(0, separator)) {
            case "data:image/jpeg;base64" -> "image/jpeg";
            case "data:image/png;base64" -> "image/png";
            case "data:image/webp;base64" -> "image/webp";
            default -> "";
        };
        if (!type.isEmpty() && data.length() <= MAX_IMAGE_DATA_LENGTH) {
            try {
                byte[] bytes = Base64.getDecoder().decode(data.substring(separator + 1));
                var header = HexFormat.of().formatHex(bytes, 0, Math.min(bytes.length, 12));
                boolean validHeader = switch (type) {
                    case "image/jpeg" -> header.startsWith("ffd8ff");
                    case "image/png" -> header.startsWith("89504e470d0a1a0a");
                    default -> header.length() == 24 && header.startsWith("52494646") && header.endsWith("57454250");
                };
                if (validHeader && bytes.length <= MAX_IMAGE_BYTES) return new EventImage(type, bytes);
            } catch (IllegalArgumentException ignored) {
                // Malformed base64 follows the same validation path as an unsupported format.
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JPEG・PNG・WebP の画像を512 KB以内で選択してください。");
    }

    @Transactional
    public void claim(long memberId, long eventId) {
        members.lockEntity(memberId);
        if (jdbc.queryForObject("SELECT COUNT(*) FROM mall_event_coupons WHERE member_id = ? AND event_id = ?",
            Integer.class, memberId, eventId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "このクーポンはすでに受け取り済みです。");
        }

        var now = now();
        int inserted = jdbc.update("""
            INSERT INTO mall_event_coupons (member_id, event_id, discount)
            SELECT ?, id, discount FROM mall_events
            WHERE id = ? AND deleted_at IS NULL AND enabled = 1 AND starts_at <= ? AND ends_at > ? AND discount > 0
            """, memberId, eventId, now, now);
        if (inserted == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "このイベントのクーポンは受け取れません。");
        }
    }

    public List<Coupon> coupons(long memberId) {
        return jdbc.query(AVAILABLE + " ORDER BY e.ends_at, e.id", this::coupon, memberId, now());
    }

    public long discount(long memberId, long eventId) {
        return jdbc.query(AVAILABLE + " AND e.id = ?", this::coupon, memberId, now(), eventId)
            .stream().findFirst().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.CONFLICT, "このクーポンは利用できません。")).discount();
    }

    private Coupon coupon(java.sql.ResultSet row, int index) throws java.sql.SQLException {
        return new Coupon(row.getLong("id"), row.getString("title"),
            row.getTimestamp("ends_at").toLocalDateTime(), row.getLong("discount"));
    }
}
