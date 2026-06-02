package io.github.afgprojects.framework.data.core.mapper.handlers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 新增 TypeHandler 综合测试
 * <p>
 * 覆盖 OffsetDateTime、Instant、LocalTime、UUID、ZonedDateTime、Year、YearMonth 七个处理器
 */
@DisplayName("新增 TypeHandler 测试")
class NewTypeHandlersTest {

    // ==================== OffsetDateTimeTypeHandler ====================

    @Nested
    @DisplayName("OffsetDateTimeTypeHandler 测试")
    class OffsetDateTimeTypeHandlerTests {

        private final OffsetDateTimeTypeHandler handler = new OffsetDateTimeTypeHandler();

        @Test
        @DisplayName("应返回 OffsetDateTime 类型")
        void shouldReturnType() {
            assertThat(handler.getType()).isEqualTo(OffsetDateTime.class);
        )

        @Test
        @DisplayName("应从 Timestamp 转换为 OffsetDateTime")
        void shouldConvertFromTimestamp() {
            Timestamp ts = Timestamp.valueOf("2024-06-15 10:30:00");
            OffsetDateTime result = handler.convert(ts, OffsetDateTime.class);

            assertThat(result).isNotNull();
            assertThat(result.getYear()).isEqualTo(2024);
            assertThat(result.getMonth()).isEqualTo(Month.JUNE);
            assertThat(result.getDayOfMonth()).isEqualTo(15);
            assertThat(result.getHour()).isEqualTo(10);
            assertThat(result.getMinute()).isEqualTo(30);
        )

        @Test
        @DisplayName("应从 ZonedDateTime 转换为 OffsetDateTime")
        void shouldConvertFromZonedDateTime() {
            ZonedDateTime zdt = ZonedDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneId.of("Asia/Shanghai"));
            OffsetDateTime result = handler.convert(zdt, OffsetDateTime.class);

            assertThat(result).isNotNull();
            assertThat(zdt.toOffsetDateTime()).isEqualTo(result);
        )

        @Test
        @DisplayName("应从 LocalDateTime 转换为 OffsetDateTime")
        void shouldConvertFromLocalDateTime() {
            LocalDateTime ldt = LocalDateTime.of(2024, 6, 15, 10, 30, 0);
            OffsetDateTime result = handler.convert(ldt, OffsetDateTime.class);

            assertThat(result).isNotNull();
            assertThat(result.getYear()).isEqualTo(2024);
            assertThat(result.getMonth()).isEqualTo(Month.JUNE);
            assertThat(result.getDayOfMonth()).isEqualTo(15);
            assertThat(result.getHour()).isEqualTo(10);
            assertThat(result.getMinute()).isEqualTo(30);
        )

