package com.campus.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.recommend.entity.Neo4jSyncLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

/** Neo4j 同步日志表 Mapper */
@Mapper
public interface Neo4jSyncLogMapper extends BaseMapper<Neo4jSyncLog> {
    @Select("SELECT * FROM neo4j_sync_log WHERE id = #{id} FOR UPDATE")
    Neo4jSyncLog lockById(@Param("id") Long id);
}
