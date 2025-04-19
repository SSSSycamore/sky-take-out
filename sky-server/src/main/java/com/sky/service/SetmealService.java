package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;
import org.springframework.stereotype.Service;

@Service
public interface SetmealService {
    void save(SetmealDTO setmealDTO);

    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    void delete(Long[] ids);

    void update(SetmealDTO setmealDTO);

    SetmealVO getSetmealAndDishesById(Long id);

    void startOrStop(Integer status, Long id);
}
