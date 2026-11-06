package com.sya.ragnote.vo.common;

import java.util.List;

/** 分页结果，页码从1开始。 */
public record PageVo<T>(List<T> records, long total, int page, int size) {
    public PageVo {
        records = List.copyOf(records);
    }
}
