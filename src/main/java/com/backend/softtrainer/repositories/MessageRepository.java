package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.enums.ChatRole;
import com.backend.softtrainer.entities.enums.MessageType;
import com.backend.softtrainer.entities.messages.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

//  @QueryHints(value = {
//    @QueryHint(name = "org.hibernate.cacheable", value = "false"),
//    @QueryHint(name = "jakarta.persistence.cache.storeMode", value = CacheStoreMode.REFRESH.name())
//  })
//  @Override
//  Optional<Message> findById(@Param("id") final String id);

  @Deprecated
  Optional<Message> getFirstByChatIdOrderByTimestampDesc(final Long chatId);

  @Query("SELECT m FROM messages m JOIN FETCH m.flowNode f WHERE m.messageType in :actionableMessageTypes AND m.chat.id = " +
    ":chatId ORDER BY m.timestamp DESC")
  List<Message> getActionableMessages(@Param("actionableMessageTypes") final List<String> actionableMessageTypes, @Param(
    "chatId") final Long chatId);

//  @Override
//  @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
//  Message saveAndFlush(Message entity);


  @Deprecated
  @Query("SELECT m FROM messages m JOIN FETCH m.flowNode f WHERE m.role = 'USER' and f.orderNumber = :orderNumber and m.chat.id" +
    " = :chatId")
  List<Message> findAllUserMessagesByOrderNumber(@Param("chatId") final Long chatId,
                                                 @Param("orderNumber") final long orderNumber);

  @Query("SELECT m FROM messages m JOIN FETCH m.flowNode f WHERE m.role = 'USER' and f.orderNumber = :orderNumber and m.chat.id" +
    " = :chatId")
  Optional<Message> findQuestionUserMessagesByOrderNumber(@Param("chatId") final Long chatId,
                                                          @Param("orderNumber") final long orderNumber);

//  @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
  @Query("SELECT m FROM messages m JOIN FETCH m.flowNode f WHERE f.orderNumber = :orderNumber and m.chat.id" +
    " = :chatId")
  List<Message> findMessagesByOrderNumber(@Param("chatId") final Long chatId,
                                             @Param("orderNumber") final long orderNumber);


  @Query("SELECT m FROM messages m JOIN FETCH m.flowNode f WHERE m.role = 'APP' and f.orderNumber = :orderNumber and m.chat.id " +
    "= :chatId")
  Optional<Message> findAppQuestionUserMessagesByOrderNumber(@Param("chatId") final Long chatId,
                                                             @Param("orderNumber") final long orderNumber);

  Optional<Message> findMessageByChatIdAndMessageTypeAndRole(@Param("chatId") final Long chatId,
                                                             @Param("messageType") final MessageType messageType,
                                                             @Param("role") final ChatRole role);

  @Query("SELECT m FROM messages m JOIN FETCH m.flowNode f WHERE m.chat = :chat and f.orderNumber = :orderNumber")
  List<Message> existsByOrderNumberAndChatId(@Param("chat") final Chat chat, @Param("orderNumber") long orderNumber);

}
