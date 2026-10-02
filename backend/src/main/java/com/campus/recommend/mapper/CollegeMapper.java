package com.campus.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.recommend.entity.College;
import org.apache.ibatis.annotations.Mapper;

/** 学院表 Mapper */
@Mapper
public interface CollegeMapper extends BaseMapper<College> {
}
