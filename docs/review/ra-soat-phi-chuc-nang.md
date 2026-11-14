# Rà soát phi chức năng sau v1.0.0

Ngày rà soát: 14 – 15/11/2026 · Phạm vi: `main` tại `fb78a61` (v1.0.0 và dữ
liệu demo #46) · Giai đoạn 1: chỉ đọc, chạy thử và đo; không sửa mã trong
Pull Request này.

Mỗi nhận định dưới đây kèm vị trí trong mã, lệnh đã chạy hoặc số đo. Chỗ nào
chưa kiểm được ghi rõ "chưa kiểm" và lý do. Mỗi mục được xếp một trong ba
loại: **lỗi thật** (tái hiện được), **rủi ro** (có điều kiện xảy ra, chưa
thấy xảy ra) và **lựa chọn thiết kế** (cố ý, có ghi trong tài liệu).

## Tóm tắt

- `mvn clean verify -Pmysql` trên MySQL 8.0.46 thử: **366 test đơn vị đạt**
  (3 bỏ qua có chủ ý) và **69/69 kiểm thử tích hợp đạt**.
- **Một mục mức Cao: P1 (G2)**. User CSDL của ứng dụng có `GRANT ALL`, nên
  xóa được trigger rồi xóa sạch nhật ký. Đã tái hiện. Rủi ro khi sửa thấp,
  sửa ở PR `security/tach-user-csdl`.
- **Hiệu năng đạt NFR-005**: với 500 hộ, 1.993 nhân khẩu, 12 – 22 tháng công
  nợ, mọi thao tác tra cứu dưới 0,25 giây; thao tác nặng nhất (dựng thông
  báo phí cho cả 500 hộ) chậm nhất 1,6 giây. Chỉ mục hiện có đủ cho các
  truy vấn chính (mục 3).
- **Độ dài dòng 80 cột:** con số "khoảng 272 dòng" là đếm theo **byte**
  UTF-8 (chữ tiếng Việt có dấu chiếm 2 – 3 byte). Đếm theo **cột** (ký tự,
  như quy ước và Checkstyle) chỉ có **9 dòng** vượt: 4 ở `src/main`, 5 ở
  `src/test`. Vì vậy mục này xếp mức Thấp (P7); vẫn sửa kèm Checkstyle ở PR
  `refactor/do-dai-dong-checkstyle` theo yêu cầu, vì chỉ là 9 chỗ ngắt dòng
  và từ nay CI tự kiểm.
- Năm mục mức Trung bình (P2 – P6) và năm mục mức Thấp còn lại (P8 – P12)
  **chờ duyệt**, không sửa.
- Không phát hiện lỗi nghiệp vụ.

Môi trường thử: `mysqld` 8.0.46 riêng ở cổng 3399, thư mục dữ liệu trong thư
mục tạm của phiên; JDK Temurin 21.0.12; Windows 11, 16 GB RAM. CSDL
`bluemoon` và dịch vụ MySQL80 (cổng 3306) không bị đụng tới. Dữ liệu đo hiệu
năng và các chương trình đo là tệp tạm, không đưa vào repo.

## 1. Bảng tóm tắt

| # | Mục | Loại | Mức | Công (giờ) | Rủi ro khi sửa | Trạng thái |
|---|---|---|---|---|---|---|
| P1 | User CSDL của ứng dụng có `GRANT ALL`: xóa được trigger và nhật ký (G2) | Lỗi thật | **Cao** | 4 | Thấp | Sửa: PR `security/tach-user-csdl` |
| P2 | Lỗi CSDL chỉ hiện thông báo chung, không ghi lại nguyên nhân ở đâu | Lỗi thật | Trung bình | 1 | Thấp | Chờ duyệt |
| P3 | Không đặt thời gian chờ kết nối; mọi thao tác CSDL chạy trên luồng giao diện | Rủi ro | Trung bình | 2 | Thấp – Trung bình | Chờ duyệt |
| P4 | Kết nối MySQL không mã hóa; mật khẩu CSDL nằm ở từng máy trạm | Rủi ro | Trung bình | 3 | Trung bình | Chờ duyệt |
| P5 | Không giới hạn số lần đăng nhập sai; không buộc đổi mật khẩu mặc định của `admin` | Lựa chọn thiết kế | Trung bình | 4 | Trung bình (đổi hành vi) | Chờ duyệt |
| P6 | "In thông báo phí cho tất cả" mở một kết nối cho mỗi hộ | Rủi ro | Trung bình | 1 | Thấp | Chờ duyệt |
| P7 | 9 dòng mã vượt 80 cột; chưa có công cụ kiểm quy ước | Lỗi thật (quy ước) | Thấp | 1 | Thấp | Sửa: PR `refactor/do-dai-dong-checkstyle` |
| P8 | `mvn javafx:run` chạy `App`, bỏ qua `Launcher` nên không đặt múi giờ | Lỗi thật | Thấp | 0,5 | Thấp | Chờ duyệt |
| P9 | Chốt công nợ ghi từng khoản một câu lệnh (1.029 câu cho 500 hộ) | Lựa chọn thiết kế | Thấp | 2 | Trung bình | Chờ duyệt |
| P10 | Thời gian đăng nhập cho biết tên đăng nhập có tồn tại hay không | Rủi ro | Thấp | 0,5 | Thấp | Chờ duyệt |
| P11 | Controller ít test tự động (29% dòng); CI chưa đo độ phủ | Rủi ro | Thấp | 2 | Thấp | Chờ duyệt |
| P12 | `architecture.md` liệt kê thiếu hai hàm Service không kiểm quyền | Tài liệu | Thấp | 0,2 | Không | Chờ duyệt |

## 2. Đánh giá theo NFR

| NFR | Kết luận | Căn cứ |
|---|---|---|
| NFR-001 Xác thực | Đạt, có lưu ý P5, P10 | `TaiKhoanService.dangNhap`; BCrypt cost 12 có salt (`MatKhauUtil`); tài khoản bị khóa không đăng nhập được; thông báo sai chung một câu |
| NFR-002 Phân quyền | Đạt | Menu theo vai trò (`TrangChinhController`, `TrangChinhMenuTest`); mọi hàm public của 14 lớp Service gọi `PhanQuyen.yeuCau` trừ các hàm không đọc dữ liệu (mục 4.1) |
| NFR-003 Sao lưu | Đạt | `scripts/sao-luu.ps1`, `khoi-phuc-thu.ps1` chạy thật trên CSDL 500 hộ: sao lưu 0,7 s (3,3 MB), khôi phục 1,6 s, kết quả KHỚP (mục 5.4) |
| NFR-004 Nhật ký | Đạt sau khi sửa P1 | Ghi trong cùng giao dịch; trigger chặn sửa, xóa. Trước khi sửa P1, user ứng dụng xóa được trigger |
| NFR-005 Phản hồi < 3 giây | Đạt | Bảng số đo mục 3; lưu ý P3, P6 khi chạy qua mạng |
| NFR-006 Pháp lý | Đạt phần chức năng; **chưa kiểm** mẫu biểu | Có danh sách tạm vắng (in, Excel) và lịch sử biến động. Chưa đối chiếu với mẫu biểu chính thức của công an phường vì dự án không có mẫu nào trong tài liệu nguồn |
| NFR-007 Java desktop + MySQL | Đạt | JavaFX 21, MySQL 8, dữ liệu chỉ ở MySQL |
| NFR-008 Dễ dùng cho người lớn tuổi | Đạt | Mục 7 |
| NFR-009 Quyền riêng tư | Đạt, có lưu ý P4 | Thủ quỹ bị Service từ chối ở hộ, nhân khẩu, báo cáo nhân khẩu; tra cứu phí chỉ trả mã phòng, chủ hộ, khoản nợ |

## 3. Số đo hiệu năng

**Dữ liệu:** dựng từ `schema.sql` + `seed.sql`, rồi sinh thêm bằng SQL: 500
hộ, 1.993 nhân khẩu, 287 đợt tạm vắng, 12 tháng chốt công nợ (12.000 khoản
phải nộp), 4.899 phiếu thu (9.798 dòng chi tiết), 6.892 dòng nhật ký. Bước
đo "chốt công nợ tháng mới" chốt thêm 10 tháng, nên các dòng đứng sau nó
trong bảng chạy trên **22 tháng, 22.000 khoản phải nộp**, trong đó 10 tháng
chưa hộ nào đóng (nặng hơn yêu cầu).

**Cách đo:** chương trình Java gọi thẳng Service và DAO thật (như Controller
gọi), mỗi lần gọi mở kết nối mới như ứng dụng; chạy khởi động 1 – 2 lần rồi
đo 10 – 20 lần. MySQL cùng máy (không có độ trễ mạng). Hai cột cuối đọc từ
`SHOW GLOBAL STATUS` (`Connections`, `Questions`) để ước lượng khi chạy qua
mạng: mỗi kết nối mới của Connector/J tốn khoảng 5 câu lệnh khởi tạo ngoài
câu truy vấn chính.

| Thao tác | Số lần | Trung bình (ms) | Chậm nhất (ms) | Kết nối / lần | Câu lệnh / lần |
|---|---|---|---|---|---|
| Đăng nhập (BCrypt cost 12) | 10 | 288 | 310 | 1 | 6 |
| Đọc lại phiên trước mỗi màn hình | 20 | 4 | 6 | 1 | 6 |
| Tra cứu mã phòng và khoản chưa đóng | 20 | 21 | 35 | 5 | 30 |
| Tìm hộ không dấu "nguyen" | 20 | 10 | 12 | 1 | 6 |
| Màn Hộ gia đình: liệt kê 500 hộ | 20 | 16 | 40 | 1 | 6 |
| Nhân khẩu của một hộ | 20 | 3 | 4 | 1 | 6 |
| Trang chính (tổng quan, hộ còn nợ, biến động, nhắc chốt) | 20 | 79 | 98 | 5 | 30 |
| Xem trước chốt công nợ | 10 | 17 | 28 | 2 | 12 |
| Chốt công nợ tháng mới (ghi 1.000 khoản) | 10 | 281 | 333 | 4 | 1.029 |
| Chốt lại tháng đã chốt (không có khoản mới) | 10 | 182 | 196 | 4 | 1.029 |
| Màn Công nợ: khoản phải nộp một tháng (1.000 dòng) | 20 | 14 | 16 | 1 | 6 |
| Thống kê theo khoản | 20 | 39 | 48 | 4 | 24 |
| Hộ nợ đọng: tất cả hộ còn nợ (500 hộ) | 20 | 150 | 210 | 1 | 6 |
| Hộ nợ đọng: nợ từ 3 tháng | 20 | 147 | 176 | 1 | 6 |
| Báo cáo thu tháng và xuất Excel | 10 | 118 | 222 | 1 | 6 |
| Nhật ký 7 ngày gần nhất | 20 | 6 | 9 | 1 | 6 |
| Nhật ký toàn bộ (6.934 dòng) | 10 | 28 | 33 | 1 | 6 |
| Danh sách tạm vắng hôm nay | 20 | 7 | 11 | 1 | 6 |
| Thông báo phí cho tất cả hộ (dữ liệu để in) | 10 | 1.284 | 1.605 | 502 | 3.012 |
| Lịch sử phiếu của một hộ | 20 | 5 | 6 | 1 | 7 |
| Thu một hộ: lập phiếu 24 – 44 khoản | 10 | 56 | 74 | 10 | 202 |
| Phiếu thu hôm nay | 20 | 11 | 19 | 1 | 7 |
| Hủy phiếu trong ngày | 10 | 22 | 37 | 3 | 27 |

**Chưa kiểm:** thời gian vẽ giao diện JavaFX (bảng 500 – 1.000 dòng, xem
trước bản in) và chạy qua mạng LAN thật, vì môi trường rà soát không điều
khiển màn hình và chỉ có một máy. Nhóm cần tự kiểm (mục 9).

**Chỉ mục.** Đọc `performance_schema.events_statements_summary_by_digest`
sau khi đo và `EXPLAIN` các câu chính:

| Truy vấn | Chỉ mục dùng | Ghi chú |
|---|---|---|
| Tra cứu mã phòng (`ma_can_ho = ?`) | `idx_ho_ma_can_ho` | 1 dòng |
| Tìm kiếm `LIKE '%…%'` theo mã, tên chủ hộ | Quét `idx_ho_ma_can_ho` (500 dòng), nối `uq_nk_chu_ho` | Ký tự đại diện ở đầu nên không tìm theo chỉ mục được; 10 ms với 500 hộ, không cần sửa |
| Tổng thu tháng | `idx_pt_thoi_diem` (range) | |
| Nhật ký theo khoảng ngày | `idx_nky_thoi_diem` (range, quét ngược) | Không cần sắp xếp thêm |
| Khoản nợ của hộ | `idx_pn_ho`, `uq_ptct_phai_nop_da_thu` | |
| Hộ nợ đọng | Quét toàn bảng `phai_nop`, `NOT EXISTS` qua `uq_ptct_phai_nop_da_thu` | Câu nặng nhất: 110.500 dòng xét, 144 ms với 22.000 khoản. Tăng tuyến tính theo số tháng; 5 năm × 500 hộ (60.000 khoản) ước khoảng 0,4 giây. Không cần thêm chỉ mục |

Kết luận: không cần thêm chỉ mục nào ở quy mô đề bài.

## 4. Bảo mật

### 4.1 Đã kiểm và đạt

- **Băm mật khẩu:** BCrypt cost 12, salt ngẫu nhiên trong chuỗi băm
  (`MatKhauUtil.java:25`); mật khẩu dài hơn 71 byte bị từ chối thay vì bị
  cắt ngầm (`MatKhauUtil.java:37`, `MatKhauUtilTest`). Đo: 259 ms cho một
  lần thử sai.
- **SQL injection:** 100% câu lệnh trong `dao/` dùng `PreparedStatement`
  với tham số `?`. Lệnh
  `grep -rn "createStatement\|String.format" src/main/java` chỉ ra một chỗ
  nối chuỗi là hằng số múi giờ (`Database.java:115`). Câu SQL dựng động duy
  nhất (`PhieuThuDaoImpl.docDong`) chỉ nối số lượng dấu `?`. Từ khóa tìm
  kiếm được thoát ký tự `%`, `_`, `\` (`HoGiaDinhDaoImpl.thoatLike`).
- **Phân quyền lớp Service:** quét mọi hàm `public` của các lớp `*Service`:
  hàm nào cũng gọi `PhanQuyen.yeuCau` (ba hàm quản lý tài khoản gọi qua
  `taiKhoanKhac`), trừ `dangNhap`, `lamMoiPhien`, ba hàm `thangHienTai`,
  `NhatKyService.tuNgayMacDinh` và `ThuPhiService.kiemTraHinhThuc`, đều
  không đọc dữ liệu nghiệp vụ. `PhanQuyen` được phủ 100% dòng và nhánh.
- **Thông báo lỗi:** người dùng chỉ thấy câu tiếng Việt chung
  (`LoiCsdl.java`), không lộ câu SQL, tên bảng hay stack trace.
- **Nhật ký không chứa mật khẩu** (đã kiểm ở bản rà soát trước, mục 4).
- **Repo:** `config/db.properties` nằm trong `.gitignore` và chưa từng được
  commit (`git log --all -- config/db.properties` rỗng). `git grep` chỉ thấy
  giá trị mẫu `doi_mat_khau_nay` và mật khẩu giả của CI.

### P1. User CSDL của ứng dụng có `GRANT ALL` (G2) — Cao, lỗi thật

- **Hiện trạng:** `docs/setup.md` bước 3 hướng dẫn
  `GRANT ALL ON bluemoon.* TO 'bluemoon'@'localhost'`. CI và kiểm thử tích
  hợp chạy bằng `root`.
- **Bằng chứng** (MySQL thử, CSDL dựng từ `schema.sql` + `seed.sql`, user có
  `GRANT ALL`):

  ```
  > DELETE FROM nhat_ky
  ERROR 1644 (45000): Nhật ký không được sửa hoặc xóa
  > DROP TRIGGER trg_nky_chan_xoa        -- thành công
  > DELETE FROM nhat_ky                  -- thành công
  > SELECT COUNT(*) FROM nhat_ky         -- 0
  ```

  Với user chỉ có `SELECT, INSERT, UPDATE`: `DROP TRIGGER`, `TRUNCATE`,
  `ALTER TABLE`, `DROP TABLE` và `DELETE` đều bị từ chối (lỗi 1142), còn
  `UPDATE nhat_ky` vẫn bị trigger chặn (1644); `SELECT … FOR UPDATE` vẫn
  chạy được.
- **Tác động:** mật khẩu user này nằm trong `config/db.properties` trên mọi
  máy cài ứng dụng. Ai đọc được tệp đó đều xóa được dấu vết trong nhật ký
  (NFR-004, AC-069), hoặc xóa cả bảng.
- **Ứng dụng cần quyền gì:** mã trong `src/main/java` không có câu `DELETE`,
  `TRUNCATE`, lệnh DDL hay gọi thủ tục nào
  (`grep -rn -i "DELETE \|TRUNCATE\|DROP \|ALTER \|CALL " src/main/java`
  rỗng). Vì vậy user ứng dụng chỉ cần `SELECT, INSERT, UPDATE`; không cần
  `DELETE` hay `EXECUTE`.
- **Đề xuất:** tách hai user. User ứng dụng chỉ có `SELECT, INSERT, UPDATE`
  trên `bluemoon.*`; `schema.sql`, `seed.sql`, khôi phục chạy bằng root.
  Kiểm thử tích hợp và CI chạy mã ứng dụng bằng user quyền tối thiểu, dựng
  bảng bằng user quản trị. Cập nhật `setup.md`, CI, `testing.md`.
- **Kiểm chứng sau khi sửa:** một kiểm thử tích hợp mới khẳng định kết nối
  của ứng dụng không `DROP TRIGGER`, `TRUNCATE`, `DELETE`, `ALTER` được trên
  `nhat_ky`; cả 69 kiểm thử tích hợp cũ đạt khi DAO chạy bằng user quyền tối
  thiểu.
- **Giới hạn còn lại sau khi sửa (lựa chọn thiết kế, NFR-007):** ứng dụng
  hai tầng nối thẳng MySQL, nên người có mật khẩu user ứng dụng vẫn `INSERT`,
  `UPDATE` trực tiếp được các bảng nghiệp vụ, bỏ qua phân quyền ở Service.
  Muốn chặn phải có tầng máy chủ ứng dụng, ngoài phạm vi đề bài.

### P4. Kết nối không mã hóa; mật khẩu CSDL ở từng máy — Trung bình, rủi ro

- **Hiện trạng:** URL mẫu (`config/db.properties.example:2`) có
  `useSSL=false&allowPublicKeyRetrieval=true`; mật khẩu CSDL lưu dạng chữ
  thường trong `config/db.properties` trên mỗi máy.
- **Tác động:** khi MySQL ở máy khác trong mạng LAN, họ tên, số CCCD và
  công nợ đi trên mạng không mã hóa (NFR-009); `allowPublicKeyRetrieval`
  cho phép kẻ đứng giữa giả máy chủ để lấy mật khẩu. Khi MySQL và ứng dụng
  cùng một máy (cách demo hiện nay) thì không ảnh hưởng.
- **Chưa kiểm:** chưa chạy thử với `sslMode=REQUIRED` qua hai máy.
- **Đề xuất:** khi triển khai nhiều máy, đổi URL mẫu sang
  `sslMode=REQUIRED` (MySQL 8 tự tạo chứng chỉ), bỏ
  `allowPublicKeyRetrieval`; hướng dẫn đặt quyền đọc tệp cấu hình chỉ cho
  tài khoản Windows của người dùng. Kiểm chứng: `SHOW STATUS LIKE
  'Ssl_cipher'` khác rỗng trong phiên của ứng dụng; bộ kiểm thử tích hợp
  đạt với URL mới.

### P5. Không giới hạn đăng nhập sai; mật khẩu mặc định — Trung bình, lựa chọn thiết kế

- **Hiện trạng:** `TaiKhoanService.dangNhap` không đếm lần sai. Khóa tài
  khoản chỉ do Quản trị viên làm tay (giả định 18). `seed.sql:8` tạo `admin`
  / `Admin@123` công khai trong repo; ứng dụng không buộc đổi ở lần đăng
  nhập đầu (ghi chú phát hành v1.0.0 chỉ nhắc đổi).
- **Tác động:** thử mật khẩu không bị chặn, chỉ bị BCrypt làm chậm (259 ms
  mỗi lần, khoảng 330.000 lần mỗi ngày). Ứng dụng desktop trong văn phòng
  Ban quản trị nên nguy cơ thực tế thấp. US-021 không có AC nào yêu cầu
  khóa sau N lần sai.
- **Đề xuất (cần duyệt vì đổi hành vi):** khóa tạm 5 phút sau 5 lần sai liên
  tiếp, hoặc ít nhất hiện cảnh báo trên Trang chính khi `admin` còn dùng
  mật khẩu mặc định. Kiểm chứng bằng test ở `TaiKhoanServiceTest`.

### P10. Thời gian đăng nhập lộ tên tồn tại — Thấp, rủi ro

- **Bằng chứng:** `TaiKhoanService.java:53-57` trả lỗi ngay khi không thấy
  tên, không chạy BCrypt. Đo 10 lần: tên đúng, mật khẩu sai 259 ms; tên
  không tồn tại 4 ms. Thông báo thì giống nhau.
- **Đề xuất:** khi không thấy tên, vẫn kiểm tra với một chuỗi băm giả cố
  định. Kiểm chứng: hai trường hợp chênh nhau dưới 20%.

## 5. Toàn vẹn dữ liệu và độ tin cậy

### 5.1 Toàn vẹn dữ liệu — đạt

- **Tiền:** cột `DECIMAL(14,2)`, mã dùng `BigDecimal` xuyên suốt. `double`
  chỉ xuất hiện ở tọa độ giao diện, trục biểu đồ (`ThangTrucY.java:94`) và ô
  số của Excel (`TepExcel.java:170`), không tham gia phép tính tiền. Làm
  tròn một chỗ: `CongNoService.java:142` (`HALF_UP` về đồng).
- **Giao dịch:** 18 chỗ `rollback()` trong `dao/`; nhật ký và biến động ghi
  trên cùng kết nối với thao tác (`NhatKyIT`, `GiaoDichHoGiaDinhIT`).
- **Khóa khi nhiều máy:** thứ tự khóa hộ → khoản phải nộp → bộ đếm biên lai;
  quy tắc "quyết định dưới khóa thì đọc có khóa" đã rà từng giao dịch ở
  [doc-co-khoa-trong-giao-dich.md](doc-co-khoa-trong-giao-dich.md), không
  thấy điểm mới.
- **Ràng buộc trong lược đồ:** 17 khóa ngoại, không `ON DELETE CASCADE`;
  `CHECK` cho số tiền không âm, ngày, trạng thái hủy phiếu; `UNIQUE` trên
  cột sinh chặn thu trùng (`uq_ptct_phai_nop_da_thu`), trùng CCCD, hai chủ
  hộ, trùng mã căn hộ đang ở, trùng số biên lai. Các ràng buộc này độc lập
  với mã ứng dụng.

### 5.2 Mất kết nối và MySQL chưa chạy — đạt, có rủi ro P3

Chạy thử bằng Service thật trên CSDL 500 hộ:

| Kịch bản | Kết quả | Thời gian |
|---|---|---|
| MySQL chưa chạy (cổng đóng) lúc đăng nhập | "Không kết nối được cơ sở dữ liệu. Kiểm tra MySQL đã chạy chưa, mạng, …" | 0,4 s |
| Kết nối bị ngắt giữa giao dịch lập phiếu 44 khoản (`KILL` phiên) | Cùng thông báo trên; **0 phiếu, 0 dòng nhật ký được ghi, 44/44 khoản vẫn là nợ** | 2,1 s |
| Máy chủ không tới được (địa chỉ không phản hồi) | Cùng thông báo | **21,2 s**, giao diện đứng |
| Máy khác đang giữ khóa dòng của hộ khi lập phiếu | "Có người khác đang thao tác trên hộ này, vui lòng thử lại"; 0 phiếu được ghi | **50,1 s**, giao diện đứng |

### P3. Không có thời gian chờ; thao tác CSDL chạy trên luồng giao diện — Trung bình, rủi ro

- **Hiện trạng:** URL mẫu không có `connectTimeout`, `socketTimeout`; mã
  không tạo luồng nền nào cho CSDL (`grep "new Thread\|javafx.concurrent"`
  rỗng); thời gian chờ khóa là mặc định 50 giây của InnoDB.
  `architecture.md` đã ghi đây là điểm chưa làm.
- **Tác động:** hai dòng in đậm ở bảng trên. Khi dây mạng lỏng hoặc máy chủ
  tắt, cửa sổ "Not Responding" 21 giây cho mỗi lần bấm. Mất mạng giữa lúc
  đang đọc kết quả thì chờ không giới hạn (**chưa kiểm** được vì cần ngắt
  mạng vật lý).
- **Đề xuất (ít rủi ro nhất):** thêm `connectTimeout=5000` vào URL mẫu và
  `setup.md`; đặt `SET innodb_lock_wait_timeout = 10` cùng chỗ đặt múi giờ ở
  `Database.getConnection`. Chạy CSDL ở luồng nền là thay đổi lớn, để sau.
  Kiểm chứng: chạy lại hai kịch bản trên, kỳ vọng khoảng 5 giây và 10 giây.

### 5.3 Ghi tệp Excel — đạt

| Kịch bản | Kết quả |
|---|---|
| Ghi đè lên tệp đang bị chương trình khác giữ | "Tệp đang được mở trong chương trình khác. Đóng tệp rồi thử lại."; tệp cũ còn nguyên, không còn tệp tạm |
| Thư mục đích không tồn tại | "Không tìm thấy thư mục để lưu tệp." |

Cơ chế: `TepExcel.ghiAnToan` ghi tệp tạm rồi đổi tên (`TepExcel.java:63`).
In ra PDF đi qua máy in của hệ điều hành (giả định 42): **chưa kiểm** vì cần
máy in và màn hình.

### 5.4 Sao lưu và khôi phục — đạt

Chạy script thật (Windows PowerShell 5.1) trên CSDL 500 hộ, user sao lưu chỉ
có `SELECT, SHOW VIEW, TRIGGER, LOCK TABLES, PROCESS`:

```
sao-luu.ps1        → THÀNH CÔNG | bluemoon-20261115-000519.sql | 3,3 MB | 0,7 s
khoi-phuc-thu.ps1  → khôi phục trong 1,6 s; 12 bảng khớp số dòng; tổng tiền
                     phiếu thu chưa hủy khớp (4.912.812.000 đ); hai trigger
                     nhật ký có; KẾT QUẢ: KHỚP
```

**Chưa kiểm:** lịch Task Scheduler chạy đúng giờ (cần để máy qua đêm).

## 6. Khả năng bảo trì

### 6.1 Đã kiểm và đạt

- **Phụ thuộc giữa các tầng** (quét `import`): `service/`, `dao/`, `model/`,
  `xuat/` không nhập `javafx` hay `controller`; `dao/` và `model/` không
  nhập `service`; `model/` không nhập `java.sql`; `service/` không nhập
  `java.sql`. `controller/` chỉ có `LoiCsdl` nhập `java.sql.SQLException`
  để phân loại lỗi. Đúng bảng tầng trong `architecture.md`.
- **Quy ước:** không có ký tự tab, không `System.out.println` trong `src/main`.
- **Độ phủ test** (JaCoCo 0.8.12 chạy tạm qua dòng lệnh, không sửa
  `pom.xml`; gồm cả kiểm thử tích hợp):

  | Gói | Dòng | Nhánh |
  |---|---|---|
  | `service` | 95% | 90% |
  | `xuat` | 98% | 97% |
  | `model` | 94% | 85% |
  | `dao` | 83% | 71% |
  | `util` | 65% | 63% |
  | `controller` | 29% | 27% |
  | Toàn dự án | 63% | 61% |

  Các lớp trọng yếu: `ThuPhiService` 93% / 90%, `CongNoService` 95% / 90%,
  `PhanQuyen` 100% / 100%, `TaiKhoanService` 97% / 84%, `PhieuThuDaoImpl`
  92% / 85%, `CongNoDaoImpl` 85% / 69%, `SoTien` 98% / 96%.

### P2. Lỗi CSDL không được ghi lại — Trung bình, lỗi thật

- **Hiện trạng:** 44 khối `catch (CauHinhException | DaoException e)` trong
  `controller/` chỉ gọi `LoiCsdl.thongBao(e)` để hiện câu chung, ví dụ
  `ThuPhiController.java:207-210`; không chỗ nào in hay ghi `e`. Chỗ duy
  nhất in stack trace là bộ xử lý lỗi chưa bắt (`LoiKhongMongDoi.java:25`),
  và chỉ ra console.
- **Tác động:** khi người dùng thấy "Có lỗi khi đọc hoặc lưu dữ liệu", mã
  lỗi MySQL và câu lệnh gây lỗi mất hẳn. Trong buổi demo, nếu CSDL thiếu
  cột hoặc thiếu quyền (dễ gặp sau khi sửa P1 mà cấp thiếu quyền), nhóm
  không có thông tin nào để tìm nguyên nhân.
- **Đề xuất:** trong `LoiCsdl.thongBao` (một chỗ, 44 nơi gọi sẵn) ghi `e`
  bằng `java.util.logging` (có sẵn trong JDK, POI đã chuyển log về đó) ra
  console và một tệp `bluemoon.log`. Không thêm thư viện.
- **Kiểm chứng:** test gọi `LoiCsdl.thongBao` với một `DaoException` và
  khẳng định handler nhận được bản ghi có nguyên nhân gốc; tắt MySQL rồi bấm
  Tra cứu, tệp log có `SQLException` kèm mã lỗi.

### P7. Độ dài dòng — Thấp, lỗi thật (quy ước)

- **Bằng chứng:** đếm theo ký tự, 9 dòng dài 81 – 88 cột:
  `KhoanThuController.java:105`, `ThuPhiController.java:591`,
  `TrangChinhController.java:210` và `:218`, `SaoLuuIT.java:106` và `:407`,
  `BoCucFxmlTest.java:63` và `:64`, `ThuMotHoNamBuocTest.java:144`. Đếm theo
  byte UTF-8 ra 272 dòng ở `src/main` và 251 ở `src/test`, vì chuỗi và chú
  thích tiếng Việt có dấu; đây là nguồn của con số 272.
- **Đề xuất:** ngắt 9 dòng, thêm Checkstyle (`LineLength` 80,
  `FileTabCharacter`) vào `mvn verify`. Checkstyle đếm theo ký tự, khớp quy
  ước "80 cột".
- **Kiểm chứng:** `mvn verify` trượt khi có dòng 81 cột, đạt sau khi ngắt.

### P8. `mvn javafx:run` không qua `Launcher` — Thấp, lỗi thật

- **Bằng chứng:** `pom.xml:91` đặt `mainClass` là `com.bluemoon.App`; múi
  giờ mặc định của JVM chỉ được đặt trong `Launcher.main`
  (`Launcher.java:31`). Bốn chỗ ở Controller dùng `LocalDate.now()` /
  `YearMonth.now()` theo múi giờ JVM (`CongNoController.java:145`, `:413`,
  `HoGiaDinhController.java:304`, `DanhSachTamVang.java:41`).
- **Tác động:** chỉ khi máy đặt múi giờ khác Việt Nam: "hôm nay" trong ô
  chọn ngày lệch so với giờ CSDL quanh nửa đêm. Máy ở Việt Nam không bị.
- **Đề xuất:** gọi `Launcher.datMuiGioViet()` trong `App.init()`. Kiểm
  chứng: test đặt múi giờ khác rồi kiểm sau `init`.

### P9. Chốt công nợ ghi từng dòng — Thấp, lựa chọn thiết kế

- **Bằng chứng:** 1.029 câu lệnh cho một lần chốt 500 hộ (bảng mục 3), kể
  cả khi chốt lại không có khoản mới. Cùng máy: 281 ms.
- **Tác động:** qua mạng có độ trễ 1 ms mỗi câu thì thêm khoảng 1 giây,
  vẫn dưới 3 giây; chốt là thao tác mỗi tháng một lần.
- **Đề xuất:** để nguyên. Nếu cần, dùng `addBatch` với
  `rewriteBatchedStatements=true`.

### P11. Controller ít test; CI chưa đo độ phủ — Thấp, rủi ro

- **Bằng chứng:** bảng độ phủ ở 6.1; 6 lớp của gói `controller` có 0% dòng được chạy
  (`HoGiaDinhController`, `NhanKhauController`, `TaiKhoanController`,
  `KhoanThuController`, `ThongKeController`, `InAn`). Bố cục FXML được kiểm
  bằng `BoCucFxmlTest`, nhưng xử lý sự kiện thì không.
- **Tác động:** nghiệp vụ nằm ở Service (95%), nên rủi ro là lỗi hiển thị.
  Các bước kiểm tay ở `testing.md` mục 5 đang bù phần này.
- **Đề xuất:** thêm JaCoCo vào `mvn verify` chỉ để xuất báo cáo, chưa đặt
  ngưỡng.

### P12. Tài liệu phân quyền thiếu hai hàm — Thấp

`architecture.md` mục "Phân quyền" ghi "trừ năm hàm"; thực tế có bảy hàm
public của Service không gọi `PhanQuyen.yeuCau`: thiếu
`BaoCaoThuService.thangHienTai` và `NhatKyService.tuNgayMacDinh` (đều không
đọc dữ liệu). Sửa câu chữ.

### P6. In thông báo phí cho tất cả: mỗi hộ một kết nối — Trung bình, rủi ro

- **Bằng chứng:** `ThongBaoPhiService.tatCa` gọi `dao.timHo` trong vòng lặp
  từng hộ (`ThongBaoPhiService.java:80`): 502 kết nối, 3.012 câu lệnh,
  trung bình 1.284 ms, chậm nhất 1.605 ms khi cả 500 hộ còn nợ.
- **Tác động:** là thao tác chậm nhất và là thao tác duy nhất có số kết nối
  tăng theo số hộ. Cùng máy vẫn dưới 3 giây; qua mạng, mỗi kết nối mới thêm
  bắt tay TCP và xác thực nên có thể vượt 3 giây. NFR-005 nói về tra cứu,
  còn đây là chuẩn bị in.
- **Đề xuất:** đọc mọi hộ trong một câu (đã có sẵn câu tìm kiếm trả
  `TomTatHo`), rồi ghép trong bộ nhớ. Kiểm chứng: đo lại, kỳ vọng 2 – 3 kết
  nối và dưới 200 ms; `ThongBaoPhiServiceTest` giữ nguyên kết quả.

## 7. Khả dụng — đạt

- **AC-072 (thu một hộ ≤ 5 bước):** `ThuMotHoNamBuocTest` chạy
  `thu-phi.fxml` thật trên cửa sổ ảo, đếm 4 thao tác (gõ mã rồi Enter, chọn
  hình thức, Thu tất cả, xác nhận).
- **Cỡ chữ:** mọi `-fx-font-size` trong `giao-dien.css` từ 14 px trở lên
  (14, 15, 16, 18, 20, 24, 32, 34 px); chữ gốc 16 px. Không có chỗ nào đặt
  cỡ chữ trong mã Java ngoài tệp Excel.
- **Enter:** nút Đăng nhập là `defaultButton` (`dang-nhap.fxml:21`); các
  form khác gắn Enter qua `PhimEnter.apDung` cho mọi màn hình tải bằng
  `DieuHuong` (`DieuHuong.java:61`).
- **Thông báo lỗi:** tiếng Việt có dấu, nói việc cần làm tiếp (ví dụ thông
  báo mất kết nối, tệp đang mở, khoản vừa đổi ở máy khác).
- **Chưa kiểm:** cảm nhận thực tế của người lớn tuổi, độ tương phản trên
  màn hình thật, thứ tự Tab. Cần người dùng thử (`testing.md` mục 5).

## 8. Điểm mạnh nên trình bày khi bảo vệ

| Điểm | Nói gì | Vị trí |
|---|---|---|
| Băm mật khẩu | BCrypt cost 12, mỗi mật khẩu một salt; khoảng 0,26 giây mỗi lần thử nên dò mật khẩu rất chậm; không lưu mật khẩu gốc | `util/MatKhauUtil.java` |
| Phân quyền hai lớp | Ẩn menu chưa đủ: mọi hàm Service nhận người đăng nhập và kiểm vai trò; tài khoản bị khóa có hiệu lực ngay vì đọc lại trước mỗi màn hình | `service/PhanQuyen.java`, `TaiKhoanService.lamMoiPhien`, `TrangChinhMenuTest` |
| Chống SQL injection | Toàn bộ là `PreparedStatement`; từ khóa tìm kiếm được thoát ký tự của `LIKE` | `dao/*DaoImpl.java`, `HoGiaDinhDaoImpl.thoatLike` |
| Tiền không sai số | `BigDecimal` và `DECIMAL(14,2)`, không dùng `double` để tính | `service/CongNoService.java:142`, `service/SoTien.java`, `schema.sql` |
| Không thu trùng dù hai máy bấm cùng lúc | Khóa dòng theo thứ tự cố định, cộng ràng buộc `UNIQUE` trên cột sinh ở CSDL làm lớp chặn cuối | `dao/PhieuThuDaoImpl.java`, `schema.sql` (`uq_ptct_phai_nop_da_thu`), `PhieuThuIT` |
| Số biên lai liên tục | Bộ đếm theo năm, khóa `FOR UPDATE` trong cùng giao dịch; không dùng `AUTO_INCREMENT` vì nó nhảy số khi hoàn tác | `schema.sql` (`bo_dem_bien_lai`), giả định 31 |
| Nhật ký không sửa được | Ghi cùng giao dịch với thao tác; trigger chặn `UPDATE`, `DELETE`; user ứng dụng không có quyền gỡ trigger (sau P1) | `dao/NhatKyDaoImpl.java`, `schema.sql`, `NhatKyIT` |
| Mất kết nối không làm hỏng dữ liệu | Ngắt kết nối giữa lúc lập phiếu 44 khoản: không có phiếu dở, không có dòng nhật ký thừa | Mục 5.2; `dao/PhieuThuDaoImpl.lapPhieu` |
| Giờ theo máy chủ | Thời điểm lập, hủy phiếu lấy giờ CSDL, nên chỉnh đồng hồ máy thu ngân không hủy được phiếu cũ | `dao/DongHoCsdl.java`, giả định 38 |
| Xuất tệp an toàn | Ghi tệp tạm rồi đổi tên; tệp đang mở trong Excel thì báo rõ, tệp cũ còn nguyên | `xuat/TepExcel.java:63` |
| Sao lưu kiểm chứng được | Script khôi phục thử so số dòng, tổng tiền và trigger với CSDL nguồn; mật khẩu không nằm trong script | `scripts/sao-luu.ps1`, `scripts/khoi-phuc-thu.ps1`, `SaoLuuIT` |
| Hiệu năng có số đo | 500 hộ, 22 tháng: tra cứu 21 ms, Trang chính 79 ms, hộ nợ đọng 150 ms | Mục 3 |
| Kiến trúc tầng sạch | Service không biết JavaFX và SQL; DAO không chứa quy tắc nghiệp vụ; Service test bằng DAO giả | Mục 6.1, `service/KhoGia.java` (test) |
| Test | 366 test đơn vị, 69 kiểm thử tích hợp trên MySQL thật; Service phủ 95% dòng; mỗi AC có dòng trong `testing.md` | `docs/testing.md`, CI hai job |

## 9. Việc nhóm cần tự kiểm trên máy thật

1. Sau khi merge PR sửa P1: trên máy mỗi thành viên, thu hồi `GRANT ALL` và
   cấp lại quyền theo `setup.md`, rồi chạy ứng dụng qua một lượt demo.
2. Chạy ứng dụng với CSDL `seed-demo.sql`, xem các màn có bảng dài (Công nợ,
   Hộ nợ đọng, Nhật ký) cuộn có mượt không; xem trước bản in "Thông báo phí
   cho tất cả".
3. Nếu demo bằng hai máy: thử rút dây mạng khi đang ở màn Thu phí để thấy
   thời gian chờ (P3), và quyết có đặt `connectTimeout` hay không.
4. Các bước kiểm tay còn lại ở `testing.md` mục 5 (in thật, lịch sao lưu
   chạy qua đêm, người chưa dùng thử thu một hộ).
