package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.OrderService;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //检查参数是否异常
        Long addressBookId = ordersSubmitDTO.getAddressBookId();
        AddressBook byId = addressBookMapper.getById(addressBookId);
        if (byId == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //检查购物车是否为空
        Long currentId = BaseContext.getCurrentId();
        List<ShoppingCart> list = shoppingCartMapper.list(currentId);
        if (list == null || list.isEmpty()) {
            throw new AddressBookBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setUserId(currentId);
        orders.setAddress(byId.getDetail());
        orders.setNumber(UUID.randomUUID().toString());
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setConsignee(byId.getConsignee());
        orders.setPhone(byId.getPhone());
        orderMapper.insert(orders);
        //向订单详细表插入多条数据
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (ShoppingCart shoppingCart : list) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetails);
        //删除购物车表中的数据
        shoppingCartMapper.clean(currentId);
        //返回订单提交结果
        return new OrderSubmitVO(orders.getId(), orders.getNumber(), orders.getAmount(),orders.getOrderTime());
    }
}
