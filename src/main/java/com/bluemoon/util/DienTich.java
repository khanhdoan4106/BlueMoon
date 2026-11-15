package com.bluemoon.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Hiển thị diện tích thống nhất ở mọi màn hình và trong nhật ký: kiểu Việt
 * Nam (dấu phẩy thập phân), luôn hai chữ số lẻ, vd "75,50 m²".
 */
public final class DienTich {

    /** Đơn vị, kèm dấu cách phía trước. */
    public static final String DON_VI = " m²";

    private static final DecimalFormatSymbols VI =
            DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN"));

    private DienTich() {
    }

    /**
     * Diện tích kèm đơn vị, vd "75,50 m²".
     *
     * @param m2 diện tích, mét vuông
     * @return chuỗi hiển thị; rỗng nếu null
     */
    public static String hien(BigDecimal m2) {
        return m2 == null ? "" : so(m2) + DON_VI;
    }

    /**
     * Chỉ con số, vd "75,50", để điền vào ô nhập có nhãn đã ghi đơn vị.
     *
     * @param m2 diện tích, mét vuông
     * @return chuỗi số; rỗng nếu null
     */
    public static String so(BigDecimal m2) {
        return m2 == null ? ""
                : new DecimalFormat("#,##0.00", VI).format(m2);
    }
}
