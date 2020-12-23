package com.cbl.spring.gateway.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
public class XssStringValidatorTest {
   // XssStringValidator xssStringValidator = new XssStringValidator();
    @Test
    public  void shouldCheckForScriptFlagAndReturnFalseForTheStringContainsScriptTag(){
        XssStringValidator xssStringValidator = new XssStringValidator();
        boolean validOutPut = xssStringValidator.isGoodHtmlString("<script>console.log(1)</script>");
        assertFalse(validOutPut);
    }
    @Test
    public  void shouldCheckGoodHtmlStringAndReturnTrueForTheStringContainsNoBadCharacter(){
        XssStringValidator xssStringValidator = new XssStringValidator();
        boolean validOutPut = xssStringValidator.isGoodHtmlString("console.log(1)");
        assertTrue(validOutPut);
    }
}
