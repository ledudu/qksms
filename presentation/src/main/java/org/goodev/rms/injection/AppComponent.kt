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
package org.goodev.rms.injection

import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule
import org.goodev.rms.common.RmsApplication
import org.goodev.rms.common.QkDialog
import org.goodev.rms.common.util.QkChooserTargetService
import org.goodev.rms.common.widget.*
import org.goodev.rms.feature.backup.BackupController
import org.goodev.rms.feature.compose.DetailedChipView
import org.goodev.rms.feature.conversationinfo.injection.ConversationInfoComponent
import org.goodev.rms.feature.settings.SettingsController
import org.goodev.rms.feature.settings.about.AboutController
import org.goodev.rms.feature.settings.swipe.SwipeActionsController
import org.goodev.rms.feature.themepicker.injection.ThemePickerComponent
import org.goodev.rms.feature.widget.WidgetAdapter
import org.goodev.rms.injection.android.ActivityBuilderModule
import org.goodev.rms.injection.android.BroadcastReceiverBuilderModule
import org.goodev.rms.injection.android.ServiceBuilderModule
import org.goodev.rms.util.ContactImageLoader
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AndroidSupportInjectionModule::class,
    AppModule::class,
    ActivityBuilderModule::class,
    BroadcastReceiverBuilderModule::class,
    ServiceBuilderModule::class])
interface AppComponent {

    fun conversationInfoBuilder(): ConversationInfoComponent.Builder
    fun themePickerBuilder(): ThemePickerComponent.Builder

    fun inject(application: RmsApplication)

    fun inject(controller: AboutController)
    fun inject(controller: BackupController)
    fun inject(controller: SettingsController)
    fun inject(controller: SwipeActionsController)

    fun inject(dialog: QkDialog)

    fun inject(fetcher: ContactImageLoader.ContactImageFetcher)

    fun inject(service: WidgetAdapter)

    /**
     * This can't use AndroidInjection, or else it will crash on pre-marshmallow devices
     */
    fun inject(service: QkChooserTargetService)

    fun inject(view: AvatarView)
    fun inject(view: DetailedChipView)
    fun inject(view: PagerTitleView)
    fun inject(view: PreferenceView)
    fun inject(view: QkEditText)
    fun inject(view: QkSwitch)
    fun inject(view: QkTextView)

}