package com.sky.service.impl;

import com.aliyun.oss.common.utils.StringUtils;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        Integer status = Orders.COMPLETED;
        List<Map<String, Object>> result = orderMapper.turnoverStatistic(status, begin, end);
        List<LocalDate> date = result.stream().map(map -> {
            Date d = (Date) map.get("date");
            return d.toLocalDate();
        }).toList();
//         补充没有营业额的日期
        while (!begin.isAfter(end)) {
            if (!date.contains(begin)) {
                result.add(Map.of("date", begin.toString(), "amount", 0.0));
            }
            begin = begin.plusDays(1);
        }
//         根据date排序Map
        result.sort((o1, o2) -> {
            LocalDate date1 = LocalDate.parse(o1.get("date").toString());
            LocalDate date2 = LocalDate.parse(o2.get("date").toString());
            return date1.compareTo(date2);
        });
        List<Object> dateList = result.stream().map(map -> map.get("date")).toList();
        List<Object> amount = result.stream().map(map -> map.get("amount")).toList();
        //把date列表转换成String数组，并用join方法拼接成一个字符串
        String dateStr = String.join(",", dateList.stream().map(Object::toString).toArray(String[]::new));
        //把amount列表转换成String数组，并用join方法拼接成一个字符串
        String amountStr = String.join(",", amount.stream().map(Object::toString).toArray(String[]::new));
        return new TurnoverReportVO(dateStr, amountStr);
    }

    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(!begin.isAfter(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            // 统计每一天的用户数量
            Integer totalUser = userMapper.getTotalUser(date);
            // 统计每一天的订单数量
            Integer newUser = userMapper.getNewUser(date);
            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }
        // 把date列表转换成String数组，并用join方法拼接成一个字符串
        String dateStr = String.join(",", dateList.stream().map(Object::toString).toArray(String[]::new));
        // 把totalUser列表转换成String数组，并用join方法拼接成一个字符串
        String totalUserStr = String.join(",", totalUserList.stream().map(Object::toString).toArray(String[]::new));
        // 把newUser列表转换成String数组，并用join方法拼接成一个字符串
        String newUserStr = String.join(",", newUserList.stream().map(Object::toString).toArray(String[]::new));
        return new UserReportVO(dateStr, totalUserStr, newUserStr);
    }

    @Override
    public OrderReportVO ordersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(!begin.isAfter(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> ordersCountList = new ArrayList<>();
        List<Integer> validOrdersCountList = new ArrayList<>();
        Double totalOrderCount = 0.0;
        Double validOrderCount = 0.0;
        for (LocalDate date : dateList) {
            // 统计每一天的订单数量
            Integer ordersCount = orderMapper.getCountByStatusAndTime(null, date);
            // 统计每一天的有效订单数量
            Integer validOrdersCount = orderMapper.getCountByStatusAndTime(Orders.COMPLETED, date);
            ordersCountList.add(ordersCount);
            validOrdersCountList.add(validOrdersCount);
            totalOrderCount += ordersCount;
            validOrderCount += validOrdersCount;
        }
        Double ordersCompletionRate = validOrderCount / totalOrderCount;
        String dateStr = String.join(",",dateList.stream().map(Object::toString).toArray(String[]::new));
        String ordersCountStr = String.join(",",ordersCountList.stream().map(Object::toString).toArray(String[]::new));
        String validOrdersCountStr = String.join(",",validOrdersCountList.stream().map(Object::toString).toArray(String[]::new));
        return OrderReportVO.builder()
                .dateList(dateStr)
                .totalOrderCount(totalOrderCount.intValue())
                .validOrderCount(validOrderCount.intValue())
                .orderCountList(ordersCountStr)
                .validOrderCountList(validOrdersCountStr)
                .orderCompletionRate(ordersCompletionRate)
                .build();
    }

    @Override
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {
        List<GoodsSalesDTO> goodsSalesList = orderMapper.top10(begin,end);
        List<String> nameList = goodsSalesList.stream().map(GoodsSalesDTO::getName).toList();
        List<Integer> numberList= goodsSalesList.stream().map(GoodsSalesDTO::getNumber).toList();
        String nameStr = String.join(",", nameList);
        String numberStr = String.join(",", numberList.stream().map(Object::toString).toArray(String[]::new));
        return SalesTop10ReportVO.builder()
                .nameList(nameStr)
                .numberList(numberStr)
                .build();
    }
}
