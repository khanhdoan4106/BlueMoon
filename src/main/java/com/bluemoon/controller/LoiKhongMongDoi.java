package com.bluemoon.controller;

import com.bluemoon.config.CauHinhException;
import com.bluemoon.dao.DaoException;
import com.bluemoon.service.NghiepVuException;
import com.bluemoon.util.NhatKyLoi;
import javafx.application.Platform;

/**
 * Bo xu ly loi toan cuc: loi ma controller khong bat (vd tai FXML, loi bat
 * thuong trong su kien) hien hop thoai thay vi chi in ra console, de nguoi
 * dung khong thay nut "khong co tac dung".
 */
public final class LoiKhongMongDoi {

    /** Thong bao cho loi khong thuoc loai da biet. */
    static final String LOI_CHUNG = "Có lỗi không mong muốn, vui lòng thử"
            + " lại. Nếu lỗi lặp lại, báo người quản trị hệ thống.";

    private LoiKhongMongDoi() {
    }

    /** Dat bo xu ly cho moi luong cua ung dung. Goi mot lan khi khoi dong. */
    public static void caiDat() {
        Thread.setDefaultUncaughtExceptionHandler((luong, loi) -> {
            String noiDung = ghiVaThongBao(loi);
            if (Platform.isFxApplicationThread()) {
                HopThoai.canhBao(noiDung);
            } else {
                Platform.runLater(() -> HopThoai.canhBao(noiDung));
            }
        });
    }

    /**
     * Ghi loi vao log roi tra noi dung hien cho nguoi dung. Loi CSDL do
     * {@link LoiCsdl#thongBao} ghi, nen khong ghi lan hai o day.
     *
     * @param loi loi khong duoc bat
     * @return thong bao phu hop loai loi
     */
    static String ghiVaThongBao(Throwable loi) {
        if (!(loi instanceof CauHinhException
                || loi instanceof DaoException)) {
            NhatKyLoi.ghi("Loi khong mong doi", loi);
        }
        return thongBao(loi);
    }

    /**
     * Noi dung hien cho nguoi dung.
     *
     * @param loi loi khong duoc bat
     * @return thong bao phu hop loai loi
     */
    static String thongBao(Throwable loi) {
        if (loi instanceof NghiepVuException) {
            return loi.getMessage();
        }
        if (loi instanceof CauHinhException || loi instanceof DaoException) {
            return LoiCsdl.thongBao((RuntimeException) loi);
        }
        return LOI_CHUNG;
    }
}
