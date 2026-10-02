package com.campus.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.recommend.entity.Neo4jSyncLog;
import org.apache.ibatis.annotations.Mapper;

/** Neo4j 同步日志表 Mapper */
@Mapper
public interface Neo4jSyncLogMapper extends BaseMapper<Neo4jSyncLog> {
}
