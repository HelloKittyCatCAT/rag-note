package com.sya.ragnote.mapper.conversation;
import com.sya.ragnote.entity.conversation.Conversation;
import com.sya.ragnote.entity.conversation.ConversationTurn;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.*;

public interface ConversationMapper {
    // 已删除来源的旧答案不能重新进入会话上下文。
    String SOURCES_AVAILABLE = """
        NOT EXISTS (
            SELECT 1 FROM jsonb_array_elements(t.answer_json->'sources') source
            WHERE NOT EXISTS (
                SELECT 1 FROM note_chunk c WHERE c.owner_id=t.owner_id
                AND c.note_id=CAST(source->>'noteId' AS uuid)
                AND c.id=CAST(source->>'chunkId' AS uuid)
            )
        )
        """;
    String TURN_SELECT = "SELECT t.client_message_id,t.question,t.request_hash,"
            + "CAST(t.answer_json AS text) AS answer_json," + SOURCES_AVAILABLE
            + " AS sources_available FROM conversation_turn t WHERE t.owner_id=#{ownerId}"
            + " AND t.conversation_id=#{id}";
    @Insert("INSERT INTO conversation(id,owner_id,title) VALUES(#{id},#{ownerId},#{title})")
    int insert(@Param("ownerId") long ownerId, @Param("id") UUID id, @Param("title") String title);

    @Select("SELECT id,title,updated_at FROM conversation WHERE owner_id=#{ownerId} AND id=#{id}")
    Conversation find(@Param("ownerId") long ownerId, @Param("id") UUID id);

    @Select("SELECT id FROM conversation WHERE owner_id=#{ownerId} AND id=#{id} FOR UPDATE")
    UUID lock(@Param("ownerId") long ownerId, @Param("id") UUID id);

    @Select("SELECT id,title,updated_at FROM conversation WHERE owner_id=#{ownerId} ORDER BY updated_at DESC,id LIMIT #{limit} OFFSET #{offset}")
    List<Conversation> list(@Param("ownerId") long ownerId, @Param("offset") long offset, @Param("limit") int limit);

    @Select("SELECT count(*) FROM conversation WHERE owner_id=#{ownerId}")
    long count(@Param("ownerId") long ownerId);

    @Delete("DELETE FROM conversation WHERE owner_id=#{ownerId} AND id=#{id}")
    int delete(@Param("ownerId") long ownerId, @Param("id") UUID id);

    @Update("UPDATE conversation_turn SET lease_token=NULL,lease_until=NULL WHERE owner_id=#{ownerId} AND conversation_id=#{id} AND lease_until <= clock_timestamp()")
    int expire(@Param("ownerId") long ownerId, @Param("id") UUID id);

    @Select("SELECT EXISTS(SELECT 1 FROM conversation_turn WHERE owner_id=#{ownerId} AND conversation_id=#{id} AND lease_token IS NOT NULL)")
    boolean busy(@Param("ownerId") long ownerId, @Param("id") UUID id);

    @Select(TURN_SELECT + " AND t.client_message_id=#{messageId}")
    ConversationTurn findTurn(@Param("ownerId") long ownerId, @Param("id") UUID id, @Param("messageId") UUID messageId);

    @Select(TURN_SELECT + " AND t.answer_json IS NOT NULL AND " + SOURCES_AVAILABLE + " ORDER BY t.seq DESC LIMIT #{limit} OFFSET #{offset}")
    List<ConversationTurn> history(@Param("ownerId") long ownerId, @Param("id") UUID id, @Param("offset") long offset, @Param("limit") int limit);

    @Select("SELECT count(*) FROM conversation_turn t WHERE t.owner_id=#{ownerId} AND t.conversation_id=#{id} AND t.answer_json IS NOT NULL AND " + SOURCES_AVAILABLE)
    long countHistory(@Param("ownerId") long ownerId, @Param("id") UUID id);

    @Insert("""
    INSERT INTO conversation_turn(conversation_id,owner_id,client_message_id,question,request_hash,lease_token,lease_until)
    VALUES(#{id},#{ownerId},#{messageId},#{question},#{hash},#{token},clock_timestamp()+interval '120 seconds')
    ON CONFLICT(conversation_id,client_message_id) DO UPDATE
    SET seq=DEFAULT,lease_token=excluded.lease_token,lease_until=excluded.lease_until
    WHERE conversation_turn.owner_id=#{ownerId} AND conversation_turn.answer_json IS NULL
    """)
    int claim(@Param("ownerId") long ownerId, @Param("id") UUID id, @Param("messageId") UUID messageId, @Param("question") String question, @Param("hash") String hash, @Param("token") UUID token);

    @Update("""
    UPDATE conversation_turn SET answer_json=CAST(#{answer} AS jsonb), lease_token=NULL, lease_until=NULL
    WHERE owner_id=#{ownerId} AND conversation_id=#{id} AND lease_token=#{token}
      AND lease_until>clock_timestamp()
      AND EXISTS(SELECT 1 FROM app_user WHERE id=#{ownerId} AND enabled)
    """)
    int finish(@Param("ownerId") long ownerId, @Param("id") UUID id, @Param("token") UUID token, @Param("answer") String answer);

    @Update("UPDATE conversation_turn SET lease_token=NULL,lease_until=NULL WHERE owner_id=#{ownerId} AND conversation_id=#{id} AND lease_token=#{token}")
    int release(@Param("ownerId") long ownerId, @Param("id") UUID id, @Param("token") UUID token);

    @Update("UPDATE conversation SET updated_at=clock_timestamp() WHERE owner_id=#{ownerId} AND id=#{id}")
    int touch(@Param("ownerId") long ownerId, @Param("id") UUID id);
}
