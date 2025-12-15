package com.frzterr.app.ui.profile

import androidx.lifecycle.ViewModel
import com.frzterr.app.data.repository.user.AppUser

class ProfileViewModel : ViewModel() {

    // State profile terakhir
    var cachedUser: AppUser? = null

    // Untuk kontrol reload manual
    var isLoading: Boolean = false
}
