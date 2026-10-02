package com.campus.recommend.common;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 分页统一出参 {list, total, pageNum, pageSize}
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    private List<T> list;
    /** 总记录数 */
    private long total;
    /** 当前页 */
    private int pageNum;
    /** 每页条数 */
    private int pageSize;

    public PageResult() {
    }

    public PageResult(List<T> list, long total, int pageNum, int pageSize) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }
}
