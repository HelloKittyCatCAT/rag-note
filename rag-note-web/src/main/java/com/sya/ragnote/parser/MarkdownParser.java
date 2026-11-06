package com.sya.ragnote.parser;

import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.*;
import org.commonmark.parser.IncludeSourceSpans;
import org.commonmark.parser.Parser;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MarkdownParser {
    public record Section(String headingPath, String text, int startLine, int endLine) {}

    public record Parsed(String firstHeading, String plainText, List<Section> sections) {}

    private final Parser parser =
            Parser.builder()
                    .extensions(List.of(TablesExtension.create()))
                    .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
                    .build();

    public Parsed parse(String markdown) {
        Node root = parser.parse(markdown);
        List<Section> sections = new ArrayList<>();
        String[] headings = new String[6];
        String first = null;
        StringBuilder all = new StringBuilder();
        for (Node block = root.getFirstChild(); block != null; block = block.getNext()) {
            String text = text(block);
            if (block instanceof FencedCodeBlock || block instanceof IndentedCodeBlock) {
                if (text.endsWith("\n")) text = text.substring(0, text.length() - 1);
            } else {
                text = text.strip();
            }
            if (text.isBlank()) continue;
            if (block instanceof Heading h) {
                if (h.getLevel() == 1 && first == null) first = text;
                headings[h.getLevel() - 1] = text;
                for (int i = h.getLevel(); i < 6; i++) headings[i] = null;
            }
            String path =
                    String.join(" > ", Arrays.stream(headings).filter(Objects::nonNull).toList());
            var spans = block.getSourceSpans();
            int start = spans.stream().mapToInt(s -> s.getLineIndex() + 1).min().orElse(1);
            int end = spans.stream().mapToInt(s -> s.getLineIndex() + 1).max().orElse(start);
            if (block instanceof FencedCodeBlock code) {
                start++;
                if (code.getClosingFenceLength() != null && code.getClosingFenceLength() > 0) end--;
                end = Math.max(start, end);
            }
            sections.add(new Section(path, text, start, end));
            if (!all.isEmpty()) all.append('\n');
            all.append(text);
        }
        return new Parsed(first, all.toString(), List.copyOf(sections));
    }

    private String text(Node node) {
        if (node instanceof HtmlBlock || node instanceof HtmlInline) return "";
        if (node instanceof Text t) return t.getLiteral();
        if (node instanceof Code c) return c.getLiteral();
        if (node instanceof FencedCodeBlock c) return c.getLiteral();
        if (node instanceof IndentedCodeBlock c) return c.getLiteral();
        if (node instanceof SoftLineBreak || node instanceof HardLineBreak) return "\n";
        StringBuilder out = new StringBuilder();
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            String value = text(child);
            if (value.isEmpty()) continue;
            if (!out.isEmpty()) {
                if (child instanceof org.commonmark.ext.gfm.tables.TableCell) out.append(" | ");
                else if (child instanceof Block
                        || child instanceof org.commonmark.ext.gfm.tables.TableRow)
                    out.append('\n');
            }
            out.append(value);
        }
        if (node instanceof Link link) out.append(" (").append(link.getDestination()).append(')');
        if (node instanceof Image image)
            out.append(" (").append(image.getDestination()).append(')');
        return out.toString();
    }
}
