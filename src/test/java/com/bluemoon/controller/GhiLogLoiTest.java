package com.bluemoon.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bluemoon.dao.DaoException;
import com.bluemoon.util.NhatKyLoi;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Lỗi CSDL và lỗi không lường trước phải được ghi lại kèm nguyên nhân gốc
 * (rà soát phi chức năng, mục P2): người dùng chỉ thấy câu thông báo
 * chung, còn mã lỗi MySQL nằm trong log để nhóm tìm nguyên nhân.
 */
class GhiLogLoiTest {

    private final List<LogRecord> banGhi = new ArrayList<>();
    private final Handler bat = new Handler() {
        @Override
        public void publish(LogRecord r) {
            banGhi.add(r);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    @BeforeEach
    void gan() {
        NhatKyLoi.LOG.addHandler(bat);
    }

    @AfterEach
    void go() {
        NhatKyLoi.LOG.removeHandler(bat);
    }

    @Test
    void loiCsdlDuocGhiKemNguyenNhanGoc() {
        DaoException loi = new DaoException("Loi luu phieu thu",
                new SQLException("Unknown column 'x'", "42S22", 1054));

        String thongBao = LoiCsdl.thongBao(loi);

        // Nguoi dung van chi thay cau chung, khong lo chi tiet ky thuat.
        assertEquals(LoiCsdl.LOI_DU_LIEU, thongBao);
        assertEquals(1, banGhi.size());
        assertEquals(Level.SEVERE, banGhi.get(0).getLevel());
        assertSame(loi, banGhi.get(0).getThrown());
    }

    @Test
    void loiKhongLuongTruocDuocGhiDungMotLan() {
        IllegalStateException loi =
                new IllegalStateException("Location is not set");

        LoiKhongMongDoi.ghiVaThongBao(loi);

        assertEquals(1, banGhi.size());
        assertSame(loi, banGhi.get(0).getThrown());

        // Loi CSDL di qua LoiCsdl: khong ghi hai lan.
        banGhi.clear();
        LoiKhongMongDoi.ghiVaThongBao(new DaoException("x",
                new SQLException("Out of range", "22003")));
        assertEquals(1, banGhi.size());
    }

    @Test
    void tepLogCoMaLoiVaCauLenhGayLoi(@TempDir Path thuMuc)
            throws IOException {
        Path tep = thuMuc.resolve("bluemoon.log");
        Handler ghiTep = NhatKyLoi.ghiRaTep(tep);
        try {
            LoiCsdl.thongBao(new DaoException("Loi luu phieu thu",
                    new SQLException("Unknown column 'x'", "42S22", 1054)));
        } finally {
            NhatKyLoi.LOG.removeHandler(ghiTep);
            ghiTep.close();
        }

        String noiDung = Files.readString(tep, StandardCharsets.UTF_8);
        assertTrue(noiDung.contains("Loi luu phieu thu"), noiDung);
        assertTrue(noiDung.contains("Unknown column 'x'"), noiDung);
        assertTrue(noiDung.contains("java.sql.SQLException"), noiDung);
    }

    @Test
    void khongTaoDuocTepLogThiUngDungVanChay(@TempDir Path thuMuc) {
        // Thu muc khong ton tai: khong nem loi, chi con log ra console.
        assertEquals(null, NhatKyLoi.ghiRaTep(
                thuMuc.resolve("khong-co").resolve("bluemoon.log")));
    }
}
