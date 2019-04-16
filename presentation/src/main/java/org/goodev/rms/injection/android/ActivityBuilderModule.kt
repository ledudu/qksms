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
package org.goodev.rms.injection.android

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.goodev.rms.feature.backup.BackupActivity
import org.goodev.rms.feature.blocked.BlockedActivity
import org.goodev.rms.feature.blocked.BlockedActivityModule
import org.goodev.rms.feature.compose.ComposeActivity
import org.goodev.rms.feature.compose.ComposeActivityModule
import org.goodev.rms.feature.conversationinfo.ConversationInfoActivity
import org.goodev.rms.feature.gallery.GalleryActivity
import org.goodev.rms.feature.gallery.GalleryActivityModule
import org.goodev.rms.feature.main.MainActivity
import org.goodev.rms.feature.main.MainActivityModule
import org.goodev.rms.feature.notificationprefs.NotificationPrefsActivity
import org.goodev.rms.feature.notificationprefs.NotificationPrefsActivityModule
import org.goodev.rms.feature.plus.PlusActivity
import org.goodev.rms.feature.plus.PlusActivityModule
import org.goodev.rms.feature.qkreply.QkReplyActivity
import org.goodev.rms.feature.qkreply.QkReplyActivityModule
import org.goodev.rms.feature.scheduled.ScheduledActivity
import org.goodev.rms.feature.scheduled.ScheduledActivityModule
import org.goodev.rms.feature.settings.SettingsActivity
import org.goodev.rms.injection.scope.ActivityScope

@Module
abstract class ActivityBuilderModule {

    @ActivityScope
    @ContributesAndroidInjector(modules = [MainActivityModule::class])
    abstract fun bindMainActivity(): MainActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [PlusActivityModule::class])
    abstract fun bindPlusActivity(): PlusActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [])
    abstract fun bindBackupActivity(): BackupActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [ComposeActivityModule::class])
    abstract fun bindComposeActivity(): ComposeActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [])
    abstract fun bindConversationInfoActivity(): ConversationInfoActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [GalleryActivityModule::class])
    abstract fun bindGalleryActivity(): GalleryActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [NotificationPrefsActivityModule::class])
    abstract fun bindNotificationPrefsActivity(): NotificationPrefsActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [QkReplyActivityModule::class])
    abstract fun bindQkReplyActivity(): QkReplyActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [ScheduledActivityModule::class])
    abstract fun bindScheduledActivity(): ScheduledActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [])
    abstract fun bindSettingsActivity(): SettingsActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [BlockedActivityModule::class])
    abstract fun bindBlockedActivity(): BlockedActivity

}