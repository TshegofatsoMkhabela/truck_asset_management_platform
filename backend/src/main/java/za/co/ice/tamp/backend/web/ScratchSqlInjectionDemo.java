package za.co.ice.tamp.backend.web;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Scratch file for #3's Minimum Integration Test: a deliberately introduced
 * known-bad SQL string-concatenation pattern, to prove CodeQL actually flags it
 * and blocks merge. Deleted once the red-step proof is captured — never wired
 * into a real route.
 */
@RestController
class ScratchSqlInjectionDemo {

    @GetMapping("/scratch/sql-injection-demo")
    String lookup(@RequestParam String name) throws SQLException {
        String sql = "SELECT * FROM users WHERE name = '" + name + "'";
        try (Connection c = DriverManager.getConnection("jdbc:postgresql://localhost/tamp");
                Statement stmt = c.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : "none";
        }
    }
}
