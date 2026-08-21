package com.aitechskill.book.document.index;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Markdown 分块规则测试。 */
class MarkdownChunkerTest {

    private final MarkdownChunker chunker = new MarkdownChunker();

    /** 标题应保留在对应片段的 heading 元数据中。 */
    @Test
    void keepsHeadingMetadata() {
        List<MarkdownChunker.Chunk> chunks = chunker.chunk("# 总览\n\n正文一\n\n## 配置\n\n正文二", 200, 0);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).heading()).isEqualTo("总览");
        assertThat(chunks.get(1).heading()).isEqualTo("配置");
    }

    /** 长段落应拆分为不超过上限的多个片段。 */
    @Test
    void splitsLongParagraph() {
        List<MarkdownChunker.Chunk> chunks = chunker.chunk("a".repeat(640), 200, 0);

        assertThat(chunks).hasSize(4);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.content().length()).isLessThanOrEqualTo(200));
    }

    /** 相邻片段应共享配置的尾部上下文。 */
    @Test
    void addsOverlap() {
        List<MarkdownChunker.Chunk> chunks = chunker.chunk("a".repeat(640), 200, 30);

        assertThat(chunks).hasSize(4);
        assertThat(chunks.get(1).content()).startsWith("a".repeat(30));
    }

    /** 空白正文不产生无意义片段。 */
    @Test
    void ignoresBlankMarkdown() {
        assertThat(chunker.chunk(" \n\n", 200, 30)).isEmpty();
    }
}
