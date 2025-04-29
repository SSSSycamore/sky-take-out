package com.sky.service.impl;

import com.aliyun.oss.common.utils.StringUtils;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

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
            Integer newUser = userMapper.getNewUser(date, date);
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
            Integer ordersCount = orderMapper.getCountByStatusAndTime(null, date,date);
            // 统计每一天的有效订单数量
            Integer validOrdersCount = orderMapper.getCountByStatusAndTime(Orders.COMPLETED, date,date);
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

    /**
     * 导出运营数据报表
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {
        //1. 查询数据库，获取营业数据---查询最近30天的运营数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        //查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(dateBegin, dateEnd);

        //2. 通过POI将数据写入到Excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            //获取表格文件的Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            //填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);

            //获得第4行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            //获得第5行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                //查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(date, date);

                //获得某一行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            //3. 通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //关闭资源
            out.close();
            excel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
