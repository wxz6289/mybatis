package com.dk.learn.common.page;

/**
 * 分页请求参数：page 从 1 开始；size 默认 10，最大 100。
 */
public record PageQuery(int page, int size) {

	public static PageQuery of(int page, int size) {
		int p = Math.max(1, page);
		int sz = size < 1 ? 10 : Math.min(100, size);
		return new PageQuery(p, sz);
	}

	public int offset() {
		return (page - 1) * size;
	}
}
