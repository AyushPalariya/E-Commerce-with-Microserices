package com.example.InventoryService.Service;

import com.example.InventoryService.Dto.ColorDto;
import com.example.InventoryService.Dto.Inventory;
import com.example.InventoryService.Dto.InventoryItem;
import com.example.InventoryService.Dto.Product;
import com.example.InventoryService.Dto.SizeDto;
import com.example.InventoryService.Entities.Colors;
import com.example.InventoryService.Entities.Gender;
import com.example.InventoryService.Entities.InventoryEntity;
import com.example.InventoryService.Entities.ReservationEntity;
import com.example.InventoryService.Entities.Sizes;
import com.example.InventoryService.Entities.Status;
import com.example.InventoryService.Global.BadRequestExceptionClass;
import com.example.InventoryService.Repository.InventoryRepo;
import com.example.InventoryService.Repository.ProductClientInterface;
import com.example.InventoryService.Repository.ReservationRepo;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InventoryService {
    @Autowired
    private ReservationRepo reservationRepo;
    @Autowired
    private InventoryRepo inventoryRepo;
    @Autowired
    private ProductClientInterface productClient;

    @Transactional
    @CacheEvict(value = {"allInventoryV2", "inventoryV2"}, allEntries = true)
    public List<Inventory> add(List<Inventory> inventories) {
        validateNewInventories(inventories);
        List<InventoryEntity> entities = inventories.stream().map(this::toEntity).toList();
        return toDtos(inventoryRepo.saveAll(entities));
    }

    private void validateNewInventories(List<Inventory> inventories) {
        if (inventories == null || inventories.isEmpty())
            throw new BadRequestExceptionClass("Inventory details are required!");

        List<Long> productIds = new ArrayList<>();
        List<Product> productsToVerify = new ArrayList<>();
        for (Inventory inventory : inventories) {
            if (inventory == null || inventory.getName() == null || inventory.getName().isBlank())
                throw new BadRequestExceptionClass("Product name is required!");
            if (inventory.getProductId() == null || inventory.getProductId() <= 0)
                throw new BadRequestExceptionClass("productId must be positive!");
            if (inventory.getColors() == null || inventory.getColors().isEmpty())
                throw new BadRequestExceptionClass("At least one color is required!");

            Gender gender = parseGender(inventory.getGender());
            inventory.setGender(gender == null ? null : gender.name());
            productIds.add(inventory.getProductId());
            productsToVerify.add(new Product(inventory.getProductId(), inventory.getName()));
            validateColorDtos(inventory.getColors());
        }
        if (productIds.size() != productIds.stream().distinct().count())
            throw new BadRequestExceptionClass("Duplicate product IDs are present!");
        if (!inventoryRepo.findAllByProductIdIn(productIds).isEmpty())
            throw new BadRequestExceptionClass("ProductId already present!");

        try {
            List<Product> verified = productClient.verifyProductIds(productsToVerify).getBody();
            if (verified == null || verified.size() != productsToVerify.size())
                throw new BadRequestExceptionClass("One or more products could not be verified!");
            Map<Long, String> namesById = verified.stream().collect(Collectors.toMap(
                    Product::getId, Product::getName, (first, ignored) -> first));
            for (Product submitted : productsToVerify) {
                if (!submitted.getName().equalsIgnoreCase(namesById.get(submitted.getId())))
                    throw new BadRequestExceptionClass("Product ID and name do not match: " + submitted.getId());
            }
        } catch (FeignException exception) {
            throw new BadRequestExceptionClass("Unable to verify products with ProductService");
        }
    }

    private void validateColorDtos(List<ColorDto> colors) {
        List<Long> colorIds = new ArrayList<>();
        for (ColorDto color : colors) {
            if (color == null || color.getColorId() == null || color.getColorId() <= 0)
                throw new BadRequestExceptionClass("Each color must have a positive colorId!");
            if (color.getColor() == null || color.getColor().isBlank())
                throw new BadRequestExceptionClass("Color name is required!");
            if (color.getSizes() == null || color.getSizes().isEmpty())
                throw new BadRequestExceptionClass("Each color must have at least one size!");
            colorIds.add(color.getColorId());

            List<Long> sizeIds = new ArrayList<>();
            for (SizeDto size : color.getSizes()) {
                if (size == null || size.getSizeId() == null || size.getSizeId() <= 0)
                    throw new BadRequestExceptionClass("Each size must have a positive sizeId!");
                if (size.getSize() == null || size.getSize().isBlank())
                    throw new BadRequestExceptionClass("Size name is required!");
                if (size.getTotalQuantity() == null || size.getTotalQuantity() <= 0)
                    throw new BadRequestExceptionClass("Size totalQuantity must be positive!");
                if (sizeIds.contains(size.getSizeId()))
                    throw new BadRequestExceptionClass("Duplicate sizeId in color: " + color.getColor());
                sizeIds.add(size.getSizeId());
            }
        }
        if (colorIds.size() != colorIds.stream().distinct().count())
            throw new BadRequestExceptionClass("Duplicate colorIds are present!");
    }

    private InventoryEntity toEntity(Inventory inventory) {
        InventoryEntity entity = new InventoryEntity();
        entity.setProductId(inventory.getProductId());
        entity.setName(inventory.getName());
        entity.setGender(parseGender(inventory.getGender()));
        List<Colors> colors = inventory.getColors().stream().map(colorDto -> {
            Colors color = new Colors();
            color.setId(colorDto.getColorId());
            color.setColor(colorDto.getColor());
            color.setImages(colorDto.getImages());
            color.setInventory(entity);
            List<Sizes> sizes = colorDto.getSizes().stream().map(sizeDto -> {
                Sizes size = new Sizes();
                size.setSizeId(sizeDto.getSizeId());
                size.setSize(sizeDto.getSize());
                size.setTotalQuantity(sizeDto.getTotalQuantity());
                size.setAvailableQuantity(sizeDto.getTotalQuantity());
                size.setReservedQuantity(0L);
                size.setSoldQuantity(0L);
                size.setColors(color);
                return size;
            }).toList();
            color.setSizes(new ArrayList<>(sizes));
            return color;
        }).toList();
        entity.setColors(new ArrayList<>(colors));
        return entity;
    }

    @Transactional
    @CacheEvict(value = {"allInventoryV2", "inventoryV2"}, allEntries = true)
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(id -> id == null || id <= 0))
            throw new BadRequestExceptionClass("Valid inventory IDs are required!");
        inventoryRepo.deleteAllById(ids.stream().distinct().toList());
    }

    @Transactional
    @CacheEvict(value = {"allInventoryV2", "inventoryV2"}, allEntries = true)
    public void reserved(Long orderId, List<InventoryItem> items) {
        if (orderId == null || orderId <= 0) throw new BadRequestExceptionClass("orderId must be positive!");
        validateVariantItems(items);
        List<Long> productIds = items.stream().map(InventoryItem::getProductId).distinct().toList();
        List<InventoryEntity> inventories = inventoryRepo.findAllByProductIdIn(productIds);
        if (inventories.size() != productIds.size())
            throw new BadRequestExceptionClass("One or more inventory records were not found!");

        Map<Long, InventoryEntity> byProductId = inventories.stream().collect(Collectors.toMap(
                InventoryEntity::getProductId, Function.identity()));
        for (InventoryItem item : items) {
            Sizes size = findSize(byProductId.get(item.getProductId()), item.getColor(), item.getSize());
            long available = available(size);
            if (item.getQuantities() > available)
                throw new BadRequestExceptionClass("Insufficient stock for productId: " + item.getProductId()
                        + ", color: " + item.getColor() + ", size: " + item.getSize());
            size.setAvailableQuantity(available - item.getQuantities());
            size.setReservedQuantity(valueOrZero(size.getReservedQuantity()) + item.getQuantities());
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
        List<ReservationEntity> reservations = items.stream()
                .map(item -> new ReservationEntity(orderId, item.getProductId(), item.getSize(), item.getColor(),
                        item.getQuantities(), Status.RESERVED, expiresAt))
                .toList();
        inventoryRepo.saveAll(inventories);
        reservationRepo.saveAll(reservations);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "allInventoryV2", key = "'all'")
    public List<Inventory> getAllStock() {
        return toDtos(inventoryRepo.findAll());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "inventoryV2", key = "#productId")
    public Inventory getStock(Long productId) {
        if (productId == null || productId <= 0) throw new BadRequestExceptionClass("productId must be positive!");
        InventoryEntity entity = inventoryRepo.findByProductId(productId)
                .orElseThrow(() -> new BadRequestExceptionClass("Product not found with productId: " + productId));
        return toDto(entity);
    }

    @Transactional
    @CacheEvict(value = {"allInventoryV2", "inventoryV2"}, allEntries = true)
    public List<Inventory> updateInventory(List<InventoryItem> items) {
        validateVariantItems(items);
        List<Long> productIds = items.stream().map(InventoryItem::getProductId).distinct().toList();
        List<InventoryEntity> inventories = inventoryRepo.findAllByProductIdIn(productIds);
        if (inventories.size() != productIds.size())
            throw new BadRequestExceptionClass("One or more inventory records were not found!");
        Map<Long, InventoryEntity> byProductId = inventories.stream().collect(Collectors.toMap(
                InventoryEntity::getProductId, Function.identity()));
        for (InventoryItem item : items) {
            Sizes size = findSize(byProductId.get(item.getProductId()), item.getColor(), item.getSize());
            size.setTotalQuantity(valueOrZero(size.getTotalQuantity()) + item.getQuantities());
            size.setAvailableQuantity(available(size) + item.getQuantities());
        }
        inventoryRepo.saveAll(inventories);
        return inventories.stream().map(this::toDto).toList();
    }

    private void validateVariantItems(List<InventoryItem> items) {
        if (items == null || items.isEmpty()) throw new BadRequestExceptionClass("Inventory items are required!");
        for (InventoryItem item : items) {
            if (item == null || item.getProductId() == null || item.getProductId() <= 0)
                throw new BadRequestExceptionClass("productId must be positive!");
            if (item.getQuantities() == null || item.getQuantities() <= 0)
                throw new BadRequestExceptionClass("quantities must be positive!");
            if (item.getColor() == null || item.getColor().isBlank())
                throw new BadRequestExceptionClass("Color is required!");
            if (item.getSize() == null || item.getSize().isBlank())
                throw new BadRequestExceptionClass("Size is required!");
        }
    }

    private Sizes findSize(InventoryEntity inventory, String colorName, String sizeName) {
        if (inventory.getColors() == null) throw new BadRequestExceptionClass("No colors found for product!");
        Colors color = inventory.getColors().stream()
                .filter(candidate -> candidate.getColor() != null
                        && candidate.getColor().equalsIgnoreCase(colorName))
                .findFirst()
                .orElseThrow(() -> new BadRequestExceptionClass("Color not found: " + colorName));
        if (color.getSizes() == null) throw new BadRequestExceptionClass("No sizes found for color: " + colorName);
        return color.getSizes().stream()
                .filter(candidate -> candidate.getSize() != null
                        && candidate.getSize().equalsIgnoreCase(sizeName))
                .findFirst()
                .orElseThrow(() -> new BadRequestExceptionClass("Size not found: " + sizeName));
    }

    @Transactional(readOnly = true)
    public List<ReservationEntity> getReservedRecords() {
        return reservationRepo.findByStatus(Status.RESERVED);
    }

    @Transactional(readOnly = true)
    public List<ReservationEntity> getReleasedRecords() {
        return reservationRepo.findByStatus(Status.RELEASED);
    }

    @Transactional(readOnly = true)
    public List<ReservationEntity> getConfirmedRecords() {
        return reservationRepo.findByStatus(Status.CONFIRM);
    }

    @Transactional
    @CacheEvict(value = {"allInventoryV2", "inventoryV2"}, allEntries = true)
    public void release(Long orderId) {
        List<ReservationEntity> reservations = activeReservations(orderId, Status.RELEASED);
        if (!reservations.isEmpty()) releaseReservations(orderId, reservations, false);
    }

    @Transactional
    @CacheEvict(value = {"allInventoryV2", "inventoryV2"}, allEntries = true)
    public void confirm(Long orderId) {
        List<ReservationEntity> reservations = activeReservations(orderId, Status.CONFIRM);
        if (!reservations.isEmpty()) releaseReservations(orderId, reservations, true);
    }

    private void releaseReservations(Long orderId, List<ReservationEntity> reservations, boolean confirmed) {
        Map<Long, InventoryEntity> inventories = loadReservedInventories(orderId, reservations);
        for (ReservationEntity reservation : reservations) {
            InventoryEntity inventory = inventories.get(reservation.getProductId());
            Sizes size = findSize(inventory, reservation.getColor(), reservation.getSize());
            long reserved = valueOrZero(size.getReservedQuantity());
            if (reserved < reservation.getQuantities())
                throw new BadRequestExceptionClass("Reserved stock is inconsistent for productId: "
                        + reservation.getProductId());
            size.setReservedQuantity(reserved - reservation.getQuantities());
            if (confirmed) {
                size.setSoldQuantity(valueOrZero(size.getSoldQuantity()) + reservation.getQuantities());
                reservation.setStatus(Status.CONFIRM);
            } else {
                size.setAvailableQuantity(available(size) + reservation.getQuantities());
                reservation.setStatus(Status.RELEASED);
            }
        }
        inventoryRepo.saveAll(inventories.values());
        reservationRepo.saveAll(reservations);
    }

    @Transactional
    @CacheEvict(value = {"allInventoryV2", "inventoryV2"}, allEntries = true)
    public void markPaymentStarted(Long orderId) {
        List<ReservationEntity> reservations = activeReservations(orderId, Status.CONFIRM);
        if (reservations.isEmpty())
            throw new BadRequestExceptionClass("No active reservation found for orderId: " + orderId);
        LocalDateTime now = LocalDateTime.now();
        if (reservations.stream().anyMatch(reservation -> reservation.getExpiredAt() == null
                || !reservation.getExpiredAt().isAfter(now)))
            throw new BadRequestExceptionClass("Reservation has expired for orderId: " + orderId);
        reservations.forEach(reservation -> reservation.setPaymentStarted(true));
        reservationRepo.saveAll(reservations);
    }

    @Transactional
    @CacheEvict(value = {"allInventoryV2", "inventoryV2"}, allEntries = true)
    public void clearPaymentStarted(Long orderId) {
        if (orderId == null || orderId <= 0) throw new BadRequestExceptionClass("orderId must be positive!");
        List<ReservationEntity> reservations = reservationRepo.findByOrderIdAndStatus(orderId, Status.RESERVED);
        reservations.forEach(reservation -> reservation.setPaymentStarted(false));
        reservationRepo.saveAll(reservations);
    }

    @Transactional
    @CacheEvict(value = {"allInventoryV2", "inventoryV2"}, allEntries = true)
    @Scheduled(fixedDelayString = "${inventory.reservation-expiration-check-ms:30000}")
    public void releaseExpiredReservations() {
        List<ReservationEntity> expired = reservationRepo.findExpiredUnpaid(Status.RESERVED, LocalDateTime.now());
        if (expired.isEmpty()) return;
        Map<Long, List<ReservationEntity>> byOrder = expired.stream()
                .collect(Collectors.groupingBy(ReservationEntity::getOrderId));
        byOrder.forEach((orderId, orderReservations) -> releaseReservations(orderId, orderReservations, false));
    }

    private List<ReservationEntity> activeReservations(Long orderId, Status completedStatus) {
        if (orderId == null || orderId <= 0) throw new BadRequestExceptionClass("orderId must be positive!");
        List<ReservationEntity> reservations = reservationRepo.findByOrderIdAndStatus(orderId, Status.RESERVED);
        if (reservations.isEmpty()) {
            if (!reservationRepo.findByOrderIdAndStatus(orderId, completedStatus).isEmpty()) return List.of();
            throw new BadRequestExceptionClass("No active reservation found for orderId: " + orderId);
        }
        return reservations;
    }

    private Map<Long, InventoryEntity> loadReservedInventories(Long orderId, List<ReservationEntity> reservations) {
        List<Long> productIds = reservations.stream().map(ReservationEntity::getProductId).distinct().toList();
        List<InventoryEntity> found = inventoryRepo.findAllByProductIdIn(productIds);
        if (found.size() != productIds.size())
            throw new BadRequestExceptionClass("Inventory records missing for orderId: " + orderId);
        return found.stream().collect(Collectors.toMap(InventoryEntity::getProductId, Function.identity()));
    }

    private List<Inventory> toDtos(List<InventoryEntity> entities) {
        return entities.stream().map(this::toDto).toList();
    }

    private Inventory toDto(InventoryEntity entity) {
        Inventory dto = new Inventory();
        dto.setId(entity.getId());
        dto.setProductId(entity.getProductId());
        dto.setName(entity.getName());
        dto.setGender(entity.getGender() == null ? null : entity.getGender().name());
        dto.setColors(entity.getColors() == null ? List.of() : entity.getColors().stream().map(color -> {
            ColorDto colorDto = new ColorDto();
            colorDto.setColorId(color.getId());
            colorDto.setColor(color.getColor());
            colorDto.setImages(color.getImages());
            colorDto.setSizes(color.getSizes() == null ? List.of() : color.getSizes().stream().map(size -> {
                SizeDto sizeDto = new SizeDto();
                sizeDto.setSizeId(size.getSizeId());
                sizeDto.setSize(size.getSize());
                sizeDto.setTotalQuantity(valueOrZero(size.getTotalQuantity()));
                sizeDto.setAvailableQuantity(available(size));
                sizeDto.setReservedQuantity(valueOrZero(size.getReservedQuantity()));
                sizeDto.setSoldQuantity(valueOrZero(size.getSoldQuantity()));
                return sizeDto;
            }).toList());
            return colorDto;
        }).toList());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<Long> maxSold() {
        return inventoryRepo.findAll().stream()
                .sorted((first, second) -> Long.compare(totalSold(second), totalSold(first)))
                .map(InventoryEntity::getProductId)
                .toList();
    }

    private long totalSold(InventoryEntity inventory) {
        if (inventory.getColors() == null) return 0;
        return inventory.getColors().stream()
                .filter(Objects::nonNull)
                .flatMap(color -> color.getSizes() == null ? java.util.stream.Stream.empty() : color.getSizes().stream())
                .filter(Objects::nonNull)
                .mapToLong(size -> valueOrZero(size.getSoldQuantity()))
                .sum();
    }

    private long available(Sizes size) {
        if (size.getAvailableQuantity() != null) return size.getAvailableQuantity();
        return Math.max(0, valueOrZero(size.getTotalQuantity())
                - valueOrZero(size.getReservedQuantity()) - valueOrZero(size.getSoldQuantity()));
    }

    private long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }

    private Gender parseGender(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Gender.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestExceptionClass("gender must be MALE, FEMALE, or UNISEX!");
        }
    }

    @Transactional
    @CacheEvict(value = {"allInventoryV2", "inventoryV2"}, allEntries = true)
    public void deleteAll() {
        reservationRepo.deleteAll();
        inventoryRepo.deleteAll();
    }
}
