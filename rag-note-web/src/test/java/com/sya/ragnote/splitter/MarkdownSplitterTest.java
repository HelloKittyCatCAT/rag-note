package com.sya.ragnote.splitter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sya.ragnote.config.RagProperties;
import com.sya.ragnote.parser.MarkdownParser;
import com.sya.ragnote.tokenizer.TokenCounter;

import org.junit.jupiter.api.Test;

class MarkdownSplitterTest {
    @Test
    void longUnicodeLineSplitsWithinBudgetAndKeepsOverlap() {
        var tokens = mock(TokenCounter.class);
        when(tokens.count(anyString()))
                .thenAnswer(
                        i -> {
                            String s = i.getArgument(0);
                            return s.codePointCount(0, s.length());
                        });
        var splitter = new MarkdownSplitter(tokens, new RagProperties());
        String row = "😀".repeat(1500);
        var chunks = splitter.split(new MarkdownParser().parse("# Section\n" + row));
        assertTrue(chunks.size() > 2);
        for (var c : chunks) {
            assertTrue(c.tokenCount() <= 600);
            assertFalse(Character.isHighSurrogate(c.content().charAt(c.content().length() - 1)));
            assertTrue(c.startLine() >= 1);
            assertTrue(c.endLine() >= c.startLine());
        }
        String first = chunks.get(1).content(), second = chunks.get(2).content();
        String suffix = first.substring(first.offsetByCodePoints(first.length(), -80));
        assertTrue(second.startsWith(suffix));
    }

    @Test
    void headingBoundaryDoesNotReusePreviousSection() {
        var tokens = mock(TokenCounter.class);
        when(tokens.count(anyString())).thenAnswer(i -> ((String) i.getArgument(0)).length());
        var chunks =
                new MarkdownSplitter(tokens, new RagProperties())
                        .split(new MarkdownParser().parse("# Alpha\nold\n# Beta\nnew"));
        assertEquals(2, chunks.size());
        assertFalse(chunks.get(1).content().contains("old"));
        assertEquals("Beta", chunks.get(1).headingPath());
    }

    @Test
    void codeIndentationAndInternalBlankLinesSurviveChunking() {
        var tokens = mock(TokenCounter.class);
        when(tokens.count(anyString())).thenAnswer(i -> ((String) i.getArgument(0)).length());
        var chunks =
                new MarkdownSplitter(tokens, new RagProperties())
                        .split(new MarkdownParser().parse("```java\n  first\n\n  last\n```"));
        assertEquals(1, chunks.size());
        assertEquals("  first\n\n  last", chunks.get(0).content());
        assertEquals(2, chunks.get(0).startLine());
        assertEquals(4, chunks.get(0).endLine());
    }
}
