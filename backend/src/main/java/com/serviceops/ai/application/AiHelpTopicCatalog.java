package com.serviceops.ai.application;

import java.util.List;

/** Static help content kept separate from matching/security logic. */
final class AiHelpTopicCatalog {
    private AiHelpTopicCatalog() {
    }

    static List<AiHelpKnowledgeBase.HelpTopic> topics() {
        return TOPICS;
    }

    private static final List<AiHelpKnowledgeBase.HelpTopic> TOPICS = List.of(
            topicDashboard(), topicServiceRequests(), topicCustomers(), topicAssets(), topicDispatcherCustomerAssetContext(), topicWorkOrders(),
            topicCustomerServiceWorkOrderFollowUp(), topicDispatch(), topicTechnicianWork(), topicTechnicianParts(), topicTechnicianPayment(),
            topicMySchedule(), topicTechnicians(), topicPartRequests(),
            topicInventory(), topicInventoryStocktake(), topicInventoryMovements(), topicInventoryReturns(), topicPayments(),
            topicPaymentSettings(), topicWorkOrderHistory(), topicChannelsManage(), topicChannelsReadOnly(), topicUsers(),
            topicAudit(), topicFormFeedback(), topicOwnerNotifications(), topicDispatcherNotifications(),
            topicCustomerServiceNotifications(), topicTechnicianNotifications(), topicWarehouseNotifications()
    );

    static String roleGuide(String role) {
        return switch (role) {
            case "OWNER" -> "Phạm vi OWNER: quản trị và giám sát toàn hệ thống trong các màn hình dành cho Owner. Owner quản lý người dùng, dữ liệu nền, điều phối, kho, audit và cấu hình tài khoản/QR thanh toán công ty; có thể xem payment/biên nhận để giám sát. Owner không ghi nhận khách xác nhận, không đối soát thanh toán, không đóng phiếu thay CSKH, không giả lập tiến độ hiện trường và không xác nhận hàng ra/vào kho thay đúng vai trò.";
            case "DISPATCHER" -> "Phạm vi DISPATCHER: đọc ngữ cảnh khách hàng/thiết bị cần cho điều phối, theo dõi Work Order, phân công hoặc điều phối lại kỹ thuật viên/lịch trước khi field work bắt đầu và operational cancellation. Không quản trị tài khoản, không tiếp nhận Service Request, không xem nhật ký hệ thống và không thao tác kho.";
            case "CUSTOMER_SERVICE" -> "Phạm vi CUSTOMER_SERVICE: khách hàng, thiết bị, yêu cầu dịch vụ, chuyển yêu cầu đủ thông tin sang Work Order và hậu xử lý dịch vụ. CSKH đối soát chuyển khoản/tiền mặt, thu các khoản khách hẹn thanh toán tại quầy, phát hành biên nhận và đóng phiếu sau khi tiền đã về công ty; có thể mở lại hoặc hủy theo policy trước khi khách xác nhận và phải nhập lý do nghiệp vụ cho hai thao tác này. Không phân công kỹ thuật viên, không ghi nhận khách xác nhận tại hiện trường và không quản trị kho/người dùng.";
            case "TECHNICIAN" -> "Phạm vi TECHNICIAN: chỉ Work Order được giao, Lịch của tôi, tiến độ/chẩn đoán/giải pháp, bằng chứng, yêu cầu phụ tùng và actual-used cho chính job. Sau COMPLETED, kỹ thuật viên ghi nhận khách xác nhận, chi phí thực tế và tình huống thanh toán: khách đã chuyển khoản, kỹ thuật viên đã nhận tiền mặt hoặc khách hẹn thanh toán tại quầy; không đối soát tiền, không phát hành biên nhận và không đóng/mở lại phiếu. Không quản trị người dùng, khách hàng, điều phối hoặc nghiệp vụ quản trị kho.";
            case "WAREHOUSE_STAFF" -> "Phạm vi WAREHOUSE_STAFF: ưu tiên xử lý hàng đợi Yêu cầu phụ tùng do kỹ thuật viên gửi, xác nhận ISSUE thực tế hoặc Không thể cấp; đồng thời quản lý danh mục phụ tùng, nhập kho, ngưỡng tồn, kiểm kê/điều chỉnh, hoàn trả và lịch sử biến động. Không sửa số lượng kỹ thuật viên đã yêu cầu, không thao tác Work Order hiện trường, Customer/Asset, User Management hoặc dashboard vận hành.";
            default -> "Chỉ hướng dẫn các chức năng ServiceOps mà tài khoản hiện tại được phép sử dụng.";
        };
    }

