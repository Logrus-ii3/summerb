package org.summerb.approaches.jdbccrud.api.dto;

import com.google.common.base.Preconditions;

/**
 * Narrow case of pagination params when we're interested only in top N records.
 * In this case we improve performance by skipping retrieval of totalResults
 * 
 * @author sergeyk
 *
 */
public class Top extends PagerParams {
	private static final long serialVersionUID = 5201858426665248240L;

	public Top() {

	}

	public Top(long max) {
		super(0, max);
	}

	public static boolean is(PagerParams pagerParams) {
		return pagerParams instanceof Top;
	}

	@Override
	public void setOffset(long offset) {
		Preconditions.checkArgument(offset == 0, "For top-based queries only 0 offset is allowed");
	}
}
