package com.itheima.prize.commons.db.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.prize.commons.db.entity.ViewCardUserHit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author shawn
* @description 针对表【view_card_user_hit】的数据库操作Mapper
* @createDate 2023-12-26 11:58:48
* @Entity com.itheima.prize.commons.db.entity.ViewCardUserHit
*/
public interface ViewCardUserHitMapper extends BaseMapper<ViewCardUserHit> {

    Page<ViewCardUserHit> hit(Page<ViewCardUserHit> page, int gameid, Integer userid);
}




