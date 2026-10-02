package com.campus.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.recommend.entity.QaPost;
import org.apache.ibatis.annotations.Mapper;

/** 问答帖子表 Mapper */
@Mapper
public interface QaPostMapper extends BaseMapper<QaPost> {
}
