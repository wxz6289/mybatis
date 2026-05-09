package com.dk.learn.common.page;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

	private List<T> records;
	private long total;
	private int page;
	private int size;
	/** 总页数 */
	private long pages;

	public static <T> PageResult<T> of(List<T> records, long total, PageQuery query) {
		int size = query.size();
		long pages = total == 0 ? 0 : (total + size - 1) / size;
		return new PageResult<>(records, total, query.page(), size, pages);
	}
}
