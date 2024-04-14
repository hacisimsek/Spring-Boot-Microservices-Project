package com.hacisimsek.orderservice.service;

import com.hacisimsek.orderservice.config.WebClientConfig;
import com.hacisimsek.orderservice.dto.InventoryResponse;
import com.hacisimsek.orderservice.dto.OrderLineItemsDto;
import com.hacisimsek.orderservice.dto.OrderRequest;
import com.hacisimsek.orderservice.model.Order;
import com.hacisimsek.orderservice.model.OrderLineItems;
import com.hacisimsek.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    private final WebClient.Builder webClientBuilder;

    public void placeOrder(OrderRequest orderRequest){

        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream().map(this::mapToDto).toList();

        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream().map(OrderLineItems::getSkuCode).toList();

//        InventoryResponse[] inventoryResponses = webClient.get()
//                .uri("http://localhost:8082/api/inventory", uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
//                .retrieve()
//                .bodyToMono(InventoryResponse[].class)
//                .block();
//
//
//        boolean allProductsInStock = true;
//
//        for(InventoryResponse inventoryResponse : inventoryResponses){
//            if(!inventoryResponse.isInStock()){
//                allProductsInStock = false;
//                break;
//            }
//        }

        boolean allProductsInStock = false;

        try {
            InventoryResponse[] inventoryResponses = webClientBuilder.build()
                    .get()
                    .uri("http://inventory-service/api/inventory", uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();

            //allProductsInStock = Arrays.stream(inventoryResponses).allMatch(InventoryResponse::isInStock);

            for(InventoryResponse inventoryResponse : inventoryResponses){
                if(!inventoryResponse.isInStock()){
                    allProductsInStock = false;
                    break;
                } else {
                    allProductsInStock = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        if(allProductsInStock){
            orderRepository.save(order);
        }else{
            throw new IllegalArgumentException("Prodcut is not is stock");
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto){
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        return orderLineItems;
    }
}
