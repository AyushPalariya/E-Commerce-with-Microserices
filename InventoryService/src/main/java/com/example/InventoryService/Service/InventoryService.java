package com.example.InventoryService.Service;

import com.example.InventoryService.Dto.Inventory;
import com.example.InventoryService.Dto.InventoryItem;
import com.example.InventoryService.Dto.Product;
import com.example.InventoryService.Entities.InventoryEntity;
import com.example.InventoryService.Entities.ReservationEntity;
import com.example.InventoryService.Entities.Status;
import com.example.InventoryService.Global.BadRequestExceptionClass;
import com.example.InventoryService.Repository.ProductClientInterface;
import com.example.InventoryService.Repository.InventoryRepo;
import com.example.InventoryService.Repository.ReservationRepo;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InventoryService {
    @Autowired
    private ReservationRepo reservationRepo;

    @Autowired
    private InventoryRepo inventoryRepo;
    @Autowired
    private ProductClientInterface feignClientInterface;
    @Transactional
    @CacheEvict(value = {"allInventoryV2","inventoryV2"},allEntries = true)
    public List<Inventory> add(List<Inventory> inventorys) {
        validate(inventorys);
        List<InventoryEntity> list=new ArrayList<>();
        inventorys.forEach(in->{
            InventoryEntity inventoryEntity=new InventoryEntity();
            inventoryEntity.setName(in.getName());
            inventoryEntity.setProductId(in.getProductId());
            inventoryEntity.setTotalQuantity(in.getTotalQuantity());
            inventoryEntity.setAvailableQuantity(in.getTotalQuantity());
            list.add(inventoryEntity);
        });
        List<InventoryEntity> inventoryEntities = inventoryRepo.saveAll(list);
        log.info("inventory added");
        return convertIntoDto(inventoryEntities);
    }

    private List<Inventory> convertIntoDto(List<InventoryEntity> inventoryEntities) {
        return inventoryEntities.stream().map(en->new Inventory(en.getId(),en.getName(),en.getProductId(),en.getTotalQuantity(),en.getAvailableQuantity()))
                .toList();
    }

    private void validate(List<Inventory> inventorys) {
        List<Product> productList=new ArrayList<>();
        List<Long> productIds=new ArrayList<>();
        inventorys.forEach(inventory->{
            if(inventory.getName()==null||inventory.getName().isBlank()) throw new BadRequestExceptionClass("name is required!");
            if(inventory.getProductId()==null||inventory.getProductId()<=0) throw new BadRequestExceptionClass("productId must be valid!");
            if(inventory.getTotalQuantity()==null||inventory.getTotalQuantity()<=0) throw new BadRequestExceptionClass("totalQuantity is required!");
            productList.add(new Product(inventory.getProductId(),inventory.getName()));
            productIds.add(inventory.getProductId());
        });
        List<InventoryEntity> allByProductIdIn = inventoryRepo.findAllByProductIdIn(productIds);
        if(!allByProductIdIn.isEmpty()) throw new BadRequestExceptionClass("ProductId already present");
        List<Product> products=feignClientInterface.verifyProductIds(productList);
        log.info("products are :{}",products);
    }
    @Transactional
    @CacheEvict(value = {"allInventoryV2","inventoryV2"},allEntries = true)
    public void delete(List<Long> ids) {
        if(ids==null||ids.isEmpty()) throw new BadRequestExceptionClass("ids must be entered by you!");
        inventoryRepo.deleteAllById(ids);
        log.info("inventorys deleted:{}",ids);
    }
    @Transactional
    @CacheEvict(value = {"allInventoryV2","inventoryV2"},allEntries = true )
    public void reserved(Long orderId, List<InventoryItem> items) {
        if(orderId==null||orderId<=0) throw new BadRequestExceptionClass("orderId must be valid!");
        List<Long> productIds=validate1(items);
        List<InventoryEntity> inventories = inventoryRepo.findAllByProductIdIn(productIds);
        if(inventories.size()!= productIds.size()) throw new BadRequestExceptionClass("some records are not found!");
        Map<Long,Long> mp=items.stream().collect(Collectors.toMap(InventoryItem::getProductId,InventoryItem::getQuantities));
        List<ReservationEntity> list=new ArrayList<>();
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(15);
        for(InventoryEntity in:inventories){
            Long dbAvailableQuantity = currentAvailableQuantity(in);
            Long userQuantity=mp.get(in.getProductId());
            if(userQuantity>dbAvailableQuantity) throw new BadRequestExceptionClass("Insufficient stock available of "+in.getName()+" for productId: " + in.getProductId());
            in.setAvailableQuantity(dbAvailableQuantity-userQuantity);
            Long reservedQuantity = in.getReservedQuantity();
            if(reservedQuantity==null) in.setReservedQuantity(userQuantity);
            else in.setReservedQuantity(reservedQuantity+userQuantity);
            list.add(new ReservationEntity(orderId,in.getProductId(),userQuantity, Status.RESERVED, expiredAt));
        }
        List<ReservationEntity> reservationEntities = reservationRepo.saveAll(list);
        inventories = inventoryRepo.saveAll(inventories);
        log.info("Reserved records:{}",reservationEntities);
        log.info("inventory update with reserved:{}",inventories);
    }
    @Transactional
    @Cacheable(value = "allInventoryV2",key = "'all'")
    public List<Inventory> getAllStock() {
        List<InventoryEntity> all = inventoryRepo.findAll();
        log.info("allStocks of inventory get..");
        //dto
        return all.stream().map(in->new Inventory(in.getId(),in.getName(),in.getProductId(),
                in.getTotalQuantity(),currentAvailableQuantity(in),in.getReservedQuantity(),in.getSoldQuantity()))
                .toList();
    }
    @Transactional
    @Cacheable(value = "inventoryV2",key = "#inventory.productId")
    public Inventory getStock(Inventory inventory){
        InventoryEntity in = inventoryRepo.findByProductId(inventory.getProductId()).orElseThrow(() -> new BadRequestExceptionClass("product not found with productId:" + inventory.getProductId()));
        log.info("inventory get with productId:{}",in.getProductId());
        return new Inventory(in.getId(),in.getName(),in.getProductId(),in.getTotalQuantity(),currentAvailableQuantity(in),
                in.getReservedQuantity(),in.getSoldQuantity());
    }
    @Transactional
    @CacheEvict(value = {"allInventoryV2","inventoryV2"},allEntries = true)
    public List<Inventory> updateInventory(List<InventoryItem> items){
        List<Long> productIds=validate1(items);
        List<InventoryEntity> inventories = inventoryRepo.findAllByProductIdIn(productIds);
        if(inventories.size()!= productIds.size()) throw new BadRequestExceptionClass("some records are not found!");

        Map<Long,Long> mp=items.stream().collect(Collectors.toMap(InventoryItem::getProductId,InventoryItem::getQuantities));
        for(InventoryEntity in:inventories){
            Long userQuantity=mp.get(in.getProductId());
            in.setTotalQuantity(in.getTotalQuantity()+userQuantity);
            Long availQuanDb = currentAvailableQuantity(in);
            in.setAvailableQuantity(availQuanDb+userQuantity);
        }
        List<InventoryEntity> updated = inventoryRepo.saveAll(inventories);
        log.info("Stock updated ids:{}",productIds);
        return updated.stream().map(in->new Inventory(in.getId(),in.getName(),in.getProductId(),
                        in.getTotalQuantity(),currentAvailableQuantity(in),in.getReservedQuantity(),in.getSoldQuantity()))
                .toList();
    }
    public List<Long> validate1(List<InventoryItem> items){
        if(items==null||items.isEmpty()) throw new BadRequestExceptionClass("Stock details required!");
        List<Long> productIds=items.stream().map(update->{
            if (update.getProductId() == null || update.getProductId() <= 0)
                throw new BadRequestExceptionClass("Invalid productId");

            if (update.getQuantities()==null||update.getQuantities() <= 0)
                throw new BadRequestExceptionClass("Invalid quantity");
            return update.getProductId();
        }).toList();
        if(productIds.size()!=productIds.stream().distinct().toList().size()) throw new BadRequestExceptionClass("duplicate ids are present!");
        return productIds;
    }

    public List<ReservationEntity> getReservedRecords() {
        return reservationRepo.findByStatus(Status.RESERVED);
    }
    public List<ReservationEntity> getReleasedRecords() {
        return reservationRepo.findByStatus(Status.RELEASED);
    }
    public List<ReservationEntity> getConfirmedRecords() {
        return reservationRepo.findByStatus(Status.CONFIRM);
    }

    @Transactional
    @CacheEvict(value = {"allInventoryV2", "inventoryV2"}, allEntries = true)
    public void release(Long orderId) {
        List<ReservationEntity> reservations = activeReservations(orderId, Status.RELEASED);
        if (reservations.isEmpty()) return;
        releaseActiveReservations(orderId, reservations);
    }

    @Transactional
    @CacheEvict(value = {"allInventoryV2", "inventoryV2"}, allEntries = true)
    public void confirm(Long orderId) {
        List<ReservationEntity> reservations = activeReservations(orderId, Status.CONFIRM);
        if (reservations.isEmpty()) return;
        Map<Long, InventoryEntity> inventories = loadReservedInventories(orderId, reservations);
        for (ReservationEntity reservation : reservations) {
            InventoryEntity inventory = inventories.get(reservation.getProductId());
            long reserved = inventory.getReservedQuantity() == null ? 0 : inventory.getReservedQuantity();
            if (reserved < reservation.getQuantities()) throw new BadRequestExceptionClass("Reserved quantity is inconsistent for productId: " + reservation.getProductId());
            inventory.setReservedQuantity(reserved - reservation.getQuantities());
            inventory.setSoldQuantity((inventory.getSoldQuantity() == null ? 0 : inventory.getSoldQuantity()) + reservation.getQuantities());
            reservation.setStatus(Status.CONFIRM);
        }
        inventoryRepo.saveAll(inventories.values());
        reservationRepo.saveAll(reservations);
    }

    @Transactional
    @CacheEvict(value = {"allInventoryV2", "inventoryV2"}, allEntries = true)
    public void markPaymentStarted(Long orderId) {
        List<ReservationEntity> reservations = activeReservations(orderId, Status.CONFIRM);
        if (reservations.isEmpty()) throw new BadRequestExceptionClass("No active reservation found for orderId: " + orderId);
        LocalDateTime now = LocalDateTime.now();
        if (reservations.stream().anyMatch(reservation -> reservation.getExpiredAt() == null || !reservation.getExpiredAt().isAfter(now)))
            throw new BadRequestExceptionClass("Reservation has expired for orderId: " + orderId);
        reservations.forEach(reservation -> reservation.setPaymentStarted(true));
        reservationRepo.saveAll(reservations);
        log.info("Payment Started..");
    }

    @Transactional
    @CacheEvict(value = {"allInventoryV2", "inventoryV2"}, allEntries = true)
    public void clearPaymentStarted(Long orderId) {
        if (orderId == null || orderId <= 0) throw new BadRequestExceptionClass("orderId must be valid!");
        List<ReservationEntity> reservations = reservationRepo.findByOrderIdAndStatus(orderId, Status.RESERVED);
        reservations.forEach(reservation -> reservation.setPaymentStarted(false));
        reservationRepo.saveAll(reservations);
    }

    @Transactional
    @CacheEvict(value = {"allInventoryV2", "inventoryV2"}, allEntries = true)
    @Scheduled(fixedDelayString = "${inventory.reservation-expiration-check-ms:30000}")
    public void releaseExpiredReservations() {
        List<ReservationEntity> expiredReservations = reservationRepo.findExpiredUnpaid(Status.RESERVED, LocalDateTime.now());
        if (expiredReservations.isEmpty()) return;
        Map<Long, List<ReservationEntity>> byOrder = expiredReservations.stream()
                .collect(Collectors.groupingBy(ReservationEntity::getOrderId));
        byOrder.forEach(this::releaseActiveReservations);
        log.info("Released expired reservations for {} orders", byOrder.size());
    }

    private List<ReservationEntity> activeReservations(Long orderId, Status completedStatus) {
        if (orderId == null || orderId <= 0) throw new BadRequestExceptionClass("orderId must be valid!");
        List<ReservationEntity> reservations = reservationRepo.findByOrderIdAndStatus(orderId, Status.RESERVED);
        if (reservations.isEmpty()) {
            if (!reservationRepo.findByOrderIdAndStatus(orderId, completedStatus).isEmpty()) return List.of();
            throw new BadRequestExceptionClass("No active reservation found for orderId: " + orderId);
        }
        return reservations;
    }

    private Map<Long, InventoryEntity> loadReservedInventories(Long orderId, List<ReservationEntity> reservations) {
        List<Long> productIds = reservations.stream().map(ReservationEntity::getProductId).toList();
        List<InventoryEntity> found = inventoryRepo.findAllByProductIdIn(productIds);
        if (found.size() != productIds.size()) throw new BadRequestExceptionClass("Inventory records missing for orderId: " + orderId);
        return found.stream().collect(Collectors.toMap(InventoryEntity::getProductId, inventory -> inventory));
    }

    private long currentAvailableQuantity(InventoryEntity inventory) {
        if (inventory.getAvailableQuantity() != null) return inventory.getAvailableQuantity();
        long reserved = inventory.getReservedQuantity() == null ? 0 : inventory.getReservedQuantity();
        long sold = inventory.getSoldQuantity() == null ? 0 : inventory.getSoldQuantity();
        return Math.max(0, inventory.getTotalQuantity() - reserved - sold);
    }

    private void releaseActiveReservations(Long orderId, List<ReservationEntity> reservations) {
        Map<Long, InventoryEntity> inventories = loadReservedInventories(orderId, reservations);
        for (ReservationEntity reservation : reservations) {
            InventoryEntity inventory = inventories.get(reservation.getProductId());
            long reserved = inventory.getReservedQuantity() == null ? 0 : inventory.getReservedQuantity();
            if (reserved < reservation.getQuantities()) throw new BadRequestExceptionClass("Reserved quantity is inconsistent for productId: " + reservation.getProductId());
            inventory.setReservedQuantity(reserved - reservation.getQuantities());
            inventory.setAvailableQuantity(currentAvailableQuantity(inventory) + reservation.getQuantities());
            reservation.setStatus(Status.RELEASED);
        }
        inventoryRepo.saveAll(inventories.values());
        reservationRepo.saveAll(reservations);
    }
}
