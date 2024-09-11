package com.itheima.prize.commons.db.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.prize.commons.db.entity.CardGame;
import com.itheima.prize.commons.db.entity.CardGameProduct;
import com.itheima.prize.commons.db.entity.CardProductDto;
import com.itheima.prize.commons.db.service.CardGameProductService;
import com.itheima.prize.commons.db.service.CardGameService;
import com.itheima.prize.commons.db.mapper.CardGameMapper;
import com.itheima.prize.commons.utils.ApiResult;
import com.itheima.prize.commons.utils.PageBean;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author shawn
* @description 针对表【card_game】的数据库操作Service实现
* @createDate 2023-12-26 11:58:48
*/
@Service
public class CardGameServiceImpl extends ServiceImpl<CardGameMapper, CardGame>
    implements CardGameService{
    private CardGameProductService cardGameProductService;

    @Override
    public ApiResult getlist(int status, int curpage, int limit) {
        Page<CardGame> page = new Page<>();
        QueryWrapper<CardGame> gameQueryWrapper= new QueryWrapper<>();
        if (status == -1){
            gameQueryWrapper.like("status","");
        }
        else if (status != -1){
            gameQueryWrapper.eq("status",status);
        }
        IPage<CardGame> result = page(page,gameQueryWrapper);
        PageBean<CardGame> pageBean = new PageBean<>(curpage,limit,result.getTotal(),result.getRecords());
        return new ApiResult<>(1,"成功",pageBean);
    }


}




