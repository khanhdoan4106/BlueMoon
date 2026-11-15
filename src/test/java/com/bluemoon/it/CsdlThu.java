package com.bluemoon.it;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.bluemoon.config.Database;
import com.bluemoon.model.TaiKhoan;
import com.bluemoon.model.VaiTro;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * CSDL dung cho kiem thu tich hop (lop *IT, chay bang mvn verify -Pmysql).
 * Chi chay khi bien moi truong BLUEMOON_DB_URL tro toi mot CSDL co chu
 * "test" trong ten, de khong bao gio xoa nham CSDL bluemoon dang dung.
 *
 * <p>Hai user (docs/setup.md, muc 4): DAO va Service duoc thu bang user ung
 * dung ({@code BLUEMOON_DB_USER}, chi co SELECT, INSERT, UPDATE); viec tao
 * bang, trigger va nap seed dung user quan tri
 * ({@code BLUEMOON_DB_ADMIN_USER}, {@code BLUEMOON_DB_ADMIN_PASSWORD}).
 * Khong dat user quan tri thi moi viec dung user ung dung, khi do user nay
 * phai du quyen tao bang (vd root).
 */
final class CsdlThu {

    /** Ten CSDL trong URL JDBC phai chua "test". */
    private static final Pattern URL_THU =
            Pattern.compile("jdbc:mysql://[^/]+/[^/?]*test[^/?]*(\\?.*)?");

    /** Bien moi truong: user quan tri cua CSDL thu. */
    static final String BIEN_QUAN_TRI = "BLUEMOON_DB_ADMIN_USER";
    /** Bien moi truong: mat khau cua user quan tri. */
    static final String BIEN_MAT_KHAU_QUAN_TRI = "BLUEMOON_DB_ADMIN_PASSWORD";

    private CsdlThu() {
    }

    /**
     * Da cau hinh user quan tri rieng, khac user ung dung, hay chua.
     *
     * @return true neu kiem thu dang chay voi hai user tach biet
     */
    static boolean tachUser() {
        String quanTri = System.getenv(BIEN_QUAN_TRI);
        return quanTri != null && !quanTri.isBlank() && !quanTri.equals(
                Database.docCauHinh().getProperty("user"));
    }

    /**
     * Ten user quan tri; chua cau hinh thi la user ung dung.
     *
     * @return ten user dung de tao bang, trigger
     */
    static String userQuanTri() {
        String quanTri = System.getenv(BIEN_QUAN_TRI);
        return quanTri == null || quanTri.isBlank()
                ? Database.docCauHinh().getProperty("user") : quanTri;
    }

    /**
     * Mat khau cua {@link #userQuanTri()}.
     *
     * @return mat khau, chuoi rong neu khong dat
     */
    static String matKhauQuanTri() {
        String quanTri = System.getenv(BIEN_QUAN_TRI);
        if (quanTri == null || quanTri.isBlank()) {
            return Database.docCauHinh().getProperty("password");
        }
        String matKhau = System.getenv(BIEN_MAT_KHAU_QUAN_TRI);
        return matKhau == null ? "" : matKhau;
    }

    /**
     * Ket noi bang user quan tri, cung CSDL va mui gio phien nhu ket noi
     * cua ung dung. Dung cho viec ung dung khong duoc lam: tao, xoa bang,
     * tao trigger, nap seed.
     *
     * @return ket noi dang mo, noi goi dong
     * @throws SQLException neu khong ket noi duoc
     */
    static Connection quanTri() throws SQLException {
        Properties cauHinh = Database.docCauHinh();
        Connection conn = DriverManager.getConnection(
                cauHinh.getProperty("url"), userQuanTri(), matKhauQuanTri());
        try (Statement st = conn.createStatement()) {
            st.execute("SET time_zone = '" + Database.MUI_GIO_PHIEN + "'");
        } catch (SQLException e) {
            conn.close();
            throw e;
        }
        return conn;
    }

