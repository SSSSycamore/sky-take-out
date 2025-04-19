package com.sky.controller.admin;

import com.sky.constant.StatusConstant;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/admin/shop")
@RestController(value = "adminShopController")
@Slf4j
@Api(tags= "商家相关接口")
public class ShopController {
    @Autowired
    private RedisTemplate redisTemplate;

    private static final String KEY = "Shop_Status";

    @PutMapping("/{status}")
    @ApiOperation(value = "修改商家状态")
    public Result setStatus(@PathVariable Integer status) {
        log.info("修改商家状态：{}", status == StatusConstant.ENABLE ? "营业中" : "已打烊");
        redisTemplate.opsForValue().set(KEY, status);
        return Result.success();
    }

    @GetMapping("/status")
    @ApiOperation(value = "查询商家状态")
    public Result<Integer> getStatus() {
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        log.info("当前商家状态：{}", status == StatusConstant.ENABLE ? "营业中" : "已打烊");
        return Result.success(status);
    }
}