package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
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
}
