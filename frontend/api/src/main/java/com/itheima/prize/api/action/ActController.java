package com.itheima.prize.api.action;

import com.alibaba.fastjson.JSON;
import com.itheima.prize.api.config.LuaScript;
import com.itheima.prize.commons.config.RabbitKeys;
import com.itheima.prize.commons.config.RedisKeys;
import com.itheima.prize.commons.db.entity.*;
import com.itheima.prize.commons.db.mapper.CardGameMapper;
import com.itheima.prize.commons.db.service.CardGameService;
import com.itheima.prize.commons.utils.ApiResult;
import com.itheima.prize.commons.utils.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/act")
@Api(tags = {"抽奖模块"})
public class ActController {

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private LuaScript luaScript;

    static final int default_max_enter = 10;
    static final int default_max_goal = 10;
    @GetMapping("/limits/{gameid}")
    @ApiOperation(value = "剩余次数")
    @ApiImplicitParams({
            @ApiImplicitParam(name="gameid",value = "活动id",example = "1",required = true)
    })
    public ApiResult<Object> limits(@PathVariable int gameid, HttpServletRequest request){
        //获取活动基本信息
        CardGame game = (CardGame) redisUtil.get(RedisKeys.INFO+gameid);
        if (game == null){
            return new ApiResult<>(-1,"活动未加载",null);
        }
        //获取当前用户
        HttpSession session = request.getSession();
        CardUser user = (CardUser) session.getAttribute("user");
        if (user == null){
            return new ApiResult(-1,"未登陆",null);
        }
        //用户可抽奖次数
        Integer enter = (Integer) redisUtil.get(RedisKeys.USERENTER+gameid+"_"+user.getId());
        if (enter == null){
            enter = 0;
        }
        //根据会员等级，获取本活动允许的最大抽奖次数
        Integer maxenter = (Integer) redisUtil.hget(RedisKeys.MAXENTER+gameid,user.getLevel()+"");
        //如果没设置，默认为0，即：不限制次数
        maxenter = maxenter==null ? 0 : maxenter;

        //用户已中奖次数
        Integer count = (Integer) redisUtil.get(RedisKeys.USERHIT+gameid+"_"+user.getId());
        if (count == null){
            count = 0;
        }
        //根据会员等级，获取本活动允许的最大中奖数
        Integer maxcount = (Integer) redisUtil.hget(RedisKeys.MAXGOAL+gameid,user.getLevel()+"");
        //如果没设置，默认为0，即：不限制次数
        maxcount = maxcount==null ? 0 : maxcount;

        //幸运转盘类，先给用户随机剔除，再获取令牌，有就中，没有就说明抢光了
        //一般这种情况会设置足够的商品，卡在随机上
        Integer randomRate = (Integer) redisUtil.hget(RedisKeys.RANDOMRATE+gameid,user.getLevel()+"");
        if (randomRate == null){
            randomRate = 100;
        }

        Map map = new HashMap();
        map.put("maxenter",maxenter);
        map.put("enter",enter);
        map.put("maxcount",maxcount);
        map.put("count",count);
        map.put("randomRate",randomRate);

        return new ApiResult<>(1,"成功",map);
    }
    @GetMapping("/go/{gameid}")
    @ApiOperation(value = "抽奖")
    @ApiImplicitParams({
            @ApiImplicitParam(name="gameid",value = "活动id",example = "1",required = true)
    })
    public ApiResult<Object> act(@PathVariable int gameid, HttpServletRequest request){
        //TODO
        CardGame cardGame = (CardGame) redisUtil.get(RedisKeys.INFO + gameid);
        if (cardGame.getStarttime().getTime()>= new Date().getTime()||cardGame==null)return new ApiResult<>(-1,"活动未开始",null);
        if (cardGame.getEndtime().getTime()<= new Date().getTime())return new ApiResult<>(-1,"活动已结束",null);
        CardUser user = (CardUser) request.getSession().getAttribute("user");
        Integer userLevel = user.getLevel();
        if (userLevel == null)return new ApiResult<>(-1,"未登陆",null);
        Integer maxGoal = (Integer) redisUtil.hget(RedisKeys.MAXGOAL + gameid, userLevel.toString());
        Integer maxEnter = (Integer) redisUtil.hget(RedisKeys.MAXENTER + gameid, userLevel.toString());
        if (maxGoal==null)maxGoal=default_max_goal;
        if (maxEnter==null)maxEnter=default_max_enter;
        if (!redisUtil.hasKey(RedisKeys.USERENTER+gameid+"_"+user.getId()))redisUtil.set(RedisKeys.USERENTER+gameid+"_"+user.getId(),maxEnter);
        if (!redisUtil.hasKey(RedisKeys.USERHIT+gameid+"_"+user.getId()))redisUtil.set(RedisKeys.USERHIT+gameid+"_"+user.getId(),maxGoal);
        Integer userEnter =(Integer) redisUtil.get(RedisKeys.USERENTER + gameid + "_" + user.getId());
        Integer userHit = (Integer) redisUtil.get(RedisKeys.USERHIT+gameid+"_"+user.getId());
        if (userEnter == null || userEnter <= 0)return new ApiResult<>(-1,"抽奖次数已用完",null);
        if (userHit == null || userHit <= 0)return new ApiResult<>(-1,"已到达最大中奖次数",null);
        Long token = luaScript.tokenCheck(RedisKeys.TOKENS + gameid, String.valueOf(new Date().getTime()));
        redisUtil.decr(RedisKeys.USERENTER + gameid + "_" + user.getId(), 1);
        if (token == 0){
            return new ApiResult<>(-1,"奖品已抽光",null);
        }else if (token == 1){
            return new ApiResult<>(-1,"未中奖",null);
        }
        CardProduct prize = (CardProduct) redisUtil.get(RedisKeys.PRODUCT + gameid + token);
        redisUtil.decr(RedisKeys.USERHIT + gameid + "_" + user.getId(), 1);
        return new ApiResult<>(1,"恭喜中奖",prize);
    }

    @GetMapping("/info/{gameid}")
    @ApiOperation(value = "缓存信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name="gameid",value = "活动id",example = "1",required = true)
    })
    public ApiResult info(@PathVariable int gameid){
        Map map = new LinkedHashMap();
        Map maxenter = new LinkedHashMap();
        Map maxgoal = new LinkedHashMap();
        redisTemplate.opsForHash().entries(RedisKeys.MAXENTER + gameid).forEach((k,v)->{
            maxenter.put(k,v);
        });
        redisTemplate.opsForHash().entries(RedisKeys.MAXGOAL + gameid).forEach((k,v)->{
           maxgoal.put(k,v);
        });
        Map<String, CardGameProduct> products = new LinkedHashMap<>();
        CardGame gameInfo = (CardGame) redisUtil.get(RedisKeys.INFO + gameid);
        List<Long> tokens = redisTemplate.opsForList().range(RedisKeys.TOKENS + gameid, 0, -1);
        for (Long token:tokens) {
            long l = token / 1000;
            Date randomDate = new Date(l);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String formattedDate = sdf.format(randomDate);
            CardGameProduct product = (CardGameProduct) redisUtil.get(RedisKeys.PRODUCT+gameid+ token);
            products.put(formattedDate, product);
        }
        map.put("gameInfo",gameInfo);
        map.put("maxenter",maxenter);
        map.put("maxgoal",maxgoal);
        map.put("gametokens",products);
        return new ApiResult<>(200,"缓存信息",map);
    }
}
