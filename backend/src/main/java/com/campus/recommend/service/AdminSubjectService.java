package com.campus.recommend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.recommend.common.BusinessException;
import com.campus.recommend.common.PageResult;
import com.campus.recommend.common.ResultCode;
import com.campus.recommend.entity.*;
import com.campus.recommend.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理端学科数据 CRUD：学院 / 专业 / 课程 / 资源分类
 */
@Slf4j
@Service
public class AdminSubjectService {

    @Autowired
    private CollegeMapper collegeMapper;
    @Autowired
    private MajorMapper majorMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private ResourceCategoryMapper categoryMapper;

    /* ---------- 学院 ---------- */
    public List<College> collegeList() {
        return collegeMapper.selectList(new LambdaQueryWrapper<College>().orderByAsc(College::getSort));
    }

    public College collegeGet(Long id) {
        return collegeMapper.selectById(id);
    }

    public void collegeSave(College c) {
        collegeMapper.insert(c);
    }

    public void collegeUpdate(College c) {
        if (collegeMapper.selectById(c.getId()) == null) throw new BusinessException(ResultCode.NOT_FOUND);
        collegeMapper.updateById(c);
    }

    public void collegeDelete(Long id) {
        // 校验无下属专业
        long cnt = majorMapper.selectCount(new LambdaQueryWrapper<Major>().eq(Major::getCollegeId, id));
        if (cnt > 0) throw new BusinessException(ResultCode.PARAM_ERROR, "该学院下存在专业，无法删除");
        collegeMapper.deleteById(id);
    }

    /* ---------- 专业 ---------- */
    public List<Major> majorList() {
        return majorMapper.selectList(null);
    }

    public Major majorGet(Long id) {
        return majorMapper.selectById(id);
    }

    public void majorSave(Major m) {
        majorMapper.insert(m);
    }

    public void majorUpdate(Major m) {
        if (majorMapper.selectById(m.getId()) == null) throw new BusinessException(ResultCode.NOT_FOUND);
        majorMapper.updateById(m);
    }

    public void majorDelete(Long id) {
        majorMapper.deleteById(id);
    }

    /* ---------- 课程 ---------- */
    public List<Course> courseList() {
        return courseMapper.selectList(null);
    }

    public Course courseGet(Long id) {
        return courseMapper.selectById(id);
    }

    public void courseSave(Course c) {
        courseMapper.insert(c);
    }

    public void courseUpdate(Course c) {
        if (courseMapper.selectById(c.getId()) == null) throw new BusinessException(ResultCode.NOT_FOUND);
        courseMapper.updateById(c);
    }

    public void courseDelete(Long id) {
        courseMapper.deleteById(id);
    }

    /* ---------- 资源分类 ---------- */
    public List<ResourceCategory> categoryList() {
        return categoryMapper.selectList(null);
    }

    public ResourceCategory categoryGet(Long id) {
        return categoryMapper.selectById(id);
    }

    public void categorySave(ResourceCategory c) {
        categoryMapper.insert(c);
    }

    public void categoryUpdate(ResourceCategory c) {
        if (categoryMapper.selectById(c.getId()) == null) throw new BusinessException(ResultCode.NOT_FOUND);
        categoryMapper.updateById(c);
    }

    public void categoryDelete(Long id) {
        categoryMapper.deleteById(id);
    }
}
