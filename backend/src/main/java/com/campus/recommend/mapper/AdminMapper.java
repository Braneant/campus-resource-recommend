package com.campus.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.recommend.entity.Admin;
import org.apache.ibatis.annotations.Mapper;

/** 管理员表 Mapper */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
}
