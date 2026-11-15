package com.bluemoon.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bluemoon.dao.BienDongDaoImpl;
import com.bluemoon.dao.DongHoCsdl;
import com.bluemoon.dao.KhoanPhiDaoImpl;
import com.bluemoon.dao.NoDongDaoImpl;
import com.bluemoon.dao.NoDongDao.TomTatNo;
import com.bluemoon.dao.TaiKhoanDaoImpl;
import com.bluemoon.dao.ThongKeDaoImpl;
import com.bluemoon.model.BienDong;
import com.bluemoon.model.CongNoThang;
import com.bluemoon.model.LoaiBienDong;
import com.bluemoon.model.TaiKhoan;
import com.bluemoon.service.BienDongService;
import com.bluemoon.service.NoDongService;
import com.bluemoon.service.SoTien;
import com.bluemoon.service.ThongKeService;
import com.bluemoon.util.PhongChu;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * TS-004, US-016: Trang chính với số liệu bình thường (một tháng chưa
 * chốt, năm tháng có công nợ, vài biến động) ở 1440×1024 và 1120×645, chữ
 * 16 px. Tải trang-chinh.fxml không controller (không CSDL), điền số liệu
 * như {@link TrangChinhController} làm, rồi kiểm nhãn tháng T7…T12, tỷ lệ
 * trên đầu cột, đơn vị, chú giải không chồng nhau, không bị cắt, không
 * xoay; cột không quá nửa khoảng cách giữa hai tháng. Có
 * {@code -DchupAnh=<thư mục>} thì chụp PNG cho báo cáo
 * (docs/review/ts-004-giao-dien.md).
 *
 * <p>Có thêm {@code -DchupCsdl=1} (và {@code BLUEMOON_DB_*} trỏ tới một
 * CSDL đã nạp seed-demo) thì vẽ cả Trang chính bằng số liệu đọc từ CSDL đó
 * qua Service thật, kiểm cùng các điều trên và chụp ảnh cả màn lẫn riêng
 * khung biểu đồ. Chỉ đọc, không sửa CSDL.
 */
class TrangChinhSoLieuTest {

    private static final YearMonth T12 = YearMonth.of(2026, 12);

    @BeforeAll
    static void khoiDong() throws Exception {
        datNeuChuaCo("glass.platform", "Monocle");
        datNeuChuaCo("monocle.platform", "Headless");
        datNeuChuaCo("prism.order", "sw");
        CompletableFuture<Void> xong = new CompletableFuture<>();
        try {
            Platform.startup(() -> xong.complete(null));
        } catch (IllegalStateException daKhoiDong) {
            xong.complete(null);
        }
        xong.get(30, TimeUnit.SECONDS);
        Platform.setImplicitExit(false);
        chay(() -> {
            PhongChu.nap();
            return null;
        });
    }

    private static void datNeuChuaCo(String khoa, String giaTri) {
        if (System.getProperty(khoa) == null) {
            System.setProperty(khoa, giaTri);
        }
    }

