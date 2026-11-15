package com.bluemoon.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.bluemoon.config.Database;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Quyền của user CSDL mà ứng dụng dùng (NFR-004, AC-069; rà soát phi chức
 * năng, mục P1): chỉ {@code SELECT, INSERT, UPDATE}. Trước đây user này có
 * {@code GRANT ALL} nên gỡ được trigger rồi xóa nhật ký; các test dưới đây
 * trượt với user như vậy.
 *
 * <p>Cần hai user (docs/setup.md, mục 4): user ứng dụng ở
 * {@code BLUEMOON_DB_USER} và user quản trị ở
 * {@code BLUEMOON_DB_ADMIN_USER}. Chỉ có một user thì bỏ qua, riêng trên CI
 * thì trượt để cấu hình thiếu không lặng lẽ tắt mất kiểm tra này.
 */
class QuyenCsdlIT {

    /** Mã lỗi MySQL "command denied to user … for table …". */
    private static final int LOI_KHONG_CO_QUYEN = 1142;
    private static final Set<String> QUYEN_CHO_PHEP =
            Set.of("USAGE", "SELECT", "INSERT", "UPDATE");
    private static final Pattern MAU_GRANT =
            Pattern.compile("GRANT (.+?) ON .+ TO .+");

    @BeforeEach
    void chuanBi() throws SQLException {
        CsdlThu.dungLai();
        if (!CsdlThu.tachUser()) {
            assertNull(System.getenv("CI"), "CI phải đặt "
                    + CsdlThu.BIEN_QUAN_TRI + " khác user ứng dụng");
        }
        assumeTrue(CsdlThu.tachUser(), "Can " + CsdlThu.BIEN_QUAN_TRI
                + " khac user ung dung de kiem quyen");
        // User ung dung van ghi duoc nhat ky (INSERT).
        try (Connection c = Database.getConnection();
                Statement st = c.createStatement()) {
            st.executeUpdate("INSERT INTO nhat_ky (tai_khoan_id, thao_tac, "
                    + "doi_tuong) VALUES (1, 'THEM_HO', 'Căn hộ A0101')");
        }
    }

    @Test
    void userUngDungKhongGoDuocTriggerNhatKy() throws SQLException {
        biTuChoi("DROP TRIGGER trg_nky_chan_sua",
                "DROP TRIGGER trg_nky_chan_xoa");
        assertEquals(List.of("trg_nky_chan_sua", "trg_nky_chan_xoa"),
                triggerCuaNhatKy());
    }

    @Test
    void userUngDungKhongXoaDuocNhatKy() throws SQLException {
        biTuChoi("DELETE FROM nhat_ky", "TRUNCATE TABLE nhat_ky",
                "DROP TABLE nhat_ky");
        // UPDATE la quyen ung dung can cho bang khac; nhat_ky do trigger
        // chan.
        try (Connection c = Database.getConnection();
                Statement st = c.createStatement()) {
            SQLException e = assertThrows(SQLException.class,
                    () -> st.executeUpdate(
                            "UPDATE nhat_ky SET noi_dung = 'sửa lén'"));
            assertEquals("45000", e.getSQLState());
        }
        assertEquals(1, CsdlThu.dem("nhat_ky"));
    }

    @Test
    void userUngDungKhongDoiDuocCauTrucVaKhongXoaCung() {
        biTuChoi("ALTER TABLE nhat_ky ADD COLUMN cot_la INT",
                "ALTER TABLE phieu_thu DROP COLUMN ly_do_huy",
                "CREATE TABLE bang_la (id INT)",
                "DROP TABLE phieu_thu_chi_tiet",
                "DELETE FROM phieu_thu",
                "DELETE FROM tai_khoan");
    }

    @Test
    void quyenCuaUserUngDungChiGomDocThemSua() throws SQLException {
        List<String> dong = new ArrayList<>();
        try (Connection c = Database.getConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("SHOW GRANTS")) {
            while (rs.next()) {
                dong.add(rs.getString(1));
            }
        }
        assertTrue(dong.stream().anyMatch(d -> d.contains("SELECT")),
                "phải có quyền đọc: " + dong);
        for (String d : dong) {
            Matcher m = MAU_GRANT.matcher(d);
            assertTrue(m.matches(), "dòng quyền lạ: " + d);
            for (String quyen : m.group(1).split(",\\s*")) {
                assertTrue(QUYEN_CHO_PHEP.contains(quyen),
                        "user ứng dụng thừa quyền " + quyen + ": " + d);
            }
        }
    }

    /** Mỗi lệnh chạy bằng user ứng dụng phải bị MySQL từ chối vì quyền. */
    private static void biTuChoi(String... cacLenh) {
        for (String lenh : cacLenh) {
            try (Connection c = Database.getConnection();
                    Statement st = c.createStatement()) {
                SQLException e = assertThrows(SQLException.class,
                        () -> st.execute(lenh),
                        "user ứng dụng chạy được: " + lenh);
                assertEquals(LOI_KHONG_CO_QUYEN, e.getErrorCode(),
                        lenh + ": " + e.getMessage());
            } catch (SQLException e) {
                throw new IllegalStateException(lenh, e);
            }
        }
    }

    private static List<String> triggerCuaNhatKy() throws SQLException {
        List<String> ten = new ArrayList<>();
        try (Connection c = CsdlThu.quanTri();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("SELECT trigger_name FROM "
                        + "information_schema.triggers WHERE trigger_schema "
                        + "= DATABASE() AND event_object_table = 'nhat_ky' "
                        + "ORDER BY trigger_name")) {
            while (rs.next()) {
                ten.add(rs.getString(1));
            }
        }
        return ten;
    }
}
