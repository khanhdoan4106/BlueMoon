package com.bluemoon.controller;

import com.bluemoon.model.CongNoThang;
import com.bluemoon.service.SoTien;
import com.bluemoon.service.ThangTrucY;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import javafx.collections.FXCollections;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.StringConverter;

/**
 * Biểu đồ cột chồng "Thu phí 6 tháng gần nhất" ở Trang chính (US-016, giả
 * định 46), theo mục "Quy tắc biểu đồ" trong docs/design/ui.md. Mỗi cột là
 * tổng phải nộp các khoản bắt buộc của một tháng đã chốt, chia từ dưới lên:
 * thu đúng hạn, thu trễ hạn, còn nợ; trên đầu cột ghi tỷ lệ đã thu. Tháng
 * chưa chốt không có cột, ghi "Chưa chốt".
 *
 * <p>Trục X có tên và nhãn từng tháng. Trục Y bắt đầu từ 0, chia theo mốc
 * tròn ({@link ThangTrucY}), lưới ngang mờ; đơn vị của trục Y nằm ngang ở
 * đầu trục (tiêu đề của biểu đồ, canh trái), không xoay chữ vì JavaFX vẽ
 * sai dấu tiếng Việt khi xoay. Ba chuỗi nên có chú giải ở dưới. Bề rộng cột
 * bằng {@link #TY_LE_COT} khoảng cách giữa hai mốc tháng, tối đa
 * {@link #COT_TOI_DA} px.
 */
public class BieuDoThuPhi extends StackedBarChart<String, Number> {

    /** Tên chuỗi và mục chú giải: phần thu không sau hạn nộp. */
    public static final String DUNG_HAN = "Thu đúng hạn";
    /** Tên chuỗi và mục chú giải: phần thu sau hạn nộp. */
    public static final String TRE_HAN = "Thu trễ hạn";
    /** Tên chuỗi và mục chú giải: phần chưa thu. */
    public static final String CON_NO = "Còn nợ";
    /** Chữ ở vị trí của tháng chưa chốt công nợ. */
    public static final String CHUA_CHOT = "Chưa chốt";
    /** Chữ trên cột của tháng đã chốt mà không có khoản phải nộp. */
    public static final String KHONG_CO = "–";
    /** Lớp CSS của nhãn trên đầu cột (tỷ lệ đã thu, "Chưa chốt"). */
    public static final String NHAN_COT = "nhan-cot";
    /** Bề rộng cột so với khoảng cách giữa hai mốc tháng. */
    public static final double TY_LE_COT = 0.45;
    /** Bề rộng cột tối đa, px. */
    public static final double COT_TOI_DA = 72;

    private static final double CACH_DAU_COT = 4;
    /** Lề mỗi bên của nhãn so với khoảng dành cho một tháng. */
    private static final double LE_NHAN = 3;

    private final List<Text> nhan = new ArrayList<>();
    private List<CongNoThang> duLieu = List.of();
    private List<String> mocThang = List.of();
    private ThangTrucY thang = ThangTrucY.tinh(List.of());

    /** Biểu đồ rỗng (FXML tạo), có sẵn tên trục và thang mặc định. */
    public BieuDoThuPhi() {
        super(new CategoryAxis(), new NumberAxis());
        getStyleClass().add("bieu-do-thu");
        setLegendVisible(true);
        setLegendSide(Side.BOTTOM);
        setAnimated(false);
        setVerticalGridLinesVisible(false);
        setHorizontalGridLinesVisible(true);
        setAlternativeRowFillVisible(false);
        NumberAxis y = (NumberAxis) getYAxis();
        y.setAutoRanging(false);
        y.setMinorTickVisible(false);
        y.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number n) {
                return dinhDang(n.doubleValue());
            }

