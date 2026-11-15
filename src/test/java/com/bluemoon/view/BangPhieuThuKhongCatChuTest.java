package com.bluemoon.view;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bluemoon.controller.ThuPhiController;
import com.bluemoon.model.HinhThucThanhToan;
import com.bluemoon.service.ThuPhiGia;
import com.bluemoon.service.ThuPhiService;
import com.bluemoon.util.PhienLamViec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tab "Phiếu thu hôm nay" ở cửa sổ nhỏ nhất 1120×645 với chữ 16 px của
 * giao-dien.css: số biên lai, thời điểm, hình thức và tiêu đề cột hiện đủ,
 * không bị rút gọn bằng "…" (rà soát giao diện, mục G1). Bảng rỗng không
 * lộ lỗi này, nên test lập một phiếu thật qua Service giả trước khi vẽ.
 */
class BangPhieuThuKhongCatChuTest {

    @BeforeAll
    static void khoiDong() throws Exception {
        FxTest.khoiDong();
    }

    @AfterEach
    void xoaPhien() {
        PhienLamViec.xoa();
    }

    @Test
    void soBienLaiVaThoiDiemHienDuO1120() throws Exception {
        ThuPhiGia gia = ThuPhiGia.moi();
        ThuPhiService service = gia.service();
        int hoId = service.traCuu(ThuPhiGia.THU_QUY, ThuPhiGia.MA).get(0)
                .ho().getId();
        List<Integer> ids = new ArrayList<>();
        service.thongTin(ThuPhiGia.THU_QUY, hoId).khoanChuaDong()
                .forEach(k -> ids.add(k.phaiNopId()));
        service.thu(ThuPhiGia.THU_QUY, hoId, ids, Map.of(),
                HinhThucThanhToan.CHUYEN_KHOAN);
        PhienLamViec.dat(ThuPhiGia.THU_QUY);

        List<String> chu = new ArrayList<>();
        List<String> biCat = FxTest.chay(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/bluemoon/view/thu-phi.fxml"));
            loader.setControllerFactory(c -> new ThuPhiController(service));
            Parent goc = loader.load();
            Scene scene = new Scene(goc, 1120, 645);
            scene.getStylesheets().add(getClass().getResource(
                    "/com/bluemoon/css/giao-dien.css").toExternalForm());
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.show();
            TabPane tab = (TabPane) loader.getNamespace().get("tabPane");
            tab.getSelectionModel().select(
                    (Tab) loader.getNamespace().get("phieuTab"));
            for (int i = 0; i < 4; i++) {
                goc.applyCss();
                goc.layout();
            }
            List<String> loi = new ArrayList<>();
            gom(goc, chu, loi);
            stage.close();
            return loi;
        });

        assertTrue(chu.stream().anyMatch(c -> c.startsWith("PT-")),
                "bảng phải có phiếu vừa lập: " + chu);
        assertTrue(chu.contains("Chuyển khoản"), chu.toString());
        assertTrue(biCat.isEmpty(), "chữ bị cắt: " + biCat);
    }

    /** Gom chữ đang hiện và các nhãn, ô bị rút gọn bằng "…". */
    private static void gom(Node n, List<String> chu, List<String> biCat) {
        if (!n.isVisible()) {
            return;
        }
        if (n instanceof Labeled l && l.getText() != null
                && !l.getText().isEmpty()) {
            chu.add(l.getText());
            for (Node con : l.getChildrenUnmodifiable()) {
                if (con instanceof Text t && !t.getText().isEmpty()
                        && !t.getText().equals(l.getText())) {
                    biCat.add("\"" + l.getText() + "\" hiện \""
                            + t.getText() + "\"");
                }
            }
        }
        if (n instanceof Parent p) {
            for (Node con : p.getChildrenUnmodifiable()) {
                gom(con, chu, biCat);
            }
        }
    }
}
