package com.bluemoon.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bluemoon.config.Database;
import com.bluemoon.dao.CongNoDaoImpl;
import com.bluemoon.dao.HoGiaDinhDaoImpl;
import com.bluemoon.dao.KhoanPhiDaoImpl;
import com.bluemoon.dao.PhieuThuDaoImpl;
import com.bluemoon.dao.ThongKeDaoImpl;
import com.bluemoon.model.CachTinhPhi;
import com.bluemoon.model.CongNoThang;
import com.bluemoon.model.GioiTinh;
import com.bluemoon.model.HinhThucThanhToan;
import com.bluemoon.model.HoGiaDinh;
import com.bluemoon.model.KhoanPhi;
import com.bluemoon.model.LoaiCanHo;
import com.bluemoon.model.LoaiCuTru;
import com.bluemoon.model.LoaiKhoanPhi;
import com.bluemoon.model.NhanKhau;
import com.bluemoon.model.PhaiNop;
import com.bluemoon.model.PhieuThuMoi;
import com.bluemoon.model.QuanHeChuHo;
import com.bluemoon.model.TaiKhoan;
import com.bluemoon.model.ThongKeKhoan;
import com.bluemoon.model.TrangThaiHo;
import com.bluemoon.model.TrangThaiKhoanPhi;
import com.bluemoon.model.TrangThaiNhanKhau;
import com.bluemoon.service.CongNoService;
import com.bluemoon.service.ThongKeService;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Số liệu thu phí trên MySQL thật (US-016, US-019, AC-046, AC-047, AC-052,
 * AC-053): phiếu đã hủy không tính, ranh giới tháng, công nợ 6 tháng chia
 * đúng hạn, trễ hạn, còn nợ (giả định 46), khoản bắt buộc theo tháng chốt,
 * khoản tự nguyện đếm hộ.
 */
class ThongKeIT {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final YearMonth T10 = YearMonth.of(2026, 10);
    private static final YearMonth T11 = YearMonth.of(2026, 11);

    private final KhoanPhiDaoImpl khoanPhiDao = new KhoanPhiDaoImpl();
    private final HoGiaDinhDaoImpl hoDao = new HoGiaDinhDaoImpl();
    private final PhieuThuDaoImpl phieuDao = new PhieuThuDaoImpl();
    private final CongNoDaoImpl congNoDao = new CongNoDaoImpl();
    private TaiKhoan admin;
    private CongNoService congNo;
    private ThongKeService thongKe;
    private KhoanPhi dichVu;
    private KhoanPhi quanLy;
    private KhoanPhi quy;
    private KhoanPhi quyTrong;
    private int hoA;
    private int hoB;
    private int hoC;

    @BeforeEach
    void chuanBi() {
        admin = CsdlThu.dungLai();
        Clock ngay12T11 = Clock.fixed(LocalDateTime.of(2026, 11, 12, 9, 0)
                .atZone(VN).toInstant(), VN);
        congNo = new CongNoService(congNoDao, khoanPhiDao, hoDao, ngay12T11);
        thongKe = new ThongKeService(new ThongKeDaoImpl(), khoanPhiDao,
                ngay12T11);
        dichVu = batBuoc("Phí dịch vụ", "5000");
        quanLy = batBuoc("Phí quản lý", "7000");
        quy = tuNguyen("Quỹ vì người nghèo 2026");
        quyTrong = tuNguyen("Quỹ khuyến học 2026");
        hoA = ho("A0601", "75.50", "001182004561");
        hoB = ho("A0602", "60.00", "001182004562");
        hoC = ho("A0603", "50.00", "001182004563");
        congNo.chot(admin, T10);
        congNo.chot(admin, T11);
    }

    private KhoanPhi batBuoc(String ten, String donGia) {
        KhoanPhi k = new KhoanPhi(0, ten, LoaiKhoanPhi.BAT_BUOC,
                CachTinhPhi.THEO_DIEN_TICH, new BigDecimal(donGia),
                LocalDate.of(2026, 1, 1), null, TrangThaiKhoanPhi.DANG_THU);
        return k.voiId(khoanPhiDao.them(k, admin.getId()));
    }

