package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private DishMapper dishMapper;

    @Override
    public void add(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = shoppingCartMapper.getByDTO(shoppingCartDTO);
        if (shoppingCart != null){
            shoppingCart.setNumber(shoppingCart.getNumber() + 1);
            shoppingCartMapper.update(shoppingCart);
        }else{
            Long setmealId = shoppingCartDTO.getSetmealId();
            Long dishId = shoppingCartDTO.getDishId();
            String dishFlavor = shoppingCartDTO.getDishFlavor();
            ShoppingCart build = new ShoppingCart();
            if (setmealId != null){
                Setmeal byId = setmealMapper.getById(setmealId);
                build = ShoppingCart.builder()
                        .image(byId.getImage())
                        .name(byId.getName())
                        .userId(BaseContext.getCurrentId())
                        .setmealId(setmealId)
                        .number(1)
                        .amount(byId.getPrice())
                        .createTime(LocalDateTime.now())
                        .build();
            }else{
                Dish dish = dishMapper.getById(dishId);
                build = ShoppingCart.builder()
                        .image(dish.getImage())
                        .name(dish.getName())
                        .userId(BaseContext.getCurrentId())
                        .dishId(dishId)
                        .dishFlavor(dishFlavor)
                        .number(1)
                        .amount(dish.getPrice())
                        .createTime(LocalDateTime.now())
                        .build();
            }
            shoppingCartMapper.insert(build);
        }
    }

    @Override
    public List<ShoppingCart> list() {
        Long currentId = BaseContext.getCurrentId();
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.list(currentId);
        return shoppingCarts;
    }

    @Override
    public void clean() {
        shoppingCartMapper.clean(BaseContext.getCurrentId());
    }

    @Override
    public void sub(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart byDTO = shoppingCartMapper.getByDTO(shoppingCartDTO);
        if (byDTO == null){
            return;
        }
        if (byDTO.getNumber() > 1) {
            byDTO.setNumber(byDTO.getNumber() - 1);
            shoppingCartMapper.update(byDTO);
        }else{
            shoppingCartMapper.deleteById(byDTO.getId());
        }
    }
}
