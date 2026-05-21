package hoangtlt.services;

import hoangtlt.config.AppConstants;
import hoangtlt.entities.*;
import hoangtlt.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final DiningTableRepository tableRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository variantRepository;
    private final AreaRepository areaRepository;
    private final RestaurantSettingService settingService;

    public OrderService(OrderRepository orderRepository, DiningTableRepository tableRepository,
            ProductRepository productRepository, UserRepository userRepository,
            ProductVariantRepository variantRepository, AreaRepository areaRepository,
            RestaurantSettingService settingService) {
        this.orderRepository = orderRepository;
        this.tableRepository = tableRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.variantRepository = variantRepository;
        this.areaRepository = areaRepository;
        this.settingService = settingService;
    }

    public Optional<SaleOrder> getActiveOrder(Long tableId) {
        return orderRepository.findByTableIdAndStatus(tableId, SaleOrder.OrderStatus.PENDING);
    }

    public Optional<SaleOrder> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public DiningTable getTable(Long tableId) {
        return tableRepository.findById(tableId).orElseThrow();
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Transactional
    public DiningTable getOrCreateTakeawayTable() {
        String takeawayAreaName = AppConstants.TAKEAWAY_AREA_NAME;
        Area area = areaRepository.findByName(takeawayAreaName)
                .orElseGet(() -> {
                    Area newArea = new Area();
                    newArea.setName(takeawayAreaName);
                    return areaRepository.save(newArea);
                });

        // Tìm bàn trống trong khu vực mang về
        List<DiningTable> takeawayTables = tableRepository.findByAreaId(area.getId());
        for (DiningTable table : takeawayTables) {
            if (table.getStatus() == DiningTable.TableStatus.AVAILABLE) {
                // Kiểm tra xem có order PENDING nào thực sự đang dùng bàn này không
                if (orderRepository.findByTableIdAndStatus(table.getId(), SaleOrder.OrderStatus.PENDING).isEmpty()) {
                    return table;
                }
            }
        }

        // Nếu không có bàn trống, tạo thêm bàn mới
        DiningTable newTable = new DiningTable();
        newTable.setTableNumber("Mang về #" + (takeawayTables.size() + 1));
        newTable.setArea(area);
        newTable.setStatus(DiningTable.TableStatus.AVAILABLE);
        return tableRepository.save(newTable);
    }

    @Transactional
    public SaleOrder getOrCreateActiveOrder(Long tableId, String username) {
        // Sử dụng Lock để tránh Race Condition (Fix bug 3.1)
        return orderRepository.findByTableIdAndStatusWithLock(tableId, SaleOrder.OrderStatus.PENDING)
                .orElseGet(() -> {
                    DiningTable table = tableRepository.findById(tableId)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn số " + tableId));
                    User staff = userRepository.findByUsername(username).orElse(null);
                    if (staff == null) {
                        throw new RuntimeException(
                                "Phiên đăng nhập không hợp lệ hoặc tài khoản đã bị xóa. Vui lòng đăng nhập lại!");
                    }

                    // Re-check status inside transaction
                    if (table.getStatus() == DiningTable.TableStatus.OCCUPIED) {
                        // This case could happen if another thread created an order just before the
                        // lock
                        return orderRepository.findByTableIdAndStatus(tableId, SaleOrder.OrderStatus.PENDING)
                                .orElseThrow();
                    }

                    SaleOrder order = new SaleOrder();
                    order.setTable(table);
                    order.setStaff(staff);
                    order.setStatus(SaleOrder.OrderStatus.PENDING);
                    order.setItems(new ArrayList<>());
                    return orderRepository.save(order);
                });
    }

    @Transactional
    public void addProductToOrder(Long orderId, Long productId, Long variantId, int quantity, String note) {
        SaleOrder order = orderRepository.findById(orderId).orElseThrow();
        Product product = productRepository.findById(productId).orElseThrow();

        BigDecimal itemPrice = product.getPrice();
        String vName = null;

        if (variantId != null) {
            ProductVariant variant = variantRepository.findById(variantId).orElseThrow();
            itemPrice = variant.getPrice();
            vName = (variant.getName() != null && !variant.getName().trim().isEmpty())
                    ? variant.getName()
                    : String.format("%,.0fđ", itemPrice); // Fix thread-safety (Fix bug 2.3)
        }

        // When adding first item, set table to OCCUPIED
        if (order.getItems().isEmpty()) {
            DiningTable table = order.getTable();
            table.setStatus(DiningTable.TableStatus.OCCUPIED);
            tableRepository.save(table);
        }

        final String finalVName = vName;
        final String finalNote = (note == null) ? "" : note.trim();

        Optional<OrderItem> existingItem = order.getItems().stream()
                .filter(item -> {
                    if (item.isPrinted())
                        return false;
                    boolean sameProduct = item.getProduct().getId().equals(productId);
                    boolean sameVariant = (finalVName == null && item.getVariantName() == null) ||
                            (finalVName != null && finalVName.equals(item.getVariantName()));
                    String itemNote = (item.getNote() == null) ? "" : item.getNote().trim();
                    boolean sameNote = finalNote.equals(itemNote);

                    return sameProduct && sameVariant && sameNote;
                })
                .findFirst();

        if (existingItem.isPresent()) {
            OrderItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            item.setSubTotal(itemPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
        } else {
            OrderItem newItem = new OrderItem();
            newItem.setOrder(order);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            newItem.setPrice(itemPrice);
            newItem.setSubTotal(itemPrice.multiply(BigDecimal.valueOf(quantity)));
            newItem.setVariantName(finalVName);
            newItem.setNote(finalNote.isEmpty() ? null : finalNote);
            order.getItems().add(newItem);
        }

        updateOrderTotal(order);
        orderRepository.save(order);
    }

    @Transactional
    public void updateItemNote(Long orderId, Long productId, String vName, String note) {
        SaleOrder order = orderRepository.findById(orderId).orElseThrow();
        // Fix logic update note (Fix bug 3.2: include variantName check)
        Optional<OrderItem> itemOpt = order.getItems().stream()
                .filter(item -> {
                    boolean sameProduct = item.getProduct().getId().equals(productId);
                    boolean sameVariant = (vName == null && item.getVariantName() == null) ||
                            (vName != null && vName.equals(item.getVariantName()));
                    return sameProduct && sameVariant;
                })
                .findFirst();

        if (itemOpt.isPresent()) {
            itemOpt.get().setNote(note);
            orderRepository.save(order);
        }
    }

    @Transactional
    public void removeProductFromOrder(Long orderId, Long orderItemId) {
        SaleOrder order = orderRepository.findById(orderId).orElseThrow();
        order.getItems().removeIf(item -> item.getId().equals(orderItemId));

        if (order.getItems().isEmpty()) {
            DiningTable table = order.getTable();
            table.setStatus(DiningTable.TableStatus.AVAILABLE);
            tableRepository.save(table);
            orderRepository.delete(order);
        } else {
            updateOrderTotal(order);
            orderRepository.save(order);
        }
    }

    @Transactional
    public void updateItemQuantity(Long orderId, Long orderItemId, int delta) {
        SaleOrder order = orderRepository.findById(orderId).orElseThrow();
        Optional<OrderItem> itemOpt = order.getItems().stream()
                .filter(item -> item.getId().equals(orderItemId))
                .findFirst();

        if (itemOpt.isPresent()) {
            OrderItem item = itemOpt.get();
            int newQty = item.getQuantity() + delta;
            if (newQty > 0) {
                item.setQuantity(newQty);
                item.setSubTotal(item.getPrice().multiply(BigDecimal.valueOf(newQty)));
            } else {
                order.getItems().remove(item);
            }
        }

        if (order.getItems().isEmpty()) {
            DiningTable table = order.getTable();
            table.setStatus(DiningTable.TableStatus.AVAILABLE);
            tableRepository.save(table);
            orderRepository.delete(order);
        } else {
            updateOrderTotal(order);
            orderRepository.save(order);
        }
    }

    @Transactional
    public void deleteOrderIfEmpty(Long orderId) {
        SaleOrder order = orderRepository.findById(orderId).orElseThrow();
        if (order.getItems() == null || order.getItems().isEmpty()) {
            DiningTable table = order.getTable();
            table.setStatus(DiningTable.TableStatus.AVAILABLE);
            tableRepository.save(table);
            orderRepository.delete(order);
        }
    }

    @Transactional
    public void checkoutOrder(Long orderId, SaleOrder.PaymentMethod paymentMethod) {
        SaleOrder order = orderRepository.findById(orderId).orElseThrow();

        // Guard check for PAID status (Fix bug 3.3)
        if (order.getStatus() == SaleOrder.OrderStatus.PAID) {
            log.warn("Order {} is already paid. Skipping double checkout.", orderId);
            return;
        }

        order.setStatus(SaleOrder.OrderStatus.PAID);
        order.setPaymentMethod(paymentMethod);

        // Calculate final amount based on VAT / Discount settings
        RestaurantSetting settings = settingService.getSettings();
        BigDecimal finalTotal = order.getTotalAmount();

        if (settings.getEnableDiscount() && settings.getDiscountPercentage() > 0) {
            BigDecimal discountAmt = finalTotal.multiply(BigDecimal.valueOf(settings.getDiscountPercentage()))
                    .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
            finalTotal = finalTotal.subtract(discountAmt);
        } else if (settings.isEnableVat() && settings.getVatPercentage() > 0) {
            BigDecimal vatAmt = finalTotal.multiply(BigDecimal.valueOf(settings.getVatPercentage()))
                    .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
            finalTotal = finalTotal.add(vatAmt);
        }
        order.setTotalAmount(finalTotal);

        // Cập nhật số lượng đã bán cho từng món
        for (hoangtlt.entities.OrderItem item : order.getItems()) {
            hoangtlt.entities.Product product = item.getProduct();
            if (product != null) {
                product.setSoldQuantity(product.getSoldQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        }

        // Update table status to AVAILABLE
        DiningTable table = order.getTable();
        table.setStatus(DiningTable.TableStatus.AVAILABLE);
        tableRepository.save(table);

        orderRepository.save(order);
    }

    @Transactional
    public void clearTable(Long tableId) {
        DiningTable table = tableRepository.findById(tableId).orElseThrow();

        // Find pending order
        orderRepository.findByTableIdAndStatus(tableId, SaleOrder.OrderStatus.PENDING)
                .ifPresent(order -> {
                    orderRepository.delete(order);
                });

        // Update table status
        table.setStatus(DiningTable.TableStatus.AVAILABLE);
        tableRepository.save(table);
    }

    public org.springframework.data.domain.Page<SaleOrder> getPaginatedOrders(int page, int size, String period, java.time.LocalDate date) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("id").descending());

        if (period == null || period.isEmpty())
            period = "WEEK";

        if ("DAY".equalsIgnoreCase(period)) {
            java.time.LocalDate targetDate = (date != null) ? date : java.time.LocalDate.now();
            LocalDateTime startDate = targetDate.atStartOfDay();
            LocalDateTime endDate = targetDate.plusDays(1).atStartOfDay();
            return orderRepository.findByCreatedAtBetween(startDate, endDate, pageable);
        }

        LocalDateTime startDate = null;
        switch (period.toUpperCase()) {
            case "WEEK":
                startDate = LocalDateTime.now().minusWeeks(1);
                break;
            case "MONTH":
                startDate = LocalDateTime.now().minusMonths(1);
                break;
            case "YEAR":
                startDate = LocalDateTime.now().minusYears(1);
                break;
            case "ALL":
                return orderRepository.findAll(pageable);
            default:
                startDate = LocalDateTime.now().minusWeeks(1);
        }

        return orderRepository.findByCreatedAtAfter(startDate, pageable);
    }

    public List<SaleOrder> getAllOrders() {
        // Fix sorting efficiency (Fix bug 4.2)
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public SaleOrder getOrderDetail(Long id) {
        SaleOrder order = orderRepository.findById(id).orElseThrow();
        // Force load items and product names for JSON
        order.getItems().size();
        for (OrderItem item : order.getItems()) {
            item.getProduct().getName();
        }
        return order;
    }

    @Transactional
    public void purgeOrders(LocalDateTime beforeDate) {
        orderRepository.deleteByCreatedAtBefore(beforeDate);
    }

    public org.springframework.data.domain.Page<SaleOrder> getStaffOrderHistory(Long staffId, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return orderRepository.findByStaffIdAndCreatedAtAfter(staffId, sevenDaysAgo, pageable);
    }

    private void updateOrderTotal(SaleOrder order) {
        BigDecimal total = order.getItems().stream()
                .map(OrderItem::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);
    }

    @Transactional
    public void markItemsAsPrinted(Long orderId, List<OrderItem> itemsToMark) {
        SaleOrder order = orderRepository.findById(orderId).orElseThrow();
        List<Long> itemIds = itemsToMark.stream().map(OrderItem::getId).collect(java.util.stream.Collectors.toList());
        boolean changed = false;

        // Đánh dấu các món vừa báo bếp là Đã in
        for (OrderItem item : order.getItems()) {
            if (itemIds.contains(item.getId()) && !item.isPrinted()) {
                item.setPrinted(true);
                changed = true;
            }
        }

        // Tự động GỘP các món Đã in nếu giống nhau (cùng SP, Biến thể, Giá, Ghi chú)
        java.util.Map<String, OrderItem> grouped = new java.util.LinkedHashMap<>();
        List<OrderItem> toRemove = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            if (!item.isPrinted())
                continue; // Không gộp các món chưa in (MỚI)

            String key = item.getProduct().getId() + "_" +
                    (item.getVariantName() != null ? item.getVariantName() : "") + "_" +
                    item.getPrice().toString() + "_" +
                    (item.getNote() != null ? item.getNote() : "");

            if (grouped.containsKey(key)) {
                OrderItem existing = grouped.get(key);
                existing.setQuantity(existing.getQuantity() + item.getQuantity());
                existing.setSubTotal(existing.getSubTotal().add(item.getSubTotal()));
                toRemove.add(item);
                changed = true;
            } else {
                grouped.put(key, item);
            }
        }

        if (!toRemove.isEmpty()) {
            order.getItems().removeAll(toRemove);
        }

        if (changed) {
            orderRepository.save(order);
        }
    }
}
