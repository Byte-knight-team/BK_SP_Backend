import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class TestDB {
    public static void main(String[] args) {
        try {
            Properties props = new Properties();
            props.load(Files.newInputStream(Paths.get(".env")));
            String password = props.getProperty("DB_PASSWORD");
            
            Connection conn = DriverManager.getConnection("jdbc:mysql://syncserve-database-syncserve-93.j.aivencloud.com:28248/defaultdb?useSSL=true&sslMode=REQUIRED", "avnadmin", password);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM coupons");
            if (rs.next()) {
                System.out.println("COUPON_COUNT=" + rs.getInt(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
