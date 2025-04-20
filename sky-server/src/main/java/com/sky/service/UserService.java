package com.sky.service;

import com.sky.dto.UserLoginDTO;
import com.sky.vo.UserLoginVO;
import org.springframework.stereotype.Service;

public interface UserService {

    UserLoginVO login(UserLoginDTO userLoginDTO);
}
