package com.bluemoon.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bluemoon.controller.BieuDoThuPhi;
import com.bluemoon.model.CongNoThang;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Biểu đồ cột chồng "Thu phí 6 tháng gần nhất" (US-016, giả định 46) theo
 * "Quy tắc biểu đồ" trong docs/design/ui.md, vẽ thật bằng Monocle: tên
 * trục X, đơn vị trục Y nằm ngang ở đầu trục (không chữ nào bị xoay), trục
 * Y từ 0, ba phần của cột cộng lại bằng phải nộp, tỷ lệ trên đầu cột,
 * "Chưa chốt", chú giải, bề rộng cột.
 */
class BieuDoThuPhiTest {

    private static final YearMonth T11 = YearMonth.of(2026, 11);
    private static final double SAI_SO = 1e-6;

    @BeforeAll
    static void khoiDong() throws Exception {
        FxTest.khoiDong();
    }

    /** Tháng đã chốt, số tiền tính bằng triệu đồng. */
    private static CongNoThang chot(YearMonth t, double phaiNop,
            double dungHan, double treHan) {
        return new CongNoThang(t, true, trieu(phaiNop), trieu(dungHan),
                trieu(treHan), 30, 39);
    }

    private static BigDecimal trieu(double so) {
        return BigDecimal.valueOf(Math.round(so * 1_000_000));
    }

    /** Sáu tháng như seed-demo: T6 đến T11 đều đã chốt. */
    private static List<CongNoThang> sauThangDaChot() {
        return List.of(
                chot(T11.minusMonths(5), 35.8, 29.7, 2.0),
                chot(T11.minusMonths(4), 36.6, 27.1, 4.9),
                chot(T11.minusMonths(3), 37.5, 26.0, 6.0),
                chot(T11.minusMonths(2), 38.0, 26.2, 6.8),
                chot(T11.minusMonths(1), 39.4, 28.8, 2.9),
                chot(T11, 39.4, 22.6, 0));
    }

    private record Ve(BieuDoThuPhi bieuDo, Stage stage) {
        List<XYChart.Data<String, Number>> phan(int chuoi) {
            return bieuDo.getData().get(chuoi).getData();
        }

        List<Text> nhan() {
            List<Text> ds = new ArrayList<>();
            for (Node n : bieuDo.lookupAll("." + BieuDoThuPhi.NHAN_COT)) {
                ds.add((Text) n);
            }
            return ds;
        }

        /** Mọi chữ đang hiện trong biểu đồ. */
        List<Text> chu() {
            List<Text> kq = new ArrayList<>();
            gomChu(bieuDo, kq);
            return kq;
        }
    }

    private static void gomChu(Node n, List<Text> kq) {
        if (!n.isVisible()) {
            return;
        }
        if (n instanceof Text t && t.getText() != null
                && !t.getText().isBlank()) {
            kq.add(t);
        }
        if (n instanceof Parent p) {
            p.getChildrenUnmodifiable().forEach(c -> gomChu(c, kq));
        }
    }

    private static Ve ve(List<CongNoThang> duLieu, YearMonth hienTai) {
        return ve(duLieu, hienTai, 800);
    }

    private static Ve ve(List<CongNoThang> duLieu, YearMonth hienTai,
            double rong) {
        BieuDoThuPhi b = new BieuDoThuPhi();
        b.hien(duLieu, hienTai);
        Scene scene = new Scene(b, rong, 460);
        scene.getStylesheets().add(BieuDoThuPhiTest.class
                .getResource("/com/bluemoon/css/giao-dien.css")
                .toExternalForm());
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        // Be rong cot duoc dat trong luc bo tri nen can vai luot.
        for (int i = 0; i < 3; i++) {
            b.applyCss();
            b.layout();
        }
        return new Ve(b, stage);
    }

    @Test
    void coTenTrucXDonViNamNgangVaChuGiai() throws Exception {
        FxTest.chay(() -> {
            Ve v = ve(sauThangDaChot(), T11);
            assertEquals("Tháng (năm 2026)", v.bieuDo().getXAxis().getLabel());
            assertEquals(List.of("T6", "T7", "T8", "T9", "T10", "T11"),
                    ((CategoryAxis) v.bieuDo().getXAxis()).getCategories());
            // Don vi khong con la ten truc Y xoay doc ma nam ngang o dau
            // truc.
            String tenTrucY = v.bieuDo().getYAxis().getLabel();
            assertTrue(tenTrucY == null || tenTrucY.isEmpty());
            assertEquals("Triệu đồng", v.bieuDo().getTitle());
            Text donVi = v.chu().stream()
                    .filter(t -> t.getText().equals("Triệu đồng"))
                    .findFirst().orElseThrow();
            Bounds bd = donVi.localToScene(donVi.getBoundsInLocal());
            Node nen = v.bieuDo().lookup(".chart-plot-background");
            Bounds bn = nen.localToScene(nen.getBoundsInLocal());
            assertTrue(bd.getMaxY() <= bn.getMinY() + 0.5,
                    "đơn vị nằm trên đầu trục Y");
            assertTrue(bd.getMinX() < bn.getMinX(),
                    "đơn vị canh trái, phía trên trục Y");
            assertTrue(bd.getWidth() > bd.getHeight(), "chữ nằm ngang");

            assertTrue(v.bieuDo().isLegendVisible(), "Ba chuỗi");
            assertEquals(List.of("Thu đúng hạn", "Thu trễ hạn", "Còn nợ"),
                    v.bieuDo().getData().stream()
                            .map(XYChart.Series::getName).toList());
            List<String> chu = v.chu().stream().map(Text::getText).toList();
            assertTrue(chu.containsAll(List.of("Thu đúng hạn",
                    "Thu trễ hạn", "Còn nợ")), chu.toString());
            v.stage().close();
            return null;
        });
    }

