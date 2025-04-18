package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     * @param id
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

    @Insert("insert into setmeal (category_id, name, price, description, image, create_time, update_time, create_user, update_user,status)" +
            " values " +
            "(#{categoryId},#{name},#{price},#{description},#{image},#{createTime},#{updateTime},#{createUser},#{updateUser},#{status})")
    @AutoFill(value = OperationType.INSERT)
    @Options(useGeneratedKeys = true,keyProperty = "id")
    void insert(Setmeal setmeal);
}
