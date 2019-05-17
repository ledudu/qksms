package org.goodev.rms.feature.blocked

import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import org.goodev.rms.injection.ViewModelKey

/**
 * Created on 2019/4/22.
 */
@Module
class BlockedSettingsActivityModule {
    @Provides
    @IntoMap
    @ViewModelKey(BlockedSettingsViewModel::class)
    fun provideBlockedSettingsViewModel(viewModel: BlockedSettingsViewModel): ViewModel = viewModel
}