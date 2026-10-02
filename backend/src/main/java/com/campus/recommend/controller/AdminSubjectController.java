package com.campus.recommend.controller;

import com.campus.recommend.annotation.RequireRole;
import com.campus.recommend.common.Result;
import com.campus.recommend.entity.*;
import com.campus.recommend.service.AdminSubjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 管理端学科数据 CRUD：学院 / 专业 / 课程 / 资源分类（共 16 个接口） */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequireRole("ADMIN")
public class AdminSubjectController {

    @Autowired
    private AdminSubjectService subjectService;

    /* ---------- 学院 college ---------- */
    @GetMapping("/college")
    public Result<List<College>> collegeList() {
        return Result.success(subjectService.collegeList());
    }

    @PostMapping("/college")
    public Result<Void> collegeSave(@RequestBody College c) {
        subjectService.collegeSave(c);
        return Result.success();
    }

    @PutMapping("/college/{id}")
    public Result<Void> collegeUpdate(@PathVariable Long id, @RequestBody College c) {
        c.setId(id);
        subjectService.collegeUpdate(c);
        return Result.success();
    }

    @DeleteMapping("/college/{id}")
    public Result<Void> collegeDelete(@PathVariable Long id) {
        subjectService.collegeDelete(id);
        return Result.success();
    }

    /* ---------- 专业 major ---------- */
    @GetMapping("/major")
    public Result<List<Major>> majorList() {
        return Result.success(subjectService.majorList());
    }

    @PostMapping("/major")
    public Result<Void> majorSave(@RequestBody Major m) {
        subjectService.majorSave(m);
        return Result.success();
    }

    @PutMapping("/major/{id}")
    public Result<Void> majorUpdate(@PathVariable Long id, @RequestBody Major m) {
        m.setId(id);
        subjectService.majorUpdate(m);
        return Result.success();
    }

    @DeleteMapping("/major/{id}")
    public Result<Void> majorDelete(@PathVariable Long id) {
        subjectService.majorDelete(id);
        return Result.success();
    }

    /* ---------- 课程 course ---------- */
    @GetMapping("/course")
    public Result<List<Course>> courseList() {
        return Result.success(subjectService.courseList());
    }

    @PostMapping("/course")
    public Result<Void> courseSave(@RequestBody Course c) {
        subjectService.courseSave(c);
        return Result.success();
    }

    @PutMapping("/course/{id}")
    public Result<Void> courseUpdate(@PathVariable Long id, @RequestBody Course c) {
        c.setId(id);
        subjectService.courseUpdate(c);
        return Result.success();
    }

    @DeleteMapping("/course/{id}")
    public Result<Void> courseDelete(@PathVariable Long id) {
        subjectService.courseDelete(id);
        return Result.success();
    }

    /* ---------- 资源分类 category ---------- */
    @GetMapping("/category")
    public Result<List<ResourceCategory>> categoryList() {
        return Result.success(subjectService.categoryList());
    }

    @PostMapping("/category")
    public Result<Void> categorySave(@RequestBody ResourceCategory c) {
        subjectService.categorySave(c);
        return Result.success();
    }

    @PutMapping("/category/{id}")
    public Result<Void> categoryUpdate(@PathVariable Long id, @RequestBody ResourceCategory c) {
        c.setId(id);
        subjectService.categoryUpdate(c);
        return Result.success();
    }

    @DeleteMapping("/category/{id}")
    public Result<Void> categoryDelete(@PathVariable Long id) {
        subjectService.categoryDelete(id);
        return Result.success();
    }
}
