package com.sky.task;

import com.sky.constant.MessageConstant;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;

    @Scheduled(cron = "0 * * * * * ")
    public void handleUnPaidOrder(){
        log.info("定时处理超时未支付订单:{}", LocalDateTime.now());
        List<Orders> orders = orderMapper.getTimeOutOrders(Orders.PENDING_PAYMENT,
                LocalDateTime.now().minusMinutes(15));
        if (orders != null && orders.size() > 0) {
            List<Long> ids = new ArrayList<>();
            orders.forEach(order -> {
                ids.add(order.getId());
            });
        orderMapper.updateByIds(ids,
                Orders.CANCELLED,
                MessageConstant.ORDER_NOT_PAY_FOR_TIME_OUT,
                LocalDateTime.now());
        log.info("超时未支付订单:{}", orders);
        } else {
            log.info("没有超时未支付订单");
        }
    }

    @Scheduled(cron = "0 0 1 * * * ")
    public void handleUnCompletedOrders(){
        log.info("定时处理超时未完成订单:{}", LocalDateTime.now());
        List<Orders> orders = orderMapper.getTimeOutOrders(Orders.DELIVERY_IN_PROGRESS,
                LocalDateTime.now().minusMinutes(60));
        if (orders != null && orders.size() > 0) {
            List<Long> ids = new ArrayList<>();
            orders.forEach(order -> {
                ids.add(order.getId());
            });
            orderMapper.updateByIds(ids,
                    Orders.COMPLETED,
                    null,
                    null);
            log.info("超时未完成订单:{}", orders);
        } else {
            log.info("没有超时未完成订单");
        }
    }
}
