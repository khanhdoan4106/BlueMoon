package com.bluemoon.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Log lỗi kỹ thuật của ứng dụng, để tìm nguyên nhân khi người dùng chỉ
 * thấy câu thông báo chung. Dùng {@code java.util.logging} có sẵn trong
 * JDK: mặc định in ra console, {@link #caiDat()} ghi thêm vào tệp
 * {@code bluemoon.log} ở thư mục chạy ứng dụng.
 *
 * <p>Khác với nhật ký thao tác (bảng {@code nhat_ky}, TS-002): tệp này ghi
 * lỗi kỹ thuật cho người hỗ trợ, không ghi thao tác nghiệp vụ.
 */
public final class NhatKyLoi {

    /** Logger chung của ứng dụng. */
    public static final Logger LOG = Logger.getLogger("com.bluemoon");

    /** Tên tệp log, nằm trong .gitignore. */
    static final String TEN_TEP = "bluemoon.log";
    /**
     * Tệp tối đa 1 MB; đầy thì ghi lại từ đầu. Chỉ một tệp, vì từ hai tệp
     * trở lên FileHandler thêm đuôi .0, .1 vào tên.
     */
    private static final int GIOI_HAN_BYTE = 1024 * 1024;
    private static final int SO_TEP = 1;

    private NhatKyLoi() {
    }

    /** Ghi thêm log vào tệp mặc định. Gọi một lần khi khởi động. */
    public static void caiDat() {
        ghiRaTep(Path.of(TEN_TEP));
    }

    /**
     * Ghi lỗi kèm stack trace.
     *
     * @param moTa việc đang làm khi lỗi
     * @param loi lỗi cần ghi
     */
    public static void ghi(String moTa, Throwable loi) {
        LOG.log(Level.SEVERE, moTa, loi);
    }

    /**
     * Gắn thêm đích ghi là một tệp (ghi nối tiếp, UTF-8). Không tạo được
     * tệp thì bỏ qua, ứng dụng vẫn chạy và log vẫn ra console.
     *
     * @param tep đường dẫn tệp log
     * @return handler vừa gắn, null nếu không tạo được tệp
     */
    public static Handler ghiRaTep(Path tep) {
        try {
            FileHandler h = new FileHandler(tep.toString(), GIOI_HAN_BYTE,
                    SO_TEP, true);
            h.setEncoding("UTF-8");
            h.setFormatter(new SimpleFormatter());
            LOG.addHandler(h);
            return h;
        } catch (IOException | SecurityException e) {
            LOG.log(Level.WARNING, "Khong tao duoc tep log " + tep, e);
            return null;
        }
    }
}
