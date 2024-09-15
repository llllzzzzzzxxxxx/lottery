package com.itheima.prize.commons.db.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.prize.commons.db.entity.ViewCardUserHit;
import com.itheima.prize.commons.db.mapper.ViewGameCurinfoMapper;
import com.itheima.prize.commons.db.service.ViewCardUserHitService;
import com.itheima.prize.commons.db.mapper.ViewCardUserHitMapper;
import com.itheima.prize.commons.utils.PageBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
* @author shawn
* @description 针对表【view_card_user_hit】的数据库操作Service实现
* @createDate 2023-12-26 11:58:48
*/
@Service
public class ViewCardUserHitServiceImpl extends ServiceImpl<ViewCardUserHitMapper, ViewCardUserHit>
    implements ViewCardUserHitService{
    @Autowired
    private ViewCardUserHitMapper viewCardUserHitMapper;
    @Autowired
    private ViewGameCurinfoMapper viewGameCurinfoMapper;
    @Override
    public PageBean<ViewCardUserHit> hit(int gameid, int curpage, int limit, Integer id) {
        Page<ViewCardUserHit> page = this.page(new Page<>(curpage, limit), null);
        page = viewCardUserHitMapper.hit(page,gameid,id);
        PageBean<ViewCardUserHit> pageBean = new PageBean<>(page);
        return pageBean;
    }


}




