package com.bluemoon.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bluemoon.util.NgayUtil;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Màn Nhân khẩu ở cửa sổ nhỏ nhất (1120×645, trừ thanh menu 64 px): hàng
 * nút của form bên phải ghim ở cuối form, luôn thấy mà không phải cuộn
 * (rà soát giao diện, mục T4). Ô ngày gợi ý bằng tiếng Việt (mục T3).
 */
class NhanKhauNutGhimTest {

    private static final double RONG = 1120;
    /** 645 trừ thanh menu của Trang chính. */
    private static final double CAO = 645 - 64;

    @BeforeAll
    static void khoiDong() throws Exception {
        FxTest.khoiDong();
    }

    @Test
    void hangNutLuonThayKhongNamTrongVungCuon() throws Exception {
        List<String> loi = FxTest.chay(() -> {
            String fxml;
            try (InputStream vao = getClass().getResourceAsStream(
                    "/com/bluemoon/view/nhan-khau.fxml")) {
                fxml = new String(vao.readAllBytes(), StandardCharsets.UTF_8)
                        .replaceAll("\\s+fx:controller=\"[^\"]*\"", "")
                        .replaceAll("\\s+on[A-Z]\\w*=\"#[^\"]*\"", "");
            }
            FXMLLoader loader = new FXMLLoader();
            Parent goc = loader.load(new ByteArrayInputStream(
                    fxml.getBytes(StandardCharsets.UTF_8)));
            Scene scene = new Scene(goc, RONG, CAO);
            scene.getStylesheets().add(getClass().getResource(
                    "/com/bluemoon/css/giao-dien.css").toExternalForm());
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.show();
            for (int i = 0; i < 4; i++) {
                goc.applyCss();
                goc.layout();
            }
            List<String> kq = new ArrayList<>();
            for (String id : List.of("luuButton", "chuHoButton",
                    "chuyenDiButton")) {
                Button nut = (Button) loader.getNamespace().get(id);
                Bounds b = nut.localToScene(nut.getBoundsInLocal());
                if (b.getMaxY() > CAO + 0.5 || b.getMinY() < 0) {
                    kq.add(id + " nằm ngoài cửa sổ: " + b.getMinY() + " – "
                            + b.getMaxY());
                }
                for (Node cha = nut.getParent(); cha != null;
                        cha = cha.getParent()) {
                    if (cha instanceof ScrollPane) {
                        kq.add(id + " nằm trong vùng cuộn");
                    }
                }
            }
            // Nhan cua form (ke ca "Noi thuong tru *") khong bi rut gon.
            gomBiCat(goc, kq);
            stage.close();
            return kq;
        });
        assertTrue(loi.isEmpty(), loi.toString());
    }

    private static void gomBiCat(Node n, List<String> kq) {
        if (n instanceof Labeled l && l.getText() != null
                && !l.getText().isEmpty() && l.isVisible()) {
            for (Node con : l.getChildrenUnmodifiable()) {
                if (con instanceof Text t && !t.getText().isEmpty()
                        && !t.getText().equals(l.getText())) {
                    kq.add("\"" + l.getText() + "\" hiện \""
                            + t.getText() + "\"");
                }
            }
        }
        if (n instanceof Parent p) {
            for (Node con : p.getChildrenUnmodifiable()) {
                gomBiCat(con, kq);
            }
        }
    }

    @Test
    void oNgayGoiYBangTiengViet() throws Exception {
        String goiY = FxTest.chay(() -> {
            DatePicker p = new DatePicker();
            NgayUtil.ganDinhDang(p);
            return p.getPromptText();
        });
        assertEquals("ngày/tháng/năm", goiY);
    }
}