    private static <T> T chay(Callable<T> viec) throws Exception {
        CompletableFuture<T> kq = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                kq.complete(viec.call());
            } catch (Throwable e) {
                kq.completeExceptionally(e);
            }
        });
        return kq.get(60, TimeUnit.SECONDS);
    }

    @ParameterizedTest
    @CsvSource({"1440, 1024", "1120, 645"})
    void soLieuBinhThuongKhongChongChu(int rong, int cao) throws Exception {
        veVaKiem(rong, cao, false);
    }

    @ParameterizedTest
    @CsvSource({"1440, 1024", "1120, 645"})
    @EnabledIfSystemProperty(named = "chupCsdl", matches = ".+")
    void soLieuTuCsdlKhongChongChu(int rong, int cao) throws Exception {
        veVaKiem(rong, cao, true);
    }

    private static void veVaKiem(int rong, int cao, boolean tuCsdl)
            throws Exception {
        List<String> loi = new ArrayList<>();
        List<WritableImage> anh = chay(() -> {
            FXMLLoader loader = new FXMLLoader();
            Parent goc;
            try (InputStream vao = new ByteArrayInputStream(fxml()
                    .getBytes(StandardCharsets.UTF_8))) {
                goc = loader.load(vao);
            }
            Map<String, Object> ten = loader.getNamespace();
            if (tuCsdl) {
                dienTuCsdl(ten);
            } else {
                dienSoLieu(ten);
            }
            Scene scene = new Scene(goc, rong, cao);
            scene.getStylesheets().add(TrangChinhSoLieuTest.class
                    .getResource("/com/bluemoon/css/giao-dien.css")
                    .toExternalForm());
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.show();
            for (int i = 0; i < 3; i++) {
                goc.applyCss();
                goc.layout();
            }
            BieuDoThuPhi bieuDo = (BieuDoThuPhi) ten.get("bieuDo");
            kiemBieuDo(bieuDo, loi, !tuCsdl);
            kiemCat(goc, loi);
            // Ca man hinh, va rieng khung bieu do (o 1120x645 khung nay
            // phai cuon moi thay het).
            List<WritableImage> a = List.of(scene.snapshot(null),
                    bieuDo.getParent().snapshot(null, null));
            stage.close();
            return a;
        });
        String thuMuc = System.getProperty("chupAnh");
        if (thuMuc != null) {
            Files.createDirectories(Path.of(thuMuc));
            String dau = System.getProperty("chupTienTo", "anh");
            String cuoi = (tuCsdl ? "-csdl-" : "-so-lieu-") + rong + "x"
                    + cao + ".png";
            ghiPng(anh.get(0), Path.of(thuMuc, dau + "-trang-chinh" + cuoi));
            if (tuCsdl) {
                ghiPng(anh.get(1), Path.of(thuMuc, dau + "-bieu-do" + cuoi));
            }
        }
        assertTrue(loi.isEmpty(), rong + "×" + cao + ":\n  "
                + String.join("\n  ", loi));
    }

    private static void kiemBieuDo(BieuDoThuPhi b, List<String> loi,
            boolean soLieuMau) {
        List<XYChart.Data<String, Number>> cot = new ArrayList<>();
        b.getData().forEach(s -> cot.addAll(s.getData()));
        cot.removeIf(d -> !d.getNode().isVisible());
        if (soLieuMau) {
            assertEquals(List.of("T7", "T8", "T9", "T10", "T11", "T12"),
                    b.getData().get(0).getData().stream()
                            .map(XYChart.Data::getXValue).toList());
        }
        double khoang = ((javafx.scene.chart.CategoryAxis) b.getXAxis())
                .getCategorySpacing();
        double rongCot = cot.get(0).getNode().getBoundsInParent()
                .getWidth();
        if (rongCot > khoang * 0.5 + 0.5
                || rongCot > BieuDoThuPhi.COT_TOI_DA + 0.5) {
            loi.add("cột rộng " + rongCot + " trên khoảng " + khoang);
        }
        List<Text> chu = new ArrayList<>();
        gomChu(b, chu);
        List<String> can = soLieuMau
                ? List.of("T7", "T8", "T9", "T10", "T11", "T12",
                        "Triệu đồng", "Tháng (năm 2026)", "Thu đúng hạn",
                        "Thu trễ hạn", "Còn nợ", "92%", "57%")
                : List.of("Triệu đồng", "Thu đúng hạn", "Thu trễ hạn",
                        "Còn nợ");
        if (soLieuMau && chu.stream().noneMatch(
                t -> t.getText().replace('\n', ' ')
                        .equals(BieuDoThuPhi.CHUA_CHOT))) {
            loi.add("thiếu chữ \"Chưa chốt\" trên biểu đồ");
        }
        for (Text t : chu) {
            for (Node n = t; n != null && n != b.getParent();
                    n = n.getParent()) {
                if (Math.abs(n.getRotate()) > 1e-6) {
                    loi.add("\"" + t.getText() + "\" bị xoay");
                }
            }
        }
        for (String c : can) {
            if (chu.stream().noneMatch(t -> t.getText().equals(c))) {
                loi.add("thiếu chữ \"" + c + "\" trên biểu đồ");
            }
        }
        Bounds khung = b.localToScene(b.getLayoutBounds());
        for (int i = 0; i < chu.size(); i++) {
            Text a = chu.get(i);
            Bounds ba = a.localToScene(a.getBoundsInLocal());
            if (ba.getMinX() < khung.getMinX() - 0.5
                    || ba.getMaxX() > khung.getMaxX() + 0.5
                    || ba.getMinY() < khung.getMinY() - 0.5
                    || ba.getMaxY() > khung.getMaxY() + 0.5) {
                loi.add("\"" + a.getText() + "\" ra ngoài biểu đồ");
            }
            if (a.getFont().getSize() < 14 - 0.01) {
                loi.add("\"" + a.getText() + "\" cỡ " + a.getFont().getSize());
            }
            for (int j = i + 1; j < chu.size(); j++) {
                Text c = chu.get(j);
                Bounds bc = c.localToScene(c.getBoundsInLocal());
                double ngang = Math.min(ba.getMaxX(), bc.getMaxX())
                        - Math.max(ba.getMinX(), bc.getMinX());
                double doc = Math.min(ba.getMaxY(), bc.getMaxY())
                        - Math.max(ba.getMinY(), bc.getMinY());
                if (ngang > 0.5 && doc > 0.5) {
                    loi.add("\"" + a.getText() + "\" chồng \"" + c.getText()
                            + "\"");
                }
            }
        }
        // So tren dau cot nam tren cot, khong de len cot ben canh.
        for (Text t : chu) {
            if (t.getStyleClass().contains(BieuDoThuPhi.NHAN_COT)) {
                for (XYChart.Data<String, Number> d : cot) {
                    Bounds bc = d.getNode().localToScene(
                            d.getNode().getBoundsInLocal());
                    Bounds bt = t.localToScene(t.getBoundsInLocal());
                    if (bt.intersects(bc)) {
                        loi.add("số \"" + t.getText() + "\" đè lên cột "
                                + d.getXValue());
                    }
                }
            }
        }
    }

    /** Khong nhan, tieu de cot hay o bang nao bi rut gon "...". */
    private static void kiemCat(Node n, List<String> loi) {
        if (!n.isVisible()) {
            return;
        }
        if (n instanceof javafx.scene.control.Labeled l && l.getText() != null
                && !l.getText().isEmpty()) {
            for (Node con : l.getChildrenUnmodifiable()) {
                if (con instanceof Text t && !t.getText().equals(l.getText())) {
                    loi.add("\"" + l.getText() + "\" hiện \"" + t.getText()
                            + "\"");
                }
            }
        }
        if (n instanceof Parent p) {
            for (Node con : p.getChildrenUnmodifiable()) {
                kiemCat(con, loi);
            }
        }
    }

    /** Moi chu dang hien trong bieu do (nhan truc, so tren cot, ten truc). */
    private static void gomChu(Node n, List<Text> kq) {
        if (!n.isVisible()) {
            return;
        }
        if (n instanceof Text t && t.getText() != null
                && !t.getText().isBlank()) {
            kq.add(t);
        }
        if (n instanceof Parent p) {
            for (Node con : p.getChildrenUnmodifiable()) {
                gomChu(con, kq);
            }
        }
    }

    /** Dien nhu TrangChinhController voi so lieu binh thuong thang 12/2026. */
    private static void dienSoLieu(Map<String, Object> ten) {
        ((Label) ten.get("tenNguoiDungLabel")).setText("Nguyễn Thị Thu Hương");
        ((Label) ten.get("vaiTroLabel")).setText("Quản trị viên");
        ((Label) ten.get("tieuDeLabel")).setText("Tổng quan tháng 12/2026");
        ((Label) ten.get("daThuLabel")).setText("186.450.000 đ");
        ((Label) ten.get("tyLeLabel")).setText("72% tổng phải thu");
        ((Label) ten.get("soHoNoLabel")).setText("14");
        ((Label) ten.get("noDongLabel")).setText("3 hộ nợ từ 3 tháng trở lên");
        List<CongNoThang> sau = congNoMau();
        ((Label) ten.get("chuThichLabel")).setText(
                BieuDoThuPhi.chuThich(sau, T12));
        Node nhac = (Node) ten.get("nhacChotBox");
        nhac.setVisible(false);
        nhac.setManaged(false);
        ((BieuDoThuPhi) ten.get("bieuDo")).hien(sau, T12);
        LocalDateTime luc = LocalDateTime.of(2026, 12, 3, 9, 15);
        List<BienDong> bd = List.of(
                new BienDong(1, 1, "A0601", 1, "Nguyễn Văn Đức",
                        LoaiBienDong.TAM_VANG, LocalDate.of(2026, 12, 2),
                        "Công tác Đà Nẵng", "Trần Thị Bình", luc),
                new BienDong(2, 2, "B1203", 2, "Lê Thị Mai",
                        LoaiBienDong.CHUYEN_DEN, LocalDate.of(2026, 11, 28),
                        null, "Trần Thị Bình", luc.minusDays(5)),
                new BienDong(3, 3, "C0805", 3, "Phạm Quốc Huy",
                        LoaiBienDong.TAM_TRU, LocalDate.of(2026, 11, 25),
                        "Sinh viên thuê trọ", "Nguyễn Thị Thu Hương",
                        luc.minusDays(8)));
        TableView<BienDong> bang = BangLichSu.taoBangGon(bd);
        bang.setPrefHeight(200);
        VBox box = (VBox) ten.get("bienDongBox");
        box.getChildren().add(bang);
        box.setVisible(true);
        box.setManaged(true);
    }

    /**
     * Điền như TrangChinhController, số liệu đọc qua Service thật từ CSDL
     * đang cấu hình (seed-demo), bằng tài khoản admin của CSDL đó.
     */
    private static void dienTuCsdl(Map<String, Object> ten) {
        TaiKhoan tk = new TaiKhoanDaoImpl().timTheoTenDangNhap("admin")
                .orElseThrow();
        ((Label) ten.get("tenNguoiDungLabel")).setText(tk.getHoTen());
        ((Label) ten.get("vaiTroLabel")).setText("Quản trị viên");
        ThongKeService.TongQuan tq = new ThongKeService(
                new ThongKeDaoImpl(), new KhoanPhiDaoImpl(),
                new DongHoCsdl()).tongQuan(tk);
        YearMonth t = tq.thang();
        ((Label) ten.get("tieuDeLabel")).setText("Tổng quan tháng "
                + t.getMonthValue() + "/" + t.getYear());
        ((Label) ten.get("daThuLabel")).setText(SoTien.hienThi(tq.daThu()));
        ((Label) ten.get("tyLeLabel")).setText(
                TrangChinhController.dongTyLe(tq));
        ((BieuDoThuPhi) ten.get("bieuDo")).hien(tq.congNo(), t);
        ((Label) ten.get("chuThichLabel")).setText(
                BieuDoThuPhi.chuThich(tq.congNo(), t));
        for (CongNoThang c : tq.congNo()) {
            System.out.println("CONG_NO " + c.thang() + " phaiNop="
                    + c.phaiNop() + " dungHan=" + c.dungHan() + " treHan="
                    + c.treHan() + " conNo=" + c.conNo() + " tyLe="
                    + c.phanTram() + " ho=" + c.soHoDongDu() + "/"
                    + c.tongSoHo());
        }
        TomTatNo no = new NoDongService(new NoDongDaoImpl()).tomTat(tk);
        ((Label) ten.get("soHoNoLabel")).setText(
                String.valueOf(no.soHoConNo()));
        ((Label) ten.get("noDongLabel")).setText(
                TrangChinhController.dongNoDong(no));
        Node nhac = (Node) ten.get("nhacChotBox");
        nhac.setVisible(false);
        nhac.setManaged(false);
        TableView<BienDong> bang = BangLichSu.taoBangGon(
                new BienDongService(new BienDongDaoImpl()).ganDay(tk));
        bang.setPrefHeight(200);
        VBox box = (VBox) ten.get("bienDongBox");
        box.getChildren().add(bang);
        box.setVisible(true);
        box.setManaged(true);
    }

    /** Tháng 7 chưa chốt; tháng 8 đến 12 có đủ ba phần. */
    static List<CongNoThang> congNoMau() {
        long[][] dong = {
            {201_200_000, 172_400_000, 13_100_000},
            {214_000_000, 180_900_000, 16_200_000},
            {209_300_000, 171_000_000, 21_500_000},
            {212_600_000, 176_300_000, 9_800_000},
            {212_600_000, 121_900_000, 0}};
        List<CongNoThang> sau = new ArrayList<>();
        sau.add(CongNoThang.chuaChot(T12.minusMonths(5)));
        for (int i = 0; i < dong.length; i++) {
            sau.add(new CongNoThang(T12.minusMonths(4L - i), true,
                    BigDecimal.valueOf(dong[i][0]),
                    BigDecimal.valueOf(dong[i][1]),
                    BigDecimal.valueOf(dong[i][2]), 300 + i, 356));
        }
        return sau;
    }

    private static String fxml() throws Exception {
        try (InputStream vao = TrangChinhSoLieuTest.class.getResourceAsStream(
                "/com/bluemoon/view/trang-chinh.fxml")) {
            return new String(vao.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("\\s+fx:controller=\"[^\"]*\"", "")
                    .replaceAll("\\s+on[A-Z]\\w*=\"#[^\"]*\"", "");
        }
    }

    private static void ghiPng(WritableImage anh, Path tep) throws Exception {
        int w = (int) anh.getWidth();
        int h = (int) anh.getHeight();
        BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        PixelReader r = anh.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                b.setRGB(x, y, r.getArgb(x, y));
            }
        }
        ImageIO.write(b, "png", tep.toFile());
    }
}
