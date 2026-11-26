package com.example.InventoryService.Controller;

import com.example.InventoryService.Dto.Inventory;
import com.example.InventoryService.Dto.InventoryItem;
import com.example.InventoryService.Entities.ReservationEntity;
import com.example.InventoryService.Service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@Tag(name = "Inventory APIs",description = "RestApis for managing Inventory")
public class InventoryController {
    @Autowired
    private InventoryService inventoryService;
    //admin
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add Inventory but ensure that Product is present or not",description = "Required accessToken of admin")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Inventory added successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "name": "Modern Joggers 050",
                                                "productId": 150,
                                                "gender": "UNISEX",
                                                "colors": [
                                                  {
                                                    "colorId": 350,
                                                    "color": "Black",
                                                    "images": "/images/products/050/black.jpg",
                                                    "sizes": [
                                                      {
                                                        "sizeId": 550,
                                                        "size": "S",
                                                        "totalQuantity": 55
                                                      },
                                                      {
                                                        "sizeId": 551,
                                                        "size": "M",
                                                        "totalQuantity": 72
                                                      },
                                                      {
                                                        "sizeId": 552,
                                                        "size": "L",
                                                        "totalQuantity": 89
                                                      },
                                                      {
                                                        "sizeId": 553,
                                                        "size": "XL",
                                                        "totalQuantity": 25
                                                      },
                                                      {
                                                        "sizeId": 554,
                                                        "size": "XXL",
                                                        "totalQuantity": 42
                                                      }
                                                    ]
                                                  }
                                                ]
                                              }
                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Something wrong while adding inventory"
            )
    })
    @PostMapping("/add")
    public ResponseEntity<List<Inventory>> add(@RequestBody List<Inventory> inventory){
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.add(inventory));
    }

    @Operation(summary = "Product Service call this api internally")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/internal/maxSold")
    public ResponseEntity<List<Long>> maxSold(){
        return ResponseEntity.status(HttpStatus.OK).body(inventoryService.maxSold());
    }
    //admin
    @Operation(summary = "Delete inventory item by providing ids as a list in RequestBody also required token of admin" +
            "Or Product Service call internally this api When Products are deleted")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Inventory deleted success"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Required ids"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/internal/delete")
    public ResponseEntity<Void> delete(@RequestBody List<Long> ids){
        inventoryService.delete(ids);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
    //admin
    @Operation(summary = "Inventory delete all data also required accessToken of admin Or Product Service call this api internally")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "All Inventory deleted success"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/internal/deleteAll")
    public ResponseEntity<Void> deleteAll(){
        inventoryService.deleteAll();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
    @Operation(summary = "Order Service call this api internally when placing the Order")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/reserved/{orderId}")
    public ResponseEntity<Void> reservedFromOrder(@PathVariable Long orderId, @RequestBody List<InventoryItem> items){
        inventoryService.reserved(orderId,items);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "no use of this api")
    @PostMapping("/released/{orderId}")
    public ResponseEntity<Void> releaseReservation(@PathVariable Long orderId){
        inventoryService.release(orderId);
        return ResponseEntity.noContent().build();
    }
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Order Service call this Api internally after payment received successfully")
    @PostMapping("/confirm/{orderId}")
    public ResponseEntity<Void> confirmReservation(@PathVariable Long orderId){
        inventoryService.confirm(orderId);
        return ResponseEntity.noContent().build();
    }
    @Operation(summary = "Payment Service call this api internally")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/payment-started/{orderId}")
    public ResponseEntity<Void> paymentStarted(@PathVariable Long orderId){
        inventoryService.markPaymentStarted(orderId);
        return ResponseEntity.noContent().build();
    }
    @Operation(summary = "Order Service and Payment Service call this api internally when payment failed")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/payment-aborted/{orderId}")
    public ResponseEntity<Void> paymentAborted(@PathVariable Long orderId){
        inventoryService.clearPaymentStarted(orderId);
        return ResponseEntity.noContent().build();
    }
    //admin
    @Operation(summary = "Get all Stocks of inventory",description = "Required accessToken of admin")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/stocks")
    public ResponseEntity<List<Inventory>> getAllStock(){
        return ResponseEntity.ok(inventoryService.getAllStock());
    }
    //admin
    @Operation(summary = "Update Inventory items",description = "Required accessToken of admin")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Update inventory item successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Something Records are not found"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/update")
    public ResponseEntity<List<Inventory>> update(@RequestBody List<InventoryItem> inventoryItems){
        return new ResponseEntity<>(inventoryService.updateInventory(inventoryItems),HttpStatus.OK);
    }
    //admin
    @Operation(summary = "Get stock of inventory with productId",description = "Required accessToken of admin")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/stock/{productId}")
    public ResponseEntity<Inventory> getStock(@PathVariable Long productId){
        return new ResponseEntity<>(inventoryService.getStock(productId),HttpStatus.OK);
    }
    //admin
    @Operation(summary = "Get Reserved Data from Reservation",description = "Required accessToken of admin")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/reserved")
    public ResponseEntity<List<ReservationEntity>> getReservedRecords(){
        return new ResponseEntity<>(inventoryService.getReservedRecords(),HttpStatus.OK);
    }
    //admin
    @Operation(summary = "Get Released Data from Reservation",description = "Required accessToken of admin")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/released")
    public ResponseEntity<List<ReservationEntity>> getReleasedRecords(){
        return new ResponseEntity<>(inventoryService.getReleasedRecords(),HttpStatus.OK);
    }
    //admin
    @Operation(summary = "Get Confirmed Data from Reservation",description = "Required accessToken of admin")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/confirmed")
    public ResponseEntity<List<ReservationEntity>> getConfirmedRecords(){
        return new ResponseEntity<>(inventoryService.getConfirmedRecords(),HttpStatus.OK);
    }


}
