package com.campus.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.recommend.entity.StudentUser;
import org.apache.ibatis.annotations.Mapper;

/** 学生用户表 Mapper */
@Mapper
public interface StudentUserMapper extends BaseMapper<StudentUser> {
}
