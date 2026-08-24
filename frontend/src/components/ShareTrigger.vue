<script setup lang="ts">
import { computed, ref } from "vue";
import { Check, Share2 } from "@lucide/vue";

const props = defineProps<{ title: string; path: string }>();
const copied = ref(false);
let clearFeedback: number | undefined;

const shareUrl = computed(() =>
  new URL(props.path, window.location.origin).toString(),
);

async function share() {
  try {
    if (navigator.share) {
      await navigator.share({ title: props.title, url: shareUrl.value });
      return;
    }
    await navigator.clipboard.writeText(shareUrl.value);
    copied.value = true;
    window.clearTimeout(clearFeedback);
    clearFeedback = window.setTimeout(() => {
      copied.value = false;
    }, 1800);
  } catch (error) {
    // The native panel rejects when a user cancels it; that is not an application error.
    if (error instanceof DOMException && error.name === "AbortError") return;
    copied.value = false;
  }
}
</script>

<template>
  <button
    class="share-trigger"
    type="button"
    :title="copied ? '链接已复制' : '分享内容'"
    :aria-label="copied ? '链接已复制' : `分享：${title}`"
    @click="share"
  >
    <Check v-if="copied" :size="16" />
    <Share2 v-else :size="16" />
    <span>{{ copied ? "已复制" : "分享" }}</span>
  </button>
</template>
