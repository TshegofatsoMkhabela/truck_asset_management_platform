package za.co.ice.tamp.backend.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.SQLException;
import org.postgresql.util.PGobject;

/**
 * Maps {@code receipts.ip_address} ({@code INET}) to a plain {@code String}.
 *
 * <p>Two things are required together, not either alone, discovered by running the tests
 * rather than assumed correct up front:
 *
 * <ul>
 *   <li>the converter wraps the value in a {@link PGobject} tagged {@code inet}, because
 *       PostgreSQL has no implicit cast from {@code varchar} to {@code inet}, so binding a
 *       plain String parameter fails outright;
 *   <li>the entity field is additionally annotated {@code @JdbcTypeCode(SqlTypes.OTHER)}, because
 *       without it Hibernate binds the converted value using an internal type code the driver
 *       does not recognise ("Unsupported Types value"), and with only the JdbcType annotation
 *       and no converter Hibernate instead tries to unwrap the String as a byte array
 *       ("Could not convert java.lang.String to [B"). Neither half works without the other.
 * </ul>
 */
@Converter
public class InetAddressConverter implements AttributeConverter<String, Object> {

    @Override
    public Object convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            PGobject inet = new PGobject();
            inet.setType("inet");
            inet.setValue(attribute);
            return inet;
        } catch (SQLException e) {
            throw new IllegalArgumentException("Not a valid inet value: " + attribute, e);
        }
    }

    @Override
    public String convertToEntityAttribute(Object dbData) {
        return dbData == null ? null : dbData.toString();
    }
}
