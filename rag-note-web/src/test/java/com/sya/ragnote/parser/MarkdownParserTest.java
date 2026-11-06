package com.sya.ragnote.parser;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MarkdownParserTest {
    @Test
    void retainsCodeTablesLinksAndSourceLines() {
        var parsed =
                new MarkdownParser()
                        .parse(
                                "# 标题\n\n"
                                        + "- item [文档](https://example.com)\n\n"
                                        + "```java\n"
                                        + "int x=1;\n"
                                        + "```\n\n"
                                        + "|a|b|\n"
                                        + "|-|-|\n"
                                        + "|1|2|\n");
        assertEquals("标题", parsed.firstHeading());
        assertTrue(parsed.plainText().contains("int x=1;"));
        assertTrue(parsed.plainText().contains("https://example.com"));
        assertTrue(parsed.plainText().contains("1"));
        assertEquals(1, parsed.sections().get(0).startLine());
    }

    @Test
    void unclosedCodeFenceKeepsLastSourceLine() {
        var parsed = new MarkdownParser().parse("```java\nfirst\nlast");
        assertEquals(2, parsed.sections().get(0).startLine());
        assertEquals(3, parsed.sections().get(0).endLine());
        assertEquals("first\nlast", parsed.sections().get(0).text());
    }
}
