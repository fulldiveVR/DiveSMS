
package com.moez.QKSMS.repository

import com.moez.QKSMS.model.Conversation
import com.moez.QKSMS.model.Recipient
import com.moez.QKSMS.model.SearchResult
import io.realm.RealmResults

interface ConversationRepository {

    fun getConversations(archived: Boolean = false): RealmResults<Conversation>

    fun getConversationsSnapshot(): List<Conversation>

    /**
     * Returns the top conversations that were active in the last week
     */
    fun getTopConversations(): List<Conversation>

    fun setConversationName(id: Long, name: String)

    fun searchConversations(query: CharSequence): List<SearchResult>

    fun getBlockedConversations(): RealmResults<Conversation>

    fun getBlockedConversationsAsync(): RealmResults<Conversation>

    fun getConversationAsync(threadId: Long): Conversation

    fun getConversation(threadId: Long): Conversation?

    /**
     * Returns all conversations with an id in [threadIds]
     */
    fun getConversations(vararg threadIds: Long): RealmResults<Conversation>

    fun getRecipient(recipientId: Long): Recipient?

    fun getThreadId(recipient: String): Long?

    fun getThreadId(recipients: Collection<String>): Long?

    fun getOrCreateConversation(threadId: Long): Conversation?

    fun getOrCreateConversation(address: String): Conversation?

    fun getOrCreateConversation(addresses: List<String>): Conversation?

    fun saveDraft(threadId: Long, draft: String)

    /**
     * Updates message-related fields in the conversation, like the date and snippet
     */
    fun updateConversations(vararg threadIds: Long)

    fun markArchived(vararg threadIds: Long)

    fun markUnarchived(vararg threadIds: Long)

    fun markPinned(vararg threadIds: Long)

    fun markUnpinned(vararg threadIds: Long)

    fun markBlocked(threadIds: List<Long>, blockingClient: Int, blockReason: String?)

    fun markUnblocked(vararg threadIds: Long)

    fun deleteConversations(vararg threadIds: Long)

}