        @Test
        @DisplayName("OffsetDateTime 输入应直接返回")
        void shouldReturnSameOffsetDateTime() {
            OffsetDateTime odt = OffsetDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneOffset.ofHours(8));
            OffsetDateTime result = handler.convert(odt, OffsetDateTime.class);
            assertThat(result).isSameAs(odt);
        )

        @Test
        @DisplayName("null 输入应返回 null")
        void shouldReturnNullForNullInput() {
            assertThat(handler.convert(null, OffsetDateTime.class)).isNull();
        )

        @Test
        @DisplayName("不支持的输入类型应返回 null")
        void shouldReturnNullForUnsupportedType() {
            assertThat(handler.convert("2024-06-15", OffsetDateTime.class)).isNull();
            assertThat(handler.convert(12345, OffsetDateTime.class)).isNull();
        )
    )

    // ==================== InstantTypeHandler ====================

    @Nested
    @DisplayName("InstantTypeHandler 测试")
    class InstantTypeHandlerTests {

        private final InstantTypeHandler handler = new InstantTypeHandler();

        @Test
        @DisplayName("应返回 Instant 类型")
        void shouldReturnType() {
            assertThat(handler.getType()).isEqualTo(Instant.class);
        )

        @Test
        @DisplayName("应从 Timestamp 转换为 Instant")
        void shouldConvertFromTimestamp() {
            Timestamp ts = Timestamp.valueOf("2024-06-15 10:30:00");
            Instant result = handler.convert(ts, Instant.class);

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(ts.toInstant());
        )

        @Test
        @DisplayName("应从 Date 转换为 Instant")
        void shouldConvertFromDate() {
            java.util.Date date = new java.util.Date(1718443800000L);
            Instant result = handler.convert(date, Instant.class);

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(date.toInstant());
        )

        @Test
        @DisplayName("应从 OffsetDateTime 转换为 Instant")
        void shouldConvertFromOffsetDateTime() {
            OffsetDateTime odt = OffsetDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneOffset.ofHours(8));
            Instant result = handler.convert(odt, Instant.class);

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(odt.toInstant());
        )

        @Test
        @DisplayName("Instant 输入应直接返回")
        void shouldReturnSameInstant() {
            Instant instant = Instant.now();
            Instant result = handler.convert(instant, Instant.class);
            assertThat(result).isSameAs(instant);
        )

        @Test
        @DisplayName("null 输入应返回 null")
        void shouldReturnNullForNullInput() {
            assertThat(handler.convert(null, Instant.class)).isNull();
        )

        @Test
        @DisplayName("不支持的输入类型应返回 null")
        void shouldReturnNullForUnsupportedType() {
            assertThat(handler.convert("2024-06-15T10:30:00Z", Instant.class)).isNull();
            assertThat(handler.convert(12345L, Instant.class)).isNull();
        )
    )

    // ==================== LocalTimeTypeHandler ====================

    @Nested
    @DisplayName("LocalTimeTypeHandler 测试")
    class LocalTimeTypeHandlerTests {

        private final LocalTimeTypeHandler handler = new LocalTimeTypeHandler();

        @Test
        @DisplayName("应返回 LocalTime 类型")
        void shouldReturnType() {
            assertThat(handler.getType()).isEqualTo(LocalTime.class);
        )

        @Test
        @DisplayName("应从 Time 转换为 LocalTime")
        void shouldConvertFromTime() {
            Time time = Time.valueOf("10:30:45");
            LocalTime result = handler.convert(time, LocalTime.class);

            assertThat(result).isEqualTo(LocalTime.of(10, 30, 45));
        )

        @Test
        @DisplayName("应从 Timestamp 转换为 LocalTime")
        void shouldConvertFromTimestamp() {
            Timestamp ts = Timestamp.valueOf("2024-06-15 10:30:45");
            LocalTime result = handler.convert(ts, LocalTime.class);

            assertThat(result).isEqualTo(LocalTime.of(10, 30, 45));
        )

        @Test
        @DisplayName("应从 String 转换为 LocalTime")
        void shouldConvertFromString() {
            LocalTime result = handler.convert("10:30:45", LocalTime.class);
            assertThat(result).isEqualTo(LocalTime.of(10, 30, 45));
        )

        @Test
        @DisplayName("LocalTime 输入应直接返回")
        void shouldReturnSameLocalTime() {
            LocalTime lt = LocalTime.of(10, 30, 45);
            LocalTime result = handler.convert(lt, LocalTime.class);
            assertThat(result).isSameAs(lt);
        )

        @Test
        @DisplayName("null 输入应返回 null")
        void shouldReturnNullForNullInput() {
            assertThat(handler.convert(null, LocalTime.class)).isNull();
        )

        @Test
        @DisplayName("不支持的输入类型应返回 null")
        void shouldReturnNullForUnsupportedType() {
            assertThat(handler.convert(12345, LocalTime.class)).isNull();
        )

        @Test
        @DisplayName("无效字符串应返回 null")
        void shouldReturnNullForInvalidString() {
            assertThat(handler.convert("not-a-time", LocalTime.class)).isNull();
        )
    )

    // ==================== UUIDTypeHandler ====================

    @Nested
    @DisplayName("UUIDTypeHandler 测试")
    class UUIDTypeHandlerTests {

        private final UUIDTypeHandler handler = new UUIDTypeHandler();

        @Test
        @DisplayName("应返回 UUID 类型")
        void shouldReturnType() {
            assertThat(handler.getType()).isEqualTo(UUID.class);
        )

        @Test
        @DisplayName("应从 String 转换为 UUID")
        void shouldConvertFromString() {
            String uuidStr = "550e8400-e29b-41d4-a716-446655440000";
            UUID result = handler.convert(uuidStr, UUID.class);

            assertThat(result).isEqualTo(UUID.fromString(uuidStr));
        )

        @Test
        @DisplayName("应从 byte[] 转换为 UUID")
        void shouldConvertFromByteArray() {
            UUID original = UUID.randomUUID();
            ByteBuffer bb = ByteBuffer.allocate(16);
            bb.putLong(original.getMostSignificantBits());
            bb.putLong(original.getLeastSignificantBits());
            byte[] bytes = bb.array();

            UUID result = handler.convert(bytes, UUID.class);
            assertThat(result).isEqualTo(original);
        )

        @Test
        @DisplayName("UUID 输入应直接返回")
        void shouldReturnSameUUID() {
            UUID uuid = UUID.randomUUID();
            UUID result = handler.convert(uuid, UUID.class);
            assertThat(result).isSameAs(uuid);
        )

        @Test
        @DisplayName("null 输入应返回 null")
        void shouldReturnNullForNullInput() {
            assertThat(handler.convert(null, UUID.class)).isNull();
        )

        @Test
        @DisplayName("不支持的输入类型应返回 null")
        void shouldReturnNullForUnsupportedType() {
            assertThat(handler.convert(12345, UUID.class)).isNull();
        )

        @Test
        @DisplayName("无效 UUID 字符串应返回 null")
        void shouldReturnNullForInvalidString() {
            assertThat(handler.convert("not-a-uuid", UUID.class)).isNull();
        )

        @Test
        @DisplayName("长度不为 16 的 byte[] 应返回 null")
        void shouldReturnNullForShortByteArray() {
            assertThat(handler.convert(new byte[8], UUID.class)).isNull();
            assertThat(handler.convert(new byte[32], UUID.class)).isNull();
        )

        @Test
        @DisplayName("应返回优先级 5")
        void shouldReturnPriority() {
            assertThat(handler.priority()).isEqualTo(5);
        )
    )

    // ==================== ZonedDateTimeTypeHandler ====================

    @Nested
    @DisplayName("ZonedDateTimeTypeHandler 测试")
    class ZonedDateTimeTypeHandlerTests {

        private final ZonedDateTimeTypeHandler handler = new ZonedDateTimeTypeHandler();

        @Test
        @DisplayName("应返回 ZonedDateTime 类型")
        void shouldReturnType() {
            assertThat(handler.getType()).isEqualTo(ZonedDateTime.class);
        )

        @Test
        @DisplayName("应从 OffsetDateTime 转换为 ZonedDateTime")
        void shouldConvertFromOffsetDateTime() {
            OffsetDateTime odt = OffsetDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneOffset.ofHours(8));
            ZonedDateTime result = handler.convert(odt, ZonedDateTime.class);

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(odt.toZonedDateTime());
        )

        @Test
        @DisplayName("应从 Timestamp 转换为 ZonedDateTime")
        void shouldConvertFromTimestamp() {
            Timestamp ts = Timestamp.valueOf("2024-06-15 10:30:00");
            ZonedDateTime result = handler.convert(ts, ZonedDateTime.class);

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(ts.toInstant().atZone(ZoneId.systemDefault()));
        )

        @Test
        @DisplayName("ZonedDateTime 输入应直接返回")
        void shouldReturnSameZonedDateTime() {
            ZonedDateTime zdt = ZonedDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneId.of("Asia/Shanghai"));
            ZonedDateTime result = handler.convert(zdt, ZonedDateTime.class);
            assertThat(result).isSameAs(zdt);
        )

        @Test
        @DisplayName("null 输入应返回 null")
        void shouldReturnNullForNullInput() {
            assertThat(handler.convert(null, ZonedDateTime.class)).isNull();
        )

        @Test
        @DisplayName("不支持的输入类型应返回 null")
        void shouldReturnNullForUnsupportedType() {
            assertThat(handler.convert("2024-06-15", ZonedDateTime.class)).isNull();
            assertThat(handler.convert(12345, ZonedDateTime.class)).isNull();
        )
    )

    // ==================== YearTypeHandler ====================

    @Nested
    @DisplayName("YearTypeHandler 测试")
    class YearTypeHandlerTests {

        private final YearTypeHandler handler = new YearTypeHandler();

        @Test
        @DisplayName("应返回 Year 类型")
        void shouldReturnType() {
            assertThat(handler.getType()).isEqualTo(Year.class);
        )

        @Test
        @DisplayName("应从 Number 转换为 Year")
        void shouldConvertFromNumber() {
            assertThat(handler.convert(2024, Year.class)).isEqualTo(Year.of(2024));
            assertThat(handler.convert(2024L, Year.class)).isEqualTo(Year.of(2024));
            assertThat(handler.convert((short) 2024, Year.class)).isEqualTo(Year.of(2024));
        )

        @Test
        @DisplayName("应从 String 转换为 Year")
        void shouldConvertFromString() {
            assertThat(handler.convert("2024", Year.class)).isEqualTo(Year.of(2024));
        )

        @Test
        @DisplayName("Year 输入应直接返回")
        void shouldReturnSameYear() {
            Year year = Year.of(2024);
            Year result = handler.convert(year, Year.class);
            assertThat(result).isSameAs(year);
        )

        @Test
        @DisplayName("null 输入应返回 null")
        void shouldReturnNullForNullInput() {
            assertThat(handler.convert(null, Year.class)).isNull();
        )

        @Test
        @DisplayName("不支持的输入类型应返回 null")
        void shouldReturnNullForUnsupportedType() {
            // Double is a Number, so it IS supported via Number.intValue()
            // Use a truly unsupported type instead
            assertThat(handler.convert(LocalDateTime.now(), Year.class)).isNull();
            assertThat(handler.convert(new Object(), Year.class)).isNull();
        )

        @Test
        @DisplayName("无效字符串应返回 null")
        void shouldReturnNullForInvalidString() {
            assertThat(handler.convert("not-a-year", Year.class)).isNull();
        )
    )

    // ==================== YearMonthTypeHandler ====================

    @Nested
    @DisplayName("YearMonthTypeHandler 测试")
    class YearMonthTypeHandlerTests {

        private final YearMonthTypeHandler handler = new YearMonthTypeHandler();

        @Test
        @DisplayName("应返回 YearMonth 类型")
        void shouldReturnType() {
            assertThat(handler.getType()).isEqualTo(YearMonth.class);
        )

        @Test
        @DisplayName("应从 String 转换为 YearMonth")
        void shouldConvertFromString() {
            assertThat(handler.convert("2024-06", YearMonth.class)).isEqualTo(YearMonth.of(2024, 6));
        )

        @Test
        @DisplayName("应从 Date 转换为 YearMonth")
        void shouldConvertFromDate() {
            Date date = Date.valueOf("2024-06-15");
            YearMonth result = handler.convert(date, YearMonth.class);

            assertThat(result).isEqualTo(YearMonth.of(2024, 6));
        )

        @Test
        @DisplayName("YearMonth 输入应直接返回")
        void shouldReturnSameYearMonth() {
            YearMonth ym = YearMonth.of(2024, 6);
            YearMonth result = handler.convert(ym, YearMonth.class);
            assertThat(result).isSameAs(ym);
        )

        @Test
        @DisplayName("null 输入应返回 null")
        void shouldReturnNullForNullInput() {
            assertThat(handler.convert(null, YearMonth.class)).isNull();
        )

        @Test
        @DisplayName("不支持的输入类型应返回 null")
        void shouldReturnNullForUnsupportedType() {
            assertThat(handler.convert(2024, YearMonth.class)).isNull();
            assertThat(handler.convert(LocalDateTime.now(), YearMonth.class)).isNull();
        )

        @Test
        @DisplayName("无效字符串应返回 null")
        void shouldReturnNullForInvalidString() {
            assertThat(handler.convert("not-a-yearmonth", YearMonth.class)).isNull();
        )
    )
)
