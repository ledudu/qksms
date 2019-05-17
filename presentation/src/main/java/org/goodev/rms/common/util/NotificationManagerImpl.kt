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
package org.goodev.rms.common.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import androidx.core.app.*
import androidx.core.graphics.drawable.IconCompat
import org.goodev.rms.R
import org.goodev.rms.common.CopyReceiver
import org.goodev.rms.common.util.extensions.dpToPx
import org.goodev.rms.extensions.isImage
import org.goodev.rms.feature.compose.ComposeActivity
import org.goodev.rms.feature.qkreply.QkReplyActivity
import org.goodev.rms.manager.PermissionManager
import org.goodev.rms.mapper.CursorToPartImpl
import org.goodev.rms.model.Message
import org.goodev.rms.model.VerificationCode
import org.goodev.rms.receiver.DeleteMessagesReceiver
import org.goodev.rms.receiver.MarkReadReceiver
import org.goodev.rms.receiver.MarkSeenReceiver
import org.goodev.rms.receiver.RemoteMessagingReceiver
import org.goodev.rms.repository.ConversationRepository
import org.goodev.rms.repository.MessageRepository
import org.goodev.rms.util.GlideApp
import org.goodev.rms.util.Preferences
import org.goodev.rms.util.tryOrNull
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManagerImpl @Inject constructor(
        private val context: Context,
        private val colors: Colors,
        private val conversationRepo: ConversationRepository,
        private val prefs: Preferences,
        private val messageRepo: MessageRepository,
        private val permissions: PermissionManager
) : org.goodev.rms.manager.NotificationManager {

    companion object {
        const val TAG = "TAG"
        val CARRIER_NUMBERS = arrayListOf("10010", "10086", "10000", "10001")
        val CARRIER_REPLY_PATTERN = Pattern.compile("([A-Za-z0-9]{1,5})：");
        const val DEFAULT_CHANNEL_ID = "notifications_default"
        const val BACKUP_RESTORE_CHANNEL_ID = "notifications_backup_restore"

        val VIBRATE_PATTERN = longArrayOf(0, 200, 0, 200)
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        @SuppressLint("NewApi")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Default"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(DEFAULT_CHANNEL_ID, name, importance).apply {
                enableLights(true)
                lightColor = Color.WHITE
                enableVibration(true)
                vibrationPattern = VIBRATE_PATTERN
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 在通知栏直接回复短信后，调用这个更新通知
     */
    override fun update(threadId: Long) {
        update(threadId, false)
    }

    /**
     * Updates the notification for a particular conversation
     */
    override fun update(threadId: Long, fromReplay: Boolean) {
        // If notifications are disabled, don't do anything
        if (!prefs.notifications(threadId).get()) {
            return
        }

        val realmMessages = if (fromReplay) {
            messageRepo.takeLastMessages(threadId, 2)
        } else {
            messageRepo.getUnreadUnseenMessages(threadId)

        }
        // If there are no messages to be displayed, make sure that the notification is dismissed
        if (realmMessages.isEmpty()) {
            notificationManager.cancel(threadId.toInt())
            return
        }

        val messages = arrayListOf<Message>()
        if (fromReplay) { // 逆序排列一下
            realmMessages.forEach { messages.add(0, it) }
        } else {
            messages.addAll(realmMessages)
        }

        val conversation = conversationRepo.getConversation(threadId) ?: return

        val contentIntent = Intent(context, ComposeActivity::class.java).putExtra("threadId", threadId)

        val seenIntent = Intent(context, MarkSeenReceiver::class.java).putExtra("threadId", threadId)
        val seenPI = PendingIntent.getBroadcast(context, threadId.toInt() + 20000, seenIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        // We can't store a null preference, so map it to a null Uri if the pref string is empty
        val ringtone = prefs.ringtone(threadId).get()
                .takeIf { it.isNotEmpty() }
                ?.let(Uri::parse)

        val notification = NotificationCompat.Builder(context, getChannelIdForNotification(threadId))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setColor(colors.theme(threadId).theme)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.drawable.ic_notification)
                .setNumber(messages.size)
                .setAutoCancel(true)
//                .setContentIntent(contentPI)
                .setDeleteIntent(seenPI)
                .setSound(ringtone)
                .setLights(Color.WHITE, 500, 2000)
                .setVibrate(if (prefs.vibration(threadId).get()) VIBRATE_PATTERN else longArrayOf(0))

        // Tell the notification if it's a group message
        val personMe = Person.Builder()
                .setName(context.getString(R.string.notification_message_me))
                .build()
        val messagingStyle = NotificationCompat.MessagingStyle(personMe)
        if (conversation.recipients.size >= 2) {
            messagingStyle.isGroupConversation = true
            messagingStyle.conversationTitle = conversation.getTitle()
        }

        var messageBody: String = ""
        var messageAddress: String = ""
        var isVerifyCode = false
        var verifyCode = ""
        // Add the messages to the notification
        messages.forEach { message ->
            var person: Person.Builder? = null

            if (!message.isMe()) {
                person = Person.Builder()
                messageBody = message.getSummary()
                messageAddress = PhoneNumberUtils.stripSeparators(message.address)
                val recipient = conversation.recipients
                        .firstOrNull { PhoneNumberUtils.compare(it.address, message.address) }

                person.setName(recipient?.getDisplayName() ?: message.address)

                person.setIcon(GlideApp.with(context)
                        .asBitmap()
                        .circleCrop()
                        .load(messageAddress)
                        .submit(64.dpToPx(context), 64.dpToPx(context))
                        .let { futureGet -> tryOrNull(false) { futureGet.get() } }
                        ?.let(IconCompat::createWithBitmap))

                recipient?.contact
                        ?.let { contact -> "${ContactsContract.Contacts.CONTENT_LOOKUP_URI}/${contact.lookupKey}" }
                        ?.let(person::setUri)
            }

            val summary = message.getSummary();
            val verify = VerificationCode(summary, colors.theme(threadId).theme)
            isVerifyCode = verify.isVerifyCode
            verifyCode = verify.verifyCode
            val messageText: CharSequence = if (verify.isVerifyCode) verify.summary else summary
            val message1 = NotificationCompat.MessagingStyle.Message(messageText, message.date, person?.build())
            message.parts.firstOrNull { it.isImage() }?.let { part ->
                message1.setData(part.type, ContentUris.withAppendedId(CursorToPartImpl.CONTENT_URI, part.id))
            }
            messagingStyle.addMessage(message1)
        }

        // Set the large icon
        val avatar = conversation.recipients.takeIf { it.size == 1 }
                ?.first()?.address
                ?.let { address ->
                    GlideApp.with(context)
                            .asBitmap()
                            .circleCrop()
                            .load(PhoneNumberUtils.stripSeparators(address))
                            .submit(64.dpToPx(context), 64.dpToPx(context))
                }
                ?.let { futureGet -> tryOrNull(false) { futureGet.get() } }

        // Bind the notification contents based on the notification preview mode
        when (prefs.notificationPreviews(threadId).get()) {
            Preferences.NOTIFICATION_PREVIEWS_ALL -> {
                notification
                        .setLargeIcon(avatar)
                        .setStyle(messagingStyle)
            }

            Preferences.NOTIFICATION_PREVIEWS_NAME -> {
                notification
                        .setLargeIcon(avatar)
                        .setContentTitle(conversation.getTitle())
                        .setContentText(context.resources.getQuantityString(R.plurals.notification_new_messages, messages.size, messages.size))
            }

            Preferences.NOTIFICATION_PREVIEWS_NONE -> {
                notification
                        .setContentTitle(context.getString(R.string.app_name))
                        .setContentText(context.resources.getQuantityString(R.plurals.notification_new_messages, messages.size, messages.size))
            }
        }

        // Add all of the people from this conversation to the notification, so that the system can
        // appropriately bypass DND mode
        conversation.recipients
                .mapNotNull { recipient -> recipient.contact?.lookupKey }
                .forEach { uri -> notification.addPerson(uri) }

        // Add the action buttons
        val actionLabels = context.resources.getStringArray(R.array.notification_actions)
        val actions = listOf(prefs.notifAction1, prefs.notifAction2, prefs.notifAction3)
                .map { preference -> preference.get() }
                .distinct()
                .mapNotNull { action ->
                    when (action) {
                        Preferences.NOTIFICATION_ACTION_READ -> {
                            val intent = Intent(context, MarkReadReceiver::class.java).putExtra("threadId", threadId)
                            val pi = PendingIntent.getBroadcast(context, threadId.toInt() + 30000, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                            NotificationCompat.Action.Builder(R.drawable.ic_check_white_24dp, actionLabels[action], pi)
                                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ).build()
                        }

                        Preferences.NOTIFICATION_ACTION_REPLY -> {
                            val replyAction = getReplyAction(threadId, messageBody, messageAddress, isVerifyCode)
//                            notification.extend(NotificationCompat.WearableExtender().addAction(replyAction))
                            replyAction
                        }

                        Preferences.NOTIFICATION_ACTION_CALL -> {
                            val address = conversation.recipients[0]?.address
                            val intentAction = if (permissions.hasCalling()) Intent.ACTION_CALL else Intent.ACTION_DIAL
                            val intent = Intent(intentAction, Uri.parse("tel:$address"))
                            val pi = PendingIntent.getActivity(context, threadId.toInt() + 50000, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                            NotificationCompat.Action.Builder(R.drawable.ic_call_white_24dp, actionLabels[action], pi)
                                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_CALL).build()
                        }

                        Preferences.NOTIFICATION_ACTION_DELETE -> {
                            val messageIds = messages.map { it.id }.toLongArray()
                            val intent = Intent(context, DeleteMessagesReceiver::class.java).putExtra("threadId", threadId).putExtra("messageIds", messageIds)
                            val pi = PendingIntent.getBroadcast(context, threadId.toInt() + 60000, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                            NotificationCompat.Action.Builder(R.drawable.ic_delete_white_24dp, actionLabels[action], pi)
                                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_DELETE).build()
                        }

                        else -> null
                    }
                }.toMutableList()

        if (isVerifyCode) {
            if (actions.size < 3) {
                actions.add(copyAction(verifyCode, threadId))
            } else {
                for (action in actions) {
                    if (NotificationCompat.Action.SEMANTIC_ACTION_REPLY == action.semanticAction
                            || NotificationCompat.Action.SEMANTIC_ACTION_CALL == action.semanticAction) {
                        actions.remove(action)
                        break
                    }
                }
                actions.add(copyAction(verifyCode, threadId))
            }

            contentIntent.putExtra("verifyCode", verifyCode)
        }

        val taskStackBuilder = TaskStackBuilder.create(context)
        taskStackBuilder.addParentStack(ComposeActivity::class.java)
        taskStackBuilder.addNextIntent(contentIntent)
        val contentPI = taskStackBuilder.getPendingIntent(threadId.toInt() + 10000, PendingIntent.FLAG_UPDATE_CURRENT)
        notification.setContentIntent(contentPI)

        notification.extend(NotificationCompat.WearableExtender().addActions(actions))
        actions.forEach {
            notification.addAction(it)
        }

        if (prefs.qkreply.get()) {
            notification.priority = NotificationCompat.PRIORITY_DEFAULT

            val intent = Intent(context, QkReplyActivity::class.java)
                    .putExtra("threadId", threadId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(intent)
        }

        notificationManager.notify(threadId.toInt(), notification.build())
    }

    private fun copyAction(verifyCode: String, threadId: Long): NotificationCompat.Action {
        val intent = Intent(context, CopyReceiver::class.java).putExtra("verifyCode", verifyCode)
        val pi = PendingIntent.getBroadcast(context, threadId.toInt() + 70000, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Action.Builder(R.drawable.ic_content_copy_black_24dp, context.resources.getString(R.string.compose_menu_copy), pi)
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_NONE).build()
    }

    override fun notifyFailed(msgId: Long) {
        val message = messageRepo.getMessage(msgId)

        if (message == null || !message.isFailedMessage()) {
            return
        }

        val conversation = conversationRepo.getConversation(message.threadId) ?: return
        val threadId = conversation.id

        val contentIntent = Intent(context, ComposeActivity::class.java).putExtra("threadId", threadId)
        val taskStackBuilder = TaskStackBuilder.create(context)
        taskStackBuilder.addParentStack(ComposeActivity::class.java)
        taskStackBuilder.addNextIntent(contentIntent)
        val contentPI = taskStackBuilder.getPendingIntent(threadId.toInt() + 40000, PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, getChannelIdForNotification(threadId))
                .setContentTitle(context.getString(R.string.notification_message_failed_title))
                .setContentText(context.getString(R.string.notification_message_failed_text, conversation.getTitle()))
                .setColor(colors.theme(threadId).theme)
                .setPriority(NotificationManagerCompat.IMPORTANCE_MAX)
                .setSmallIcon(R.drawable.ic_notification_failed)
                .setAutoCancel(true)
                .setContentIntent(contentPI)
                .setSound(Uri.parse(prefs.ringtone(threadId).get()))
                .setLights(Color.WHITE, 500, 2000)
                .setVibrate(if (prefs.vibration(threadId).get()) VIBRATE_PATTERN else longArrayOf(0))

        notificationManager.notify(threadId.toInt() + 50000, notification.build())
    }

    private fun getReplyAction(threadId: Long, body: String, address: String, verifyCode: Boolean): NotificationCompat.Action {
        val title = context.resources.getStringArray(R.array.notification_actions)[Preferences.NOTIFICATION_ACTION_REPLY]
        val remoteInput = RemoteInput.Builder("body").setLabel(title)
        var allowGenerateReplies = false
        if (!verifyCode) {
            val choices = processChoices(body, address)
            val responseSet = choices ?: context.resources.getStringArray(R.array.qk_responses)
            remoteInput.setChoices(responseSet)
            allowGenerateReplies = choices.isNullOrEmpty()
        }

        val replyIntent = Intent(context, RemoteMessagingReceiver::class.java).putExtra("threadId", threadId)
        val replyPI = PendingIntent.getBroadcast(context, threadId.toInt() + 40000, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Action.Builder(R.drawable.ic_reply_white_24dp, title, replyPI)
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                .addRemoteInput(remoteInput.build())
                .setAllowGeneratedReplies(allowGenerateReplies)
                .build()
    }

    private fun processChoices(body: String, address: String): Array<out CharSequence>? {
        if (!CARRIER_NUMBERS.contains(address)) {
            return null
        }
        val result = arrayListOf<CharSequence>()
        if (body.contains("回复") && body.contains("编码|指令".toRegex())) {
            body.lines().forEach {
                val matcher = CARRIER_REPLY_PATTERN.matcher(it);
                if (matcher.find()) {
                    result.add(matcher.group(1))
                }
            }
            if (result.isNotEmpty()) {
                return result.toTypedArray()
            }
        }
        if ("10010".equals(address)) {
            result.add("CXLL") //查询流量
            result.add("CXHF") //当月话费
            result.add("CXYE") //余额
            result.add("CXJF") //积分查询
        } else if ("10086".equals(address)) {
            result.add("CXYYSJLL") // 已使用流量
            result.add("CXHF") //当月话费
            result.add("CXYE") //余额
            result.add("CXJF") //积分查询
        } else if ("10001".equals(address)) {
            result.add("SSHF") //(101)(SSHF)	实时话费
            result.add("ZHYE") //(102)(ZHYE)	帐户余额
            result.add("SYZD") //(103)(SYZD)	上月帐单
            result.add("LSZD") //(104)(LSZD)	历史帐单
            result.add("SYDXCX") //(106)(SYDXCX)	剩余短信查询
            result.add("TCXF") //(108)(TCXF)	套餐使用情况
        }
        if (result.isNotEmpty()) {
            return result.toTypedArray()
        }
        return null
    }

    /**
     * Creates a notification channel for the given conversation
     */
    override fun createNotificationChannel(threadId: Long) {

        // Only proceed if the android version supports notification channels
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        conversationRepo.getConversation(threadId)?.let { conversation ->
            val channelId = buildNotificationChannelId(threadId)
            val name = conversation.getTitle()
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                enableLights(true)
                lightColor = Color.WHITE
                enableVibration(true)
                vibrationPattern = VIBRATE_PATTERN
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Returns the notification channel for the given conversation, or null if it doesn't exist
     */
    private fun getNotificationChannel(threadId: Long): NotificationChannel? {
        val channelId = buildNotificationChannelId(threadId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return notificationManager
                    .notificationChannels
                    .firstOrNull { channel -> channel.id == channelId }
        }

        return null
    }

    /**
     * Returns the channel id that should be used for a notification based on the threadId
     *
     * If a notification channel for the conversation exists, use the id for that. Otherwise return
     * the default channel id
     */
    private fun getChannelIdForNotification(threadId: Long): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = buildNotificationChannelId(threadId)

            return notificationManager
                    .notificationChannels
                    .map { channel -> channel.id }
                    .firstOrNull { id -> id == channelId }
                    ?: DEFAULT_CHANNEL_ID
        }

        return DEFAULT_CHANNEL_ID
    }

    /**
     * Formats a notification channel id for a given thread id, whether the channel exists or not
     */
    override fun buildNotificationChannelId(threadId: Long): String {
        return when (threadId) {
            0L -> DEFAULT_CHANNEL_ID
            else -> "notifications_$threadId"
        }
    }

    override fun getNotificationForBackup(): NotificationCompat.Builder {
        if (Build.VERSION.SDK_INT >= 26) {
            val name = context.getString(R.string.backup_notification_channel_name)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(BACKUP_RESTORE_CHANNEL_ID, name, importance)
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(context, BACKUP_RESTORE_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.backup_restoring))
                .setShowWhen(false)
                .setWhen(System.currentTimeMillis()) // Set this anyway in case it's shown
                .setSmallIcon(R.drawable.ic_file_download_black_24dp)
                .setColor(colors.theme().theme)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setProgress(0, 0, true)
                .setOngoing(true)
    }

}