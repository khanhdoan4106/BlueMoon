package com.bluemoon.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bluemoon.model.CongNoThang;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Chữ của biểu đồ công nợ (US-016, giả định 46), không cần JavaFX: nhãn
 * trên đầu cột, tooltip của cột, chú thích dưới biểu đồ, khoảng trống giữa
 * các cột.
 */
class BieuDoThuPhiChuTest {

    private static final YearMonth T10 = YearMonth.of(2026, 10);
    private static final YearMonth T11 = YearMonth.of(2026, 11);
    private static final CongNoThang THANG_10 = new CongNoThang(T10, true,
            new BigDecimal("39367080"), new BigDecimal("28827960"),
            new BigDecimal("2869920"), 31, 39);

    @Test
    void nhanTrenDauCot() {
        assertEquals("80%", BieuDoThuPhi.nhanCot(THANG_10));
        assertEquals("Chưa chốt",
                BieuDoThuPhi.nhanCot(CongNoThang.chuaChot(T10)));
        assertEquals("–", BieuDoThuPhi.nhanCot(new CongNoThang(T10, true,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0)));
    }

    @Test
    void tooltipCoDuBaPhanPhaiNopTyLeVaSoHo() {
        assertEquals("Tháng 10/2026\n"
                + "Phải nộp: 39.367.080 đ\n"
                + "Thu đúng hạn: 28.827.960 đ\n"
                + "Thu trễ hạn: 2.869.920 đ\n"
                + "Còn nợ: 7.669.200 đ\n"
                + "Đã thu: 80%\n"
                + "Đã đóng đủ: 31/39 hộ",
                BieuDoThuPhi.moTa(THANG_10, false));
        assertTrue(BieuDoThuPhi.moTa(THANG_10, true).startsWith(
                "Tháng 10/2026 (đang thu, tính đến hôm nay)\n"));
        assertEquals("Tháng 10/2026 chưa chốt công nợ",
                BieuDoThuPhi.moTa(CongNoThang.chuaChot(T10), false));
    }

    @Test
    void chuThichNoiCachTinhKhacTheDaThuThangNay() {
        CongNoThang thang11 = new CongNoThang(T11, true,
                new BigDecimal("39367080"), new BigDecimal("22615560"),
                BigDecimal.ZERO, 22, 39);
        String chu = BieuDoThuPhi.chuThich(List.of(THANG_10, thang11), T11);
        assertEquals("Mỗi cột là công nợ bắt buộc của một tháng; hạn nộp là"
                + " ngày cuối tháng đó. Tháng 11 đang thu, tính đến hôm nay."
                + " Thẻ \"Đã thu tháng này\" tính theo ngày thu nên có thể"
                + " khác.", chu);
        assertTrue(BieuDoThuPhi.chuThich(List.of(THANG_10,
                CongNoThang.chuaChot(T11)), T11)
                .contains("Tháng 11 chưa chốt công nợ."));
    }

    @Test
    void cotRongBonMuoiLamPhanTramKhoangCachToiDaBayHai() {
        // Khoang cach 100: cot 45, trong 55.
        assertEquals(55, BieuDoThuPhi.khoangTrong(100), 1e-9);
        // Khoang cach 300: 45% la 135, vuot gioi han nen cot 72.
        assertEquals(300 - 72, BieuDoThuPhi.khoangTrong(300), 1e-9);
    }
}
