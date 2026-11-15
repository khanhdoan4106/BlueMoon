package com.bluemoon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LauncherTest {

    private final TimeZone muiGioCu = TimeZone.getDefault();

    @AfterEach
    void khoiPhuc() {
        TimeZone.setDefault(muiGioCu);
    }

    @Test
    void khoiDongDatMuiGioVietNamDuMayDatMuiGioKhac() {
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"));

        Launcher.datMuiGioViet();

        assertEquals("Asia/Ho_Chi_Minh", TimeZone.getDefault().getID());
        assertEquals(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")),
                LocalDate.now(), "LocalDate.now() theo giờ Việt Nam");
    }

    /**
     * "mvn javafx:run" chạy thẳng App (pom.xml), không qua Launcher.main,
     * nên App phải tự đặt múi giờ trước khi mở màn hình đầu tiên.
     */
    @Test
    void chayThangAppCungDatMuiGioVietNam() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"));

        new App().init();

        assertEquals("Asia/Ho_Chi_Minh", TimeZone.getDefault().getID());
    }
}