    @Test
    void khongChuNaoBiXoayVaDuCoChu() throws Exception {
        FxTest.chay(() -> {
            Ve v = ve(sauThangDaChot(), T11);
            for (Text t : v.chu()) {
                for (Node n = t; n != null && n != v.bieuDo().getParent();
                        n = n.getParent()) {
                    assertEquals(0, n.getRotate(), SAI_SO,
                            "\"" + t.getText() + "\" bị xoay");
                }
                assertTrue(t.getFont().getSize() >= 14 - 0.01,
                        "\"" + t.getText() + "\" cỡ " + t.getFont().getSize());
            }
            v.stage().close();
            return null;
        });
    }

    @Test
    void haiNamThiNhanCoNam() throws Exception {
        FxTest.chay(() -> {
            YearMonth t3 = YearMonth.of(2027, 3);
            List<CongNoThang> ds = new ArrayList<>();
            for (int i = 5; i >= 0; i--) {
                ds.add(chot(t3.minusMonths(i), 10, 5, 1));
            }
            Ve v = ve(ds, t3);
            assertEquals("Tháng", v.bieuDo().getXAxis().getLabel());
            assertEquals(List.of("T10/26", "T11/26", "T12/26", "T1/27",
                    "T2/27", "T3/27"), v.phan(0).stream()
                    .map(XYChart.Data::getXValue).toList());
            v.stage().close();
            return null;
        });
    }

    @Test
    void trucYBatDauTuKhongTheoMocTronCoLuoiNgang() throws Exception {
        FxTest.chay(() -> {
            Ve v = ve(sauThangDaChot(), T11);
            NumberAxis y = (NumberAxis) v.bieuDo().getYAxis();
            assertFalse(y.isAutoRanging());
            assertEquals(0, y.getLowerBound());
            // Cot cao nhat 39,4 trieu: thang 0 - 50, buoc 10.
            assertEquals(50, y.getUpperBound());
            assertEquals(10, y.getTickUnit());
            assertTrue(v.bieuDo().isHorizontalGridLinesVisible());
            assertFalse(v.bieuDo().getVerticalGridLinesVisible());
            v.stage().close();
            return null;
        });
    }

    @Test
    void baPhanChongLenNhauCongLaiBangPhaiNop() throws Exception {
        FxTest.chay(() -> {
            List<CongNoThang> ds = sauThangDaChot();
            Ve v = ve(ds, T11);
            NumberAxis y = (NumberAxis) v.bieuDo().getYAxis();
            for (int i = 0; i < ds.size(); i++) {
                CongNoThang c = ds.get(i);
                double tong = 0;
                for (int s = 0; s < 3; s++) {
                    tong += v.phan(s).get(i).getYValue().doubleValue();
                }
                assertEquals(c.phaiNop().doubleValue() / 1e6, tong, SAI_SO);
                assertEquals(c.dungHan().doubleValue() / 1e6,
                        v.phan(0).get(i).getYValue().doubleValue(), SAI_SO);
                assertEquals(c.treHan().doubleValue() / 1e6,
                        v.phan(1).get(i).getYValue().doubleValue(), SAI_SO);
                assertEquals(c.conNo().doubleValue() / 1e6,
                        v.phan(2).get(i).getYValue().doubleValue(), SAI_SO);
                // Tu duoi len: dung han sat truc, con no tren cung, dinh
                // cot dung muc phai nop tren truc Y (cot dung ty le).
                Bounds dung = v.phan(0).get(i).getNode().getBoundsInParent();
                Bounds no = v.phan(2).get(i).getNode().getBoundsInParent();
                assertEquals(y.getDisplayPosition(0), dung.getMaxY(), 1.0);
                assertEquals(y.getDisplayPosition(tong), no.getMinY(), 1.0);
                assertTrue(no.getMaxY() <= dung.getMinY() + 1.0);
            }
            // Thang 11 khong co phan tre han: o do bi an, khong con vien.
            assertFalse(v.phan(1).get(5).getNode().isVisible());
            assertTrue(v.phan(1).get(4).getNode().isVisible());
            v.stage().close();
            return null;
        });
    }

