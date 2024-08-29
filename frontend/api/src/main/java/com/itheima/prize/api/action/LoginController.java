package com.itheima.prize.api.action;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itheima.prize.commons.config.RedisKeys;
import com.itheima.prize.commons.db.entity.CardUser;
import com.itheima.prize.commons.db.mapper.CardUserMapper;
import com.itheima.prize.commons.db.service.CardUserService;
import com.itheima.prize.commons.utils.ApiResult;
import com.itheima.prize.commons.utils.PasswordUtil;
import com.itheima.prize.commons.utils.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping(value = "/api")
@Api(tags = {"登录模块"})
public class LoginController {
    @Autowired
    private CardUserService userService;

    @Autowired
    private RedisUtil redisUtil;

    @PostMapping("/login")
    @ApiOperation(value = "登录")
    @ApiImplicitParams({
            @ApiImplicitParam(name="account",value = "用户名",required = true),
            @ApiImplicitParam(name="password",value = "密码",required = true)
    })
    public ApiResult login(HttpServletRequest request, @RequestParam String account,@RequestParam String password) {
        if (redisUtil.hasKey("lockAccount:"+account)){
            long expire = redisUtil.getExpire("lockAccount:" + account);
            if(expire>0){
                return new ApiResult(400,"尝试次数过多，请"+expire+"秒后重试",null);
            }else {
                redisUtil.del("lockAccount:"+account);
            }
        }
        CardUser user = userService.login(account,password);
        if (user ==null){
            if(!redisUtil.hasKey("errorTimes:"+account)){
                redisUtil.set("errorTimes:"+account,1);
            }else {
                redisUtil.incr("errorTimes:"+account,1);
            }
            if((int)redisUtil.get("errorTimes:"+account)>=5){
                redisUtil.del("errorTimes:"+account);
                redisUtil.set("lockAccount:"+account,1);
                redisUtil.expire("lockAccount:"+account,300);
                return new ApiResult(400,"密码错误5次，请5分钟之后尝试",null);
            }
            return new ApiResult(400,"用户名或密码错误",null);
        }
        return new ApiResult(200,"登录成功",user);
    }

    @GetMapping("/logout")
    @ApiOperation(value = "退出")
    public ApiResult logout(HttpServletRequest request) {
        return new ApiResult<>(200,"退出成功",null);
    }

}