package org.goodev.rms.feature.blocked

import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import org.goodev.rms.injection.ViewModelKey

/**
 * Created on 2019/5/5.
 */
@Module
class BlockedNumberSettingsActivityModule {
    @Provides
    @IntoMap
    @ViewModelKey(BlockedNumberSettingsViewModel::class)
    fun provideBlockedSettingsViewModel(viewModel: BlockedNumberSettingsViewModel): ViewModel = viewModel
}