    private KhoanPhi tuNguyen(String ten) {
        KhoanPhi k = new KhoanPhi(0, ten, LoaiKhoanPhi.TU_NGUYEN,
                CachTinhPhi.KHONG_CO_DINH, null, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 12, 31), TrangThaiKhoanPhi.DANG_THU);
        return k.voiId(khoanPhiDao.them(k, admin.getId()));
    }

    private int ho(String ma, String dienTich, String cccd) {
        return hoDao.them(new HoGiaDinh(0, ma, LoaiCanHo.CAN_HO, 6,
                new BigDecimal(dienTich), null, LocalDate.of(2026, 1, 1),
                TrangThaiHo.DANG_O),
                new NhanKhau(0, 0, "Chủ hộ " + ma, LocalDate.of(1980, 1, 1),
                        GioiTinh.NU, cccd, QuanHeChuHo.CHU_HO,
                        TrangThaiNhanKhau.DANG_O, LoaiCuTru.THUONG_TRU, null),
                null, admin.getId());
    }

    private PhaiNop phaiNop(YearMonth thang, int hoId, KhoanPhi k) {
        return congNo.theoThang(admin, thang).stream()
                .filter(p -> p.hoId() == hoId && p.khoanPhiId() == k.id())
                .findFirst().orElseThrow();
    }

    /** Lập phiếu đóng một khoản phải nộp, thời điểm chỉ định. */
    private int thu(PhaiNop p, LocalDateTime luc) {
        return phieuDao.lapPhieu(new PhieuThuMoi(p.hoId(),
                HinhThucThanhToan.TIEN_MAT, admin.getId(), luc,
                List.of(new PhieuThuMoi.Dong(p.khoanPhiId(), p.id(),
                        p.soTien())))).id();
    }

    /** Lập phiếu đóng góp tự nguyện, thời điểm chỉ định. */
    private int dongGop(int hoId, KhoanPhi k, String soTien,
            LocalDateTime luc) {
        BigDecimal tien = new BigDecimal(soTien);
        return phieuDao.lapPhieu(new PhieuThuMoi(hoId,
                tien.signum() == 0 ? null : HinhThucThanhToan.TIEN_MAT,
                admin.getId(), luc,
                List.of(new PhieuThuMoi.Dong(k.id(), null, tien)))).id();
    }

    private void huy(int phieuId) {
        phieuDao.huy(phieuId, admin.getId(),
                LocalDateTime.of(2026, 11, 12, 10, 0), "Thu nhầm");
    }

    private static void bang(String mong, BigDecimal thuc) {
        assertEquals(0, new BigDecimal(mong).compareTo(thuc),
                "mong " + mong + ", thực " + thuc);
    }

    /** AC-046, AC-047: phiếu đã hủy không tính; tháng trống là 0 đ. */
    @Test
    void phieuDaHuyKhongTinh() {
        bang("0", thongKe.tongThuThang(admin, T11));
        dongGop(hoA, quy, "100000", LocalDateTime.of(2026, 11, 5, 9, 0));
        int bo = dongGop(hoB, quy, "50000",
                LocalDateTime.of(2026, 11, 6, 9, 0));
        int boDv = thu(phaiNop(T11, hoA, dichVu),
                LocalDateTime.of(2026, 11, 7, 9, 0));
        huy(bo);
        huy(boDv);
        bang("100000", thongKe.tongThuThang(admin, T11));
        bang("100000", thongKe.tongQuan(admin).daThu());
        ThongKeKhoan q = thongKe.theoKhoan(admin, quy, null);
        bang("100000", q.tongThu());
        assertEquals(1, q.soHoDaDong());
        ThongKeKhoan dv = thongKe.theoKhoan(admin, dichVu, T11);
        bang("0", dv.tongThu());
        assertEquals(0, dv.soHoDaDong());
    }

    /** Giả định 24: tháng tính theo thời điểm lập phiếu, đúng ranh giới. */
    @Test
    void ranhGioiThang() {
        dongGop(hoA, quy, "1000", LocalDateTime.of(2026, 10, 31, 23, 59,
                59));
        dongGop(hoB, quy, "2000", LocalDateTime.of(2026, 11, 1, 0, 0, 0));
        dongGop(hoC, quy, "4000", LocalDateTime.of(2026, 11, 30, 23, 59,
                59));
        dongGop(hoC, quy, "8000", LocalDateTime.of(2026, 12, 1, 0, 0, 0));
        bang("1000", thongKe.tongThuThang(admin, T10));
        bang("6000", thongKe.tongThuThang(admin, T11));
        bang("8000", thongKe.tongThuThang(admin, YearMonth.of(2026, 12)));
    }

    /** Tiền nợ tháng trước đóng trong tháng này tính vào tháng này. */
    @Test
    void noThangTruocDongThangNayTinhThangNay() {
        PhaiNop dv10 = phaiNop(T10, hoA, dichVu);
        thu(dv10, LocalDateTime.of(2026, 11, 3, 8, 0));
        bang("0", thongKe.tongThuThang(admin, T10));
        bang(dv10.soTien().toPlainString(),
                thongKe.tongThuThang(admin, T11));
    }

    /**
     * Giả định 46. Mỗi tháng phải nộp 2.226.000 đ (DV 927.500 + QL
     * 1.298.500 của ba hộ). Tháng 10: A đóng DV lúc 23:59:59 ngày 31/10
     * (đúng hạn) và QL lúc 0 giờ ngày 1/11 (trễ hạn); B đóng DV trong
     * tháng rồi bị hủy phiếu, đóng bù ngày 5/11 (trễ hạn); C chưa đóng.
     */
    @Test
    void congNoThangChiaDungHanTreHanConNo() {
        thu(phaiNop(T10, hoA, dichVu),
                LocalDateTime.of(2026, 10, 31, 23, 59, 59));
        thu(phaiNop(T10, hoA, quanLy), LocalDateTime.of(2026, 11, 1, 0, 0));
        huy(thu(phaiNop(T10, hoB, dichVu),
                LocalDateTime.of(2026, 10, 10, 9, 0)));
        thu(phaiNop(T10, hoB, dichVu), LocalDateTime.of(2026, 11, 5, 9, 0));
        // Dong gop tu nguyen khong phai cong no.
        dongGop(hoC, quy, "100000", LocalDateTime.of(2026, 10, 12, 9, 0));

        List<CongNoThang> ds = thongKe.tongQuan(admin).congNo();

        assertEquals(6, ds.size());
        assertEquals(YearMonth.of(2026, 6), ds.get(0).thang());
        // Thang 6 den thang 9 chua chot.
        for (int i = 0; i < 4; i++) {
            assertFalse(ds.get(i).daChot(), ds.get(i).toString());
            assertNull(ds.get(i).phanTram());
        }
        CongNoThang t10 = ds.get(4);
        assertEquals(T10, t10.thang());
        assertTrue(t10.daChot());
        bang("2226000", t10.phaiNop());
        bang("377500", t10.dungHan());
        bang("828500", t10.treHan());
        bang("1020000", t10.conNo());
        assertEquals(54, t10.phanTram());
        assertEquals(1, t10.soHoDongDu());
        assertEquals(3, t10.tongSoHo());

        CongNoThang t11 = ds.get(5);
        assertEquals(T11, t11.thang());
        bang("2226000", t11.phaiNop());
        bang("0", t11.daThu());
        bang("2226000", t11.conNo());
        assertEquals(0, t11.phanTram());
        assertEquals(0, t11.soHoDongDu());
        assertEquals(3, t11.tongSoHo());
    }

    /**
     * Khoản sửa về 0 đ không phải công nợ (giả định 33): không vào phải
     * nộp, và hộ chỉ còn khoản đó thì đã đóng đủ. Tổng ba phần luôn bằng
     * phải nộp.
     */
    @Test
    void congNoThangBoKhoanKhongDongVaDemHoDongDu() {
        congNoDao.suaSoTien(phaiNop(T11, hoC, dichVu).id(), BigDecimal.ZERO,
                admin.getId(), "Miễn phí");
        thu(phaiNop(T11, hoA, dichVu), LocalDateTime.of(2026, 11, 5, 9, 0));
        thu(phaiNop(T11, hoA, quanLy), LocalDateTime.of(2026, 11, 5, 9, 1));
        thu(phaiNop(T11, hoC, quanLy), LocalDateTime.of(2026, 11, 6, 9, 0));
        thu(phaiNop(T11, hoB, dichVu), LocalDateTime.of(2026, 11, 7, 9, 0));

        List<CongNoThang> ds = thongKe.tongQuan(admin).congNo();

        CongNoThang t11 = ds.get(5);
        // 2.226.000 - 250.000 (DV cua C da sua ve 0).
        bang("1976000", t11.phaiNop());
        bang("1556000", t11.dungHan());
        bang("0", t11.treHan());
        bang("420000", t11.conNo());
        assertEquals(78, t11.phanTram());
        assertEquals(2, t11.soHoDongDu(), "A và C; B còn phí quản lý");
        assertEquals(3, t11.tongSoHo());
        for (CongNoThang c : ds) {
            bang(c.phaiNop().toPlainString(),
                    c.dungHan().add(c.treHan()).add(c.conNo()));
            assertTrue(c.conNo().signum() >= 0, c.toString());
        }
    }

    /** Tháng đã chốt mà không có khoản phải nộp: có cột 0, không tỷ lệ. */
    @Test
    void congNoThangDaChotKhongCoKhoanPhaiNop() throws SQLException {
        try (Connection c = Database.getConnection();
                Statement st = c.createStatement()) {
            st.executeUpdate("INSERT INTO dot_thu (thang, nguoi_chot) "
                    + "VALUES ('2026-09-01', " + admin.getId() + ")");
        }

        CongNoThang t9 = thongKe.tongQuan(admin).congNo().get(3);

        assertEquals(YearMonth.of(2026, 9), t9.thang());
        assertTrue(t9.daChot());
        bang("0", t9.phaiNop());
        bang("0", t9.conNo());
        assertNull(t9.phanTram());
        assertEquals(0, t9.tongSoHo());
        assertEquals(0, t9.soHoDongDu());
    }

    /**
     * AC-052: khoản bắt buộc theo tháng chốt; khoản sửa về 0 đ không phải
     * đóng; tiền tháng khác không lẫn. Tỷ lệ thu tháng hiện tại.
     */
    @Test
    void khoanBatBuocTheoThangChot() {
        PhaiNop dvA11 = phaiNop(T11, hoA, dichVu);
        thu(dvA11, LocalDateTime.of(2026, 11, 5, 9, 0));
        thu(phaiNop(T10, hoA, dichVu), LocalDateTime.of(2026, 11, 5, 9, 1));
        congNoDao.suaSoTien(phaiNop(T11, hoC, dichVu).id(), BigDecimal.ZERO,
                admin.getId(), "Miễn phí");

        ThongKeKhoan t11 = thongKe.theoKhoan(admin, dichVu, T11);
        bang("377500", t11.tongThu());
        // A 75,5 x 5.000 + B 60 x 5.000; C da sua ve 0 d.
        bang("677500", t11.tongPhaiThu());
        assertEquals(2, t11.soHoPhaiDong());
        assertEquals(1, t11.soHoDaDong());

        ThongKeKhoan t10 = thongKe.theoKhoan(admin, dichVu, T10);
        bang("377500", t10.tongThu());
        bang("927500", t10.tongPhaiThu());
        assertEquals(3, t10.soHoPhaiDong());
        assertEquals(1, t10.soHoDaDong());

        // Ty le T11: 377.500 / (DV A, B + QL A, B, C) = 19,1% -> 19%.
        ThongKeService.TongQuan tq = thongKe.tongQuan(admin);
        assertEquals(true, tq.daChot());
        assertEquals(19, tq.phanTram());
        assertEquals(List.of(T11, T10), thongKe.thangDaChot(admin));
    }

    /** AC-053: khoản, đợt chưa có giao dịch thì tổng là 0 đ. */
    @Test
    void chuaCoGiaoDichLaKhong() {
        ThongKeKhoan q = thongKe.theoKhoan(admin, quyTrong, null);
        bang("0", q.tongThu());
        assertEquals(0, q.soHoDaDong());
        assertEquals(0, q.soHoKhongDong());
        ThongKeKhoan ql = thongKe.theoKhoan(admin, quanLy, T10);
        bang("0", ql.tongThu());
        assertEquals(3, ql.soHoPhaiDong());
        assertEquals(0, ql.soHoDaDong());
    }

    /**
     * Giả định 29: hộ đóng nhiều lần cộng dồn; chỉ có lần 0 đ là "không
     * đóng"; 0 đ rồi đóng thêm là "đã đóng"; phiếu hủy không tính.
     */
    @Test
    void tuNguyenDemDungHoDongVaHoGhiKhong() {
        dongGop(hoA, quy, "100000", LocalDateTime.of(2026, 10, 2, 9, 0));
        dongGop(hoA, quy, "50000", LocalDateTime.of(2026, 11, 2, 9, 0));
        huy(dongGop(hoA, quy, "999000",
                LocalDateTime.of(2026, 11, 3, 9, 0)));
        dongGop(hoB, quy, "0", LocalDateTime.of(2026, 10, 3, 9, 0));
        dongGop(hoC, quy, "0", LocalDateTime.of(2026, 10, 4, 9, 0));
        dongGop(hoC, quy, "30000", LocalDateTime.of(2026, 11, 4, 9, 0));
        // Dong gop quy khac khong lan vao.
        dongGop(hoB, quyTrong, "70000", LocalDateTime.of(2026, 11, 4, 9, 0));

        ThongKeKhoan q = thongKe.theoKhoan(admin, quy, null);
        bang("180000", q.tongThu());
        assertEquals(2, q.soHoDaDong());
        assertEquals(1, q.soHoKhongDong());
    }
}
