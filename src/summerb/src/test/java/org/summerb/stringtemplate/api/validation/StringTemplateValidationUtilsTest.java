package org.summerb.stringtemplate.api.validation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.summerb.stringtemplate.api.StringTemplateCompiler;
import org.summerb.stringtemplate.impl.StringTemplateCompilerlImpl;
import org.summerb.validation.ValidationContext;

public class StringTemplateValidationUtilsTest {

	@Test
	public void testValidateStringTemplate_blackbox_expectOkForConstant() throws Exception {
		StringTemplateCompiler stringTemplateCompiler = new StringTemplateCompilerlImpl();
		String stringTemplate = "Constant text";
		ValidationContext ctx = new ValidationContext();
		String fieldToken = "fieldToken";

		StringTemplateValidationUtils.validateStringTemplate(stringTemplateCompiler, stringTemplate, ctx, fieldToken);

		assertEquals(false, ctx.getHasErrors());
	}

	@Test
	public void testValidateStringTemplate_blackbox_expectOkForValidExpression() throws Exception {
		StringTemplateCompiler stringTemplateCompiler = new StringTemplateCompilerlImpl();
		String stringTemplate = "Constant text plus ${vars['4444']}";
		ValidationContext ctx = new ValidationContext();
		String fieldToken = "fieldToken";

		StringTemplateValidationUtils.validateStringTemplate(stringTemplateCompiler, stringTemplate, ctx, fieldToken);

		assertEquals(false, ctx.getHasErrors());
	}

	@Test
	public void testValidateStringTemplate_blackbox_expectValidationErrorForWrongExpression() throws Exception {
		StringTemplateCompiler stringTemplateCompiler = new StringTemplateCompilerlImpl();
		String stringTemplate = "Constant text plus ${vars['4444";
		ValidationContext ctx = new ValidationContext();
		String fieldToken = "fieldToken";

		StringTemplateValidationUtils.validateStringTemplate(stringTemplateCompiler, stringTemplate, ctx, fieldToken);

		assertEquals(true, ctx.getHasErrors());
	}

}