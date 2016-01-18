package org.summerb.microservices.properties.impl;

import java.util.List;
import java.util.Map;

import org.summerb.microservices.properties.api.PropertyService;
import org.summerb.microservices.properties.api.SimplifiedPropertyService;
import org.summerb.microservices.properties.api.dto.NamedProperty;

/**
 * This wrapper will simplify calls to same app+domain properties. WIll not
 * require to pass them every time they needed
 * 
 * @author skarpushin
 * 
 */
public class PropertyServiceWrapper implements SimplifiedPropertyService {
	private final PropertyService propertyService;
	private final String appName;
	private final String domainName;

	public PropertyServiceWrapper(PropertyService propertyService, String appName, String domainName) {
		this.propertyService = propertyService;
		this.appName = appName;
		this.domainName = domainName;
	}

	@Override
	public void putSubjectProperty(String subjectId, String propertyName, String propertyValue) {
		propertyService.putSubjectProperty(appName, domainName, subjectId, propertyName, propertyValue);
	}

	@Override
	public void putSubjectProperties(String subjectId, List<NamedProperty> namedProperties) {
		propertyService.putSubjectProperties(appName, domainName, subjectId, namedProperties);
	}

	@Override
	public void putSubjectsProperty(List<String> subjectsIds, String name, String value) {
		propertyService.putSubjectsProperty(appName, domainName, subjectsIds, name, value);
	}

	@Override
	public String findSubjectProperty(String subjectId, String propertyName) {
		return propertyService.findSubjectProperty(appName, domainName, subjectId, propertyName);
	}

	@Override
	public Map<String, String> findSubjectProperties(String subjectId) {
		return propertyService.findSubjectProperties(appName, domainName, subjectId);
	}

	@Override
	public Map<String, Map<String, String>> findSubjectsProperties(List<String> subjectsIds) {
		return propertyService.findSubjectsProperties(appName, domainName, subjectsIds);
	}

	@Override
	public Map<String, String> findSubjectsProperty(List<String> subjectsIds, String propertyName) {
		return propertyService.findSubjectsProperty(appName, domainName, subjectsIds, propertyName);
	}

	@Override
	public void deleteSubjectProperties(String subjectId) {
		propertyService.deleteSubjectProperties(appName, domainName, subjectId);
	}

	@Override
	public void deleteSubjectsProperties(List<String> subjectsIds) {
		propertyService.deleteSubjectsProperties(appName, domainName, subjectsIds);
	}

	public PropertyService getPropertyService() {
		return propertyService;
	}
}
