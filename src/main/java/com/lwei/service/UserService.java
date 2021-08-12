package com.lwei.service;

import com.lwei.error.BusinessException;
import com.lwei.service.model.UserModel;
import org.springframework.stereotype.Repository;

/**
 * Created by hzllb on 2018/11/11.
 */
@Repository
public interface UserService {
    //通过用户ID获取用户对象的方法
    UserModel getUserById(Integer id);

    // 通过缓存获取用户对象
    UserModel getUserByIdInCache(Integer id);

    void register(UserModel userModel) throws BusinessException;

    /*
    telphone:用户注册手机
    password:用户加密后的密码
     */
    UserModel validateLogin(String telphone, String encrptPassword) throws BusinessException;
}
