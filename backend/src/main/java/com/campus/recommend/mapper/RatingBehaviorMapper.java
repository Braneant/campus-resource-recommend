package com.campus.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.recommend.entity.RatingBehavior;
import org.apache.ibatis.annotations.Mapper;

/** 评分行为表 Mapper（ItemCF 显式数据源） */
@Mapper
public interface RatingBehaviorMapper extends BaseMapper<RatingBehavior> {
}