            @Override
            public Number fromString(String s) {
                throw new UnsupportedOperationException();
            }
        });
        datThang(thang);
        getXAxis().setLabel("Tháng");
    }

    /**
     * Vẽ công nợ của các tháng.
     *
     * @param congNo công nợ từng tháng, cũ trước, tháng hiện tại cuối
     * @param hienTai tháng hiện tại (đang thu)
     */
    public void hien(List<CongNoThang> congNo, YearMonth hienTai) {
        duLieu = List.copyOf(congNo);
        thang = ThangTrucY.tinh(duLieu.stream().map(CongNoThang::phaiNop)
                .toList());
        datThang(thang);
        boolean haiNam = duLieu.stream().map(t -> t.thang().getYear())
                .distinct().count() > 1;
        getXAxis().setLabel(haiNam || duLieu.isEmpty() ? "Tháng"
                : "Tháng (năm " + duLieu.get(0).thang().getYear() + ")");
        mocThang = duLieu.stream()
                .map(t -> nhanThang(t.thang(), haiNam)).toList();

        getPlotChildren().removeAll(nhan);
        nhan.clear();
        getData().clear();
        ((CategoryAxis) getXAxis()).setCategories(
                FXCollections.observableArrayList(mocThang));
        getData().add(chuoi(DUNG_HAN, CongNoThang::dungHan, hienTai));
        getData().add(chuoi(TRE_HAN, CongNoThang::treHan, hienTai));
        getData().add(chuoi(CON_NO, CongNoThang::conNo, hienTai));
        for (CongNoThang c : duLieu) {
            Text t = new Text(nhanCot(c));
            t.getStyleClass().add(NHAN_COT);
            t.setTextAlignment(TextAlignment.CENTER);
            if (!c.daChot()) {
                Tooltip.install(t, new Tooltip(moTa(c, false)));
            }
            nhan.add(t);
        }
        getPlotChildren().addAll(nhan);
        requestChartLayout();
    }

    /** Một phần của cột chồng; phần 0 đ thì ẩn để không còn viền. */
    private XYChart.Series<String, Number> chuoi(String ten,
            Function<CongNoThang, BigDecimal> phan, YearMonth hienTai) {
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName(ten);
        for (int i = 0; i < duLieu.size(); i++) {
            s.getData().add(new XYChart.Data<>(mocThang.get(i),
                    thang.doi(phan.apply(duLieu.get(i)))));
        }
        // Nut cua tung o chi co sau khi chuoi vao bieu do.
        s.getData().forEach(d -> d.nodeProperty().addListener(
                (o, cu, moi) -> ganNut(moi, d, hienTai)));
        return s;
    }

    private void ganNut(Node nut, XYChart.Data<String, Number> d,
            YearMonth hienTai) {
        if (nut == null) {
            return;
        }
        CongNoThang c = duLieu.get(mocThang.indexOf(d.getXValue()));
        nut.setVisible(d.getYValue().doubleValue() > 0);
        Tooltip.install(nut, new Tooltip(moTa(c,
                c.thang().equals(hienTai))));
    }

    /**
     * Chữ trên đầu cột: tỷ lệ đã thu, "Chưa chốt" hoặc "–".
     *
     * @param c công nợ của tháng
     * @return chữ hiển thị
     */
    static String nhanCot(CongNoThang c) {
        if (!c.daChot()) {
            return CHUA_CHOT;
        }
        Integer phanTram = c.phanTram();
        return phanTram == null ? KHONG_CO : phanTram + "%";
    }

    /**
     * Nội dung tooltip của một cột: phải nộp, ba phần, tỷ lệ, số hộ đã
     * đóng đủ.
     *
     * @param c công nợ của tháng
     * @param dangThu tháng này chưa kết thúc
     * @return các dòng chữ
     */
    static String moTa(CongNoThang c, boolean dangThu) {
        String dau = "Tháng " + c.thang().getMonthValue() + "/"
                + c.thang().getYear();
        if (!c.daChot()) {
            return dau + " chưa chốt công nợ";
        }
        Integer phanTram = c.phanTram();
        return dau + (dangThu ? " (đang thu, tính đến hôm nay)" : "")
                + "\nPhải nộp: " + SoTien.hienThi(c.phaiNop())
                + "\n" + DUNG_HAN + ": " + SoTien.hienThi(c.dungHan())
                + "\n" + TRE_HAN + ": " + SoTien.hienThi(c.treHan())
                + "\n" + CON_NO + ": " + SoTien.hienThi(c.conNo())
                + "\nĐã thu: " + (phanTram == null ? KHONG_CO
                        : phanTram + "%")
                + "\nĐã đóng đủ: " + c.soHoDongDu() + "/" + c.tongSoHo()
                + " hộ";
    }

    /**
     * Chú thích dưới biểu đồ: cách tính (theo công nợ từng tháng, khác thẻ
     * "Đã thu tháng này" tính theo ngày thu) và tình trạng tháng hiện tại.
     *
     * @param congNo công nợ các tháng đang vẽ
     * @param hienTai tháng hiện tại
     * @return câu chú thích
     */
    static String chuThich(List<CongNoThang> congNo, YearMonth hienTai) {
        String thangNay = "";
        for (CongNoThang c : congNo) {
            if (c.thang().equals(hienTai)) {
                thangNay = " Tháng " + hienTai.getMonthValue() + (c.daChot()
                        ? " đang thu, tính đến hôm nay."
                        : " chưa chốt công nợ.");
            }
        }
        return "Mỗi cột là công nợ bắt buộc của một tháng; hạn nộp là ngày"
                + " cuối tháng đó." + thangNay + " Thẻ \"Đã thu tháng này\""
                + " tính theo ngày thu nên có thể khác.";
    }

    /**
     * Thang trục Y đang dùng.
     *
     * @return thang trục Y
     */
    public ThangTrucY getThang() {
        return thang;
    }

    /** Đơn vị của trục Y là tiêu đề biểu đồ, nằm ngang ở đầu trục. */
    private void datThang(ThangTrucY t) {
        NumberAxis y = (NumberAxis) getYAxis();
        y.setLowerBound(0);
        y.setUpperBound(t.tren().doubleValue());
        y.setTickUnit(t.buoc().doubleValue());
        y.setLabel(null);
        setTitle(t.donVi());
    }

    /** Nhãn tháng: "T11"; biểu đồ trải hai năm thì "T12/25". */
    static String nhanThang(YearMonth t, boolean haiNam) {
        return "T" + t.getMonthValue()
                + (haiNam ? String.format("/%02d", t.getYear() % 100) : "");
    }

    /**
     * Số theo kiểu Việt Nam: từ 100 trở lên làm tròn số nguyên (186), nhỏ
     * hơn thì giữ một chữ số lẻ (2,5).
     */
    static String dinhDang(double so) {
        DecimalFormatSymbols vn = DecimalFormatSymbols.getInstance(
                Locale.forLanguageTag("vi-VN"));
        DecimalFormat f = new DecimalFormat(
                Math.abs(so) >= 100 ? "#,##0" : "#,##0.#", vn);
        return f.format(BigDecimal.valueOf(so));
    }

    /**
     * Khoảng trống giữa hai cột để cột rộng {@link #TY_LE_COT} khoảng cách
     * giữa hai mốc, không quá {@link #COT_TOI_DA}.
     *
     * @param khoangCach khoảng cách giữa hai mốc tháng, px
     * @return khoảng trống, px
     */
    static double khoangTrong(double khoangCach) {
        return khoangCach - Math.min(COT_TOI_DA, khoangCach * TY_LE_COT);
    }

    @Override
    protected void layoutPlotChildren() {
        CategoryAxis x = (CategoryAxis) getXAxis();
        double khoangCach = x.getCategorySpacing();
        if (khoangCach > 0) {
            double trong = khoangTrong(khoangCach);
            if (Math.abs(trong - getCategoryGap()) > 0.5) {
                setCategoryGap(trong);
            }
        }
        super.layoutPlotChildren();
        NumberAxis y = (NumberAxis) getYAxis();
        for (int i = 0; i < nhan.size(); i++) {
            Text t = nhan.get(i);
            CongNoThang c = duLieu.get(i);
            // "Chua chot" dai hon khoang cua mot thang thi xuong hai dong.
            if (!c.daChot()) {
                t.setText(CHUA_CHOT);
                if (khoangCach > 0 && t.getLayoutBounds().getWidth()
                        > khoangCach - 2 * LE_NHAN) {
                    t.setText(CHUA_CHOT.replace(' ', '\n'));
                }
            }
            double giua = x.getDisplayPosition(mocThang.get(i));
            double dinh = y.getDisplayPosition(thang.doi(c.phaiNop()));
            t.setLayoutX(giua - t.getLayoutBounds().getWidth() / 2
                    - t.getLayoutBounds().getMinX());
            t.setLayoutY(dinh - CACH_DAU_COT
                    - t.getLayoutBounds().getMaxY());
        }
    }
}