    static AiHelpKnowledgeBase.HelpTopic roleOverview(String role) {
        return switch (role) {
            case "OWNER" -> new AiHelpKnowledgeBase.HelpTopic(
                    "Phạm vi Chủ sở hữu",
                    "/",
                    List.of("OWNER"),
                    List.of(),
                    "Bạn có thể quản trị người dùng; khách hàng và thiết bị; tiếp nhận/chuyển yêu cầu dịch vụ; cấu hình kênh tiếp nhận; theo dõi Work Order và điều phối; quản lý đội ngũ kỹ thuật; quản lý kho, kiểm kê và lịch sử biến động; cấu hình ngân hàng/QR công ty; xem payment, biên nhận, dashboard, notification và audit. Owner giám sát thay vì thực hiện routine field work, không giả lập field progress, customer acceptance, payment settlement hoặc closure thay vai trò phụ trách.",
                    List.of("Bắt đầu ở Tổng quan để xem tình trạng vận hành", "Mở Người dùng để quản lý tài khoản và lọc Đang hoạt động/Tạm ngưng", "Dùng Khách hàng, Thiết bị và Yêu cầu dịch vụ cho dữ liệu đầu vào", "Dùng Phiếu công việc/Lịch điều phối để quản trị điều phối và hậu xử lý", "Dùng Kho phụ tùng, Kiểm kê và Lịch sử biến động cho tồn kho", "Dùng Nhật ký hệ thống và chuông thông báo để truy vết việc quan trọng")
            );
            case "DISPATCHER" -> new AiHelpKnowledgeBase.HelpTopic(
                    "Phạm vi Điều phối viên",
                    "/work-orders",
                    List.of("DISPATCHER"),
                    List.of(),
                    "Bạn tập trung vào Work Order đã chuyển sang điều phối: xem dữ liệu liên quan, phân công kỹ thuật viên, xếp lịch hoặc điều phối lại trước khi công việc bắt đầu, theo dõi lịch và xử lý hủy vận hành khi cần. Bạn không quản trị tài khoản, không tiếp nhận Service Request và không thao tác kho.",
                    List.of("Mở Phiếu công việc để xem hàng việc", "Mở Lịch điều phối để phân công hoặc điều phối lại", "Kiểm tra đội ngũ kỹ thuật trước khi chọn người", "Mở Lịch sử phiếu hoặc tab Tiến trình khi cần truy vết nghiệp vụ điều phối")
            );
            case "CUSTOMER_SERVICE" -> new AiHelpKnowledgeBase.HelpTopic(
                    "Phạm vi Chăm sóc khách hàng",
                    "/service-requests",
                    List.of("CUSTOMER_SERVICE"),
                    List.of(),
                    "Bạn quản lý khách hàng và thiết bị, tiếp nhận/cập nhật yêu cầu dịch vụ, chọn kênh tiếp nhận và chuyển yêu cầu đủ thông tin sang Work Order. Sau dịch vụ, bạn theo dõi Work Order đã hoàn thành; nếu cần mở lại/hủy trước customer acceptance thì phải nhập lý do nghiệp vụ. Bạn theo dõi hàng đợi thanh toán, xác minh tiền chuyển khoản, nhận bàn giao tiền mặt hoặc trực tiếp thu khoản khách hẹn thanh toán tại quầy, phát hành biên nhận và đóng phiếu; không phân công kỹ thuật viên, không ghi nhận khách xác nhận tại hiện trường và không quản trị người dùng/kho.",
                    List.of("Kiểm tra hoặc tạo Khách hàng", "Gắn đúng Thiết bị", "Tiếp nhận Yêu cầu dịch vụ", "Chuyển yêu cầu đủ thông tin sang điều phối", "Theo dõi Work Order đã hoàn thành", "Mở Xử lý thanh toán để đối soát", "Sau SETTLED, phát hành biên nhận và đóng phiếu")
            );
            case "TECHNICIAN" -> new AiHelpKnowledgeBase.HelpTopic(
                    "Phạm vi Kỹ thuật viên",
                    "/work-orders",
                    List.of("TECHNICIAN"),
                    List.of(),
                    "Bạn chỉ thao tác công việc được giao cho mình: xem Lịch của tôi, cập nhật tiến độ thực tế, ghi bằng chứng/chẩn đoán/giải pháp, yêu cầu và ghi actual-used phụ tùng. Sau COMPLETED, bạn nhập chi phí thực tế, ghi nhận khách xác nhận và chọn đúng tình huống thanh toán: khách đã chuyển khoản, đã giao tiền mặt cho kỹ thuật viên hoặc hẹn thanh toán tại quầy; CSKH chịu trách nhiệm đối soát, biên nhận và đóng phiếu. Bạn không quản trị người dùng và không thực hiện nghiệp vụ quản trị kho.",
                    List.of("Mở Lịch của tôi để xem lịch hẹn", "Mở Phiếu công việc được giao", "Cập nhật tiến độ đúng thực tế", "Yêu cầu/ghi actual-used phụ tùng khi cần", "Nhập chẩn đoán và giải pháp trước khi Hoàn thành", "Cho khách xem kết quả và tổng chi phí", "Ghi nhận Khách xác nhận", "Ghi nhận khách chuyển khoản, tiền mặt hoặc hẹn thanh toán tại quầy")
            );
            case "WAREHOUSE_STAFF" -> new AiHelpKnowledgeBase.HelpTopic(
                    "Phạm vi Nhân viên kho",
                    "/part-requests",
                    List.of("WAREHOUSE_STAFF"),
                    List.of(),
                    "Bạn ưu tiên xử lý hàng đợi Yêu cầu phụ tùng: kiểm tra yêu cầu do kỹ thuật viên gửi, xác nhận ISSUE đúng số lượng khi đã giao hàng thực tế hoặc đánh dấu Không thể cấp kèm lý do. Bạn cũng quản lý catalog/tồn kho, nhập kho, ngưỡng tồn tối thiểu, kiểm kê, hoàn trả và lịch sử biến động; không sửa số lượng kỹ thuật viên đã yêu cầu và không thao tác Work Order hiện trường, Customer/Asset, quản trị người dùng hoặc dashboard vận hành.",
                    List.of("Mở Yêu cầu phụ tùng để xử lý request đang chờ", "Xác nhận cấp khi đã giao đúng số lượng thực tế hoặc chọn Không thể cấp kèm lý do", "Mở Kho phụ tùng để kiểm tra tồn và ngưỡng", "Dùng Kiểm kê tồn kho để đối soát", "Dùng Lịch sử biến động để truy vết ISSUE/RETURN/điều chỉnh")
            );
            default -> new AiHelpKnowledgeBase.HelpTopic("Hướng dẫn ServiceOps", "/", List.of("USER"), List.of(), "Chỉ hướng dẫn chức năng mà tài khoản hiện tại được phép sử dụng.", List.of("Mở menu được cấp quyền để bắt đầu"));
        };
    }

