package com.aitechskill.book.community.mapper;
import com.aitechskill.book.community.domain.entity.CommunityVoteOptionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
public interface CommunityVoteOptionMapper extends BaseMapper<CommunityVoteOptionEntity> {
 @Select("SELECT COUNT(*) FROM knowledge_post_vote_record WHERE post_id=#{postId} AND user_id=#{userId}") long countVote(@Param("postId") long postId,@Param("userId") long userId);
 @Insert("INSERT INTO knowledge_post_vote_record(post_id,user_id) VALUES(#{postId},#{userId})") @org.apache.ibatis.annotations.Options(useGeneratedKeys=true,keyProperty="id") int insertVote(CommunityVoteRecord record);
 @Insert("INSERT INTO knowledge_post_vote_selection(vote_record_id,option_id) VALUES(#{recordId},#{optionId})") int insertSelection(@Param("recordId") long recordId,@Param("optionId") long optionId);
 @Update("UPDATE knowledge_post_vote_option SET vote_count=vote_count+1 WHERE id=#{id}") int increment(@Param("id") long id);
 @Update("UPDATE knowledge_post_vote SET vote_count=vote_count+1 WHERE post_id=#{postId}") int incrementTotal(@Param("postId") long postId);
}
