package com.sky.mapper;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {
    ShoppingCart getByDTO(ShoppingCartDTO shoppingCartDTO);

    void update(ShoppingCart shoppingCart);

    @Insert("insert into shopping_cart (name, image, user_id, dish_id, setmeal_id, dish_flavor, number, amount, create_time) " +
            "values" +
            " (#{name},#{image},#{userId},#{dishId},#{setmealId},#{dishFlavor},#{number},#{amount},#{createTime})")
    void insert(ShoppingCart build);

    @Select("select * from shopping_cart where user_id = #{currentId}")
    List<ShoppingCart> list(Long currentId);

    @Delete("delete from shopping_cart where user_id = #{currentId}")
    void clean(Long currentId);

    @Delete("delete from shopping_cart where id = #{id}")
    void deleteById(Long id);
}
