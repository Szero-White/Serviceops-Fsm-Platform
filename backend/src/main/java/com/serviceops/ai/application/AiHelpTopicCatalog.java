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
            case "OWNER" -> "Chủ sở hữu quản trị và giám sát toàn hệ thống: người dùng, dữ liệu nền, điều phối, kho, thông tin nhận thanh toán và nhật ký hệ thống. Vai trò này chủ yếu giám sát; không thay kỹ thuật viên cập nhật công việc, không thay chăm sóc khách hàng đối soát thanh toán hoặc đóng phiếu, và không thay nhân viên kho xác nhận hàng ra/vào.";
            case "DISPATCHER" -> "Điều phối viên theo dõi phiếu công việc, xem thông tin khách hàng và thiết bị cần cho việc phân công, xếp lịch hoặc điều chỉnh lịch trước khi công việc bắt đầu. Không quản trị tài khoản, không tiếp nhận yêu cầu dịch vụ, không xem nhật ký hệ thống và không thao tác kho.";
            case "CUSTOMER_SERVICE" -> "Chăm sóc khách hàng quản lý khách hàng, thiết bị và yêu cầu dịch vụ; chuyển yêu cầu đủ thông tin sang phiếu công việc và thực hiện các bước hậu xử lý. Vai trò này đối soát chuyển khoản, nhận bàn giao tiền mặt, thu khoản khách hẹn thanh toán tại quầy, phát hành biên nhận và đóng phiếu sau khi tiền đã về công ty. Không phân công kỹ thuật viên, không ghi nhận khách xác nhận tại hiện trường và không quản trị kho hoặc người dùng.";
            case "TECHNICIAN" -> "Kỹ thuật viên chỉ thao tác các phiếu công việc được giao: xem lịch, cập nhật tiến độ, chẩn đoán, giải pháp, bằng chứng, yêu cầu phụ tùng và số lượng thực tế đã sử dụng. Sau khi hoàn thành công việc, kỹ thuật viên ghi nhận khách xác nhận, chi phí thực tế và tình huống thanh toán. Không đối soát tiền, không phát hành biên nhận, không đóng hoặc mở lại phiếu và không quản trị người dùng, khách hàng hay kho.";
            case "WAREHOUSE_STAFF" -> "Nhân viên kho ưu tiên xử lý yêu cầu phụ tùng do kỹ thuật viên gửi, xác nhận việc cấp phụ tùng thực tế hoặc ghi nhận không thể cấp; đồng thời quản lý danh mục, tồn kho, nhập kho, ngưỡng cảnh báo, kiểm kê, điều chỉnh, hoàn trả và lịch sử biến động. Không sửa số lượng kỹ thuật viên đã yêu cầu và không thao tác công việc hiện trường, khách hàng, thiết bị hoặc người dùng.";
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
                    "Bạn có thể quản trị người dùng; khách hàng và thiết bị; tiếp nhận và chuyển yêu cầu dịch vụ; cấu hình kênh tiếp nhận; theo dõi phiếu công việc và điều phối; quản lý đội ngũ kỹ thuật; quản lý kho, kiểm kê và lịch sử biến động; cấu hình thông tin nhận thanh toán; xem biên nhận, tổng quan, thông báo và nhật ký hệ thống. Chủ sở hữu chủ yếu giám sát, không làm thay các bước nghiệp vụ thuộc kỹ thuật viên, chăm sóc khách hàng hoặc nhân viên kho.",
                    List.of("Bắt đầu ở Tổng quan để xem tình trạng vận hành", "Mở Người dùng để quản lý tài khoản và lọc Đang hoạt động/Tạm ngưng", "Dùng Khách hàng, Thiết bị và Yêu cầu dịch vụ cho dữ liệu đầu vào", "Dùng Phiếu công việc/Lịch điều phối để quản trị điều phối và hậu xử lý", "Dùng Kho phụ tùng, Kiểm kê và Lịch sử biến động cho tồn kho", "Dùng Nhật ký hệ thống và chuông thông báo để truy vết việc quan trọng")
            );
            case "DISPATCHER" -> new AiHelpKnowledgeBase.HelpTopic(
                    "Phạm vi Điều phối viên",
                    "/work-orders",
                    List.of("DISPATCHER"),
                    List.of(),
                    "Bạn tập trung vào phiếu công việc đã chuyển sang điều phối: xem dữ liệu liên quan, phân công kỹ thuật viên, xếp lịch hoặc điều phối lại trước khi công việc bắt đầu, theo dõi lịch và xử lý hủy vận hành khi cần. Bạn không quản trị tài khoản, không tiếp nhận yêu cầu dịch vụ và không thao tác kho.",
                    List.of("Mở Phiếu công việc để xem hàng việc", "Mở Lịch điều phối để phân công hoặc điều phối lại", "Kiểm tra đội ngũ kỹ thuật trước khi chọn người", "Mở Lịch sử phiếu hoặc tab Tiến trình khi cần truy vết nghiệp vụ điều phối")
            );
            case "CUSTOMER_SERVICE" -> new AiHelpKnowledgeBase.HelpTopic(
                    "Phạm vi Chăm sóc khách hàng",
                    "/service-requests",
                    List.of("CUSTOMER_SERVICE"),
                    List.of(),
                    "Bạn quản lý khách hàng và thiết bị, tiếp nhận/cập nhật yêu cầu dịch vụ, chọn kênh tiếp nhận và chuyển yêu cầu đủ thông tin sang phiếu công việc. Sau dịch vụ, bạn theo dõi phiếu công việc đã hoàn thành; nếu cần mở lại/hủy trước khách xác nhận thì phải nhập lý do nghiệp vụ. Bạn theo dõi hàng đợi thanh toán, xác minh tiền chuyển khoản, nhận bàn giao tiền mặt hoặc trực tiếp thu khoản khách hẹn thanh toán tại quầy, phát hành biên nhận và đóng phiếu; không phân công kỹ thuật viên, không ghi nhận khách xác nhận tại hiện trường và không quản trị người dùng/kho.",
                    List.of("Kiểm tra hoặc tạo Khách hàng", "Gắn đúng Thiết bị", "Tiếp nhận Yêu cầu dịch vụ", "Chuyển yêu cầu đủ thông tin sang điều phối", "Theo dõi phiếu công việc đã hoàn thành", "Mở Xử lý thanh toán để đối soát", "Sau khi tiền đã được đối soát, phát hành biên nhận và đóng phiếu")
            );
            case "TECHNICIAN" -> new AiHelpKnowledgeBase.HelpTopic(
                    "Phạm vi Kỹ thuật viên",
                    "/work-orders",
                    List.of("TECHNICIAN"),
                    List.of(),
                    "Bạn chỉ thao tác công việc được giao cho mình: xem Lịch của tôi, cập nhật tiến độ thực tế, ghi bằng chứng, chẩn đoán, giải pháp, yêu cầu phụ tùng và số lượng thực tế đã sử dụng. Sau khi hoàn thành, bạn nhập chi phí thực tế, ghi nhận khách xác nhận và chọn đúng tình huống thanh toán. Chăm sóc khách hàng chịu trách nhiệm đối soát, phát hành biên nhận và đóng phiếu. Bạn không quản trị người dùng và không thực hiện nghiệp vụ quản trị kho.",
                    List.of("Mở Lịch của tôi để xem lịch hẹn", "Mở Phiếu công việc được giao", "Cập nhật tiến độ đúng thực tế", "Yêu cầu/ghi số lượng thực tế đã sử dụng phụ tùng khi cần", "Nhập chẩn đoán và giải pháp trước khi Hoàn thành", "Cho khách xem kết quả và tổng chi phí", "Ghi nhận Khách xác nhận", "Ghi nhận khách chuyển khoản, tiền mặt hoặc hẹn thanh toán tại quầy")
            );
            case "WAREHOUSE_STAFF" -> new AiHelpKnowledgeBase.HelpTopic(
                    "Phạm vi Nhân viên kho",
                    "/part-requests",
                    List.of("WAREHOUSE_STAFF"),
                    List.of(),
                    "Bạn ưu tiên xử lý Yêu cầu phụ tùng: kiểm tra yêu cầu do kỹ thuật viên gửi, xác nhận đúng số lượng đã giao thực tế hoặc ghi nhận Không thể cấp kèm lý do. Bạn cũng quản lý danh mục và tồn kho, nhập kho, ngưỡng tồn tối thiểu, kiểm kê, hoàn trả và lịch sử biến động; không sửa số lượng kỹ thuật viên đã yêu cầu và không thao tác công việc hiện trường, khách hàng, thiết bị hoặc người dùng.",
                    List.of("Mở Yêu cầu phụ tùng để xử lý các yêu cầu đang chờ", "Xác nhận cấp khi đã giao đúng số lượng thực tế hoặc chọn Không thể cấp kèm lý do", "Mở Kho phụ tùng để kiểm tra tồn và ngưỡng", "Dùng Kiểm kê tồn kho để đối soát", "Dùng Lịch sử biến động để kiểm tra các lần cấp, hoàn trả và điều chỉnh")
            );
            default -> new AiHelpKnowledgeBase.HelpTopic("Hướng dẫn ServiceOps", "/", List.of("USER"), List.of(), "Chỉ hướng dẫn chức năng mà tài khoản hiện tại được phép sử dụng.", List.of("Mở menu được cấp quyền để bắt đầu"));
        };
    }

    static AiHelpKnowledgeBase.HelpTopic topicDashboard() {
        return new AiHelpKnowledgeBase.HelpTopic("Tổng quan", "/", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE", "TECHNICIAN"),
                List.of("tong quan", "dashboard", "bao cao", "hom nay", "can xem gi", "bat dau", "moi vao lam"),
                "Trang tổng quan giúp người dùng bắt đầu công việc và xem các chỉ số, tình trạng mà hệ thống đang hiển thị. Trợ lý không tự đọc dữ liệu vận hành hiện tại của doanh nghiệp.",
                List.of("Mở menu Tổng quan", "Xem các thẻ/chỉ số và danh sách đang hiển thị", "Xác định việc cần xử lý theo vai trò", "Đi tới trang nghiệp vụ liên quan để thao tác"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicServiceRequests() {
        return new AiHelpKnowledgeBase.HelpTopic("Yêu cầu dịch vụ", "/service-requests", List.of("OWNER", "CUSTOMER_SERVICE"),
                List.of("yeu cau", "tiep nhan", "khach bao", "tao yeu cau", "service request", "ai goi y", "chuyen thanh phieu", "chuyen sang dieu phoi"),
                "Dùng để ghi nhận nhu cầu hoặc sự cố khách hàng phản ánh trước khi chuyển sang phiếu công việc. Chủ sở hữu và Chăm sóc khách hàng có thể chuyển yêu cầu khi thông tin khách hàng, thiết bị và trạng thái yêu cầu hợp lệ; hệ thống tự kiểm tra để tránh tạo trùng hoặc liên kết sai dữ liệu.",
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
                "Dùng để theo dõi thiết bị theo khách hàng, số sê-ri, bảo hành và trạng thái sử dụng.",
                List.of("Mở menu Thiết bị", "Tìm thiết bị theo khách hàng hoặc số sê-ri", "Nếu giao diện cho phép, bấm Thêm thiết bị", "Nhập thông tin thiết bị và bảo hành", "Lưu để dùng khi tiếp nhận yêu cầu hoặc tạo phiếu"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicDispatcherCustomerAssetContext() {
        return new AiHelpKnowledgeBase.HelpTopic("Ngữ cảnh khách hàng và thiết bị khi điều phối", "/work-orders", List.of("DISPATCHER"),
                List.of("khach hang", "thiet bi", "serial", "dia chi", "customer", "asset"),
                "Điều phối viên được xem thông tin khách hàng và thiết bị cần thiết để phân công đúng phiếu công việc. Việc tạo hoặc chỉnh sửa hồ sơ khách hàng, thiết bị thuộc Chủ sở hữu hoặc Chăm sóc khách hàng; Điều phối viên chỉ sử dụng thông tin này để sắp xếp công việc.",
                List.of("Mở Phiếu công việc", "Chọn phiếu cần điều phối", "Kiểm tra khách hàng, thiết bị và địa chỉ phục vụ", "Mở Lịch điều phối để chọn kỹ thuật viên và thời gian phù hợp"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicWorkOrders() {
        return new AiHelpKnowledgeBase.HelpTopic("Phiếu công việc", "/work-orders", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE", "TECHNICIAN"),
                List.of("phieu", "work order", "cong viec", "phan cong", "xep lich", "trang thai", "ky thuat", "tien trinh", "phu tung da dung", "wo-"),
                "Phiếu công việc là nơi theo dõi toàn bộ quá trình thực hiện: điều phối, lịch hẹn, tiến độ, phụ tùng, chi phí và thanh toán. Mục Phụ tùng cho biết số lượng đã yêu cầu, đã cấp, đã dùng và đã hoàn trả; mục Tiến trình trình bày các mốc theo thứ tự thời gian. Sau khi khách xác nhận, Chăm sóc khách hàng đối soát thanh toán, phát hành biên nhận và đóng phiếu. Phụ tùng còn đang giữ không chặn việc đóng phiếu; Nhân viên kho vẫn có thể nhận hoàn trả hợp lệ sau đó.",
                List.of("Mở menu Phiếu công việc", "Tìm phiếu theo thông tin mà giao diện cho phép", "Mở chi tiết phiếu", "Mở mục Phụ tùng để xem yêu cầu, cấp, sử dụng và hoàn trả", "Mở mục Tiến trình để xem các mốc xử lý", "Thực hiện thao tác đúng vai trò", "Sau khi tiền đã được đối soát, Chăm sóc khách hàng phát hành biên nhận và đóng phiếu"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicCustomerServiceWorkOrderFollowUp() {
        return new AiHelpKnowledgeBase.HelpTopic("Hậu xử lý phiếu cho chăm sóc khách hàng", "/work-orders", List.of("CUSTOMER_SERVICE"),
                List.of("mo lai phieu", "mo lai work order", "reopen", "khach bao loi van con", "khach phan hoi loi", "huy phieu", "cancel phieu"),
                "Chăm sóc khách hàng chỉ mở lại phiếu công việc đã hoàn thành khi khách phản hồi cùng sự cố trước bước khách xác nhận. Mở lại hoặc hủy phiếu đều bắt buộc nhập lý do để lịch sử xử lý và thông báo có đủ ngữ cảnh. Sau khi khách đã xác nhận hoặc phiếu đã đóng, sự cố mới phải được tiếp nhận bằng yêu cầu dịch vụ và phiếu công việc mới.",
                List.of("Mở Phiếu công việc", "Mở đúng phiếu đã hoàn thành cần hậu xử lý", "Chọn Khách yêu cầu xử lý lại", "Nhập lý do thực tế khách phản hồi", "Xác nhận mở lại và kiểm tra Tiến trình", "Nếu khách đã xác nhận hoặc phiếu đã đóng, tạo yêu cầu dịch vụ và phiếu công việc mới"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicDispatch() {
        return new AiHelpKnowledgeBase.HelpTopic("Điều phối và xếp lịch", "/schedule", List.of("OWNER", "DISPATCHER"),
                List.of("phan cong", "xep lich", "dieu phoi", "dieu phoi lai", "doi ky thuat vien", "doi lich", "chua bat dau", "reschedule", "reassign", "trung lich"),
                "Dùng để phân công kỹ thuật viên và thời gian thực hiện. Sau lần phân công đầu, Chủ sở hữu hoặc Điều phối viên có thể đổi kỹ thuật viên hoặc lịch khi công việc chưa bắt đầu thực tế hoặc vừa được mở lại để xử lý. Điều phối lại phải có lý do; nếu đổi người, kỹ thuật viên cũ và mới đều nhận thông báo phù hợp và Tiến trình ghi nhận thay đổi. Khi kỹ thuật viên đã di chuyển hoặc bắt đầu xử lý thì không được điều phối lại.",
                List.of("Mở Lịch điều phối hoặc chi tiết Phiếu công việc", "Chọn kỹ thuật viên và thời gian phù hợp", "Kiểm tra cảnh báo trùng lịch", "Nếu phiếu đã phân công nhưng chưa bắt đầu, dùng Điều phối lại", "Nhập lý do khi đổi kỹ thuật viên hoặc lịch", "Kiểm tra thông báo và mục Tiến trình sau khi lưu"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicTechnicianWork() {
        return new AiHelpKnowledgeBase.HelpTopic("Công việc kỹ thuật viên", "/work-orders", List.of("TECHNICIAN"),
                List.of("toi la ky thuat", "viec duoc giao", "cap nhat trang thai", "chan doan", "giai phap", "viec cua toi", "dung phu tung", "phu tung da dung", "tien trinh xu ly"),
                "Kỹ thuật viên tập trung vào phiếu được giao, cập nhật tiến độ và ghi nhận kết quả xử lý. Khi cần phụ tùng, kỹ thuật viên tạo Yêu cầu phụ tùng; chỉ khi Nhân viên kho xác nhận đã giao thực tế thì tồn kho mới giảm. Kỹ thuật viên ghi số lượng thực tế đã sử dụng trước khi hoàn thành. Khi khách đồng ý, nhập chi phí thực tế và ghi nhận khách xác nhận để khóa tổng chi phí đã thống nhất. Sau đó chọn đúng tình huống thanh toán: khách đã chuyển khoản, kỹ thuật viên đã nhận tiền mặt hoặc khách sẽ thanh toán tại quầy. Việc đối soát tiền, phát hành biên nhận và đóng phiếu thuộc Chăm sóc khách hàng. Phiếu đã đóng hoặc đã hủy là kết thúc; sự cố mới phải đi qua yêu cầu dịch vụ mới.",
                List.of("Mở Phiếu công việc được giao", "Cập nhật trạng thái theo tiến độ thực tế", "Nếu cần phụ tùng, tạo, sửa hoặc hủy Yêu cầu phụ tùng trước khi kho cấp", "Sau khi kho cấp, cập nhật số lượng thực tế đã sử dụng", "Khi Hoàn thành, nhập Chẩn đoán và Giải pháp", "Nhập tiền công và phí phát sinh thực tế", "Cho khách xem kết quả và tổng tiền", "Bấm Ghi nhận khách xác nhận", "Chọn đúng tình huống thanh toán và xác nhận lại trước khi lưu", "Chăm sóc khách hàng sẽ đối soát, thu tại quầy nếu cần và đóng phiếu"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicTechnicianParts() {
        return new AiHelpKnowledgeBase.HelpTopic("Phụ tùng cho công việc được giao", "/work-orders", List.of("TECHNICIAN"),
                List.of("phu tung", "ton kho", "inventory", "sku", "yeu cau phu tung", "cap phu tung", "vat tu"),
                "Kỹ thuật viên được xem danh mục phụ tùng và tạo yêu cầu từ phiếu công việc được giao khi trạng thái cho phép. Yêu cầu chưa làm giảm tồn kho; Nhân viên kho chỉ trừ tồn sau khi xác nhận đã cấp thực tế. Sau khi nhận, kỹ thuật viên ghi số lượng thực tế đã dùng. Kỹ thuật viên không nhập kho, không sửa ngưỡng tồn, không kiểm kê, điều chỉnh hoặc xác nhận hoàn trả thay Nhân viên kho.",
                List.of("Mở phiếu công việc được giao", "Mở mục Phụ tùng", "Tạo Yêu cầu phụ tùng và nhập số lượng, mục đích", "Sửa hoặc hủy yêu cầu nếu cần trước khi kho cấp", "Sau khi Nhân viên kho xác nhận cấp, ghi số lượng thực tế đã dùng"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicTechnicianPayment() {
        return new AiHelpKnowledgeBase.HelpTopic("Thanh toán tại hiện trường", "/work-orders", List.of("TECHNICIAN"),
                List.of("khach chuyen khoan", "chuyen khoan vao dau", "tai khoan cong ty", "qr cong ty", "nhan tien mat", "thanh toan tai quay", "hen thanh toan", "ghi nhan thanh toan", "payment tai hien truong"),
                "Sau khi khách đã xác nhận kết quả và tổng chi phí đã được khóa, kỹ thuật viên chọn đúng tình huống thanh toán tại phiếu công việc được giao. Với chuyển khoản, kỹ thuật viên xem tài khoản và mã QR của công ty ở chế độ chỉ đọc, ghi nhận khách đã chuyển và có thể đính kèm ảnh giao dịch. Với tiền mặt, ghi nhận đã nhận tiền để Chăm sóc khách hàng biết ai đang giữ. Nếu chưa thu tiền và khách sẽ thanh toán trực tiếp tại quầy, chọn Hẹn thanh toán tại quầy. Kỹ thuật viên không xác minh tiền đã về công ty, không phát hành biên nhận và không đóng phiếu.",
                List.of("Mở phiếu công việc được giao sau Khách xác nhận", "Kiểm tra tổng tiền khách đã xác nhận", "Chọn đúng một tình huống: khách đã chuyển khoản / đã nhận tiền mặt / khách hẹn thanh toán tại quầy", "Đọc lại thông tin và tick xác nhận trước khi lưu", "chăm sóc khách hàng đối soát hoặc thu tiền tại quầy, phát hành biên nhận và đóng phiếu"));
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
                List.of("Mở menu Kỹ thuật viên", "Tìm kỹ thuật viên cần xem", "Kiểm tra trạng thái và kỹ năng", "Chủ sở hữu có thể đổi Hoạt động/Tạm ngưng ngay tại đây hoặc tại Người dùng", "Kiểm tra trạng thái đã đồng bộ trước khi phân công phiếu công việc"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicPartRequests() {
        return new AiHelpKnowledgeBase.HelpTopic("Yêu cầu phụ tùng", "/part-requests", List.of("OWNER", "WAREHOUSE_STAFF"),
                List.of("yeu cau phu tung", "hang doi phu tung", "part request", "pending request", "xac nhan cap", "khong the cap", "issue phu tung", "cap vat tu", "sua so luong", "so luong ky thuat vien"),
                "Đây là danh sách Nhân viên kho cần xử lý khi kỹ thuật viên yêu cầu phụ tùng. Yêu cầu mới chỉ ghi nhận nhu cầu và chưa làm giảm tồn kho. Nhân viên kho không sửa số lượng kỹ thuật viên đã yêu cầu: nếu đã giao đúng số lượng thực tế thì xác nhận cấp để trừ kho đúng một lần; nếu không thể cấp thì chọn Không thể cấp và nhập lý do. Chủ sở hữu được xem để giám sát nhưng không xác nhận hàng ra hoặc vào kho thay Nhân viên kho.",
                List.of("Mở Yêu cầu phụ tùng", "Ưu tiên các yêu cầu đang chờ cấp", "Đối chiếu mã phụ tùng, số lượng và phiếu công việc", "Khi đã giao đủ đúng số lượng, bấm Xác nhận cấp", "Nếu không thể cấp, chọn Không thể cấp và nhập lý do", "Theo dõi mục Vật tư đang do kỹ thuật viên giữ để nhận hoàn trả; dùng Lịch sử biến động để kiểm tra các lần cấp và hoàn trả"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicInventory() {
        return new AiHelpKnowledgeBase.HelpTopic("Kho phụ tùng", "/inventory", List.of("OWNER", "WAREHOUSE_STAFF"),
                List.of("kho", "phu tung", "ton kho", "nhap kho", "het ton", "inventory", "sku", "muc dat hang", "nguong ton toi thieu", "reorder level"),
                "Dùng để quản lý danh mục phụ tùng và tồn hiện tại. Ngưỡng tồn tối thiểu là mốc cảnh báo tồn thấp, không phải số lượng đặt mua. Chủ sở hữu và Nhân viên kho có thể quản lý danh mục, nhập kho và chỉnh ngưỡng. Nếu ngưỡng mới khiến tồn hiện tại ở mức thấp, Nhân viên kho nhận cảnh báo; Chủ sở hữu theo dõi tình hình trực tiếp tại các màn hình kho.",
                List.of("Mở menu Kho phụ tùng", "Tìm phụ tùng theo mã hoặc tên", "Kiểm tra tồn hiện tại và ngưỡng tồn tối thiểu", "Tạo hoặc cập nhật phụ tùng, nhập kho theo quyền", "Dùng Sửa ngưỡng khi cần thay đổi mốc cảnh báo", "Nếu tồn chạm hoặc thấp hơn ngưỡng, kiểm tra cảnh báo tồn thấp", "Dùng Kiểm kê tồn kho hoặc Lịch sử biến động khi cần đối soát"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicInventoryStocktake() {
        return new AiHelpKnowledgeBase.HelpTopic("Kiểm kê tồn kho", "/inventory-stocktake", List.of("OWNER", "WAREHOUSE_STAFF"),
                List.of("kiem ke", "stocktake", "doi soat ton", "chenh lech ton", "ton thuc te", "dieu chinh ton", "adjustment", "thong bao kiem ke", "ai nhan thong bao"),
                "Dùng để đối chiếu số lượng đang ghi nhận với số đếm thực tế. Nếu có chênh lệch, hệ thống ghi nhận điều chỉnh tăng hoặc giảm kèm lý do. Chủ sở hữu nhận thông báo về chênh lệch; nếu tồn sau kiểm kê chạm hoặc thấp hơn ngưỡng tối thiểu thì Nhân viên kho nhận cảnh báo tồn thấp. Yêu cầu phụ tùng được xử lý riêng tại màn Yêu cầu phụ tùng.",
                List.of("Mở menu Kiểm kê tồn kho", "Tìm phụ tùng cần kiểm kê theo mã hoặc tên", "Bấm Kiểm kê", "Nhập số lượng thực tế và lý do", "Xác nhận điều chỉnh", "Chủ sở hữu nhận thông báo nếu có chênh lệch", "Nếu tồn thấp, Nhân viên kho nhận cảnh báo", "Mở Lịch sử biến động để kiểm tra lần điều chỉnh và tồn sau"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicInventoryMovements() {
        return new AiHelpKnowledgeBase.HelpTopic("Lịch sử biến động kho", "/inventory-movements", List.of("OWNER", "WAREHOUSE_STAFF"),
                List.of("lich su bien dong kho", "lich su bien dong", "bien dong kho", "giao dich kho", "inventory movement", "inventory transaction", "ledger", "receive", "consume", "adjustment"),
                "Dùng để kiểm tra các thay đổi tồn kho như nhập kho, cấp phụ tùng cho phiếu công việc, hoàn trả và điều chỉnh kiểm kê. Mỗi dòng hiển thị người thực hiện, thời gian, số lượng, tồn sau giao dịch và mã phiếu công việc khi có. Với lần cấp hoặc hoàn trả, hệ thống lưu tên kỹ thuật viên nhận hoặc trả tại thời điểm phát sinh để lịch sử luôn rõ ràng.",
                List.of("Mở menu Lịch sử biến động", "Tìm theo mã phụ tùng, tên, mã phiếu, kỹ thuật viên nhận hoặc trả, người thực hiện hoặc ghi chú", "Lọc theo loại giao dịch hoặc khoảng ngày", "Đối chiếu số lượng thay đổi, kỹ thuật viên nhận hoặc trả và tồn sau", "Đây là màn hình tra cứu; thao tác hoàn trả được thực hiện tại Yêu cầu phụ tùng → Vật tư đang do kỹ thuật viên giữ"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicInventoryReturns() {
        return new AiHelpKnowledgeBase.HelpTopic("Hoàn trả phụ tùng theo phiếu công việc", "/part-requests", List.of("OWNER", "WAREHOUSE_STAFF"),
                List.of("hoan tra", "tra lai phu tung", "return part", "part return", "phu tung chua dung", "khong dung het"),
                "Dùng mục Vật tư đang do kỹ thuật viên giữ để Nhân viên kho xác nhận nhận lại phần phụ tùng đã cấp nhưng kỹ thuật viên chưa dùng hết. Hệ thống tự tính số lượng còn đang giữ dựa trên lượng đã cấp, đã sử dụng và đã hoàn trả. Hoàn trả hợp lệ vẫn được phép sau khi phiếu công việc đã đóng và không làm mở lại phiếu.",
                List.of("Mở menu Yêu cầu phụ tùng", "Xuống mục Vật tư đang do kỹ thuật viên giữ", "Tìm theo phiếu công việc, kỹ thuật viên hoặc phụ tùng", "Bấm Hoàn trả ở đúng dòng còn số lượng đang giữ", "Kiểm tra số lượng tối đa và chỉ xác nhận sau khi kho đã nhận hàng thực tế", "Nhập số lượng và lý do rồi xác nhận; tồn kho tăng tương ứng", "Khi hoàn hết, dòng tự biến mất; mở Lịch sử biến động để kiểm tra lần hoàn trả"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicPayments() {
        return new AiHelpKnowledgeBase.HelpTopic("Xử lý thanh toán", "/payments", List.of("OWNER", "CUSTOMER_SERVICE"),
                List.of("thanh toan", "chuyen khoan", "tien mat", "thanh toan tai quay", "doi soat", "bien nhan", "payment", "settled"),
                "Danh sách Xử lý thanh toán cho biết phiếu nào chưa thanh toán, khách đã báo chuyển khoản, tiền mặt đang do kỹ thuật viên giữ, khách hẹn thanh toán tại quầy hoặc đã được đối soát. Với khoản đang chờ, Chăm sóc khách hàng bấm Đối soát thanh toán để mở đúng phiếu công việc, kiểm tra chi phí khách đã xác nhận rồi mới ghi nhận tiền thực nhận. Nếu khách hẹn thanh toán tại quầy, chỉ xác nhận sau khi trực tiếp nhận tiền mặt hoặc kiểm tra chuyển khoản đã vào tài khoản công ty. Sau khi đối soát, Chăm sóc khách hàng phát hành biên nhận và đóng phiếu. Chủ sở hữu chỉ xem để giám sát và cấu hình thông tin nhận thanh toán.",
                List.of("Mở Xử lý thanh toán", "Lọc các khoản cần xử lý", "Bấm Đối soát thanh toán để mở đúng phiếu công việc và mục Thanh toán", "Kiểm tra chi phí khách đã xác nhận", "Nếu chuyển khoản, đối chiếu tiền thực tế vào tài khoản công ty", "Nếu tiền mặt, nhận và đếm tiền kỹ thuật viên bàn giao", "Nếu khách hẹn tại quầy, thu tiền trực tiếp rồi chọn đúng phương thức và xác nhận lại", "Xác nhận đã đối soát", "Phát hành hoặc tải biên nhận", "Đóng phiếu"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicPaymentSettings() {
        return new AiHelpKnowledgeBase.HelpTopic("Thiết lập thanh toán", "/payment-settings", List.of("OWNER"),
                List.of("tai khoan thanh toan", "tai khoan ngan hang", "ngan hang cong ty", "qr cong ty", "payment settings", "bank account", "bank qr"),
                "Chỉ Chủ sở hữu cấu hình tài khoản ngân hàng và QR nhận tiền của công ty. Đây là thông tin kỹ thuật viên chỉ được xem trong phiếu công việc để hướng dẫn khách chuyển khoản; kỹ thuật viên, chăm sóc khách hàng và các vai trò khác không được tự thay đổi tài khoản nhận tiền.",
                List.of("Mở Thiết lập thanh toán", "Nhập ngân hàng, chủ tài khoản và số tài khoản của công ty", "Gắn QR công ty nếu có", "Lưu cấu hình", "Kiểm tra lại thông tin hiển thị cho kỹ thuật viên trước khi dùng tại hiện trường"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicWorkOrderHistory() {
        return new AiHelpKnowledgeBase.HelpTopic("Lịch sử phiếu công việc", "/work-order-history", List.of("OWNER", "DISPATCHER", "CUSTOMER_SERVICE", "TECHNICIAN"),
                List.of("lich su phieu", "lich su phieu da dong", "lich su work order", "work order history", "phieu da dong", "phieu da huy", "cho hoan tat ho so", "da doi soat chua dong", "settled chua dong", "chua dong phieu", "tien trinh thanh toan", "tra lich su"),
                "Dùng để tra các phiếu đã đóng, đã hủy và cả phiếu khách đã xác nhận, tiền đã đối soát nhưng Chăm sóc khách hàng chưa hoàn tất bước đóng phiếu. Trường hợp chưa đóng hiển thị Chờ hoàn tất hồ sơ để người phụ trách quay lại Xử lý thanh toán, phát hành hoặc tải biên nhận nếu cần rồi Đóng phiếu. Mục Tiến trình trình bày đầy đủ các mốc điều phối, phụ tùng, hoàn thành, khách xác nhận, thanh toán, biên nhận và đóng phiếu.",
                List.of("Mở Lịch sử phiếu công việc", "Tìm theo mã phiếu, khách hàng hoặc nội dung", "Mở phiếu cần kiểm tra", "Xem Tiến trình để đối chiếu các mốc nghiệp vụ", "Dùng Nhật ký hệ thống hoặc Lịch sử biến động khi cần kiểm tra chi tiết"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicChannelsManage() {
        return new AiHelpKnowledgeBase.HelpTopic("Kênh tiếp nhận", "/service-channels", List.of("OWNER"),
                List.of("kenh", "zalo", "website", "dien thoai", "channel", "nguon tiep nhan"),
                "Dùng để chuẩn hóa nguồn tiếp nhận yêu cầu như điện thoại, trang web, Zalo hoặc nội bộ.",
                List.of("Mở menu Kênh tiếp nhận", "Kiểm tra các kênh đang hoạt động", "Nếu giao diện cho phép, thêm hoặc chỉnh sửa kênh", "Ưu tiên tắt kênh không còn dùng thay vì xóa dữ liệu đã phát sinh"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicChannelsReadOnly() {
        return new AiHelpKnowledgeBase.HelpTopic("Kênh tiếp nhận", "/service-channels", List.of("CUSTOMER_SERVICE"),
                List.of("kenh", "zalo", "website", "dien thoai", "channel", "nguon tiep nhan"),
                "Chăm sóc khách hàng được xem danh sách kênh tiếp nhận để chọn đúng nguồn khi tạo yêu cầu; trang này là chỉ đọc với vai trò hiện tại.",
                List.of("Mở menu Kênh tiếp nhận", "Xem tên, mô tả và trạng thái kênh", "Dùng kênh phù hợp khi tiếp nhận yêu cầu", "Nếu cần thay đổi cấu hình kênh, chuyển yêu cầu cho vai trò có quyền quản trị"));
    }

    static AiHelpKnowledgeBase.HelpTopic topicUsers() {
        return new AiHelpKnowledgeBase.HelpTopic("Người dùng", "/users", List.of("OWNER"),
                List.of(
                        "nguoi dung", "tai khoan", "phan quyen", "role", "roles", "tao user",
                        "user management", "user account", "new user account", "create user", "create a user",
                        "create new user", "create a new user", "create account", "create an account",
                        "assign role", "assign roles", "permission", "permissions"
                ),
                "Chủ sở hữu dùng trang này để quản lý tài khoản, vai trò và trạng thái truy cập của nhân viên. Bộ lọc Tất cả trạng thái, Hoạt động và Tạm ngưng kết hợp với ô tìm kiếm để rà nhanh tài khoản. Tên đăng nhập và vai trò được giữ cố định sau khi tạo để bảo toàn lịch sử và các dữ liệu liên quan; Chủ sở hữu vẫn có thể đổi họ tên, mật khẩu và trạng thái theo quy tắc bảo vệ tài khoản.",
                List.of("Mở menu Người dùng", "Dùng ô tìm kiếm và bộ lọc trạng thái để tìm tài khoản", "Tạo hoặc cập nhật tài khoản theo chính sách nội bộ", "Chọn đúng vai trò khi tạo mới", "Tạm ngưng tài khoản khi không còn quyền truy cập", "Không tắt chính tài khoản đang đăng nhập hoặc Chủ sở hữu cuối cùng"));
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
                "Nếu bấm Lưu hoặc Hoàn thành khi còn thiếu dữ liệu bắt buộc, trường cần bổ sung sẽ được đánh dấu, trang tự chuyển tới lỗi đầu tiên và hiện cảnh báo ngắn. Nếu thao tác đã gửi nhưng hệ thống xử lý chưa thành công, giao diện hiển thị thông báo phù hợp và nút Thử lại ở các màn hình tải dữ liệu chính.",
                List.of("Đọc cảnh báo vừa hiện", "Tìm trường có dấu bắt buộc hoặc viền lỗi", "Bổ sung dữ liệu theo thông báo", "Bấm lại nút hành động có tên cụ thể như Hoàn thành công việc hoặc Lưu thay đổi", "Nếu là lỗi tải dữ liệu, bấm Thử lại; nếu vẫn lỗi, liên hệ người phụ trách hệ thống"));
    }

    static List<String> notificationKeywords() {
        return List.of("thong bao", "thong bao nao", "chuong thong bao", "notification", "bell notification", "chua doc", "da doc", "danh dau chua doc", "mark unread", "mark as unread", "bam nham da doc");
    }

    static List<String> notificationSteps() {
        return List.of("Đọc tiêu đề để biết việc cần chú ý", "Đọc dòng mô tả để biết bước tiếp theo", "Bấm một thông báo chưa đọc để đánh dấu đã đọc", "Nếu lỡ đánh dấu đã đọc, bấm nút ngoài cùng bên phải để chuyển lại Chưa đọc", "Dùng mã phiếu hoặc mã phụ tùng trong thông báo để tìm đúng dữ liệu khi cần");
    }

    static AiHelpKnowledgeBase.HelpTopic topicOwnerNotifications() {
        return new AiHelpKnowledgeBase.HelpTopic("Thông báo", "/", List.of("OWNER"), notificationKeywords(),
                "Chuông của Chủ sở hữu chỉ dành cho kết quả hoặc ngoại lệ cần giám sát, như phiếu công việc đã đóng, đã hủy hoặc kiểm kê có chênh lệch cần chú ý. Các việc vận hành thường ngày như mở lại phiếu, chờ phụ tùng, yêu cầu phụ tùng hoặc tồn kho thấp được theo dõi tại các màn hình nghiệp vụ để tránh làm danh sách thông báo quá tải.",
                notificationSteps());
    }

    static AiHelpKnowledgeBase.HelpTopic topicDispatcherNotifications() {
        return new AiHelpKnowledgeBase.HelpTopic("Thông báo", "/", List.of("DISPATCHER"), notificationKeywords(),
                "Chuông của Điều phối viên tập trung vào việc cần điều phối: phiếu cần phân công, đang chờ phụ tùng, cần xử lý lại hoặc quá hạn thực hiện. Hãy mở Phiếu công việc hoặc Lịch điều phối để xử lý. Các thay đổi thông thường của khách hàng, thiết bị và nhật ký hệ thống không được đưa vào chuông thông báo.",
                notificationSteps());
    }

    static AiHelpKnowledgeBase.HelpTopic topicCustomerServiceNotifications() {
        return new AiHelpKnowledgeBase.HelpTopic("Thông báo", "/", List.of("CUSTOMER_SERVICE"), notificationKeywords(),
                "Chuông của Chăm sóc khách hàng tập trung vào việc cần liên hệ/hậu xử lý khách: phiếu công việc vừa hoàn thành, bị mở lại/hủy bởi vai trò khác, quá hạn kéo dài qua thời gian theo dõi, hoặc phát sinh bàn giao thanh toán cần xử lý. Khi kỹ thuật viên báo khách đã chuyển khoản, đang giữ tiền mặt hoặc khách hẹn thanh toán tại quầy, chăm sóc khách hàng nhận thông báo tương ứng và xử lý chi tiết trong màn Xử lý thanh toán; chuông chỉ nhắc sự kiện cần hành động, không thay thế màn nghiệp vụ này.",
                notificationSteps());
    }

    static AiHelpKnowledgeBase.HelpTopic topicTechnicianNotifications() {
        return new AiHelpKnowledgeBase.HelpTopic("Thông báo", "/", List.of("TECHNICIAN"), notificationKeywords(),
                "Chuông của Kỹ thuật viên chỉ nhắc các công việc được giao cho chính mình: phân công mới, đổi lịch/chuyển giao, mở lại, hủy, đóng hoặc quá lịch. Kỹ thuật viên không nhận cảnh báo quản trị kho, đối soát thanh toán hoặc quản trị người dùng.",
                notificationSteps());
    }

    static AiHelpKnowledgeBase.HelpTopic topicWarehouseNotifications() {
        return new AiHelpKnowledgeBase.HelpTopic("Thông báo", "/part-requests", List.of("WAREHOUSE_STAFF"), notificationKeywords(),
                "Chuông của Nhân viên kho tập trung vào yêu cầu phụ tùng mới cần xử lý và cảnh báo tồn thấp. Yêu cầu mới được xử lý tại Yêu cầu phụ tùng; kiểm kê và lịch sử tồn kho nằm trong nhóm Kho & vật tư. Nhân viên kho không nhận thông báo về tiến độ phiếu công việc, đối soát thanh toán hoặc quản trị người dùng.",
                notificationSteps());
    }
}
