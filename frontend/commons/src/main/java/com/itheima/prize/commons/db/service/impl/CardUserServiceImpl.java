package com.itheima.prize.commons.db.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.prize.commons.db.entity.CardUser;
import com.itheima.prize.commons.db.service.CardUserService;
import com.itheima.prize.commons.db.mapper.CardUserMapper;
import com.itheima.prize.commons.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
* @author shawn
* @description 针对表【card_user(会员信息表)】的数据库操作Service实现
* @createDate 2023-12-26 11:58:48
*/
@Service
public class CardUserServiceImpl extends ServiceImpl<CardUserMapper, CardUser>
    implements CardUserService{

    @Autowired
    private CardUserMapper cardUserMapper;
    @Override
    public CardUser login(String account, String passwrod) {
        String passwd = PasswordUtil.encodePassword(passwrod);
        CardUser user = cardUserMapper.login(account, passwd);
        if (user != null){
            user.setPasswd(null);
            return user;
        }
        return null;
    }
}




