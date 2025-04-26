package com.sky.mapper;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.TurnoverReportVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    void insert(Orders orders);
    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    List<Orders> getByUserIdAndStatus(Long userId, Integer status);

    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    List<Orders> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    @Select("select count(*) from orders where status = #{status}")
    Integer getCountByStatus(Integer status);

    @Select("select * from orders where status = #{status} and order_time < #{localDateTime}")
    List<Orders> getTimeOutOrders(Integer status, LocalDateTime localDateTime);

    void updateByIds(List<Long> ids, Integer status, String cancelReason, LocalDateTime cancelTime);

    List<Map<String, Object>> turnoverStatistic(Integer status, LocalDate begin, LocalDate end);

    Integer getCountByStatusAndTime(Integer status, LocalDate date);
}
