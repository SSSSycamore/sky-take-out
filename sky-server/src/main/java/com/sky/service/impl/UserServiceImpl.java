package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.JwtProperties;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private JwtProperties jwtProperties;
    private static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    private String getString(UserLoginDTO userLoginDTO) {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("appid", weChatProperties.getAppid());
        paramMap.put("secret", weChatProperties.getSecret());
        paramMap.put("js_code", userLoginDTO.getCode());
        paramMap.put("grant_type", "authorization_code");
        String s = HttpClientUtil.doGet(WX_LOGIN, paramMap);
        JSONObject json = JSONObject.parseObject(s);
        String openid = json.getString("openid");
        return openid;
    }

    @Override
    public UserLoginVO login(UserLoginDTO userLoginDTO) {
        String openid = getString(userLoginDTO);
        if(openid == null) {
            throw new LoginFailedException(MessageConstant.CODE_INVALID_OR_EXPIRED);
        }

        User byOpenid = userMapper.selectByOpenid(openid);
        if(byOpenid != null) {
            Map<String, Object> claims = new HashMap<>();
            claims.put(JwtClaimsConstant.USER_ID, byOpenid.getId());

            return new UserLoginVO(
                    byOpenid.getId(),
                    openid,
                    JwtUtil.createJWT(
                            jwtProperties.getUserSecretKey(),
                            jwtProperties.getUserTtl(),
                            claims
                    )
            );
        }

        User user = new User();
        user.setOpenid(openid);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(),
                claims
                );

        return new UserLoginVO(
                user.getId(),
                openid,
                token
        );
    }
}
