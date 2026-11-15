package com.bluemoon.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

/**
 * Công nợ của một tháng trên biểu đồ Trang chính (US-016, giả định 46):
 * tổng phải nộp các khoản bắt buộc của đợt chốt tháng đó, chia ba phần là
 * thu đúng hạn, thu trễ hạn và còn nợ. Hạn nộp là ngày cuối cùng của tháng
 * được chốt. Chỉ tính phiếu thu chưa hủy; khoản đã sửa về 0 đ không tính
 * (giả định 33).
 *
 * @param thang tháng công nợ
 * @param daChot tháng đã chốt công nợ hay chưa; chưa chốt thì mọi số là 0
 * @param phaiNop tổng phải nộp
 * @param dungHan phần đã thu bằng phiếu lập không sau hạn nộp
 * @param treHan phần đã thu bằng phiếu lập sau hạn nộp
 * @param soHoDongDu số hộ không còn khoản nào chưa đóng trong tháng
 * @param tongSoHo số hộ có khoản phải nộp trong tháng
 */
public record CongNoThang(YearMonth thang, boolean daChot,
        BigDecimal phaiNop, BigDecimal dungHan, BigDecimal treHan,
        int soHoDongDu, int tongSoHo) {

    /**
     * Tháng chưa chốt công nợ.
     *
     * @param thang tháng
     * @return công nợ rỗng, {@code daChot} là false
     */
    public static CongNoThang chuaChot(YearMonth thang) {
        return new CongNoThang(thang, false, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, 0, 0);
    }

    /**
     * Tổng đã thu, đúng hạn cộng trễ hạn.
     *
     * @return số tiền đã thu
     */
    public BigDecimal daThu() {
        return dungHan.add(treHan);
    }

    /**
     * Phần chưa thu, để ba phần cộng lại đúng bằng phải nộp.
     *
     * @return số tiền còn nợ
     */
    public BigDecimal conNo() {
        return phaiNop.subtract(daThu());
    }

    /**
     * Tỷ lệ đã thu trên phải nộp, làm tròn xuống để chỉ ghi 100% khi thu
     * đủ.
     *
     * @return phần trăm; null nếu tháng chưa chốt hoặc không có khoản phải
     *         nộp
     */
    public Integer phanTram() {
        if (!daChot || phaiNop.signum() <= 0) {
            return null;
        }
        return daThu().multiply(BigDecimal.valueOf(100))
                .divide(phaiNop, 0, RoundingMode.DOWN).intValue();
    }
}