    @Test
    void tyLeTrenDauCotChuaChotVaKhongCoCongNo() throws Exception {
        FxTest.chay(() -> {
            List<CongNoThang> ds = List.of(
                    CongNoThang.chuaChot(T11.minusMonths(5)),
                    chot(T11.minusMonths(4), 0, 0, 0),
                    chot(T11.minusMonths(3), 37.5, 26.0, 6.0),
                    chot(T11.minusMonths(2), 38.0, 38.0, 0),
                    chot(T11.minusMonths(1), 39.4, 28.8, 2.9),
                    chot(T11, 39.4, 22.6, 0));
            Ve v = ve(ds, T11);
            List<Text> nhan = v.nhan();
            assertEquals(List.of("Chưa chốt", "–", "85%", "100%", "80%",
                    "57%"), nhan.stream().map(Text::getText).toList());
            CategoryAxis x = (CategoryAxis) v.bieuDo().getXAxis();
            NumberAxis y = (NumberAxis) v.bieuDo().getYAxis();
            for (int i = 0; i < nhan.size(); i++) {
                Bounds chu = nhan.get(i).getBoundsInParent();
                double dinh = y.getDisplayPosition(
                        ds.get(i).phaiNop().doubleValue() / 1e6);
                assertTrue(chu.getMaxY() <= dinh + 0.5,
                        "nhãn nằm trên đầu cột " + i);
                assertEquals(x.getDisplayPosition(
                        x.getCategories().get(i)), chu.getCenterX(), 1.0);
            }
            // Thang chua chot khong ve cot nao.
            for (int s = 0; s < 3; s++) {
                assertFalse(v.phan(s).get(0).getNode().isVisible());
            }
            v.stage().close();
            return null;
        });
    }

    @Test
    void beRongCotBangGanNuaKhoangCachCoGioiHan() throws Exception {
        FxTest.chay(() -> {
            // Hep: cot theo ty le. Rong: cot cham gioi han toi da.
            for (double rong : new double[] {460, 800, 1300}) {
                Ve v = ve(sauThangDaChot(), T11, rong);
                double khoang = ((CategoryAxis) v.bieuDo().getXAxis())
                        .getCategorySpacing();
                double cot = v.phan(0).get(0).getNode().getBoundsInParent()
                        .getWidth();
                double mong = Math.min(BieuDoThuPhi.COT_TOI_DA,
                        khoang * BieuDoThuPhi.TY_LE_COT);
                assertEquals(mong, cot, 1.5, "rộng " + rong);
                assertTrue(cot <= BieuDoThuPhi.COT_TOI_DA + 0.5);
                assertTrue(cot <= khoang * 0.5 + 0.5,
                        "cột không quá nửa khoảng cách, rộng " + rong);
                v.stage().close();
            }
            assertEquals(0.45, BieuDoThuPhi.TY_LE_COT);
            return null;
        });
    }

    @Test
    void baMauKhacNhauConNoKhongDungDo() throws Exception {
        FxTest.chay(() -> {
            Ve v = ve(sauThangDaChot(), T11);
            Color dung = nen(v.phan(0).get(0).getNode());
            Color tre = nen(v.phan(1).get(0).getNode());
            Color no = nen(v.phan(2).get(0).getNode());
            assertNotEquals(dung, tre);
            assertNotEquals(dung, no);
            assertNotEquals(tre, no);
            // Trung tinh nhat: ba kenh gan bang nhau va sang.
            assertTrue(no.getSaturation() < 0.15, "còn nợ: " + no);
            assertTrue(no.getBrightness() > 0.85, "còn nợ: " + no);
            v.stage().close();
            return null;
        });
    }

    @Test
    void toanChuaChotVanVeTrucVaChuKhongChong() throws Exception {
        FxTest.chay(() -> {
            List<CongNoThang> ds = new ArrayList<>();
            for (int i = 5; i >= 0; i--) {
                ds.add(CongNoThang.chuaChot(T11.minusMonths(i)));
            }
            // Hep nhu khi khung "Bien dong" hien ben canh o 1120 px.
            Ve v = ve(ds, T11, 460);
            NumberAxis y = (NumberAxis) v.bieuDo().getYAxis();
            assertEquals(0, y.getLowerBound());
            assertEquals(5, y.getUpperBound());
            assertEquals("Nghìn đồng", v.bieuDo().getTitle());
            List<Text> nhan = v.nhan();
            assertEquals(6, nhan.size());
            for (int i = 0; i < nhan.size(); i++) {
                assertEquals("Chưa chốt",
                        nhan.get(i).getText().replace('\n', ' '));
                if (i > 0) {
                    assertTrue(nhan.get(i - 1).getBoundsInParent().getMaxX()
                            <= nhan.get(i).getBoundsInParent().getMinX(),
                            "\"Chưa chốt\" " + i + " chồng nhãn trước");
                }
            }
            v.stage().close();
            return null;
        });
    }

    /** Lớp nền trên cùng của ô (ô "Còn nợ" có viền ở lớp dưới). */
    private static Color nen(Node n) {
        n.applyCss();
        var lop = ((Region) n).getBackground().getFills();
        return (Color) lop.get(lop.size() - 1).getFill();
    }
}