    static AiHelpKnowledgeBase.HelpTopic topicDashboard() {
        return new AiHelpKnowledgeBase.HelpTopic("Tổng quan", "/", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE", "TECHNICIAN"),
                List.of("tong quan", "dashboard", "bao cao", "hom nay", "can xem gi", "bat dau", "moi vao lam"),
                "Trang tổng quan giúp người dùng bắt đầu công việc và xem các chỉ số/tình trạng được giao diện hiện tại cung cấp. Trợ lý không tự đọc dữ liệu runtime trong database.",
                List.of("Mở menu Tổng quan", "Xem các thẻ/chỉ số và danh sách đang hiển thị", "Xác định việc cần xử lý theo vai trò", "Đi tới trang nghiệp vụ liên quan để thao tác"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicServiceRequests() {
        return new AiHelpKnowledgeBase.HelpTopic("Yêu cầu dịch vụ", "/service-requests", List.of("OWNER", "CUSTOMER_SERVICE"),
                List.of("yeu cau", "tiep nhan", "khach bao", "tao yeu cau", "service request", "ai goi y", "chuyen thanh phieu", "chuyen sang dieu phoi"),
                "Dùng để ghi nhận sự cố khách báo trước khi đủ điều kiện chuyển thành phiếu công việc. OWNER và CUSTOMER_SERVICE đều có thể chuyển một yêu cầu hợp lệ sang Work Order; backend vẫn kiểm tra customer/asset/state để không tạo trùng hoặc sai liên kết.",
                List.of("Mở menu Yêu cầu dịch vụ", "Bấm Tiếp nhận yêu cầu", "Chọn khách hàng, thiết bị, mức ưu tiên và kênh tiếp nhận", "Nhập tiêu đề hoặc mô tả; AI gợi ý chỉ hỗ trợ chuẩn hóa hai ô này và không thay đổi ưu tiên/kênh", "Bấm Tiếp nhận yêu cầu để lưu", "Khi đủ thông tin, bấm Chuyển sang điều phối"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicCustomers() {
        return new AiHelpKnowledgeBase.HelpTopic("Khách hàng", "/customers", List.of("OWNER", "CUSTOMER_SERVICE"),
                List.of("khach hang", "tao khach", "so dien thoai", "dia chi", "customer", "ho so khach"),
                "Dùng để quản lý hồ sơ khách hàng và dữ liệu nền cho thiết bị/yêu cầu theo quyền của vai trò.",
                List.of("Mở menu Khách hàng", "Tìm khách hàng cần xử lý", "Nếu giao diện cho phép, bấm Thêm khách hàng", "Nhập thông tin cần thiết", "Lưu hồ sơ rồi tiếp tục với thiết bị hoặc yêu cầu dịch vụ"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicAssets() {
        return new AiHelpKnowledgeBase.HelpTopic("Thiết bị", "/assets", List.of("OWNER", "CUSTOMER_SERVICE"),
                List.of("thiet bi", "serial", "bao hanh", "may lanh", "tu lanh", "asset", "model"),
                "Dùng để theo dõi thiết bị theo khách hàng, serial, bảo hành và trạng thái phục vụ.",
                List.of("Mở menu Thiết bị", "Tìm thiết bị theo khách hàng hoặc serial", "Nếu giao diện cho phép, bấm Thêm thiết bị", "Nhập thông tin thiết bị và bảo hành", "Lưu để dùng khi tiếp nhận yêu cầu hoặc tạo phiếu"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicDispatcherCustomerAssetContext() {
        return new AiHelpKnowledgeBase.HelpTopic("Ngữ cảnh khách hàng và thiết bị khi điều phối", "/work-orders", List.of("DISPATCHER"),
                List.of("khach hang", "thiet bi", "serial", "dia chi", "customer", "asset"),
                "Điều phối viên được đọc thông tin khách hàng/thiết bị cần thiết để phân công đúng Work Order, nhưng đây không phải workspace quản lý dữ liệu chính của Dispatcher. Hãy xem ngữ cảnh khách hàng và thiết bị ngay trong Phiếu công việc; việc tạo/sửa hồ sơ thuộc Owner hoặc Chăm sóc khách hàng.",
                List.of("Mở Phiếu công việc", "Chọn phiếu cần điều phối", "Kiểm tra khách hàng, thiết bị và địa chỉ phục vụ", "Mở Lịch điều phối để chọn kỹ thuật viên và thời gian phù hợp"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicWorkOrders() {
        return new AiHelpKnowledgeBase.HelpTopic("Phiếu công việc", "/work-orders", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE", "TECHNICIAN"),
                List.of("phieu", "work order", "cong viec", "phan cong", "xep lich", "trang thai", "ky thuat", "tien trinh", "phu tung da dung", "wo-"),
                "Phiếu công việc là nhiệm vụ thực tế để theo dõi điều phối, lịch hẹn, trạng thái xử lý, phụ tùng, billing và payment. Tab Phụ tùng hiển thị REQUEST/ISSUE/USED/RETURN cùng số lượng; tab Tiến trình kể business story hợp nhất. Sau khách xác nhận, CSKH đối soát payment, biên nhận và closure; vật tư outstanding không chặn đóng phiếu và Warehouse vẫn được RETURN hợp lệ sau CLOSED.",
                List.of("Mở menu Phiếu công việc", "Tìm phiếu theo thông tin mà giao diện cho phép", "Mở chi tiết phiếu", "Mở tab Phụ tùng để xem yêu cầu/cấp/dùng/hoàn", "Mở tab Tiến trình để xem toàn bộ business story", "Thực hiện thao tác đúng vai trò", "Sau SETTLED, CSKH phát hành biên nhận và đóng phiếu"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicCustomerServiceWorkOrderFollowUp() {
        return new AiHelpKnowledgeBase.HelpTopic("Hậu xử lý phiếu cho CSKH", "/work-orders", List.of("CUSTOMER_SERVICE"),
                List.of("mo lai phieu", "mo lai work order", "reopen", "khach bao loi van con", "khach phan hoi loi", "huy phieu", "cancel phieu"),
                "CSKH chỉ mở lại Work Order khi phiếu đang COMPLETED và khách phản hồi cùng sự cố trước customer acceptance; thao tác REOPENED bắt buộc nhập lý do để lưu status history và gửi notification có ngữ cảnh. CANCELLED cũng bắt buộc nhập lý do. Sau CUSTOMER_ACCEPTED hoặc CLOSED không reopen im lặng; nhu cầu/sự cố mới phải đi qua Service Request/Work Order mới.",
                List.of("Mở Phiếu công việc", "Mở đúng phiếu COMPLETED cần hậu xử lý", "Chọn Khách yêu cầu xử lý lại", "Nhập lý do thực tế khách phản hồi", "Xác nhận mở lại và kiểm tra Tiến trình", "Nếu phiếu đã CUSTOMER_ACCEPTED/CLOSED, tạo luồng Service Request/Work Order mới thay vì reopen"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicDispatch() {
        return new AiHelpKnowledgeBase.HelpTopic("Điều phối và xếp lịch", "/schedule", List.of("OWNER", "DISPATCHER"),
                List.of("phan cong", "xep lich", "dieu phoi", "dieu phoi lai", "doi ky thuat vien", "doi lich", "chua bat dau", "reschedule", "reassign", "trung lich"),
                "Dùng để phân công kỹ thuật viên và thời gian thực hiện. Sau lần phân công đầu, OWNER/DISPATCHER có thể điều phối lại kỹ thuật viên hoặc lịch khi Work Order vẫn ở OPEN, SCHEDULED, ASSIGNED hoặc REOPENED. Điều phối lại phải có lý do; nếu đổi người, kỹ thuật viên cũ và mới đều nhận thông báo phù hợp và Tiến trình ghi nhận thay đổi. Khi đã ON_THE_WAY hoặc IN_PROGRESS thì không điều phối lại bằng schedule endpoint.",
                List.of("Mở Lịch điều phối hoặc chi tiết Phiếu công việc", "Chọn kỹ thuật viên và thời gian phù hợp", "Kiểm tra cảnh báo trùng lịch", "Nếu phiếu đã phân công nhưng chưa bắt đầu, dùng Điều phối lại", "Nhập lý do khi đổi kỹ thuật viên hoặc lịch", "Kiểm tra notification và tab Tiến trình sau khi lưu"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicTechnicianWork() {
        return new AiHelpKnowledgeBase.HelpTopic("Công việc kỹ thuật viên", "/work-orders", List.of("TECHNICIAN"),
                List.of("toi la ky thuat", "viec duoc giao", "cap nhat trang thai", "chan doan", "giai phap", "viec cua toi", "dung phu tung", "phu tung da dung", "tien trinh xu ly"),
                "Kỹ thuật viên tập trung vào phiếu được giao, cập nhật trạng thái và ghi nhận kết quả xử lý. Khi cần phụ tùng, kỹ thuật viên tạo Yêu cầu phụ tùng; yêu cầu không giảm tồn kho, Warehouse ISSUE thực tế mới trừ kho. Kỹ thuật viên cập nhật actual-used đến COMPLETED; khi khách đồng ý, nhập chi phí thực tế và dùng action Khách xác nhận để khóa tổng chi phí khách đã đồng ý. Sau đó kỹ thuật viên chọn đúng tình huống thanh toán: khách đã chuyển khoản vào tài khoản công ty, kỹ thuật viên đã nhận tiền mặt hoặc khách chưa thanh toán và sẽ đến quầy CSKH. Kỹ thuật viên không xác minh tiền đã về công ty, không phát hành biên nhận và không đóng/mở lại phiếu; các bước đó thuộc CSKH. CLOSED/CANCELLED là trạng thái kết thúc và sự cố mới sau CLOSED phải đi qua yêu cầu/phiếu mới.",
                List.of("Mở Phiếu công việc được giao", "Cập nhật trạng thái theo tiến độ thực tế", "Nếu cần phụ tùng, tạo/sửa/hủy Yêu cầu phụ tùng trước khi kho cấp", "Sau ISSUE, cập nhật actual-used", "Khi Hoàn thành, nhập Chẩn đoán và Giải pháp", "Nhập tiền công/phí phát sinh thực tế", "Cho khách xem kết quả và tổng tiền", "Bấm Ghi nhận khách xác nhận", "Chọn đúng tình huống thanh toán và xác nhận lại trước khi lưu", "CSKH sẽ đối soát/thu tại quầy và đóng phiếu"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicTechnicianParts() {
        return new AiHelpKnowledgeBase.HelpTopic("Phụ tùng cho công việc được giao", "/work-orders", List.of("TECHNICIAN"),
                List.of("phu tung", "ton kho", "inventory", "sku", "yeu cau phu tung", "cap phu tung", "vat tu"),
                "Kỹ thuật viên được xem danh mục phụ tùng và tạo yêu cầu từ Work Order được giao khi trạng thái cho phép. Yêu cầu không làm giảm tồn; Warehouse xác nhận cấp thực tế mới tạo ISSUE và trừ kho. Sau khi nhận, kỹ thuật viên ghi số lượng thực tế đã dùng. Kỹ thuật viên không nhập kho, không sửa ngưỡng tồn, không kiểm kê/điều chỉnh và không hoàn trả thay Warehouse.",
                List.of("Mở Work Order được giao", "Mở tab Phụ tùng", "Tạo Yêu cầu phụ tùng và nhập số lượng/mục đích", "Sửa hoặc hủy yêu cầu nếu cần trước khi kho cấp", "Sau khi Warehouse xác nhận cấp, ghi số lượng thực tế đã dùng"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicTechnicianPayment() {
        return new AiHelpKnowledgeBase.HelpTopic("Thanh toán tại hiện trường", "/work-orders", List.of("TECHNICIAN"),
                List.of("khach chuyen khoan", "chuyen khoan vao dau", "tai khoan cong ty", "qr cong ty", "nhan tien mat", "thanh toan tai quay", "hen thanh toan", "ghi nhan thanh toan", "payment tai hien truong"),
                "Sau khi khách đã xác nhận kết quả và tổng chi phí đã được khóa, kỹ thuật viên chọn đúng tình huống thanh toán tại Work Order được giao. Với chuyển khoản, kỹ thuật viên xem tài khoản/QR công ty ở chế độ chỉ đọc và ghi nhận khách đã chuyển, có thể đính kèm ảnh giao dịch; với tiền mặt, ghi nhận đã nhận tiền để CSKH biết ai đang giữ; nếu chưa thu tiền và khách sẽ thanh toán trực tiếp với CSKH, chọn Hẹn thanh toán tại quầy. Mỗi lựa chọn đều có bước xác nhận lại. Kỹ thuật viên không xác minh tiền đã về công ty, không SETTLED, không phát hành biên nhận và không đóng phiếu.",
                List.of("Mở Work Order được giao sau Khách xác nhận", "Kiểm tra tổng tiền khách đã xác nhận", "Chọn đúng một tình huống: khách đã chuyển khoản / đã nhận tiền mặt / khách hẹn thanh toán tại quầy", "Đọc lại thông tin và tick xác nhận trước khi lưu", "CSKH đối soát hoặc thu tiền tại quầy, phát hành biên nhận và đóng phiếu"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicMySchedule() {
        return new AiHelpKnowledgeBase.HelpTopic("Lịch của tôi", "/my-schedule", List.of("TECHNICIAN"),
                List.of("lich cua toi", "lich lam viec", "lich hom nay", "lich tuan", "hen cua toi", "ca lam"),
                "Lịch của tôi hiển thị các lịch hẹn được phân công cho kỹ thuật viên đang đăng nhập.",
                List.of("Mở menu Lịch của tôi", "Chọn khoảng thời gian cần xem", "Đọc giờ hẹn và phiếu được giao", "Mở Phiếu công việc để cập nhật tiến độ khi bắt đầu xử lý"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicTechnicians() {
        return new AiHelpKnowledgeBase.HelpTopic("Kỹ thuật viên", "/technicians", List.of("OWNER", "DISPATCHER"),
                List.of("ky thuat vien", "nhan su hien truong", "skills", "tay nghe", "technician", "ho so ky thuat"),
                "Dùng để theo dõi hồ sơ kỹ thuật viên, kỹ năng và trạng thái hoạt động. Với kỹ thuật viên, trạng thái tại Đội ngũ kỹ thuật và trạng thái tài khoản tại Người dùng là cùng một trạng thái nghiệp vụ và được đồng bộ hai chiều: bật/tắt ở một màn hình sẽ cập nhật màn hình còn lại trong cùng giao dịch. Tạm ngưng đồng nghĩa không đăng nhập và không nhận lịch mới; kích hoạt lại đồng nghĩa có thể đăng nhập và nhận lịch mới.",
                List.of("Mở menu Kỹ thuật viên", "Tìm kỹ thuật viên cần xem", "Kiểm tra trạng thái và kỹ năng", "Owner có thể đổi Hoạt động/Tạm ngưng ngay tại đây hoặc tại Người dùng", "Kiểm tra trạng thái đã đồng bộ trước khi phân công phiếu công việc"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicPartRequests() {
        return new AiHelpKnowledgeBase.HelpTopic("Yêu cầu phụ tùng", "/part-requests", List.of("OWNER", "WAREHOUSE_STAFF"),
                List.of("yeu cau phu tung", "hang doi phu tung", "part request", "pending request", "xac nhan cap", "khong the cap", "issue phu tung", "cap vat tu", "sua so luong", "so luong ky thuat vien"),
                "Đây là hàng đợi Warehouse cần xử lý khi kỹ thuật viên yêu cầu phụ tùng. REQUEST chỉ là nhu cầu công việc và không làm giảm tồn kho. Nhân viên kho không sửa số lượng kỹ thuật viên đã yêu cầu: nếu đã giao đúng số lượng thực tế thì xác nhận ISSUE để trừ kho đúng một lần; nếu không thể cấp thì chọn Không thể cấp và nhập lý do. OWNER được xem để giám sát nhưng không xác nhận hàng ra/vào kho thay Warehouse.",
                List.of("Mở Yêu cầu phụ tùng", "Ưu tiên các request REQUESTED", "Đối chiếu SKU, số lượng và Work Order", "Khi đã giao đủ đúng số lượng, bấm Xác nhận cấp", "Nếu không thể cấp, chọn Không thể cấp và nhập lý do", "Theo dõi mục Vật tư đang do kỹ thuật viên giữ để nhận hoàn trả; dùng Lịch sử biến động chỉ để truy vết ISSUE/RETURN"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicInventory() {
        return new AiHelpKnowledgeBase.HelpTopic("Kho phụ tùng", "/inventory", List.of("OWNER", "WAREHOUSE_STAFF"),
                List.of("kho", "phu tung", "ton kho", "nhap kho", "het ton", "inventory", "sku", "muc dat hang", "nguong ton toi thieu", "reorder level"),
                "Dùng để quản lý danh mục phụ tùng và tồn hiện tại. Ngưỡng tồn tối thiểu là mốc cảnh báo tồn thấp, không phải số lượng đặt mua. OWNER/WAREHOUSE_STAFF có thể quản lý catalog, nhập kho và chỉnh ngưỡng; nếu ngưỡng mới làm tồn hiện tại chuyển sang trạng thái tồn thấp, hệ thống phát cảnh báo sau commit cho Warehouse liên quan; Owner theo dõi tồn kho qua workspace thay vì nhận bell vận hành.",
                List.of("Mở menu Kho phụ tùng", "Tìm phụ tùng theo SKU hoặc tên", "Kiểm tra tồn hiện tại và ngưỡng tồn tối thiểu", "Tạo/cập nhật catalog hoặc nhập kho theo quyền", "Dùng Sửa ngưỡng khi cần thay đổi mốc cảnh báo", "Nếu tồn chạm hoặc thấp hơn ngưỡng, kiểm tra cảnh báo tồn thấp", "Dùng Kiểm kê tồn kho hoặc Lịch sử biến động khi cần đối soát"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicInventoryStocktake() {
        return new AiHelpKnowledgeBase.HelpTopic("Kiểm kê tồn kho", "/inventory-stocktake", List.of("OWNER", "WAREHOUSE_STAFF"),
                List.of("kiem ke", "stocktake", "doi soat ton", "chenh lech ton", "ton thuc te", "dieu chinh ton", "adjustment", "thong bao kiem ke", "ai nhan thong bao"),
                "Dùng để đối chiếu số lượng hệ thống với số đếm thực tế. Chênh lệch được ghi thành ADJUSTMENT_IN hoặc ADJUSTMENT_OUT kèm lý do và audit trail. Sau khi giao dịch commit, OWNER được thông báo về chênh lệch; nếu tồn sau kiểm kê chạm hoặc thấp hơn ngưỡng tồn tối thiểu thì Warehouse nhận cảnh báo tồn thấp. Yêu cầu phụ tùng của Technician được xử lý riêng tại hàng đợi Yêu cầu phụ tùng, không nhận broadcast kiểm kê.",
                List.of("Mở menu Kiểm kê tồn kho", "Tìm SKU cần kiểm kê", "Bấm Kiểm kê", "Nhập số lượng thực tế và lý do", "Xác nhận điều chỉnh", "OWNER nhận thông báo nếu có chênh lệch", "Nếu tồn thấp, Warehouse nhận cảnh báo", "Mở Lịch sử biến động để kiểm tra giao dịch điều chỉnh và tồn sau"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicInventoryMovements() {
        return new AiHelpKnowledgeBase.HelpTopic("Lịch sử biến động kho", "/inventory-movements", List.of("OWNER", "WAREHOUSE_STAFF"),
                List.of("lich su bien dong kho", "lich su bien dong", "bien dong kho", "giao dich kho", "inventory movement", "inventory transaction", "ledger", "receive", "consume", "adjustment"),
                "Dùng để truy vết các thay đổi tồn kho như nhập kho, Warehouse cấp phụ tùng cho Work Order, hoàn trả và điều chỉnh kiểm kê; hiển thị actor, thời gian, số lượng, tồn sau và mã Work Order khi có. ISSUE và RETURN hiển thị kỹ thuật viên nhận / trả được ghi nhận tại thời điểm giao dịch để phân biệt rõ người bàn giao vật tư với nhân viên kho thực hiện giao dịch; CONSUME chỉ còn để đọc dữ liệu lịch sử cũ.",
                List.of("Mở menu Lịch sử biến động", "Tìm theo SKU, tên, mã WO, kỹ thuật viên nhận / trả, người thực hiện hoặc ghi chú", "Lọc theo loại giao dịch hoặc khoảng ngày", "Đối chiếu số lượng biến động, kỹ thuật viên nhận / trả và tồn sau", "Đây là sổ truy vết chỉ đọc; nghiệp vụ hoàn trả được xử lý ở Yêu cầu phụ tùng → Vật tư đang do kỹ thuật viên giữ"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicInventoryReturns() {
        return new AiHelpKnowledgeBase.HelpTopic("Hoàn trả phụ tùng theo Work Order", "/part-requests", List.of("OWNER", "WAREHOUSE_STAFF"),
                List.of("hoan tra", "tra lai phu tung", "return part", "part return", "phu tung chua dung", "khong dung het"),
                "Dùng mục Vật tư đang do kỹ thuật viên giữ để Warehouse xác nhận nhận lại phụ tùng đã ISSUE cho Work Order nhưng không được Technician dùng hết. Outstanding được tính theo ISSUE - USED - RETURN; RETURN hợp lệ vẫn được phép sau khi Work Order đã CLOSED và không làm mở lại phiếu. Dữ liệu CONSUME legacy vẫn được hỗ trợ để không phá lịch sử cũ.",
                List.of("Mở menu Yêu cầu phụ tùng", "Xuống mục Vật tư đang do kỹ thuật viên giữ", "Tìm theo Work Order, kỹ thuật viên hoặc phụ tùng", "Bấm Hoàn trả ở đúng dòng còn số lượng đang giữ", "Kiểm tra số lượng tối đa và chỉ xác nhận sau khi kho đã nhận hàng thực tế", "Nhập số lượng và lý do rồi xác nhận; tồn kho tăng và ledger ghi RETURN", "Khi hoàn hết, dòng tự biến mất; mở Lịch sử biến động để truy vết giao dịch RETURN"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicPayments() {
        return new AiHelpKnowledgeBase.HelpTopic("Xử lý thanh toán", "/payments", List.of("OWNER", "CUSTOMER_SERVICE"),
                List.of("thanh toan", "chuyen khoan", "tien mat", "thanh toan tai quay", "doi soat", "bien nhan", "payment", "settled"),
                "Hàng đợi thanh toán cho biết phiếu nào chưa thanh toán, khách đã báo chuyển khoản, tiền mặt đang do kỹ thuật viên giữ, khách hẹn thanh toán tại quầy hoặc đã SETTLED. Với khoản đang chờ, CUSTOMER_SERVICE bấm Đối soát thanh toán để mở đúng Work Order ở tab Thanh toán và kiểm tra lại chi phí khách đã xác nhận trước khi ghi nhận tiền. Nếu khách hẹn thanh toán tại quầy, CSKH chỉ ghi nhận sau khi trực tiếp nhận tiền mặt hoặc xác minh chuyển khoản vào tài khoản công ty; hệ thống yêu cầu xác nhận lại để tránh bấm nhầm. Sau SETTLED, CSKH phát hành biên nhận và đóng Work Order. OWNER chỉ xem để giám sát và cấu hình ngân hàng/QR công ty, không settle/close thay CSKH.",
                List.of("Mở Xử lý thanh toán", "Lọc trạng thái cần xử lý", "Bấm Đối soát thanh toán để mở đúng Work Order / tab Thanh toán", "Kiểm tra chi phí khách đã xác nhận", "Nếu chuyển khoản, đối chiếu tiền thật vào tài khoản công ty", "Nếu tiền mặt, nhận và đếm tiền KTV bàn giao", "Nếu khách hẹn tại quầy, thu tiền trực tiếp rồi chọn đúng phương thức và xác nhận lại", "Xác nhận SETTLED", "Phát hành/tải biên nhận", "Đóng phiếu"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicPaymentSettings() {
        return new AiHelpKnowledgeBase.HelpTopic("Thiết lập thanh toán", "/payment-settings", List.of("OWNER"),
                List.of("tai khoan thanh toan", "tai khoan ngan hang", "ngan hang cong ty", "qr cong ty", "payment settings", "bank account", "bank qr"),
                "Chỉ Chủ sở hữu cấu hình tài khoản ngân hàng và QR nhận tiền của công ty. Đây là thông tin kỹ thuật viên chỉ được xem trong Work Order để hướng dẫn khách chuyển khoản; kỹ thuật viên, CSKH và các vai trò khác không được tự thay đổi tài khoản nhận tiền.",
                List.of("Mở Thiết lập thanh toán", "Nhập ngân hàng, chủ tài khoản và số tài khoản của công ty", "Gắn QR công ty nếu có", "Lưu cấu hình", "Kiểm tra lại thông tin hiển thị cho kỹ thuật viên trước khi dùng tại hiện trường"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicWorkOrderHistory() {
        return new AiHelpKnowledgeBase.HelpTopic("Lịch sử phiếu công việc", "/work-order-history", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE", "TECHNICIAN"),
                List.of("lich su phieu", "lich su phieu da dong", "lich su work order", "work order history", "phieu da dong", "phieu da huy", "cho hoan tat ho so", "da doi soat chua dong", "settled chua dong", "chua dong phieu", "tien trinh thanh toan", "tra lich su"),
                "Dùng để tra Work Order đã CLOSED/CANCELLED và cả hồ sơ CUSTOMER_ACCEPTED có payment đã SETTLED nhưng CSKH chưa đóng phiếu. Trường hợp sau hiển thị Chờ hoàn tất hồ sơ để CSKH quay lại Xử lý thanh toán, phát hành/tải biên nhận nếu cần và Đóng phiếu. Tab Tiến trình của từng Work Order là business story chi tiết gồm điều phối, phụ tùng, hoàn thành, khách xác nhận, thanh toán, biên nhận, đóng phiếu và RETURN hợp lệ sau CLOSED.",
                List.of("Mở Lịch sử phiếu công việc", "Tìm theo mã phiếu, khách hàng hoặc nội dung", "Mở phiếu cần kiểm tra", "Xem Tiến trình để đối chiếu các mốc nghiệp vụ", "Dùng Audit hoặc Lịch sử biến động khi cần chi tiết hệ thống/kho"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicChannelsManage() {
        return new AiHelpKnowledgeBase.HelpTopic("Kênh tiếp nhận", "/service-channels", List.of("OWNER"),
                List.of("kenh", "zalo", "website", "dien thoai", "channel", "nguon tiep nhan"),
                "Dùng để chuẩn hóa nguồn tiếp nhận yêu cầu như điện thoại, website, Zalo hoặc nội bộ.",
                List.of("Mở menu Kênh tiếp nhận", "Kiểm tra các kênh đang hoạt động", "Nếu giao diện cho phép, thêm hoặc chỉnh sửa kênh", "Ưu tiên tắt kênh không còn dùng thay vì xóa dữ liệu đã phát sinh"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicChannelsReadOnly() {
        return new AiHelpKnowledgeBase.HelpTopic("Kênh tiếp nhận", "/service-channels", List.of("CUSTOMER_SERVICE"),
                List.of("kenh", "zalo", "website", "dien thoai", "channel", "nguon tiep nhan"),
                "Chăm sóc khách hàng được xem danh sách kênh tiếp nhận để chọn đúng nguồn khi tạo yêu cầu; trang này là chỉ đọc với vai trò hiện tại.",
                List.of("Mở menu Kênh tiếp nhận", "Xem mã, tên và trạng thái kênh", "Dùng kênh phù hợp khi tiếp nhận yêu cầu", "Nếu cần thay đổi cấu hình kênh, chuyển yêu cầu cho vai trò có quyền quản trị"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicUsers() {
        return new AiHelpKnowledgeBase.HelpTopic("Người dùng", "/users", List.of("OWNER"),
                List.of(
                        "nguoi dung", "tai khoan", "phan quyen", "role", "roles", "tao user",
                        "user management", "user account", "new user account", "create user", "create a user",
                        "create new user", "create a new user", "create account", "create an account",
                        "assign role", "assign roles", "permission", "permissions"
                ),
                "OWNER dùng trang này để quản lý tài khoản, vai trò và trạng thái truy cập của nhân viên. Bộ lọc Tất cả trạng thái / Hoạt động / Tạm ngưng kết hợp với ô tìm kiếm để rà nhanh tài khoản. Username và role được cố định sau khi tạo để giữ audit/ownership; Owner vẫn được đổi họ tên, mật khẩu và trạng thái theo guard an toàn.",
                List.of("Mở menu Người dùng", "Dùng ô tìm kiếm và bộ lọc trạng thái để tìm tài khoản", "Tạo hoặc cập nhật tài khoản theo chính sách nội bộ", "Chọn đúng vai trò khi tạo mới", "Tạm ngưng tài khoản khi không còn quyền truy cập", "Không tắt chính tài khoản đang đăng nhập hoặc Owner cuối cùng"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicAudit() {
        return new AiHelpKnowledgeBase.HelpTopic("Nhật ký hệ thống", "/audit", List.of("OWNER"),
                List.of("nhat ky", "audit", "ai da sua", "ai sua", "lich su thay doi", "truy vet"),
                "Dùng để truy vết hoạt động hệ thống trong phạm vi vai trò được phép xem.",
                List.of("Mở menu Nhật ký hệ thống", "Lọc theo thời gian, người thao tác hoặc đối tượng", "Mở bản ghi liên quan", "Đối chiếu khi cần rà soát thay đổi bất thường"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicFormFeedback() {
        return new AiHelpKnowledgeBase.HelpTopic("Phản hồi biểu mẫu", "/", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE", "TECHNICIAN", "WAREHOUSE_STAFF"),
                List.of("bam dong y khong duoc", "bam hoan thanh khong duoc", "bam luu khong duoc", "nut khong chay", "khong co phan hoi", "thieu thong tin", "truong bat buoc", "validation", "form loi"),
                "Nếu bấm Lưu/Hoàn thành mà dữ liệu bắt buộc còn thiếu, ServiceOps không gửi request: trường lỗi được đánh dấu, trang cuộn tới lỗi đầu tiên và hiện cảnh báo ngắn để người dùng biết cần bổ sung gì. Nếu request đã gửi nhưng API hoặc dữ liệu phụ trợ lỗi, giao diện hiển thị thông báo lỗi và nút Thử lại ở các màn hình tải dữ liệu chính.",
                List.of("Đọc cảnh báo vừa hiện", "Tìm trường có dấu bắt buộc hoặc viền lỗi", "Bổ sung dữ liệu theo thông báo", "Bấm lại nút hành động có tên cụ thể như Hoàn thành công việc hoặc Lưu thay đổi", "Nếu là lỗi tải dữ liệu, bấm Thử lại hoặc kiểm tra backend"));
    }

    static List<String> notificationKeywords() {
        return List.of("thong bao", "thong bao nao", "chuong thong bao", "notification", "bell notification", "chua doc", "da doc", "danh dau chua doc", "mark unread", "mark as unread", "bam nham da doc");
    }

    static List<String> notificationSteps() {
        return List.of("Đọc tiêu đề để biết việc cần chú ý", "Đọc dòng mô tả để biết bước tiếp theo", "Bấm một thông báo chưa đọc để đánh dấu đã đọc", "Nếu lỡ đánh dấu đã đọc, bấm nút ngoài cùng bên phải để chuyển lại Chưa đọc", "Dùng mã WO/SKU trong thông báo để tìm đúng phiếu hoặc phụ tùng khi cần");
    }

    static AiHelpKnowledgeBase.HelpTopic topicOwnerNotifications() {
        return new AiHelpKnowledgeBase.HelpTopic("Thông báo", "/", List.of("OWNER"), notificationKeywords(),
                "Chuông của Chủ sở hữu chỉ dành cho kết quả/ngoại lệ cần giám sát: Work Order đã CLOSED/CANCELLED và chênh lệch kiểm kê cần chú ý. Owner không nhận routine reopen, overdue, waiting-for-parts, part-request hoặc low-stock để tránh biến bell thành audit log; dùng Dashboard, Timeline, Audit và các workspace chuyên môn để xem chi tiết.",
                notificationSteps());
    }

    static AiHelpKnowledgeBase.HelpTopic topicDispatcherNotifications() {
        return new AiHelpKnowledgeBase.HelpTopic("Thông báo", "/", List.of("DISPATCHER"), notificationKeywords(),
                "Chuông của Điều phối viên tập trung vào việc cần điều phối: Work Order cần phân công, đang chờ phụ tùng, cần xử lý lại và lịch thực hiện đã quá hạn. Hãy mở Phiếu công việc hoặc Lịch điều phối để xử lý; CRUD khách hàng/thiết bị và audit hệ thống không phải notification queue của Dispatcher.",
                notificationSteps());
    }

    static AiHelpKnowledgeBase.HelpTopic topicCustomerServiceNotifications() {
        return new AiHelpKnowledgeBase.HelpTopic("Thông báo", "/", List.of("CUSTOMER_SERVICE"), notificationKeywords(),
                "Chuông của Chăm sóc khách hàng tập trung vào việc cần liên hệ/hậu xử lý khách: Work Order vừa hoàn thành, bị mở lại/hủy bởi vai trò khác, quá hạn kéo dài qua grace period, hoặc phát sinh payment handoff cần xử lý. Khi kỹ thuật viên báo khách đã chuyển khoản, đang giữ tiền mặt hoặc khách hẹn thanh toán tại quầy, CSKH nhận notification tương ứng và xử lý chi tiết trong hàng đợi Xử lý thanh toán; bell chỉ nhắc sự kiện cần hành động, không thay thế payment queue.",
                notificationSteps());
    }

    static AiHelpKnowledgeBase.HelpTopic topicTechnicianNotifications() {
        return new AiHelpKnowledgeBase.HelpTopic("Thông báo", "/", List.of("TECHNICIAN"), notificationKeywords(),
                "Chuông của Kỹ thuật viên chỉ nhắc các công việc được giao cho chính mình: phân công mới, đổi lịch/chuyển giao, mở lại, hủy, đóng hoặc quá lịch. Kỹ thuật viên không nhận cảnh báo quản trị kho, đối soát thanh toán hoặc quản trị người dùng.",
                notificationSteps());
    }

    static AiHelpKnowledgeBase.HelpTopic topicWarehouseNotifications() {
        return new AiHelpKnowledgeBase.HelpTopic("Thông báo", "/part-requests", List.of("WAREHOUSE_STAFF"), notificationKeywords(),
                "Chuông của Nhân viên kho tập trung vào yêu cầu phụ tùng mới cần xử lý và cảnh báo tồn thấp. Yêu cầu mới được xử lý tại Yêu cầu phụ tùng; kiểm kê và lịch sử stock nằm trong workspace Kho & vật tư. Warehouse không nhận notification về tiến độ Work Order, đối soát thanh toán hoặc quản trị người dùng.",
                notificationSteps());
    }
}
