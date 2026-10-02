package com.campus.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.recommend.entity.LearningResource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/** 学习资源表 Mapper（含大屏聚合查询） */
@Mapper
public interface LearningResourceMapper extends BaseMapper<LearningResource> {

    /** 资源分类占比（饼图）：按 category_id 分组统计 */
    @Select("""
            SELECT c.id AS category, c.name AS name, COUNT(r.id) AS value
            FROM learning_resource r
            LEFT JOIN resource_category c ON r.category_id = c.id
            GROUP BY c.id, c.name
            ORDER BY value DESC
            """)
    List<Map<String, Object>> categoryDist();

    /** 学院资源量排行（柱状图） */
    @Select("""
            SELECT col.id AS collegeId, col.name AS name, COUNT(r.id) AS value
            FROM learning_resource r
            LEFT JOIN college col ON r.college_id = col.id
            GROUP BY col.id, col.name
            ORDER BY value DESC
            """)
    List<Map<String, Object>> collegeRank();

    /** 资源增长趋势（折线图）：按创建日期分组 */
    @Select("""
            SELECT DATE(create_time) AS dates, COUNT(*) AS counts
            FROM learning_resource
            GROUP BY DATE(create_time)
            ORDER BY dates
            """)
    List<Map<String, Object>> resourceTrend();
}
