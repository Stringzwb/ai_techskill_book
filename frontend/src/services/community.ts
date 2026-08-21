import { apiRequest } from './http'
import type { CommunityAttachmentPreview, CommunityComment, CommunityPost, CommunityPostPage } from '../types'

export interface CommunityPostPayload { postType: CommunityPost['postType']; title: string; markdown?: string; linkUrl?: string; tagIds: number[]; voteQuestion?: string; voteOptions?: string[]; allowMultiple?: boolean; anonymous?: boolean }
export function fetchCommunityPosts(params: { keyword?: string; tagId?: number; page?: number; size?: number } = {}) { const q = new URLSearchParams({ page: String(params.page ?? 1), size: String(params.size ?? 12) }); if(params.keyword)q.set('keyword',params.keyword);if(params.tagId)q.set('tagId',String(params.tagId));return apiRequest<CommunityPostPage>(`/api/community/posts?${q}`) }
export function createCommunityPost(payload: CommunityPostPayload) { return apiRequest<CommunityPost>('/api/community/posts',{method:'POST',body:JSON.stringify(payload)}) }
export function uploadCommunityAttachment(postId:number,file:File) { const body=new FormData();body.append('file',file);return apiRequest<CommunityPost>(`/api/community/posts/${postId}/attachments`,{method:'POST',body}) }
export function fetchComments(postId:number) { return apiRequest<CommunityComment[]>(`/api/community/posts/${postId}/comments`) }
export function createComment(postId:number,markdown:string,parentId?:number) { return apiRequest<CommunityComment>(`/api/community/posts/${postId}/comments`,{method:'POST',body:JSON.stringify({markdown,parentId})}) }
export function vote(postId:number,optionIds:number[]) { return apiRequest<CommunityPost>(`/api/community/posts/${postId}/vote`,{method:'POST',body:JSON.stringify({optionIds})}) }
export function deleteCommunityPost(postId:number) { return apiRequest<void>(`/api/community/posts/${postId}`,{method:'DELETE'}) }
export function attachmentUrl(id:number,download=false) { return `/api/community/attachments/${id}/content${download?'?download=true':''}` }
export function fetchAttachmentPreview(id:number) { return apiRequest<CommunityAttachmentPreview>(`/api/community/attachments/${id}/preview`) }
