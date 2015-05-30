package org.summerb.easycrud.api;

import org.summerb.easycrud.api.dto.PagerParams;
import org.summerb.easycrud.api.dto.PaginatedList;
import org.summerb.easycrud.api.exceptions.EntityNotFoundException;
import org.summerb.easycrud.api.query.OrderBy;
import org.summerb.easycrud.api.query.Query;
import org.summerb.security.api.exceptions.NotAuthorizedException;
import org.summerb.validation.FieldValidationException;

/**
 * This service is intended for use with DTO's which logic is damn simple
 * (simple dictionaries, surveys, etc..). Avoid using it for complicated
 * business logic to avoid bull shit and spaghetti code. Better avoid it for
 * core functionality which might greatly evolve.
 * 
 * @author sergey.karpushin
 *
 * @param <TId>
 *            type of primary key
 * @param <TDto>
 *            type of dto
 */
public interface EasyCrudService<TId, TDto> {
	TDto create(TDto dto) throws FieldValidationException, NotAuthorizedException;

	TDto update(TDto dto) throws FieldValidationException, NotAuthorizedException, EntityNotFoundException;

	TDto findById(TId id) throws NotAuthorizedException;

	TDto findOneByQuery(Query query) throws NotAuthorizedException;

	PaginatedList<TDto> query(PagerParams pagerParams, Query optionalQuery, OrderBy... orderBy)
			throws NotAuthorizedException;

	void deleteById(TId id) throws NotAuthorizedException, EntityNotFoundException;

	void deleteByIdOptimistic(TId id, long modifiedAt) throws NotAuthorizedException, EntityNotFoundException;

	int deleteByQuery(Query query) throws NotAuthorizedException;

	Class<TDto> getDtoClass();
}