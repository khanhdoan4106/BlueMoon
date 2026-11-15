package com.bluemoon.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

/** Ba phần của công nợ một tháng và tỷ lệ đã thu (US-016, giả định 46). */
class CongNoThangTest {

    private static final YearMonth T10 = YearMonth.of(2026, 10);

    private static CongNoThang congNo(String phaiNop, String dungHan,
            String treHan) {
        return new CongNoThang(T10, true, new BigDecimal(phaiNop),
                new BigDecimal(dungHan), new BigDecimal(treHan), 0, 0);
    }

    @Test
    void baPhanCongLaiBangPhaiNop() {
        CongNoThang c = congNo("39367080", "28827960", "2869920");
        assertEquals(new BigDecimal("31697880"), c.daThu());
        assertEquals(new BigDecimal("7669200"), c.conNo());
        assertEquals(c.phaiNop(),
                c.dungHan().add(c.treHan()).add(c.conNo()));
    }

    @Test
    void tyLeLamTronXuongChiMotTramKhiThuDu() {
        assertEquals(80, congNo("39367080", "28827960", "2869920")
                .phanTram());
        assertEquals(99, congNo("1000", "900", "99").phanTram());
        assertEquals(100, congNo("1000", "900", "100").phanTram());
        assertEquals(0, congNo("1000", "0", "0").phanTram());
    }

    @Test
    void thangKhongCoCongNoKhongCoTyLe() {
        CongNoThang c = congNo("0", "0", "0");
        assertNull(c.phanTram());
        assertEquals(0, c.conNo().signum());
    }

    @Test
    void thangChuaChotKhongCoTyLe() {
        CongNoThang c = CongNoThang.chuaChot(T10);
        assertFalse(c.daChot());
        assertEquals(T10, c.thang());
        assertNull(c.phanTram());
        assertEquals(0, c.phaiNop().signum());
    }
}
