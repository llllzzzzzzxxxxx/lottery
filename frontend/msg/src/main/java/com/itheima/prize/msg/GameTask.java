package com.itheima.prize.msg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itheima.prize.commons.config.RedisKeys;
import com.itheima.prize.commons.db.entity.*;
import com.itheima.prize.commons.db.service.CardGameProductService;
import com.itheima.prize.commons.db.service.CardGameRulesService;
import com.itheima.prize.commons.db.service.CardGameService;
import com.itheima.prize.commons.db.service.GameLoadService;
import com.itheima.prize.commons.utils.RedisUtil;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 活动信息预热，每隔1分钟执行一次
 * 查找未来1分钟内（含），要开始的活动
 */
@Component
public class GameTask {
    private final static Logger log = LoggerFactory.getLogger(GameTask.class);
    @Autowired
    private CardGameService gameService;
    @Autowired
    private CardGameProductService gameProductService;
    @Autowired
    private CardGameRulesService gameRulesService;
    @Autowired
    private GameLoadService gameLoadService;
    @Autowired
    private RedisUtil redisUtil;

    @Scheduled(cron = "0 * * * * ?")
    public void execute() {
        System.out.printf("scheduled!"+new Date());
        log.info("scheduled!"+new Date());
        gameService.list().forEach(game -> {//查询一分钟内的活动
            if (game.getStatus()==0&&game.getStarttime().getTime()<= System.currentTimeMillis()+60000) {
                List<Long> tokens = new ArrayList<>();
                Random random = new Random();
                long startTime = game.getStarttime().getTime();
                long endTime = game.getEndtime().getTime();
                long l1 = endTime - startTime + 1;
                if (!redisUtil.hasKey(RedisKeys.TOKEN+game.getId())){
                    redisUtil.set(RedisKeys.INFO+game.getId(),game,-1);
                    gameProductService.getByGameId(game.getId()).forEach(product -> {
                    long l = random.nextInt((int) l1) + startTime ;
                    long token = l*1000+(long) random.nextInt(1000);
                    redisUtil.hset(RedisKeys.TOKEN+game.getId(),token+"",product.getId(),-1);
                    redisUtil.set(RedisKeys.PRODUCT + game.getId() + token, product);
                    tokens.add(token);
                });

                gameRulesService.list(new QueryWrapper<CardGameRules>().eq("gameid",game.getId())).forEach(rule -> {
                    redisUtil.hset(RedisKeys.MAXGOAL+game.getId(),rule.getUserlevel()+"",rule.getGoalTimes());
                    redisUtil.hset(RedisKeys.MAXENTER+game.getId(),rule.getUserlevel()+"",rule.getEnterTimes());
                });
                tokens.sort(Comparator.naturalOrder());
                redisUtil.rightPushAll(RedisKeys.TOKENS+game.getId(),tokens);
                }
            }
        });
    }
}
