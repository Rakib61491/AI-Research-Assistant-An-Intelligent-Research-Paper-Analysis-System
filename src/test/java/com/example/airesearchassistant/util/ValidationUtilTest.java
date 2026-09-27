package com.example.airesearchassistant.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationUtilTest {

    @Test
    public void testIsValidTitle() {
        assertTrue(ValidationUtil.isValidTitle("Attention Is All You Need"));
        assertFalse(ValidationUtil.isValidTitle(""));
        assertFalse(ValidationUtil.isValidTitle("   "));
        assertFalse(ValidationUtil.isValidTitle(null));
    }

    @Test
    public void testIsValidYear() {
        assertTrue(ValidationUtil.isValidYear("2024"));
        assertTrue(ValidationUtil.isValidYear("1998"));
        assertTrue(ValidationUtil.isValidYear(""));     // Optional field
        assertTrue(ValidationUtil.isValidYear(null));   // Optional field
        assertFalse(ValidationUtil.isValidYear("24"));  // Must be 4 digits
        assertFalse(ValidationUtil.isValidYear("20244"));
        assertFalse(ValidationUtil.isValidYear("abcd"));
        assertFalse(ValidationUtil.isValidYear("1700")); // Out of realistic range
    }

    @Test
    public void testIsPositiveInteger() {
        assertTrue(ValidationUtil.isPositiveInteger("1"));
        assertTrue(ValidationUtil.isPositiveInteger("100"));
        assertFalse(ValidationUtil.isPositiveInteger("0"));
        assertFalse(ValidationUtil.isPositiveInteger("-5"));
        assertFalse(ValidationUtil.isPositiveInteger("abc"));
        assertFalse(ValidationUtil.isPositiveInteger(null));
    }

    @Test
    public void testIsRequired() {
        assertTrue(ValidationUtil.isRequired("Deep Learning"));
        assertFalse(ValidationUtil.isRequired(""));
        assertFalse(ValidationUtil.isRequired("   "));
        assertFalse(ValidationUtil.isRequired(null));
    }

    @Test
    public void testIsValidDoi() {
        assertTrue(ValidationUtil.isValidDoi("10.1145/3377325.3377498"));
        assertTrue(ValidationUtil.isValidDoi("10.1000/182"));
        assertTrue(ValidationUtil.isValidDoi(""));     // Optional field
        assertTrue(ValidationUtil.isValidDoi(null));   // Optional field
        assertFalse(ValidationUtil.isValidDoi("not-a-doi"));
        assertFalse(ValidationUtil.isValidDoi("11.1234/test"));
    }
}
