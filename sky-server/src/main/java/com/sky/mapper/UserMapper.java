package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface UserMapper {
    @Insert("INSERT INTO user (openid) VALUES (#{openid})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);

    @Select("SELECT * FROM user WHERE openid = #{openid}")
    User selectByOpenid(String openid);

    @Select("select * from user where id = #{userId}")
    User getById(Long userId);

    @Select("select count(id) from user where date(create_time) <= date(#{date})")
    Integer getTotalUser(LocalDate date);

    Integer getNewUser(LocalDate begin, LocalDate end);
}
