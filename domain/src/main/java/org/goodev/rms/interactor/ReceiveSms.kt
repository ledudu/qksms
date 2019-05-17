/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.goodev.rms.interactor

import android.content.SharedPreferences
import android.telephony.SmsMessage
import io.reactivex.Flowable
import org.goodev.rms.extensions.mapNotNull
import org.goodev.rms.manager.NotificationManager
import org.goodev.rms.manager.ShortcutManager
import org.goodev.rms.repository.ConversationRepository
import org.goodev.rms.repository.MessageRepository
import timber.log.Timber
import javax.inject.Inject

class ReceiveSms @Inject constructor(
        private val conversationRepo: ConversationRepository,
        //private val externalBlockingManager: ExternalBlockingManager,
        private val messageRepo: MessageRepository,
        private val notificationManager: NotificationManager,
        private val updateBadge: UpdateBadge,
        private val pref: SharedPreferences,
        private val shortcutManager: ShortcutManager
) : Interactor<ReceiveSms.Params>() {

    class Params(val subId: Int, val messages: Array<SmsMessage>)

    override fun buildObservable(params: Params): Flowable<*> {
        return Flowable.just(params)
                .filter { it.messages.isNotEmpty() }
//                .filter {
//                    // Don't continue if the sender is blocked
//                    val address = it.messages[0].displayOriginatingAddress
//                    return@filter !externalBlockingManager.shouldBlock(address).blockingGet()
//                }
                .map {
                    val messages = it.messages
                    val address = messages[0].displayOriginatingAddress
                    val time = messages[0].timestampMillis
                    val body: String = messages
                            .mapNotNull { message -> message.displayMessageBody }
                            .reduce { body, new -> body + new }

                    messageRepo.insertReceivedSms(it.subId, address, body, time) // Add the message to the db
                }
                .doOnNext { message ->
                    conversationRepo.updateConversations(message.threadId) // Update the conversation
                }
                .filter { message ->
                    val numbers = pref.getStringSet("block_numbers", emptySet())
                    val address = message.address
                    numbers?.forEach {
                        if (address.startsWith(it) || address.startsWith("86$it") || address.startsWith("+86$it")) {
                            Timber.v("message blocked by number: $it")
                            conversationRepo.getOrCreateConversation(message.threadId)
                            conversationRepo.markBlocked(message.threadId)
                            return@filter false
                        }
                    }
                    return@filter true
                }
                .filter { message ->
                    val keywords = pref.getStringSet("keywords", emptySet())
                    val body = message.body
                    keywords?.forEach {
                        if (body.contains(Regex.fromLiteral(it))) {
                            Timber.v("message blocked by word: $it")
                            conversationRepo.getOrCreateConversation(message.threadId)
                            conversationRepo.markBlocked(message.threadId)
                            return@filter false
                        }
                    }
                    return@filter true
                }
                .mapNotNull { message ->
                    conversationRepo.getOrCreateConversation(message.threadId)
                    // Map message to conversation
                }
                .filter { conversation -> !conversation.blocked } // Don't notify for blocked conversations
                .doOnNext { conversation ->
                    // Unarchive conversation if necessary
                    if (conversation.archived) conversationRepo.markUnarchived(conversation.id)
                }
                .map { conversation -> conversation.id } // Map to the id because [delay] will put us on the wrong thread
                .doOnNext { threadId -> notificationManager.update(threadId) } // Update the notification
                .doOnNext { shortcutManager.updateShortcuts() } // Update shortcuts
                .flatMap { updateBadge.buildObservable(Unit) } // Update the badge
    }

}