    /**
     * Bo qua test neu chua cau hinh CSDL thu; neu co thi xoa moi bang, tao
     * lai theo database/schema.sql va them tai khoan admin.
     *
     * @return tai khoan Quan tri vien vua them
     */
    static TaiKhoan dungLai() {
        String url = System.getenv("BLUEMOON_DB_URL");
        assumeTrue(url != null && URL_THU.matcher(url).matches(),
                "Can BLUEMOON_DB_URL tro toi CSDL ten co 'test'");
        try (Connection conn = quanTri();
                Statement st = conn.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 0");
            for (String bang : cacBang(st)) {
                st.execute("DROP TABLE `" + bang + "`");
            }
            st.execute("SET FOREIGN_KEY_CHECKS = 1");
            for (String lenh : lenhTaoBang()) {
                st.execute(lenh);
            }
            st.executeUpdate("INSERT INTO tai_khoan (id, ten_dang_nhap, "
                    + "mat_khau_bam, ho_ten, vai_tro) VALUES (1, 'admin', "
                    + "'x', 'Quản trị viên', 'QUAN_TRI')");
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Khong dung duoc CSDL thu", e);
        }
        return new TaiKhoan(1, "admin", "x", "Quản trị viên",
                VaiTro.QUAN_TRI, true);
    }

    /**
     * Chay lai database/schema.sql tren CSDL dang co du lieu, nhu khi nang
     * cap (setup.md): khong xoa bang nao truoc.
     */
    static void chayLaiSchema() {
        try (Connection conn = quanTri();
                Statement st = conn.createStatement()) {
            for (String lenh : lenhTaoBang()) {
                st.execute(lenh);
            }
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Khong chay lai duoc schema", e);
        }
    }

    /** Chay database/seed.sql (bo chu thich va USE) tren CSDL thu. */
    static void chaySeed() {
        try (Connection conn = quanTri();
                Statement st = conn.createStatement()) {
            for (String lenh : lenhCuaTep("seed.sql")) {
                st.execute(lenh);
            }
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Khong chay duoc seed.sql", e);
        }
    }

    /**
     * Dem so dong cua mot bang.
     *
     * @param bang ten bang (hang so trong test, khong phai du lieu nhap)
     * @return so dong
     */
    static int dem(String bang) {
        try (Connection conn = Database.getConnection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT COUNT(*) FROM `" + bang + "`")) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Khong dem duoc " + bang, e);
        }
    }

    /**
     * Chay database/seed-demo.sql (bo chu thich va USE) tren CSDL thu. Tep
     * nay co dau ";" trong chuoi (noi dung nhat ky) nen tach lenh theo dau
     * ";" nam ngoai chuoi.
     */
    static void chaySeedDemo() {
        try (Connection conn = quanTri();
                Statement st = conn.createStatement()) {
            for (String lenh : lenhNgoaiChuoi("seed-demo.sql")) {
                st.execute(lenh);
            }
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Khong chay duoc seed-demo.sql", e);
        }
    }

    private static List<String> lenhNgoaiChuoi(String tep) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String dong : Files.readAllLines(Path.of("database", tep),
                StandardCharsets.UTF_8)) {
            if (!dong.strip().startsWith("--")) {
                sb.append(dong).append('\n');
            }
        }
        List<String> lenh = new ArrayList<>();
        StringBuilder hienTai = new StringBuilder();
        boolean trongChuoi = false;
        for (char c : sb.toString().toCharArray()) {
            if (c == '\'') {
                trongChuoi = !trongChuoi;
            }
            if (c == ';' && !trongChuoi) {
                String t = hienTai.toString().strip();
                if (!t.isEmpty() && !t.startsWith("USE ")) {
                    lenh.add(t);
                }
                hienTai.setLength(0);
            } else {
                hienTai.append(c);
            }
        }
        return lenh;
    }

    private static List<String> cacBang(Statement st) throws SQLException {
        List<String> bang = new ArrayList<>();
        try (ResultSet rs = st.executeQuery("SELECT table_name FROM "
                + "information_schema.tables "
                + "WHERE table_schema = DATABASE()")) {
            while (rs.next()) {
                bang.add(rs.getString(1));
            }
        }
        return bang;
    }

    /** Cac lenh trong schema.sql, bo chu thich, CREATE DATABASE va USE. */
    private static List<String> lenhTaoBang() throws IOException {
        return lenhCuaTep("schema.sql");
    }

    private static List<String> lenhCuaTep(String tep) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String dong : Files.readAllLines(Path.of("database", tep),
                StandardCharsets.UTF_8)) {
            if (!dong.strip().startsWith("--")) {
                sb.append(dong).append('\n');
            }
        }
        List<String> lenh = new ArrayList<>();
        for (String s : sb.toString().split(";")) {
            String t = s.strip();
            if (!t.isEmpty() && !t.startsWith("CREATE DATABASE")
                    && !t.startsWith("USE ")) {
                lenh.add(t);
            }
        }
        return lenh;
    }
}
