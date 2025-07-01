package com.backend.softtrainer.utils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class WordCountValidator implements ConstraintValidator<WordCount, String> {
    
    private int maxWords;
    private int minWords;
    
    @Override
    public void initialize(WordCount constraintAnnotation) {
        this.maxWords = constraintAnnotation.max();
        this.minWords = constraintAnnotation.min();
    }
    
    @Override
    public boolean isValid(String text, ConstraintValidatorContext context) {
        if (text == null || text.trim().isEmpty()) {
            return minWords == 0; // Allow empty if min is 0
        }
        
        // Count words by splitting on whitespace and filtering out empty strings
        String[] words = text.trim().split("\\s+");
        int wordCount = 0;
        
        for (String word : words) {
            if (!word.trim().isEmpty()) {
                wordCount++;
            }
        }
        
        return wordCount >= minWords && wordCount <= maxWords;
    }
} 