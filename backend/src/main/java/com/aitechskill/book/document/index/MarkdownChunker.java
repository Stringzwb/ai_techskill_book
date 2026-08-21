package com.aitechskill.book.document.index;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** 将 Markdown 按标题和段落切成稳定、可重复生成的检索片段。 */
@Component
public class MarkdownChunker {

    private static final Pattern HEADING = Pattern.compile("^#{1,6}\\s+(.+?)\\s*$");

    /** 分块并为相邻片段保留有限字符重叠。 */
    public List<Chunk> chunk(String markdown, int maxChars, int overlap) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }
        int safeMax = Math.max(maxChars, 200);
        int safeOverlap = Math.max(0, Math.min(overlap, safeMax / 3));
        List<RawChunk> rawChunks = new ArrayList<>();
        String heading = "";
        StringBuilder block = new StringBuilder();
        boolean headingOnly = false;
        for (String line : markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
            Matcher matcher = HEADING.matcher(line);
            if (matcher.matches()) {
                if (block.length() > 0) {
                    addBlock(rawChunks, heading, block.toString(), safeMax);
                    block.setLength(0);
                }
                heading = matcher.group(1).trim();
                block.append(line.trim());
                headingOnly = true;
            } else if (line.isBlank()) {
                if (block.length() > 0 && !headingOnly) {
                    addBlock(rawChunks, heading, block.toString(), safeMax);
                    block.setLength(0);
                }
            } else {
                if (block.length() > 0) {
                    block.append('\n');
                }
                block.append(line);
                headingOnly = false;
            }
        }
        if (block.length() > 0) {
            addBlock(rawChunks, heading, block.toString(), safeMax);
        }
        List<Chunk> chunks = new ArrayList<>(rawChunks.size());
        for (int index = 0; index < rawChunks.size(); index++) {
            RawChunk raw = rawChunks.get(index);
            String content = raw.content();
            if (index > 0 && safeOverlap > 0) {
                String previous = rawChunks.get(index - 1).content();
                String prefix = suffixByCodePoints(previous, safeOverlap);
                int room = safeMax - prefix.length() - 1;
                if (room > 0) {
                    content = prefix + "\n" + content.substring(0, Math.min(room, content.length()));
                }
            }
            chunks.add(new Chunk(index, raw.heading(), content));
        }
        return List.copyOf(chunks);
    }

    private void addBlock(List<RawChunk> chunks, String heading, String block, int maxChars) {
        String normalized = block.trim();
        if (normalized.isEmpty()) {
            return;
        }
        for (int start = 0; start < normalized.length();) {
            int end = normalized.offsetByCodePoints(start, Math.min(maxChars, normalized.codePointCount(start, normalized.length())));
            chunks.add(new RawChunk(heading, normalized.substring(start, end)));
            start = end;
        }
    }

    private String suffixByCodePoints(String value, int count) {
        int codePoints = value.codePointCount(0, value.length());
        int start = value.offsetByCodePoints(0, Math.max(0, codePoints - count));
        return value.substring(start);
    }

    public record Chunk(int index, String heading, String content) {
    }

    private record RawChunk(String heading, String content) {
    }
}
