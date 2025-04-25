package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderQueryVO;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.websocket.WebSocketServer;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

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
        User user = userMapper.getById(currentId);
        //向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setUserName(user.getName());
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

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
        JSONObject result = new JSONObject();
        result.put("type",1);
        result.put("orderId",ordersDB.getId());
        result.put("content", outTradeNo + "订单已支付");
        webSocketServer.sendToAllClient(result.toJSONString());
    }

    @Override
    public PageResult historyOrders(Integer page, Integer pageSize, Integer status) {
        PageHelper.startPage(page,pageSize);
        Long userId = BaseContext.getCurrentId();
        List<Orders> ordersList = orderMapper.getByUserIdAndStatus(userId,status);
        List<OrderQueryVO> result = new ArrayList<>();
        if (ordersList != null && !ordersList.isEmpty()) {
            for (Orders orders : ordersList) {
                OrderQueryVO orderHistroyVO = new OrderQueryVO();
                BeanUtils.copyProperties(orders, orderHistroyVO);
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());
                orderHistroyVO.setOrderDetailList(orderDetails);
                result.add(orderHistroyVO);
            }
        }
        Page p = (Page) ordersList;
        return new PageResult(p.getTotal(), result);
    }

    @Override
    public OrderQueryVO orderDetail(Long id) {
        Orders orders = orderMapper.getById(id);
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
        OrderQueryVO orderQueryVO = new OrderQueryVO();
        BeanUtils.copyProperties(orders, orderQueryVO);
        orderQueryVO.setOrderDetailList(orderDetails);
        StringBuilder orderDishes = new StringBuilder();
        for (OrderDetail orderDetail : orderDetails) {
            orderDishes.append(orderDetail.getName());
            if (orderDetail.getDishFlavor() != null) {
                orderDishes.append("[").append(orderDetail.getDishFlavor()).append("]");
            }
            orderDishes.append("x").append(orderDetail.getNumber()).append("、");
        }
        return orderQueryVO;
    }

    @Override
    public void userCancel(Long id) {
        Orders byId = orderMapper.getById(id);
        if (byId == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (byId.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setId(id);
        if (byId.getStatus().equals(Orders.TO_BE_CONFIRMED)){
//            weChatPayUtil.refund(
//                    byId.getNumber(), //商户订单号
//                    byId.getNumber(), //商户退款单号
//                    new BigDecimal(0.01),//退款金额，单位 元
//                    new BigDecimal(0.01));//原订单金额
            orders.setPayStatus(Orders.REFUND);
            log.info("申请退款，单号：{}", orders.getNumber());
        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户主动取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void repetition(Long id) {
        Orders byId = orderMapper.getById(id);
        if (byId == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Long userId = BaseContext.getCurrentId();

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        List<Orders> orderList = orderMapper.conditionSearch(ordersPageQueryDTO);
        Page p = (Page) orderList;
        List<OrderQueryVO> orderQueryVOList = new ArrayList<>();
        for (Orders orders : orderList) {
            List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());
            OrderQueryVO orderQueryVO = new OrderQueryVO();
            BeanUtils.copyProperties(orders, orderQueryVO);
            StringBuilder orderDishes = new StringBuilder();
            for (OrderDetail orderDetail : orderDetails) {
                orderDishes.append(orderDetail.getName());
                if (orderDetail.getDishFlavor() != null) {
                    orderDishes.append("[").append(orderDetail.getDishFlavor()).append("]");
                }
                orderDishes.append("x").append(orderDetail.getNumber()).append("、");
            }
            orderQueryVO.setOrderDishes(orderDishes.toString());
            orderQueryVOList.add(orderQueryVO);
        }
        return new PageResult(p.getTotal(), orderQueryVOList);
    }

    @Override
    public OrderStatisticsVO statistics() {
        Integer toBeConfirmed = orderMapper.getCountByStatus(Orders.TO_BE_CONFIRMED);
        Integer deliveryInProgress = orderMapper.getCountByStatus(Orders.DELIVERY_IN_PROGRESS);
        Integer confirmed = orderMapper.getCountByStatus(Orders.CONFIRMED);
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        orderStatisticsVO.setConfirmed(confirmed);
        return orderStatisticsVO;
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        //根据id查询订单
        Orders byId = orderMapper.getById(ordersConfirmDTO.getId());
        if (byId == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (byId.getStatus() != Orders.TO_BE_CONFIRMED) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        ordersConfirmDTO.setStatus(Orders.CONFIRMED);
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersConfirmDTO, orders);
        orderMapper.update(orders);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
       Orders orders = orderMapper.getById(ordersRejectionDTO.getId());
       if (orders == null) {
          throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
      }
      if (orders.getStatus() > Orders.TO_BE_CONFIRMED) {
         throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
      }
      if (orders.getStatus() == Orders.TO_BE_CONFIRMED) {
//          weChatPayUtil.refund(
//                  orders.getNumber(), //商户订单号
//                  orders.getNumber(), //商户退款单号
//                  new java.math.BigDecimal(0.01),//退款金额，单位 元
//                  new java.math.BigDecimal(0.01));//原订单金额
          orders.setPayStatus(Orders.REFUND);
          log.info("申请退款，单号：{}", orders.getNumber());
      }
       orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
       orders.setCancelTime(LocalDateTime.now());
       orders.setStatus(Orders.CANCELLED);
       orderMapper.update(orders);
    }

    @Override
    public void adminCancel(OrdersCancelDTO ordersCancelDTO) {
        Orders orders = orderMapper.getById(ordersCancelDTO.getId());
        if (orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (orders.getStatus() == Orders.CANCELLED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        if (orders.getStatus() >= Orders.TO_BE_CONFIRMED){
//          weChatPayUtil.refund(
//                  orders.getNumber(), //商户订单号
//                  orders.getNumber(), //商户退款单号
//                  new java.math.BigDecimal(0.01),//退款金额，单位 元
//                  new java.math.BigDecimal(0.01));//原订单金额
            orders.setPayStatus(Orders.REFUND);
            log.info("申请退款，单号：{}", orders.getNumber());
        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    @Override
    public void delivery(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (orders.getStatus() != Orders.CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);
    }

    @Override
    public void complete(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (orders.getStatus() != Orders.DELIVERY_IN_PROGRESS){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    @Override
    public void reminder(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        JSONObject json = new JSONObject();
        json.put("type",2);
        json.put("orderId",id);
        json.put("content","订单号:"+id);
        webSocketServer.sendToAllClient(json.toJSONString());
    }
}
